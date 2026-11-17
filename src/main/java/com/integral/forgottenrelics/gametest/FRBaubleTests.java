package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;
import static com.integral.forgottenrelics.gametest.FRSystemsTests.initSlots;
import static com.integral.forgottenrelics.gametest.FRSystemsTests.wand;

import com.integral.forgottenrelics.compat.CuriosBridge;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.event.FRCombatEvents;
import com.integral.forgottenrelics.event.FRMiningEvents;
import com.integral.forgottenrelics.item.RelicCurioItem;
import com.integral.forgottenrelics.item.bauble.NebulousCoreItem;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRItems;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import net.minecraft.core.Holder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import top.theillusivec4.curios.api.SlotContext;

/** M5: worn relics (curios). Handlers are called directly with a synthetic damage event on a mock player. */
final class FRBaubleTests {
    private FRBaubleTests() {
    }

    static Player wearer(net.minecraft.gametest.framework.GameTestHelper helper, Item... baubles) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        initSlots(player);
        for (Item item : baubles) {
            check(helper, CuriosBridge.equip(player, ((RelicCurioItem) item).slot(), new ItemStack(item)), "could not equip " + item);
        }
        return player;
    }

    /** Runs both damage phases like the game does and returns the event. */
    static LivingIncomingDamageEvent hit(Player player, DamageSource source, float amount) {
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(player, new DamageContainer(source, amount));
        FRCombatEvents.onAttack(event);
        FRCombatEvents.onHurt(event);
        return event;
    }

    static void register(FRGameTests.Registrar r) {
        r.add("bauble/mining_speed", helper -> {
            check(helper, FRMiningEvents.multiplier(wearer(helper)) == 1.0F, "no charm");
            check(helper, FRMiningEvents.multiplier(wearer(helper, FRItems.MINING_CHARM.get())) == 2.0F, "mining charm must give x2");
            check(helper, FRMiningEvents.multiplier(wearer(helper, FRItems.ADVANCED_MINING_CHARM.get())) == 4.0F, "ethereal charm must give x4");
            check(helper, FRMiningEvents.multiplier(wearer(helper, FRItems.MINING_CHARM.get(), FRItems.ADVANCED_MINING_CHARM.get())) == 5.0F,
                    "both charms must give x5");
            helper.succeed();
        });

        r.add("bauble/attribute_modifiers", helper -> {
            Player player = wearer(helper);
            check(helper, modifier(player, FRItems.MINING_CHARM.get(), Attributes.BLOCK_INTERACTION_RANGE) == 2.0, "mining charm reach");
            check(helper, modifier(player, FRItems.ADVANCED_MINING_CHARM.get(), Attributes.BLOCK_INTERACTION_RANGE) == 4.0, "ethereal reach");
            check(helper, modifier(player, FRItems.ANCIENT_AEGIS.get(), Attributes.KNOCKBACK_RESISTANCE) == 1.0, "aegis knockback");
            helper.succeed();
        });

        r.add("bauble/ancient_aegis", helper -> {
            Player player = wearer(helper, FRItems.ANCIENT_AEGIS.get());
            float reduced = hit(player, helper.getLevel().damageSources().generic(), 10).getAmount();
            check(helper, Math.abs(reduced - 7.5F) < 1e-4, "aegis must reduce 10 to 7.5, got " + reduced);
            float absolute = hit(player, FRDamageSources.fate(helper.getLevel()), 10).getAmount();
            check(helper, absolute == 10F, "absolute damage must not be reduced, got " + absolute);
            helper.succeed();
        });

        r.add("bauble/ancient_aegis_redirect", helper -> {
            // the owner search scans level entities; mock players are not in the level, so the found owner is given
            Player guardian = wearer(helper, FRItems.ANCIENT_AEGIS.get());
            Player player = wearer(helper);
            var event = new LivingIncomingDamageEvent(player, new DamageContainer(helper.getLevel().damageSources().generic(), 10));
            FRCombatEvents.redirectToAegis(event, guardian);
            check(helper, Math.abs(event.getAmount() - 6F) < 1e-4, "victim must keep 60 %, kept " + event.getAmount());
            // the guardian takes 4, reduced by its own aegis to 3
            check(helper, Math.abs(guardian.getHealth() - 17F) < 1e-3, "aegis owner health " + guardian.getHealth() + ", expected 17");
            helper.succeed();
        });

        r.add("bauble/dark_sun_ring", helper -> {
            Player player = wearer(helper, FRItems.DARK_SUN_RING.get());
            check(helper, hit(player, helper.getLevel().damageSources().generic(), 150).isCanceled(), "150 damage must be negated");
            player.setHealth(10);
            var lava = hit(player, helper.getLevel().damageSources().lava(), 4);
            check(helper, lava.isCanceled() && player.getHealth() == 14, "lava must heal 4, health " + player.getHealth());
            // 25 % of hits grow by up to 100 %: over many hits some grow, none shrink or more than double
            int grown = 0;
            for (int i = 0; i < 400; i++) {
                player.invulnerableTime = 0;
                var e = hit(player, helper.getLevel().damageSources().fall(), 10);
                if (e.isCanceled()) {
                    continue;
                }
                check(helper, e.getAmount() >= 10 && e.getAmount() <= 20, "amplified hit out of range: " + e.getAmount());
                if (e.getAmount() > 10) {
                    grown++;
                }
            }
            check(helper, grown > 60 && grown < 140, "expected ~100 of 400 hits amplified, got " + grown);
            helper.succeed();
        });

        r.add("bauble/oblivion_amulet", helper -> {
            Player player = wearer(helper, FRItems.OBLIVION_AMULET.get());
            var noVis = hit(player, helper.getLevel().damageSources().generic(), 5);
            check(helper, !noVis.isCanceled(), "absorbed without vis");
            ItemStack wand = wand();
            VisHelper.addVis(wand, TTAspects.IGNIS, 10);
            VisHelper.addVis(wand, TTAspects.PERDITIO, 10);
            player.getInventory().setItem(0, wand);
            var absorbed = hit(player, helper.getLevel().damageSources().generic(), 5);
            check(helper, absorbed.isCanceled(), "not absorbed with vis");
            ItemStack amulet = CuriosBridge.findEquipped(player, FRItems.OBLIVION_AMULET.get());
            check(helper, amulet.getOrDefault(FRDataComponents.STORED_DAMAGE.get(), 0F) == 5F, "stored " + amulet.get(FRDataComponents.STORED_DAMAGE.get()));
            check(helper, VisHelper.stored(wand, TTAspects.IGNIS) < 1000, "ignis not paid");
            helper.succeed();
        });

        r.add("bauble/superposition_split", helper -> {
            // the wearer list comes from the server's player list; mock players are not in it, so it is given
            Player other = wearer(helper, FRItems.SUPERPOSITION_RING.get());
            other.setInvulnerable(true);
            Player player = wearer(helper, FRItems.SUPERPOSITION_RING.get());
            double sum = 0;
            for (int i = 0; i < 300; i++) {
                var event = new LivingIncomingDamageEvent(player, new DamageContainer(helper.getLevel().damageSources().generic(), 100));
                FRCombatEvents.splitSuperposition(event, new java.util.ArrayList<>(java.util.List.of(other)));
                float left = event.getAmount();
                check(helper, left >= 26 - 1e-3 && left <= 88 + 1e-3, "kept " + left + " of 100, expected 26..88");
                sum += left;
            }
            double mean = sum / 300;
            check(helper, mean > 52 && mean < 62, "mean kept damage " + mean + ", expected ~57");
            var alone = new LivingIncomingDamageEvent(player, new DamageContainer(helper.getLevel().damageSources().generic(), 100));
            FRCombatEvents.splitSuperposition(alone, new java.util.ArrayList<>());
            check(helper, alone.getAmount() == 100, "no other wearer: damage must stay");
            helper.succeed();
        });

        r.add("bauble/deific_amulet", helper -> {
            Player player = wearer(helper);
            ItemStack amulet = new ItemStack(FRItems.DEIFIC_AMULET.get());
            player.addEffect(new MobEffectInstance(MobEffects.SPEED, 200));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200));
            player.igniteForSeconds(5);
            player.invulnerableTime = 15;
            tick(player, amulet, "necklace");
            check(helper, player.getActiveEffects().isEmpty(), "effects not removed: " + player.getActiveEffects());
            check(helper, !player.isOnFire(), "not extinguished");
            check(helper, player.invulnerableTime == 40, "invulnerability " + player.invulnerableTime + ", expected 40");
            check(helper, amulet.getOrDefault(FRDataComponents.RELIC_COOLDOWN.get(), 0) == 31, "cooldown " + amulet.get(FRDataComponents.RELIC_COOLDOWN.get()));
            // air refill costs 10 aer + 10 aqua
            player.setAirSupply(0);
            ItemStack wand = wand();
            VisHelper.addVis(wand, TTAspects.AER, 20);
            VisHelper.addVis(wand, TTAspects.AQUA, 20);
            player.getInventory().setItem(0, wand);
            tick(player, amulet, "necklace");
            check(helper, player.getAirSupply() == 300, "air " + player.getAirSupply());
            check(helper, VisHelper.stored(wand, TTAspects.AER) < 2000, "air refill was free");
            helper.succeed();
        });

        r.add("bauble/shiny_stone", helper -> {
            Player player = wearer(helper);
            ItemStack stone = new ItemStack(FRItems.SHINY_STONE.get());
            player.setPos(0.5, 100, 0.5); // away from the component's default (0, 0, 0)
            int rate = -1;
            // checks every 4 ticks; "static" grows by 4 per check standing still: rate 2 from 40, 3 from 80, 4 from 200
            for (int check = 0; check <= 51; check++) {
                player.tickCount = check * 4;
                tick(player, stone, "necklace");
                rate = stone.get(FRDataComponents.STAND_STATE.get()).healRate();
                int expected = check == 0 ? 0 : check <= 10 ? 1 : check <= 20 ? 2 : check <= 50 ? 3 : 4;
                FRGameTests.check(helper, rate == expected, "check " + check + ": heal rate " + rate + ", expected " + expected);
            }
            player.setPos(player.getX() + 1, player.getY(), player.getZ());
            player.tickCount += 4;
            tick(player, stone, "necklace");
            check(helper, stone.get(FRDataComponents.STAND_STATE.get()).healRate() == 0, "moving must reset the heal rate");
            helper.succeed();
        });

        r.add("bauble/nebulous_core_dormant_cycle", helper -> {
            Player player = wearer(helper);
            ItemStack wand = wand();
            for (var aspect : VisHelper.PRIMALS) {
                VisHelper.addVis(wand, aspect, 10);
            }
            player.getInventory().setItem(0, wand);
            CuriosBridge.equip(player, "necklace", NebulousCoreItem.dormant(2));
            for (int i = 0; i < 3; i++) {
                ItemStack worn = CuriosBridge.findEquipped(player, FRItems.NEBULOUS_CORE_DORMANT.get());
                check(helper, !worn.isEmpty(), "dormant core vanished at step " + i);
                tick(player, worn, "necklace");
            }
            check(helper, CuriosBridge.hasBauble(player, FRItems.NEBULOUS_CORE.get()), "core did not wake up after its lifetime");
            check(helper, VisHelper.stored(wand, TTAspects.PERDITIO) < 1000, "dormant core did not pay perditio");
            helper.succeed();
        });

        r.add("bauble/nebulous_core_vis", helper -> {
            Player player = wearer(helper);
            ItemStack wand = wand();
            player.getInventory().setItem(0, wand);
            ItemStack core = new ItemStack(FRItems.NEBULOUS_CORE.get());
            for (int i = 0; i < 400; i++) {
                tick(player, core, "necklace");
            }
            int total = 0;
            for (var aspect : VisHelper.PRIMALS) {
                total += VisHelper.stored(wand, aspect);
            }
            // 2.5 % per tick x 400 ticks = 10 vis expected
            check(helper, total >= 300 && total <= 2000, "generated " + total + " centivis, expected ~1000");
            check(helper, VisHelper.stored(wand, TTAspects.PERDITIO) == 0, "perditio generated, the original never picked it");
            check(helper, ((NebulousCoreItem) FRItems.NEBULOUS_CORE.get()).getVisDiscount(core) == 35, "discount");
            helper.succeed();
        });
    }

    /** One Curios worn tick in slot 0 of the given type. */
    static void tick(Player player, ItemStack stack, String slot) {
        ((RelicCurioItem) stack.getItem()).curioTick(new SlotContext(slot, player, 0, false, true), stack);
    }

    private static double modifier(Player player, Item item, Holder<Attribute> attribute) {
        var map = ((RelicCurioItem) item).getAttributeModifiers(new SlotContext(((RelicCurioItem) item).slot(), player, 0, false, true),
                com.integral.forgottenrelics.ForgottenRelics.id("test"), new ItemStack(item));
        return map.get(attribute).stream().mapToDouble(AttributeModifier::amount).sum();
    }
}
