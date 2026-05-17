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
    "f52de6f8-a7dd-5eae-b472-ba0552b7b439": {
        "indications": "High blood pressure; heart failure; improve survival after heart attack.",
        "contraindications": "Pregnancy.",
        "side_effects": "Cough, dizziness, headache, tiredness, nausea, diarrhea.",
        "dosage_instructions": "Usually taken once daily at the same time; follow the prescription label.",
    },
    "1e2a01e1-dddd-58bb-a246-4d21c1c0655b": {
        "indications": "High blood pressure; edema (fluid retention).",
        "side_effects": "Frequent urination, headache, constipation, diarrhea, blurred vision.",
        "dosage_instructions": "Usually taken once or twice daily; for edema may be taken daily or on certain days.",
    },
    "e6664ed6-9245-5b4c-83c8-3b0431ee0615": {
        "indications": "High blood pressure; heart failure; improve survival after heart attack with heart failure.",
        "contraindications": "Pregnancy.",
        "side_effects": "Dizziness, headache, tiredness, abdominal pain, diarrhea, low blood pressure, high potassium.",
        "dosage_instructions": "Usually taken once daily for high blood pressure and twice daily for heart failure or post-heart attack.",
    },
    "0f98056c-7402-50a7-9e4f-b4e54d2edc12": {
        "indications": "High cholesterol; reduce risk of heart attack, stroke, and angina in at-risk patients.",
        "contraindications": "Active liver disease or pregnancy.",
        "side_effects": "Muscle or joint pain, diarrhea, nausea, upset stomach.",
        "dosage_instructions": "Taken once daily with or without food at the same time each day.",
    },
    "3303860a-192e-5215-9ad5-63492102cf84": {
        "indications": "Lower LDL cholesterol and triglycerides; reduce risk of stroke and heart attack in high-risk patients.",
        "contraindications": "Active liver disease or pregnancy.",
        "side_effects": "Headache, constipation, nausea, stomach pain; muscle pain can occur.",
        "dosage_instructions": "Usually taken at bedtime or with the evening meal; follow the prescription label.",
    },
    "51fefd46-2f60-57f3-9d61-af86252f0b60": {
        "indications": "High blood pressure; reduce stroke risk in patients with left ventricular hypertrophy; diabetic kidney disease.",
        "contraindications": "Pregnancy.",
        "side_effects": "Dizziness, tiredness, low blood pressure, diarrhea, back pain.",
        "dosage_instructions": "Usually taken once daily with or without food; follow the prescription label.",
    },
    "79ad4d8d-4af4-5f96-90a0-4aaed2745525": {
        "indications": "High blood pressure; angina; coronary artery disease.",
        "side_effects": "Dizziness, leg or ankle swelling, flushing, tiredness, nausea.",
        "dosage_instructions": "Usually taken once daily at the same time, with or without food.",
    },
    "e66f54a6-c434-59a7-99b9-ea35b4f79b5d": {
        "indications": "Heart failure, high blood pressure, low potassium, edema, or hyperaldosteronism.",
        "contraindications": "Addison's disease, high potassium, inability to urinate, or use with eplerenone.",
        "side_effects": "Breast tenderness or swelling, dizziness.",
        "dosage_instructions": "Take as prescribed, with or without food, at the same time each day.",
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
