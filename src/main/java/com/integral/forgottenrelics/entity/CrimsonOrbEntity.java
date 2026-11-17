package com.integral.forgottenrelics.entity;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.compat.TTSounds;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import velrondevs.botania.client.fx.WispParticleData;

/**
 * Crimson Orb (EntityCrimsonOrb, Crimson Spell): slow homing orb (acceleration 0.3 / distance², speed clamp 0.25 per
 * axis, rises during its first 5 ticks); picks a new visible target within 32 blocks when it has none; breaks on
 * blocks (except leaves, plants, fluids); can be batted away by hitting it.
 */
public class CrimsonOrbEntity extends FRProjectile {
    private LivingEntity target;

    public CrimsonOrbEntity(EntityType<? extends CrimsonOrbEntity> type, Level level) {
        super(type, level);
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public LivingEntity target() {
        return target;
    }

    @Override
    protected int lifetime() {
        return 1000;
    }

    @Override
    protected void onEntity(ServerLevel level, EntityHitResult hit) {
        if (getOwner() == null) {
            return;
        }
        double min = FRConfig.crimsonSpellDamageMIN.getAsDouble();
        double max = FRConfig.crimsonSpellDamageMAX.getAsDouble();
        hit.getEntity().hurtServer(level, level.damageSources().indirectMagic(this, getOwner()), (float) (min + Math.random() * (max - min)));
        burst(level);
    }

    @Override
    protected void onBlock(ServerLevel level, BlockHitResult hit) {
        burst(level);
    }

    private void burst(ServerLevel level) {
        level.playSound(null, getX(), getY(), getZ(), TTSounds.shock(), SoundSource.PLAYERS, 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
        TTEffects.burst(level, getX(), getY(), getZ(), 1.0F);
        discard();
    }

    /** Original {@code getNewTarget}: a random living entity within 32 blocks that can see the orb, not the caster. */
    void newTarget(ServerLevel level) {
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(32),
                e -> e != getOwner() && e.hasLineOfSight(this));
        if (!list.isEmpty()) {
            target = list.get((int) ((list.size() - 1) * Math.random()));
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (target != null && !target.isAlive()) {
            newTarget(level);
        }
        if (target != null && target.isAlive()) {
            home(aimPoint(target, 0.6), 0.3, 0.25);
            if (tickCount < 5 && getDeltaMovement().y < 0) {
                setDeltaMovement(getDeltaMovement().multiply(1, -1, 1));
            }
        } else {
            newTarget(level);
        }
    }

    @Override
    protected void clientTick() {
        for (int i = 0; i < 2; i++) {
            level().addParticle(WispParticleData.wisp(0.3F, 0.9F, 0.1F, 0.1F, 0.6F), getX(), getY(), getZ(),
                    (random.nextFloat() - 0.5F) * 0.02F, (random.nextFloat() - 0.5F) * 0.02F, (random.nextFloat() - 0.5F) * 0.02F);
        }
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    /** Original {@code attackEntityFrom}: a hit sends the orb along the attacker's look at 0.9. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isInvulnerableToBase(source) || source.getEntity() == null) {
            return false;
        }
        Vec3 look = source.getEntity().getLookAngle();
        setDeltaMovement(look.scale(0.9));
        level.playSound(null, getX(), getY(), getZ(), TTSounds.zap(), SoundSource.PLAYERS, 1.0F, 1.0F + (random.nextFloat() - random.nextFloat()) * 0.2F);
        return true;
    }
}
