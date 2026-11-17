package com.integral.forgottenrelics.entity;

import com.integral.forgottenrelics.FRConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import velrondevs.botania.client.fx.SparkleParticleData;

/**
 * Rageous Missile (EntityRageousMissile, Nuclear Fury; an upgraded Botania magic missile). Flies through blocks at
 * 0.5 per tick towards a random living entity within 32 blocks (rising for its first 30 ticks); without a target it
 * wanders and expires after 160 ticks. Only its own target can be hit: nuclearFuryDamageMIN..MAX magic damage.
 */
public class RageousMissileEntity extends FRProjectile {
    private LivingEntity target;
    private int time;

    public RageousMissileEntity(EntityType<? extends RageousMissileEntity> type, Level level) {
        super(type, level);
    }

    public LivingEntity target() {
        return target;
    }

    @Override
    protected int lifetime() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean collidesWithBlocks() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return e == target;
    }

    @Override
    protected void onEntity(ServerLevel level, EntityHitResult hit) {
        if (getOwner() != null) {
            double min = FRConfig.nuclearFuryDamageMIN.getAsDouble();
            double max = FRConfig.nuclearFuryDamageMAX.getAsDouble();
            hit.getEntity().hurtServer(level, level.damageSources().indirectMagic(this, getOwner()), (float) (min + Math.random() * (max - min)));
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2.0F, (float) (0.8F + Math.random() * 0.2F));
        discard();
    }

    /** Original {@code getTarget}: keep a living target, otherwise pick a random living entity within 32 blocks. */
    boolean acquire(ServerLevel level) {
        if (target != null && target.getHealth() > 0 && target.isAlive()) {
            return true;
        }
        target = null;
        List<LivingEntity> list = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(32),
                e -> e != getOwner() && e.isAlive()));
        if (!list.isEmpty()) {
            target = list.get(random.nextInt(list.size()));
        }
        return target != null;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (!acquire(level) && time > 160) {
            discard();
            return;
        }
        Vec3 center = getBoundingBox().getCenter();
        if (target != null) {
            Vec3 m = target.getBoundingBox().getCenter().subtract(center).normalize().scale(0.5);
            if (time < 30) {
                m = new Vec3(m.x, Math.abs(m.y), m.z);
            }
            setDeltaMovement(m);
        } else {
            Vec3 wander = center.add((Math.random() - 0.5D) * 16, (Math.random() - 0.5D) * 16, (Math.random() - 0.5D) * 16);
            setDeltaMovement(wander.subtract(center).normalize().scale(0.5));
        }
        time++;
    }

    @Override
    protected void clientTick() {
        Vec3 old = new Vec3(xOld, yOld + getBbHeight() / 2, zOld);
        Vec3 now = getBoundingBox().getCenter();
        int steps = (int) (now.distanceTo(old) / 0.05);
        for (int i = 0; i < steps; i++) {
            Vec3 p = old.lerp(now, i / (double) steps);
            level().addParticle(SparkleParticleData.sparkle(0.8F, 0.0F, (float) (0.8F + Math.random() * 0.2F), (float) (0.4F + Math.random() * 0.6F), 2),
                    p.x, p.y, p.z, 0, 0, 0);
        }
    }
}
