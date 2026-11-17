package com.integral.forgottenrelics.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Factories for the original DamageSource subclasses ({@code DamageRegistryHandler}). Entity-bound ones
 * ({@code EntityDamageSource} in 1.7) take the attacker; the others have none.
 */
public final class FRDamageSources {
    private FRDamageSources() {
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> type, Entity attacker) {
        var holder = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type);
        return attacker == null ? new DamageSource(holder) : new DamageSource(holder, attacker);
    }

    /** {@code DamageSourceTrueDamage(entity)}: False Justice. */
    public static DamageSource trueDamage(Level level, Entity attacker) {
        return of(level, FRDamageTypes.TRUE_DAMAGE, attacker);
    }

    /** {@code DamageSourceTrueDamageUndef()}. */
    public static DamageSource trueDamageUndefined(Level level) {
        return of(level, FRDamageTypes.TRUE_DAMAGE_UNDEFINED, null);
    }

    /** {@code DamageSourceSoulDrain(entity)}: Soul Tome. */
    public static DamageSource soulDrain(Level level, Entity attacker) {
        return of(level, FRDamageTypes.SOUL_DRAIN, attacker);
    }

    /** {@code DamageSourceOblivion()}: Oblivion Amulet. */
    public static DamageSource oblivion(Level level) {
        return of(level, FRDamageTypes.OBLIVION, null);
    }

    /** {@code DamageSourceFate()}: Tome of Broken Fates. */
    public static DamageSource fate(Level level) {
        return of(level, FRDamageTypes.FATE, null);
    }

    /** {@code DamageSourceTLightning(entity)}: Telekinesis Tome. */
    public static DamageSource telekineticLightning(Level level, Entity attacker) {
        return of(level, FRDamageTypes.TELEKINETIC_LIGHTNING, attacker);
    }

    /** {@code DamageSourceDarkMatter(entity)}: Eldritch Spell. */
    public static DamageSource darkMatter(Level level, Entity attacker) {
        return of(level, FRDamageTypes.DARK_MATTER, attacker);
    }

    /** Original {@code isDamageTypeAbsolute}. */
    public static boolean isAbsolute(DamageSource source) {
        return source.is(FRTags.ABSOLUTE_DAMAGE);
    }
}
