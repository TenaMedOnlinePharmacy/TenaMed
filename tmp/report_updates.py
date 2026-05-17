import json
from pathlib import Path

BASE = Path('docs') / 'lmcp_extraction'
master = json.loads((BASE / 'medicines.json').read_text(encoding='utf-8'))
master_by_id = {m['id']: m for m in master}
fields = ['indications','contraindications','side_effects','dosage_instructions','pregnancy_category','image_url']
updated = []
for b in range(4,11):
    bf = Path('tmp') / f'batch{b}.json'
    if not bf.exists():
        continue
    batch = json.loads(bf.read_text(encoding='utf-8'))
    for it in batch:
        mid = it['id']
        m = master_by_id.get(mid)
        if not m:
            continue
        changed = []
        for f in fields:
            if it.get(f) is None and m.get(f) is not None:
                changed.append(f)
        if changed:
            updated.append({'id': mid, 'name': m.get('name'), 'fields_filled': changed})

print(json.dumps(updated, indent=2, ensure_ascii=False))
