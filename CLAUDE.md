# Forgotten Relics port (NeoForge 26.1.2, Thaumaturge 1.1.0, Botania Revived 2.0.2)

- Plan: PORTING_PLAN.md. Code map: PROJECT_MAP.md. Status: PROGRESS.md (update after every milestone, Russian).
- Original 1.7.10 source: reference/original (read only). Research/recipe data: tools/data/original_research.json.
- Skills: .claude/skills (legacy-mod-porting, neoforge-26-1-api, thaumcraft-to-thaumaturge, mc-port-verification).
- Previous port with working build/test patterns: A:\Projects\Java\forbidden-magic.
- JDK 25: C:\Users\lesha\.jdks\openjdk-25.0.2 (set JAVA_HOME for every gradle call).
- Botania API lives in package velrondevs.botania; call it only from compat/BotaniaBridge.
- Thaumaturge internal classes (content.*) only from compat/*.
- A milestone is done only after its check passes (server boot, GameTest, client screenshot), never on compile alone.
- Generate JSON with tools/*.py, not by hand.
