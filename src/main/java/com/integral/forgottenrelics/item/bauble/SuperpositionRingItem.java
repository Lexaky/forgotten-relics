package com.integral.forgottenrelics.item.bauble;

import com.integral.forgottenrelics.compat.CuriosBridge;
import com.integral.forgottenrelics.item.RelicCurioItem;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.theillusivec4.curios.api.SlotContext;

/**
 * Ring of Superposition (ItemSuperpositionRing). Damage splitting lives in
 * {@link com.integral.forgottenrelics.event.FRCombatEvents}; here every 600 ticks there is a 95 % chance to swap places
 * with a random other wearer, across dimensions too.
 */
public class SuperpositionRingItem extends RelicCurioItem {
    public SuperpositionRingItem(Properties properties) {
        super(properties, 0, "ring");
    }

    @Override
    protected void wornTick(ItemStack stack, LivingEntity entity, SlotContext slot) {
        if (entity.tickCount % 600 != 0 || !(entity instanceof ServerPlayer self) || Math.random() < 0.05) {
            return;
        }
        List<Player> players = CuriosBridge.owners(self.level().getServer(), this);
        players.remove(self);
        if (players.isEmpty()) {
            return;
        }
        ServerPlayer other = (ServerPlayer) players.get((int) (Math.random() * players.size()));
        swap(self, other);
    }

    /** Original: both players go to the other's bounding-box centre (Vector3.fromEntityCenter), dimension included. */
    public static void swap(ServerPlayer a, ServerPlayer b) {
        ServerLevel levelA = a.level();
        ServerLevel levelB = b.level();
        Vec3 posA = a.getBoundingBox().getCenter();
        Vec3 posB = b.getBoundingBox().getCenter();
        a.teleportTo(levelB, posB.x, posB.y, posB.z, Set.of(), a.getYRot(), a.getXRot(), false);
        b.teleportTo(levelA, posA.x, posA.y, posA.z, Set.of(), b.getYRot(), b.getXRot(), false);
        levelB.playSound(null, posB.x, posB.y, posB.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, (float) (0.8F + Math.random() * 0.2));
        levelA.playSound(null, posA.x, posA.y, posA.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, (float) (0.8F + Math.random() * 0.2));
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemSuperpositionRing1", "ItemSuperpositionRing2", "ItemSuperpositionRing3", "FREmpty",
                    "ItemSuperpositionRing4", "ItemSuperpositionRing5", "FREmpty"}) {
                lines.accept(lore(key));
            }
            lines.accept(baubleLine());
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
