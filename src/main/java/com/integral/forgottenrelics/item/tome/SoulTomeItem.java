package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.SoulEnergyEntity;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FREntities;
import com.integral.forgottenrelics.util.FRFx;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Edict of a Thousand Damned Souls (ItemSoulTome). While held the caster stands still. After the first second:
 * every tick, each living entity within 3 blocks is thrown back with 20..100 lightning damage for 1.5 ignis +
 * 1.2 perditio vis; every fourth tick, for 0.25 terra + 0.2 aer + 0.35 ignis + 0.5 perditio vis, a random living
 * entity within 20 blocks loses maxHealth / soulTomeDivisor (1..20) to soul drain and a soul flies to the caster.
 */
public class SoulTomeItem extends HeldTomeItem {
    public SoulTomeItem(Properties properties) {
        super(properties, 3);
    }

    static double mult() {
        return FRConfig.soulTomeVisMult.getAsDouble();
    }

    @Override
    protected void clientUseTick(Player player, ItemStack stack, int remaining) {
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
    }

    @Override
    protected void castTick(ServerLevel level, Player player, ItemStack stack, int remaining) {
        player.setDeltaMovement(0, player.getDeltaMovement().y, 0);
        if (remaining >= USE_TICKS - 20) {
            return;
        }
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20), e -> e != player);
        for (int i = entities.size() - 1; i >= 0; i--) {
            LivingEntity e = entities.get(i);
            if (player.distanceTo(e) <= 3 && VisHelper.consumeFromInventory(player,
                    VisHelper.cost(TTAspects.IGNIS, (int) (150 * mult()), TTAspects.PERDITIO, (int) (120 * mult())))) {
                throwBack(level, player, e);
            }
        }
        if (remaining % 4 == 0 && VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.TERRA, (int) (25 * mult()),
                TTAspects.AER, (int) (20 * mult()), TTAspects.IGNIS, (int) (35 * mult()), TTAspects.PERDITIO, (int) (50 * mult())))) {
            if (!entities.isEmpty()) {
                drain(level, player, entities.get((int) (entities.size() * Math.random())));
            }
        }
    }

    /** Lightning, 20..100 damage and a push of 3 away (+1 up). */
    public static void throwBack(ServerLevel level, Player player, LivingEntity e) {
        Vec3 ec = e.getBoundingBox().getCenter();
        Vec3 pc = player.getBoundingBox().getCenter();
        Vec3 diff = ec.subtract(pc).scale(1 / Math.max(0.01, player.distanceTo(e)) * 3.0F);
        for (int i = 0; i <= 3; i++) {
            FRFx.lightning(level, player.position().add(0, 1, 0), ec, 40, 0.075F);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.zap(), SoundSource.PLAYERS, 1F, 0.8F);
        e.hurtServer(level, FRDamageSources.telekineticLightning(level, player), (float) (20 + 80 * Math.random()));
        e.setDeltaMovement(diff.x, diff.y + 1.0F, diff.z);
        e.hurtMarked = true;
    }

    /** Soul drain of {@code maxHealth / soulTomeDivisor}, clamped to 1..20, and a soul back to the caster. */
    public static float drain(ServerLevel level, Player player, LivingEntity e) {
        float damage = (float) (e.getMaxHealth() / FRConfig.soulTomeDivisor.getAsDouble());
        damage = Math.max(1.0F, Math.min(20.0F, damage));
        e.hurtServer(level, FRDamageSources.soulDrain(level, player), damage);
        SoulEnergyEntity soul = new SoulEnergyEntity(FREntities.SOUL_ENERGY.get(), level, false);
        soul.setOwner(player);
        soul.setTarget(player);
        soul.setPos(e.getBoundingBox().getCenter().add(e.getLookAngle()).add(0, 0.5, 0));
        soul.setDeltaMovement(e.getLookAngle().scale(1.25F));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.missile(), SoundSource.PLAYERS, 2.0F, 0.8F + (float) Math.random() * 0.2F);
        level.addFreshEntity(soul);
        return damage;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        double m = mult();
        tomeTooltip(lines, new String[]{"ItemSoulTome1", "ItemSoulTome2", "ItemSoulTome3", "FREmpty", "ItemSoulTome4", "ItemSoulTome5", "FREmpty",
                "ItemSoulTome6", "ItemSoulTome7", "ItemSoulTome8", "FREmpty", "ItemSoulTome9"}, "FRVisPerSecond",
                new double[]{20 * m / 100 * 10, 25 * m / 100 * 10, 35 * m / 100 * 10, 0, 0, 50 * m / 100 * 10});
    }
}
