package com.integral.forgottenrelics.registry;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.entity.BabylonWeaponEntity;
import com.integral.forgottenrelics.entity.ChaoticOrbEntity;
import com.integral.forgottenrelics.entity.CrimsonOrbEntity;
import com.integral.forgottenrelics.entity.DarkMatterOrbEntity;
import com.integral.forgottenrelics.entity.LunarFlareEntity;
import com.integral.forgottenrelics.entity.RageousMissileEntity;
import com.integral.forgottenrelics.entity.SoulEnergyEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The original's 8 entities (EntityRegistry ids 237..245). They are short-lived projectiles: not saved with the
 * chunk (the original's NBT kept only Botania-copied counters) and not summonable from commands' suggestions.
 */
public final class FREntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ForgottenRelics.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<RageousMissileEntity>> RAGEOUS_MISSILE =
            register("rageous_missile", EntityType.Builder.<RageousMissileEntity>of(RageousMissileEntity::new, MobCategory.MISC).sized(0.15F, 0.15F));
    public static final DeferredHolder<EntityType<?>, EntityType<CrimsonOrbEntity>> CRIMSON_ORB =
            register("crimson_orb", EntityType.Builder.<CrimsonOrbEntity>of(CrimsonOrbEntity::new, MobCategory.MISC).sized(0.25F, 0.25F));
    public static final DeferredHolder<EntityType<?>, EntityType<DarkMatterOrbEntity>> DARK_MATTER_ORB =
            register("dark_matter_orb", EntityType.Builder.<DarkMatterOrbEntity>of(DarkMatterOrbEntity::new, MobCategory.MISC).sized(0.25F, 0.25F));
    public static final DeferredHolder<EntityType<?>, EntityType<SoulEnergyEntity>> SOUL_ENERGY =
            register("soul_energy", EntityType.Builder.<SoulEnergyEntity>of(SoulEnergyEntity::soul, MobCategory.MISC).sized(0.1F, 0.1F));
    public static final DeferredHolder<EntityType<?>, EntityType<LunarFlareEntity>> LUNAR_FLARE =
            register("lunar_flare", EntityType.Builder.<LunarFlareEntity>of(LunarFlareEntity::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(12), false);
    public static final DeferredHolder<EntityType<?>, EntityType<SoulEnergyEntity>> SHINY_ENERGY =
            register("shiny_energy", EntityType.Builder.<SoulEnergyEntity>of(SoulEnergyEntity::shiny, MobCategory.MISC).sized(0.1F, 0.1F));
    public static final DeferredHolder<EntityType<?>, EntityType<BabylonWeaponEntity>> BABYLON_WEAPON =
            register("babylon_weapon", EntityType.Builder.<BabylonWeaponEntity>of(BabylonWeaponEntity::new, MobCategory.MISC).sized(0.25F, 0.25F));
    public static final DeferredHolder<EntityType<?>, EntityType<ChaoticOrbEntity>> CHAOTIC_ORB =
            register("chaotic_orb", EntityType.Builder.<ChaoticOrbEntity>of(ChaoticOrbEntity::new, MobCategory.MISC).sized(0.25F, 0.25F));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String id, EntityType.Builder<T> builder) {
        return register(id, builder, true);
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String id, EntityType.Builder<T> builder, boolean defaultRange) {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, ForgottenRelics.id(id));
        return ENTITIES.register(id, () -> (defaultRange ? builder.clientTrackingRange(4) : builder).updateInterval(1).noSave().build(key));
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }

    private FREntities() {
    }
}
