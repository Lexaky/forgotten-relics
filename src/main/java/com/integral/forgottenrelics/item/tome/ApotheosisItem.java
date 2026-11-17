package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.BabylonWeaponEntity;
import com.integral.forgottenrelics.registry.FREntities;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Apotheosis (ItemApotheosis): while held, every second tick pays 0.3 terra + 0.6 ignis + 0.5 ordo + 0.75 perditio
 * vis (x apotheosisVisMult) and summons a Babylon Weapon at a free spot (no other weapon within 2 blocks) of an arc
 * 2..12 blocks to the caster's sides and up to 7.5 blocks above.
 */
public class ApotheosisItem extends HeldTomeItem {
    public ApotheosisItem(Properties properties) {
        super(properties, 5);
    }

    static int[] cost() {
        double m = FRConfig.apotheosisVisMult.getAsDouble();
        return new int[]{(int) (30 * m), (int) (60 * m), (int) (50 * m), (int) (75 * m)};
    }

    @Override
    protected void castTick(ServerLevel level, Player player, ItemStack stack, int remaining) {
        int[] c = cost();
        if (everySecondTick(remaining) && VisHelper.consumeFromInventory(player,
                VisHelper.cost(TTAspects.TERRA, c[0], TTAspects.IGNIS, c[1], TTAspects.ORDO, c[2], TTAspects.PERDITIO, c[3]))) {
            spawnWeapon(level, player);
        }
    }

    /**
     * Original {@code spawnBabylonWeapon}. The original approximated the horizontal look direction piecewise from the
     * head yaw; the exact horizontal look vector is used here (the spot is random either way).
     */
    public static BabylonWeaponEntity spawnWeapon(ServerLevel level, Player player) {
        Vec3 center = player.getBoundingBox().getCenter();
        float yaw = player.getYHeadRot() * Mth.DEG_TO_RAD;
        Vec3 look = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 spot = look;
        for (int attempt = 0; attempt <= 100; attempt++) {
            double sign = Math.random() >= 0.5 ? -1.0D : 1.0D;
            Vec3 v = look.yRot((float) Math.toRadians(80.0D * sign)).scale(2.0F + Math.random() * 10.0F)
                    .add(look.scale(Math.random())).add(0, -0.5 + Math.random() * 8, 0).add(center);
            if (level.getEntitiesOfClass(BabylonWeaponEntity.class, new AABB(v, v).inflate(2.0D)).isEmpty()) {
                spot = v;
                break;
            }
        }
        BabylonWeaponEntity weapon = new BabylonWeaponEntity(FREntities.BABYLON_WEAPON.get(), level);
        weapon.setOwner(player);
        weapon.setPos(spot);
        weapon.setYRot(player.getYHeadRot());
        weapon.setVariety(level.getRandom().nextInt(12));
        weapon.setDelay(0);
        weapon.setRotation(Mth.wrapDegrees(-player.getYHeadRot() + 180));
        level.addFreshEntity(weapon);
        level.playSound(null, weapon.getX(), weapon.getY(), weapon.getZ(), TTSounds.babylonSpawn(), SoundSource.PLAYERS, 1F, 1F + level.getRandom().nextFloat() * 3F);
        return weapon;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        int[] c = cost();
        tomeTooltip(lines, new String[]{"ItemApotheosis1", "FREmpty", "ItemApotheosis2", "ItemApotheosis3", "FREmpty",
                loreValue("ItemApotheosis4_1", (int) FRConfig.damageApotheosisDirect.getAsDouble(), "ItemApotheosis4_2"),
                loreValue("ItemApotheosis5_1", (int) FRConfig.damageApotheosisImpact.getAsDouble(), "ItemApotheosis5_2"), "ItemApotheosis6"},
                "FRVisPerSecond", new double[]{0, c[0] / 100.0 * 10, c[1] / 100.0 * 10, 0, c[2] / 100.0 * 10, c[3] / 100.0 * 10});
    }
}
