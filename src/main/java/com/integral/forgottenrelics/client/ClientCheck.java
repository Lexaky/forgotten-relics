package com.integral.forgottenrelics.client;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.registry.FRCreativeTabs;
import com.integral.forgottenrelics.registry.FRItems;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.client.screen.research.EntryDetailScreen;
import com.leclowndu93150.thaumaturge.client.screen.research.ThaumonomiconBrowserScreen;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.entity.BabylonWeaponEntity;
import com.integral.forgottenrelics.entity.ChaoticOrbEntity;
import com.integral.forgottenrelics.entity.CrimsonOrbEntity;
import com.integral.forgottenrelics.entity.DarkMatterOrbEntity;
import com.integral.forgottenrelics.entity.RageousMissileEntity;
import com.integral.forgottenrelics.registry.FREntities;
import com.integral.forgottenrelics.util.TooltipKeys;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.resources.ResourceKey;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Development-only visual check, enabled with {@code -Dforgottenrelics.clientcheck=true} (the {@code clientCheck}
 * run). Once in a world it takes screenshots ({@code run-check/screenshots/frcheck_*.png}) and quits. Later milestones
 * add scenes (entities, held items, Thaumonomicon pages) as new steps.
 */
@EventBusSubscriber(modid = ForgottenRelics.MODID, value = Dist.CLIENT)
public final class ClientCheck {
    private static int ticks;
    private static int step;
    private static int wait;

    private ClientCheck() {
    }

    @SubscribeEvent
    public static void enable(FMLClientSetupEvent event) {
        if (Boolean.getBoolean("forgottenrelics.clientcheck")) {
            NeoForge.EVENT_BUS.addListener(ClientCheck::onTick);
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        if (++ticks < 100 || --wait > 0) {
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        switch (step++) {
            case 0 -> server.execute(() -> setUp(server, server.getPlayerList().getPlayer(mc.player.getUUID())));
            case 1 -> pause(60);
            case 2 -> {
                clean(mc);
                mc.setScreen(new ItemGrid());
                pause(20);
            }
            case 3 -> shot(mc, "items");
            // a second frame 10 ticks later: only the animated Runic Stone (8 frames x 2 ticks) may differ (tools/check_animation.py)
            case 4 -> shot(mc, "items_later");
            case 5 -> {
                clean(mc);
                openCreativeTab(mc, FRCreativeTabs.MAIN.get());
                pause(20);
            }
            case 6 -> shot(mc, "creative_tab");
            case 7 -> {
                mc.setScreen(null);
                clean(mc);
                openBrowser(mc);
                pause(40);
            }
            case 8 -> shot(mc, "thaumonomicon");
            case 9 -> {
                openEntry(mc, "apotheosis");
                pause(40);
            }
            case 10 -> shot(mc, "entry_apotheosis");
            case 11 -> click(mc, 591, 148);
            case 12 -> shot(mc, "recipe_apotheosis");
            case 13 -> {
                openEntry(mc, "the_paradox");
                pause(40);
            }
            case 14 -> click(mc, 591, 148);
            case 15 -> shot(mc, "recipe_paradox");
            case 16 -> {
                mc.setScreen(null);
                clean(mc);
                server.execute(() -> spawnScene(server, server.getPlayerList().getPlayer(mc.player.getUUID())));
                pause(10);
            }
            case 17 -> {
                clean(mc);
                ageSceneOnClient(mc);
                pause(10);
            }
            case 18 -> shot(mc, "entities");
            case 19 -> {
                server.execute(() -> clearScene(server, server.getPlayerList().getPlayer(mc.player.getUUID())));
                pause(20);
            }
            case 20, 21, 22, 23, 24, 25 -> {
                int n = step - 22; // step is already the next one: case 19 gives -1
                server.execute(() -> effects(server.getPlayerList().getPlayer(mc.player.getUUID()), n));
                pause(2);
            }
            case 26 -> shot(mc, "effects");
            case 27 -> {
                clean(mc);
                mc.setScreen(new TooltipGrid());
                pause(20);
            }
            case 28 -> shot(mc, "tooltips");
            case 29 -> {
                openEntry(mc, "oblivion_amulet");
                pause(40);
            }
            case 30 -> shot(mc, "entry_oblivion");
            case 31, 33 -> {
                // next spread; the poem is on the third <PAGE>
                mc.screen.keyPressed(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
                pause(20);
            }
            case 32 -> shot(mc, "entry_oblivion_2");
            case 34 -> shot(mc, "entry_oblivion_3");
            case 35 -> {
                mc.setScreen(null);
                if (!ScenarioRunner.tick(mc, server)) {
                    step--; // stay here until every in-world scenario has run
                }
            }
            case 36 -> {
                ForgottenRelics.LOGGER.info("[clientcheck] done");
                mc.stop();
            }
            default -> {
            }
        }
    }

    private static void pause(int t) {
        wait = t;
    }

    private static void run(MinecraftServer server, ServerPlayer player, String command) {
        server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), command);
    }

    private static void setUp(MinecraftServer server, ServerPlayer player) {
        // always start at the check world's spawn in the overworld (the scenarios travel to other dimensions)
        run(server, player, "execute in minecraft:overworld run tp @s 0.5 201 10.5 0 0");
        for (String c : List.of("gamerule sendCommandFeedback false", "gamemode creative", "time set noon", "weather clear",
                "gamerule doDaylightCycle false", "gamerule doMobSpawning false")) {
            run(server, player, c);
        }
        // Unlock every research so the Thaumonomicon shows the whole tree and its recipe bookmarks.
        var entries = server.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY);
        entries.listElementIds().forEach(key -> ResearchManager.complete(player, key.identifier()));
    }

    private static final List<Entity> SCENE = new ArrayList<>();

    /**
     * The five rendered projectiles in a row 5 blocks in front of the player (facing south), then {@code tick freeze}
     * so that they hold still for the screenshot (the orbs would fly off or burst otherwise).
     */
    private static void spawnScene(MinecraftServer server, ServerPlayer player) {
        run(server, player, "tp @s ~ ~ ~ 0 0");
        run(server, player, "time set midnight"); // the orbs are additive glows, as in the original
        ServerLevel level = player.level();
        Vec3 base = player.getEyePosition().add(0, 0, 5);
        CrimsonOrbEntity crimson = new CrimsonOrbEntity(FREntities.CRIMSON_ORB.get(), level);
        DarkMatterOrbEntity darkMatter = new DarkMatterOrbEntity(FREntities.DARK_MATTER_ORB.get(), level);
        RageousMissileEntity missile = new RageousMissileEntity(FREntities.RAGEOUS_MISSILE.get(), level);
        ChaoticOrbEntity chaotic = new ChaoticOrbEntity(FREntities.CHAOTIC_ORB.get(), level);
        BabylonWeaponEntity weapon = new BabylonWeaponEntity(FREntities.BABYLON_WEAPON.get(), level);
        weapon.setVariety(5);
        weapon.setRotation(90F); // side view; in play the weapons point away from the caster
        weapon.setDelay(100000);
        List<Entity> row = List.of(crimson, darkMatter, missile, chaotic, weapon);
        for (int i = 0; i < row.size(); i++) {
            Entity e = row.get(i);
            ((net.minecraft.world.entity.projectile.Projectile) e).setOwner(player);
            e.setPos(base.add(3.0 - i * 1.5, i == 4 ? 0.5 : 0, 0));
            e.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(e);
            SCENE.add(e);
        }
        run(server, player, "tick freeze");
    }

    /** Frozen entities do not tick, so the client ages them by hand past the 10-tick grow/charge animation. */
    private static void ageSceneOnClient(Minecraft mc) {
        for (Entity e : mc.level.entitiesForRendering()) {
            if (ForgottenRelics.MODID.equals(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace())) {
                e.tickCount = 40;
            }
        }
    }

    private static void clearScene(MinecraftServer server, ServerPlayer player) {
        SCENE.forEach(Entity::discard);
        SCENE.clear();
        run(server, player, "tick unfreeze");
        run(server, player, "time set noon");
    }

    /**
     * TC4 effects through Thaumaturge: from left to right a lightning bolt as thin as the Soul Tome's (0.075), one as
     * wide as Telekinesis at 16 blocks (0.35), the Obelisk Drainer's pink arc, and bursts of size 1 and 2.
     */
    private static void effects(ServerPlayer player, int n) {
        ServerLevel level = player.level();
        Vec3 base = player.getEyePosition().add(0, 0, 6);
        com.integral.forgottenrelics.util.FRFx.lightning(level, base.add(4, 2, 0), base.add(4, -2, 0), 40, 0.075F);
        com.integral.forgottenrelics.util.FRFx.lightning(level, base.add(2, 2, 0), base.add(2, -2, 0), 20, 0.35F);
        TTEffects.arc(level, base.add(0, 2, 0), base.add(0, -2, 0), 1F, 0.6F, 1F, 0.4F);
        if (n == -1) {
            TTEffects.burst(level, base.x - 2, base.y, base.z, 1.0F);
            TTEffects.burst(level, base.x - 4, base.y, base.z, 2.0F);
        }
    }

    private static void openBrowser(Minecraft mc) {
        try {
            Field field = ThaumonomiconBrowserScreen.class.getDeclaredField("rememberedCategory");
            field.setAccessible(true);
            field.set(null, ForgottenRelics.id(ForgottenRelics.MODID));
        } catch (ReflectiveOperationException e) {
            ForgottenRelics.LOGGER.error("[clientcheck] could not preselect the category", e);
        }
        mc.setScreen(new ThaumonomiconBrowserScreen());
    }

    private static void openEntry(Minecraft mc, String entry) {
        var key = ResourceKey.create(IResearchEntry.REGISTRY_KEY, ForgottenRelics.id(entry));
        var holder = mc.level.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY).getOrThrow(key);
        mc.setScreen(new EntryDetailScreen(holder, ForgottenRelics.id(entry), null));
    }

    /** Left-clicks the current screen at GUI coordinates (a recipe bookmark on the right edge of the book). */
    private static void click(Minecraft mc, double x, double y) {
        mc.screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(0, 0)), false);
        pause(40);
    }

    private static void clean(Minecraft mc) {
        mc.gui.getChat().clearMessages(false);
        mc.getToastManager().clear();
    }

    /** A screenshot from inside a scenario (GUI hidden, the game keeps running). */
    static void screenshot(String name) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.hideGui = true;
        Screenshot.grab(mc.gameDirectory, "frcheck_" + name + ".png", mc.getMainRenderTarget(), 1,
                msg -> ForgottenRelics.LOGGER.info("[clientcheck] {}: {}", name, msg.getString()));
    }

    private static void shot(Minecraft mc, String name) {
        mc.options.hideGui = mc.screen == null;
        Screenshot.grab(mc.gameDirectory, "frcheck_" + name + ".png", mc.getMainRenderTarget(), 1,
                msg -> ForgottenRelics.LOGGER.info("[clientcheck] {}: {}", name, msg.getString()));
        pause(10);
    }

    /** Opens the creative inventory on a given tab; the tab is preselected through the screen's static field. */
    private static void openCreativeTab(Minecraft mc, CreativeModeTab tab) {
        try {
            Field selected = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            selected.setAccessible(true);
            selected.set(null, tab);
        } catch (ReflectiveOperationException e) {
            ForgottenRelics.LOGGER.error("[clientcheck] could not preselect the creative tab", e);
        }
        mc.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true));
    }

    /**
     * Tooltips as the original showed them: Apotheosis plain and with Shift, the Tome of Broken Fates with Ctrl and the
     * Ancient Aegis with Shift (Shift/Ctrl are forced through {@link TooltipKeys}).
     */
    static final class TooltipGrid extends Screen {
        private final List<List<Component>> columns = new ArrayList<>();

        TooltipGrid() {
            super(Component.literal("Forgotten Relics tooltips"));
        }

        @Override
        protected void init() {
            columns.clear();
            columns.add(lines(FRItems.APOTHEOSIS.get(), false, false));
            columns.add(lines(FRItems.APOTHEOSIS.get(), true, false));
            columns.add(lines(FRItems.FATE_TOME.get(), false, true));
            columns.add(lines(FRItems.ANCIENT_AEGIS.get(), true, false));
        }

        private List<Component> lines(net.minecraft.world.item.Item item, boolean shift, boolean control) {
            var oldShift = TooltipKeys.shift;
            var oldControl = TooltipKeys.control;
            TooltipKeys.shift = () -> shift;
            TooltipKeys.control = () -> control;
            try {
                return new ItemStack(item).getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(minecraft.level), minecraft.player,
                        net.minecraft.world.item.TooltipFlag.NORMAL);
            } finally {
                TooltipKeys.shift = oldShift;
                TooltipKeys.control = oldControl;
            }
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            g.fill(0, 0, width, height, 0xFF100010);
            int columnWidth = width / columns.size();
            for (int c = 0; c < columns.size(); c++) {
                int y = 4;
                for (Component line : columns.get(c)) {
                    for (var part : font.split(line, columnWidth - 6)) {
                        g.text(font, part, c * columnWidth + 3, y, 0xFFFFFFFF, true);
                        y += 9;
                    }
                }
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    /** All relics at twice their normal size with their translated names. */
    static final class ItemGrid extends Screen {
        private final List<ItemStack> stacks = new ArrayList<>();

        ItemGrid() {
            super(Component.literal("Forgotten Relics items"));
            FRItems.ITEMS.getEntries().forEach(item -> stacks.add(new ItemStack(item.get())));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
            g.fill(0, 0, width, height, 0xFF202028);
            int columns = 8;
            int cellW = width / columns;
            int cellH = 46;
            for (int i = 0; i < stacks.size(); i++) {
                int x = (i % columns) * cellW;
                int y = (i / columns) * cellH + 4;
                g.pose().pushMatrix();
                g.pose().translate(x + cellW / 2.0F - 16, y);
                g.pose().scale(2.0F, 2.0F);
                g.item(stacks.get(i), 0, 0);
                g.pose().popMatrix();
                String name = stacks.get(i).getHoverName().getString();
                if (font.width(name) > cellW - 4) {
                    name = font.plainSubstrByWidth(name, cellW - 8) + "…";
                }
                g.text(font, name, x + (cellW - font.width(name)) / 2, y + 34, 0xFFFFFFFF, true);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
