# Thaumaturge 1.1.0 data formats (copied from the release jar and verified by loading)

## Aspect — `data/<ns>/thaumaturge/aspect/<tag>.json`
```json
{"tag": "ira", "color": 8848388, "components": ["thaumaturge:aversio", "thaumaturge:ignis"],
 "chat_color": "4", "texture": "<ns>:textures/aspects/ira.png", "blend": 771}
```
Without `texture` TT looks under **thaumaturge**'s namespace, so addons must set it. Icon 32×32.
Lang: `aspect.<ns>.<tag>`, `aspect.<ns>.<tag>.desc`, `aspect.<ns>.<tag>.help`.

## Item aspects — `data/thaumaturge/data_maps/item/base_aspects.json` (inside the addon)
```json
{"values": {"<ns>:wrath_shard": [{"aspect": "<ns>:infernus", "amount": 2}, {"aspect": "thaumaturge:vitreus", "amount": 2}],
            "#c:some_tag": [ ... ]}}
```
Entries merge across packs **per key**: an addon entry for `minecraft:netherrack` replaces TT's whole list, so write
TT's existing aspects plus yours (read them from `config/thaumaturge/aspect_index.json` of a booted server, and drop
your own aspects from it first so regeneration stays idempotent). Items without an entry get aspects derived from their
recipes (crucible, infusion, crafting, arcane). Entities: `data/thaumaturge/data_maps/entity_type/entity_aspects.json`,
same shape (TT's own values are in the jar).

## Research category — `data/<ns>/thaumaturge/research_category/<id>.json`
```json
{"background": "<ns>:textures/research/runecircle.png", "icon": "<ns>:textures/research/forbidden.png",
 "formula": [{"aspect": "<ns>:infernus", "amount": 20}], "index": 10}
```
Optional `required_research`, `overlay_background`. TT backgrounds are 1024², smaller images tile (fine).
Lang: `research_category.<ns>.<id>`.

## Research entry — `data/<ns>/thaumaturge/research_entry/<id>.json`
```json
{"category": "<ns>:forbidden", "name": "research.<ns>.skull_axe.title",
 "parents": ["<ns>:nether_shards", "thaumaturge:metallurgy", "~thaumaturge:infusion"],
 "column": -6, "row": -4, "icons": ["<ns>:skull_axe"],
 "stages": [
   {"text": "research.<ns>.skull_axe.stage_0",
    "required_knowledge": [{"type": "theory", "category": "<ns>:forbidden", "amount": 1}]},
   {"text": "research.<ns>.skull_axe.stage_1", "recipes": ["<ns>:infusion/skull_axe"], "warp": 1}],
 "note_aspects": [{"aspect": "thaumaturge:aversio", "amount": 3}, {"aspect": "<ns>:ira", "amount": 4}],
 "complexity": 2, "meta": ["round"]}
```
Icons: item id, a texture path ending in `.png`, or `focus:<ns>:<spell_part>`. Stage fields also include `obtain`,
`craft`, `knowledge` (rewards), `required_research`, `construct`. `meta`: round, spiky, reverse, hidden, autounlock, hex.
Recipes listed in stages appear as **bookmarks on the right edge of the book**, not as pages.

## Infusion — `data/<ns>/recipe/infusion/<name>.json`
```json
{"type": "thaumaturge:infusion", "catalyst": "thaumaturge:thaumium_axe", "instability": 1,
 "aspects": [{"aspect": "<ns>:ira", "amount": 8}],
 "components": ["<ns>:wrath_shard", "minecraft:diamond", "#thaumaturge:silverwood_logs",
   {"neoforge:ingredient_type": "neoforge:components", "items": "thaumaturge:essentia_crystal",
    "components": {"thaumaturge:crystal_aspect": {"aspect": "thaumaturge:ordo"}}}],
 "research": {"entry": "<ns>:skull_axe"}, "result": {"id": "<ns>:skull_axe"}}
```
`result` may carry components, e.g. an enchanted book:
`{"id":"minecraft:enchanted_book","components":{"minecraft:stored_enchantments":{"<ns>:wrath":1}}}`.

## Crucible — `"type": "thaumaturge:crucible"`, fields `catalyst`, `aspects`, `research`, `result` (`count` allowed).

## Arcane workbench
```json
{"type": "thaumaturge:arcane_workbench_shaped", "pattern": ["SSS","SXS","SSS"], "key": {"S": "minecraft:stone", "X": "#<ns>:taint_resources"},
 "vis": 10, "crystals": [{"aspect": "thaumaturge:perditio", "amount": 2}], "research": {"entry": "<ns>:taint_stone"},
 "result": {"id": "<ns>:taint_stone", "count": 9}}
```
Shapeless: `"type": "thaumaturge:arcane_workbench_shapeless"`, `"ingredients": [...]`. TC4 listed per-primal vis costs;
TT wants total `vis` plus crystals — document the conversion rule you pick.

## Infusion enchantment (TT's own, closed list)
`{"type":"thaumaturge:infusion_enchantment","enchantment":"arcing",...}` accepts only values of the
`InfusionEnchantment` enum (collector, destructive, burrowing, sounding, refining, arcing, essence, visbattery,
vischarge, swift, agile, infested, lamplight). Addon enchantments must use a normal datapack enchantment instead.

## Spell part — `data/<ns>/thaumaturge/spell_part/<id>.json`
```json
{"aspect": "<ns>:infernus", "behavior": {"type": "<ns>:hellfire"}, "color": 16729088, "complexity": 5,
 "icon": "<ns>:textures/foci/hellfire.png", "max_per_spell": 1, "research": {"entry": "<ns>:blink_upgrades"}}
```
Lang: `spell.<ns>.<id>.name`, `spell.<ns>.<id>.desc`. The behaviour type is registered in code (code-integration).

## Wand assembly recipes
Do not write them. At pack load TT serves `thaumaturge:wand/assembly/<cap>_<rod>` (and sceptre) recipes for every
registered `ItemWandRod` × `ItemWandCap` item, gated by the rod's `assemblyResearch`. Addon pairs are named
`<capns>_<cap>_<rodns>_<rod>`, e.g. `thaumaturge:wand/assembly/thaumaturge_iron_forbidden_infernal`. The log line
"Serving N wand assembly recipes from X caps and Y rods" confirms your rods were picked up.

## Other tags addons use
`thaumaturge:scribing_tools`, `thaumaturge:taint_tools`, `thaumaturge:wand_rods`, `thaumaturge:wands`,
`thaumaturge:runic_shieldable`, `thaumaturge:vis_crystals`, `thaumaturge:taint_blocks` (block), `curios:<slot>`.
