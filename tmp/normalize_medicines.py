import json
import re
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "docs" / "lmcp_extraction"

MEDICINES_JSON = DATA_DIR / "medicines.json"
MEDICINES_SQL = DATA_DIR / "medicines.sql"
CATEGORIES_SQL = DATA_DIR / "categories.sql"
SUMMARY_JSON = DATA_DIR / "summary.json"

PREFIX_RE = re.compile(r"^\s*\d+\s*(?:[.)]\s+|-\s+)")
LEADING_NUMBER_RE = re.compile(r"^\s*\d+\s+(?=[A-Za-z])")
MULTI_SPACE_RE = re.compile(r"\s{2,}")
DOT_LEADER_RE = re.compile(r"\s*\.{2,}\s*\d+\s*$")


def clean_name(value: str) -> str:
    if value is None:
        return value
    new_value = value.strip()
    new_value = PREFIX_RE.sub("", new_value)
    new_value = LEADING_NUMBER_RE.sub("", new_value)
    new_value = MULTI_SPACE_RE.sub(" ", new_value)
    return new_value.strip()


def clean_category_text(value: str) -> str:
    if value is None:
        return value
    new_value = value.strip()
    new_value = DOT_LEADER_RE.sub("", new_value)
    new_value = new_value.replace("SSuullpphhoonnaammiiddeess", "Sulfonamides")
    new_value = MULTI_SPACE_RE.sub(" ", new_value)
    return new_value.strip()


def sql_literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    text = str(value)
    text = text.replace("'", "''")
    return f"'{text}'"


def parse_sql_values(value_text: str):
    values = []
    current = []
    in_string = False
    i = 0
    while i < len(value_text):
        ch = value_text[i]
        if in_string:
            if ch == "'":
                if i + 1 < len(value_text) and value_text[i + 1] == "'":
                    current.append("'")
                    i += 1
                else:
                    in_string = False
            else:
                current.append(ch)
        else:
            if ch == "'":
                in_string = True
            elif ch == ",":
                token = "".join(current).strip()
                values.append(token if token != "NULL" else None)
                current = []
            else:
                current.append(ch)
        i += 1

    token = "".join(current).strip()
    values.append(token if token != "NULL" else None)
    return values


def rebuild_insert(prefix: str, values, suffix: str = ");"):
    rendered = ", ".join(sql_literal(v) for v in values)
    return f"{prefix}{rendered}{suffix}"


def update_categories():
    if not CATEGORIES_SQL.exists():
        return 0
    updated = 0
    lines = CATEGORIES_SQL.read_text(encoding="utf-8").splitlines()
    new_lines = []
    for line in lines:
        if not line.startswith("INSERT INTO categories"):
            new_lines.append(line)
            continue
        prefix, raw_values = line.split("VALUES (", 1)
        raw_values = raw_values.rstrip(");")
        values = parse_sql_values(raw_values)
        if len(values) >= 4:
            old_name = values[2]
            old_section = values[3]
            new_name = clean_category_text(old_name) if old_name else old_name
            new_section = clean_category_text(old_section) if old_section else old_section
            if new_name != old_name or new_section != old_section:
                updated += 1
            values[2] = new_name
            values[3] = new_section
        new_lines.append(rebuild_insert(prefix + "VALUES (", values))
    CATEGORIES_SQL.write_text("\n".join(new_lines) + "\n", encoding="utf-8")
    return updated


def update_medicines():
    data = json.loads(MEDICINES_JSON.read_text(encoding="utf-8"))
    cleaned_names = 0
    cleaned_generics = 0

    for item in data:
        old_name = item.get("name")
        old_generic = item.get("generic_name")

        new_name = clean_name(old_name) if old_name else old_name
        new_generic = clean_name(old_generic) if old_generic else old_generic

        if new_name != old_name:
            cleaned_names += 1
            item["name"] = new_name
        if new_generic != old_generic:
            cleaned_generics += 1
            item["generic_name"] = new_generic

    MEDICINES_JSON.write_text(json.dumps(data, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")

    sql_lines = []
    for item in data:
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
        prefix = "INSERT INTO medicines (id, name, generic_name, category_id, dosage_form_id, therapeutic_class, schedule, need_manual_review, dose_value, dose_unit, regulatory_code, requires_prescription, indications, contraindications, side_effects, dosage_instructions, pregnancy_category, image_url) VALUES ("
        sql_lines.append(rebuild_insert(prefix, values))

    MEDICINES_SQL.write_text("\n".join(sql_lines) + "\n", encoding="utf-8")
    return data, cleaned_names, cleaned_generics


def update_summary(data, cleaned_names, cleaned_generics, categories_updated):
    total = len(data)
    nullable_fields = [
        "indications",
        "contraindications",
        "side_effects",
        "dosage_instructions",
        "pregnancy_category",
        "schedule",
    ]
    remaining_nulls = 0
    for item in data:
        for field in nullable_fields:
            if item.get(field) is None:
                remaining_nulls += 1

    total_nullable = total * len(nullable_fields)
    quality_score = 0
    if total_nullable > 0:
        quality_score = round(100 * (1 - (remaining_nulls / total_nullable)))
        quality_score = max(0, min(100, quality_score))

    summary = {
        "total_medicines_processed": total,
        "number_of_names_cleaned": cleaned_names,
        "number_of_generic_names_corrected": cleaned_generics,
        "number_of_fields_filled_from_internet": 0,
        "number_of_prescription_changes": 0,
        "number_of_remaining_null_fields": remaining_nulls,
        "number_of_categories_created": 0,
        "number_of_forms_created": 0,
        "number_of_allergies_created": 0,
        "data_quality_score": quality_score,
        "categories_cleaned": categories_updated,
    }

    SUMMARY_JSON.write_text(json.dumps(summary, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


def main():
    categories_updated = update_categories()
    data, cleaned_names, cleaned_generics = update_medicines()
    update_summary(data, cleaned_names, cleaned_generics, categories_updated)


if __name__ == "__main__":
    main()
