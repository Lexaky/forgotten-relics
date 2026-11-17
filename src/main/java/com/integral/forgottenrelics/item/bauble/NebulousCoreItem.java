package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicCurioItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRItems;
import com.integral.forgottenrelics.util.SuperpositionHandler;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.items.IVisDiscountGear;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Nebulous Core (ItemArcanum): 35 % vis discount; each tick on the server either (2.5 % x arcanumGenRate) adds 1 vis
 * of a random primal to a random non-full wand, or (0.0208 %) teleports the wearer up to 32 blocks, or (0.0027 %)
 * falls dormant for (1200..7200) x dormantArcanumVisMult paid ticks. The 40 % dodge is in
 * {@link com.integral.forgottenrelics.event.FRCombatEvents}.
 */
public class NebulousCoreItem extends RelicCurioItem implements IVisDiscountGear {
    public static final int VIS_DISCOUNT = 35;

    public NebulousCoreItem(Properties properties) {
        super(properties, 0, "necklace");
    }

    @Override
    public int getVisDiscount(ItemStack stack) {
        return VIS_DISCOUNT;
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        if (entity.level().isClientSide()) {
            return;
        }
        if (entity instanceof Player player && Math.random() <= 0.025 * FRConfig.arcanumGenRate.getAsDouble()) {
            List<ItemStack> wands = VisHelper.wands(player);
            if (!wands.isEmpty()) {
                // (int) (random * 5): the original never picked the sixth primal (perditio)
                ResourceKey<IAspect> aspect = VisHelper.PRIMALS.get((int) (Math.random() * 5));
                ItemStack wand = VisHelper.randomWandWithRoom(wands, aspect, player.getRandom());
                if (!wand.isEmpty()) {
                    VisHelper.addVis(wand, aspect, 1);
                }
            }
        } else if (Math.random() <= 0.000208) {
            for (int counter = 0; counter <= 32; counter++) {
                if (SuperpositionHandler.validTeleportRandomly(entity, entity.level(), 32)) {
                    break;
                }
            }
        } else if (Math.random() <= 0.000027 && entity instanceof Player) {
            replaceInSlot(slot, dormant((int) ((1200 + Math.random() * 6000) * FRConfig.dormantArcanumVisMult.getAsDouble())));
        }
    }

    /** A dormant core that wakes up after the given number of paid ticks. */
    public static ItemStack dormant(int lifetime) {
        ItemStack stack = new ItemStack(FRItems.NEBULOUS_CORE_DORMANT.get());
        stack.set(FRDataComponents.DORMANT_LIFETIME.get(), lifetime);
        return stack;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        lines.accept(lore("ItemArcanum1") + " " + VIS_DISCOUNT + "%");
        lines.accept(lore("FREmpty"));
        if (shift()) {
            for (String key : new String[]{"ItemArcanum2", "ItemArcanum3", "ItemArcanum4", "ItemArcanum5", "ItemArcanum6", "FREmpty",
                    "ItemArcanum7", "ItemArcanum8", "ItemArcanum9", "FREmpty"}) {
                lines.accept(lore(key));
            }
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
