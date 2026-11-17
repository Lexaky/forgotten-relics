package com.integral.forgottenrelics.network;

import com.integral.forgottenrelics.ForgottenRelics;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * Original {@code LightningMessage}: a TC4 {@code FXLightningBolt} from one point to another. TC4 type 0 is drawn twice,
 * a violet glow (0.6, 0.3, 0.6) and a pink core (1.0, 0.6, 1.0); {@code width} is TC4's half-width in blocks and
 * {@code duration} the bolt's lifetime in ticks.
 */
public record LightningPayload(Vec3 from, Vec3 to, int duration, float width) implements CustomPacketPayload {
    public static final Type<LightningPayload> TYPE = new Type<>(ForgottenRelics.id("lightning"));
    public static final StreamCodec<ByteBuf, LightningPayload> STREAM_CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC, LightningPayload::from,
            Vec3.STREAM_CODEC, LightningPayload::to,
            ByteBufCodecs.VAR_INT, LightningPayload::duration,
            ByteBufCodecs.FLOAT, LightningPayload::width,
            LightningPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
