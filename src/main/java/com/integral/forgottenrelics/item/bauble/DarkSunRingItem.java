package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.item.RelicCurioItem;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Ring of the Seven Suns (ItemDarkSunRing): keeps the wearer extinguished; the damage cap, fire-to-healing,
 * deflection and the 25 % attack amplification are in {@link com.integral.forgottenrelics.event.FRCombatEvents}.
 * A second ring adds nothing because every check is "has the ring", as in the original.
 */
public class DarkSunRingItem extends RelicCurioItem {
    public DarkSunRingItem(Properties properties) {
        super(properties, 0, "ring");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        if (entity.isOnFire()) {
            entity.clearFire();
        }
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemDarkSunRing1"));
            lines.accept(lore("ItemDarkSunRing2_1") + " " + (int) FRConfig.darkSunRingDamageCap.getAsDouble() + lore("ItemDarkSunRing2_2"));
            lines.accept(lore("ItemDarkSunRing3"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemDarkSunRing4_1") + " " + (int) (FRConfig.darkSunRingDeflectChance.getAsDouble() * 100F) + lore("ItemDarkSunRing4_2"));
            lines.accept(lore("ItemDarkSunRing5"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemDarkSunRing6"));
            lines.accept(lore("ItemDarkSunRing7"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemDarkSunRing8"));
            lines.accept(lore("ItemDarkSunRing9"));
            lines.accept(lore("FREmpty"));
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
