package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicCurioItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRItems;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Dormant Nebulous Core (ItemDormantArcanum): no effects; each tick pays 3 centivis of every primal and counts its
 * lifetime down on success; at 0 it turns back into a Nebulous Core in the same slot. A core without a lifetime
 * (taken from the creative tab) stays dormant, as in the original.
 */
public class DormantNebulousCoreItem extends RelicCurioItem {
    public DormantNebulousCoreItem(Properties properties) {
        super(properties, 0, "necklace");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        Integer lifetime = stack.get(FRDataComponents.DORMANT_LIFETIME.get());
        if (lifetime == null || entity.level().isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        if (lifetime > 0) {
            var cost = VisHelper.cost(VisHelper.PRIMALS.get(0), 3, VisHelper.PRIMALS.get(1), 3, VisHelper.PRIMALS.get(2), 3,
                    VisHelper.PRIMALS.get(3), 3, VisHelper.PRIMALS.get(4), 3, VisHelper.PRIMALS.get(5), 3);
            if (VisHelper.consumeFromInventory(player, cost)) {
                stack.set(FRDataComponents.DORMANT_LIFETIME.get(), lifetime - 1);
            }
        } else {
            replaceInSlot(slot, new ItemStack(FRItems.NEBULOUS_CORE.get()));
        }
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemDormantArcanum1"));
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        Integer lifetime = stack.get(FRDataComponents.DORMANT_LIFETIME.get());
        if (lifetime != null) {
            lines.accept(lore("FREmpty"));
            lines.accept(lore("FRCode6") + (lifetime * 2) / 100.0D + lore("ItemDormantArcanum2"));
        }
    }
}
