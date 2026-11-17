package com.integral.forgottenrelics.item.utility;

import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.item.RelicItem;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ghastly Skull (itemGhastlySkull): the original's debug item and research-category icon. Right-click sets the
 * user's health to 1 with a flash. Its tooltip lines were never translated, so the item shows them as raw keys in
 * the original; here only the shared lines are shown.
 */
public class GhastlySkullItem extends RelicItem {
    public GhastlySkullItem(Properties properties) {
        super(properties, 3);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.setHealth(1);
        if (level instanceof ServerLevel server) {
            Vec3 c = player.getBoundingBox().getCenter();
            TTEffects.burst(server, c.x, c.y, c.z, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (!shift()) {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
    }
}
