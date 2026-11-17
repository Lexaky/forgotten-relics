package com.integral.forgottenrelics.registry;

import com.integral.forgottenrelics.ForgottenRelics;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.netty.buffer.ByteBuf;

/** Per-stack state that the original kept in item NBT (key names noted on each component). */
public final class FRDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ForgottenRelics.MODID);

    /** Oblivion Amulet {@code IDamageStored}: absorbed damage waiting to be released. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> STORED_DAMAGE = COMPONENTS.registerComponentType(
            "stored_damage", b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));

    /** Dormant Nebulous Core {@code ILifetime}: successful vis payments left until it wakes up. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> DORMANT_LIFETIME = COMPONENTS.registerComponentType(
            "dormant_lifetime", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Deific Amulet {@code ICooldown}: ticks until the next prolonged invincibility. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RELIC_COOLDOWN = COMPONENTS.registerComponentType(
            "relic_cooldown", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Tome of Broken Fates {@code IFateCooldown}: ticks until it can save its owner again. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FATE_COOLDOWN = COMPONENTS.registerComponentType(
            "fate_cooldown", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Tome of Ageless Wisdom {@code XPStored}: experience points in the tome. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> STORED_XP = COMPONENTS.registerComponentType(
            "stored_xp", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Tome of Ageless Wisdom {@code IsActive}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ACTIVE = COMPONENTS.registerComponentType(
            "active", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Tome of Ageless Wisdom {@code AbsorptionMode} (absent = absorption, as the original default). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ABSORPTION = COMPONENTS.registerComponentType(
            "absorption", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    /** Dimensional Mirror {@code IStoredX/Y/Z} + {@code IDimensionID}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> MIRROR_TARGET = COMPONENTS.registerComponentType(
            "mirror_target", b -> b.persistent(net.minecraft.core.GlobalPos.CODEC).networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));

    /** Devourer of the Void {@code IDetectedX/Y/Z}: the obelisk being drained. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> OBELISK_POS = COMPONENTS.registerComponentType(
            "obelisk_pos", b -> b.persistent(net.minecraft.core.BlockPos.CODEC).networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    /** Shiny Stone {@code LastX/LastY/LastZ/Static/HealRate}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StandState>> STAND_STATE = COMPONENTS.registerComponentType(
            "stand_state", b -> b.persistent(StandState.CODEC).networkSynchronized(StandState.STREAM_CODEC));

    /** Tome of Predestiny {@code target/dist/reDist/ticksTillExpire/ticksCooldown/AttackCooldown}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TelekinesisState>> TELEKINESIS = COMPONENTS.registerComponentType(
            "telekinesis", b -> b.persistent(TelekinesisState.CODEC).networkSynchronized(TelekinesisState.STREAM_CODEC));

    public record TelekinesisState(int target, double reDist, int ticksTillExpire, int cooldown, int attackCooldown) {
        public static final TelekinesisState EMPTY = new TelekinesisState(-1, -1, 0, 0, 0);
        public static final Codec<TelekinesisState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("target").forGetter(TelekinesisState::target),
                Codec.DOUBLE.fieldOf("re_dist").forGetter(TelekinesisState::reDist),
                Codec.INT.fieldOf("expire").forGetter(TelekinesisState::ticksTillExpire),
                Codec.INT.fieldOf("cooldown").forGetter(TelekinesisState::cooldown),
                Codec.INT.fieldOf("attack_cooldown").forGetter(TelekinesisState::attackCooldown)).apply(i, TelekinesisState::new));
        public static final StreamCodec<ByteBuf, TelekinesisState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT, TelekinesisState::target, ByteBufCodecs.DOUBLE, TelekinesisState::reDist,
                ByteBufCodecs.VAR_INT, TelekinesisState::ticksTillExpire, ByteBufCodecs.VAR_INT, TelekinesisState::cooldown,
                ByteBufCodecs.VAR_INT, TelekinesisState::attackCooldown, TelekinesisState::new);

        public TelekinesisState withTarget(int t, double re) {
            return new TelekinesisState(t, re, ticksTillExpire, cooldown, attackCooldown);
        }
    }

    public record StandState(double lastX, double lastY, double lastZ, int still, int healRate) {
        public static final StandState EMPTY = new StandState(0, 0, 0, 0, 0);
        public static final Codec<StandState> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("last_x").forGetter(StandState::lastX),
                Codec.DOUBLE.fieldOf("last_y").forGetter(StandState::lastY),
                Codec.DOUBLE.fieldOf("last_z").forGetter(StandState::lastZ),
                Codec.INT.fieldOf("static").forGetter(StandState::still),
                Codec.INT.fieldOf("heal_rate").forGetter(StandState::healRate)).apply(i, StandState::new));
        public static final StreamCodec<ByteBuf, StandState> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, StandState::lastX, ByteBufCodecs.DOUBLE, StandState::lastY, ByteBufCodecs.DOUBLE, StandState::lastZ,
                ByteBufCodecs.VAR_INT, StandState::still, ByteBufCodecs.VAR_INT, StandState::healRate, StandState::new);
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }

    private FRDataComponents() {
    }
}
