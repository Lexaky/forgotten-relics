package com.integral.forgottenrelics.item.utility;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.util.FRFx;
import com.integral.forgottenrelics.util.SuperpositionHandler;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Tome of Discord (ItemTeleportationTome), 20-tick cooldown, 1.6 aer + 2.4 ordo + 2.4 perditio vis per cast
 * (x discordTomeVisMult): Shift teleports 16 blocks along the look ignoring obstacles; a living entity looked at
 * within 128 blocks swaps places with the player; otherwise the player lands on the looked-at block (first floor
 * with two air blocks above, up to 32 blocks higher).
 */
public class TeleportationTomeItem extends RelicItem {
    public TeleportationTomeItem(Properties properties) {
        super(properties, 2);
    }

    static int aer() {
        return (int) (160 * FRConfig.discordTomeVisMult.getAsDouble());
    }

    static int ordoPerditio() {
        return (int) (240 * FRConfig.discordTomeVisMult.getAsDouble());
    }

    static Map<ResourceKey<IAspect>, Integer> cost() {
        return VisHelper.cost(TTAspects.AER, aer(), TTAspects.ORDO, ordoPerditio(), TTAspects.PERDITIO, ordoPerditio());
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        int cooldown = stack.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0);
        if (cooldown > 0) {
            stack.set(FRDataComponents.RELIC_COOLDOWN.get(), cooldown - 1);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0) != 0) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (cast(server, player)) {
            stack.set(FRDataComponents.RELIC_COOLDOWN.get(), 20);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** One cast; the vis is paid once (see PORTING_PLAN.md: the original's {@code &} charged up to three times). */
    public static boolean cast(ServerLevel level, Player player) {
        Vec3 start = player.getBoundingBox().getCenter();
        if (player.isShiftKeyDown()) {
            if (!VisHelper.consumeFromInventory(player, cost())) {
                return false;
            }
            TTEffects.burst(level, player.getX(), player.getY() + 1, player.getZ(), 1.25F);
            Vec3 from = start.add(0, -0.5, 0);
            Vec3 to = from.add(player.getLookAngle().scale(16.0));
            sound(level, player.position());
            player.teleportTo(to.x, to.y, to.z);
            sound(level, to);
            FRFx.portalTrace(level, from, player.getBoundingBox().getCenter().add(0, -0.5, 0));
            return true;
        }
        LivingEntity pointed = pointedLiving(level, player, 128.0);
        if (pointed != null) {
            if (!VisHelper.consumeFromInventory(player, cost())) {
                return false;
            }
            TTEffects.burst(level, player.getX(), player.getY() + 1, player.getZ(), 1.25F);
            Vec3 other = pointed.getBoundingBox().getCenter();
            player.teleportTo(other.x, other.y, other.z);
            pointed.teleportTo(start.x, start.y, start.z);
            sound(level, player.position());
            sound(level, pointed.position());
            FRFx.portalTrace(level, start, other);
            return true;
        }
        BlockHitResult hit = SuperpositionHandler.getPointedBlock(player, level, 128.0F);
        if (hit == null) {
            return false;
        }
        BlockPos block = hit.getBlockPos();
        for (int counter = 0; counter <= 32; counter++) {
            BlockPos floor = block.above(counter - 1);
            BlockState below = level.getBlockState(floor);
            if (!below.isAir() && !below.getCollisionShape(level, floor).isEmpty()
                    && level.getBlockState(floor.above()).isAir() && level.getBlockState(floor.above(2)).isAir()) {
                if (!VisHelper.consumeFromInventory(player, cost())) {
                    return false;
                }
                TTEffects.burst(level, player.getX(), player.getY() + 1, player.getZ(), 1.25F);
                sound(level, player.position());
                player.teleportTo(block.getX() + 0.5, block.getY() + counter, block.getZ() + 0.5);
                sound(level, player.position());
                FRFx.portalTrace(level, start, player.getBoundingBox().getCenter());
                return true;
            }
        }
        return false;
    }

    /** TC4 {@code EntityUtils.getPointedEntity(world, player, 0, range, 4)}: first living entity on the look line. */
    static LivingEntity pointedLiving(Level level, Player player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB box = player.getBoundingBox().expandTowards(player.getLookAngle().scale(range)).inflate(4.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, box,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), range * range);
        return hit == null ? null : (LivingEntity) hit.getEntity();
    }

    private static void sound(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemTeleportationTome1", "ItemTeleportationTome2", "ItemTeleportationTome3", "FREmpty",
                    "ItemTeleportationTome4", "ItemTeleportationTome5", "ItemTeleportationTome6"}) {
                lines.accept(lore(key));
            }
        } else if (control()) {
            lines.accept(lore("FRVisPerCast"));
            lines.accept(" " + lore("FRAerCost") + aer() / 100.0D);
            lines.accept(" " + lore("FROrdoCost") + ordoPerditio() / 100.0D);
            lines.accept(" " + lore("FRPerditioCost") + ordoPerditio() / 100.0D);
        } else {
            lines.accept(lore("FRShiftTooltip"));
            lines.accept(lore("FRViscostTooltip"));
        }
        lines.accept(lore("FREmpty"));
    }
}
