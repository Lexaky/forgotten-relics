package com.integral.forgottenrelics.compat;

import com.integral.forgottenrelics.registry.FRTags;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import com.leclowndu93150.thaumaturge.content.wands.WandVisHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Vis of wands in a player's inventory: the original's {@code WandManager.consumeVisFromInventory},
 * {@code ItemWandCasting.addVis} and {@code SuperpositionHandler.wandSearch/getRandomValidWand}.
 *
 * <p>Amounts are centivis (100 per vis), as in TC4's AspectLists, which Thaumaturge also uses internally. Paying goes
 * through Thaumaturge's {@code WandVisHelper.payCosts}, the direct counterpart of TC4's {@code consumeAllVis}: it
 * applies the wand's cap/rod modifiers and the player's vis-discount gear. That class lives in Thaumaturge's
 * {@code content} package (there is no public equivalent), so it is used only here.
 */
public final class VisHelper {
    /** TC4 {@code Aspect.getPrimalAspects()} order: aer, terra, ignis, aqua, ordo, perditio. */
    public static final List<ResourceKey<IAspect>> PRIMALS = List.of(TTAspects.AER, TTAspects.TERRA, TTAspects.IGNIS,
            TTAspects.AQUA, TTAspects.ORDO, TTAspects.PERDITIO);

    private VisHelper() {
    }

    /** Builder for a cost map in centivis, in insertion order. */
    public static Map<ResourceKey<IAspect>, Integer> cost(Object... aspectAndCentivis) {
        Map<ResourceKey<IAspect>, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < aspectAndCentivis.length; i += 2) {
            @SuppressWarnings("unchecked")
            ResourceKey<IAspect> aspect = (ResourceKey<IAspect>) aspectAndCentivis[i];
            int amount = ((Number) aspectAndCentivis[i + 1]).intValue();
            if (amount > 0) {
                map.merge(aspect, amount, Integer::sum);
            }
        }
        return map;
    }

    /** Every wand in the main inventory (hotbar + backpack), first slot first. */
    public static List<ItemStack> wands(Player player) {
        List<ItemStack> wands = new ArrayList<>();
        var items = player.getInventory().getNonEquipmentItems();
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && stack.is(FRTags.WANDS)) {
                wands.add(stack);
            }
        }
        return wands;
    }

    /**
     * TC4 {@code WandManager.consumeVisFromInventory}: walks the main inventory from the last slot to the first and
     * lets the first wand that can afford the whole cost pay it (with the crafting discount, as TC4 did).
     *
     * @return whether some wand paid
     */
    public static boolean consumeFromInventory(Player player, Map<ResourceKey<IAspect>, Integer> centivis) {
        if (centivis.isEmpty()) {
            return true;
        }
        var items = player.getInventory().getNonEquipmentItems();
        for (int slot = items.size() - 1; slot >= 0; slot--) {
            ItemStack stack = items.get(slot);
            if (!stack.isEmpty() && stack.is(FRTags.WANDS) && WandVisHelper.payCosts(stack, player, centivis, true, true)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The original's {@code sidedVisConsumption}: on the client it always succeeds (the server decides), on the
     * server it pays from the inventory.
     */
    public static boolean consumeSided(Player player, Map<ResourceKey<IAspect>, Integer> centivis) {
        return player.level().isClientSide() || consumeFromInventory(player, centivis);
    }

    /** Stored centivis of one aspect. */
    public static int stored(ItemStack wand, ResourceKey<IAspect> aspect) {
        return WandVisHelper.storedIn(wand, aspect);
    }

    /** Capacity per aspect in centivis. */
    public static int capacity(ItemStack wand) {
        return WandVisHelper.capacityOf(wand);
    }

    /** TC4 {@code ItemWandCasting.addVis(stack, aspect, vis, true)}: adds whole vis, clamped to the capacity. */
    public static void addVis(ItemStack wand, ResourceKey<IAspect> aspect, int vis) {
        WandVisHelper.topUp(wand, aspect, vis, true);
    }

    /** Original {@code getRandomValidWand}: a random wand that is not full of the given aspect, or empty. */
    public static ItemStack randomWandWithRoom(List<ItemStack> wands, ResourceKey<IAspect> aspect, RandomSource random) {
        List<ItemStack> valid = new ArrayList<>();
        for (ItemStack wand : wands) {
            if (stored(wand, aspect) < capacity(wand)) {
                valid.add(wand);
            }
        }
        return valid.isEmpty() ? ItemStack.EMPTY : valid.get(random.nextInt(valid.size()));
    }
}
