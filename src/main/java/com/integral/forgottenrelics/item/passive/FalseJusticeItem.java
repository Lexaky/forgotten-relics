package com.integral.forgottenrelics.item.passive;

import com.integral.forgottenrelics.item.RelicItem;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;

/**
 * False Justice (ItemFalseJustice): anywhere in the inventory, all damage dealt and taken becomes doubled true damage
 * and other deaths are cancelled ({@link com.integral.forgottenrelics.event.FRCombatEvents}). Its research and recipe
 * disappear when {@code falseJusticeEnabled} is off ({@link com.integral.forgottenrelics.registry.FRConditions}).
 */
public class FalseJusticeItem extends RelicItem {
    public FalseJusticeItem(Properties properties) {
        super(properties, 4);
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemFalseJustice1"));
            lines.accept(lore("ItemFalseJustice2"));
            lines.accept(lore("ItemFalseJustice3"));
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
    }
}
