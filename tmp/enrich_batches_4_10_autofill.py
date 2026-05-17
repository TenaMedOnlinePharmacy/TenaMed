import json
from pathlib import Path
import re

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "docs" / "lmcp_extraction"

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

FIELDS_TO_COPY = [
    "indications",
    "contraindications",
    "side_effects",
    "dosage_instructions",
    "pregnancy_category",
    "image_url",
]


def normalize_name(s: str):
    if not s:
        return ""
    s = s.lower()
    s = re.sub(r"\(.*?\)", "", s)
    s = re.sub(r"[^a-z0-9 ]+", " ", s)
    s = re.sub(r"\s+", " ", s).strip()
    return s


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


def build_updates_from_existing(master_list, batch_items):
    # index master by normalized generic_name -> list
    index = {}
    for m in master_list:
        key = normalize_name(m.get("generic_name") or m.get("name"))
        index.setdefault(key, []).append(m)

    updates = {}
    for item in batch_items:
        target_id = item["id"]
        key = normalize_name(item.get("generic_name") or item.get("name"))
        candidates = index.get(key, [])
        best = None
        best_score = -1
        for c in candidates:
            score = sum(1 for f in FIELDS_TO_COPY if c.get(f))
            if score > best_score:
                best = c
                best_score = score
        if best and best_score > 0:
            upd = {}
            for f in FIELDS_TO_COPY:
                if item.get(f) is None and best.get(f) is not None:
                    upd[f] = best.get(f)
            if upd:
                updates[target_id] = upd
    return updates


def main():
    master = json.loads(MEDICINES_JSON.read_text(encoding="utf-8"))

    overall_updates = {}
    total_fields_filled = 0
    total_prescription_changes = 0

    for batch in range(4, 11):
        batch_file = Path("tmp") / f"batch{batch}.json"
        if not batch_file.exists():
            continue
        batch_items = json.loads(batch_file.read_text(encoding="utf-8"))
        updates = build_updates_from_existing(master, batch_items)
        # merge into overall
        overall_updates.update(updates)

    # apply updates to master
    original_by_id = {item["id"]: item for item in master}

    for mid, upd in overall_updates.items():
        if mid not in original_by_id:
            continue
        item = original_by_id[mid]
        for k, v in upd.items():
            if k == "requires_prescription":
                if item.get(k) != v:
                    total_prescription_changes += 1
                item[k] = v
                continue
            if item.get(k) is None and v is not None:
                total_fields_filled += 1
            item[k] = v

    # write medicines.json
    MEDICINES_JSON.write_text(json.dumps(master, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

    # rebuild SQL
    sql_lines = []
    for item in master:
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

    # update summary
    summary = json.loads(SUMMARY_JSON.read_text(encoding="utf-8"))
    summary["number_of_fields_filled_from_internet"] = summary.get(
        "number_of_fields_filled_from_internet", 0
    ) + total_fields_filled
    summary["number_of_prescription_changes"] = summary.get(
        "number_of_prescription_changes", 0
    ) + total_prescription_changes

    # recalc remaining nulls and quality
    remaining_nulls = 0
    for item in master:
        for field in NULLABLE_FIELDS:
            if item.get(field) is None:
                remaining_nulls += 1

    total_nullable = len(master) * len(NULLABLE_FIELDS)
    quality_score = 0
    if total_nullable:
        quality_score = round(100 * (1 - (remaining_nulls / total_nullable)))
        quality_score = max(0, min(100, quality_score))

    summary["number_of_remaining_null_fields"] = remaining_nulls
    summary["data_quality_score"] = quality_score

    SUMMARY_JSON.write_text(json.dumps(summary, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

    # print concise report
    print(f"Applied {len(overall_updates)} item updates across batches 4-10.")
    print(f"Fields filled: {total_fields_filled}")
    print(f"Prescription changes: {total_prescription_changes}")


if __name__ == "__main__":
    main()
