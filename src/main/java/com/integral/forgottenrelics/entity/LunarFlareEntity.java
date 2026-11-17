package com.integral.forgottenrelics.entity;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.registry.FRSounds;
import com.integral.forgottenrelics.util.FRFx;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import velrondevs.botania.client.fx.SparkleParticleData;

/**
 * Lunar Flare (EntityLunarFlare, Tome of Lunar Flares): falls from the sky through every block towards the block the
 * caster looked at. Entities it passes take {@code damageLunarFlareDirect}; at the target block it explodes:
 * {@code damageLunarFlareImpact} with knockback to living entities within 2.5 blocks (not the caster).
 */
public class LunarFlareEntity extends FRProjectile {
    private BlockPos lock = BlockPos.ZERO;
    private Entity directlyHit;

    public LunarFlareEntity(EntityType<? extends LunarFlareEntity> type, Level level) {
        super(type, level);
    }

    public void setLock(BlockPos lock) {
        this.lock = lock;
    }

    public BlockPos lock() {
        return lock;
    }

    @Override
    protected int lifetime() {
        return 1000;
    }

    @Override
    protected boolean collidesWithBlocks() {
        return false;
    }

    @Override
    protected void onEntity(ServerLevel level, EntityHitResult hit) {
        if (getOwner() == null) {
            discard();
            return;
        }
        if (hit.getEntity() != getOwner()) {
            directlyHit = hit.getEntity();
            hit.getEntity().hurtServer(level, level.damageSources().indirectMagic(this, getOwner()), (float) FRConfig.damageLunarFlareDirect.getAsDouble());
        }
    }

    @Override
    protected void serverTick(ServerLevel level) {
        Vec3 from = new Vec3(xOld, yOld, zOld);
        if (new AABB(lock).clip(from, position()).isPresent() || new AABB(lock).contains(position())) {
            explode(level);
        }
    }

    public void explode(ServerLevel level) {
        if (getOwner() == null) {
            discard();
            return;
        }
        Vec3 c = getBoundingBox().getCenter();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.5),
                e -> e != getOwner() && e != directlyHit && e.isAlive())) {
            e.hurtServer(level, level.damageSources().indirectMagic(this, getOwner()), (float) FRConfig.damageLunarFlareImpact.getAsDouble());
            Vec3 diff = e.getBoundingBox().getCenter().subtract(c);
            double proportion = 1 / Math.max(0.01, distanceTo(e));
            e.push(diff.scale(0.2F + proportion / 16));
        }
        FRFx.wispBurst(level, new Vec3(lock.getX() + 0.5, lock.getY() + 1.25, lock.getZ() + 0.5), 48, 0.0F, 0.9F, 1.0F, 0.6F);
        level.levelEvent(2001, lock, Block.getId(level.getBlockState(lock)));
        level.playSound(null, lock.getX(), lock.getY(), lock.getZ(), FRSounds.LUNAR_FLARE.get(), SoundSource.PLAYERS, 16.0F, 0.8F + (float) Math.random() * 0.2F);
        TTEffects.burst(level, lock.getX() + 0.5, lock.getY() + 1.5, lock.getZ() + 0.5, 2.0F);
        discard();
    }

    @Override
    protected void clientTick() {
        Vec3 old = new Vec3(xOld, yOld, zOld);
        Vec3 now = position();
        int steps = Math.min(200, (int) (now.distanceTo(old) / 0.05));
        for (int i = 0; i < steps; i++) {
            Vec3 p = old.lerp(now, i / (double) steps);
            level().addParticle(SparkleParticleData.sparkle(2.0F, 0.0F, (float) (0.8F + Math.random() * 0.2F), (float) (0.4F + Math.random() * 0.6F), 1),
                    p.x + (Math.random() - 0.5) * 0.2, p.y + (Math.random() - 0.5) * 0.2, p.z + (Math.random() - 0.5) * 0.2, 0, 0, 0);
        }
    }
}
