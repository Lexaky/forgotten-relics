package com.integral.forgottenrelics.entity;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.registry.FRDamageSources;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import velrondevs.botania.client.fx.WispParticleData;

/**
 * Dark Matter Orb (EntityDarkMatterOrb, Eldritch Spell): fast straight orb. On impact every living entity within
 * 1 block (not the caster) takes {@code eldritchSpellDamage} dark-matter damage with weakness II 8 s, slowness II 5 s
 * and wither I 10 s; in the Outer Lands {@code eldritchSpellDamageEx} with weakness III 16 s, slowness III 20 s and
 * wither IV 12.5 s. An orb that has nearly stopped after 1 s bursts by itself.
 */
public class DarkMatterOrbEntity extends FRProjectile {
    public static final ResourceKey<Level> OUTER_LANDS = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath("thaumaturge", "outer_lands"));

    public DarkMatterOrbEntity(EntityType<? extends DarkMatterOrbEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int lifetime() {
        return 1000;
    }

    @Override
    protected void onBlock(ServerLevel level, BlockHitResult hit) {
        impact(level);
    }

    @Override
    protected void onEntity(ServerLevel level, EntityHitResult hit) {
        impact(level);
    }

    @Override
    protected void serverTick(ServerLevel level) {
        var m = getDeltaMovement();
        // the original forgot to keep the Math.abs results, which made orbs flying towards -x/-y/-z burst after 1 s
        if (tickCount >= 20 && Math.abs(m.x) < 0.01 && Math.abs(m.y) < 0.01 && Math.abs(m.z) < 0.01) {
            impact(level);
        }
    }

    public void impact(ServerLevel level) {
        if (getOwner() == null) {
            discard();
            return;
        }
        boolean outer = level.dimension().equals(OUTER_LANDS);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.0), e -> e != getOwner())) {
            if (outer) {
                e.hurtServer(level, FRDamageSources.darkMatter(level, getOwner()), (float) FRConfig.eldritchSpellDamageEx.getAsDouble());
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 320, 2));
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 400, 2));
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 250, 3));
            } else {
                e.hurtServer(level, FRDamageSources.darkMatter(level, getOwner()), (float) FRConfig.eldritchSpellDamage.getAsDouble());
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
            }
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
        level.sendParticles(WispParticleData.wisp(0.3F, 0.3F, 0.0F, 0.4F, 1.0F), getX(), getY(), getZ(), 30, 0.3, 0.3, 0.3, 0.05);
        discard();
    }

    @Override
    protected void clientTick() {
        level().addParticle(WispParticleData.wisp(0.25F, 0.2F, 0.0F, 0.35F, 0.5F), getX(), getY() + 0.22 * getBbHeight(), getZ(), 0, 0, 0);
    }
}
