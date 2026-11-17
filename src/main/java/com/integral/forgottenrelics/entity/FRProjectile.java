package com.integral.forgottenrelics.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Base of the relic projectiles (1.7 {@code EntityThrowable} subclasses). Moves by its own rules so each projectile
 * can keep the original's quirks: no air drag, optional gravity, optional block collision (several original
 * projectiles flew through blocks because their {@code onImpact} ignored block hits), and entity hits along the path.
 * All gameplay runs on the server; {@link #clientTick()} draws the particles the original spawned client-side.
 */
public abstract class FRProjectile extends Projectile {
    protected FRProjectile(EntityType<? extends FRProjectile> type, Level level) {
        super(type, level);
    }

    /** Downward acceleration per tick (1.7 {@code getGravityVelocity}). */
    protected double gravity() {
        return 0.0;
    }

    /** Ticks before the projectile removes itself. */
    protected abstract int lifetime();

    /** Whether blocks stop this projectile at all. */
    protected boolean collidesWithBlocks() {
        return true;
    }

    /** Original {@code BlockBush / BlockLeaves / BlockLiquid} exemption; collider clipping already skips plants and fluids. */
    protected static boolean ignoredBlock(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    /** Called on the server when the path crosses a block. */
    protected void onBlock(ServerLevel level, BlockHitResult hit) {
    }

    /** Called on the server when the path crosses an entity that {@link #canHitEntity} accepts. */
    protected void onEntity(ServerLevel level, EntityHitResult hit) {
    }

    /** Steering and per-tick logic on the server, after moving. */
    protected void serverTick(ServerLevel level) {
    }

    /** Particles on the client, after moving. */
    protected void clientTick() {
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target instanceof LivingEntity && super.canHitEntity(target);
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > lifetime()) {
            discard();
            return;
        }
        Vec3 pos = position();
        Vec3 motion = getDeltaMovement();
        Vec3 next = pos.add(motion);
        if (level() instanceof ServerLevel server) {
            if (collidesWithBlocks()) {
                BlockHitResult block = server.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (block.getType() == HitResult.Type.BLOCK && !ignoredBlock(server.getBlockState(block.getBlockPos()))) {
                    next = block.getLocation();
                    setPos(next); // impacts act around the point of contact, as 1.7 throwables did
                    onBlock(server, block);
                    if (isRemoved()) {
                        return;
                    }
                }
            }
            EntityHitResult entity = ProjectileUtil.getEntityHitResult(server, this, pos, next,
                    getBoundingBox().expandTowards(motion).inflate(1.0), this::canHitEntity, 0.3F);
            if (entity != null) {
                Vec3 keep = position();
                setPos(entity.getLocation());
                onEntity(server, entity);
                if (!isRemoved()) {
                    setPos(keep); // projectiles that fly on (lunar flare, ...) continue from where they were
                }
                if (isRemoved()) {
                    return;
                }
            }
        }
        setPos(next);
        if (gravity() != 0) {
            setDeltaMovement(getDeltaMovement().add(0, -gravity(), 0));
        }
        if (level() instanceof ServerLevel server) {
            serverTick(server);
        } else {
            clientTick();
        }
    }

    /** 1.7 homing step: motion += (target - this) / distSq * strength, each axis clamped to +-clamp. */
    protected void home(Vec3 target, double strength, double clamp) {
        Vec3 diff = target.subtract(position());
        double d = diff.lengthSqr();
        if (d < 1e-6) {
            return;
        }
        Vec3 m = getDeltaMovement().add(diff.scale(strength / d));
        setDeltaMovement(clampAxes(m, clamp));
    }

    protected static Vec3 clampAxes(Vec3 v, double clamp) {
        return new Vec3(Math.max(-clamp, Math.min(clamp, v.x)), Math.max(-clamp, Math.min(clamp, v.y)), Math.max(-clamp, Math.min(clamp, v.z)));
    }

    /** {@code target.boundingBox.minY + height * factor}: the aim point of the original homing code. */
    protected static Vec3 aimPoint(Entity target, double factor) {
        return new Vec3(target.getX(), target.getBoundingBox().minY + target.getBbHeight() * factor, target.getZ());
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }
}
