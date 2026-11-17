package com.integral.forgottenrelics.item.utility;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.ObeliskBridge;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.util.FRFx;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Devourer of the Void (ItemObeliskDrainer): Right-click near an Eldritch Obelisk (16 blocks) and hold. Every 30
 * ticks: heals 4, restores 2 food (saturation modifier 1), adds 5..20 vis (x obeliskDrainerVisMult) of a random primal
 * to a random non-full wand, with a 0.01 "zap" hit; the drain stops when the obelisk is gone or more than 16 blocks
 * from its centre (y + 2.5).
 */
public class ObeliskDrainerItem extends RelicItem {
    public static final int USE_TICKS = 72000;

    public ObeliskDrainerItem(Properties properties) {
        super(properties, 4);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockPos obelisk = ObeliskBridge.findNear(player, 16.0D);
        if (obelisk == null) {
            return InteractionResult.PASS;
        }
        player.getItemInHand(hand).set(FRDataComponents.OBELISK_POS.get(), obelisk);
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    static Vec3 drainPoint(BlockPos obelisk) {
        return new Vec3(obelisk.getX(), obelisk.getY() + 2.5, obelisk.getZ());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        BlockPos obelisk = stack.get(FRDataComponents.OBELISK_POS.get());
        if (obelisk == null || !ObeliskBridge.isObelisk(level, obelisk) || player.position().distanceTo(drainPoint(obelisk)) > 16) {
            player.stopUsingItem();
            return;
        }
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (remaining % 30 == 0 && remaining != USE_TICKS) {
            drainPulse(server, player, obelisk);
        }
        if (Math.random() <= 0.175) {
            Vec3 a = new Vec3(obelisk.getX() + 0.5, obelisk.getY() + 2.5 + (Math.random() - 0.5D) * 2, obelisk.getZ() + 0.5);
            Vec3 b = a.add((Math.random() - 0.5D) * 4, (Math.random() - 0.5D) * 4, (Math.random() - 0.5D) * 4);
            FRFx.arc(server, a, b, 1F, 0.6F, 1F, Math.random() > 0.5 ? 0.4F : -0.4F);
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, obelisk.getX() + 0.5, obelisk.getY() + 2.5, obelisk.getZ() + 0.5,
                5, 1.5, 1.0, 1.5, 0.3);
    }

    /** One drain pulse (every 30 ticks of use). */
    public static void drainPulse(ServerLevel level, Player player, BlockPos obelisk) {
        Vec3 top = new Vec3(obelisk.getX() + 0.5, obelisk.getY() + 2.75, obelisk.getZ() + 0.5);
        for (int i = 0; i <= 3; i++) {
            FRFx.lightning(level, top, player.getBoundingBox().getCenter(), 20, 0.075F);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.zap(), SoundSource.PLAYERS, 1F, 0.8F);
        player.hurtServer(level, level.damageSources().generic(), 0.01F);
        player.heal(4);
        player.getFoodData().eat(2, 1.0F);
        ResourceKey<IAspect> aspect = VisHelper.PRIMALS.get((int) (Math.random() * VisHelper.PRIMALS.size()));
        ItemStack wand = VisHelper.randomWandWithRoom(VisHelper.wands(player), aspect, player.getRandom());
        if (!wand.isEmpty()) {
            VisHelper.addVis(wand, aspect, (int) ((5 + Math.random() * 15) * FRConfig.obeliskDrainerVisMult.getAsDouble()));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemObeliskDrainer1", "ItemObeliskDrainer2", "FREmpty", "ItemObeliskDrainer3",
                    "ItemObeliskDrainer4", "ItemObeliskDrainer5"}) {
                lines.accept(lore(key));
            }
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        lines.accept(lore("FREmpty"));
    }
}
