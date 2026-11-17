package com.integral.forgottenrelics.compat;

import com.leclowndu93150.thaumaturge.content.effect.Effects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

/**
 * Thaumcraft client effects the original asked for through {@code Thaumcraft.proxy} and its own packets
 * ({@code BurstMessage}, {@code ArcLightningMessage}). Thaumaturge ports the same effects and
 * sends them itself from {@link Effects} (an internal {@code content} class, used only here).
 */
public final class TTEffects {
    private TTEffects() {
    }

    /**
     * TC4 {@code FXBurst} (checked in the TC4 4.2 classes) drew a quad of half-size {@code particleScale · size}, where
     * 1.7's {@code particleScale} is random in 1..2; Thaumaturge's burst particle has half-size {@code 0.1 · size}, so
     * sizes are scaled by 15 (the original's mean extent).
     */
    private static final float BURST_SIZE_SCALE = 15.0F;

    /** {@code Thaumcraft.proxy.burst(world, x, y, z, size)}: the animated node-flash (31 frames). */
    public static void burst(ServerLevel level, double x, double y, double z, float size) {
        Effects.burst(level, new Vec3(x, y, z)).size(size * BURST_SIZE_SCALE).send();
    }

    /** {@code Thaumcraft.proxy.arcLightning(world, x, y, z, tx, ty, tz, r, g, b, h)} (TC4 {@code FXArc}). */
    public static void arc(ServerLevel level, Vec3 from, Vec3 to, float r, float g, float b, float h) {
        Effects.zapArc(level, from).to(to).color(ARGB.colorFromFloat(1.0F, r, g, b) & 0xFFFFFF).drift(h).send();
    }
}
