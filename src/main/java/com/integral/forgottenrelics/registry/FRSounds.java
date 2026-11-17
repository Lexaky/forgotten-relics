package com.integral.forgottenrelics.registry;

import com.integral.forgottenrelics.ForgottenRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sounds of the original sounds.json ("sound.discharge", "sound.lunarFlare", "sound.starfall"). */
public final class FRSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ForgottenRelics.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> DISCHARGE = register("discharge");
    public static final DeferredHolder<SoundEvent, SoundEvent> LUNAR_FLARE = register("lunar_flare");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARFALL = register("starfall");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(ForgottenRelics.id(path)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private FRSounds() {
    }
}
