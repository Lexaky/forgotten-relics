package com.integral.forgottenrelics.item.utility;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.item.RelicItem;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The Paradox (ItemParadox, a sword): a left-click deals a random 0..paradoxDamageCap to the target and the rest of
 * the cap to the attacker, replacing the normal attack; it repairs 1 durability every 20 ticks in the inventory.
 */
public class ParadoxItem extends RelicItem {
    public ParadoxItem(Properties properties) {
        super(properties, 8);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (player.level() instanceof ServerLevel level) {
            strike(level, player, target, Math.random());
        }
        return true;
    }

    /** Original: target takes {@code random * cap}, the attacker {@code cap - that}, both as player damage. */
    public static void strike(ServerLevel level, Player player, Entity target, double random) {
        double cap = FRConfig.paradoxDamageCap.getAsDouble();
        double dealt = random * cap;
        target.hurtServer(level, level.damageSources().playerAttack(player), (float) dealt);
        player.hurtServer(level, level.damageSources().playerAttack(player), (float) (cap - dealt));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (stack.isDamaged() && owner.tickCount % 20 == 0 && owner instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemParadox1", "ItemParadox2", "ItemParadox3", "ItemParadox4", "ItemParadox5"}) {
                lines.accept(lore(key));
            }
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
        lines.accept(lore("ItemParadoxDamage_1") + (int) FRConfig.paradoxDamageCap.getAsDouble() + lore("ItemParadoxDamage_2"));
    }
}
