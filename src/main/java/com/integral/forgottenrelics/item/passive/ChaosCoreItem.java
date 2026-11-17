package com.integral.forgottenrelics.item.passive;

import com.integral.forgottenrelics.item.RelicItem;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Chaos Core (ItemChaosCore): anywhere in the inventory. Damage redirection and the x0..2 multiplier are in
 * {@link com.integral.forgottenrelics.event.FRCombatEvents}; here a 0.0208 %-per-tick chance of a random effect.
 */
public class ChaosCoreItem extends RelicItem {
    /**
     * 1.7 potion ids 1..21 in order; ids 6 (instant health) and 7 (instant damage) were replaced by 20 (wither).
     */
    static final List<Holder<MobEffect>> EFFECTS = List.of(MobEffects.SPEED, MobEffects.SLOWNESS, MobEffects.HASTE,
            MobEffects.MINING_FATIGUE, MobEffects.STRENGTH, MobEffects.WITHER, MobEffects.WITHER, MobEffects.JUMP_BOOST,
            MobEffects.NAUSEA, MobEffects.REGENERATION, MobEffects.RESISTANCE, MobEffects.FIRE_RESISTANCE,
            MobEffects.WATER_BREATHING, MobEffects.INVISIBILITY, MobEffects.BLINDNESS, MobEffects.NIGHT_VISION,
            MobEffects.HUNGER, MobEffects.WEAKNESS, MobEffects.POISON, MobEffects.WITHER, MobEffects.HEALTH_BOOST);

    public ChaosCoreItem(Properties properties) {
        super(properties, 2);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (owner instanceof LivingEntity living && Math.random() <= 0.000208) {
            applyRandomEffect(living);
        }
    }

    /** Original: id 1 + (int) (random * 21), 100..2500 ticks, amplifier 0..2. */
    public static void applyRandomEffect(LivingEntity living) {
        int id = 1 + (int) (Math.random() * 21);
        living.addEffect(new MobEffectInstance(EFFECTS.get(id - 1), 100 + (int) (Math.random() * 2400), (int) (Math.random() * 3), false, true));
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemChaosCore1", "ItemChaosCore2", "ItemChaosCore3", "FREmpty", "ItemChaosCore4",
                    "ItemChaosCore5", "ItemChaosCore6", "FREmpty", "ItemChaosCore7"}) {
                lines.accept(lore(key));
            }
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
    }
}
