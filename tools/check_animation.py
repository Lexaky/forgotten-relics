"""Checks that the Runic Stone icon is animated: frcheck_items.png and frcheck_items_later.png (10 ticks apart) must
differ, and only inside one item cell. Run after runClientCheck: python -I tools/check_animation.py"""
import pathlib
import sys

from PIL import Image, ImageChops

SHOTS = pathlib.Path(__file__).resolve().parent.parent / "run-check/screenshots"
a = Image.open(SHOTS / "frcheck_items.png").convert("RGB")
b = Image.open(SHOTS / "frcheck_items_later.png").convert("RGB")
box = ImageChops.difference(a, b).getbbox()
if box is None:
    sys.exit("no difference: the animated icon did not change between the frames")
w, h = box[2] - box[0], box[3] - box[1]
print(f"changed region {box} ({w}x{h} px)")
if w > 80 or h > 80:
    sys.exit("changes outside a single item cell")
print("ok: one animated icon")
