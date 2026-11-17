# Thaumaturge effects, TC4 visuals and real-altar tests (TT 1.1.0)

Found while porting Forgotten Relics; all checked in game or in GameTests.

## Client effects without own packets: `content.effect.Effects`

Internal facade (use from one compat class). Each builder ends with `.send()` and TT broadcasts to players near the
start point (64 blocks).

| TC4 call | TT |
|---|---|
| `Thaumcraft.proxy.burst(world, x, y, z, size)` (`FXBurst`) | `Effects.burst(level, pos).size(size * 15)` — TC4 drew half-size `particleScale · size` with `particleScale` random 1..2, TT draws `0.1 · size` |
| `Thaumcraft.proxy.arcLightning(world, x, y, z, tx, ty, tz, r, g, b, h)` (`FXArc`) | `Effects.zapArc(level, from).to(to).color(rgb).drift(h)` |
| `FXLightningBolt` (`Main.proxy.lightning`, `nodeBolt`) | **not ported by TT**. `Effects.boltStrike` is a straight 3-tick beam. Use Botania Revived's `BoltRenderer.INSTANCE.add(level, new BoltParticleOptions(info, from, to).size(width).lifespan(duration), 0)` from an own S2C payload; TC4 width (blocks) = Botania size |
| sparkles, wisps, block runes, crimson puffs... | the other `Effects.*` builders (`spark`, `wispParticles`, `bamf`, `glint`, ...) |

TC4 `FXLightningBolt` colours by type (outer glow / inner core): 0 = (0.6, 0.3, 0.6) / (1.0, 0.6, 1.0),
1 = (0.6, 0.6, 0.1) / (1, 1, 0.1), 2 = (0.1, 0.1, 0.6) / (0.1, 0.1, 1), 3 = (0.1, 1, 0.1) / (0.1, 0.6, 0.1),
4 = (0.6, 0.1, 0.1) / (1, 0.1, 0.1), 5 = (0.6, 0.2, 0.6) / black, 6 = (0.75, 1, 1) / same; default width 0.03.

## TC4 particle-sheet frames that TT ships as separate strips

TC4's `textures/misc/particles.png` (256², 16 px cells) is not in TT, but the frames are:

| TC4 sheet use | TT texture |
|---|---|
| bottom row, 32 px, 6 golem-orb frames (`RenderCrimsonOrb`, golem orbs) | `effect/golem_orb_blue.png`, `effect/golem_orb_red.png` |
| row 2, 13 cyan swirl frames (`RenderPrimalOrb` core) | `particle/flux_swirl.png` (14 frames) |
| row 3, 13 dark orb frames (`RenderEldritchOrb` core) | `effect/eldritch_orb.png` |
| white glow frames | `effect/orb_glow.png` |

TC4 orb rays (`RenderPrimalOrb`/`RenderEldritchOrb`): 12 triangle fans, `new Random(entityId)`, six rotations per ray
applied to the **same** matrix (each ray builds on the previous), length `(rand·20+5)/30`, width `(rand·2+1)/30`, both
× `min(age, 10)/10`, spin `age/80 · 360°`; apex white alpha 1, tips alpha 0 in `BlockCustomOreItem.colors`
{white, FFFF7E air, FF3C01 fire, 0090FF water, 00A000 earth, EECCFF order, 555577 entropy}; primal uses `colors[i/2+1]`,
eldritch `colors[5]`. In 26.1: `RenderTypes.dragonRays()`.

## Crafting an infusion on a real altar in a GameTest

1. A structure big enough (17×8×17): generate an empty one (gzipped NBT: `DataVersion`, `size`, palette with air,
   empty `blocks`/`entities`).
2. Rebuild `/thaumaturge build infusion_altar`: central `pedestal_arcane`, `infusion_matrix` two above,
   `pillar_arcane` on the four diagonal corners (block above removed), component pedestals around.
3. Essentia: `jar_creative` blocks within 12 blocks of the matrix,
   `BlockEntityJarCreative.setStorageContents(aspectKey, n)`.
4. Stability: TT drifts stability by up to `instability / 5..8` per cycle; negative values fire instability events
   and stall the craft. Symmetric stabiliser pairs restore it, and each distinct block identity counts once at full
   strength (repeats ×0.75 per rank) — use one pair per candle colour (`thaumaturge:candle_<colour>`) and per head type.
5. The matrix needs **two** wand clicks: `onRightClick` once activates it (then it regenerates stability up to 25 while
   idle — wait ~300 ticks), the second starts the craft (`tryStartCraft`). Give the FakePlayer the recipe's research.
6. Succeed when the central pedestal holds the result and `isCrafting()` is false. All 26 Forgotten Relics recipes
   (up to 12 components and ~1000 essentia) finish within 6000 GameTest ticks (~30 s real time).

## Admin commands worth knowing

`/thaumaturge build infusion_altar` (creative), `research grant|revoke|reset|stage|ready|query`, `scan everything`,
`knowledge query|all`, `showcase`.
