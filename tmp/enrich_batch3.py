import json
from pathlib import Path

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

UPDATES = {
    "03a1ff5e-1ebd-5d2e-97ec-7ffad7396329": {
        "indications": "High blood pressure; heart failure; improve survival after heart attack.",
        "contraindications": "Pregnancy.",
        "side_effects": "Cough, dizziness, headache, tiredness, nausea, diarrhea.",
        "dosage_instructions": "Usually taken once daily at the same time; follow the prescription label.",
    },
    "b85a4810-3902-5182-a79c-8ba0b71a6ae4": {
        "indications": "High blood pressure; reduce stroke risk in patients with left ventricular hypertrophy; diabetic kidney disease.",
        "contraindications": "Pregnancy.",
        "side_effects": "Dizziness, tiredness, low blood pressure, diarrhea, back pain.",
        "dosage_instructions": "Usually taken once daily with or without food; follow the prescription label.",
    },
    "b0e3a534-fc7e-5201-bbbf-12a323c54a11": {
        "indications": "High blood pressure; angina.",
        "contraindications": "Severe coronary artery disease or heart attack within the past 2 weeks.",
        "side_effects": "Dizziness, flushing, weakness, headache, nausea, heartburn, muscle cramps.",
        "dosage_instructions": "Take as prescribed; do not crush extended-release tablets; keep a consistent daily schedule.",
    },
}


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
    original = json.loads(MEDICINES_JSON.read_text(encoding="utf-8"))
    data = json.loads(MEDICINES_JSON.read_text(encoding="utf-8"))
    original_by_id = {item["id"]: item for item in original}

    prescription_changes = 0
    fields_filled = 0

    for item in data:
        update = UPDATES.get(item["id"])
        if not update:
            continue
        before = original_by_id[item["id"]]
        for key, value in update.items():
            if key == "requires_prescription":
                if item.get("requires_prescription") != value:
                    prescription_changes += 1
                item["requires_prescription"] = value
                continue
            if before.get(key) is None and value is not None:
                fields_filled += 1
            item[key] = value

    MEDICINES_JSON.write_text(
        json.dumps(data, indent=2, ensure_ascii=True) + "\n", encoding="utf-8"
    )

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
        sql_lines.append(rebuild_insert(values))

    MEDICINES_SQL.write_text("\n".join(sql_lines) + "\n", encoding="utf-8")

    summary = json.loads(SUMMARY_JSON.read_text(encoding="utf-8"))
    summary["number_of_fields_filled_from_internet"] = summary.get(
        "number_of_fields_filled_from_internet", 0
    ) + fields_filled
    summary["number_of_prescription_changes"] = summary.get(
        "number_of_prescription_changes", 0
    ) + prescription_changes

    remaining_nulls = 0
    for item in data:
        for field in NULLABLE_FIELDS:
            if item.get(field) is None:
                remaining_nulls += 1

    total_nullable = len(data) * len(NULLABLE_FIELDS)
    quality_score = 0
    if total_nullable:
        quality_score = round(100 * (1 - (remaining_nulls / total_nullable)))
        quality_score = max(0, min(100, quality_score))

    summary["number_of_remaining_null_fields"] = remaining_nulls
    summary["data_quality_score"] = quality_score

    SUMMARY_JSON.write_text(
        json.dumps(summary, indent=2, ensure_ascii=True) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    main()
