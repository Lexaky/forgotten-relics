"""Generate Thaumaturge data for Forgotten Relics from tools/data/original_research.json.

Writes: research category, 28 research entries, scan triggers for hidden entries, 26 infusion recipes, relic item
aspects (data map thaumaturge:base_aspects) and research lang (tools/data/research_lang_<lang>.json, merged into the
lang files by gen_assets.py). Every item id used is checked against the Thaumaturge, Botania Revived and Minecraft
jars, every TT research id against the Thaumaturge jar, so a typo fails the run instead of a recipe at load time.

Order: python -I tools/extract_original.py; python -I tools/gen_data.py; python -I tools/gen_assets.py
"""
import json
import math
import re
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NS = "forgottenrelics"
TT = "thaumaturge"
CATEGORY = f"{NS}:{NS}"
DATA = ROOT / "src/main/resources/data"
ORIG_LANG = ROOT / "reference/original/src/main/resources/assets/forgottenrelics/lang"
SRC = json.loads((ROOT / "tools/data/original_research.json").read_text(encoding="utf-8"))
GRADLE = Path.home() / ".gradle/caches/modules-2/files-2.1"

# ---------------------------------------------------------------------------------------------------------------
# Mapping tables (PORTING_PLAN.md 3.2-3.4)
# ---------------------------------------------------------------------------------------------------------------
ASPECTS = {
    "AIR": "aer", "WATER": "aqua", "FIRE": "ignis", "EARTH": "terra", "ORDER": "ordo", "ENTROPY": "perditio",
    "DARKNESS": "tenebrae", "ELDRITCH": "alienis", "EXCHANGE": "permutatio", "TOOL": "instrumentum", "MOTION": "motus",
    "MAN": "humanus", "AURA": "auram", "MAGIC": "praecantatio", "MIND": "cognitio", "VOID": "vacuos", "LIFE": "victus",
    "FLIGHT": "volatus", "TRAP": "vinculum", "ENERGY": "potentia", "LIGHT": "lux", "SOUL": "spiritus", "DEATH": "mortuus",
    "SENSES": "sensus", "CRYSTAL": "vitreus", "METAL": "metallum", "TAINT": "vitium", "CRAFT": "fabrico",
    # TC4 aspects TT (TC6) no longer has
    "TRAVEL": "motus", "WEATHER": "aer", "MINE": "instrumentum", "ARMOR": "praemunio", "HEAL": "victus",
    "WEAPON": "aversio", "GREED": "desiderium",
}
# TC4 item aspects are scaled x2 to the TC6 scale (same rule as the Forbidden Magic port).
ITEM_ASPECT_SCALE = 2


def crystal(aspect):
    return {"neoforge:ingredient_type": "neoforge:components", "items": f"{TT}:essentia_crystal",
            "components": {f"{TT}:crystal_aspect": {"aspect": f"{TT}:{aspect}"}}}


def potion(p):
    return {"neoforge:ingredient_type": "neoforge:components", "items": "minecraft:potion",
            "components": {"minecraft:potion_contents": {"potion": f"minecraft:{p}"}}}


# local ItemStack variable of RelicsResearchRegistry -> ingredient
INGREDIENTS = {
    "enderEye": "minecraft:ender_eye", "salisMundus": f"{TT}:salis_mundus", "voidSeed": f"{TT}:void_seed",
    "enderAir": "botania:ender_air_bottle", "primalCharm": f"{TT}:primal_charm", "blankRing": f"{TT}:ring_mundane",
    "ghastTear": "minecraft:ghast_tear", "airRune": "botania:rune_air",
    "airShard": crystal("aer"), "fireShard": crystal("ignis"), "waterShard": crystal("aqua"),
    "earthShard": crystal("terra"), "orderShard": crystal("ordo"), "entropyShard": crystal("perditio"),
    "knowledgeFragment": f"{TT}:curio_knowledge", "arcaneStone": f"{TT}:stone_arcane",
    "gaiaSpirit": "botania:life_essence", "terrasteelIngot": "botania:terrasteel_ingot",
    "nitor": f"{TT}:nitor_yellow", "elementalPickaxe": f"{TT}:elemental_pickaxe",
    "speedPotionII": potion("strong_swiftness"), "reachRing": "botania:reach_ring", "dragonStone": "botania:dragonstone",
    "portableHole": f"{TT}:focus_2", "handMirror": f"{TT}:hand_mirror", "glowstone": "minecraft:glowstone_dust",
    "etherealEssence": crystal("auram"), "elementiumIngot": "botania:elementium_ingot",
    "voidPickaxe": f"{TT}:void_pickaxe", "kineticRunicGirdle": f"{TT}:girdle_fancy",
    "tectonicGirdle": "botania:knockback_belt", "healPotionII": potion("strong_healing"),
    "pixieDust": "botania:pixie_dust", "enchantedFabric": f"{TT}:fabric", "goldIngot": "minecraft:gold_ingot",
    "blazePowder": "minecraft:blaze_powder", "blazeRod": "minecraft:blaze_rod",
    "runicRingCharged": f"{TT}:ring_fancy", "cinderPearl": f"{TT}:cinderpearl",
    "superLavaPendant": "botania:super_lava_pendant", "runicAmuletAdv": f"{TT}:amulet_fancy",
    "lavaPendant": "botania:lava_pendant", "voidIngot": f"{TT}:ingot_void", "alumentum": f"{TT}:alumentum",
    "voidSword": f"{TT}:void_sword", "primordialPearl": f"{TT}:primordial_pearl",
    "superGoldenApple": "minecraft:enchanted_golden_apple", "bloodPendant": "botania:blood_pendant",
    "netherStar": "minecraft:nether_star", "eldritchEye": f"{TT}:eldritch_eye", "writableBook": "minecraft:writable_book",
    "amber": f"{TT}:amber", "greatVisAmulet": f"{TT}:amulet_vis_crafted", "thaumiumIngot": f"{TT}:ingot_thaumium",
    "enderPearl": "minecraft:ender_pearl", "shockFocus": f"{TT}:focus_1", "gravityRod": "botania:gravity_rod",
    "pechFocus": f"{TT}:pech_wand", "jarredBrain": f"{TT}:jar_brain", "goldLaurel": f"{TT}:charm_undying",
    "fireball": "minecraft:fire_charge", "bottledTaint": f"{TT}:bottle_taint", "primalFocus": f"{TT}:focus_3",
    "redstone": "minecraft:redstone", "crimsonRites": f"{TT}:curio_rites", "starSword": "botania:star_sword",
    "missileRod": "botania:missile_rod", "kingKey": "botania:king_key", "emerald": "minecraft:emerald",
    "elementiumNugget": "botania:elementium_nugget", "elementiumBlock": "botania:elementium_block",
    "dragonstoneBlock": "botania:dragonstone_block", "dreamwood": "botania:dreamwood",
    "eldritchTablet": f"{TT}:runed_tablet",
}

# original item field (Main.itemX) -> new id
RELICS = {
    "itemFalseJustice": "false_justice", "itemDeificAmulet": "deific_amulet", "itemParadox": "paradox",
    "itemOblivionAmulet": "oblivion_amulet", "itemArcanum": "nebulous_core", "itemDormantArcanum": "nebulous_core_dormant",
    "itemFateTome": "fate_tome", "itemDarkSunRing": "dark_sun_ring", "itemChaosCore": "chaos_core",
    "itemMiningCharm": "mining_charm", "itemAdvancedMiningCharm": "advanced_mining_charm",
    "itemTelekinesisTome": "telekinesis_tome", "itemAncientAegis": "ancient_aegis", "itemMissileTome": "missile_tome",
    "itemCrimsonSpell": "crimson_spell", "itemGhastlySkull": "ghastly_skull", "itemObeliskDrainer": "obelisk_drainer",
    "itemEldritchSpell": "eldritch_spell", "itemLunarFlares": "lunar_flares",
    "itemTeleportationTome": "teleportation_tome", "itemShinyStone": "shiny_stone",
    "itemSuperpositionRing": "superposition_ring", "itemSoulTome": "soul_tome",
    "itemDimensionalMirror": "dimensional_mirror", "itemWeatherStone": "weather_stone", "itemApotheosis": "apotheosis",
    "itemChaosTome": "chaos_tome", "itemOmegaCore": "omega_core", "itemXPTome": "xp_tome",
}

# TC4 research key -> TT research entry
TC4_RESEARCH = {
    "INFUSION": "infusion", "THAUMIUM": "metallurgy", "ELDRITCHMINOR": "base_eldritch",
    "ELDRITCHMAJOR": "outer_revelations", "ELEMENTALPICK": "elemental_tools", "MIRRORHAND": "mirror_hand",
    "FOCUSPORTABLEHOLE": "focus_rift", "FOCUSSHOCK": "focus_bolt", "RUNICKINETIC": "runic_shielding",
    "RUNICCHARGED": "runic_shielding", "RUNICEMERGENCY": "runic_shielding", "ENCHFABRIC": "unlock_infusion",
    "JARBRAIN": "jar_brain", "NITOR": "unlock_alchemy", "OCULUS": "oculus", "CRIMSON": "crimson_rites",
    "ROD_primal_staff": "staff_primal", "PRIMPEARL": "primordial_pearl", "VOIDMETAL": "base_eldritch",
    "ALUMENTUM": "alumentum", "VISAMULET": "vis_amulet",
}
# Research (and its recipe) that a config option can remove: original falseJusticeEnabled.
CONDITIONAL = {"FalseJustice": [{"type": f"{NS}:false_justice_enabled"}]}

# Typos in the original's parent lists -> intended key.
KEY_FIXES = {"NebulosCore": "NebulousCore"}
# Untranslated strings left in the original ru_RU.lang (research title of the Ethereal Mining Charm)
RU_FIXES = {"fr.name.AdvancedMiningCharm": "Эфирный шахтёрский талисман"}


def snake(key):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", key).lower()


# research key -> entry id (Discord Tome / Nuclear Fury / The Paradox keep their research names)
ENTRY_IDS = {r["key"]: snake(r["key"]) for r in SRC["research"]}
ENTRY_IDS["XPTome"] = "xp_tome"

# ---------------------------------------------------------------------------------------------------------------
# Validation against the real jars
# ---------------------------------------------------------------------------------------------------------------


def jar_entries(pattern):
    jars = sorted(GRADLE.glob(pattern))
    if not jars:
        sys.exit(f"jar not found: {pattern} (run gradlew compileJava first)")
    return zipfile.ZipFile(jars[-1]).namelist()


def known_items():
    ids = set()
    for pattern, ns in (("curse.maven/thaumaturge-1628024/*/*/*.jar", TT),
                        ("curse.maven/botania-revived-1725641/*/*/*.jar", "botania")):
        for name in jar_entries(pattern):
            m = re.fullmatch(rf"assets/{ns}/items/([a-z0-9_/]+)\.json", name)
            if m:
                ids.add(f"{ns}:{m.group(1)}")
    mc = ROOT / "build/moddev/artifacts/minecraft-patched-26.1.2.122.jar"
    for name in zipfile.ZipFile(mc).namelist():
        m = re.fullmatch(r"assets/minecraft/items/([a-z0-9_]+)\.json", name)
        if m:
            ids.add(f"minecraft:{m.group(1)}")
    return ids


def known_tt_research():
    return {m.group(1) for name in jar_entries("curse.maven/thaumaturge-1628024/*/*/*.jar")
            if (m := re.fullmatch(r"data/thaumaturge/thaumaturge/research_entry/([a-z0-9_]+)\.json", name))}


ITEM_IDS = known_items()
TT_RESEARCH = known_tt_research()
ERRORS = []


def item(id_):
    if id_ not in ITEM_IDS and not id_.startswith(f"{NS}:"):
        ERRORS.append(f"unknown item {id_}")
    return id_


def ingredient(expr):
    """Resolve a stack variable or 'new ItemStack(Main.itemX)' expression."""
    m = re.search(r"Main\.(\w+)", expr)
    if m:
        return f"{NS}:{RELICS[m.group(1)]}"
    if expr in RELIC_VARS:
        return f"{NS}:{RELIC_VARS[expr]}"
    if expr not in INGREDIENTS:
        ERRORS.append(f"no ingredient mapping for {expr}")
        return "minecraft:barrier"
    ing = INGREDIENTS[expr]
    if isinstance(ing, str):
        item(ing)
    else:
        item(ing["items"])
    return ing


# stack variables that hold relics (superpositionRing, chaosTome, ...)
RELIC_VARS = {var: RELICS[m.group(1)] for var, expr in SRC["stacks"].items()
              if (m := re.search(r"Main\.(\w+)", expr))}


def aspect_list(tc4, scale=1):
    merged = {}
    for name, amount in tc4.items():
        if name not in ASPECTS:
            ERRORS.append(f"no aspect mapping for {name}")
            continue
        merged[ASPECTS[name]] = merged.get(ASPECTS[name], 0) + amount * scale
    return [{"aspect": f"{TT}:{a}", "amount": n} for a, n in merged.items()]


def note_aspects(tc4):
    """TC4 research aspects -> TT note aspects. TT notes use at most 5 aspects totalling at most 36 (checked over
    all 202 TT entries), so keep the 5 largest and scale the total down proportionally when it exceeds 36."""
    notes = sorted(aspect_list(tc4), key=lambda a: -a["amount"])[:5]
    total = sum(a["amount"] for a in notes)
    if total > 36:
        for a in notes:
            a["amount"] = max(1, round(a["amount"] * 36 / total))
    return notes


def instability(tc4):
    """TC4 instability 1..48 -> TT 1..10, keeping the order: round(2 + tc4 / 6), capped at 10."""
    return max(1, min(10, round(2 + tc4 / 6)))


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def read_lang(path):
    out = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v
    return out


def page_text(text):
    """TC4 page markup to Thaumaturge's: <BR>, <LINE> (TC4 wrote <LINE/>). The Oblivion Amulet poem was laid out with
    runs of spaces that relied on TC4's page width; they become line breaks."""
    text = text.replace("<BR/>", "<BR>").replace("<br/>", "<BR>").replace("<LINE/>", "<LINE>")
    def brk(m):
        # Thaumaturge resets formatting at <BR>; carry the last active code (the poem is italic) onto the new line
        codes = re.findall("§[0-9a-fk-or]", text[:m.start()])
        return "<BR>" + (codes[-1] if codes and codes[-1] != "§r" else "")
    return re.sub(r" {3,}", brk, text).replace("<BR>§r", "§r")


# ---------------------------------------------------------------------------------------------------------------
# Generation
# ---------------------------------------------------------------------------------------------------------------


def main():
    for sub in ("thaumaturge/research_entry", "thaumaturge/research_category", "thaumaturge/scan", "recipe/infusion"):
        shutil.rmtree(DATA / NS / sub, ignore_errors=True)

    # recipes
    recipe_of_research = {}
    for rkey, r in SRC["recipes"].items():
        out_id = ingredient(r["output"])
        entry = ENTRY_IDS[r["research"]]
        name = out_id.split(":")[1]
        write(DATA / NS / "recipe/infusion" / f"{name}.json", {
            **({"neoforge:conditions": CONDITIONAL[r["research"]]} if r["research"] in CONDITIONAL else {}),
            "type": f"{TT}:infusion",
            "catalyst": ingredient(r["center"]),
            "instability": instability(r["instability"]),
            "aspects": aspect_list(r["aspects"]),
            "components": [ingredient(c) for c in r["components"]],
            "research": {"entry": f"{NS}:{entry}"},
            "result": {"id": out_id},
        })
        recipe_of_research.setdefault(r["research"], []).append(f"{NS}:infusion/{name}")

    # research entries
    en, ru = read_lang(ORIG_LANG / "en_US.lang"), read_lang(ORIG_LANG / "ru_RU.lang")
    lang = {"en": {}, "ru": {}}
    formula = {}
    for r in SRC["research"]:
        key, eid = r["key"], ENTRY_IDS[r["key"]]
        parents = [f"{NS}:{ENTRY_IDS[p]}" for p in r["parents"]]
        for p in r["parents_hidden"]:
            p = KEY_FIXES.get(p, p)
            if p in ENTRY_IDS:
                parents.append(f"~{NS}:{ENTRY_IDS[p]}")
            elif p in TC4_RESEARCH:
                tt = TC4_RESEARCH[p]
                if tt not in TT_RESEARCH:
                    ERRORS.append(f"TT research {tt} does not exist")
                if f"~{TT}:{tt}" not in parents:
                    parents.append(f"~{TT}:{tt}")
            else:
                ERRORS.append(f"{key}: unknown hidden parent {p}")
        meta = []
        if "Round" in r["flags"]:
            meta.append("round")
        if "Special" in r["flags"]:
            meta.append("spiky")
        if "Hidden" in r["flags"]:
            meta.append("hidden")
        if r["item_triggers"]:
            scan_key = f"{NS}:scanned/{eid}"
            items = []
            for t in r["item_triggers"]:
                ing = ingredient(t)
                items.append(ing if isinstance(ing, str) else ing["items"])
            write(DATA / NS / "thaumaturge/scan" / f"{eid}.json", {"items": sorted(set(items)), "key": scan_key})
            parents.append(scan_key)

        complexity = max(1, min(3, r["complexity"]))
        first = {"text": f"research.{NS}.{eid}.stage_0",
                 "required_knowledge": [{"type": "theory" if complexity >= 2 else "observation",
                                         "category": CATEGORY, "amount": 1}]}
        last = {"text": f"research.{NS}.{eid}.stage_1"}
        if key in recipe_of_research:
            last["recipes"] = recipe_of_research[key]
        warp = SRC["warp_research"].get(key)
        if warp:
            last["warp"] = warp
        icon_m = re.search(r"Main\.(\w+)", r["icon"])
        icon = f"{NS}:{RELICS[icon_m.group(1)]}" if icon_m else ("minecraft:writable_book" if "writable_book" in r["icon"]
                                                                 else ingredient(r["icon"]))
        entry = {"category": CATEGORY, "name": f"research.{NS}.{eid}.title", "column": r["col"], "row": r["row"],
                 "icons": [icon], "stages": [first, last], "note_aspects": note_aspects(r["aspects"]),
                 "complexity": complexity}
        if parents:
            entry["parents"] = parents
        if meta:
            entry["meta"] = meta
        if key in CONDITIONAL:
            entry = {"neoforge:conditions": CONDITIONAL[key], **entry}
        write(DATA / NS / "thaumaturge/research_entry" / f"{eid}.json", entry)

        for a in entry["note_aspects"]:
            formula[a["aspect"]] = formula.get(a["aspect"], 0) + a["amount"]
        for code, src in (("en", en), ("ru", ru)):
            pages = sorted((k for k in src if re.fullmatch(rf"fr\.text\.{key}\.\d+", k)), key=lambda k: int(k.rsplit(".", 1)[1]))
            if not pages:
                pages = sorted((k for k in en if re.fullmatch(rf"fr\.text\.{key}\.\d+", k)), key=lambda k: int(k.rsplit(".", 1)[1]))
                src = en
            texts = [page_text(src[k]) for k in pages]
            title = (RU_FIXES.get(f"fr.name.{key}") if code == "ru" else None) or src.get(f"fr.name.{key}") or en.get(f"fr.name.{key}", key)
            tag = src.get(f"fr.tag.{key}") or en.get(f"fr.tag.{key}", "")
            lang[code][f"research.{NS}.{eid}.title"] = title
            lang[code][f"research.{NS}.{eid}.stage_0"] = tag or (texts[0] if texts else title)
            lang[code][f"research.{NS}.{eid}.stage_1"] = "<PAGE>".join(texts) if texts else title

    # category: the original drew on Thaumcraft's default background; formula = the most used research aspects
    top = sorted(formula.items(), key=lambda kv: -kv[1])[:8]
    weights = [20, 10, 10, 5, 5, 5, 5, 5]
    write(DATA / NS / "thaumaturge/research_category" / f"{NS}.json", {
        "background": f"{TT}:textures/gui/gui_research_back_1_smooth.png",
        "overlay_background": f"{TT}:textures/gui/gui_research_back_over.png",
        "icon": f"{NS}:textures/item/ghastly_skull.png",
        "formula": [{"aspect": a, "amount": w} for (a, _), w in zip(top, weights)],
        "index": 11,
        "required_research": f"{TT}:unlock_infusion",
    })
    for code, src in (("en", en), ("ru", ru)):
        lang[code][f"research_category.{NS}.{NS}"] = src.get("tc.research_category.ForgottenRelics", "Forgotten Relics")

    # relic item aspects (TT reads data maps from the thaumaturge namespace)
    values = {}
    for field, tc4 in SRC["item_aspects"].items():
        values[f"{NS}:{RELICS[field]}"] = aspect_list(tc4, ITEM_ASPECT_SCALE)
    write(DATA / TT / "data_maps/item/base_aspects.json", {"values": dict(sorted(values.items()))})

    for code in ("en", "ru"):
        write(ROOT / f"tools/data/research_lang_{code}.json", dict(sorted(lang[code].items())))

    if ERRORS:
        print("\n".join(sorted(set(ERRORS))))
        sys.exit(1)
    print(f"{len(SRC['research'])} research entries, {len(SRC['recipes'])} infusion recipes, "
          f"{sum(1 for r in SRC['research'] if r['item_triggers'])} scans, {len(values)} item aspect entries")


if __name__ == "__main__":
    main()
