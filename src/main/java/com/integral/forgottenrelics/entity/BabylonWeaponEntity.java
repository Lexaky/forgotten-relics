package com.integral.forgottenrelics.entity;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.util.FRFx;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import velrondevs.botania.client.fx.WispParticleData;

/**
 * Babylon Weapon of Apotheosis (EntityBabylonWeaponSS, a copy of Botania's babylon weapon). Hovers and charges for
 * 15 ticks, then flies at speed 3 towards the block the caster looks at (64 blocks). Touching a living entity:
 * {@code damageApotheosisDirect} with knockback, then the impact; hitting a block: the impact. Impact:
 * {@code damageApotheosisImpact} with knockback to living entities within 3 blocks (not the caster).
 */
public class BabylonWeaponEntity extends FRProjectile {
    private static final EntityDataAccessor<Integer> VARIETY = SynchedEntityData.defineId(BabylonWeaponEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ROTATION = SynchedEntityData.defineId(BabylonWeaponEntity.class, EntityDataSerializers.FLOAT);
    // synced like the original's data watcher: the renderer fades the sigil once the weapon is launched
    private static final EntityDataAccessor<Integer> LIVE_TICKS = SynchedEntityData.defineId(BabylonWeaponEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(BabylonWeaponEntity.class, EntityDataSerializers.INT);
    private int liveTicks;
    private int delay;

    public BabylonWeaponEntity(EntityType<? extends BabylonWeaponEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(VARIETY, 0);
        builder.define(ROTATION, 0F);
        builder.define(LIVE_TICKS, 0);
        builder.define(DELAY, 0);
    }

    public int liveTicks() {
        return entityData.get(LIVE_TICKS);
    }

    public int delay() {
        return entityData.get(DELAY);
    }

    public void setVariety(int v) {
        entityData.set(VARIETY, v);
    }

    public int variety() {
        return entityData.get(VARIETY);
    }

    public void setRotation(float r) {
        entityData.set(ROTATION, r);
    }

    public float rotation() {
        return entityData.get(ROTATION);
    }

    public void setDelay(int delay) {
        this.delay = delay;
        entityData.set(DELAY, delay);
    }

    @Override
    protected int lifetime() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        return false; // contact damage is the original's own box sweep below
    }

    @Override
    protected void onBlock(ServerLevel level, BlockHitResult hit) {
        if (tickCount > 15) {
            impact(level);
        }
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            if (!(getOwner() instanceof Player player) || !player.isAlive()) {
                discard();
                return;
            }
            if (tickCount <= 15) {
                setDeltaMovement(Vec3.ZERO);
                if (random.nextInt(20) == 0) {
                    server.playSound(null, getX(), getY(), getZ(), TTSounds.babylonSpawn(), SoundSource.PLAYERS, 0.1F, 1F + random.nextFloat() * 3F);
                }
            } else {
                if (liveTicks < delay) {
                    setDeltaMovement(Vec3.ZERO);
                } else if (liveTicks == delay) {
                    aim(server, player);
                }
                liveTicks++;
                entityData.set(LIVE_TICKS, liveTicks);
                AABB sweep = new AABB(position(), new Vec3(xOld, yOld, zOld)).inflate(2);
                for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, sweep, e -> e != getOwner())) {
                    if (living.hurtTime == 0) {
                        living.hurtServer(server, server.damageSources().indirectMagic(this, player), (float) FRConfig.damageApotheosisDirect.getAsDouble());
                        knock(living, 0.2F);
                        impact(server);
                        return;
                    }
                }
                if (liveTicks > 200 + delay) {
                    discard();
                    return;
                }
            }
        }
        super.tick();
    }

    /** Original: aim at the centre of the block the caster looks at (64 blocks), else 64 blocks along the look. */
    void aim(ServerLevel level, Player player) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult look = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(64)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY, player));
        Vec3 aim = look.getType() == HitResult.Type.BLOCK ? look.getBlockPos().getCenter() : player.position().add(player.getLookAngle().scale(64));
        setDeltaMovement(aim.subtract(getBoundingBox().getCenter()).normalize().scale(3.0D));
        level.playSound(null, getX(), getY(), getZ(), TTSounds.babylonAttack(), SoundSource.PLAYERS, 2F, 0.1F + random.nextFloat() * 3F);
    }

    private void knock(LivingEntity e, float base) {
        Vec3 diff = e.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter());
        double proportion = Math.min(1.2D, 1 / Math.max(0.01, distanceTo(e)));
        e.push(diff.scale(base + proportion / 8));
    }

    public void impact(ServerLevel level) {
        if (getOwner() != null) {
            TTEffects.burst(level, getX(), getY(), getZ(), 1.5F);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3), e -> e != getOwner() && e.isAlive())) {
                e.hurtServer(level, level.damageSources().indirectMagic(this, getOwner()), (float) FRConfig.damageApotheosisImpact.getAsDouble());
                knock(e, 0.1F);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 8.0F, (float) (0.8F + Math.random() * 0.2));
        }
        FRFx.wispBurst(level, position(), 40, 1.0F, 1.0F, 0.0F, 0.5F);
        discard();
    }

    @Override
    protected void clientTick() {
        if (tickCount > 15) {
            level().addParticle(WispParticleData.wisp(0.3F, 1F, 1F, 0F, 1F), getX(), getY(), getZ(), 0, 0, 0);
        }
    }
}
