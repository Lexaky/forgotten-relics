package com.integral.forgottenrelics.util;

import com.integral.forgottenrelics.compat.TTEffects;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side replacements for the original's client packets. Portal traces and wisp bursts are particles sent with
 * {@link ServerLevel#sendParticles}; bolts and arcs are Thaumaturge's own effects ({@link TTEffects}).
 */
public final class FRFx {
    private FRFx() {
    }

    /** Original {@code PortalTraceMessage}: portal particles along the path of a teleport. */
    public static void portalTrace(ServerLevel level, Vec3 from, Vec3 to) {
        line(level, ParticleTypes.PORTAL, from, to, 4, 0.2);
    }

    /**
     * Original {@code LightningMessage} / {@code Main.proxy.lightning(..., duration, curve, speed, type 0, width)}: a TC4
     * fractal bolt drawn by Botania's bolt renderer. Curve and speed (how fast the bolt grew) have no counterpart.
     */
    public static void lightning(ServerLevel level, Vec3 from, Vec3 to, int duration, float width) {
        com.integral.forgottenrelics.network.FRNetwork.lightning(level, from, to, duration, width);
    }

    /** Original {@code ArcLightningMessage}: a TC4 arc of colour (r, g, b) and drift {@code h}. */
    public static void arc(ServerLevel level, Vec3 from, Vec3 to, float r, float g, float b, float h) {
        TTEffects.arc(level, from, to, r, g, b, h);
    }

    /** Original {@code spawnParticle("portal", ...)} bursts around a point. */
    public static void portalBurst(ServerLevel level, Vec3 at, int count) {
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y, at.z, count, 0.5, 0.5, 0.5, 1.0);
    }

    /**
     * Original {@code ApotheosisParticleMessage}/{@code LunarBurstMessage}/{@code LunarFlaresParticleMessage}: a burst of
     * Botania wisps flying outwards from a point.
     */
    public static void wispBurst(ServerLevel level, Vec3 at, int count, float r, float g, float b, float speed) {
        for (int i = 0; i < count; i++) {
            com.integral.forgottenrelics.compat.BotaniaBridge.wisp(level, at.x, at.y, at.z, r, g, b, 0.3F + (float) Math.random() * 0.3F,
                    (Math.random() - 0.5) * speed, (Math.random() - 0.5) * speed, (Math.random() - 0.5) * speed, 1.0F);
        }
    }

    private static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int perBlock, double spread) {
        double length = from.distanceTo(to);
        int steps = Math.max(2, (int) (length * perBlock));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            level.sendParticles(particle, p.x, p.y, p.z, 1, spread, spread, spread, 0.0);
        }
    }
}
