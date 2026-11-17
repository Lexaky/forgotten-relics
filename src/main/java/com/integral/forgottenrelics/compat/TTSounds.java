package com.integral.forgottenrelics.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Thaumaturge's ports of the Thaumcraft sounds the original played ({@code thaumcraft:hhon}, ...). */
public final class TTSounds {
    private TTSounds() {
    }

    private static SoundEvent get(String path, SoundEvent fallback) {
        return of("thaumaturge", path, fallback);
    }

    /** A sound of another mod by id, with a vanilla fallback if it is renamed. */
    public static SoundEvent of(String namespace, String path, SoundEvent fallback) {
        return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath(namespace, path)).orElse(fallback);
    }

    /** {@code thaumcraft:shock}. */
    public static SoundEvent shock() {
        return get("shock", SoundEvents.LIGHTNING_BOLT_IMPACT);
    }

    /** {@code thaumcraft:egattack}. */
    public static SoundEvent egAttack() {
        return get("egattack", SoundEvents.EVOKER_CAST_SPELL);
    }

    /** {@code thaumcraft:ice}. */
    public static SoundEvent ice() {
        return get("ice", SoundEvents.GLASS_BREAK);
    }

    /** {@code botania:missile}. */
    public static SoundEvent missile() {
        return of("botania", "missile", SoundEvents.FIREWORK_ROCKET_LAUNCH);
    }

    /** {@code botania:babylonSpawn}. */
    public static SoundEvent babylonSpawn() {
        return of("botania", "babylon_spawn", SoundEvents.ENCHANTMENT_TABLE_USE);
    }

    /** {@code botania:babylonAttack}. */
    public static SoundEvent babylonAttack() {
        return of("botania", "babylon_attack", SoundEvents.TRIDENT_THROW.value());
    }

    /** {@code thaumcraft:hhon}. */
    public static SoundEvent hhOn() {
        return get("hhon", SoundEvents.UI_BUTTON_CLICK.value());
    }

    /** {@code thaumcraft:hhoff}. */
    public static SoundEvent hhOff() {
        return get("hhoff", SoundEvents.UI_BUTTON_CLICK.value());
    }

    /** {@code thaumcraft:jar}. */
    public static SoundEvent jar() {
        return get("jar", SoundEvents.BOTTLE_FILL);
    }

    /** {@code thaumcraft:zap}. */
    public static SoundEvent zap() {
        return get("zap", SoundEvents.LIGHTNING_BOLT_IMPACT);
    }

    /** {@code botania:altarCraft}. */
    public static SoundEvent altarCraft() {
        return BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath("botania", "altar_craft"))
                .orElse(SoundEvents.PLAYER_LEVELUP);
    }

    /** {@code thaumcraft:runicShieldCharge}. */
    public static SoundEvent runicShieldCharge() {
        return get("runicshieldcharge", SoundEvents.TOTEM_USE);
    }
}
