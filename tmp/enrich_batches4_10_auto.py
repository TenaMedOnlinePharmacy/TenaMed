import json
import re
import time
from pathlib import Path
from urllib.parse import quote

import requests

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "docs" / "lmcp_extraction"
TMP_DIR = BASE_DIR / "tmp"

MEDICINES_JSON = DATA_DIR / "medicines.json"
MEDICINES_SQL = DATA_DIR / "medicines.sql"
SUMMARY_JSON = DATA_DIR / "summary.json"

NULLABLE_FIELDS = [
    "indications",
    "contraindications",
    "side_effects",
    "dosage_instructions",
    "pregnancy_category",
    "schedule",
]

HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}


def slugify(name: str) -> str:
    s = name or ""
    s = s.strip()
    # remove common trailing qualifiers
    s = re.sub(r"\s*\(.*?\)", "", s)
    s = re.sub(r"[^0-9A-Za-z\s/+\-]+", "", s)
    s = s.replace(" ", "-")
    s = s.replace("/", "-")
    s = s.replace("+", "-plus")
    s = s.strip("-")
    return s.lower()


def fetch_drugsdotcom(slug: str):
    base = f"https://www.drugs.com/{quote(slug)}.html"
    try:
        r = requests.get(base, headers=HEADERS, timeout=15)
        if r.status_code == 200:
            return r.text, base, 200
    except Exception as e:
        last_err = str(e)

    # try simple variants
    variants = [
        slug,
        slug + "-oral",
        slug + "-tablets",
        slug + "-syrup",
        slug + "-injection",
    ]
    for v in variants:
        url = f"https://www.drugs.com/{quote(v)}.html"
        try:
            r = requests.get(url, headers=HEADERS, timeout=12)
            if r.status_code == 200:
                return r.text, url, 200
        except Exception as e:
            last_err = str(e)

    # fallback: use drugs.com search
    try:
        search_url = f"https://www.drugs.com/search.php?searchterm={quote(slug)}"
        s = requests.get(search_url, headers=HEADERS, timeout=12)
        if s.status_code == 200:
            # find first html link
            m = re.search(r'href="(/[^"\s>]+\.html)"', s.text)
            if m:
                page = "https://www.drugs.com" + m.group(1)
                r = requests.get(page, headers=HEADERS, timeout=12)
                if r.status_code == 200:
                    return r.text, page, 200
            return None, search_url, 404
        else:
            return None, search_url, s.status_code
    except Exception as e:
        return None, f"https://www.drugs.com/{quote(slug)}.html", last_err if 'last_err' in locals() else str(e)


def extract_uses_from_html(html: str):
    if not html:
        return None
    # try common headings
    patterns = [
        r"<h2[^>]*>Uses</h2>(.*?)<h2",
        r"<h2[^>]*>What is [^<]+ used for\?</h2>(.*?)<h2",
        r"<h3[^>]*>Uses</h3>(.*?)<h2",
        r"<div[^>]+id=\"uses(?:\-section)?\".*?>(.*?)<h2",
    ]
    for pat in patterns:
        m = re.search(pat, html, re.I | re.S)
        if m:
            text = re.sub(r"<[^>]+>", "", m.group(1))
            text = re.sub(r"\s+", " ", text).strip()
            if len(text) > 60:
                return text
    # fallback: search for 'Uses:' label
    m = re.search(r"Uses:\s*</strong>\s*<p>(.*?)</p>", html, re.I | re.S)
    if m:
        text = re.sub(r"<[^>]+>", "", m.group(1)).strip()
        if len(text) > 60:
            return text
    return None


def sql_literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    text = str(value)
    text = text.replace("'", "''")
    return f"'{text}'"


def rebuild_insert(values):
    rendered = ", ".join(sql_literal(v) for v in values)
    return (
        "INSERT INTO medicines (id, name, generic_name, category_id, dosage_form_id, therapeutic_class, schedule, need_manual_review, dose_value, dose_unit, regulatory_code, requires_prescription, indications, contraindications, side_effects, dosage_instructions, pregnancy_category, image_url) VALUES ("
        + rendered
        + ");"
    )


def main():
    data = json.loads(MEDICINES_JSON.read_text(encoding="utf-8"))
    by_id = {item["id"]: item for item in data}

    summary = json.loads(SUMMARY_JSON.read_text(encoding="utf-8"))

    total_fields_filled = 0
    total_prescription_changes = 0

    for batch in range(4, 11):
        batch_file = TMP_DIR / f"batch{batch}.json"
        if not batch_file.exists():
            print(f"Batch file missing: {batch_file}")
            continue
        items = json.loads(batch_file.read_text(encoding="utf-8"))
        updates = {}
        print(f"Processing batch {batch}, {len(items)} items")
        for item in items:
            mid = item.get("id")
            name = item.get("generic_name") or item.get("name")
            slug = slugify(name)
            html, url, status = fetch_drugsdotcom(slug)
            if html:
                uses = extract_uses_from_html(html)
                if uses:
                    original = by_id.get(mid, {})
                    # only fill if original field is null
                    if original.get("indications") is None:
                        updates[mid] = {"indications": uses}
                        # apply immediately to in-memory structure
                        by_id[mid]["indications"] = uses
                        total_fields_filled += 1
                        print(f"Filled indications for {name} ({mid}) from {url}")
            else:
                print(f"No page for {name} ({slug}) — {status}")
            time.sleep(0.8)

        # After batch, write medicines.json and medicines.sql and update summary
        # build list from by_id preserving original order
        new_data = [by_id[item["id"]] for item in data]
        MEDICINES_JSON.write_text(json.dumps(new_data, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

        sql_lines = []
        for item in new_data:
            values = [
                item.get("id"),
                item.get("name"),
                item.get("generic_name"),
                item.get("category_id"),
                item.get("dosage_form_id"),
                item.get("therapeutic_class"),
                item.get("schedule"),
                item.get("need_manual_review"),
                item.get("dose_value"),
                item.get("dose_unit"),
                item.get("regulatory_code"),
                item.get("requires_prescription"),
                item.get("indications"),
                item.get("contraindications"),
                item.get("side_effects"),
                item.get("dosage_instructions"),
                item.get("pregnancy_category"),
                item.get("image_url"),
            ]
            sql_lines.append(rebuild_insert(values))
        MEDICINES_SQL.write_text("\n".join(sql_lines) + "\n", encoding="utf-8")

        # recompute remaining nulls and quality
        remaining_nulls = 0
        for item in new_data:
            for field in NULLABLE_FIELDS:
                if item.get(field) is None:
                    remaining_nulls += 1
        total_nullable = len(new_data) * len(NULLABLE_FIELDS)
        quality_score = 0
        if total_nullable:
            quality_score = round(100 * (1 - (remaining_nulls / total_nullable)))
            quality_score = max(0, min(100, quality_score))

        summary["number_of_fields_filled_from_internet"] = summary.get(
            "number_of_fields_filled_from_internet", 0
        ) + total_fields_filled
        summary["number_of_prescription_changes"] = summary.get(
            "number_of_prescription_changes", 0
        ) + total_prescription_changes
        summary["number_of_remaining_null_fields"] = remaining_nulls
        summary["data_quality_score"] = quality_score

        SUMMARY_JSON.write_text(json.dumps(summary, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

    print("Completed batches 4-10 run.")


if __name__ == "__main__":
    main()
