package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;

import com.google.gson.JsonObject;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.CuriosBridge;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.event.FRCraftingWarp;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRItems;
import com.integral.forgottenrelics.util.SuperpositionHandler;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.api.infusion.InfusionCraftedEvent;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import top.theillusivec4.curios.api.CuriosApi;

/** M4: shared systems — wand vis, curios, damage sources, boss tag, crafting warp, teleport. */
final class FRSystemsTests {
    private FRSystemsTests() {
    }

    static void register(FRGameTests.Registrar r) {
        r.add("systems/vis_add_and_consume", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack wand = wand();
            player.getInventory().setItem(5, wand);
            check(helper, VisHelper.wands(player).size() == 1, "wand not found in inventory");
            int capacity = VisHelper.capacity(wand);
            check(helper, capacity > 0, "wand capacity " + capacity);
            VisHelper.addVis(wand, TTAspects.AER, 5);
            check(helper, VisHelper.stored(wand, TTAspects.AER) == 500, "addVis(5) stored " + VisHelper.stored(wand, TTAspects.AER) + " centivis");
            // too expensive: nothing is paid
            check(helper, !VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, 100_000)), "paid an impossible cost");
            check(helper, VisHelper.stored(wand, TTAspects.AER) == 500, "failed payment changed the wand");
            // aspect the wand does not hold: nothing is paid
            check(helper, !VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, 100, TTAspects.IGNIS, 100)),
                    "paid without ignis");
            check(helper, VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, 300)), "could not pay 300 aer");
            int left = VisHelper.stored(wand, TTAspects.AER);
            // the iron cap costs 110 % (as in TC4), so 300 centivis take 330
            check(helper, left == 170, "after paying 300 centivis with an iron cap the wand holds " + left + ", expected 170");
            // fill to capacity: no room any more for this aspect
            VisHelper.addVis(wand, TTAspects.AER, capacity);
            check(helper, VisHelper.stored(wand, TTAspects.AER) == capacity, "addVis is not clamped to capacity");
            check(helper, VisHelper.randomWandWithRoom(VisHelper.wands(player), TTAspects.AER, helper.getLevel().getRandom()).isEmpty(),
                    "a full wand counted as having room");
            check(helper, !VisHelper.randomWandWithRoom(VisHelper.wands(player), TTAspects.IGNIS, helper.getLevel().getRandom()).isEmpty(),
                    "an empty wand counted as full");
            helper.succeed();
        });

        r.add("systems/curios_bauble", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            initSlots(player);
            check(helper, !CuriosBridge.hasBauble(player, FRItems.SUPERPOSITION_RING.get()), "ring before equipping");
            check(helper, CuriosBridge.equip(player, "ring", new ItemStack(FRItems.SUPERPOSITION_RING.get())), "no free ring slot");
            check(helper, CuriosBridge.hasBauble(player, FRItems.SUPERPOSITION_RING.get()), "ring not detected after equipping");
            check(helper, CuriosBridge.countEquipped(player, FRItems.SUPERPOSITION_RING.get()) == 1, "ring count");
            check(helper, CuriosBridge.equip(player, "ring", new ItemStack(FRItems.SUPERPOSITION_RING.get())), "no second ring slot");
            check(helper, CuriosBridge.countEquipped(player, FRItems.SUPERPOSITION_RING.get()) == 2, "two rings not counted");
            check(helper, CuriosBridge.equip(player, "necklace", new ItemStack(FRItems.DEIFIC_AMULET.get())), "no necklace slot");
            check(helper, CuriosBridge.equip(player, "belt", new ItemStack(FRItems.ANCIENT_AEGIS.get())), "no belt slot");
            check(helper, CuriosBridge.hasBauble(player, FRItems.ANCIENT_AEGIS.get()), "belt not detected");
            check(helper, CuriosBridge.findEquipped(player, FRItems.DEIFIC_AMULET.get()).is(FRItems.DEIFIC_AMULET.get()), "findEquipped");
            helper.succeed();
        });

        r.add("systems/damage_and_bosses", helper -> {
            var level = helper.getLevel();
            check(helper, FRDamageSources.isAbsolute(FRDamageSources.fate(level)), "fate not absolute");
            check(helper, FRDamageSources.isAbsolute(FRDamageSources.oblivion(level)), "oblivion not absolute");
            check(helper, FRDamageSources.isAbsolute(level.damageSources().fellOutOfWorld()), "void not absolute");
            check(helper, FRDamageSources.isAbsolute(level.damageSources().starve()), "starvation not absolute");
            check(helper, !FRDamageSources.isAbsolute(FRDamageSources.telekineticLightning(level, null)), "lightning absolute");
            check(helper, !FRDamageSources.isAbsolute(level.damageSources().generic()), "generic absolute");
            DamageSource justice = FRDamageSources.trueDamage(level, null);
            check(helper, justice.is(com.integral.forgottenrelics.registry.FRDamageTypes.TRUE_DAMAGE) && FRDamageSources.isAbsolute(justice),
                    "true damage type");
            var wither = EntityType.WITHER.create(level, EntitySpawnReason.COMMAND);
            var zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            check(helper, BotaniaBridge.isRelicBoss(wither), "wither (c:bosses) not a relic boss");
            check(helper, !BotaniaBridge.isRelicBoss(zombie), "zombie is a relic boss");
            var gaia = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("botania", "doppleganger"));
            check(helper, gaia.builtInRegistryHolder().is(com.integral.forgottenrelics.registry.FRTags.RELIC_BOSSES), "gaia not a relic boss");
            var warden = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "eldritch_warden"));
            check(helper, warden.builtInRegistryHolder().is(com.integral.forgottenrelics.registry.FRTags.RELIC_BOSSES), "eldritch warden not a relic boss");
            check(helper, !BotaniaBridge.isTruePlayer(zombie), "zombie is a true player");
            helper.succeed();
        });

        r.add("systems/crafting_warp", helper -> {
            ServerPlayer player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "fr-warp-test"));
            try {
                var warp = WarpHelper.getWarp(player);
                int permanent = warp.get(WarpType.PERMANENT);
                int normal = warp.get(WarpType.NORMAL);
                NeoForge.EVENT_BUS.post(new InfusionCraftedEvent(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), player,
                        new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK), new ItemStack(FRItems.FALSE_JUSTICE.get())));
                check(helper, warp.get(WarpType.PERMANENT) == permanent + 5 && warp.get(WarpType.NORMAL) == normal + 5,
                        "false justice (10) gave permanent " + (warp.get(WarpType.PERMANENT) - permanent) + ", normal " + (warp.get(WarpType.NORMAL) - normal));
                NeoForge.EVENT_BUS.post(new InfusionCraftedEvent(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), player,
                        new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK), new ItemStack(FRItems.OBELISK_DRAINER.get())));
                check(helper, warp.get(WarpType.PERMANENT) == permanent + 6 && warp.get(WarpType.NORMAL) == normal + 7,
                        "obelisk drainer (3) split wrong");
                NeoForge.EVENT_BUS.post(new InfusionCraftedEvent(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), player,
                        new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK), new ItemStack(FRItems.XP_TOME.get())));
                check(helper, warp.get(WarpType.PERMANENT) == permanent + 6 && warp.get(WarpType.NORMAL) == normal + 7,
                        "xp tome must not give crafting warp");
                check(helper, FRCraftingWarp.warpFor(new ItemStack(FRItems.PARADOX.get())) == 8, "paradox crafting warp");
            } finally {
                net.neoforged.neoforge.common.util.FakePlayerFactory.unloadLevel(helper.getLevel());
            }
            helper.succeed();
        });

        r.add("systems/valid_teleport", helper -> {
            // floor at relative y=1, target column in the air above it: the search goes down to the floor
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    helper.setBlock(x, 1, z, Blocks.STONE);
                }
            }
            var stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(0.5, 2, 0.5));
            BlockPos floor = helper.absolutePos(new BlockPos(3, 1, 3));
            boolean ok = SuperpositionHandler.validTeleport(stand, floor.getX() + 0.5, floor.getY() + 4, floor.getZ() + 0.5, helper.getLevel());
            check(helper, ok, "teleport over a floor failed");
            check(helper, stand.blockPosition().equals(floor.above()), "landed at " + stand.blockPosition() + ", expected " + floor.above());
            helper.succeed();
        });
    }

    /** A mock player never "joins", so Curios has not built its slots yet; real players get them on login. */
    static void initSlots(Player player) {
        CuriosApi.getCuriosInventory(player).ifPresent(inv -> {
            if (inv.getCurios().isEmpty()) {
                inv.reset();
            }
        });
    }

    /** An iron-capped wooden wand (Thaumaturge's own parts). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static ItemStack wand() {
        ItemStack wand = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand")));
        DataComponentType type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("thaumaturge", "wand_parts"));
        JsonObject json = new JsonObject();
        json.addProperty("cap", "thaumaturge:iron");
        json.addProperty("rod", "thaumaturge:wood");
        wand.set(type, ((Codec) type.codec()).parse(JsonOps.INSTANCE, json).getOrThrow());
        return wand;
    }
}
