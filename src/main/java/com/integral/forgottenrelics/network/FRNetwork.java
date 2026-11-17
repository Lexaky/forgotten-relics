package com.integral.forgottenrelics.network;

import com.integral.forgottenrelics.ForgottenRelics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The one S2C packet the port still needs (the original had seven; Thaumaturge and Botania particles cover the rest).
 * Its client handler is registered in {@code client.FRClient}.
 */
@EventBusSubscriber(modid = ForgottenRelics.MODID)
public final class FRNetwork {
    /** Original {@code sendToAllAround(..., new TargetPoint(dimension, x, y, z, 128))}. */
    public static final double RANGE = 128.0;

    private FRNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(LightningPayload.TYPE, LightningPayload.STREAM_CODEC);
    }

    public static void lightning(ServerLevel level, Vec3 from, Vec3 to, int duration, float width) {
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, RANGE, new LightningPayload(from, to, duration, width));
    }
}
