"""Checks client assets of a NeoForge 26.1 mod: every registered item and block has an item model definition
(assets/<ns>/items/<id>.json), every blockstate/model/parent reference of the mod's namespace resolves, and every
referenced texture is a real PNG (square unless animated or an entity/model sheet; animated height multiple of width).
Also checks textures referenced as "<ns>:textures/....png" anywhere in data/ JSON.

Registered ids are found in Java sources by regex on DeferredRegister calls:
  ITEMS.registerItem("id"  ITEMS.registerSimpleItem("id"  ITEMS.register("id"  BLOCKS.registerBlock("id"
  BLOCKS.register("id"  BLOCKS.registerSimpleBlock("id"
Every block is assumed to have a block item (pass --no-block-items otherwise).

Usage: python -I check_assets.py <project-dir> <modid> [--no-block-items] [--extra-texture ns:path ...]
Exit code 1 on any problem.
"""
import argparse
import json
import re
import struct
import sys
from pathlib import Path

ap = argparse.ArgumentParser()
ap.add_argument("project")
ap.add_argument("modid")
ap.add_argument("--no-block-items", action="store_true")
ap.add_argument("--extra-texture", nargs="*", default=[], help="texture refs used from code, e.g. ns:models/wand_rod_x")
args = ap.parse_args()

ROOT = Path(args.project)
NS = args.modid
RES = ROOT / "src/main/resources"
ASSETS_ROOT = RES / "assets"
ASSETS = ASSETS_ROOT / NS
problems = []


def split(ref):
    return ref.split(":", 1) if ":" in ref else ("minecraft", ref)


def png_size(path):
    data = path.read_bytes()[:24]
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", data[16:24])


def check_texture(ref, where):
    ns, path = split(ref)
    if ns != NS:
        return
    file = ASSETS_ROOT / ns / "textures" / f"{path}.png"
    if not file.exists():
        problems.append(f"{where}: missing texture {ref}")
        return
    size = png_size(file)
    if size is None:
        problems.append(f"{where}: {ref} is not a PNG")
        return
    w, h = size
    meta = file.with_name(file.name + ".mcmeta")
    if meta.exists():
        try:
            json.loads(meta.read_text(encoding="utf-8"))
        except ValueError as e:
            problems.append(f"{where}: bad mcmeta for {ref}: {e}")
        if h % w:
            problems.append(f"{where}: animated {ref} is {w}x{h}, height not a multiple of width")
    elif w != h and not re.search(r"/(models|entity|gui|research|environment|misc)/", "/" + path):
        problems.append(f"{where}: {ref} is {w}x{h}, not square and not animated")


checked_models = set()


def check_model(ref, where):
    ns, path = split(ref)
    if ns != NS or ref in checked_models:
        return
    checked_models.add(ref)
    file = ASSETS_ROOT / ns / "models" / f"{path}.json"
    if not file.exists():
        problems.append(f"{where}: missing model {ref}")
        return
    try:
        model = json.loads(file.read_text(encoding="utf-8"))
    except ValueError as e:
        problems.append(f"{ref}: invalid JSON: {e}")
        return
    if "parent" in model:
        check_model(model["parent"], ref)
    for key, tex in model.get("textures", {}).items():
        tex = tex["sprite"] if isinstance(tex, dict) else tex
        if not tex.startswith("#"):
            check_texture(tex, f"{ref}#{key}")


def walk(node, where):
    """Finds every model reference in an item model definition or blockstate (variants and multipart)."""
    if isinstance(node, dict):
        if isinstance(node.get("model"), str):
            check_model(node["model"], where)
        for value in node.values():
            walk(value, where)
    elif isinstance(node, list):
        for value in node:
            walk(value, where)


items, blocks = set(), set()
for java in (ROOT / "src/main/java").rglob("*.java"):
    text = java.read_text(encoding="utf-8")
    items |= set(re.findall(r'ITEMS\.register(?:Simple)?Item\(\s*"([a-z0-9_/]+)"', text))
    items |= set(re.findall(r'ITEMS\.register\(\s*"([a-z0-9_/]+)"', text))
    blocks |= set(re.findall(r'BLOCKS\.register(?:Simple)?Block\(\s*"([a-z0-9_/]+)"', text))
    blocks |= set(re.findall(r'BLOCKS\.register\(\s*"([a-z0-9_/]+)"', text))
if not args.no_block_items:
    items |= blocks

for item in sorted(items):
    file = ASSETS / "items" / f"{item}.json"
    if not file.exists():
        problems.append(f"item {item}: no assets/{NS}/items/{item}.json (26.1 needs an item model definition)")
        continue
    walk(json.loads(file.read_text(encoding="utf-8")), f"items/{item}")

for block in sorted(blocks):
    file = ASSETS / "blockstates" / f"{block}.json"
    if not file.exists():
        problems.append(f"block {block}: no blockstate")
        continue
    walk(json.loads(file.read_text(encoding="utf-8")), f"blockstates/{block}")

for ref in args.extra_texture:
    check_texture(ref, "code")
pattern = re.compile(r'"' + re.escape(NS) + r':textures/([a-z0-9_/.-]+)\.png"')
for f in (RES / "data").rglob("*.json"):
    for ref in pattern.findall(f.read_text(encoding="utf-8")):
        check_texture(f"{NS}:{ref}", f.relative_to(RES).as_posix())

print(f"checked {len(items)} items, {len(blocks)} blocks, {len(checked_models)} models")
for p in problems:
    print("PROBLEM", p)
sys.exit(1 if problems else 0)
