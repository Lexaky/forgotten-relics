package com.integral.forgottenrelics.client;

import com.google.gson.JsonObject;
import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.compat.CuriosBridge;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.BabylonWeaponEntity;
import com.integral.forgottenrelics.entity.ChaoticOrbEntity;
import com.integral.forgottenrelics.item.bauble.SuperpositionRingItem;
import com.integral.forgottenrelics.item.passive.XpTomeItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRItems;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import com.mojang.authlib.GameProfile;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * In-world checks with the real player of the client check ({@code runClientCheck}): the mechanics GameTests cannot
 * reach because they need a {@link ServerPlayer} that is really in the world — real key presses (hold Right-click,
 * Shift, Left-click), cross-dimension teleports, a second joined player, deaths. Each scenario logs
 * {@code [scenario] PASS name: detail} or {@code [scenario] FAIL name: reason}.
 */
final class ScenarioRunner {
    private static final BlockPos ARENA = new BlockPos(1000, 100, 1000);
    private static final BlockPos NETHER = new BlockPos(0, 100, 0);

    private record Step(int delay, boolean server, boolean cleanup, Runnable action) {
    }

    private static final class Scenario {
        final String name;
        final List<Step> steps = new ArrayList<>();
        String detail = "";

        Scenario(String name) {
            this.name = name;
        }

        Scenario client(int delay, Runnable r) {
            steps.add(new Step(delay, false, false, r));
            return this;
        }

        Scenario server(int delay, Runnable r) {
            steps.add(new Step(delay, true, false, r));
            return this;
        }

        Scenario cleanup(boolean server, Runnable r) {
            steps.add(new Step(1, server, true, r));
            return this;
        }
    }

    private static final List<Scenario> SCENARIOS = new ArrayList<>();
    private static int scenario = -1;
    private static int stepIndex;
    private static int wait;
    private static boolean failed;
    private static CompletableFuture<Void> pending;
    private static volatile Throwable error;
    static int passed;
    static int failures;

    // shared state of the scenarios
    private static MinecraftServer server;
    private static ServerPlayer twin;
    private static final List<Entity> JOINED = new ArrayList<>();
    private static final List<float[]> EXPLOSIONS = new ArrayList<>();
    private static final List<String> DAMAGE = new ArrayList<>();
    private static final List<String> SOUNDS = new ArrayList<>();
    private static boolean listening;
    static volatile int lightningPackets;

    private ScenarioRunner() {
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private static ServerPlayer sp() {
        return server.getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
    }

    private static ServerLevel overworld() {
        return server.overworld();
    }

    private static void run(String command) {
        ServerPlayer p = sp();
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), command);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    /** An iron-capped wand holding plenty of every primal (Thaumaturge wand parts). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ItemStack richWand() {
        ItemStack wand = new ItemStack(item("thaumaturge:wand"));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", "thaumaturge:iron");
        json.addProperty("rod", "thaumaturge:wood");
        wand.set(type, ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow());
        for (var aspect : VisHelper.PRIMALS) {
            wand = WandAccess.withVis(wand, aspect, 100_000);
        }
        return wand;
    }

    /** Empty inventory and curios, full health, a vis wand in the last slot, the given item in hand. */
    private static void kit(ItemStack inHand) {
        ServerPlayer p = sp();
        if (p.level().dimension() != Level.OVERWORLD) {
            arena();
            p = sp();
        }
        p.getInventory().clearContent();
        CuriosBridge.clear(p);
        p.getInventory().setSelectedSlot(0);
        p.getInventory().setItem(0, inHand);
        p.getInventory().setItem(35, richWand());
        p.setHealth(p.getMaxHealth());
        p.getFoodData().setFoodLevel(20);
        p.removeAllEffects();
        p.invulnerableTime = 0;
    }

    /** The arena is in the overworld whatever dimension the previous scenario left the player in. */
    private static void arena() {
        run("execute in minecraft:overworld run tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 0");
        run("execute in minecraft:overworld run fill " + (ARENA.getX() - 12) + " " + ARENA.getY() + " " + (ARENA.getZ() - 20) + " " + (ARENA.getX() + 12) + " " + ARENA.getY() + " "
                + (ARENA.getZ() + 40) + " minecraft:stone");
        run("execute in minecraft:overworld run fill " + (ARENA.getX() - 12) + " " + (ARENA.getY() + 1) + " " + (ARENA.getZ() - 20) + " " + (ARENA.getX() + 12) + " "
                + (ARENA.getY() + 12) + " " + (ARENA.getZ() + 40) + " minecraft:air");
        run("execute in minecraft:overworld run kill @e[type=!minecraft:player,distance=..80]");
    }

    /** A husk (does not burn) standing still at the arena offset. */
    private static Mob target(double dx, double dz, float health) {
        ServerLevel level = sp().level();
        Mob mob = EntityType.HUSK.create(level, EntitySpawnReason.COMMAND);
        mob.setPos(ARENA.getX() + 0.5 + dx, ARENA.getY() + 1, ARENA.getZ() + 0.5 + dz);
        mob.setNoAi(true);
        mob.setPersistenceRequired();
        mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(health);
        mob.setHealth(health);
        level.addFreshEntity(mob);
        return mob;
    }

    /**
     * A second player that is online and in the world: a NeoForge FakePlayer added to the server's player list and to
     * the level (a mock player on an embedded channel breaks Curios, which refuses to sync to an unnegotiated
     * connection). FakePlayer ignores teleports, so its own side of a swap is not observable.
     */
    private static ServerPlayer joinTwin(ServerLevel level) {
        ServerPlayer p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes("fr_twin".getBytes()), "fr_twin"));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).ifPresent(top.theillusivec4.curios.api.type.capability.ICuriosItemHandler::reset);
        playerList().add(p);
        if (level.getEntity(p.getUUID()) == null) {
            level.addNewPlayer(p);
        }
        return p;
    }

    /** The server's own (mutable) online-player list; {@code getPlayers()} is a read-only view. */
    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> playerList() {
        try {
            var f = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
            f.setAccessible(true);
            return (List<ServerPlayer>) f.get(server.getPlayerList());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void leaveTwin() {
        if (twin != null) {
            playerList().remove(twin);
            twin.level().removePlayerImmediately(twin, Entity.RemovalReason.DISCARDED);
            twin = null;
        }
    }

    /** Right-click as the server handles the use packet ({@code ServerPlayerGameMode.useItem}); held items start using. */
    private static void use() {
        ServerPlayer p = sp();
        p.gameMode.useItem(p, p.level(), p.getMainHandItem(), InteractionHand.MAIN_HAND);
    }

    /** Releasing Right-click (the release packet). */
    private static void release() {
        sp().releaseUsingItem();
    }

    /** Shift as the player-input packet sets it. */
    private static void sneak(boolean down) {
        sp().setShiftKeyDown(down);
    }

    private static boolean useHeld;
    private static boolean shiftHeld;
    private static boolean forwardHeld;

    private static void holdUse(boolean down) {
        Minecraft mc = Minecraft.getInstance();
        useHeld = down;
        mc.options.keyUse.setDown(down);
        if (down) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        } else if (mc.player.isUsingItem()) {
            mc.gameMode.releaseUsingItem(mc.player);
        }
    }

    private static void shift(boolean down) {
        shiftHeld = down;
        Minecraft.getInstance().options.keyShift.setDown(down);
    }

    private static void forward(boolean down) {
        forwardHeld = down;
        Minecraft.getInstance().options.keyUp.setDown(down);
    }

    private static void listen() {
        if (listening) {
            return;
        }
        listening = true;
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (!e.getLevel().isClientSide() && e.getEntity().getType().builtInRegistryHolder().key().identifier().getNamespace().equals(ForgottenRelics.MODID)) {
                synchronized (JOINED) {
                    JOINED.add(e.getEntity());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ExplosionEvent.Start e) -> {
            if (e.getLevel().isClientSide()) {
                return;
            }
            var x = e.getExplosion();
            synchronized (EXPLOSIONS) {
                EXPLOSIONS.add(new float[]{x.radius(), (float) x.center().x, (float) x.center().y, (float) x.center().z});
            }
            e.setCanceled(true); // counted, not detonated: the check world stays intact
        });
        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Post e) -> {
            if (!e.getEntity().level().isClientSide()) {
                Entity direct = e.getSource().getDirectEntity();
                synchronized (DAMAGE) {
                    DAMAGE.add(e.getEntity().getType().builtInRegistryHolder().key().identifier().getPath() + "<-"
                            + (direct == null ? "none" : direct.getType().builtInRegistryHolder().key().identifier()) + ":" + e.getInflictedDamage());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.PlayLevelSoundEvent e) -> {
            if (!e.getLevel().isClientSide() && e.getSound() != null) {
                synchronized (SOUNDS) {
                    SOUNDS.add("server " + e.getSound().value().location());
                }
            }
        });
        Minecraft.getInstance().getSoundManager().addListener((sound, accessor, range) -> {
            synchronized (SOUNDS) {
                SOUNDS.add(sound.getIdentifier().toString());
            }
        });
    }

    private static long joined(Class<?> type) {
        synchronized (JOINED) {
            return JOINED.stream().filter(type::isInstance).count();
        }
    }

    private static void build() {
        SCENARIOS.clear();
        final Vec3[] mark = new Vec3[2];
        final float[] value = new float[4];
        final Mob[] mob = new Mob[1];

        // ---- M5 baubles ---------------------------------------------------------------------------------------
        SCENARIOS.add(new Scenario("m5_equip_by_right_click_and_slot_validity")
                .server(1, () -> {
                    arena();
                    run("gamemode survival");
                    kit(new ItemStack(FRItems.ANCIENT_AEGIS.get()));
                })
                .server(5, ScenarioRunner::use)
                .server(3, ScenarioRunner::release)
                .server(2, () -> {
                    ServerPlayer p = sp();
                    check(CuriosBridge.hasBauble(p, FRItems.ANCIENT_AEGIS.get()), "right-click did not put the Aegis into a belt slot");
                    check(p.getMainHandItem().isEmpty(), "the Aegis stayed in hand");
                    // every bauble must be accepted by its Curios slot (what the Curios screen checks before placing)
                    String[][] slots = {{"superposition_ring", "ring"}, {"mining_charm", "ring"}, {"advanced_mining_charm", "ring"},
                            {"dark_sun_ring", "ring"}, {"deific_amulet", "necklace"}, {"shiny_stone", "necklace"},
                            {"oblivion_amulet", "necklace"}, {"nebulous_core", "necklace"}};
                    for (String[] s : slots) {
                        check(CuriosBridge.accepts(p, s[1], new ItemStack(item("forgottenrelics:" + s[0]))), s[0] + " refused by the " + s[1] + " slot");
                    }
                }));

        SCENARIOS.add(new Scenario("m5_aegis_owner_found_in_world")
                .server(1, () -> {
                    kit(ItemStack.EMPTY);
                    twin = joinTwin(sp().level());
                    twin.setPos(ARENA.getX() + 2.5, ARENA.getY() + 1, ARENA.getZ() + 0.5);
                    check(CuriosBridge.equip(twin, "belt", new ItemStack(FRItems.ANCIENT_AEGIS.get())), "could not equip the twin's Aegis");
                    twin.setHealth(20);
                    twin.invulnerableTime = 0;
                })
                .server(5, () -> {
                    ServerPlayer p = sp();
                    Mob husk = target(0, 3, 20);
                    p.invulnerableTime = 0;
                    twin.invulnerableTime = 0;
                    p.hurtServer(p.level(), p.level().damageSources().mobAttack(husk), 10);
                    float mine = 20 - p.getHealth();
                    // the owner's own share is covered by the GameTest bauble_ancient_aegis_redirect (a FakePlayer is invulnerable)
                    check(Math.abs(mine - 6) < 0.01, "victim took " + mine + ", expected 6 (60 % of 10: the owner 2 blocks away was found)");
                    leaveTwin();
                    p.setHealth(20);
                    p.invulnerableTime = 0;
                    p.hurtServer(p.level(), p.level().damageSources().mobAttack(husk), 10);
                    float alone = 20 - p.getHealth();
                    husk.discard();
                    check(Math.abs(alone - 10) < 0.01, "without the owner online the victim took " + alone + ", expected 10");
                    SCENARIOS.get(scenario).detail = "owner in the world: victim 6 of 10; owner gone: 10 of 10";
                })
                .cleanup(true, ScenarioRunner::leaveTwin));

        SCENARIOS.add(new Scenario("m5_superposition_swap_across_dimensions")
                .server(1, () -> {
                    kit(ItemStack.EMPTY);
                    ServerPlayer p = sp();
                    p.teleportTo(overworld(), ARENA.getX() + 0.5, ARENA.getY() + 1, ARENA.getZ() + 0.5, Set.of(), 0, 0, false);
                    check(CuriosBridge.equip(p, "ring", new ItemStack(FRItems.SUPERPOSITION_RING.get())), "no ring slot");
                    ServerLevel nether = server.getLevel(Level.NETHER);
                    twin = joinTwin(nether);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(nether).withPermission(PermissionSet.ALL_PERMISSIONS),
                            "fill " + (NETHER.getX() - 2) + " " + NETHER.getY() + " " + (NETHER.getZ() - 2) + " " + (NETHER.getX() + 2) + " " + NETHER.getY() + " "
                                    + (NETHER.getZ() + 2) + " minecraft:obsidian");
                    twin.setPos(NETHER.getX() + 0.5, NETHER.getY() + 1, NETHER.getZ() + 0.5);
                    check(CuriosBridge.equip(twin, "ring", new ItemStack(FRItems.SUPERPOSITION_RING.get())), "twin has no ring slot");
                })
                .server(5, () -> {
                    List<net.minecraft.world.entity.player.Player> owners = CuriosBridge.owners(server, FRItems.SUPERPOSITION_RING.get());
                    check(owners.size() == 2, "ring wearers found: " + owners.size());
                    mark[0] = sp().getBoundingBox().getCenter();
                    mark[1] = twin.getBoundingBox().getCenter();
                    sp().tickCount = 599; // the ring's own check runs every 600 ticks (95 %)
                })
                .server(3, () -> {
                    if (sp().level().dimension() != Level.NETHER) {
                        sp().tickCount = 599;
                    }
                })
                .server(3, () -> {
                    if (sp().level().dimension() != Level.NETHER) {
                        SuperpositionRingItem.swap(sp(), twin); // the 5 % miss twice in a row; swap as the ring would
                        SCENARIOS.get(scenario).detail = "(ring roll missed twice, swapped directly) ";
                    }
                })
                .server(3, () -> {
                    ServerPlayer p = sp();
                    check(p.level().dimension() == Level.NETHER, "player still in " + p.level().dimension().identifier());
                    check(p.getBoundingBox().getCenter().distanceTo(mark[1]) < 1.5, "player at " + p.position() + ", expected near " + mark[1]);
                    SCENARIOS.get(scenario).detail += "overworld <-> nether swapped";
                })
                .cleanup(true, () -> {
                    leaveTwin();
                    arena();
                }));

        SCENARIOS.add(new Scenario("m5_nebulous_core_dodge_teleport")
                .server(1, () -> {
                    kit(ItemStack.EMPTY);
                    check(CuriosBridge.equip(sp(), "necklace", new ItemStack(FRItems.NEBULOUS_CORE.get())), "no necklace slot");
                })
                .server(3, () -> {
                    ServerPlayer p = sp();
                    Mob husk = target(0, 3, 20);
                    int dodges = 0;
                    for (int i = 0; i < 40; i++) {
                        p.teleportTo(ARENA.getX() + 0.5, ARENA.getY() + 1, ARENA.getZ() + 0.5);
                        p.setHealth(20);
                        p.invulnerableTime = 0;
                        Vec3 before = p.position();
                        p.hurtServer(p.level(), p.level().damageSources().mobAttack(husk), 2);
                        if (p.getHealth() == 20 && p.position().distanceTo(before) > 1) {
                            dodges++;
                        }
                    }
                    husk.discard();
                    // 40 % per hit: 40 hits give 6..26 dodges with probability > 0.999
                    check(dodges >= 6 && dodges <= 26, "dodges " + dodges + " of 40");
                    SCENARIOS.get(scenario).detail = dodges + " of 40 hits dodged by teleport";
                }));

        // Thaumaturge reads vis discount from the player attribute thaumaturge:vis_discount, which Curios fills from
        // IVisDiscountGear on the player tick after an equip change
        SCENARIOS.add(new Scenario("m5_nebulous_core_discount_paid_by_a_wand")
                .server(1, () -> kit(ItemStack.EMPTY))
                .server(10, () -> { // after the previous scenario's curios are gone from the attribute
                    var aer = com.leclowndu93150.thaumaturge.api.aspect.TTAspects.AER;
                    ItemStack wand = richWand();
                    sp().getInventory().setItem(35, wand);
                    int before = VisHelper.stored(sp().getInventory().getItem(35), aer);
                    check(VisHelper.consumeFromInventory(sp(), VisHelper.cost(aer, 1000)), "10 aer not paid");
                    value[0] = before - VisHelper.stored(sp().getInventory().getItem(35), aer);
                    check(CuriosBridge.equip(sp(), "necklace", new ItemStack(FRItems.NEBULOUS_CORE.get())), "no necklace slot");
                })
                .server(10, () -> {
                    var attribute = BuiltInRegistries.ATTRIBUTE.get(Identifier.fromNamespaceAndPath("thaumaturge", "vis_discount")).orElseThrow();
                    double discount = sp().getAttributeValue(attribute);
                    var aer = com.leclowndu93150.thaumaturge.api.aspect.TTAspects.AER;
                    int before = VisHelper.stored(sp().getInventory().getItem(35), aer);
                    check(VisHelper.consumeFromInventory(sp(), VisHelper.cost(aer, 1000)), "10 aer not paid with the core");
                    value[1] = before - VisHelper.stored(sp().getInventory().getItem(35), aer);
                    check(Math.abs(discount - 0.35) < 1e-6, "vis_discount attribute " + discount + " with the core worn, expected 0.35");
                    check(value[0] == 1100, "without the core 10 aer cost " + value[0] + " centivis, expected 1100 (iron cap +10 %)");
                    check(Math.abs(value[1] - 750) <= 10, "with the core 10 aer cost " + value[1] + " centivis, expected 750 (1.10 - 0.35)");
                    SCENARIOS.get(scenario).detail = "attribute vis_discount 0.35; 10 aer cost " + (int) value[0] + " -> " + (int) value[1] + " centivis";
                }));

        // ---- M6 passive relics -----------------------------------------------------------------------------
        SCENARIOS.add(new Scenario("m6_real_death_cancelled_by_relics")
                .server(1, () -> {
                    StringBuilder d = new StringBuilder();
                    for (Item relic : List.of(FRItems.FALSE_JUSTICE.get(), FRItems.OMEGA_CORE.get())) {
                        kit(new ItemStack(relic));
                        run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5");
                        ServerPlayer p = sp();
                        p.hurtServer(p.level(), p.level().damageSources().generic(), 1000);
                        check(!p.isRemoved() && p.deathTime == 0, BuiltInRegistries.ITEM.getKey(relic).getPath() + " did not cancel a lethal hit");
                        d.append(BuiltInRegistries.ITEM.getKey(relic).getPath()).append(" saved (health ").append(p.getHealth()).append("); ");
                        p.setHealth(p.getMaxHealth());
                    }
                    ItemStack tome = new ItemStack(FRItems.FATE_TOME.get());
                    tome.set(FRDataComponents.FATE_COOLDOWN.get(), 0);
                    kit(tome);
                    ServerPlayer p = sp();
                    p.hurtServer(p.level(), p.level().damageSources().generic(), 1000);
                    check(p.getHealth() == p.getMaxHealth(), "Tome of Broken Fates did not restore health: " + p.getHealth());
                    d.append("fate_tome restored full health");
                    SCENARIOS.get(scenario).detail = d.toString();
                }));

        SCENARIOS.add(new Scenario("m6_xp_tome_right_click_and_shift")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.XP_TOME.get()));
                    sp().setExperienceLevels(30);
                })
                .server(3, () -> sneak(true))
                .server(3, ScenarioRunner::use)
                .server(2, () -> sneak(false))
                .server(40, () -> {
                    ItemStack tome = sp().getMainHandItem();
                    check(XpTomeItem.active(tome), "Shift + Right-click did not switch the tome on");
                    check(XpTomeItem.absorption(tome), "a new tome should absorb");
                    check(XpTomeItem.stored(tome) > 0 && sp().experienceLevel < 30, "nothing absorbed: stored " + XpTomeItem.stored(tome)
                            + ", level " + sp().experienceLevel);
                    value[0] = XpTomeItem.stored(tome);
                    value[1] = sp().experienceLevel;
                })
                .server(1, ScenarioRunner::use)
                .server(40, () -> {
                    ItemStack tome = sp().getMainHandItem();
                    check(!XpTomeItem.absorption(tome), "Right-click did not switch to extraction");
                    check(XpTomeItem.stored(tome) < value[0] && sp().experienceLevel > value[1], "nothing extracted: stored "
                            + XpTomeItem.stored(tome) + ", level " + sp().experienceLevel);
                    SCENARIOS.get(scenario).detail = "Shift+RMB on, absorbed " + (int) value[0] + " points (level 30 -> " + (int) value[1]
                            + "); RMB extraction, stored " + XpTomeItem.stored(tome) + ", level " + sp().experienceLevel;
                }));

        // ---- M7 utility relics ----------------------------------------------------------------------------
        SCENARIOS.add(new Scenario("m7_dimensional_mirror_return_from_nether")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.DIMENSIONAL_MIRROR.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + (ARENA.getZ() + 5) + ".5");
                    mark[0] = sp().position();
                })
                .server(5, () -> sneak(true))
                .server(3, ScenarioRunner::use)
                .server(2, () -> {
                    release();
                    sneak(false);
                })
                .server(3, () -> {
                    check(sp().getMainHandItem().has(FRDataComponents.MIRROR_TARGET.get()), "Shift + Right-click stored no target");
                    ServerLevel nether = server.getLevel(Level.NETHER);
                    sp().teleportTo(nether, NETHER.getX() + 0.5, NETHER.getY() + 1, NETHER.getZ() + 0.5, Set.of(), 0, 0, false);
                })
                .server(20, ScenarioRunner::use)
                .server(40, () -> SCENARIOS.get(scenario).detail = "after 40 ticks: using " + sp().isUsingItem() + ", remaining "
                        + sp().getUseItemRemainingTicks() + "; ")
                .server(45, () -> SCENARIOS.get(scenario).detail += "after 85 ticks: " + sp().level().dimension().identifier() + " "
                        + sp().position() + ", using " + sp().isUsingItem() + "; ")
                .server(15, ScenarioRunner::release)
                .server(5, () -> {
                    ServerPlayer p = sp();
                    check(p.level().dimension() == Level.OVERWORLD, "still in " + p.level().dimension().identifier() + " after holding the mirror ("
                            + SCENARIOS.get(scenario).detail + ")");
                    check(p.position().distanceTo(mark[0]) < 1.0, "returned to " + p.position() + ", stored " + mark[0]);
                    SCENARIOS.get(scenario).detail = "held 80 ticks in the Nether, back at the stored point";
                }));

        SCENARIOS.add(new Scenario("m7_tome_of_discord_block_and_blink")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.TELEPORTATION_TOME.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 30");
                    mark[0] = sp().position();
                })
                .server(5, ScenarioRunner::use)
                .server(2, ScenarioRunner::release)
                .server(5, () -> {
                    ServerPlayer p = sp();
                    // looking 30° down at the floor: the pointed block is a few blocks ahead
                    check(p.position().distanceTo(mark[0]) > 1.5 && p.getZ() > mark[0].z, "no teleport to the pointed block: " + p.position());
                    mark[1] = p.position();
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 0");
                })
                .server(25, () -> sneak(true))
                .server(3, ScenarioRunner::use)
                .server(2, () -> {
                    release();
                    sneak(false);
                })
                .server(5, () -> {
                    double moved = sp().position().z - (ARENA.getZ() + 0.5);
                    check(Math.abs(moved - 16) < 1.5, "Shift blink moved " + moved + " blocks, expected 16");
                    SCENARIOS.get(scenario).detail = "pointed block " + String.format("%.1f", mark[1].distanceTo(mark[0])) + " blocks, blink " + String.format("%.1f", moved);
                }));

        SCENARIOS.add(new Scenario("m7_obelisk_drainer_hold_and_interrupt")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.OBELISK_DRAINER.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 0");
                    run("setblock " + ARENA.getX() + " " + (ARENA.getY() + 1) + " " + (ARENA.getZ() + 4) + " thaumaturge:eldritch_obelisk");
                    sp().setHealth(8);
                    sp().getFoodData().setFoodLevel(10);
                    lightningPackets = 0;
                })
                .server(5, ScenarioRunner::use)
                .server(70, () -> {
                    ServerPlayer p = sp();
                    check(p.isUsingItem(), "stopped using next to the obelisk");
                    check(p.getHealth() > 8, "no drain pulse healed the player: " + p.getHealth());
                    check(p.getFoodData().getFoodLevel() > 10, "no drain pulse fed the player: " + p.getFoodData().getFoodLevel());
                    value[0] = p.getHealth();
                    p.teleportTo(ARENA.getX() + 0.5, ARENA.getY() + 1, ARENA.getZ() - 13.5); // 17.5 blocks from the obelisk
                })
                .server(10, () -> {
                    check(!sp().isUsingItem(), "still draining 16+ blocks away from the obelisk");
                    check(lightningPackets > 0, "no lightning packet reached the client");
                    SCENARIOS.get(scenario).detail = "health 8 -> " + value[0] + ", interrupted at 16 blocks, " + lightningPackets + " lightning packets";
                })
                .cleanup(true, ScenarioRunner::release)
                .cleanup(true, () -> run("setblock " + ARENA.getX() + " " + (ARENA.getY() + 1) + " " + (ARENA.getZ() + 4) + " minecraft:air")));

        SCENARIOS.add(new Scenario("m7_paradox_left_click")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.PARADOX.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 0");
                    mob[0] = target(0, 2, 300);
                    DAMAGE.clear();
                })
                .server(25, () -> {
                    sp().attack(mob[0]); // what the attack packet calls once the attack cooldown has recharged
                    sp().swing(InteractionHand.MAIN_HAND, true);
                })
                .server(5, () -> {
                    float lost = 300 - mob[0].getHealth();
                    mob[0].discard();
                    check(lost >= 2, "a Left-click with The Paradox took only " + lost + " health");
                    SCENARIOS.get(scenario).detail = "one hit took " + lost + " health";
                }));

        // ---- M8 battle tomes ------------------------------------------------------------------------------
        SCENARIOS.add(new Scenario("m8_tome_of_predestiny_grab_and_shift_throw")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.TELEKINESIS_TOME.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 10");
                    mob[0] = target(0, 6, 300);
                    mob[0].setNoAi(false);
                    mark[0] = mob[0].position();
                })
                .server(10, ScenarioRunner::use)
                .server(4, ScenarioRunner::use)
                .server(4, ScenarioRunner::use)
                .server(4, ScenarioRunner::use)
                .server(4, ScenarioRunner::use)
                .server(4, () -> {
                    use();
                    var st = sp().getMainHandItem().get(FRDataComponents.TELEKINESIS.get());
                    SCENARIOS.get(scenario).detail = "after holding: using " + sp().isUsingItem() + ", state " + st + ", husk moved "
                            + mob[0].position().subtract(mark[0]);
                    sneak(true);
                })
                .server(3, () -> {
                    sp().swing(InteractionHand.MAIN_HAND, true);
                    value[0] = (float) mob[0].getDeltaMovement().horizontalDistance(); // set by the throw, before friction
                })
                .server(2, () -> {
                    release();
                    sneak(false);
                })
                .server(1, () -> {
                    check(mob[0].getHealth() < 300, "no lightning on the held target (" + SCENARIOS.get(scenario).detail + ")");
                    check(value[0] > 1.0F, "Shift + Left-click did not throw the target: speed " + value[0]);
                    SCENARIOS.get(scenario).detail = "lightning took " + (300 - mob[0].getHealth()) + ", thrown at " + value[0] + " blocks/tick";
                    mob[0].discard();
                }));

        // the client half of the Edict: while the tome is in use, the client zeroes the caster's own motion every tick
        SCENARIOS.add(new Scenario("m8_edict_holds_the_caster_still")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.SOUL_TOME.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 0");
                })
                .server(5, ScenarioRunner::use)
                .client(1, () -> {
                    Minecraft.getInstance().player.startUsingItem(InteractionHand.MAIN_HAND);
                    useHeld = true;
                })
                .client(2, () -> {
                    mark[0] = Minecraft.getInstance().player.position();
                    Minecraft.getInstance().player.setDeltaMovement(0.4, 0, 0);
                })
                .client(1, () -> Minecraft.getInstance().player.setDeltaMovement(0.4, 0, 0))
                .client(1, () -> Minecraft.getInstance().player.setDeltaMovement(0.4, 0, 0))
                .client(1, () -> Minecraft.getInstance().player.setDeltaMovement(0.4, 0, 0))
                .client(1, () -> {
                    double moved = Minecraft.getInstance().player.position().subtract(mark[0]).horizontalDistance();
                    check(Minecraft.getInstance().player.isUsingItem(), "client stopped using the tome");
                    check(moved < 0.05, "pushed 0.4 blocks/tick for 4 ticks while casting, moved " + moved);
                    SCENARIOS.get(scenario).detail = "pushed while casting: moved " + String.format("%.3f", moved);
                    useHeld = false;
                    Minecraft.getInstance().player.stopUsingItem();
                })
                .server(1, ScenarioRunner::release)
                .client(3, () -> {
                    mark[1] = Minecraft.getInstance().player.position();
                    Minecraft.getInstance().player.setDeltaMovement(0.4, 0, 0);
                })
                .client(2, () -> {
                    double moved = Minecraft.getInstance().player.position().subtract(mark[1]).horizontalDistance();
                    check(moved > 0.2, "not moving after casting either: " + moved);
                    SCENARIOS.get(scenario).detail += "; same push after release moved " + String.format("%.2f", moved);
                }));

        SCENARIOS.add(new Scenario("m8_primal_chaos_orbs_in_flight")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.CHAOS_TOME.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 5");
                    for (int i = -2; i <= 2; i++) {
                        target(i * 2, 25, 200);
                    }
                    synchronized (JOINED) {
                        JOINED.clear();
                    }
                    synchronized (EXPLOSIONS) {
                        EXPLOSIONS.clear();
                    }
                })
                .server(5, ScenarioRunner::use)
                .client(30, () -> ClientCheck.screenshot("scenario_chaos_orbs"))
                .server(10, ScenarioRunner::release)
                .server(80, () -> {
                    long orbs = joined(ChaoticOrbEntity.class);
                    int blasts;
                    synchronized (EXPLOSIONS) {
                        blasts = EXPLOSIONS.size();
                    }
                    check(orbs > 0, "no orb launched");
                    check(blasts > 0, "no orb exploded on impact");
                    SCENARIOS.get(scenario).detail = orbs + " orbs, " + blasts + " impact explosions (recorded, not detonated)";
                    run("kill @e[type=minecraft:husk]");
                }));

        SCENARIOS.add(new Scenario("m8_apotheosis_weapons_in_flight")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.APOTHEOSIS.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 8");
                    mob[0] = target(0, 14, 2000);
                    synchronized (JOINED) {
                        JOINED.clear();
                    }
                    synchronized (DAMAGE) {
                        DAMAGE.clear();
                    }
                })
                .server(5, ScenarioRunner::use)
                .client(25, () -> ClientCheck.screenshot("scenario_babylon_charging"))
                .server(5, ScenarioRunner::release)
                .client(20, () -> ClientCheck.screenshot("scenario_babylon_launched"))
                .server(60, () -> {
                    long weapons = joined(BabylonWeaponEntity.class);
                    long hits;
                    synchronized (DAMAGE) {
                        hits = DAMAGE.stream().filter(d -> d.startsWith("husk<-forgottenrelics:babylon_weapon")).count();
                    }
                    check(weapons > 0, "no weapon summoned");
                    check(hits > 0, "no weapon hit the target 14 blocks ahead (" + weapons + " summoned)");
                    SCENARIOS.get(scenario).detail = weapons + " weapons, " + hits + " hits, target lost " + (2000 - mob[0].getHealth());
                    mob[0].discard();
                }));

        // ---- M2 sounds ------------------------------------------------------------------------------------
        SCENARIOS.add(new Scenario("m2_sounds_played_on_the_client")
                .server(1, () -> {
                    kit(new ItemStack(FRItems.LUNAR_FLARES.get()));
                    run("tp @s " + ARENA.getX() + ".5 " + (ARENA.getY() + 1) + " " + ARENA.getZ() + ".5 0 20"); // looking at the floor: the flares need a pointed block
                    mob[0] = target(0, 8, 2000);
                })
                .server(5, ScenarioRunner::use)
                .server(40, ScenarioRunner::release)
                .client(60, () -> {
                    List<String> ours;
                    synchronized (SOUNDS) {
                        ours = SOUNDS.stream().filter(s -> s.contains(ForgottenRelics.MODID + ":")).distinct().toList();
                    }
                    int total;
                    synchronized (SOUNDS) {
                        total = SOUNDS.size();
                    }
                    check(ours.contains(ForgottenRelics.MODID + ":starfall"), "starfall not heard on the client; ours " + ours + " of " + total + " sounds");
                    check(ours.contains(ForgottenRelics.MODID + ":lunar_flare"), "lunar_flare not heard on the client; ours " + ours);
                    SCENARIOS.get(scenario).detail = "heard " + ours;
                })
                .cleanup(true, () -> {
                    if (mob[0] != null) {
                        mob[0].discard();
                    }
                    run("gamemode creative");
                    run("execute in minecraft:overworld run tp @s 0.5 201 10.5 0 0");
                }));
    }

    /** Runs one tick of the scenario queue; returns true when every scenario has finished. */
    static boolean tick(Minecraft mc, MinecraftServer srv) {
        // the check window is usually not focused: without this the game pauses (and the integrated server with it)
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        mc.options.keyUse.setDown(useHeld);
        mc.options.keyShift.setDown(shiftHeld);
        mc.options.keyUp.setDown(forwardHeld);
        if (scenario == -1) {
            server = srv;
            listen();
            build();
            scenario = 0;
            stepIndex = 0;
            wait = 5;
        }
        if (pending != null) {
            if (!pending.isDone()) {
                return false;
            }
            pending = null;
        }
        if (error != null) {
            fail(error);
            error = null;
        }
        if (scenario >= SCENARIOS.size()) {
            ForgottenRelics.LOGGER.info("[scenario] done: {} passed, {} failed", passed, failures);
            return true;
        }
        if (--wait > 0) {
            return false;
        }
        Scenario s = SCENARIOS.get(scenario);
        while (stepIndex < s.steps.size() && failed && !s.steps.get(stepIndex).cleanup()) {
            stepIndex++;
        }
        if (stepIndex >= s.steps.size()) {
            if (!failed) {
                passed++;
                ForgottenRelics.LOGGER.info("[scenario] PASS {}: {}", s.name, s.detail);
            }
            failed = false;
            scenario++;
            stepIndex = 0;
            wait = 5;
            return false;
        }
        Step step = s.steps.get(stepIndex++);
        wait = stepIndex < s.steps.size() ? s.steps.get(stepIndex).delay() : 1;
        Runnable guarded = () -> {
            try {
                step.action().run();
            } catch (Throwable t) {
                if (!step.cleanup()) {
                    error = t;
                } else {
                    ForgottenRelics.LOGGER.warn("[scenario] cleanup of {} failed", s.name, t);
                }
            }
        };
        if (step.server()) {
            pending = srv.submit(guarded);
        } else {
            guarded.run();
            if (error != null) {
                fail(error);
                error = null;
            }
        }
        return false;
    }

    private static void fail(Throwable t) {
        Scenario s = SCENARIOS.get(scenario);
        if (!failed) {
            failures++;
            failed = true;
            ForgottenRelics.LOGGER.error("[scenario] FAIL {}: {}", s.name, t instanceof AssertionError ? t.getMessage() : t.toString());
            if (!(t instanceof AssertionError)) {
                ForgottenRelics.LOGGER.error("[scenario] stack", t);
            }
            useHeld = shiftHeld = forwardHeld = false;
        }
    }
}
