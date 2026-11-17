package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.item.RelicCurioItem;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRDataComponents;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Amulet of the Oblivion (ItemOblivionAmulet). Absorbing damage for ignis + perditio vis is in
 * {@link com.integral.forgottenrelics.event.FRCombatEvents}; here, each tick: 0.08 % chance to release a random part
 * of the stored damage (capped to a random 0..100 with 90 % chance when above 100), otherwise 0.04 % chance of a
 * random curse: mining fatigue, blindness, weakness or wither, 100..2100 ticks, amplifier 0..2.
 */
public class OblivionAmuletItem extends RelicCurioItem {
    public OblivionAmuletItem(Properties properties) {
        super(properties, 4, "necklace");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        Float stored = stack.get(FRDataComponents.STORED_DAMAGE.get());
        if (stored != null && Math.random() <= 0.0008) {
            // the original read the float with getInt, i.e. it released only once at least 1 damage was stored
            if ((int) stored.floatValue() > 0) {
                float damage = (float) (stored * Math.random());
                if (damage > 100 && Math.random() <= 0.9) {
                    damage = (float) (100 * Math.random());
                }
                stack.set(FRDataComponents.STORED_DAMAGE.get(), stored - damage);
                entity.hurtServer(level, FRDamageSources.oblivion(level), damage);
            }
        } else if (Math.random() <= 0.0004) {
            double omega = Math.random();
            Holder<MobEffect> effect;
            if (omega <= 0.25) {
                effect = MobEffects.MINING_FATIGUE;
            } else if (omega <= 0.5) {
                effect = MobEffects.BLINDNESS;
            } else if (omega <= 0.75) {
                effect = MobEffects.WEAKNESS;
            } else {
                effect = MobEffects.WITHER;
            }
            entity.addEffect(new MobEffectInstance(effect, (int) (100 + Math.random() * 2000), (int) (Math.random() * 3), true, true));
        }
    }

    /** Adds absorbed damage to the amulet (original {@code IDamageStored} += amount). */
    public static void store(ItemStack stack, float amount) {
        stack.set(FRDataComponents.STORED_DAMAGE.get(), stack.getOrDefault(FRDataComponents.STORED_DAMAGE.get(), 0F) + amount);
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemOblivionAmulet1", "ItemOblivionAmulet2", "ItemOblivionAmulet3", "FREmpty",
                    "ItemOblivionAmulet4", "FREmpty", "ItemOblivionAmulet5", "FREmpty"}) {
                lines.accept(lore(key));
            }
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
        Float stored = stack.get(FRDataComponents.STORED_DAMAGE.get());
        if (stored != null) {
            lines.accept(lore("ItemOblivionAmuletDamage") + " " + Math.round(stored * 100.0) / 100.0);
            lines.accept(lore("FREmpty"));
        }
    }
}
