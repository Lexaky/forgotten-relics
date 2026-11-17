---
name: legacy-mod-porting
description: >-
  Step-by-step workflow for porting an old Minecraft mod or addon (Forge 1.7.10 / 1.12 era, Thaumcraft 4 addons and
  similar) to a modern NeoForge version such as 26.1.x, keeping the original mechanics. Use it whenever a task says
  "port", "перенести", "портировать", "воссоздать мод", "rewrite this old mod for NeoForge", asks for a porting plan or
  PORTING_PLAN.md, or starts from an old mod's GitHub repo plus a modern target mod — even if the user only asks for
  "analysis first". It covers research, inventory, mapping, planning, milestone order and honest status reporting, and
  points to the API, Thaumaturge and verification skills for the details.
---

# Porting a legacy mod to modern NeoForge

This workflow was used to port Forbidden Magic (1.7.10, Thaumcraft 4 addon) to Thaumaturge 1.1.0 on Minecraft 26.1.2,
ending with 58 passing GameTests and an automated client screenshot check. Follow it in order; each phase feeds the next.

Related skills (load them when you reach the matching phase):
- `neoforge-26-1-api` — how old Forge/Minecraft code maps onto the 26.1 API, build setup, where to read sources.
- `thaumcraft-to-thaumaturge` — TC4 concepts mapped to Thaumaturge (aspects, research, recipes, wands, essentia...).
- `mc-port-verification` — GameTests, statistical tests for random mechanics, client screenshot checks, asset/lang scripts.

## Phase 1 — Get every source on disk

1. Clone the old mod, the target library and any "reference port" into the scratchpad (not the project). Large repos
   can time out on the first clone; retry once with a longer timeout before giving up.
2. The target library's git repo often lacks generated data (recipes, aspects, research categories are produced by
   datagen and git-ignored). Download the **release jar** of the exact target version and unzip it: it is the only
   place to see the real JSON formats. CurseForge release jars can be fetched through CurseMaven
   (`https://cursemaven.com/curse/maven/<slug>-<projectId>/<fileId>/<slug>-<projectId>-<fileId>.jar`); the file id comes
   from `https://www.curseforge.com/api/v1/mods/<projectId>/files`.
3. Extract Minecraft + NeoForge sources once the project has compiled (see `neoforge-26-1-api`); grep them instead of
   guessing method names. Most API mistakes in a port are names that changed between versions.

## Phase 2 — Inventory the old mod

Read the registries first (items, blocks, aspects, enchantments), then research/recipes, then the event handler, then
each item class. Write the inventory as tables, because the plan, the mapping and the tests all reuse them:

| Table | Columns |
|---|---|
| Items/blocks | old id + meta → behaviour (numbers: damage, chances, durability, ticks) |
| Research | key, parents, pages, recipes, warp, hidden/auto-unlock flags |
| Recipes | type, catalyst, components, aspects, instability |
| Events | trigger → condition → exact formula (copy the constants) |
| Integrations | other mods → what breaks if the mod does not exist |
| Config | option → default → what it gates |

Keep the exact formulas (`rand.nextInt(61) <= 2 + damage`, `4/30`, `1 + 2*fortune`...). They become the expected values
of statistical tests later; vague notes like "sometimes drops" cannot be verified.

## Phase 3 — Analyse the target and classify every mechanic

For each mechanic decide one of:
- **Data** — the target exposes it as datapack JSON (aspects, research, recipes, data maps, loot, enchantments, trades,
  tags). Prefer this: no code, survives target updates, reviewable.
- **Public API / registry** — the target has an `api` package, a custom registry or an event (wand rods, caster
  triggers, warp, essentia capability). Use it.
- **Event bridge** — vanilla/NeoForge events (drops, damage, break speed, eating, spawn) re-create old hooks.
- **Own mechanism** — the target has no extension point (e.g. a closed enum). Build the closest thing that keeps the
  player-facing behaviour, and document the deviation.
- **Dropped** — depends on mods that do not exist for the new version. Say so explicitly.

Check the target's `api` vs `content`/`internal` packages. Using a public class from `content` is sometimes unavoidable
(e.g. an item class the target scans for); isolate it and note it in the plan. Never copy target code into the addon.

## Phase 4 — Write PORTING_PLAN.md before coding

Use `references/plan-template.md`. It must contain: sources with exact versions/commits, the inventory, the target
architecture table (what is extensible and how), the old→new mapping tables, the deviations list, milestones with
dependencies and a verification criterion each, and a status table. The user asked for this file first in the original
port; write it in the user's language.

## Phase 5 — Build milestone by milestone

Proven order (each depends only on earlier ones):

1. Scaffold: build files, mod class, empty registries; prove the server boots with the target installed.
2. Aspects/resources + item aspect data; 3. research category/entries + simple recipes; 4. events;
5. tools; 6. wand parts; 7. blocks (+ loot, tags, tree features); 8. block entities/machines;
9. enchantments; 10. baubles (Curios); 11. misc (triggers, villagers, spell parts); 12. localisation + polish.

Per milestone: write code → generate data with a script (keep generators in `tools/` so data is reproducible) →
compile → boot server and grep the log → GameTests → update the plan status. Fix what the tests find before moving on.

Generate data with Python scripts rather than hand-writing hundreds of JSON files: one table per concept (aspects,
recipes, research rows) keeps ids consistent across recipes, research and lang. Make generators idempotent — one bug
in this port came from re-reading the target's computed aspect index that already contained the addon's own additions,
which doubled every value.

## Phase 6 — Report honestly

Status legend: ✅ done and verified · 🟡 done, partly verified · ⬜ not started. For every 🟡 write *what exactly* was
not verified. "Compiles" is not verification; "server boots without codec errors" verifies data loading only; a GameTest
or a screenshot verifies behaviour. Keep a "Deviations already in code" list and a final "What is left" section
(not ported / not verified / covered only by code review).

## Practical lessons

- Read the source before assuming an old mechanic. Example: the original Wrath Cage never checked player distance even
  though it declared a range field — porting the declared-but-unused field would have changed gameplay.
- Old 1.7 values often need a scale decision (Thaumaturge item aspects are ~2–5× TC4). Pick one rule, apply it in the
  generator, document it.
- Values you could not read in the old code (a dependency's FX colours, particle sizes) are guesses until checked:
  fetch the old dependency jar (CurseMaven), decompile the class and replace "recalled" with "checked in <class>" in the
  plan. In Forgotten Relics two of three remembered values were wrong (burst size 3× too small).
- A milestone whose mechanics need a real player in the world (teleports across dimensions, held Right-click, deaths,
  a second online player) stays 🟡 after GameTests; close it with in-world scenarios from the client check
  (mc-port-verification).
- When a test fails, decide first whether the test or the mod is wrong, and record which: in this port most red tests
  were test-harness mistakes (FakePlayer never ticks, wrong expected default, stale dimension), a few were real bugs.
- Metadata items become separate ids (`NetherShard:0..6` → `wrath_shard`, `envy_shard`...).
- Reuse the original's textures and lang only if its licence allows (Forbidden Magic is WTFPL). Lang keys change, so map
  old keys to new ones in the generator and fill missing translations; check with the lang script.
- The Bash tool in this environment can fail on long quoted heredocs (`unexpected EOF while looking for matching '`).
  Write patch scripts and Java files with the Write tool, then run them.
- `cd` in a Bash call can persist as the session working directory; use absolute paths.
