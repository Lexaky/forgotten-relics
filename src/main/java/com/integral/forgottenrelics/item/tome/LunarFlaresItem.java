package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.LunarFlareEntity;
import com.integral.forgottenrelics.registry.FREntities;
import com.integral.forgottenrelics.registry.FRSounds;
import com.integral.forgottenrelics.util.SuperpositionHandler;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tome of Lunar Flares (ItemLunarFlares): while held and looking at a block within 128, every second tick pays
 * 0.35 aer + 0.5 ignis + 0.65 ordo vis (x lunarFlaresVisMult) and drops a Lunar Flare on that block from 6..42
 * blocks above it.
 */
public class LunarFlaresItem extends HeldTomeItem {
    public LunarFlaresItem(Properties properties) {
        super(properties, 3);
    }

    static int[] cost() {
        double m = FRConfig.lunarFlaresVisMult.getAsDouble();
        return new int[]{(int) (35 * m), (int) (50 * m), (int) (65 * m)};
    }

    @Override
    protected void castTick(ServerLevel level, Player player, ItemStack stack, int remaining) {
        if (!everySecondTick(remaining)) {
            return;
        }
        BlockHitResult hit = SuperpositionHandler.getPointedBlock(player, level, 128);
        int[] c = cost();
        if (hit != null && VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, c[0], TTAspects.IGNIS, c[1], TTAspects.ORDO, c[2]))) {
            spawnFlare(level, player, hit.getBlockPos());
            if (remaining % 4 == 0) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), FRSounds.STARFALL.get(), SoundSource.PLAYERS, 2.0F, (float) (1.0F + Math.random() * 0.5F));
            }
        }
    }

    public static LunarFlareEntity spawnFlare(ServerLevel level, Player player, BlockPos lock) {
        LunarFlareEntity flare = new LunarFlareEntity(FREntities.LUNAR_FLARE.get(), level);
        flare.setOwner(player);
        flare.setLock(lock);
        Vec3 offset = new Vec3((Math.random() - 0.5) * 18, (24 + (Math.random() - 0.5) * 18) * 2.0D, (Math.random() - 0.5) * 18);
        flare.setPos(new Vec3(lock.getX() + 0.5, lock.getY(), lock.getZ() + 0.5).add(offset));
        flare.setDeltaMovement(offset.normalize().scale(-4.0));
        level.addFreshEntity(flare);
        return flare;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        int[] c = cost();
        tomeTooltip(lines, new String[]{"ItemLunarFlares1", "FREmpty", "ItemLunarFlares2",
                loreValue("ItemLunarFlares3_1", (int) FRConfig.damageLunarFlareImpact.getAsDouble(), "ItemLunarFlares3_2"), "ItemLunarFlares35", "FREmpty",
                loreValue("ItemLunarFlares4_1", (int) FRConfig.damageLunarFlareDirect.getAsDouble(), "ItemLunarFlares4_2")},
                "FRVisPerSecond", new double[]{c[0] / 100.0 * 10, 0, c[1] / 100.0 * 10, 0, c[2] / 100.0 * 10, 0});
    }
}
