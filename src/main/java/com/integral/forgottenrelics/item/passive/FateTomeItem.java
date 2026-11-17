package com.integral.forgottenrelics.item.passive;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTEffects;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tome of Broken Fates (ItemFateTome): when its owner would die it pays 100 vis of every primal
 * (x fateTomeVisMult), restores full health and applies buffs (75 %) or debuffs (25 %), then cools down for
 * 30..90 s. Carrying two or more tomes risks "insanely disastrous consequences" (0.0016 % per tick per tome).
 */
public class FateTomeItem extends RelicItem {
    public FateTomeItem(Properties properties) {
        super(properties, 7);
    }

    /** Original {@code AerCost..PerditioCost}: 10000 centivis each, x fateTomeVisMult. */
    public static int primalCost() {
        return (int) (10000 * FRConfig.fateTomeVisMult.getAsDouble());
    }

    static Map<ResourceKey<IAspect>, Integer> cost() {
        int c = primalCost();
        var p = VisHelper.PRIMALS;
        return VisHelper.cost(p.get(0), c, p.get(1), c, p.get(2), c, p.get(3), c, p.get(4), c, p.get(5), c);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!(owner instanceof Player player)) {
            return;
        }
        Integer cooldown = stack.get(FRDataComponents.FATE_COOLDOWN.get());
        if (cooldown == null) {
            stack.set(FRDataComponents.FATE_COOLDOWN.get(), 0);
        } else if (cooldown > 0) {
            stack.set(FRDataComponents.FATE_COOLDOWN.get(), cooldown - 1);
        }
        if (Math.random() <= 0.000016 && countTomes(player) > 1) {
            insanelyDisastrousConsequences(player);
        }
    }

    /** Number of Fate Tome stacks in the main inventory (original {@code itemSearch(...).size()}). */
    public static int countTomes(Player player) {
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.getItem() instanceof FateTomeItem) {
                n++;
            }
        }
        return n;
    }

    /**
     * Death prevention (original {@code onPlayerDeath}): only a tome that has been ticked once (has a cooldown value)
     * and is off cooldown saves, and only if the vis is paid.
     *
     * @return whether the death must be cancelled
     */
    public static boolean tryRescue(Player player) {
        ItemStack tome = ItemStack.EMPTY;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.getItem() instanceof FateTomeItem) {
                tome = s;
                break;
            }
        }
        if (tome.isEmpty()) {
            return false;
        }
        Integer cooldown = tome.get(FRDataComponents.FATE_COOLDOWN.get());
        if (cooldown == null || cooldown != 0 || !VisHelper.consumeFromInventory(player, cost())) {
            return false;
        }
        tome.set(FRDataComponents.FATE_COOLDOWN.get(), (int) (600 + Math.random() * 1200));
        player.setHealth(player.getMaxHealth());
        if (Math.random() <= 0.75) {
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 2));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 500, 1));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1000, 0));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 300, 1));
        }
        if (player.level() instanceof ServerLevel level) {
            TTEffects.burst(level, player.getX(), player.getY() + 1, player.getZ(), 1.5F);
            level.playSound(null, player.getX() + 0.5, player.getY() + 1.5, player.getZ() + 0.5, TTSounds.runicShieldCharge(), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return true;
    }

    /**
     * Original {@code SuperpositionHandler.insanelyDisastrousConsequences}: every tome is consumed, every living
     * entity within 64 blocks takes 40000 fate damage with a power-16 explosion, then a power-100 explosion at the
     * player. Explosions set fire and break blocks, as {@code newExplosion(..., true, true)} did.
     */
    public static void insanelyDisastrousConsequences(Player player) {
        var items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItem() instanceof FateTomeItem) {
                items.set(i, ItemStack.EMPTY);
            }
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        var entities = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(64));
        for (int i = entities.size(); i > 0; i--) {
            LivingEntity e = entities.get(i - 1);
            e.hurtServer(level, FRDamageSources.fate(level), 40000.0F);
            level.explode(player, e.getX(), e.getY(), e.getZ(), 16F, true, Level.ExplosionInteraction.TNT);
        }
        level.explode(player, player.getX(), player.getY(), player.getZ(), 100F, true, Level.ExplosionInteraction.TNT);
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            for (String key : new String[]{"ItemFateTome1", "ItemFateTome2", "ItemFateTome3", "ItemFateTome4", "FREmpty", "ItemFateTome5",
                    "FREmpty", "ItemFateTome6", "ItemFateTome7", "FREmpty", "ItemFateTome8", "ItemFateTome9"}) {
                lines.accept(lore(key));
            }
        } else if (control()) {
            double vis = primalCost() / 100.0D;
            lines.accept(lore("FRVisPerCast"));
            for (String aspect : new String[]{"FRAerCost", "FRTerraCost", "FRIgnisCost", "FRAquaCost", "FROrdoCost", "FRPerditioCost"}) {
                lines.accept(" " + lore(aspect) + vis);
            }
        } else {
            lines.accept(lore("FRShiftTooltip"));
            lines.accept(lore("FRViscostTooltip"));
        }
        Integer cooldown = stack.get(FRDataComponents.FATE_COOLDOWN.get());
        if (cooldown != null && cooldown > 0) {
            lines.accept(lore("FREmpty"));
            double seconds = new BigDecimal(cooldown / 20.0D).setScale(1, RoundingMode.HALF_UP).doubleValue();
            lines.accept(lore("FRCode" + (int) (Math.random() * 15 + 1)) + lore("ItemFateTomeCooldown") + " " + seconds + " " + lore("FRSeconds"));
        }
        lines.accept(lore("FREmpty"));
    }
}
