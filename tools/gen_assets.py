"""Generate client assets, lang files and damage-type data for Forgotten Relics.

Single table ITEMS maps the original registry name to the new id, texture and model parent. From it the script
writes item model definitions, item models, copies textures/sounds from reference/original and converts the
original .lang files to en_us.json / ru_ru.json. Damage types (data) live here too because their death messages
are lang entries. Idempotent: rerun after any table change.

Run: python -I tools/gen_assets.py
"""
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
NS = "forgottenrelics"
ORIG = ROOT / "reference/original/src/main/resources/assets/forgottenrelics"
ASSETS = ROOT / "src/main/resources/assets" / NS
DATA = ROOT / "src/main/resources/data"

# old registry/unlocalized name -> (new id, original texture, model parent)
ITEMS = {
    "ItemFalseJustice": ("false_justice", "False_Justice", "generated"),
    "ItemDeificAmulet": ("deific_amulet", "Deific_Amulet", "generated"),
    "ItemParadox": ("paradox", "Paradox", "handheld"),
    "ItemOblivionAmulet": ("oblivion_amulet", "Oblivion_Amulet", "generated"),
    "ItemArcanum": ("nebulous_core", "Nebulous_Core", "generated"),
    "ItemDormantArcanum": ("nebulous_core_dormant", "Nebulous_Core_Dormant", "generated"),
    "ItemFateTome": ("fate_tome", "Tome_of_Broken_Fates", "generated"),
    "ItemDarkSunRing": ("dark_sun_ring", "Dark_Sun_Ring", "generated"),
    "ItemChaosCore": ("chaos_core", "Chaos_Core", "generated"),
    "ItemMiningCharm": ("mining_charm", "Mining_Charm", "generated"),
    "ItemAdvancedMiningCharm": ("advanced_mining_charm", "Advanced_Mining_Charm", "generated"),
    "ItemTelekinesisTome": ("telekinesis_tome", "Telekinesis_Tome", "generated"),
    "ItemAncientAegis": ("ancient_aegis", "Ancient_Aegis", "generated"),
    "ItemMissileTome": ("missile_tome", "Missile_Tome", "generated"),
    "ItemCrimsonSpell": ("crimson_spell", "Crimson_Spell", "generated"),
    "ItemGhastlySkull": ("ghastly_skull", "Ghastly_Skull", "generated"),
    "ItemObeliskDrainer": ("obelisk_drainer", "Obelisk_Drainer", "generated"),
    "ItemEldritchSpell": ("eldritch_spell", "Eldritch_Spell", "generated"),
    "ItemLunarFlares": ("lunar_flares", "Lunar_Flares", "generated"),
    "ItemTeleportationTome": ("teleportation_tome", "Teleportation_Tome", "generated"),
    "ItemShinyStone": ("shiny_stone", "Shiny_Stone", "generated"),
    "ItemSuperpositionRing": ("superposition_ring", "Superposition_Ring", "generated"),
    "ItemSoulTome": ("soul_tome", "Soul_Tome", "generated"),
    "ItemDimensionalMirror": ("dimensional_mirror", "Dimensional_Mirror", "generated"),
    "ItemWeatherStone": ("weather_stone", "Weather_Stone", "generated"),
    "ItemApotheosis": ("apotheosis", "Apotheosis", "generated"),
    "ItemChaosTome": ("chaos_tome", "Chaos_Tome", "generated"),
    "ItemOmegaCore": ("omega_core", "Omega_Core", "generated"),
    "ItemXPTome": ("xp_tome", "XP_Tome", "generated"),
}

CURIO_SLOTS = {
    "ring": ["superposition_ring", "mining_charm", "advanced_mining_charm", "dark_sun_ring"],
    "necklace": ["deific_amulet", "shiny_stone", "oblivion_amulet", "nebulous_core", "nebulous_core_dormant"],
    "belt": ["ancient_aegis"],
}

# Names the original never translated.
EXTRA_NAMES = {
    "ghastly_skull": ("Ghastly Skull", "Призрачный череп"),
}

# old sound event -> (new id, file)
SOUNDS = {
    "discharge": ("discharge", "discharge"),
    "lunarFlare": ("lunar_flare", "lunarFlare"),
    "starfall": ("starfall", "starfall"),
}
SUBTITLES = {
    "discharge": ("Lightning discharge", "Разряд молнии"),
    "lunar_flare": ("Lunar flare falls", "Падает лунный огонь"),
    "starfall": ("Stars fall", "Звездопад"),
}

# Original DamageSource subclasses: msgId -> (new id, flags). Flags follow the 1.7 setters:
# magic -> neoforge:is_magic + witch resistance, absolute -> resistance/enchantment bypass, armor -> armour+shield bypass,
# creative -> hurts creative players.
DAMAGE_TYPES = {
    "trueDamage": ("true_damage", {"magic", "absolute", "armor", "creative"}),
    "trueDamageUndef": ("true_damage_undefined", {"magic", "absolute", "armor", "creative"}),
    "soulAttack": ("soul_drain", {"magic", "absolute", "armor"}),
    "attackOblivion": ("oblivion", {"magic"}),
    "attackFate": ("fate", {"magic", "absolute", "armor", "creative"}),
    "attackLightning": ("telekinetic_lightning", {"magic"}),
    "attackDarkMatter": ("dark_matter", {"magic", "armor"}),
}
FLAG_TAGS = {
    "magic": ["neoforge:is_magic", "minecraft:witch_resistant_to"],
    "absolute": ["minecraft:bypasses_resistance", "minecraft:bypasses_enchantments"],
    "armor": ["minecraft:bypasses_armor", "minecraft:bypasses_shield"],
    "creative": ["minecraft:bypasses_invulnerability"],
}
# Death messages the original lacked.
EXTRA_DEATH = {
    "soul_drain": ("%1$s's soul was drained", "Душа %1$s была иссушена"),
}


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def read_lang(path):
    out = {}
    for line in path.read_text(encoding="utf-8-sig").splitlines():
        if "=" not in line or line.lstrip().startswith("#"):
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v
    return out


def convert_lang(old):
    new = {}
    old_to_id = {k: v[0] for k, v in ITEMS.items()}
    for k, v in old.items():
        m = re.fullmatch(r"item\.(\w+)\.name", k)
        if m and m.group(1) in old_to_id:
            new[f"item.{NS}.{old_to_id[m.group(1)]}"] = v
            continue
        m = re.fullmatch(r"item\.(\w+)\.lore", k)
        if m:
            new[f"lore.{NS}.{m.group(1)}"] = v
            continue
        m = re.fullmatch(r"death\.attack\.(\w+)", k)
        if m and m.group(1) in DAMAGE_TYPES:
            new_id = DAMAGE_TYPES[m.group(1)][0]
            new[f"death.attack.{NS}.{new_id}"] = v
            new[f"death.attack.{NS}.{new_id}.player"] = v
            new[f"death.attack.{NS}.{new_id}.item"] = v
            continue
        if k == "itemGroup.tabForgottenRelics":
            new[f"itemGroup.{NS}"] = v
    return new


def main():
    # textures + models
    tex_dir = ASSETS / "textures/item"
    tex_dir.mkdir(parents=True, exist_ok=True)
    for old, (new_id, tex, parent) in ITEMS.items():
        src = ORIG / "textures/items" / f"{tex}.png"
        shutil.copyfile(src, tex_dir / f"{new_id}.png")
        meta = src.with_name(src.name + ".mcmeta")
        if meta.exists():
            shutil.copyfile(meta, tex_dir / f"{new_id}.png.mcmeta")
        write_json(ASSETS / "models/item" / f"{new_id}.json",
                   {"parent": f"minecraft:item/{parent}", "textures": {"layer0": f"{NS}:item/{new_id}"}})
        write_json(ASSETS / "items" / f"{new_id}.json",
                   {"model": {"type": "minecraft:model", "model": f"{NS}:item/{new_id}"}})

    # sounds (resource paths must be lower case now)
    snd_dir = ASSETS / "sounds"
    snd_dir.mkdir(parents=True, exist_ok=True)
    sounds_json = {}
    for old, (new_id, file) in SOUNDS.items():
        shutil.copyfile(ORIG / "sounds/specific" / f"{file}.ogg", snd_dir / f"{new_id}.ogg")
        sounds_json[new_id] = {"category": "player", "subtitle": f"subtitles.{NS}.{new_id}",
                               "sounds": [{"name": f"{NS}:{new_id}", "stream": False}]}
    write_json(ASSETS / "sounds.json", sounds_json)

    # damage types + tags
    tags = {}
    for msg, (new_id, flags) in DAMAGE_TYPES.items():
        write_json(DATA / NS / "damage_type" / f"{new_id}.json",
                   {"message_id": f"{NS}.{new_id}", "exhaustion": 0.0, "scaling": "never"})
        for f in sorted(flags):
            for tag in FLAG_TAGS[f]:
                tags.setdefault(tag, []).append(f"{NS}:{new_id}")
    for tag, values in tags.items():
        ns, path = tag.split(":")
        write_json(DATA / ns / "tags/damage_type" / f"{path}.json", {"replace": False, "values": sorted(values)})

    # Curios slots (Baubles RING/AMULET/BELT of the original)
    for slot, ids in CURIO_SLOTS.items():
        write_json(DATA / "curios/tags/item" / f"{slot}.json",
                   {"replace": False, "values": [f"{NS}:{i}" for i in ids]})
    # SuperpositionHandler.isDamageTypeAbsolute of the original
    write_json(DATA / NS / "tags/damage_type/absolute.json", {"replace": False, "values": [
        "minecraft:out_of_world", "minecraft:starve"] + [f"{NS}:{DAMAGE_TYPES[m][0]}" for m in (
            "attackFate", "attackOblivion", "soulAttack", "trueDamage", "trueDamageUndef")]})
    # EntityThaumcraftBoss / EntityDoppleganger checks of the original
    write_json(DATA / NS / "tags/entity_type/relic_bosses.json", {"replace": False, "values": [
        "#c:bosses", {"id": "botania:doppleganger", "required": False}] + [
        {"id": f"thaumaturge:{b}", "required": False} for b in (
            "cultist_leader", "cultist_portal_greater", "eldritch_golem", "eldritch_warden", "eldritch_hierophant",
            "taintacle_giant")]})
    # Paradox repair items: TC4 repaired it only with the Repair infusion enchantment, so the tag is empty.
    write_json(DATA / NS / "tags/item/paradox_repair_materials.json", {"replace": False, "values": []})

    # lang
    for old_file, new_file, idx in (("en_US.lang", "en_us.json", 0), ("ru_RU.lang", "ru_ru.json", 1)):
        lang = convert_lang(read_lang(ORIG / "lang" / old_file))
        if idx == 1:
            # the original ru_RU left the tab name in English; its research category was already "Забытые реликвии"
            lang[f"itemGroup.{NS}"] = "Забытые реликвии"
        for item_id, names in EXTRA_NAMES.items():
            lang.setdefault(f"item.{NS}.{item_id}", names[idx])
        for sid, subs in SUBTITLES.items():
            lang[f"subtitles.{NS}.{sid}"] = subs[idx]
        for did, msgs in EXTRA_DEATH.items():
            for suffix in ("", ".player", ".item"):
                lang.setdefault(f"death.attack.{NS}.{did}{suffix}", msgs[idx])
        # research titles/stages and the category name, produced by gen_data.py
        research_lang = ROOT / "tools/data" / f"research_lang_{('en', 'ru')[idx]}.json"
        if research_lang.exists():
            lang.update(json.loads(research_lang.read_text(encoding="utf-8")))
        write_json(ASSETS / "lang" / new_file, dict(sorted(lang.items())))
        print(f"{new_file}: {len(lang)} keys")
    print(f"{len(ITEMS)} items, {len(SOUNDS)} sounds, {len(DAMAGE_TYPES)} damage types, {len(tags)} damage tags")


if __name__ == "__main__":
    main()
