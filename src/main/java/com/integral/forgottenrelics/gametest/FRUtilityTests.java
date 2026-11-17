package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;
import static com.integral.forgottenrelics.gametest.FRSystemsTests.wand;

import com.integral.forgottenrelics.compat.ObeliskBridge;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.utility.DimensionalMirrorItem;
import com.integral.forgottenrelics.item.utility.ObeliskDrainerItem;
import com.integral.forgottenrelics.item.utility.ParadoxItem;
import com.integral.forgottenrelics.item.utility.TeleportationTomeItem;
import com.integral.forgottenrelics.item.utility.WeatherStoneItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRItems;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** M7: utility relics. */
final class FRUtilityTests {
    private FRUtilityTests() {
    }

    /** A wand holding plenty of every primal (written past the cap's capacity on purpose). */
    static ItemStack richWand() {
        ItemStack wand = wand();
        for (var aspect : VisHelper.PRIMALS) {
            wand = WandAccess.withVis(wand, aspect, 100_000);
        }
        return wand;
    }

    static void register(FRGameTests.Registrar r) {
        r.add("utility/dimensional_mirror", helper -> {
            // FakePlayer ignores teleports; a mock player covers the same-dimension path
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            {
                BlockPos home = helper.absolutePos(new BlockPos(1, 2, 1));
                player.teleportTo(home.getX() + 0.7, home.getY() + 0.2, home.getZ() + 0.7);
                ItemStack mirror = new ItemStack(FRItems.DIMENSIONAL_MIRROR.get());
                DimensionalMirrorItem.store(mirror, player);
                var target = mirror.get(FRDataComponents.MIRROR_TARGET.get());
                check(helper, target != null && target.pos().equals(new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ()))
                        && target.dimension().equals(helper.getLevel().dimension()), "stored " + target);
                check(helper, mirror.getItem().isFoil(mirror), "a written mirror must glint");
                player.teleportTo(home.getX() + 3.5, home.getY() + 3, home.getZ() + 3.5);
                check(helper, DimensionalMirrorItem.teleport(player, mirror), "teleport failed");
                Vec3 expected = new Vec3(target.pos().getX() + 0.5, target.pos().getY() + 0.5, target.pos().getZ() + 0.5);
                check(helper, player.position().distanceTo(expected) < 1e-3, "landed at " + player.position() + ", expected " + expected);
            }
            helper.succeed();
        });

        r.add("utility/weather_stone", helper -> {
            var server = helper.getLevel().getServer();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            try {
                // isRaining() follows the rain level, which fades in over ticks; the weather flag changes at once
                server.setWeatherParameters(0, 6000, true, false);
                check(helper, server.getWeatherData().isRaining(), "could not start rain");
                check(helper, !WeatherStoneItem.dispel(helper.getLevel(), player), "dispelled without vis");
                check(helper, server.getWeatherData().isRaining(), "rain stopped without vis");
                ItemStack wand = richWand();
                player.getInventory().setItem(0, wand);
                check(helper, WeatherStoneItem.dispel(helper.getLevel(), player), "not dispelled with vis");
                check(helper, !server.getWeatherData().isRaining(), "still raining");
                check(helper, VisHelper.stored(wand, com.leclowndu93150.thaumaturge.api.aspect.TTAspects.TERRA) < 100_000, "terra not paid");
            } finally {
                server.setWeatherParameters(6000, 0, false, false);
            }
            helper.succeed();
        });

        r.add("utility/discord_tome_blink", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().setItem(5, richWand()); // slot 0 is the main hand, the tome goes there below
            Vec3 start = helper.absoluteVec(new Vec3(0.5, 2, 2.5));
            player.setPos(start);
            player.setYRot(-90); // looking towards +x
            player.setXRot(0);
            player.setShiftKeyDown(true);
            check(helper, TeleportationTomeItem.cast(helper.getLevel(), player), "shift cast failed");
            double moved = Math.hypot(player.getX() - start.x, player.getZ() - start.z);
            check(helper, Math.abs(moved - 16) < 0.05 && player.getX() > start.x, "blinked " + moved + " blocks, expected 16 along +x");
            check(helper, !TeleportationTomeItem.cast(helper.getLevel(), helper.makeMockPlayer(GameType.SURVIVAL)), "cast without vis");
            // cooldown: use() sets 20, ticks count it down
            ItemStack tome = new ItemStack(FRItems.TELEPORTATION_TOME.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, tome);
            tome.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(helper, tome.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0) == 20, "cooldown not set");
            tome.getItem().inventoryTick(tome, helper.getLevel(), player, null);
            check(helper, tome.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0) == 19, "cooldown not counting down");
            helper.succeed();
        });

        r.add("utility/discord_tome_swap", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.getInventory().setItem(0, richWand());
            Vec3 start = helper.absoluteVec(new Vec3(0.5, 2, 2.5));
            player.setPos(start);
            player.setYRot(-90);
            player.setXRot(0);
            var zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(4.5, 2, 2.5));
            zombie.setNoAi(true);
            // aim at the zombie's eyes
            Vec3 eye = player.getEyePosition();
            Vec3 to = zombie.getBoundingBox().getCenter();
            player.setXRot((float) Math.toDegrees(-Math.atan2(to.y - eye.y, to.x - eye.x)));
            Vec3 zombieCenter = zombie.getBoundingBox().getCenter();
            Vec3 playerCenter = player.getBoundingBox().getCenter();
            check(helper, TeleportationTomeItem.cast(helper.getLevel(), player), "swap cast failed");
            check(helper, player.position().distanceTo(zombieCenter) < 1e-3, "player at " + player.position() + ", expected " + zombieCenter);
            check(helper, zombie.position().distanceTo(playerCenter) < 1e-3, "zombie at " + zombie.position() + ", expected " + playerCenter);
            zombie.discard();
            helper.succeed();
        });

        r.add("utility/obelisk_drainer", helper -> {
            var obeliskBlock = BuiltInRegistries.BLOCK.getValue(ObeliskBridge.OBELISK);
            check(helper, obeliskBlock != null && obeliskBlock != net.minecraft.world.level.block.Blocks.AIR, "no thaumaturge:eldritch_obelisk block");
            helper.setBlock(new BlockPos(2, 1, 2), obeliskBlock);
            BlockPos obelisk = helper.absolutePos(new BlockPos(2, 1, 2));
            check(helper, ObeliskBridge.isObelisk(helper.getLevel(), obelisk), "the block has no obelisk block entity: "
                    + helper.getLevel().getBlockEntity(obelisk));
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setPos(helper.absoluteVec(new Vec3(2.5, 2, 4.5)));
            check(helper, obelisk.equals(ObeliskBridge.findNear(player, 16)), "obelisk not found nearby");
            Player far = helper.makeMockPlayer(GameType.SURVIVAL);
            far.setPos(helper.absoluteVec(new Vec3(30, 2, 30)));
            check(helper, ObeliskBridge.findNear(far, 16) == null, "obelisk found 30 blocks away");
            ItemStack wand = wand();
            player.getInventory().setItem(0, wand);
            player.setHealth(10);
            player.getFoodData().setFoodLevel(10);
            ObeliskDrainerItem.drainPulse(helper.getLevel(), player, obelisk);
            check(helper, player.getHealth() > 13.9F && player.getHealth() <= 14F, "health " + player.getHealth() + ", expected ~14");
            check(helper, player.getFoodData().getFoodLevel() == 12, "food " + player.getFoodData().getFoodLevel());
            int vis = 0;
            for (var aspect : VisHelper.PRIMALS) {
                vis += VisHelper.stored(wand, aspect);
            }
            check(helper, vis >= 500 && vis <= 2000, "pulse added " + vis + " centivis, expected 5..20 vis");
            helper.setBlock(new BlockPos(2, 1, 2), net.minecraft.world.level.block.Blocks.AIR);
            helper.succeed();
        });

        r.add("utility/paradox", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            var zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(2.5, 2, 2.5));
            ParadoxItem.strike(helper.getLevel(), player, zombie, 0.98);
            check(helper, !zombie.isAlive() || zombie.getHealth() <= 0, "196 damage did not kill the zombie");
            check(helper, Math.abs(player.getHealth() - 16F) < 1e-3, "attacker must take 200 - 196 = 4, health " + player.getHealth());
            ItemStack sword = new ItemStack(FRItems.PARADOX.get());
            sword.setDamageValue(10);
            player.tickCount = 40;
            sword.getItem().inventoryTick(sword, helper.getLevel(), player, null);
            check(helper, sword.getDamageValue() == 9, "self repair, damage " + sword.getDamageValue());
            helper.succeed();
        });

        r.add("utility/ghastly_skull", helper -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack skull = new ItemStack(FRItems.GHASTLY_SKULL.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, skull);
            skull.getItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            check(helper, player.getHealth() == 1F, "health " + player.getHealth());
            helper.succeed();
        });
    }
}
