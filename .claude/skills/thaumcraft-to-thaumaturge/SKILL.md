---
name: thaumcraft-to-thaumaturge
description: >-
  How to port Thaumcraft 4 (1.7.10) addons and mechanics to Thaumaturge (Leclowndu93150/Thaumaturge, 1.1.0 on
  Minecraft 26.1.2): aspect mapping TC4→TT, research entries with stages/notes/knowledge, infusion/crucible/arcane
  recipe JSON, item and entity aspects, wand rods and caps with auto-generated assembly recipes, wand vis access,
  essentia transport and suction, caster (wand) triggers, warp, taint, aspect pools, scribing tools, spell parts, Curios
  baubles, TC4 particle/lightning effects and testing infusions on a real altar. Use it whenever an addon targets Thaumaturge, when someone ports a Thaumcraft/TC4/TC6 addon (Forbidden Magic,
  Tainted Magic, Thaumic Tinkerer, Thaumic Horizons...), or asks how to add aspects, research, wands or essentia
  machines to Thaumaturge — even if they just say "Thaumcraft addon for 26.1".
---

# Thaumcraft 4 → Thaumaturge

Thaumaturge (TT) is a TC4/TC6 hybrid: wands, rods, caps and vis are TC4; the 37 aspects, staged research with
knowledge/notes, focus spells built from parts and block-level taint are TC6. Almost everything an addon adds is
**data**; code is needed for wand rods, block entities, events and spell behaviours.

- `references/tc4-tt-mapping.md` — aspect, item, research-id and system mappings (TC4 name → TT id).
- `references/data-formats.md` — exact JSON for aspects, item/entity aspect maps, research categories/entries, infusion,
  crucible, arcane and infusion-enchantment recipes, spell parts, and the research flow rules.
- `references/code-integration.md` — Java for rods/caps, wand vis read/write, essentia consumers, caster triggers,
  warp, taint, aspect pools, knowledge, spells, Curios, and which TT classes are API vs internal.
- `references/effects-and-testing.md` — TC4 client FX (burst, arcs, lightning bolts, orb renderers) on TT's `Effects`
  facade and textures, TC4 colour/size values checked in the TC4 classes, and crafting infusions on a real altar in a
  GameTest (stability, activation, creative jars).

## Ground rules

1. Get the **release jar** for the exact TT version (CurseForge project 1628024; 1.1.0 for 26.1.2 is file 9428045).
   Generated JSON lives only there (`data/thaumaturge/recipe`, `.../thaumaturge/aspect`, `research_category`...).
   Read a few TT files of the same kind before writing yours.
2. Package `com.leclowndu93150.thaumaturge.api` is the contract. `content`/`client` are internal; use them only when
   unavoidable (e.g. `content.wands.ItemWandRod`, because TT generates wand assembly recipes only for items of that
   class) and say so in the plan.
3. TT registry paths are nested: datapack registries live in `data/<your-ns>/thaumaturge/<registry>/<name>.json`
   (`aspect`, `research_category`, `research_entry`, `spell_part`...). Recipes are ordinary `data/<ns>/recipe/`.
4. TT item aspect values are on the TC6 scale (≈2–5× TC4). Choose a factor (Forbidden Magic used ×2) and apply it in a
   generator script.
5. Boot a server after every data change: TT parses everything at load, and its log shows codec errors. A strong sign
   that infusion recipes parsed: TT's aspect index (`run-server/config/thaumaturge/aspect_index.json`) lists aspects for
   your tools derived from those recipes.

## What maps to what (summary)

| TC4 / old addon | Thaumaturge |
|---|---|
| `new Aspect(...)` | `data/<ns>/thaumaturge/aspect/<tag>.json` (0 or 2 components, own `texture`) |
| `registerObjectTag` | data map `thaumaturge:base_aspects` (item) / `thaumaturge:entity_aspects` |
| `ResearchItem` + pages | `research_entry` JSON with `stages`; text keys `research.<ns>.<id>.stage_N` |
| `setHidden/setConcealed`, `setAutoUnlock`, `setRound` | `meta`: `hidden`, `autounlock`, `round` |
| `addWarpToResearch` | `"warp": n` on the final stage |
| `addInfusionCraftingRecipe` | recipe type `thaumaturge:infusion`; TC4 shards → `essentia_crystal` with `crystal_aspect` |
| `addCrucibleRecipe` / `addArcaneCraftingRecipe` | `thaumaturge:crucible` / `thaumaturge:arcane_workbench_shaped|shapeless` (`vis`, `crystals`) |
| `addInfusionEnchantmentRecipe` | TT's `InfusionEnchantment` is a **closed enum** → datapack enchantment + infusion that outputs an enchanted book |
| `WandRod`/`StaffRod`/`WandCap` | same names in TT's `wand_rod`/`wand_cap` registries; items via `ItemWandRod`/`ItemWandCap` |
| `IWandRodOnUpdate` | same interface: `onUpdate(ItemStack wand, Player player)` |
| `ItemWandCasting.addVis/consumeVis` | `WandAccess` + the `thaumaturge:wand_vis` component (see code-integration) |
| `IWandTriggerManager` | `CasterTriggerRegistry` + `ICasterTriggerManager` |
| `ItemFocusBasic` + upgrades | spell parts (`spell_part` JSON + `SpellBehaviorType`); upgrades become standalone effects |
| `IEssentiaTransport` tile | capability `EssentiaCapabilities.TRANSPORT`; consumers expose suction and pull |
| `IScribeTools` | item tag `thaumaturge:scribing_tools` |
| aspect pool points | `AspectPoolAccess.grant` |
| sticky warp | `WarpHelper.addWarp(player, n, WarpType.NORMAL)`; `IWarpingGear` counts in hands, armour and curios |
| taint biome | `TaintApi.isTainted(level, pos)` (taint is chunk saturation, not a biome) |
| Baubles | Curios (hard dependency of TT); slot tags `curios:ring`, `curios:necklace`... |
| `IRunicArmor` | tag `thaumaturge:runic_shieldable` |
| `Config.taintMaterial` | tag `thaumaturge:taint_blocks`; fast digging on them via `thaumaturge:taint_tools` |

| `Thaumcraft.proxy.burst/arcLightning`, other client FX | `content.effect.Effects` (server-side, TT sends its own packets) — see references/effects-and-testing.md |
| TC4 `FXLightningBolt` (branching, with duration) | no TT equivalent (`boltStrike` is a 3-tick beam); Botania Revived `BoltRenderer` + own S2C payload |
| vis discount gear | `IVisDiscountGear`; TT turns it into the player attribute `thaumaturge:vis_discount` (curios: on the player tick) |

## Research flow you must design for

A stage with `required_knowledge` is not satisfied by knowledge points: each `theory` row needs a solved research note
for that entry (puzzle built from `note_aspects`), and `observation` rows charge the entry's `note_aspects` × amount from
the player's aspect pool. So every non-auto entry needs sensible `note_aspects` made of aspects players can discover,
including the addon's own aspects (players learn them by scanning items that carry them — give your aspects to items).
Parents use `"ns:id"`, `"ns:id@2"` (stage reached) or `"~ns:id"` (no connector line). A research category may omit
`required_research` to be visible from the start.

Testing research for real: `ResearchManager` (internal) has `unlock`, `advanceStage`, `stageRequirementsMet`,
`parentsSatisfied`; observation is paid with `AspectPoolAccess.grant/spendAll`, theory needs
`ResearchNoteData.learnKey(entry, row)` known (what solving the note produces); scans go through the public
`ScanningManager.scan(player, ScanTarget.stack(stack))`. An infusion recipe can be crafted on a real altar inside a
GameTest — recipe in references/effects-and-testing.md.

## Typical deviations to document

- Infusion enchanting → enchanted book + anvil (closed enum).
- TC4 biome checks → `TaintApi`.
- TC4 vis amulets stored vis; TT amulets draw from the aura. An item that must store vis needs its own component
  (Forbidden Magic used `WandVis` as the value type).
- Focus upgrades → separate spell parts, order-dependent in the spell graph.
- TC4 research difficulty modes don't exist; port the "normal" path.
- TT's primordial pearl loses a use when crafted instead of returning whole.
