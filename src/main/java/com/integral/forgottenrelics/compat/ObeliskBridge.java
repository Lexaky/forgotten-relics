package com.integral.forgottenrelics.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Thaumaturge's Eldritch Obelisk (TC4 {@code TileEldritchObelisk}). Recognised by its block entity type id, so no
 * internal Thaumaturge class is needed.
 */
public final class ObeliskBridge {
    public static final Identifier OBELISK = Identifier.fromNamespaceAndPath("thaumaturge", "eldritch_obelisk");

    private ObeliskBridge() {
    }

    public static boolean isObelisk(BlockEntity be) {
        return be != null && OBELISK.equals(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
    }

    public static boolean isObelisk(Level level, BlockPos pos) {
        return isObelisk(level.getBlockEntity(pos));
    }

    /**
     * Original {@code onItemRightClick} search: an obelisk in the player's dimension within 16 blocks (distance to
     * its block corner). The original took the first one in the loaded block entity list; the nearest is used here.
     */
    public static BlockPos findNear(Player player, double range) {
        Level level = player.level();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = (int) Math.ceil(range / 16.0) + 1;
        ChunkPos center = player.chunkPosition();
        for (int cx = center.x() - r; cx <= center.x() + r; cx++) {
            for (int cz = center.z() - r; cz <= center.z() + r; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!isObelisk(be)) {
                        continue;
                    }
                    BlockPos pos = be.getBlockPos();
                    double d = player.position().distanceTo(net.minecraft.world.phys.Vec3.atLowerCornerOf(pos));
                    if (d <= range && d < bestDist) {
                        best = pos;
                        bestDist = d;
                    }
                }
            }
        }
        return best;
    }
}
