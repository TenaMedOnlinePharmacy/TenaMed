import json
import os
import re
import sys
import uuid
from dataclasses import dataclass, asdict
from decimal import Decimal

PDF_PATH = os.path.join(os.path.dirname(__file__), "..", "List-of-Medicines-for-Community-Pharmacy.pdf")
OUT_DIR = os.path.dirname(__file__)

CATEGORY_RE = re.compile(r"^(?P<code>[A-Z]{2,}\d{3})\s+(?P<name>.+)$")
SECTION_RE = re.compile(r"^(?P<code>[A-Z]{2,}\d{3})\s+(?P<name>.+)$")
DOSAGE_RE = re.compile(r"\b(Tablet|Capsule|Injection|Suspension|Syrup|Drops|Powder for Injection|Oral Solution|Cream|Ointment|Gel|Solution|Suppository|Inhaler|Spray|Patch|Lotion)\b", re.IGNORECASE)
DOSE_RE = re.compile(r"(?P<value>\d+(?:\.\d+)?)\s*(?P<unit>mg|mcg|g|ml|IU|units|%)(?:\b|/)", re.IGNORECASE)

COMBO_MARKERS = ["+", "/"]
OTC_KEYWORDS = ["antacid", "ors", "zinc", "activated charcoal"]
RX_KEYWORDS = ["antibiotic", "inject", "corticosteroid", "psych", "controlled"]

@dataclass
class Category:
    id: str
    code: str
    name: str
    therapeutic_section: str

@dataclass
class DosageForm:
    id: str
    name: str

@dataclass
class Allergen:
    id: str
    name: str
    code: str
    description: str
    allergen_type: str

@dataclass
class Medicine:
    id: str
    name: str
    generic_name: str
    category_id: str
    dosage_form_id: str
    therapeutic_class: str
    schedule: str
    need_manual_review: bool
    dose_value: str
    dose_unit: str
    regulatory_code: str
    requires_prescription: bool
    indications: str
    contraindications: str
    side_effects: str
    dosage_instructions: str
    pregnancy_category: str
    image_url: str

@dataclass
class MedicineAllergen:
    id: str
    medicine_id: str
    allergen_id: str


def load_pdf_text(pdf_path):
    try:
        import pdfplumber
        texts = []
        with pdfplumber.open(pdf_path) as pdf:
            for page in pdf.pages:
                texts.append(page.extract_text() or "")
        return "\n".join(texts)
    except Exception:
        try:
            import fitz
        except Exception as exc:
            raise RuntimeError("Missing pdfplumber or PyMuPDF (fitz). Install one to proceed.") from exc
        doc = fitz.open(pdf_path)
        texts = []
        for page in doc:
            texts.append(page.get_text())
        return "\n".join(texts)


def normalize_whitespace(text):
    return re.sub(r"\s+", " ", text).strip()


def normalize_name(name):
    return normalize_whitespace(name).title()


def detect_dosage_form(line):
    match = DOSAGE_RE.search(line)
    if not match:
        return None
    return match.group(0).title()


def detect_dose(line):
    match = DOSE_RE.search(line)
    if not match:
        return None, None
    return match.group("value"), match.group("unit").lower()


def is_combo(name):
    return any(marker in name for marker in COMBO_MARKERS)


def requires_prescription(therapeutic_class, dosage_form):
    text = f"{therapeutic_class} {dosage_form}".lower()
    if any(k in text for k in OTC_KEYWORDS):
        return False
    if any(k in text for k in RX_KEYWORDS):
        return True
    if dosage_form and "Injection" in dosage_form:
        return True
    return True


def infer_allergens(name):
    lower = name.lower()
    allergens = []
    if "penicillin" in lower or "amoxicillin" in lower or "ampicillin" in lower:
        allergens.append(("Penicillin", "PEN"))
    if "sulfa" in lower or "sulfonamide" in lower:
        allergens.append(("Sulfonamide", "SULF"))
    if "ibuprofen" in lower or "diclofenac" in lower or "naproxen" in lower:
        allergens.append(("NSAID", "NSAID"))
    return allergens


def stable_uuid(namespace, value):
    return str(uuid.uuid5(namespace, value))


def parse_lines(text):
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    return lines


def extract_medicines(lines):
    namespace = uuid.uuid5(uuid.NAMESPACE_DNS, "tenamed-lmcp")
    categories = {}
    dosage_forms = {}
    allergens = {}
    medicines = []
    medicine_allergens = []

    current_section = None
    current_category = None
    category_seq = {}

    for line in lines:
        if len(line) < 3:
            continue

        section_match = SECTION_RE.match(line)
        if section_match:
            code = section_match.group("code")
            name = normalize_whitespace(section_match.group("name"))
            current_section = name
            if code not in categories:
                cat_id = stable_uuid(namespace, f"cat:{code}")
                categories[code] = Category(
                    id=cat_id,
                    code=code,
                    name=name,
                    therapeutic_section=name,
                )
                category_seq[code] = 0
            current_category = categories[code]
            continue

        if current_category is None:
            continue

        if line.lower().startswith(("annex", "index", "acknowledgement", "ethics", "introduction")):
            continue

        dosage_form = detect_dosage_form(line)
        if dosage_form is None:
            continue

        name_part = line.split(dosage_form, 1)[0]
        name = normalize_whitespace(name_part)
        if not name:
            continue

        display_name = name
        generic_name = normalize_name(name)
        dose_value, dose_unit = detect_dose(line)
        combo = is_combo(name)

        category_seq[current_category.code] += 1
        seq = category_seq[current_category.code]
        regulatory_code = f"EFDA-{current_category.code}-{seq:04d}"

        dosage_form_id = dosage_forms.get(dosage_form)
        if dosage_form_id is None:
            dosage_form_id = stable_uuid(namespace, f"dosage:{dosage_form}")
            dosage_forms[dosage_form] = dosage_form_id

        need_manual = combo or (dose_value is None and dose_unit is None)

        requires_rx = requires_prescription(current_category.therapeutic_section, dosage_form)
        if combo:
            need_manual = True

        med_id = stable_uuid(namespace, f"med:{current_category.code}:{display_name}:{dosage_form}:{dose_value}:{dose_unit}")

        medicine = Medicine(
            id=med_id,
            name=display_name,
            generic_name=generic_name,
            category_id=current_category.id,
            dosage_form_id=dosage_form_id,
            therapeutic_class=current_section or current_category.therapeutic_section,
            schedule=None,
            need_manual_review=need_manual,
            dose_value=str(dose_value) if dose_value else None,
            dose_unit=dose_unit,
            regulatory_code=regulatory_code,
            requires_prescription=requires_rx,
            indications=None,
            contraindications=None,
            side_effects=None,
            dosage_instructions=None,
            pregnancy_category=None,
            image_url=None,
        )
        medicines.append(medicine)

        for allergen_name, allergen_code in infer_allergens(display_name):
            if allergen_code not in allergens:
                allergen_id = stable_uuid(namespace, f"allergen:{allergen_code}")
                allergens[allergen_code] = Allergen(
                    id=allergen_id,
                    name=allergen_name,
                    code=allergen_code,
                    description=None,
                    allergen_type="DRUG"
                )
            allergen_id = allergens[allergen_code].id
            medicine_allergens.append(MedicineAllergen(
                id=stable_uuid(namespace, f"medallergen:{med_id}:{allergen_id}"),
                medicine_id=med_id,
                allergen_id=allergen_id
            ))

    return categories, dosage_forms, allergens, medicines, medicine_allergens


def write_json(medicines):
    out_path = os.path.join(OUT_DIR, "medicines.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump([asdict(m) for m in medicines], f, indent=2)


def sql_escape(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    return "'" + str(value).replace("'", "''") + "'"


def write_sql(categories, dosage_forms, allergens, medicines, medicine_allergens):
    with open(os.path.join(OUT_DIR, "categories.sql"), "w", encoding="utf-8") as f:
        for cat in categories.values():
            f.write(
                "INSERT INTO categories (id, code, name, therapeutic_section) VALUES (" +
                ", ".join([sql_escape(cat.id), sql_escape(cat.code), sql_escape(cat.name), sql_escape(cat.therapeutic_section)]) +
                ");\n"
            )

    with open(os.path.join(OUT_DIR, "dosage_forms.sql"), "w", encoding="utf-8") as f:
        for name, df_id in dosage_forms.items():
            f.write(
                "INSERT INTO dosage_forms (id, name) VALUES (" +
                ", ".join([sql_escape(df_id), sql_escape(name)]) +
                ");\n"
            )

    with open(os.path.join(OUT_DIR, "allergens.sql"), "w", encoding="utf-8") as f:
        for allergen in allergens.values():
            f.write(
                "INSERT INTO allergens (id, name, code, description, allergen_type) VALUES (" +
                ", ".join([
                    sql_escape(allergen.id),
                    sql_escape(allergen.name),
                    sql_escape(allergen.code),
                    sql_escape(allergen.description),
                    sql_escape(allergen.allergen_type)
                ]) +
                ");\n"
            )

    with open(os.path.join(OUT_DIR, "medicines.sql"), "w", encoding="utf-8") as f:
        for med in medicines:
            f.write(
                "INSERT INTO medicines (id, name, generic_name, category_id, dosage_form_id, therapeutic_class, schedule, need_manual_review, dose_value, dose_unit, regulatory_code, requires_prescription, indications, contraindications, side_effects, dosage_instructions, pregnancy_category, image_url) VALUES (" +
                ", ".join([
                    sql_escape(med.id),
                    sql_escape(med.name),
                    sql_escape(med.generic_name),
                    sql_escape(med.category_id),
                    sql_escape(med.dosage_form_id),
                    sql_escape(med.therapeutic_class),
                    sql_escape(med.schedule),
                    sql_escape(med.need_manual_review),
                    sql_escape(med.dose_value),
                    sql_escape(med.dose_unit),
                    sql_escape(med.regulatory_code),
                    sql_escape(med.requires_prescription),
                    sql_escape(med.indications),
                    sql_escape(med.contraindications),
                    sql_escape(med.side_effects),
                    sql_escape(med.dosage_instructions),
                    sql_escape(med.pregnancy_category),
                    sql_escape(med.image_url)
                ]) +
                ");\n"
            )

    with open(os.path.join(OUT_DIR, "medicine_allergens.sql"), "w", encoding="utf-8") as f:
        for ma in medicine_allergens:
            f.write(
                "INSERT INTO medicine_allergens (id, medicine_id, allergen_id) VALUES (" +
                ", ".join([sql_escape(ma.id), sql_escape(ma.medicine_id), sql_escape(ma.allergen_id)]) +
                ");\n"
            )


def main():
    if not os.path.exists(PDF_PATH):
        print("PDF not found:", PDF_PATH)
        sys.exit(1)

    text = load_pdf_text(PDF_PATH)
    lines = parse_lines(text)
    categories, dosage_forms, allergens, medicines, medicine_allergens = extract_medicines(lines)

    write_json(medicines)
    write_sql(categories, dosage_forms, allergens, medicines, medicine_allergens)

    summary = {
        "medicines_extracted": len(medicines),
        "categories_extracted": len(categories),
        "dosage_forms_extracted": len(dosage_forms),
        "allergens_inferred": len(allergens),
        "manual_review_rows": sum(1 for m in medicines if m.need_manual_review)
    }

    with open(os.path.join(OUT_DIR, "summary.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, indent=2)

    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
