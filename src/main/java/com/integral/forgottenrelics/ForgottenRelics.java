package com.integral.forgottenrelics;

import com.integral.forgottenrelics.registry.FRCreativeTabs;
import com.integral.forgottenrelics.registry.FRItems;
import com.integral.forgottenrelics.registry.FRSounds;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forgotten Relics for Thaumaturge and Botania Revived. Research, recipes, item aspects and damage types are data
 * ({@code src/main/resources/data}); this class wires the code-side registries and the config.
 */
@Mod(ForgottenRelics.MODID)
public final class ForgottenRelics {
    public static final String MODID = "forgottenrelics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public ForgottenRelics(IEventBus modBus, ModContainer container) {
        com.integral.forgottenrelics.registry.FRDataComponents.register(modBus);
        com.integral.forgottenrelics.registry.FRConditions.register(modBus);
        FRItems.register(modBus);
        com.integral.forgottenrelics.registry.FREntities.register(modBus);
        FRSounds.register(modBus);
        FRCreativeTabs.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, FRConfig.SPEC);
        if (Boolean.getBoolean("forgottenrelics.gametest")) {
            modBus.addListener(com.integral.forgottenrelics.gametest.FRGameTests::register);
        }
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
