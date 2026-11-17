---
name: mc-port-verification
description: >-
  How to prove that ported Minecraft mod mechanics actually work on NeoForge 26.1.x before calling them done: headless
  GameTests registered from code (RegisterGameTestsEvent, inline GameTestInstance, isolated batches), statistical checks
  for random mechanics, capturing drops, mock players vs FakePlayer pitfalls, testing research/aura/essentia integration,
  server boot log checks, asset/lang linters, and an automated client screenshot run for visuals and GUIs. Use it
  whenever you implement or port a mod feature and need to verify it, when asked to "test", "check", "verify",
  "make sure it works", write GameTests, or when a status in a porting plan says partially verified.
---

# Verifying a ported mod

A feature is **verified** only when something executed it and checked the observable result: a GameTest assertion,
a clean server/client log with the content loaded, or a screenshot you actually looked at. Compiling is not
verification; neither is "the JSON looks right". Mark the rest 🟡 and say what is missing.

## Layers (cheapest first)

1. **Linters** — `scripts/check_assets.py` and `scripts/check_lang.py` (copy from this skill; both take the project dir
   and mod id). Catch missing item model definitions, broken model/texture references, bad PNG/animation sizes and
   missing translations before launching the game.
2. **Server boot** — `runServer`, wait for `Done (`, then grep the log for `ERROR`, `Exception`, `Failed to`, your mod id
   and the target library's parse errors. Datapack codecs fail here, not at compile time.
3. **GameTests** — every mechanic with a rule ("drops 1 shard 1/(20−2·looting)", "rod recharges 1 vis per 5 s up to
   20%") gets a test that sets up the situation and asserts the rule. See `references/gametest-patterns.md`.
4. **Client run** — only for what has no server-side effect: item/block models, tints, renderers, GUIs, book pages.
   See `references/client-check.md` for a harness that builds a scene, takes screenshots and quits.

## GameTest essentials for 26.1

- Tests are registered in code: `RegisterGameTestsEvent` on the mod bus → `event.registerEnvironment(...)` and
  `event.registerTest(id, GameTestInstance)`. Subclass `GameTestInstance` once ("InlineTest") with a
  `Consumer<GameTestHelper>`; use a tiny structure that already exists (e.g. an empty structure shipped by a dependency)
  instead of shipping NBT.
- Register only when a system property is set (`-Dyourmod.gametest=true` in the `gameTestServer` run) so tests never
  load in a real game, and set `neoforge.enabledGameTestNamespaces` to your mod id.
- Run: `./gradlew runGameTestServer`; results are in the console and `run-gametest/logs/latest.log`
  (`All N required tests passed`). Run the full suite at least twice — flaky tests show up on repetition.
- Tests in one batch share the world and run in parallel. Anything that looks for nearby POIs, mobs or blocks
  (villager job sites, AI, explosions, neighbour scans) belongs in its **own environment** ("isolated batch").
- Use `helper.assertionException(Component.literal(msg))` with a message that prints the actual values.

## Pitfalls that cost hours (all real)

- `helper.makeMockPlayer(...)` is not a `ServerPlayer`. Code that needs one (Curios sync, `KnowledgeAccess.sync`,
  advancement grants) crashes or no-ops. `FakePlayerFactory.get(level, profile)` gives a ServerPlayer, but it **cannot
  ride**, is **invulnerable to damage for its first ticks** (`LivingIncomingDamageEvent` never fires), and Curios mock
  inventories need `inv.reset()` before use.
- To test damage handlers that FakePlayer can't receive, construct the event directly
  (`new LivingDamageEvent.Post(entity, container)` with a `DamageContainer` and `captureInflictedDamage()`), or use a
  mob as the victim.
- Drops created in unloaded chunks or other dimensions are invisible to area scans — capture them with an
  `EntityJoinLevelEvent` listener that records and cancels `ItemEntity`s (`DropCapture` pattern in the reference).
- Blocks placed at the structure's floor (y=0 relative) may have no room above; trees and tall plants need headroom.
- Directional capabilities: query the face the real neighbour would use (essentia jars only connect through the top).
- Random mechanics: never assert one roll. Run N trials and accept |hits − Np| ≤ 5σ + 1 (σ = √(Np(1−p))) — this
  catches a wrong formula (e.g. 1/20 vs 1/10) with N≈2000 and essentially never fails by chance.
- Content gated by progression (research, advancements): emulate the player's steps through the public API where
  possible; use the library's internal "complete" call only inside tests, and say so.
- Kill leftover dev JVMs before rerunning, or the world/log files stay locked.
- A Gradle run that "takes 10+ minutes" before the game starts is usually Gradle, not the tests: `jstack` the BUSY
  daemon; a hang in `PosixFileSystemFunctions.listFileSystems` (file-system watching) is fixed by
  `org.gradle.vfs.watch=false` in gradle.properties.
- FakePlayer never ticks, so Curios never applies a worn item's attribute modifiers to it. Thaumaturge's vis discount
  (`IVisDiscountGear` → attribute `thaumaturge:vis_discount`) and any curio attribute must be checked on a ticking
  player, and only some ticks after the equip change (the old value lingers for a tick).

## In-world scenarios with the real player (client check)

For what a GameTest cannot reach (cross-dimension teleports, held Right-click over many real ticks, real deaths, a
second online player, client-side movement), run a scenario queue from the screenshot client (forgotten-relics:
`client/ScenarioRunner`): steps run on the client or via `server.submit(...)` (wait for the future before the next
step), each scenario logs `[scenario] PASS/FAIL name: detail`, then grep the log.

- The check window usually has no focus: set `options.pauseOnLostFocus = false` and close any screen every tick, or the
  pause menu stops the integrated server and every key press is lost.
- Simulated key presses (`KeyMapping.setDown`) are unreliable in an unfocused window. Drive the server the way the
  packet handlers do: `player.gameMode.useItem(player, level, stack, hand)` (starts a held use), `releaseUsingItem()`,
  `setShiftKeyDown(b)`, `attack(entity)`, `swing(hand, true)` (fires `onEntitySwing`). Items whose 1.7 version relied
  on Right-click auto-repeat need `useItem` every 4 ticks. Test client-only code (motion zeroing) on the client player.
- A second "player": a mock `ServerPlayer` on an `EmbeddedChannel` breaks Curios (`payload curios:sync_data may not be
  sent`). Add a FakePlayer to `PlayerList.players` (reflection; `getPlayers()` is read-only) and `level.addNewPlayer`;
  remember it is invulnerable and ignores teleports — observe the real player's side.
- The check world persists between runs: start and end every run at a fixed overworld spot
  (`execute in minecraft:overworld run tp ...`), build a throwaway arena far away, and record-and-cancel explosions
  (`ExplosionEvent.Start`) instead of detonating them.
- Sound "played" = the client `SoundManager.addListener` callback fired (plus `PlayLevelSoundEvent` on the server);
  a held tome that needs a pointed block plays nothing while looking at the sky.
- Infusion on a real Thaumaturge altar works in a GameTest: generate a large empty structure, rebuild the
  `/thaumaturge build infusion_altar` layout, add creative jars and distinct stabiliser pairs (each candle colour/head
  counts once at full strength), right-click the matrix once to activate, wait ~300 ticks for stability, then start.

## Reporting

Keep a table per milestone: feature → how verified (test name / log line / screenshot file) → ✅/🟡/⬜. When a test
exposed a bug, write down the bug and the fix. If the user asks "is everything verified?", answer from this table, not
from memory.
