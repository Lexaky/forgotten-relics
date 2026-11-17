package com.integral.forgottenrelics.item.passive;

import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.util.XpHelper;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tome of Ageless Wisdom (ItemXPTome): while active and anywhere in the inventory, moves 5 experience points per
 * tick from the player into the tome (absorption) or back (extraction). Right-click switches the mode,
 * Shift+Right-click toggles it on and off. Glints while active.
 */
public class XpTomeItem extends RelicItem {
    public static final int XP_PORTION = 5;

    public XpTomeItem(Properties properties) {
        super(properties, 0);
    }

    public static boolean active(ItemStack stack) {
        return stack.getOrDefault(FRDataComponents.ACTIVE.get(), false);
    }

    public static boolean absorption(ItemStack stack) {
        return stack.getOrDefault(FRDataComponents.ABSORPTION.get(), true);
    }

    public static int stored(ItemStack stack) {
        return stack.getOrDefault(FRDataComponents.STORED_XP.get(), 0);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (owner instanceof Player player && active(stack)) {
            transfer(stack, player);
        }
    }

    /** One tick of the original {@code onUpdate}. */
    public static void transfer(ItemStack stack, Player player) {
        if (absorption(stack)) {
            int xp = XpHelper.playerXp(player);
            int moved = Math.min(xp, XP_PORTION);
            if (moved > 0) {
                XpHelper.drainPlayerXp(player, moved);
                stack.set(FRDataComponents.STORED_XP.get(), stored(stack) + moved);
            }
        } else {
            int moved = Math.min(stored(stack), XP_PORTION);
            if (moved > 0) {
                stack.set(FRDataComponents.STORED_XP.get(), stored(stack) - moved);
                XpHelper.addPlayerXp(player, moved);
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS; // the server toggles and syncs the components
        }
        if (!player.isShiftKeyDown()) {
            stack.set(FRDataComponents.ABSORPTION.get(), !absorption(stack));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS,
                    1.0F, (float) (0.4F + Math.random() * 0.1F));
        } else {
            boolean on = !active(stack);
            stack.set(FRDataComponents.ACTIVE.get(), on);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), on ? TTSounds.hhOn() : TTSounds.hhOff(), SoundSource.PLAYERS,
                    1.0F, (float) (0.8F + Math.random() * 0.2F));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return active(stack);
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        String mode = !active(stack) ? lore("ItemXPTomeDeactivated") : absorption(stack) ? lore("ItemXPTomeAbsorption") : lore("ItemXPTomeExtraction");
        if (shift()) {
            for (String key : new String[]{"ItemXPTome1", "FREmpty", "ItemXPTome2", "ItemXPTome3", "ItemXPTome4", "FREmpty",
                    "ItemXPTome5", "ItemXPTome6", "FREmpty", "ItemXPTome7", "ItemXPTome8", "ItemXPTome9"}) {
                lines.accept(lore(key));
            }
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
        lines.accept(lore("ItemXPTomeMode") + " " + mode);
        lines.accept(lore("FREmpty"));
        lines.accept(lore("ItemXPTomeExp"));
        int xp = stored(stack);
        lines.accept(lore("FRCode6") + xp + " " + lore("ItemXPTomeUnits") + " " + XpHelper.levelForExperience(xp) + " " + lore("ItemXPTomeLevels"));
    }
}
