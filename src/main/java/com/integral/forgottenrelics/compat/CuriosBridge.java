package com.integral.forgottenrelics.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/**
 * Curios (Baubles in the original). Curios is a hard dependency of Thaumaturge, so it is always present. Slot
 * membership comes from the {@code curios:ring/necklace/belt} item tags; relic behaviour from
 * {@link com.integral.forgottenrelics.item.RelicCurioItem}. Mirrors the bauble helpers of SuperpositionHandler.
 */
public final class CuriosBridge {
    private CuriosBridge() {
    }

    /**
     * Every equipped stack of the item, read directly from the slot handlers. Curios' own {@code isEquipped} /
     * {@code findFirstCurio} answer from a cache refreshed on the inventory tick: a query made before equipping kept
     * returning "not equipped" afterwards in the GameTest {@code systems_curios_bauble}.
     */
    public static List<ItemStack> equipped(LivingEntity entity, Item item) {
        List<ItemStack> found = new ArrayList<>();
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> inv.getCurios().values().forEach(handler -> {
            var stacks = handler.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack.is(item)) {
                    found.add(stack);
                }
            }
        }));
        return found;
    }

    /** Original {@code hasBauble}: the item is equipped in any curio slot. */
    public static boolean hasBauble(LivingEntity entity, Item item) {
        return !equipped(entity, item).isEmpty();
    }

    /** First equipped stack of the item, or empty. */
    public static ItemStack findEquipped(LivingEntity entity, Item item) {
        List<ItemStack> found = equipped(entity, item);
        return found.isEmpty() ? ItemStack.EMPTY : found.getFirst();
    }

    /** Number of equipped stacks of the item (Dark Sun Ring: a second ring has no extra effect). */
    public static int countEquipped(LivingEntity entity, Item item) {
        return equipped(entity, item).size();
    }

    /** Original {@code getBaubleOwnersList}: every online player wearing the item, in any dimension. */
    public static List<Player> owners(MinecraftServer server, Item item) {
        List<Player> owners = new ArrayList<>();
        var players = server.getPlayerList().getPlayers();
        for (int i = players.size() - 1; i >= 0; i--) {
            if (hasBauble(players.get(i), item)) {
                owners.add(players.get(i));
            }
        }
        return owners;
    }

    /**
     * Original {@code findPlayerWithBauble}: a random player (other than {@code around}) within a cube of the given
     * radius wearing the item, or null. Keeps the original pick {@code (int) ((n - 1) * random)}, which never selects
     * the last of several candidates.
     */
    public static Player findPlayerWithBauble(Level level, int radius, Item item, LivingEntity around) {
        if (level.isClientSide()) {
            return null;
        }
        AABB box = new AABB(around.getX() - radius, around.getY() - radius, around.getZ() - radius,
                around.getX() + radius, around.getY() + radius, around.getZ() + radius);
        List<Player> found = new ArrayList<>();
        List<Player> players = level.getEntitiesOfClass(Player.class, box, p -> p != around);
        for (int i = players.size() - 1; i >= 0; i--) {
            if (hasBauble(players.get(i), item)) {
                found.add(players.get(i));
            }
        }
        if (found.isEmpty()) {
            return null;
        }
        return found.get((int) ((found.size() - 1) * Math.random()));
    }

    /** Empties every curio slot (test support). */
    public static void clear(LivingEntity entity) {
        CuriosApi.getCuriosInventory(entity).ifPresent(inv -> inv.getCurios().values().forEach(h -> {
            var stacks = h.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                stacks.setStackInSlot(i, ItemStack.EMPTY);
            }
        }));
    }

    /** Whether the first slot of the given curio type accepts the stack: the check the Curios screen makes on a click. */
    public static boolean accepts(LivingEntity entity, String slot, ItemStack stack) {
        return CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.getStacksHandler(slot))
                .map(h -> h.getStacks().getSlots() > 0 && h.getStacks().isItemValid(0, stack)).orElse(false);
    }

    /** Puts the stack into the first empty slot of the given curio type; false when there is none. */
    public static boolean equip(LivingEntity entity, String slot, ItemStack stack) {
        Optional<ICurioStacksHandler> handler = CuriosApi.getCuriosInventory(entity).flatMap(inv -> inv.getStacksHandler(slot));
        if (handler.isEmpty()) {
            return false;
        }
        var stacks = handler.get().getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (stacks.getStackInSlot(i).isEmpty()) {
                stacks.setStackInSlot(i, stack);
                return true;
            }
        }
        return false;
    }
}
