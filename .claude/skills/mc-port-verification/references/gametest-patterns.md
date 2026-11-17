# GameTest patterns (NeoForge 26.1.2)

## Registration scaffold

```java
// Mod constructor
if (Boolean.getBoolean("yourmod.gametest")) modBus.addListener(YourGameTests::register);

public final class YourGameTests {
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath("thaumaturge", "empty_5x5x7"); // any existing structure

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(YourMod.id("default"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Registrar r = new Registrar(event, env);
        r.add("data/recipes_loaded", helper -> { ...; helper.succeed(); });
        r.addIsolated("villager/heretic_job", 400, helper -> { ... });
    }

    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        void add(String name, Consumer<GameTestHelper> body) { add(name, 40, body); }
        void add(String name, int maxTicks, Consumer<GameTestHelper> body) {
            var data = new TestData<>(env, STRUCTURE, maxTicks, 0, true, Rotation.NONE);
            event.registerTest(YourMod.id(name.replace('/', '_')), new InlineTest(data, body));
        }
        /** Own environment = own batch: no other test runs next to it. */
        void addIsolated(String name, int maxTicks, Consumer<GameTestHelper> body) {
            var alone = event.registerEnvironment(YourMod.id("isolated_" + name.replace('/', '_')),
                    new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(YourMod.id(name.replace('/', '_')),
                    new InlineTest(new TestData<>(alone, STRUCTURE, maxTicks, 0, true, Rotation.NONE), body));
        }
    }

    static final class InlineTest extends GameTestInstance {
        private static final MapCodec<InlineTest> CODEC = MapCodec.unit((InlineTest) null); // never serialized
        private final Consumer<GameTestHelper> body;
        InlineTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, Consumer<GameTestHelper> body) { super(data); this.body = body; }
        @Override public void run(GameTestHelper helper) { body.accept(helper); }
        @Override public MapCodec<? extends GameTestInstance> codec() { return CODEC; }
        @Override protected MutableComponent typeDescription() { return Component.literal("yourmod:inline"); }
    }

    static void check(GameTestHelper h, boolean ok, String msg) { if (!ok) throw h.assertionException(Component.literal(msg)); }
}
```
`TestData(env, structure, maxTicks, setupTicks, required, rotation)`. Split tests over several classes by feature, each
with `static void register(Registrar r)`.

## What to test, by kind of content

| Content | Test shape |
|---|---|
| Datapack entries (aspects, research, recipes, enchantments, trades) | look each id up in `registryAccess().lookupOrThrow(KEY)` / `getRecipeManager().byKey(...)`; assert key properties (compound aspect has 2 components, enchantment not in `#in_enchanting_table`, exclusivity) |
| Generated content of a library (e.g. Thaumaturge wand assembly recipes) | assert the generated id exists |
| Item aspects / data maps | `AspectIndexAccess.of(item).amountOf(key, registryAccess)` equals the expected number |
| Tool/rod/tick logic | build the stack (components via codec), call the hook directly with a mock player, assert components |
| Block behaviour | `helper.setBlock`, `helper.useBlock(pos, player)` / `randomTick`, `helper.assertBlockState`, `helper.runAfterDelay` |
| Block entity machines | place source + machine, `helper.succeedWhen(() -> check(...))` over ticks |
| Mob drops / loot | spawn the mob, kill it with a damage source from a player holding the item, collect drops (DropCapture), repeat N times |
| Random effects | statistical check below |
| Event handlers | trigger the vanilla action (break, hurt, eat) or post the event yourself when the action can't be simulated |
| Recipes end-to-end | `recipe.value().matches(input, level)` + `assemble`; for library machines, drive the machine's public API |
| Villager professions/trades | isolated batch: place job site, spawn villager, wait for profession, inspect offers |
| Spells | `Spells.analyze(...).problems()` is empty; `Spells.cast(...)` then assert on the target |

## Statistical check

```java
static int trials(int n, BooleanSupplier trial) { int hits = 0; for (int i = 0; i < n; i++) if (trial.getAsBoolean()) hits++; return hits; }
static void expect(GameTestHelper h, int hits, int n, double p, String what) {
    double mean = n * p, sigma = Math.sqrt(n * p * (1 - p));
    check(h, Math.abs(hits - mean) <= 5 * sigma + 1, what + ": " + hits + "/" + n + ", expected about " + Math.round(mean));
}
```
Pick N so that the wrong-but-plausible alternative formula is > 5σ away. Call the handler's chance function in a loop
when spawning N mobs would be slow, and add one end-to-end test that the handler is wired at all.

## DropCapture

```java
final class DropCapture {
    private final ServerLevel level; private final List<ItemStack> captured = new ArrayList<>();
    static List<ItemStack> during(ServerLevel level, Runnable action) {
        DropCapture c = new DropCapture(level); NeoForge.EVENT_BUS.register(c);
        try { action.run(); } finally { NeoForge.EVENT_BUS.unregister(c); } return c.captured;
    }
    @SubscribeEvent public void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel() == level && e.getEntity() instanceof ItemEntity item) { captured.add(item.getItem().copy()); e.setCanceled(true); }
    }
}
```
Use it for drops in another dimension (`server.getLevel(Level.NETHER)`), block-break drops, and to keep repeated
trials from littering the test area.

## Players

| Need | Use |
|---|---|
| hold an item, call an item hook, basic inventory | `helper.makeMockPlayer(GameType.SURVIVAL)` |
| riding an entity | mock player (FakePlayer can't ride) |
| a real `ServerPlayer` (sync packets, Curios slots, knowledge sync) | `FakePlayerFactory.get(level, new GameProfile(uuid, name))`, position it with `setPos` |
| damage events on the player | not FakePlayer early on (invulnerable); post the event or use a mob |
| creative-only behaviour | `makeMockPlayer(GameType.CREATIVE)` |

## Progression-gated content

For research-like systems, reproduce what a player does using the public API (start the entry, grant knowledge/pool
points, solve the note) and assert each stage advances; fall back to the library's internal completion call only to
set up *other* tests that just need the research done. This caught that Thaumaturge theory rows need a solved note,
which a "grant knowledge and assert" test would have missed.

## Running

```bash
export JAVA_HOME=<jdk25>; ./gradlew runGameTestServer > build/gametest.log 2>&1
grep -E "passed|failed|required tests" build/gametest.log | tail
```
Run it in the background when it takes more than ~2 minutes. A failing test prints its name and your message; a
timeout means `succeed()` was never reached (often an exception inside `runAfterDelay` — check the stack trace above).
