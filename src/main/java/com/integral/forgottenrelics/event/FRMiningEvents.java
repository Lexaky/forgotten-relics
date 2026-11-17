package com.integral.forgottenrelics.event;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.compat.CuriosBridge;
import com.integral.forgottenrelics.item.bauble.MiningCharmItem;
import com.integral.forgottenrelics.registry.FRItems;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Original {@code RelicsEventHandler.miningStuff}: speed x (1 + boosts of the worn charms; both charms stack). */
@EventBusSubscriber(modid = ForgottenRelics.MODID)
public final class FRMiningEvents {
    private FRMiningEvents() {
    }

    public static float multiplier(Player player) {
        float boost = 1.0F;
        if (CuriosBridge.hasBauble(player, FRItems.ADVANCED_MINING_CHARM.get())) {
            boost += (float) ((MiningCharmItem) FRItems.ADVANCED_MINING_CHARM.get()).boost();
        }
        if (CuriosBridge.hasBauble(player, FRItems.MINING_CHARM.get())) {
            boost += (float) ((MiningCharmItem) FRItems.MINING_CHARM.get()).boost();
        }
        return boost;
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(event.getNewSpeed() * multiplier(event.getEntity()));
    }
}
