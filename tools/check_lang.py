"""Checks that translation keys a NeoForge 26.1 mod uses exist in its lang files, and lists entries of non-English
files that are still identical to en_us.

Keys collected:
  - Component.translatable("...") literals in Java;
  - "item.<ns>.<id>" / "block.<ns>.<id>" for registered items and blocks (same regexes as check_assets.py);
  - "description": {"translate": ...} in data/<ns>/enchantment/*.json;
  - "itemGroup.<ns>" when the sources mention CreativeModeTab;
  - with --thaumaturge: research entry names and stage texts, aspect.<ns>.<tag>(.desc), research_category.<ns>.<id>,
    spell.<ns>.<id>.name/.desc.
  - --extra key ... adds keys produced at runtime (e.g. wand.thaumaturge.rod.<path>);
  - --ignore key ... drops keys known to be unused (e.g. a block item using the block's description id).

Usage: python -I check_lang.py <project-dir> <modid> [--langs en_us ru_ru] [--thaumaturge] [--extra k ...] [--ignore k ...]
Exit code 1 when a key is missing.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ap = argparse.ArgumentParser()
ap.add_argument("project")
ap.add_argument("modid")
ap.add_argument("--langs", nargs="+", default=["en_us"])
ap.add_argument("--thaumaturge", action="store_true")
ap.add_argument("--extra", nargs="*", default=[])
ap.add_argument("--ignore", nargs="*", default=[])
args = ap.parse_args()

ROOT = Path(args.project)
NS = args.modid
SRC = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"
LANG = RES / "assets" / NS / "lang"
DATA = RES / "data" / NS

keys = set(args.extra)
uses_tab = False
for java in SRC.rglob("*.java"):
    text = java.read_text(encoding="utf-8")
    keys |= set(re.findall(r'translatable(?:WithFallback)?\(\s*"([A-Za-z0-9_.-]+)"', text))
    for item in re.findall(r'ITEMS\.register(?:Simple)?Item\(\s*"([a-z0-9_/]+)"', text) + re.findall(r'ITEMS\.register\(\s*"([a-z0-9_/]+)"', text) + re.findall(r'relic\(\s*"([a-z0-9_/]+)"', text):
        keys.add(f"item.{NS}.{item}")
    for block in re.findall(r'BLOCKS\.register(?:Simple)?Block\(\s*"([a-z0-9_/]+)"', text) + re.findall(r'BLOCKS\.register\(\s*"([a-z0-9_/]+)"', text):
        keys.add(f"block.{NS}.{block}")
    uses_tab |= "CreativeModeTab" in text
if uses_tab:
    keys.add(f"itemGroup.{NS}")

for f in (DATA / "enchantment").glob("*.json"):
    desc = json.loads(f.read_text(encoding="utf-8")).get("description")
    if isinstance(desc, dict) and "translate" in desc:
        keys.add(desc["translate"])

if args.thaumaturge:
    tt = DATA / "thaumaturge"
    for f in (tt / "research_entry").glob("*.json"):
        entry = json.loads(f.read_text(encoding="utf-8"))
        keys.add(entry["name"])
        keys |= {stage["text"] for stage in entry.get("stages", []) if "text" in stage}
    for f in (tt / "aspect").glob("*.json"):
        keys |= {f"aspect.{NS}.{f.stem}", f"aspect.{NS}.{f.stem}.desc"}
    for f in (tt / "research_category").glob("*.json"):
        keys.add(f"research_category.{NS}.{f.stem}")
    for f in (tt / "spell_part").glob("*.json"):
        keys |= {f"spell.{NS}.{f.stem}.name", f"spell.{NS}.{f.stem}.desc"}

keys -= set(args.ignore)
keys = {k for k in keys if not k.endswith(".")}  # prefixes built in code, e.g. "tooltip.ns." + suffix

failed = False
langs = {}
for code in args.langs:
    file = LANG / f"{code}.json"
    if not file.exists():
        print(f"{code}: no {file}")
        failed = True
        continue
    langs[code] = json.loads(file.read_text(encoding="utf-8"))
    missing = sorted(keys - langs[code].keys())
    if missing:
        failed = True
        print(f"{code}: {len(missing)} missing")
        for k in missing:
            print("   ", k)
    else:
        print(f"{code}: all {len(keys)} used keys present")

base = langs.get("en_us", {})
for code, lang in langs.items():
    if code == "en_us":
        continue
    same = sorted(k for k, v in lang.items() if base.get(k) == v and re.search(r"[A-Za-z]{4}", v))
    print(f"{code}: {len(same)} entries identical to en_us (untranslated or proper names)")
    for k in same:
        print("   ", k)
sys.exit(1 if failed else 0)
