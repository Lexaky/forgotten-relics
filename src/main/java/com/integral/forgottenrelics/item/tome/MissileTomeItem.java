package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.RageousMissileEntity;
import com.integral.forgottenrelics.registry.FREntities;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Nuclear Fury (ItemMissileTome): while held, every second tick pays 0.2 ignis + 0.1 ordo + 0.15 perditio vis
 * (x nuclearFuryVisMult) and launches a Rageous Missile from above the caster.
 */
public class MissileTomeItem extends HeldTomeItem {
    public MissileTomeItem(Properties properties) {
        super(properties, 5);
    }

    static int[] cost() {
        double m = FRConfig.nuclearFuryVisMult.getAsDouble();
        return new int[]{(int) (20 * m), (int) (10 * m), (int) (15 * m)};
    }

    @Override
    protected void castTick(ServerLevel level, Player player, ItemStack stack, int remaining) {
        if (!everySecondTick(remaining)) {
            return;
        }
        int[] c = cost();
        if (VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.IGNIS, c[0], TTAspects.ORDO, c[1], TTAspects.PERDITIO, c[2]))) {
            // the original wrote "posY + 3.8 + (Math.random() - 0.5 * 3.1)", i.e. 2.25..3.25 above the feet
            spawnMissile(level, player, player.getX() + (Math.random() - 0.5) * 3.1, player.getY() + 3.8 + (Math.random() - 0.5 * 3.1),
                    player.getZ() + (Math.random() - 0.5) * 3.1);
        }
    }

    public static RageousMissileEntity spawnMissile(ServerLevel level, Player player, double x, double y, double z) {
        RageousMissileEntity missile = new RageousMissileEntity(FREntities.RAGEOUS_MISSILE.get(), level);
        missile.setOwner(player);
        missile.setPos(x, y, z);
        level.playSound(null, x, y, z, TTSounds.missile(), SoundSource.PLAYERS, 0.6F, 0.8F + (float) Math.random() * 0.2F);
        level.addFreshEntity(missile);
        TTEffects.burst(level, x, y, z, 0.25F);
        return missile;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        int[] c = cost();
        String damage = (int) FRConfig.nuclearFuryDamageMIN.getAsDouble() + "-" + (int) FRConfig.nuclearFuryDamageMAX.getAsDouble();
        tomeTooltip(lines, new String[]{"ItemMissileTome1", "ItemMissileTome2", "ItemMissileTome3", "FREmpty", "ItemMissileTome4",
                "ItemMissileTome5", "FREmpty", "ItemMissileTome6", "FREmpty", loreValue("ItemMissileTome7_1", damage, "ItemMissileTome7_2")},
                "FRVisPerSecond", new double[]{0, 0, c[0] / 100.0 * 10, 0, c[1] / 100.0 * 10, c[2] / 100.0 * 10});
    }
}
