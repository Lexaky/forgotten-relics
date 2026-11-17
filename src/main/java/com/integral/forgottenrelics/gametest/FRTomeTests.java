package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;
import static com.integral.forgottenrelics.gametest.FRUtilityTests.richWand;

import com.integral.forgottenrelics.entity.BabylonWeaponEntity;
import com.integral.forgottenrelics.entity.ChaoticOrbEntity;
import com.integral.forgottenrelics.entity.LunarFlareEntity;
import com.integral.forgottenrelics.entity.RageousMissileEntity;
import com.integral.forgottenrelics.entity.SoulEnergyEntity;
import com.integral.forgottenrelics.item.tome.ApotheosisItem;
import com.integral.forgottenrelics.item.tome.CrimsonSpellItem;
import com.integral.forgottenrelics.item.tome.EldritchSpellItem;
import com.integral.forgottenrelics.item.tome.HeldTomeItem;
import com.integral.forgottenrelics.item.tome.LunarFlaresItem;
import com.integral.forgottenrelics.item.tome.MissileTomeItem;
import com.integral.forgottenrelics.item.tome.SoulTomeItem;
import com.integral.forgottenrelics.item.tome.TelekinesisTomeItem;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FREntities;
import com.integral.forgottenrelics.registry.FRItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** M8: combat tomes and their projectiles, run over real ticks where the projectile has to fly. */
final class FRTomeTests {
    private FRTomeTests() {
    }

    static Player caster(GameTestHelper helper, Vec3 rel) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(rel));
        player.getInventory().setItem(5, richWand());
        return player;
    }

    /** A sturdy, still target (iron golem, 100 health). */
    static Mob target(GameTestHelper helper, Vec3 rel) {
        Mob mob = helper.spawn(EntityType.IRON_GOLEM, rel);
        mob.setNoAi(true);
        return mob;
    }

    static void lookAt(Player player, Vec3 point) {
        Vec3 d = point.subtract(player.getEyePosition());
        player.setYRot((float) Math.toDegrees(Math.atan2(-d.x, d.z)));
        player.setXRot((float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z))));
        player.setYHeadRot(player.getYRot());
    }

    static void floor(GameTestHelper helper) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(x, 1, z, Blocks.STONE);
            }
        }
    }

    static void hurt(GameTestHelper helper, Mob mob) {
        if (mob.getHealth() >= mob.getMaxHealth()) {
            throw helper.assertionException(Component.literal("target not hurt yet"));
        }
    }

    static void register(FRGameTests.Registrar r) {
        r.addIsolated("tome/crimson_orb", 100, helper -> {
            floor(helper);
            Mob golem = target(helper, new Vec3(3.5, 2, 3.5));
            Player player = caster(helper, new Vec3(0.5, 2, 0.5));
            lookAt(player, golem.getBoundingBox().getCenter());
            check(helper, CrimsonSpellItem.findTarget(helper.getLevel(), player) == golem, "the golem in sight is not the target");
            CrimsonSpellItem.spawnOrb(helper.getLevel(), player, golem);
            helper.succeedWhen(() -> {
                hurt(helper, golem);
                float lost = golem.getMaxHealth() - golem.getHealth();
                if (lost < 42 - 1e-3 || lost > 100 + 1e-3) {
                    throw helper.assertionException(Component.literal("crimson orb dealt " + lost + ", expected 42..100"));
                }
            });
        });

        r.addIsolated("tome/dark_matter_orb", 100, helper -> {
            floor(helper);
            Mob golem = target(helper, new Vec3(3.5, 2, 2.5));
            Player player = caster(helper, new Vec3(0.5, 2, 2.5));
            lookAt(player, golem.getBoundingBox().getCenter());
            EldritchSpellItem.spawnOrb(helper.getLevel(), player);
            helper.succeedWhen(() -> {
                hurt(helper, golem);
                if (!golem.hasEffect(MobEffects.WITHER) || !golem.hasEffect(MobEffects.WEAKNESS)) {
                    throw helper.assertionException(Component.literal("dark matter effects missing"));
                }
                float lost = golem.getMaxHealth() - golem.getHealth();
                if (lost < 32) {
                    throw helper.assertionException(Component.literal("dark matter dealt " + lost + ", expected 32.5"));
                }
            });
        });

        r.addIsolated("tome/rageous_missile", 200, helper -> {
            Mob golem = target(helper, new Vec3(2.5, 2, 2.5));
            Player player = caster(helper, new Vec3(0.5, 2, 0.5));
            MissileTomeItem.spawnMissile(helper.getLevel(), player, helper.absoluteVec(new Vec3(0.5, 5, 0.5)).x,
                    helper.absoluteVec(new Vec3(0.5, 5, 0.5)).y, helper.absoluteVec(new Vec3(0.5, 5, 0.5)).z);
            helper.succeedWhen(() -> {
                hurt(helper, golem);
                float lost = golem.getMaxHealth() - golem.getHealth();
                if (lost < 24 - 1e-3 || lost > 32 + 1e-3) {
                    throw helper.assertionException(Component.literal("missile dealt " + lost + ", expected 24..32"));
                }
            });
        });

        r.addIsolated("tome/lunar_flare", 200, helper -> {
            floor(helper);
            Mob golem = target(helper, new Vec3(2.5, 2, 2.5));
            Player player = caster(helper, new Vec3(0.5, 2, 0.5));
            BlockPos lock = helper.absolutePos(new BlockPos(2, 1, 2));
            LunarFlareEntity flare = LunarFlaresItem.spawnFlare(helper.getLevel(), player, lock);
            check(helper, flare.getY() > lock.getY() + 5, "flare must start high above the target, y " + flare.getY());
            // the random start can be up to 9 blocks aside, in a chunk the test world does not tick: start straight above
            flare.setPos(lock.getX() + 0.5, lock.getY() + 20, lock.getZ() + 0.5);
            flare.setDeltaMovement(0, -4, 0);
            helper.succeedWhen(() -> {
                if (!flare.isRemoved()) {
                    throw helper.assertionException(Component.literal("flare still flying"));
                }
                hurt(helper, golem);
            });
        });

        r.add("tome/babylon_weapon_impact", helper -> {
            Mob golem = target(helper, new Vec3(2.5, 2, 2.5));
            Player player = caster(helper, new Vec3(0.5, 2, 0.5));
            BabylonWeaponEntity weapon = ApotheosisItem.spawnWeapon(helper.getLevel(), player);
            check(helper, weapon.distanceTo(player) <= 14, "weapon spawned " + weapon.distanceTo(player) + " blocks away");
            BabylonWeaponEntity second = ApotheosisItem.spawnWeapon(helper.getLevel(), player);
            check(helper, second.distanceTo(weapon) > 2 - 1e-3, "two weapons spawned within 2 blocks");
            weapon.setPos(golem.position().add(0, 1, 0));
            weapon.impact(helper.getLevel());
            float lost = golem.getMaxHealth() - golem.getHealth();
            check(helper, Math.abs(lost - 75) < 1e-3, "impact dealt " + lost + ", expected 75");
            check(helper, weapon.isRemoved(), "weapon not removed after impact");
            second.discard();
            helper.succeed();
        });

        // M9: the renderer reads the charge/launch state from synced data, as the original's data watcher did
        r.add("client/babylon_weapon_synced_state", helper -> {
            Player player = caster(helper, new Vec3(0.5, 2, 0.5));
            BabylonWeaponEntity weapon = ApotheosisItem.spawnWeapon(helper.getLevel(), player);
            weapon.setDelay(1000);
            check(helper, weapon.delay() == 1000, "synced delay " + weapon.delay());
            check(helper, weapon.liveTicks() == 0, "synced live ticks before charging " + weapon.liveTicks());
            for (int i = 0; i < 20; i++) {
                weapon.tickCount++; // the level's entity loop advances it, not tick()
                weapon.tick();
            }
            // ticks 16..20 count as live ticks (the first 15 are charging)
            check(helper, weapon.liveTicks() == 5, "synced live ticks after 20 ticks: " + weapon.liveTicks());
            weapon.discard();
            // Thaumaturge effects must be safe to send from the server with nobody in range
            com.integral.forgottenrelics.compat.TTEffects.burst(helper.getLevel(), 0, 0, 0, 1.0F);
            com.integral.forgottenrelics.util.FRFx.lightning(helper.getLevel(), Vec3.ZERO, new Vec3(0, 4, 0), 20, 0.075F);
            com.integral.forgottenrelics.compat.TTEffects.arc(helper.getLevel(), Vec3.ZERO, new Vec3(0, 4, 0), 1F, 0.6F, 1F, 0.4F);
            helper.succeed();
        });

        r.add("tome/soul_tome", helper -> {
            Mob golem = target(helper, new Vec3(2.5, 2, 2.5));
            Player player = caster(helper, new Vec3(0.5, 2, 0.5));
            float drained = SoulTomeItem.drain(helper.getLevel(), player, golem);
            check(helper, drained == 10F && Math.abs(golem.getHealth() - 90F) < 1e-3, "soul drain " + drained + ", golem " + golem.getHealth());
            var souls = helper.getLevel().getEntitiesOfClass(SoulEnergyEntity.class, golem.getBoundingBox().inflate(4));
            check(helper, souls.size() == 1, "expected one soul, found " + souls.size());
            souls.forEach(e -> e.discard());
            golem.setHealth(golem.getMaxHealth());
            golem.invulnerableTime = 0;
            SoulTomeItem.throwBack(helper.getLevel(), player, golem);
            float lost = golem.getMaxHealth() - golem.getHealth();
            check(helper, lost >= 20 - 1e-3 && lost <= 100 + 1e-3, "throw-back lightning " + lost + ", expected 20..100");
            check(helper, golem.getDeltaMovement().y > 0.5, "not thrown upwards");
            helper.succeed();
        });

        r.add("tome/telekinesis", helper -> {
            Mob golem = target(helper, new Vec3(3.5, 2, 2.5));
            Player player = caster(helper, new Vec3(0.5, 2, 2.5));
            lookAt(player, golem.getBoundingBox().getCenter());
            ItemStack tome = new ItemStack(FRItems.TELEKINESIS_TOME.get());
            TelekinesisTomeItem.pull(helper.getLevel(), player, tome);
            var state = tome.get(FRDataComponents.TELEKINESIS.get());
            check(helper, state != null && state.target() == golem.getId() && state.ticksTillExpire() == 5, "target not held: " + state);
            check(helper, golem.getDeltaMovement().length() > 0.1, "held entity not moved");
            check(helper, golem.hasEffect(MobEffects.SLOWNESS), "held entity not slowed");
            TelekinesisTomeItem.lightningAttack(helper.getLevel(), player, golem, tome);
            float lost = golem.getMaxHealth() - golem.getHealth();
            check(helper, lost >= 16 - 1e-3 && lost <= 40 + 1e-3, "lightning " + lost + ", expected 16..40");
            check(helper, tome.get(FRDataComponents.TELEKINESIS.get()).attackCooldown() == 10, "attack cooldown");
            helper.succeed();
        });

        r.add("tome/held_casting_costs", helper -> {
            Player poor = helper.makeMockPlayer(GameType.SURVIVAL);
            poor.setPos(helper.absoluteVec(new Vec3(1.5, 2, 1.5)));
            ItemStack fury = new ItemStack(FRItems.MISSILE_TOME.get());
            AABB around = new AABB(poor.position(), poor.position()).inflate(8);
            fury.getItem().onUseTick(helper.getLevel(), poor, fury, HeldTomeItem.USE_TICKS - 2);
            check(helper, helper.getLevel().getEntitiesOfClass(RageousMissileEntity.class, around).isEmpty(), "missile without vis");
            Player rich = caster(helper, new Vec3(1.5, 2, 1.5));
            fury.getItem().onUseTick(helper.getLevel(), rich, fury, HeldTomeItem.USE_TICKS);
            check(helper, helper.getLevel().getEntitiesOfClass(RageousMissileEntity.class, around).isEmpty(), "missile on the first tick");
            fury.getItem().onUseTick(helper.getLevel(), rich, fury, HeldTomeItem.USE_TICKS - 3);
            check(helper, helper.getLevel().getEntitiesOfClass(RageousMissileEntity.class, around).isEmpty(), "missile on an odd tick");
            fury.getItem().onUseTick(helper.getLevel(), rich, fury, HeldTomeItem.USE_TICKS - 2);
            check(helper, helper.getLevel().getEntitiesOfClass(RageousMissileEntity.class, around).size() == 1, "no missile with vis");
            ItemStack chaos = new ItemStack(FRItems.CHAOS_TOME.get());
            chaos.getItem().onUseTick(helper.getLevel(), rich, chaos, HeldTomeItem.USE_TICKS - 2);
            var orbs = helper.getLevel().getEntitiesOfClass(ChaoticOrbEntity.class, around);
            check(helper, orbs.size() == 1, "no chaotic orb");
            for (var e : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class, around,
                    e -> e.getType() == FREntities.RAGEOUS_MISSILE.get() || e.getType() == FREntities.CHAOTIC_ORB.get())) {
                e.discard();
            }
            helper.succeed();
        });
    }
}
