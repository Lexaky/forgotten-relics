package com.integral.forgottenrelics.registry;

import com.integral.forgottenrelics.ForgottenRelics;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The original "tabForgottenRelics" with Apotheosis as its icon. */
public final class FRCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForgottenRelics.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(ForgottenRelics.MODID,
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + ForgottenRelics.MODID))
                    .icon(() -> new ItemStack(FRItems.APOTHEOSIS.get()))
                    .displayItems((params, output) -> FRItems.ITEMS.getEntries().forEach(item -> output.accept(item.get())))
                    .build());

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }

    private FRCreativeTabs() {
    }
}
