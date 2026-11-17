package com.integral.forgottenrelics.item.bauble;

import com.google.common.collect.Multimap;
import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.item.RelicCurioItem;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Ancient Aegis (ItemAncientAegis, belt): +1 knockback resistance, heals 1 every 20 ticks; damage reduction and
 * taking 40 % of nearby players' damage are in {@link com.integral.forgottenrelics.event.FRCombatEvents}.
 */
public class AncientAegisItem extends RelicCurioItem {
    public AncientAegisItem(Properties properties) {
        super(properties, 0, "belt");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        if (!entity.level().isClientSide() && entity.tickCount % 20 == 0 && entity.getMaxHealth() != entity.getHealth()) {
            entity.heal(1);
        }
    }

    @Override
    protected void attributeModifiers(ItemStack stack, Identifier slotId, Multimap<Holder<Attribute>, AttributeModifier> out) {
        out.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(slotId, 1, AttributeModifier.Operation.ADD_VALUE));
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemAncientAegis1_1") + " " + (int) (FRConfig.ancientAegisDamageReduction.getAsDouble() * 100F) + lore("ItemAncientAegis1_2"));
            lines.accept(lore("ItemAncientAegis2"));
            lines.accept(lore("ItemAncientAegis3"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemAncientAegis4"));
            lines.accept(lore("ItemAncientAegis5"));
            lines.accept(lore("FREmpty"));
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
