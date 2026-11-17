package com.integral.forgottenrelics.registry;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.ForgottenRelics;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Datapack conditions. {@code forgottenrelics:false_justice_enabled} gates the False Justice research entry and
 * recipe on the {@code falseJusticeEnabled} option, as the original removed the research when it was off (existing
 * copies and the creative tab are unaffected, as in the original).
 */
public final class FRConditions {
    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITIONS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, ForgottenRelics.MODID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<FalseJusticeEnabled>> FALSE_JUSTICE_ENABLED =
            CONDITIONS.register("false_justice_enabled", () -> FalseJusticeEnabled.CODEC);

    public record FalseJusticeEnabled() implements ICondition {
        public static final FalseJusticeEnabled INSTANCE = new FalseJusticeEnabled();
        public static final MapCodec<FalseJusticeEnabled> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public boolean test(IContext context) {
            try {
                return FRConfig.falseJusticeEnabled.getAsBoolean();
            } catch (IllegalStateException notLoadedYet) {
                return true; // config default
            }
        }

        @Override
        public MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

    public static void register(IEventBus modBus) {
        CONDITIONS.register(modBus);
    }

    private FRConditions() {
    }
}
