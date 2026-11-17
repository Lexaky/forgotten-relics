package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.item.RelicCurioItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRDataComponents.StandState;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Shiny Stone (ItemShinyStone): standing still raises a heal rate 1..4 (thresholds 40/80/200 of a counter growing by
 * 4 per check); every {@code shinyStoneCheckrate} ticks the position is compared, and healing of 1 happens every
 * 10/5/2/1 x (checkrate / 4) ticks.
 */
public class ShinyStoneItem extends RelicCurioItem {
    public ShinyStoneItem(Properties properties) {
        super(properties, 0, "necklace");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        int checkrate = FRConfig.shinyStoneCheckrate.getAsInt();
        StandState state = stack.getOrDefault(FRDataComponents.STAND_STATE.get(), StandState.EMPTY);
        if (!entity.level().isClientSide() && entity.tickCount % checkrate == 0) {
            int still = state.still();
            int healRate;
            if (entity.getX() == state.lastX() && entity.getY() == state.lastY() && entity.getZ() == state.lastZ()) {
                int particleNumber = 3;
                healRate = 1;
                if (still >= 40) {
                    healRate = 2;
                    particleNumber = 2;
                }
                if (still >= 80) {
                    healRate = 3;
                    particleNumber = 1;
                }
                if (still >= 200) {
                    healRate = 4;
                    particleNumber = 0;
                }
                for (int counter = particleNumber; counter <= 3; counter++) {
                    spawnEnergyParticle(entity);
                }
                still += 4;
            } else {
                still = 0;
                healRate = 0;
            }
            state = new StandState(entity.getX(), entity.getY(), entity.getZ(), still, healRate);
            stack.set(FRDataComponents.STAND_STATE.get(), state);
        }
        int healRate = state.healRate();
        // the original computed (int) (checkrate / 4.0); at checkrate 1..3 that is 0 and "% 0" crashed it, so use >= 1
        int healCheckrate = Math.max(1, (int) (checkrate / 4.0D));
        if (healRate == 1 && entity.tickCount % (10 * healCheckrate) == 0
                || healRate == 2 && entity.tickCount % (5 * healCheckrate) == 0
                || healRate == 3 && entity.tickCount % (2 * healCheckrate) == 0
                || healRate == 4 && entity.tickCount % healCheckrate == 0) {
            entity.heal(1.0F);
        }
    }

    /**
     * Original {@code spawnEnergyParticle}: an EntityShinyEnergy that flies from a random point 1.5 blocks around the
     * wearer back into them leaving orange sparkles (purely visual).
     */
    private static void spawnEnergyParticle(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel level) {
            Vec3 offset = new Vec3((Math.random() - 0.5D) * 3.0D, (Math.random() - 0.5D) * 3.0D, (Math.random() - 0.5D) * 3.0D);
            var energy = new com.integral.forgottenrelics.entity.SoulEnergyEntity(com.integral.forgottenrelics.registry.FREntities.SHINY_ENERGY.get(), level, true);
            energy.setOwner(entity);
            energy.setTarget(entity);
            energy.setPos(entity.getBoundingBox().getCenter().add(offset));
            energy.setDeltaMovement(offset.normalize().scale(-0.1));
            level.addFreshEntity(energy);
        }
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemShinyStone1"));
            lines.accept(lore("ItemShinyStone2"));
            lines.accept(lore("FREmpty"));
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
