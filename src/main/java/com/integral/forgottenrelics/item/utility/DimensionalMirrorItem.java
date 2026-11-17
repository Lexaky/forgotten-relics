package com.integral.forgottenrelics.item.utility;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.util.FRFx;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
 * Dimensional Mirror (ItemDimensionalMirror): Shift+Right-click stores the position and dimension; holding
 * Right-click for 80 ticks returns there (across dimensions unless {@code interdimensionalMirror} is off).
 */
public class DimensionalMirrorItem extends RelicItem {
    public static final int USE_TICKS = 80;

    public DimensionalMirrorItem(Properties properties) {
        super(properties, 0);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        GlobalPos target = stack.get(FRDataComponents.MIRROR_TARGET.get());
        if (target != null && !player.isShiftKeyDown()) {
            if (!FRConfig.interdimensionalMirror.getAsBoolean() && !player.level().dimension().equals(target.dimension())) {
                return InteractionResult.PASS;
            }
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        } else if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                store(stack, player);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.jar(), SoundSource.PLAYERS, 1.0F, 2.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Original: (int) truncation of the position, as stored in NBT. */
    public static void store(ItemStack stack, Player player) {
        BlockPos pos = new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ());
        stack.set(FRDataComponents.MIRROR_TARGET.get(), GlobalPos.of(player.level().dimension(), pos));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) {
            return;
        }
        Vec3 center = player.getBoundingBox().getCenter();
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL, center.x, center.y, center.z, 4, 0.5, 0.5, 0.5, 1.5);
        if (remaining == 1) {
            teleport(player, stack);
        }
    }

    /** The final tick of the gaze: burst, change dimension if needed, land on the block centre + 0.5. */
    public static boolean teleport(Player player, ItemStack stack) {
        GlobalPos target = stack.get(FRDataComponents.MIRROR_TARGET.get());
        if (target == null || !(player.level() instanceof ServerLevel from)) {
            return false;
        }
        ServerLevel to = from.getServer().getLevel(target.dimension());
        if (to == null) {
            return false;
        }
        Vec3 center = player.getBoundingBox().getCenter();
        TTEffects.burst(from, center.x, center.y, center.z, 1.25F);
        BlockPos p = target.pos();
        if (to == from) {
            player.teleportTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);
        } else if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.teleportTo(to, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot(), false);
        } else {
            return false;
        }
        from.playSound(null, center.x, center.y, center.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, (float) (0.8F + Math.random() * 0.2));
        to.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, (float) (0.8F + Math.random() * 0.2));
        FRFx.portalBurst(to, player.position().add(0, -1, 0), 129);
        return true;
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
    public boolean isFoil(ItemStack stack) {
        return stack.has(FRDataComponents.MIRROR_TARGET.get());
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemDimensionalMirror1"));
            lines.accept(lore("ItemDimensionalMirror2"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemDimensionalMirror3"));
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
        GlobalPos target = stack.get(FRDataComponents.MIRROR_TARGET.get());
        if (target != null) {
            lines.accept(lore("FREmpty"));
            lines.accept(lore("MirrorLoc"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("MirrorX") + target.pos().getX());
            lines.accept(lore("MirrorY") + target.pos().getY());
            lines.accept(lore("MirrorZ") + target.pos().getZ());
            lines.accept(lore("FREmpty"));
            // the original printed the numeric dimension id; dimensions are identifiers now
            lines.accept(lore("MirrorDimension") + target.dimension().identifier());
        }
    }
}
