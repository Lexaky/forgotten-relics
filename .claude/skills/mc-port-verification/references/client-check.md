# Automated client screenshot check

Use it when visuals must be confirmed and you cannot drive the desktop yourself. The client starts, enters a prepared
world, builds a scene with commands, takes screenshots step by step and quits. You then open the PNGs with the Read tool
and actually look at them.

## Run configuration

```groovy
clientCheck {
    client()
    gameDirectory = project.file('run-check')
    systemProperty 'yourmod.clientcheck', 'true'
    programArguments.addAll '--quickPlaySingleplayer', 'check'
}
```
Quick play needs an **existing** world: create one once with `runClient` (superflat, creative, cheats on), then before
each check `rm -rf run-check/saves/check && cp -r run/saves/<world> run-check/saves/check` and copy `run/options.txt`
so no first-launch screens appear. Run `./gradlew runClientCheck` in the background, wait for `[clientcheck] done` in
`run-check/logs/latest.log`, then read `run-check/screenshots/*.png`.

## Harness skeleton

```java
@EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
public final class ClientCheck {
    private static int ticks, step, wait;
    @SubscribeEvent public static void enable(FMLClientSetupEvent e) {
        if (Boolean.getBoolean("yourmod.clientcheck")) NeoForge.EVENT_BUS.addListener(ClientCheck::onTick);
    }
    private static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (++ticks < 100 || --wait > 0) return;          // let the world settle; then one step per pause
        MinecraftServer server = mc.getSingleplayerServer();
        switch (step++) {
            case 0 -> server.execute(() -> setUp(server, server.getPlayerList().getPlayer(mc.player.getUUID())));
            case 1 -> wait = 200;                          // chunks + block entities render
            case 2 -> view(server, mc, "0.5 201 3.5 180 12");   // tp x y z yaw pitch
            case 3 -> shot(mc, "blocks");
            // ... more views, screens, clicks
            default -> { LOGGER.info("[clientcheck] done"); mc.stop(); }
        }
    }
    static void run(MinecraftServer s, ServerPlayer p, String cmd) {
        s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), cmd);
    }
    static void shot(Minecraft mc, String name) {
        mc.options.hideGui = mc.screen == null && !name.startsWith("hand");
        Screenshot.grab(mc.gameDirectory, "check_" + name + ".png", mc.getMainRenderTarget(), 1, msg -> LOGGER.info("{}", msg.getString()));
        wait = 10;
    }
}
```
Setup commands that make scenes reproducible: `gamerule sendCommandFeedback false`, `time set noon`,
`gamerule doDaylightCycle false`, `gamerule doMobSpawning false`, `weather clear`, a `fill` floor high in the sky
(y≈200) and `fill ... air` above it, `summon ... {NoAI:1b}` for mobs. Clear chat and toasts before each shot
(`mc.gui.getChat().clearMessages(false)`, `mc.getToastManager().clear()`).

## What to show

- **Item grid screen**: a `Screen` that draws every registered item at 2× with its name (`extractRenderState` with
  `GuiGraphicsExtractor`: `g.fill`, `g.pose().pushMatrix/translate/scale`, `g.item(stack, 0, 0)`, `g.text(...)`).
  Add variants that only differ by components (tint phases, imprinted items, wands with each rod).
  Missing-texture magenta or a raw translation key is immediately visible.
- **Blocks** in all relevant states (`setblock id[prop=value]`), connected/random textures in a wall, block entities.
- **Hand rendering**: put items in hotbar slots and select them (`mc.player.getInventory().setSelectedSlot(i)`).
- **Library GUIs**: open the screen directly (`mc.setScreen(new SomeScreen(...))`); preselect state via reflection only
  inside the harness. Simulate input with `screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false)`
  and `screen.keyPressed(new KeyEvent(GLFW.GLFW_KEY_RIGHT, 0, 0))`. Coordinates are GUI-scaled; find them from a
  first screenshot (GUI scale × pixel position). Thaumaturge research recipes are bookmarks on the right page edge.
- Mobs and villagers with custom professions (texture of the profession layer).

## Reviewing

Read every screenshot. Check: textures not magenta/black, correct tint, names translated, models oriented and sized
right, GUI text inside its box. Also grep `run-check/logs/latest.log` for `Unable to load model`, `Missing texture`,
`Exception`. Record each screenshot file as the evidence for its feature.
