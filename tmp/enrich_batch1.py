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
    "9ad94057-458c-577b-8b20-3ffe426ca1f2": {
        "indications": "Heartburn, acid indigestion, upset stomach, sour stomach.",
        "dosage_instructions": "Use as directed on the label; typically taken between meals or at bedtime.",
    },
    "fe94a743-a86c-5710-97b3-8b99d12262ff": {
        "indications": "Occasional constipation; antacid for heartburn, acid indigestion, upset stomach.",
        "dosage_instructions": "Use as directed on the label; usually taken once daily; do not use longer than 1 week without medical advice.",
    },
    "6a4cce0d-52fe-5561-a7a4-6f576537993f": {
        "indications": "Occasional constipation; antacid for heartburn, acid indigestion, upset stomach.",
        "dosage_instructions": "Use as directed on the label; usually taken once daily; do not use longer than 1 week without medical advice.",
    },
    "200f7e4d-06e3-545d-b435-3e5f8ea5424a": {
        "indications": "Antacid for heartburn, acid indigestion, upset stomach; calcium supplement when dietary intake is insufficient.",
        "dosage_instructions": "Use as directed on the label; do not use as an antacid for more than 2 weeks without medical advice.",
    },
    "c2a25401-be41-5d4c-b313-f40b18aefc2a": {
        "indications": "Relief of gas, bloating, and pressure caused by excess gas.",
        "dosage_instructions": "Use as directed on the label; typically taken after meals and at bedtime.",
        "requires_prescription": False,
    },
    "fdad730a-e073-51e6-8713-fb73a066b743": {
        "indications": "Diarrhea, traveler's diarrhea, nausea, heartburn, indigestion, upset stomach.",
        "contraindications": "Bleeding problems, stomach ulcer, blood in stools, or allergy to aspirin/salicylates.",
        "side_effects": "Black or darkened tongue, dark stools, constipation.",
        "dosage_instructions": "Use as directed on the label; shake suspension before measuring; chew chewable tablets.",
        "requires_prescription": False,
    },
    "52b61323-9c1c-5348-83b8-e6e820c5fa5c": {
        "indications": "Acute diarrhea; to reduce stool output in people with ileostomy.",
        "contraindications": "Ulcerative colitis, bloody or tarry stools, diarrhea with high fever, or antibiotic-associated diarrhea.",
        "side_effects": "Constipation, dizziness, drowsiness.",
        "dosage_instructions": "Use as directed; typical adult dose is 4 mg after first loose stool then 2 mg after each loose stool; do not exceed 16 mg/day.",
        "requires_prescription": False,
    },
    "0d1a3133-3c2b-59e0-a1ac-25f468cb1fb1": {
        "indications": "Constipation; bowel preparation before procedures.",
        "contraindications": "Abdominal pain, nausea, or vomiting of unknown cause.",
        "side_effects": "Abdominal cramps, rectal burning.",
        "dosage_instructions": "Use as directed on the label; oral effect in 6-12 hours; rectal effect in 15-60 minutes.",
        "requires_prescription": False,
    },
    "5b2dd5a4-bbe7-5ab7-85da-c5b451ea2f0b": {
        "indications": "Stool softener for occasional constipation.",
        "dosage_instructions": "Use as directed on the label.",
        "requires_prescription": False,
    },
    "0fab4a1f-5378-5f1f-8f80-1ba3ce820dc7": {
        "indications": "Stool softener for occasional constipation.",
        "dosage_instructions": "Use as directed on the label.",
        "requires_prescription": False,
    },
    "81d7f707-3739-5fe0-986c-333bc746a232": {
        "indications": "Laxative for occasional constipation or irregular bowel movements.",
        "side_effects": "Rectal discomfort or burning.",
        "dosage_instructions": "Rectal use only; typically used once daily; bowel movement usually within 15-60 minutes.",
        "requires_prescription": False,
    },
    "4ce9fe73-fd03-5934-9c10-1e34a751f57a": {
        "indications": "Occasional constipation.",
        "contraindications": "Diarrhea or intestinal blockage; inflammatory bowel disease.",
        "dosage_instructions": "Use as directed on the label; typically produces a bowel movement in 6-12 hours.",
        "requires_prescription": False,
    },
    "3a783fc2-e884-5b4e-96db-4560f7b6a823": {
        "indications": "Constipation or bowel irregularity.",
        "dosage_instructions": "Mix with a full glass of water; use as directed on the label.",
        "requires_prescription": False,
    },
    "b1a25915-b468-5b7b-af3c-7c0ac414d458": {
        "indications": "Constipation; helps maintain regular bowel movements.",
        "dosage_instructions": "Mix powder with a full glass of water; use as directed on the label.",
        "requires_prescription": False,
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
        sql_lines.append(rebuild_insert(values))

    MEDICINES_SQL.write_text("\n".join(sql_lines) + "\n", encoding="utf-8")

    summary = json.loads(SUMMARY_JSON.read_text(encoding="utf-8"))
    summary["number_of_fields_filled_from_internet"] = summary.get("number_of_fields_filled_from_internet", 0) + fields_filled
    summary["number_of_prescription_changes"] = summary.get("number_of_prescription_changes", 0) + prescription_changes

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

    SUMMARY_JSON.write_text(json.dumps(summary, indent=2, ensure_ascii=True) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
