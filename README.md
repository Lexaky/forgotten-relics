# Forgotten Relics — NeoForge 26.1.2 port

An unofficial port of [Forgotten Relics](https://github.com/spannerman79/Forgotten-Relics) 1.1.0 Beta (Minecraft 1.7.10,
Thaumcraft 4 + Botania) to **Minecraft 26.1.2 / NeoForge**, on top of **Thaumaturge** and **Botania Revived**. The
mechanics, numbers and texts are the original's. Where the port had to change something, that change is listed in
[PORTING_PLAN.md §6](PORTING_PLAN.md).

Version `1.1.0-26.1.2`. Licence: WTFPL, the same as the original. The original mod is by Integral (Extegral).

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.122 or newer |
| Thaumaturge | 1.1.0 or newer |
| Botania Revived | 2.0.0 or newer (tested with 2.0.2), with its own dependencies (Patchouli, Lithostitched) |
| Curios | 15.0.0 or newer |

## Content

29 items, grouped under the Thaumonomicon category **Forgotten Relics** (28 research entries, 26 infusion recipes). The
category opens after Thaumaturge's infusion research.

- **Baubles (Curios):** Ring of Superposition, Mining Charm, Ethereal Mining Charm, Ring of The Seven Suns, Ancient
  Aegis, Deific Amulet, Shiny Stone, Amulet of The Oblivion, Nebulous Core (and its dormant form).
- **Passive relics** (work while they are in the inventory): Chaos Core, False Justice, Tome of Broken Fates, The Omega
  Core, Tome of Ageless Wisdom.
- **Utility relics:** Dimensional Mirror, Runic Stone, Tome of Discord, Devourer of The Void, Ghastly Skull, The Paradox.
- **Battle tomes:** Crimson Spell, Eldritch Spell, Tome of Primal Chaos, Nuclear Fury, Tome of Lunar Flares, Apotheosis,
  Edict of a Thousand Damned Souls, Tome of Predestiny.

Relics take vis from a wand in the main inventory, as in TC4 (Thaumaturge's craft discount applies). Hold **Shift** for
a relic's description and **Ctrl** for its vis costs. All 45 options of the original are in
`config/forgottenrelics-common.toml`, with the original names (damage, vis multipliers, cooldowns, `falseJusticeEnabled`).

Languages: English and Russian (items, tooltips and all research texts).

## Building

JDK 25 is required.

```sh
JAVA_HOME=<jdk-25> ./gradlew build              # build/libs/forgotten-relics-1.1.0-26.1.2.jar
JAVA_HOME=<jdk-25> ./gradlew runClient
```

Checks used during the port (see [PROGRESS.md](PROGRESS.md) for what each milestone verified):

```sh
./gradlew runGameTestServer      # 80 headless GameTests (formulas, projectiles, all 26 infusions on a real altar, research)
./gradlew runServer              # dedicated server boot
./gradlew runClientCheck         # client run: screenshots + 16 in-world scenarios with the real player ([scenario] PASS/FAIL)
python -I tools/check_assets.py . forgottenrelics  # models and textures for every item
python -I tools/check_lang.py . forgottenrelics    # en_us / ru_ru keys
python -I tools/check_animation.py               # Runic Stone animation, from the runClientCheck screenshots
```

Research, recipes, item aspects, models and language files are generated. Do not edit the JSON by hand: change the
tables in `tools/` and run `extract_original.py`, then `gen_data.py`, then `gen_assets.py`
(`gen_test_structures.py` makes the GameTest altar structure).

## Project documents

- [PORTING_PLAN.md](PORTING_PLAN.md): inventory of the original, mapping to the new APIs, deviations.
- [PROJECT_MAP.md](PROJECT_MAP.md): code map.
- [PROGRESS.md](PROGRESS.md): status per milestone, what was verified and what was not.
- [CURSE_FORGE.md](CURSE_FORGE.md): texts for the CurseForge project page.
