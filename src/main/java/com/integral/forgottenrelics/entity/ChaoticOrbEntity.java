package com.integral.forgottenrelics.entity;

import com.integral.forgottenrelics.FRConfig;
import com.leclowndu93150.thaumaturge.api.taint.TaintApi;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import velrondevs.botania.client.fx.WispParticleData;

/**
 * Orb of Primal Energy (EntityChaoticOrb, Tome of Primal Chaos; a copy of TC4's EntityPrimalOrb). After 20 ticks it
 * either drifts randomly or (35 %, "seeker") homes on the nearest living entity within 16 blocks, its caster skipped
 * only 80 % of the time. Impact: 1..1+chaosTomeDamageCap magic damage to an entity hit, an explosion of power 1..7
 * that breaks blocks, and for non-seekers a 1 % chance (10 % inside a nether portal) of a taint outbreak.
 */
public class ChaoticOrbEntity extends FRProjectile {
    private boolean seeker;
    private int count;

    public ChaoticOrbEntity(EntityType<? extends ChaoticOrbEntity> type, Level level) {
        super(type, level);
    }

    public void setSeeker(boolean seeker) {
        this.seeker = seeker;
    }

    public boolean seeker() {
        return seeker;
    }

    @Override
    protected double gravity() {
        return 0.001;
    }

    @Override
    protected int lifetime() {
        return 5000;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return target instanceof LivingEntity && target != getOwner() || tickCount > 5 && target == getOwner();
    }

    @Override
    protected void onBlock(ServerLevel level, BlockHitResult hit) {
        impact(level, hit);
    }

    @Override
    protected void onEntity(ServerLevel level, EntityHitResult hit) {
        impact(level, hit);
    }

    private boolean inPortal() {
        return level().getBlockState(blockPosition()).is(Blocks.NETHER_PORTAL);
    }

    @Override
    protected void serverTick(ServerLevel level) {
        count++;
        if (inPortal()) {
            impact(level, new BlockHitResult(position(), net.minecraft.core.Direction.UP, blockPosition(), true));
            return;
        }
        if (tickCount <= 20) {
            return;
        }
        if (!seeker) {
            Random rr = new Random(getId() + count);
            setDeltaMovement(getDeltaMovement().add((rr.nextFloat() - rr.nextFloat()) * 0.01F,
                    (rr.nextFloat() - rr.nextFloat()) * 0.01F, (rr.nextFloat() - rr.nextFloat()) * 0.01F));
            return;
        }
        Entity owner = getOwner();
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16), e -> distanceTo(e) <= 16)) {
            if (e == owner && Math.random() < 0.8D || !e.isAlive()) {
                continue;
            }
            double dd = distanceToSqr(e);
            if (dd < bestD) {
                bestD = dd;
                best = e;
            }
        }
        if (best != null) {
            home(aimPoint(best, 0.9), 0.2, 0.2);
        }
    }

    void impact(ServerLevel level, HitResult hit) {
        float specialChance = 1.0F;
        if (hit.getType() == HitResult.Type.BLOCK && inPortal()) {
            specialChance = 10.0F;
        }
        if (hit instanceof EntityHitResult eh && getOwner() != null) {
            eh.getEntity().hurtServer(level, level.damageSources().indirectMagic(this, getOwner()),
                    (float) (1.0F + Math.random() * FRConfig.chaosTomeDamageCap.getAsDouble()));
        }
        level.explode(null, getX(), getY(), getZ(), (float) (1.0F + Math.random() * 6.0F), Level.ExplosionInteraction.TNT);
        if (!seeker && random.nextInt(100) <= specialChance && random.nextBoolean()) {
            // the other half of the original's special outcome created a random aura node; not ported
            taintSplosion(level);
        }
        level.sendParticles(WispParticleData.wisp(0.4F, 0.8F, 0.3F, 1.0F, 1.0F), getX(), getY(), getZ(), 36, 0.5, 0.5, 0.5, 0.1);
        discard();
    }

    /** Original {@code taintSplosion}: 10 tries around the impact to start taint on the surface. */
    void taintSplosion(ServerLevel level) {
        int x = (int) getX();
        int z = (int) getZ();
        for (int a = 0; a < 10; a++) {
            int xx = x + (int) (random.nextFloat() - random.nextFloat() * 6.0F);
            int zz = z + (int) (random.nextFloat() - random.nextFloat() * 6.0F);
            if (random.nextBoolean()) {
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(xx, 0, zz));
                if (!TaintApi.isTainted(level, top)) {
                    TaintApi.spreadFibres(level, top, true);
                }
            }
        }
    }

    @Override
    protected void clientTick() {
        for (int a = 0; a < 3; a++) {
            float r = random.nextFloat(), g = random.nextFloat(), b = random.nextFloat();
            level().addParticle(WispParticleData.wisp(0.15F, r, g, b, 0.4F), getX() + (random.nextFloat() - random.nextFloat()) * 0.2,
                    getY() + (random.nextFloat() - random.nextFloat()) * 0.2, getZ() + (random.nextFloat() - random.nextFloat()) * 0.2, 0, 0, 0);
        }
    }
}
