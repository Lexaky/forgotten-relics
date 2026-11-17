package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.item.RelicItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

/**
 * A tome cast by holding Right-click (original {@code setItemInUse} for 72000 ticks with a bow pose).
 * {@link #castTick} runs on the server every tick of use with the ticks used so far; the original checked
 * {@code count != max && count % 2 == 0} on the remaining count, which with an even maximum is "used ticks even and
 * not the first tick".
 */
public abstract class HeldTomeItem extends RelicItem {
    public static final int USE_TICKS = 72000;

    protected HeldTomeItem(Properties properties, int gearWarp) {
        super(properties, gearWarp);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof Player player) {
            if (level instanceof ServerLevel server) {
                castTick(server, player, stack, remaining);
            } else {
                clientUseTick(player, stack, remaining);
            }
        }
    }

    /** Server, every tick of use; {@code remaining} counts down from {@link #USE_TICKS}. */
    protected abstract void castTick(ServerLevel level, Player player, ItemStack stack, int remaining);

    /** Client, every tick of use (movement the client owns, such as the Soul Tome's slowdown). */
    protected void clientUseTick(Player player, ItemStack stack, int remaining) {
    }

    /** The original's {@code count != max && count % 2 == 0}. */
    protected static boolean everySecondTick(int remaining) {
        return remaining != USE_TICKS && remaining % 2 == 0;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }
}
