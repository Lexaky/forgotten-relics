package com.integral.forgottenrelics.event;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.registry.FRItems;
import com.leclowndu93150.thaumaturge.api.infusion.InfusionCraftedEvent;
import com.leclowndu93150.thaumaturge.api.warp.WarpHelper;
import com.leclowndu93150.thaumaturge.api.warp.WarpType;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * TC4 {@code ThaumcraftApi.addWarpToItem}: warp the crafter receives when the infusion finishes. Thaumaturge's
 * {@code thaumaturge:warp} item data map is worn-gear warp, not crafting warp, so the original values are applied
 * here on {@link InfusionCraftedEvent}. Split like TC4 research warp: half (rounded down) permanent, the rest normal.
 */
@EventBusSubscriber(modid = ForgottenRelics.MODID)
public final class FRCraftingWarp {
    /** Original values from RelicsResearchRegistry.integrateResearch. */
    static final Map<Supplier<Item>, Integer> WARP = Map.of(
            FRItems.CHAOS_TOME::get, 4,
            FRItems.FATE_TOME::get, 6,
            FRItems.PARADOX::get, 8,
            FRItems.OBLIVION_AMULET::get, 4,
            FRItems.FALSE_JUSTICE::get, 10,
            FRItems.OBELISK_DRAINER::get, 3);

    private FRCraftingWarp() {
    }

    /** Crafting warp of a stack, 0 for anything else. */
    public static int warpFor(ItemStack stack) {
        for (var entry : WARP.entrySet()) {
            if (stack.is(entry.getKey().get())) {
                return entry.getValue();
            }
        }
        return 0;
    }

    /** Applies crafting warp to the player: half permanent, the rest normal. */
    public static void apply(ServerPlayer player, int warp) {
        if (warp <= 0) {
            return;
        }
        int permanent = warp / 2;
        if (permanent > 0) {
            WarpHelper.addWarp(player, permanent, WarpType.PERMANENT);
        }
        WarpHelper.addWarp(player, warp - permanent, WarpType.NORMAL);
    }

    @SubscribeEvent
    public static void onInfusion(InfusionCraftedEvent event) {
        if (event.getPlayer() != null) {
            apply(event.getPlayer(), warpFor(event.getResult()));
        }
    }
}
