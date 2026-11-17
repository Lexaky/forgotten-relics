package com.integral.forgottenrelics.item.passive;

import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicItem;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Omega Core (ItemOmegaCore): creative-only. Every tick adds 1 vis of every primal to a random non-full wand in
 * the inventory; its owner cannot die ({@link com.integral.forgottenrelics.event.FRCombatEvents}).
 */
public class OmegaCoreItem extends RelicItem {
    public OmegaCoreItem(Properties properties) {
        super(properties, 0);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!(owner instanceof Player player)) {
            return;
        }
        List<ItemStack> wands = VisHelper.wands(player);
        if (wands.isEmpty()) {
            return;
        }
        for (var aspect : VisHelper.PRIMALS) {
            ItemStack wand = VisHelper.randomWandWithRoom(wands, aspect, player.getRandom());
            if (!wand.isEmpty()) {
                VisHelper.addVis(wand, aspect, 1);
            }
        }
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        lines.accept(shift() ? lore("FREmpty") : lore("FRShiftTooltip"));
    }
}
