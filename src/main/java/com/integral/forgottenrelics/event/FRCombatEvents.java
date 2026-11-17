package com.integral.forgottenrelics.event;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.compat.CuriosBridge;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.bauble.OblivionAmuletItem;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRItems;
import com.integral.forgottenrelics.util.SuperpositionHandler;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import com.integral.forgottenrelics.item.passive.FateTomeItem;
import com.integral.forgottenrelics.registry.FRDamageTypes;

/**
 * Damage handlers of the original {@code RelicsEventHandler}.
 *
 * <p>1.7 had two events: {@code LivingAttackEvent} (before invincibility frames) and {@code LivingHurtEvent} (after
 * them, before armour). NeoForge 26.1 has one, {@link LivingIncomingDamageEvent}, fired before the frame check. The
 * "attack" handlers run at HIGHEST priority; the "hurt" handlers run at NORMAL and skip hits the frames will block,
 * which is when 1.7 never fired LivingHurtEvent. Order inside each phase follows the original.
 */
@EventBusSubscriber(modid = ForgottenRelics.MODID)
public final class FRCombatEvents {
    private FRCombatEvents() {
    }

    // ---------------------------------------------------------------------------------------------------- attack

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingIncomingDamageEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        LivingEntity victim = event.getEntity();
        // Chaos Core of the attacker: 45 % to redirect x0..2 of the hit to a random entity within 16 blocks
        // (15 % of those back to the attacker)
        if (!event.isCanceled() && source.getEntity() instanceof Player attacker
                && SuperpositionHandler.hasItem(attacker, FRItems.CHAOS_CORE.get()) && Math.random() < 0.45) {
            List<Entity> around = around(victim, 16);
            if (!around.isEmpty()) {
                Entity random = pickChaotic(around);
                float redirected = event.getAmount() * (float) (Math.random() * 2);
                if (Math.random() < 0.15) {
                    attacker.hurtServer(level, source, redirected);
                } else {
                    random.hurtServer(level, source, redirected);
                }
                event.setCanceled(true);
            }
        }
        if (!(victim instanceof Player player)) {
            return;
        }
        // Chaos Core of the victim: 42 % to redirect x0..2 of the hit to a random entity within 16 blocks
        if (!event.isCanceled() && SuperpositionHandler.hasItem(player, FRItems.CHAOS_CORE.get()) && Math.random() < 0.42D) {
            List<Entity> around = around(player, 16);
            if (!around.isEmpty()) {
                pickChaotic(around).hurtServer(level, source, event.getAmount() * (float) (Math.random() * 2));
                event.setCanceled(true);
            }
        }
        // Nebulous Core: 40 % (config) to dodge by teleporting up to 16 blocks away
        if (!event.isCanceled() && CuriosBridge.hasBauble(player, FRItems.NEBULOUS_CORE.get())
                && Math.random() < FRConfig.nebulousCoreDodgeChance.getAsDouble() && !FRDamageSources.isAbsolute(source)) {
            for (int counter = 0; counter <= 32; counter++) {
                if (SuperpositionHandler.validTeleportRandomly(player, level, 16)) {
                    player.invulnerableTime = 20;
                    event.setCanceled(true);
                    break;
                }
            }
        }
        // Ring of the Seven Suns: lava / in-fire / on-fire damage heals instead
        boolean ring = CuriosBridge.hasBauble(player, FRItems.DARK_SUN_RING.get());
        if (!event.isCanceled() && ring && isFireNegated(source)) {
            if (FRConfig.darkSunRingHealLimit.getAsBoolean()) {
                if (player.invulnerableTime == 0) {
                    player.heal(event.getAmount());
                    player.invulnerableTime = 20;
                }
            } else {
                player.heal(event.getAmount());
            }
            event.setCanceled(true);
        } else if (!event.isCanceled() && ring && source.getEntity() != null
                && Math.random() <= FRConfig.darkSunRingDeflectChance.getAsDouble()) {
            // ... or deflects the attack back to its source
            if (player.invulnerableTime == 0) {
                player.invulnerableTime = 20;
                source.getEntity().hurtServer(level, source, event.getAmount());
                event.setCanceled(true);
            }
        }
    }

    /** Original {@code darkRingDamageNegations}: lava, inFire, onFire. */
    static boolean isFireNegated(DamageSource source) {
        return source.is(DamageTypes.LAVA) || source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE);
    }

    // ------------------------------------------------------------------------------------------------------ hurt

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.isCanceled() || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        DamageSource source = event.getSource();
        if (entity.invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            return; // 1.7 fired LivingHurtEvent only for hits that got past the invincibility frames
        }
        // False Justice: damage taken by its owner, or (when the victim is not a player) dealt by it, becomes doubled
        // true damage
        if (entity instanceof Player victim) {
            if (SuperpositionHandler.hasItem(victim, FRItems.FALSE_JUSTICE.get()) && !FRDamageSources.isAbsolute(source)) {
                event.setCanceled(true);
                DamageSource truth = source.getEntity() == null ? FRDamageSources.trueDamageUndefined(level)
                        : FRDamageSources.trueDamage(level, source.getEntity());
                victim.hurtServer(level, truth, event.getAmount() * 2);
                return;
            }
        } else if (source.getEntity() instanceof Player attacker && SuperpositionHandler.hasItem(attacker, FRItems.FALSE_JUSTICE.get())
                && !FRDamageSources.isAbsolute(source)) {
            event.setCanceled(true);
            entity.hurtServer(level, FRDamageSources.trueDamage(level, attacker), event.getAmount() * 2);
            return;
        }
        if (!(entity instanceof Player player)) {
            return;
        }
        // Chaos Core: incoming damage x random 0..2
        if (SuperpositionHandler.hasItem(player, FRItems.CHAOS_CORE.get())) {
            event.setAmount(event.getAmount() * (float) (Math.random() * 2));
        }
        boolean ring = CuriosBridge.hasBauble(player, FRItems.DARK_SUN_RING.get());
        // Ring of the Seven Suns: attacks above the cap are negated
        if (event.getAmount() > FRConfig.darkSunRingDamageCap.getAsDouble() && ring) {
            event.setCanceled(true);
            return;
        }
        // ... and regular attacks may become up to twice as strong
        if (ring && Math.random() <= 0.25 && !FRDamageSources.isAbsolute(source)) {
            event.setAmount(event.getAmount() + event.getAmount() * (float) Math.random());
        }
        // Ancient Aegis of another player within 32 blocks takes 40 %
        if (!CuriosBridge.hasBauble(player, FRItems.ANCIENT_AEGIS.get())) {
            redirectToAegis(event, CuriosBridge.findPlayerWithBauble(level, 32, FRItems.ANCIENT_AEGIS.get(), player));
        }
        // Ancient Aegis damage reduction
        if (CuriosBridge.hasBauble(player, FRItems.ANCIENT_AEGIS.get()) && !FRDamageSources.isAbsolute(source)) {
            event.setAmount(event.getAmount() * (1.0F - (float) FRConfig.ancientAegisDamageReduction.getAsDouble()));
        }
        // Ring of Superposition: 12..74 % split among every other wearer, wherever they are
        if (CuriosBridge.hasBauble(player, FRItems.SUPERPOSITION_RING.get())) {
            List<Player> others = CuriosBridge.owners(level.getServer(), FRItems.SUPERPOSITION_RING.get());
            others.remove(player);
            splitSuperposition(event, others);
        }
        // Amulet of the Oblivion: absorbs the hit for 8 centivis of ignis and perditio per point of damage
        if (CuriosBridge.hasBauble(player, FRItems.OBLIVION_AMULET.get()) && !FRDamageSources.isAbsolute(source)) {
            int cost = (int) (event.getAmount() * 8 * FRConfig.oblivionAmuletVisMult.getAsDouble());
            if (VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.IGNIS, cost, TTAspects.PERDITIO, cost))) {
                ItemStack amulet = CuriosBridge.findEquipped(player, FRItems.OBLIVION_AMULET.get());
                OblivionAmuletItem.store(amulet, event.getAmount());
                event.setCanceled(true);
            }
        }
    }

    /** Ancient Aegis redirection: the owner (if any) takes 40 % of the hit, the victim keeps 60 %. */
    public static void redirectToAegis(LivingIncomingDamageEvent event, Player aegisOwner) {
        if (aegisOwner != null) {
            aegisOwner.hurtServer((ServerLevel) aegisOwner.level(), event.getSource(), event.getAmount() * 0.4F);
            event.setAmount(event.getAmount() * 0.6F);
        }
    }

    /** Superposition: 12..74 % of the hit is split evenly among the other wearers. */
    public static void splitSuperposition(LivingIncomingDamageEvent event, List<Player> others) {
        if (others.isEmpty()) {
            return;
        }
        double percent = 0.12 + Math.random() * 0.62;
        float split = (float) (event.getAmount() * percent);
        for (int i = others.size() - 1; i >= 0; i--) {
            Player other = others.get(i);
            other.hurtServer((ServerLevel) other.level(), event.getSource(), split / others.size());
        }
        event.setAmount(event.getAmount() - split);
    }

    /** Original {@code getEntitiesWithinAABBExcludingEntity} with a cube of the given radius around the entity. */
    static List<Entity> around(Entity center, double radius) {
        return center.level().getEntities(center, new net.minecraft.world.phys.AABB(center.getX() - radius, center.getY() - radius,
                center.getZ() - radius, center.getX() + radius, center.getY() + radius, center.getZ() + radius));
    }

    /** Original pick {@code (int) (random * (n - 1))}: the last entity of several is never chosen. */
    static Entity pickChaotic(List<Entity> entities) {
        return entities.get((int) (Math.random() * (entities.size() - 1)));
    }

    // ----------------------------------------------------------------------------------------------------- death

    /** Original {@code onPlayerDeath} (HIGHEST): Omega Core, then Tome of Broken Fates. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (SuperpositionHandler.hasItem(player, FRItems.OMEGA_CORE.get())) {
            event.setCanceled(true);
            player.setHealth(player.getMaxHealth());
        } else if (!player.level().isClientSide() && FateTomeItem.tryRescue(player)) {
            event.setCanceled(true);
        }
    }

    /**
     * Original {@code onDeath}: with False Justice involved, only true damage may kill. The death is cancelled
     * without healing, exactly as in the original.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        DamageSource source = event.getSource();
        boolean truth = source.is(FRDamageTypes.TRUE_DAMAGE) || source.is(FRDamageTypes.TRUE_DAMAGE_UNDEFINED);
        if (truth) {
            return;
        }
        if (event.getEntity() instanceof Player victim) {
            if (SuperpositionHandler.hasItem(victim, FRItems.FALSE_JUSTICE.get())) {
                event.setCanceled(true);
            }
        } else if (source.getEntity() instanceof Player attacker && SuperpositionHandler.hasItem(attacker, FRItems.FALSE_JUSTICE.get())) {
            event.setCanceled(true);
        }
    }
}
