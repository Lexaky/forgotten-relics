package com.integral.forgottenrelics.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Original {@code ItemBaubleBase} / {@code ItemBaubleBaseModifier} (copies of Botania's bauble classes) on Curios.
 * Curios attaches the curio capability to every {@link ICurioItem} itself; slot membership is the
 * {@code curios:<slot>} item tag. {@code onEquippedOrLoadedIntoWorld} + {@code applyAttributeModifiers} become
 * {@link #attributeModifiers}, which Curios applies and removes. Botania's cosmetic overrides and phantom ink are
 * not ported (they belong to Botania's own bauble class).
 */
public abstract class RelicCurioItem extends RelicItem implements ICurioItem {
    private final String slot;

    protected RelicCurioItem(Properties properties, int gearWarp, String slot) {
        super(properties, gearWarp);
        this.slot = slot;
    }

    /** Curio slot type: ring, necklace or belt (original BaubleType RING/AMULET/BELT). */
    public String slot() {
        return slot;
    }

    /** Original {@code onWornTick}, both sides. The slot context locates the stack (for in-place replacement). */
    protected void wornTick(ItemStack stack, LivingEntity wearer, SlotContext slot) {
    }

    /** Original {@code SuperpositionHandler.getBaubleTooltip}: "Can be equipped as Ring/Amulet/Belt." */
    protected String baubleLine() {
        return switch (slot) {
            case "ring" -> lore("FRRing");
            case "necklace" -> lore("FRAmulet");
            case "belt" -> lore("FRBelt");
            default -> "";
        };
    }

    /** Replaces the stack in the curio slot it is worn in (Nebulous Core falling dormant and waking up). */
    protected static void replaceInSlot(SlotContext slot, ItemStack replacement) {
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(slot.entity())
                .flatMap(inv -> inv.getStacksHandler(slot.identifier()))
                .ifPresent(handler -> handler.getStacks().setStackInSlot(slot.index(), replacement));
    }

    /** Original {@code onEquipped} (after the equip sound). */
    protected void onEquipped(ItemStack stack, LivingEntity wearer) {
    }

    /** Original {@code onUnequipped}. */
    protected void onUnequipped(ItemStack stack, LivingEntity wearer) {
    }

    /** Original {@code fillModifiers}: attribute modifiers active while worn. */
    protected void attributeModifiers(ItemStack stack, Identifier slotId, Multimap<Holder<Attribute>, AttributeModifier> out) {
    }

    @Override
    public final void curioTick(SlotContext context, ItemStack stack) {
        wornTick(stack, context.entity(), context);
    }

    @Override
    public final void onEquip(SlotContext context, ItemStack previous, ItemStack stack) {
        if (!ItemStack.isSameItem(previous, stack)) {
            onEquipped(stack, context.entity());
        }
    }

    @Override
    public final void onUnequip(SlotContext context, ItemStack next, ItemStack stack) {
        if (!ItemStack.isSameItem(next, stack)) {
            onUnequipped(stack, context.entity());
        }
    }

    @Override
    public final Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext context, Identifier id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
        attributeModifiers(stack, id, map);
        return map;
    }

    /** Original: right-click equipping refused fake players ({@code EntityDoppleganger.isTruePlayer}). */
    @Override
    public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
        return !(context.entity() instanceof FakePlayer);
    }

    /** Original: Botania's "equipBauble" sound at volume 0.1, pitch 1.3. */
    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext context, ItemStack stack) {
        return new ICurio.SoundInfo(BotaniaBridge.equipBaubleSound(), 0.1F, 1.3F);
    }
}
