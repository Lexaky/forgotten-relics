# TC4 → Thaumaturge mapping tables

## Aspects

TT has exactly these 37 (ids `thaumaturge:<name>`): aer, terra, ignis, aqua, ordo, perditio (primals), vacuos, lux,
motus, gelum, vitreus, metallum, victus, mortuus, potentia, permutatio, praecantatio, auram, alkimia, vitium, tenebrae,
alienis, volatus, herba, instrumentum, fabrico, machina, vinculum, spiritus, cognitio, sensus, aversio, praemunio,
desiderium, exanimis, bestia, humanus.

TC4 aspects without a same-name TT aspect (choices used in the Forbidden Magic port):

| TC4 | TT | TC4 | TT |
|---|---|---|---|
| telum (weapon) | aversio | tutamen (armor) | praemunio |
| fames (hunger) | desiderium | lucrum (greed) | desiderium |
| corpus (flesh) | bestia | sano (heal) | victus |
| iter (travel) | motus | perfodio (mine) | instrumentum |
| meto (harvest) | instrumentum | messis (crop) | herba |
| arbor (tree) | herba | pannus (cloth) | fabrico |
| venenum (poison) | mortuus | limus (slime) | aqua |
| tempestas (weather) | aer | | |

Code-name → TT: TAINT=vitium, ELDRITCH=alienis, MAGIC=praecantatio, TRAP=vinculum, SOUL=spiritus, DEATH=mortuus,
UNDEAD=exanimis, ENERGY=potentia, EXCHANGE=permutatio, CRAFT=fabrico, MECHANISM=machina, TOOL=instrumentum,
FLIGHT=volatus, DARKNESS=tenebrae, LIGHT=lux, COLD=gelum, CRYSTAL=vitreus, METAL=metallum, LIFE=victus, PLANT=herba,
BEAST=bestia, MAN=humanus, AURA=auram, MIND=cognitio, SENSES=sensus.

Compound addon aspects must have exactly 0 or 2 components; remap TC4 components with the table above
(e.g. Forbidden Magic `ira = telum+ignis` → `aversio + ignis`).

## Items and blocks

| TC4 (`ItemApi`/`ConfigItems`) | TT id |
|---|---|
| itemShard 0–5 (aer, ignis, aqua, terra, ordo, perditio) | in recipes: `thaumaturge:essentia_crystal` with component `thaumaturge:crystal_aspect` = that aspect (`#thaumaturge:vis_crystals` contains `essentia_crystal`) |
| itemResource 0/1/2/3/4/5/6/7 | alumentum / nitor_yellow / ingot_thaumium / quicksilver / tallow / brain / amber / fabric |
| itemResource 9/10/11/12/13 | (knowledge fragment) / mirrored_glass / tainted_goo / taint_tendril / label |
| itemResource 14/15/16/17 | salis_mundus / primal_charm / ingot_void / void_seed |
| thaumium tools | thaumium_pickaxe/_sword/_shovel/_axe/_hoe |
| elemental tools | elemental_pickaxe/_shovel/_axe/_sword/_hoe |
| blockMagicalLog 0/1 | `#thaumaturge:greatwood_logs` / `#thaumaturge:silverwood_logs` |
| blockCosmeticSolid 4 (thaumium block) | metal_thaumium |
| blockJar 0 | jar_normal (essentia only through its **top** face) |
| blockTube | tube |
| itemBaubleBlanks 0/1/2 | amulet_mundane / ring_mundane / girdle_mundane |
| itemAmuletVis 1 | amulet_vis_crafted |
| itemEldritchObject 3 | primordial_pearl |
| itemInkwell | scribing_tools |
| blockFluxGoo | flux_goo (fluid block); there is no flux gas block |
| taint blocks | taint_fibre, taint_crust, taint_soil, taint_rock, taint_log... (`#thaumaturge:taint_blocks`) |
| ore clusters | cluster_iron/copper/gold/lead/silver/tin/cinnabar/quartz |
| mob effects | thaumaturge:flux_taint, thaumarhia, vis_exhaust, unnatural_hunger, warp_ward... |
| damage type | `TTDamageTypes.TAINT` |
| entities | firebat, wisp, thaumic_slime, brainy_zombie, giant_brainy_zombie, taint_crawler, taint_swarm, taintacle, cultist_knight, cultist_cleric, eldritch_crab, eldritch_guardian, inhabited_zombie, pech |

## TC4 research keys → TT entry ids (`thaumaturge:<id>`)

THAUMIUM → metallurgy · INFUSION → infusion · INFUSIONENCHANTMENT → infusion_enchantment · ROD_silverwood →
rod_silverwood · ETHEREALBLOOM → ethereal_bloom · VISAMULET → vis_amulet · PRIMPEARL → primordial_pearl · RESEARCH →
research · VOIDMETAL → base_eldritch · ELEMENTALPICK/ELEMENTALSHOVEL → elemental_tools · DISTILESSENTIA →
essentia_smelter · TRANSGOLD → metal_purification (closest) · FOCUSBLINK (TT's own) → focus_blink.
List all TT entries from the release jar: `data/thaumaturge/thaumaturge/research_entry/*.json`.

## Wand parts in TT 1.1.0

Caps `thaumaturge:iron, copper, gold, silver, thaumium, void`; rods `wood, greatwood, obsidian, blaze, ice, quartz,
bone, reed, silverwood` and staves `<name>_staff`, `primal_staff`. Wand display name uses lang keys
`wand.thaumaturge.rod.<rod path>` and `wand.thaumaturge.cap.<cap path>` — **path only, namespace ignored**, so an addon
rod `ns:infernal` needs `wand.thaumaturge.rod.infernal` in the addon's lang file.
