import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
DATA_DIR = BASE_DIR / "docs" / "lmcp_extraction"
TMP_DIR = BASE_DIR / "tmp"

MEDICINES_JSON = DATA_DIR / "medicines.json"
NULLABLE_FIELDS = [
    "indications",
    "contraindications",
    "side_effects",
    "dosage_instructions",
    "pregnancy_category",
    "schedule",
]

meds = json.loads(MEDICINES_JSON.read_text(encoding='utf-8'))
by_id = {m['id']: m for m in meds}
report = {}
for batch in range(4,11):
    batch_file = TMP_DIR / f"batch{batch}.json"
    if not batch_file.exists():
        report[f"batch{batch}"] = {'missing': [], 'note': 'batch file missing'}
        continue
    items = json.loads(batch_file.read_text(encoding='utf-8'))
    missing = []
    for it in items:
        mid = it['id']
        m = by_id.get(mid, it)
        nulls = [f for f in NULLABLE_FIELDS if m.get(f) is None]
        if nulls:
            missing.append({'id': mid, 'name': m.get('generic_name') or m.get('name'), 'missing_fields': nulls})
    report[f"batch{batch}"] = {'missing_count': len(missing), 'missing': missing}

out = TMP_DIR / 'batches4_10_missing.json'
out.write_text(json.dumps(report, indent=2, ensure_ascii=True)+"\n", encoding='utf-8')
print('Wrote', out)
