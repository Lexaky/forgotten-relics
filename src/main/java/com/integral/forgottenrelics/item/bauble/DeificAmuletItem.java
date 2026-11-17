package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicCurioItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.function.Consumer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Deific Amulet (ItemDeificAmulet): every tick on the server removes all effects (config), extinguishes, refills air
 * for 10 vis of aer and aqua (x deificAmuletVisMult) when it runs out, and stretches fresh invincibility frames to 40
 * ticks with a 32-tick cooldown (config).
 */
public class DeificAmuletItem extends RelicCurioItem {
    public DeificAmuletItem(Properties properties) {
        super(properties, 0, "necklace");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        if (entity.level().isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        if (FRConfig.deificAmuletEffectImmunity.getAsBoolean()) {
            player.removeAllEffects();
        }
        if (player.isOnFire()) {
            player.clearFire();
        }
        if (player.getAirSupply() == 0) {
            int cost = (int) (1000 * FRConfig.deificAmuletVisMult.getAsDouble());
            if (VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, cost, TTAspects.AQUA, cost))) {
                player.setAirSupply(300);
            }
        }
        if (FRConfig.deificAmuletInvincibility.getAsBoolean()) {
            int cooldown = stack.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0);
            if (cooldown == 0 && player.invulnerableTime > 10) {
                player.invulnerableTime = 40;
                cooldown = 32;
            }
            if (cooldown > 0) {
                cooldown--;
            }
            if (cooldown != stack.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0)) {
                stack.set(FRDataComponents.RELIC_COOLDOWN.get(), cooldown);
            }
        }
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            if (FRConfig.deificAmuletEffectImmunity.getAsBoolean()) {
                lines.accept(lore("ItemDeificAmulet1"));
            }
            lines.accept(lore("ItemDeificAmulet2"));
            if (FRConfig.deificAmuletInvincibility.getAsBoolean()) {
                lines.accept(lore("ItemDeificAmulet3"));
            }
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemDeificAmulet4"));
            lines.accept(lore("ItemDeificAmulet5"));
            lines.accept(lore("FREmpty"));
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
