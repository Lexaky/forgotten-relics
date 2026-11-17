package com.integral.forgottenrelics.util;

import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * General helpers of the original {@code SuperpositionHandler}. Bauble helpers live in
 * {@link com.integral.forgottenrelics.compat.CuriosBridge}, wand helpers in
 * {@link com.integral.forgottenrelics.compat.VisHelper}, damage helpers in
 * {@link com.integral.forgottenrelics.registry.FRDamageSources}.
 */
public final class SuperpositionHandler {
    private SuperpositionHandler() {
    }

    /** Original {@code getPointedBlock}: first block along the look vector within range, or null. */
    public static BlockHitResult getPointedBlock(Player player, Level level, float range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? null : hit;
    }

    private static boolean solid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * Original {@code validTeleport}: if the block under the target is solid, search up to 32 blocks upwards for a
     * solid floor with two air blocks above it, otherwise search downwards; teleport to the centre of that column.
     * The original truncated coordinates with {@code (int)}; flooring is used so negative coordinates do not shift
     * by one block.
     */
    public static boolean validTeleport(Entity entity, double xInit, double yInit, double zInit, Level level) {
        int x = Mth.floor(xInit);
        int y = Mth.floor(yInit);
        int z = Mth.floor(zInit);
        int direction = solid(level, new BlockPos(x, y - 1, z)) ? 1 : -1;
        for (int counter = 0; counter <= 32; counter++) {
            int ty = y + counter * direction;
            if (solid(level, new BlockPos(x, ty - 1, z)) && level.getBlockState(new BlockPos(x, ty, z)).isAir()
                    && level.getBlockState(new BlockPos(x, ty + 1, z)).isAir()) {
                if (level instanceof ServerLevel server) {
                    TTEffects.burst(server, entity.getX(), entity.getY() + 1, entity.getZ(), 1.25F);
                }
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                entity.teleportTo(x + 0.5, ty, z + 0.5);
                level.playSound(null, x + 0.5, ty, z + 0.5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.0F);
                return true;
            }
        }
        return false;
    }

    /** Original {@code validTeleportRandomly}: random target in a cube of the given radius, then validTeleport. */
    public static boolean validTeleportRandomly(Entity entity, Level level, int radius) {
        int d = radius * 2;
        double x = entity.getX() + (Math.random() - 0.5D) * d;
        double y = entity.getY() + (Math.random() - 0.5D) * d;
        double z = entity.getZ() + (Math.random() - 0.5D) * d;
        return validTeleport(entity, x, y, z, level);
    }

    /** Original {@code findFirst}: first stack of the item in the main inventory, or empty. */
    public static ItemStack findFirst(Player player, Item item) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** {@code player.inventory.hasItem(item)} of 1.7: main inventory only. */
    public static boolean hasItem(Player player, Item item) {
        return !findFirst(player, item).isEmpty();
    }

    /** Original {@code convertStuff}: replaces the first stack of {@code from} with a single {@code to}. */
    public static void convert(Player player, Item from, Item to) {
        var items = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < items.size(); slot++) {
            if (items.get(slot).is(from)) {
                items.set(slot, new ItemStack(to));
                return;
            }
        }
    }
}
