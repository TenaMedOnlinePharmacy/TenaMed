import json
import re
import sys
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

HEADERS = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}


def slugify(name: str) -> str:
    s = name or ""
    s = s.strip()
    s = re.sub(r"\s*\(.*?\)", "", s)
    s = re.sub(r"[^0-9A-Za-z\s/+\-]+", "", s)
    s = s.replace(" ", "-")
    s = s.replace("/", "-")
    s = s.replace("+", "-plus")
    s = s.strip("-")
    return s.lower()


def fetch_drugsdotcom_variants(slug: str, timeout=6):
    candidates = [slug, slug + "-tablets", slug + "-oral", slug + "-syrup", slug + "-injection"]
    for c in candidates:
        url = f"https://www.drugs.com/{quote(c)}.html"
        try:
            r = requests.get(url, headers=HEADERS, timeout=timeout)
            if r.status_code == 200:
                return r.text, url, 200
        except Exception as e:
            last = str(e)
    # search fallback
    try:
        search_url = f"https://www.drugs.com/search.php?searchterm={quote(slug)}"
        s = requests.get(search_url, headers=HEADERS, timeout=timeout)
        if s.status_code == 200:
            m = re.search(r'href="(/[^"\s>]+\.html)"', s.text)
            if m:
                page = "https://www.drugs.com" + m.group(1)
                r = requests.get(page, headers=HEADERS, timeout=timeout)
                if r.status_code == 200:
                    return r.text, page, 200
            return None, search_url, 404
        return None, search_url, s.status_code
    except Exception as e:
        return None, f"https://www.drugs.com/{quote(slug)}.html", last if 'last' in locals() else str(e)


def extract_uses(html: str):
    if not html:
        return None
    m = re.search(r"<h2[^>]*>Uses</h2>(.*?)<h2", html, re.I | re.S)
    if m:
        text = re.sub(r"<[^>]+>", "", m.group(1)).strip()
        text = re.sub(r"\s+", " ", text)
        return text
    m = re.search(r"What is [^<]+ used for\?</h2>(.*?)<h2", html, re.I | re.S)
    if m:
        text = re.sub(r"<[^>]+>", "", m.group(1)).strip()
        text = re.sub(r"\s+", " ", text)
        return text
    return None


def run_batch(batch):
    data = json.loads(MEDICINES_JSON.read_text(encoding="utf-8"))
    by_id = {item['id']: item for item in data}
    batch_file = TMP_DIR / f"batch{batch}.json"
    if not batch_file.exists():
        print(f"Missing {batch_file}")
        return 0
    items = json.loads(batch_file.read_text(encoding="utf-8"))
    filled = 0
    for item in items:
        mid = item['id']
        name = item.get('generic_name') or item.get('name')
        slug = slugify(name)
        html, url, status = fetch_drugsdotcom_variants(slug, timeout=6)
        if html:
            uses = extract_uses(html)
            if uses and by_id[mid].get('indications') is None:
                by_id[mid]['indications'] = uses
                filled += 1
                print(f"Filled {mid} {name} from {url}")
        else:
            print(f"No page for {name} ({slug}) -> {status}")
        time.sleep(0.5)
    # write back
    new_data = [by_id[item['id']] for item in data]
    MEDICINES_JSON.write_text(json.dumps(new_data, indent=2, ensure_ascii=True)+"\n", encoding='utf-8')
    return filled


if __name__ == '__main__':
    which = 4
    if len(sys.argv) > 1:
        try:
            which = int(sys.argv[1])
        except:
            pass
    print(f"Running focused batch {which}")
    f = run_batch(which)
    print(f"Filled {f} items in batch {which}")
