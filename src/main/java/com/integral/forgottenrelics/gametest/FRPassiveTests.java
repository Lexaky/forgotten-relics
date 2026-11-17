package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;
import static com.integral.forgottenrelics.gametest.FRSystemsTests.wand;

import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.event.FRCombatEvents;
import com.integral.forgottenrelics.item.passive.ChaosCoreItem;
import com.integral.forgottenrelics.item.passive.FateTomeItem;
import com.integral.forgottenrelics.item.passive.XpTomeItem;
import com.integral.forgottenrelics.registry.FRConditions;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRItems;
import com.integral.forgottenrelics.util.XpHelper;
import com.leclowndu93150.thaumaturge.api.wands.WandAccess;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** M6: relics that work from the inventory. */
final class FRPassiveTests {
    private FRPassiveTests() {
    }

    static Player holder(net.minecraft.gametest.framework.GameTestHelper helper, ItemStack... items) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < items.length; i++) {
            player.getInventory().setItem(10 + i, items[i]);
        }
        return player;
    }

    static LivingIncomingDamageEvent event(net.minecraft.world.entity.LivingEntity victim, DamageSource source, float amount) {
        return new LivingIncomingDamageEvent(victim, new DamageContainer(source, amount));
    }

    static void register(FRGameTests.Registrar r) {
        r.add("passive/false_justice_taken", helper -> {
            Player player = holder(helper, new ItemStack(FRItems.FALSE_JUSTICE.get()));
            var e = event(player, helper.getLevel().damageSources().generic(), 5);
            FRCombatEvents.onHurt(e);
            check(helper, e.isCanceled(), "original hit not cancelled");
            check(helper, Math.abs(player.getHealth() - 10F) < 1e-3, "5 damage must become 10 true damage, health " + player.getHealth());
            var absolute = event(player, FRDamageSources.fate(helper.getLevel()), 5);
            FRCombatEvents.onHurt(absolute);
            check(helper, !absolute.isCanceled(), "absolute damage must stay as it is");
            helper.succeed();
        });

        r.add("passive/false_justice_dealt", helper -> {
            Player player = holder(helper, new ItemStack(FRItems.FALSE_JUSTICE.get()));
            var zombie = helper.spawn(EntityType.ZOMBIE, new Vec3(1.5, 2, 1.5));
            float before = zombie.getHealth();
            var e = event(zombie, helper.getLevel().damageSources().playerAttack(player), 4);
            FRCombatEvents.onHurt(e);
            check(helper, e.isCanceled(), "player hit not cancelled");
            check(helper, Math.abs(before - zombie.getHealth() - 8F) < 1e-3, "4 damage must become 8 true damage, lost " + (before - zombie.getHealth()));
            zombie.discard();
            helper.succeed();
        });

        r.add("passive/false_justice_death", helper -> {
            Player player = holder(helper, new ItemStack(FRItems.FALSE_JUSTICE.get()));
            var normal = new LivingDeathEvent(player, helper.getLevel().damageSources().generic());
            FRCombatEvents.onDeath(normal);
            check(helper, normal.isCanceled(), "non-true death must be cancelled");
            var truth = new LivingDeathEvent(player, FRDamageSources.trueDamage(helper.getLevel(), null));
            FRCombatEvents.onDeath(truth);
            check(helper, !truth.isCanceled(), "true damage death must go through");
            check(helper, FRConditions.FalseJusticeEnabled.INSTANCE.test(null), "falseJusticeEnabled defaults to true");
            helper.succeed();
        });

        r.add("passive/chaos_core_multiplier", helper -> {
            Player player = holder(helper, new ItemStack(FRItems.CHAOS_CORE.get()));
            double sum = 0;
            for (int i = 0; i < 400; i++) {
                var e = event(player, helper.getLevel().damageSources().generic(), 10);
                FRCombatEvents.onHurt(e);
                check(helper, e.getAmount() >= 0 && e.getAmount() < 20, "multiplied damage " + e.getAmount() + " outside 0..20");
                sum += e.getAmount();
            }
            check(helper, sum / 400 > 9 && sum / 400 < 11, "mean multiplied damage " + sum / 400 + ", expected ~10");
            helper.succeed();
        });

        r.add("passive/chaos_core_redirect", helper -> {
            Player player = holder(helper, new ItemStack(FRItems.CHAOS_CORE.get()));
            player.setInvulnerable(true);
            var victim = helper.spawn(EntityType.ZOMBIE, new Vec3(1.5, 2, 1.5));
            victim.setInvulnerable(true);
            for (int i = 0; i < 3; i++) {
                helper.spawn(EntityType.ZOMBIE, new Vec3(3.5, 2, 1.5 + i)).setInvulnerable(true);
            }
            int redirected = 0;
            for (int i = 0; i < 400; i++) {
                var e = event(victim, helper.getLevel().damageSources().playerAttack(player), 4);
                FRCombatEvents.onAttack(e);
                if (e.isCanceled()) {
                    redirected++;
                }
            }
            check(helper, redirected > 150 && redirected < 210, "expected ~45 % of 400 attacks redirected, got " + redirected);
            for (var e : helper.getLevel().getEntities(EntityType.ZOMBIE, victim.getBoundingBox().inflate(8), z -> true)) {
                e.discard();
            }
            helper.succeed();
        });

        r.add("passive/chaos_core_effects", helper -> {
            Player player = holder(helper);
            for (int i = 0; i < 100; i++) {
                ChaosCoreItem.applyRandomEffect(player);
            }
            check(helper, !player.getActiveEffects().isEmpty(), "no effect applied");
            check(helper, player.getActiveEffects().stream().allMatch(e -> e.getAmplifier() <= 2 && e.getDuration() <= 2500),
                    "amplifier or duration out of range");
            helper.succeed();
        });

        r.add("passive/omega_core", helper -> {
            Player player = holder(helper, new ItemStack(FRItems.OMEGA_CORE.get()));
            player.setHealth(1);
            var death = new LivingDeathEvent(player, helper.getLevel().damageSources().generic());
            FRCombatEvents.onPlayerDeath(death);
            check(helper, death.isCanceled() && player.getHealth() == player.getMaxHealth(), "omega core must cancel death and heal");
            ItemStack wand = wand();
            player.getInventory().setItem(0, wand);
            ItemStack core = player.getInventory().getItem(10);
            core.getItem().inventoryTick(core, helper.getLevel(), player, null);
            for (var aspect : VisHelper.PRIMALS) {
                check(helper, VisHelper.stored(wand, aspect) == 100, "omega core must add 1 vis of " + aspect.identifier());
            }
            helper.succeed();
        });

        r.add("passive/fate_tome", helper -> {
            ItemStack tome = new ItemStack(FRItems.FATE_TOME.get());
            Player player = holder(helper, tome);
            check(helper, !FateTomeItem.tryRescue(player), "a tome never ticked has no cooldown value and must not save");
            tome.getItem().inventoryTick(tome, helper.getLevel(), player, null);
            check(helper, tome.get(FRDataComponents.FATE_COOLDOWN.get()) == 0, "first tick must set the cooldown to 0");
            check(helper, !FateTomeItem.tryRescue(player), "saved without vis");
            ItemStack wand = wand();
            for (var aspect : VisHelper.PRIMALS) {
                wand = WandAccess.withVis(wand, aspect, 3 * FateTomeItem.primalCost());
            }
            player.getInventory().setItem(0, wand);
            player.setHealth(1);
            check(helper, FateTomeItem.tryRescue(player), "did not save with vis");
            int cooldown = tome.get(FRDataComponents.FATE_COOLDOWN.get());
            check(helper, cooldown >= 600 && cooldown <= 1800, "cooldown " + cooldown + " outside 600..1800");
            check(helper, player.getHealth() == player.getMaxHealth(), "not healed");
            check(helper, player.getActiveEffects().size() == 3, "expected 3 effects, got " + player.getActiveEffects());
            check(helper, !FateTomeItem.tryRescue(player), "saved twice during cooldown");
            tome.getItem().inventoryTick(tome, helper.getLevel(), player, null);
            check(helper, tome.get(FRDataComponents.FATE_COOLDOWN.get()) == cooldown - 1, "cooldown does not count down");
            check(helper, FateTomeItem.countTomes(holder(helper, new ItemStack(FRItems.FATE_TOME.get()), new ItemStack(FRItems.FATE_TOME.get()))) == 2,
                    "two tomes not counted");
            helper.succeed();
        });

        r.add("passive/xp_tome", helper -> {
            check(helper, XpHelper.levelForExperience(XpHelper.experienceForLevel(30)) == 30
                    && XpHelper.levelForExperience(XpHelper.experienceForLevel(30) - 1) == 29, "level curve");
            ItemStack tome = new ItemStack(FRItems.XP_TOME.get());
            Player player = holder(helper, tome);
            XpHelper.setPlayerXp(player, 103);
            check(helper, XpHelper.playerXp(player) == 103, "player xp " + XpHelper.playerXp(player));
            tome.set(FRDataComponents.ACTIVE.get(), true);
            for (int i = 0; i < 25; i++) {
                tome.getItem().inventoryTick(tome, helper.getLevel(), player, null);
            }
            check(helper, XpTomeItem.stored(tome) == 103 && XpHelper.playerXp(player) == 0,
                    "absorption: tome " + XpTomeItem.stored(tome) + ", player " + XpHelper.playerXp(player));
            tome.set(FRDataComponents.ABSORPTION.get(), false);
            for (int i = 0; i < 25; i++) {
                tome.getItem().inventoryTick(tome, helper.getLevel(), player, null);
            }
            check(helper, XpTomeItem.stored(tome) == 0 && XpHelper.playerXp(player) == 103,
                    "extraction: tome " + XpTomeItem.stored(tome) + ", player " + XpHelper.playerXp(player));
            tome.set(FRDataComponents.ACTIVE.get(), false);
            XpHelper.setPlayerXp(player, 50);
            tome.getItem().inventoryTick(tome, helper.getLevel(), player, null);
            check(helper, XpHelper.playerXp(player) == 50, "inactive tome moved experience");
            check(helper, tome.getItem().isFoil(tome) == false, "inactive tome glints");
            helper.succeed();
        });
    }
}
