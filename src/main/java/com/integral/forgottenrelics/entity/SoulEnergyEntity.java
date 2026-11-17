package com.integral.forgottenrelics.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import velrondevs.botania.client.fx.SparkleParticleData;

/**
 * Soul Energy (EntitySoulEnergy, Edict of a Thousand Damned Souls) and Shiny Energy (EntityShinyEnergy, Shiny Stone):
 * a mote that homes on its target through blocks. Soul energy heals its target 1 and gives 1 food when it arrives;
 * shiny energy is only a visual and expires after 30 ticks.
 */
public class SoulEnergyEntity extends FRProjectile {
    private LivingEntity target;
    private final boolean shiny;

    public SoulEnergyEntity(EntityType<? extends SoulEnergyEntity> type, Level level, boolean shiny) {
        super(type, level);
        this.shiny = shiny;
    }

    public static SoulEnergyEntity soul(EntityType<? extends SoulEnergyEntity> type, Level level) {
        return new SoulEnergyEntity(type, level, false);
    }

    public static SoulEnergyEntity shiny(EntityType<? extends SoulEnergyEntity> type, Level level) {
        return new SoulEnergyEntity(type, level, true);
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    @Override
    protected int lifetime() {
        return shiny ? 30 : 1000;
    }

    @Override
    protected boolean collidesWithBlocks() {
        return false;
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity e) {
        return false;
    }

    @Override
    protected void serverTick(ServerLevel level) {
        if (target == null || !target.isAlive()) {
            discard();
            return;
        }
        if (new AABB(position(), position()).inflate(0.5).intersects(target.getBoundingBox())) {
            if (!shiny) {
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 0.8F + (float) Math.random() * 0.2F);
                target.heal(1);
                if (target instanceof Player player) {
                    player.getFoodData().eat(1, 1.0F);
                }
            }
            discard();
            return;
        }
        if (shiny) {
            setDeltaMovement(target.getBoundingBox().getCenter().subtract(position()).normalize().scale(0.15));
        } else {
            home(aimPoint(target, 0.6), 0.3, 0.35);
        }
    }

    @Override
    protected void clientTick() {
        float r = shiny ? (float) (0.9F + Math.random() * 0.1F) : 1.0F;
        float g = shiny ? (float) (0.2F + Math.random() * 0.2F) : 1.0F;
        float b = shiny ? 0.0F : 1.0F;
        level().addParticle(SparkleParticleData.sparkle(shiny ? 1.0F : 0.8F, r, g, b, 2), getX(), getY(), getZ(), 0, 0, 0);
    }
}
