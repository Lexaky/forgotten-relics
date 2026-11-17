# CurseForge page

Paste-ready texts for the CurseForge project. CurseForge's description editor accepts Markdown: switch the editor to
Markdown and paste the **Description** section as it is.

## Project fields

- **Name:** Forgotten Relics (Thaumaturge Port)
- **Summary** (shown in search, keep it short): Ancient relics of immense power for Thaumaturge and Botania Revived — a port of the 1.7.10 Thaumcraft addon to 26.1.2.
- **Main category:** Magic. **Additional:** Adventure and RPG, Armor, Tools and Weapons.
- **Game version:** 26.1.2. **Mod loader:** NeoForge. **Environment:** client and server (needed on both).
- **License:** WTFPL (the original mod's licence).
- **Relations** (mark all as *Required Dependency*): Thaumaturge, Botania Revived, Curios API. Patchouli and
  Lithostitched come as Botania Revived's own requirements.
- **Source / issues:** fill in the repository links once the code is published.
- **Screenshots** (from `run-check/screenshots`, no extra setup needed): `frcheck_items.png` (all relics),
  `frcheck_thaumonomicon.png` (research tree), `frcheck_recipe_apotheosis.png` (infusion recipe),
  `frcheck_scenario_babylon_charging.png` (Apotheosis in action), `frcheck_entities.png` (orbs and the Babylon
  weapon), `frcheck_effects.png` (lightning and arcs), `frcheck_tooltips.png`.

---

## Description

# Forgotten Relics

**Ancient relics of immense power — tomes of devastation, the Ring of the Seven Suns, the Paradox and many more.**

Forgotten Relics was a Thaumcraft 4 / Botania addon for Minecraft 1.7.10 by **Integral**. This is an **unofficial
port** to **Minecraft 26.1.2 / NeoForge**, built on **Thaumaturge** and **Botania Revived**. The relics, their
numbers, research texts and recipes are the original's. Where the new platform forced a change, the change is
documented in the project's porting plan.

Most relics are mid- to end-game. They are researched in the Thaumonomicon, crafted on the infusion altar, and most
of them run on vis from a wand in your inventory.

### Requirements

| Mod | Version |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.122 or newer |
| Thaumaturge | 1.1.0 or newer |
| Botania Revived | 2.0.0 or newer, with its dependencies (Patchouli, Lithostitched) |
| Curios API | 15.0.0 or newer |

Install on the client and on the server.

### Getting started

Complete Thaumaturge's infusion research. The **Forgotten Relics** category then opens in the Thaumonomicon: 28
research entries, 26 infusion recipes. Several entries are hidden until you scan the right item — a Life Essence, a
Dragonstone, a Nether Star…

Hold **Shift** over a relic to read what it does, and **Ctrl** to see its vis cost. Most relics also add warp.

### The relics

**Baubles** (Curios slots)
- **Ring of Superposition** — 12–74 % of the damage you take is split among every other wearer of the ring, wherever
  they are. Now and then it swaps you with another wearer, even across dimensions.
- **Mining Charm / Ethereal Mining Charm** — faster mining (×2 / ×4) and longer block reach (+2 / +4).
- **Ring of the Seven Suns** — negates any attack above a damage cap. Fire and lava heal you. Can reflect attacks,
  and your regular attacks may grow stronger.
- **Ancient Aegis** — 25 % damage reduction, faster regeneration, no knockback. Takes 40 % of the damage dealt to
  players within 32 blocks.
- **Deific Amulet** — immunity to most status effects, puts out fire, longer invincibility frames, air underwater for
  Aer and Aqua vis.
- **Shiny Stone** — the longer you stand still, the faster you regenerate.
- **Amulet of the Oblivion** — absorbs the damage dealt to you, for Ignis and Perditio vis… and may release it upon
  you at random.
- **Nebulous Core** — 35 % vis discount, generates vis on your wands, and a 40 % chance to dodge an attack by
  teleporting away. Sometimes falls dormant and needs vis to wake up.

**Passive relics** (work from your inventory)
- **Chaos Core** — incoming damage multiplied by a random 0.0–2.0. Damage you deal or take may be redirected to a
  random nearby creature, and random effects may hit you.
- **False Justice** — every hit you deal or take becomes True Damage, and nothing short of True Damage can kill you.
- **Tome of Broken Fates** — saves you from a lethal hit once every 30–90 seconds, for vis. Never carry two of them.
- **Tome of Ageless Wisdom** — stores any amount of experience. Shift+Right-click switches it on; Right-click
  switches between absorbing and giving back.
- **The Omega Core** *(creative)* — death has no meaning for its owner.

**Utility relics**
- **Dimensional Mirror** — Shift+Right-click remembers a place. Gaze into the mirror to return there, even from
  another dimension (configurable).
- **Runic Stone** — dispels the rain for Aer, Terra and Aqua vis.
- **Tome of Discord** — teleport to the block you look at within 128 blocks, or swap places with the creature you
  look at. Shift+Right-click jumps 16 blocks ahead through anything.
- **Devourer of the Void** — drains Thaumaturge's Eldritch Obelisks. The stolen power heals you, feeds you and fills
  your wands.
- **The Paradox** — a sword with a random damage range. Every hit also wounds you by what it fell short of the
  maximum.
- **Ghastly Skull** *(creative)*.

**Battle tomes** (hold Right-click)
- **Crimson Spell** — slow homing Crimson Orbs.
- **Eldritch Spell** — Dark Matter Orbs: unblockable damage that weakens and withers. Even stronger in the Outer
  Lands.
- **Tome of Primal Chaos** — a stream of unpredictable Orbs of Primal Energy that explode on contact.
- **Nuclear Fury** — magic missiles that pass through walls and seek targets within 32 blocks.
- **Tome of Lunar Flares** — flares rain down from the sky onto the block you look at.
- **Apotheosis** — summons an array of Babylon Weapons that fly where you look and explode on impact.
- **Edict of a Thousand Damned Souls** — a soul-draining aura 20 blocks wide. Anything that comes closer than 4 blocks
  is thrown back by lightning. You stand still while you cast.
- **Tome of Predestiny** — telekinesis: hold Right-click to seize a creature, Left-click strikes it with lightning,
  Shift+Left-click hurls it away.

### Configuration

`config/forgottenrelics-common.toml` holds all 45 options of the original, under the original names: damage of every
weapon, vis cost multipliers, cooldowns, chances. `falseJusticeEnabled = false` removes False Justice together with
its research and recipe.

### Languages

English and Russian: items, tooltips and all research texts.

### Differences from the 1.7.10 original

- Built on Thaumaturge instead of Thaumcraft 4, and on Botania Revived instead of Botania. Thaumcraft's runic baubles
  in recipes became Thaumaturge's fancy baubles.
- Research follows Thaumaturge's rules: observation stages are paid from your aspect pool, theory stages need a solved
  research note.
- A few original bugs are fixed. For example, the Ring of the Seven Suns now uses its configured damage cap, Lunar
  Flares use their configured damage, and the Tome of Discord charges vis once per cast.
- Not ported: the random aura node created by an Orb of Primal Energy (Thaumaturge has no API for it), and the cosmetic
  bauble overrides.

### Credits and licence

- Original mod: **Integral** (also known as Extegral), released under the WTFPL.
- Port: NeoForge 26.1.2 rewrite. Same licence: WTFPL.
- Built on Thaumaturge by Leclowndu93150, Botania Revived, Curios API by TheIllusiveC4.

This is an unofficial port. Please report bugs on this page, not to the original author or to the authors of
Thaumaturge or Botania Revived.

---

### Кратко по-русски

Неофициальный порт аддона Forgotten Relics (1.7.10, автор Integral) на Minecraft 26.1.2 / NeoForge, для Thaumaturge и
Botania Revived. Внутри 29 реликвий: баублы, пассивные реликвии, утилиты и боевые тома. Они изучаются в Таумономиконе
(28 записей) и собираются на инфузионном алтаре (26 рецептов). Механики и числа — как в оригинале, конфиг из 45 опций.
Предметы, подсказки и все тексты исследований переведены на русский.
