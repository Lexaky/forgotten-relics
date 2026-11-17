package com.integral.forgottenrelics.item.bauble;

import com.google.common.collect.Multimap;
import com.integral.forgottenrelics.item.RelicCurioItem;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/**
 * Mining Charm and Ethereal Mining Charm (ItemMiningCharm / ItemAdvancedMiningCharm): extra block reach while worn
 * (Botania's {@code setExtraReach} → the {@code block_interaction_range} attribute) and a mining speed boost applied in
 * {@link com.integral.forgottenrelics.event.FRMiningEvents}.
 */
public class MiningCharmItem extends RelicCurioItem {
    private final DoubleSupplier boost;
    private final DoubleSupplier reach;
    private final String lorePrefix;

    public MiningCharmItem(Properties properties, DoubleSupplier boost, DoubleSupplier reach, String lorePrefix) {
        super(properties, 0, "ring");
        this.boost = boost;
        this.reach = reach;
        this.lorePrefix = lorePrefix;
    }

    /** Extra mining speed, 1.0 = +100 %. */
    public double boost() {
        return boost.getAsDouble();
    }

    @Override
    protected void attributeModifiers(ItemStack stack, Identifier slotId, Multimap<Holder<Attribute>, AttributeModifier> out) {
        out.put(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(slotId, reach.getAsDouble(), AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore(lorePrefix + "1_1") + " " + (int) (boost() * 100F) + lore(lorePrefix + "1_2"));
            lines.accept(lore(lorePrefix + "2_1") + " " + (float) reach.getAsDouble() + lore(lorePrefix + "2_2"));
            lines.accept(lore("FREmpty"));
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
