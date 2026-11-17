package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FRDamageSources;
import com.integral.forgottenrelics.registry.FRDataComponents;
import com.integral.forgottenrelics.registry.FRDataComponents.TelekinesisState;
import com.integral.forgottenrelics.util.FRFx;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Tome of Predestiny (ItemTelekinesisTome; based on Botania's Rod of the Shaded Mesa). Holding Right-click grabs the
 * entity in sight (searched in 3-block boxes stepped along the look) for 0.2 aer + 0.2 ordo vis per pull and drags it
 * 7.5 blocks in front of the caster (Shift: keeps its distance). Left-click strikes the held entity within 16 blocks
 * with lightning (16..40) for 0.8 aer + 0.5 ordo + 2 ignis vis; Shift+Left-click also hurls it (1.5 aer + 0.8 ordo).
 * Bosses cannot be moved.
 */
public class TelekinesisTomeItem extends RelicItem {
    static final float RANGE = 3F;

    public TelekinesisTomeItem(Properties properties) {
        super(properties, 4);
    }

    static double mult() {
        return FRConfig.telekinesisTomeVisMult.getAsDouble();
    }

    static TelekinesisState state(ItemStack stack) {
        return stack.getOrDefault(FRDataComponents.TELEKINESIS.get(), TelekinesisState.EMPTY);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (!(owner instanceof Player)) {
            return;
        }
        TelekinesisState s = state(stack);
        int target = s.target();
        double reDist = s.reDist();
        if (s.ticksTillExpire() == 0) {
            target = -1;
            reDist = -1;
        }
        stack.set(FRDataComponents.TELEKINESIS.get(), new TelekinesisState(target, reDist, s.ticksTillExpire() - 1,
                Math.max(0, s.cooldown() - 1), Math.max(0, s.attackCooldown() - 1)));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level instanceof ServerLevel server) {
            pull(server, player, player.getItemInHand(hand));
        }
        return InteractionResult.SUCCESS;
    }

    /** One Right-click repeat (every 4 ticks while held, as in 1.7). */
    public static void pull(ServerLevel level, Player player, ItemStack stack) {
        TelekinesisState s = state(stack);
        if (s.cooldown() != 0) {
            return;
        }
        LivingEntity item = null;
        if (s.target() != -1 && level.getEntity(s.target()) != null) {
            item = existingTarget(level, player, s.target(), RANGE + 3);
        }
        if (item == null) {
            item = searchTarget(level, player, RANGE);
        }
        double length = 7.5D;
        double reDist = s.reDist();
        if (item != null && reDist == -1) {
            reDist = player.distanceTo(item);
        }
        if (item != null && VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, (int) (20 * mult()), TTAspects.ORDO, (int) (20 * mult())))) {
            item.fallDistance = 0.0F;
            if (!item.hasEffect(MobEffects.SLOWNESS)) {
                item.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 2, 3, true, true));
            }
            Vec3 target3 = player.getBoundingBox().getCenter();
            if (player.isShiftKeyDown()) {
                target3 = target3.add(player.getLookAngle().scale(reDist));
            } else {
                target3 = target3.add(player.getLookAngle().scale(length));
                reDist = player.distanceTo(item);
            }
            target3 = target3.add(0, 0.5, 0);
            Vec3 c = item.getBoundingBox().getCenter();
            for (int i = 0; i <= 6; i++) {
                BotaniaBridge.wisp(level, c.x, c.y, c.z, 0.2F + (float) Math.random() * 0.3F, 0.0F, 0.5F + (float) Math.random() * 0.2F,
                        0.2F + (float) Math.random() * 0.1F, (Math.random() - 0.5) * 0.15, (Math.random() - 0.5) * 0.15, (Math.random() - 0.5) * 0.15, 1.0F);
            }
            FRFx.portalBurst(level, c, 9);
            double distance = item.position().distanceTo(target3);
            float power = 0.66666F;
            if (distance < 1.5) {
                power = 0.333333F;
            } else if (distance >= 8) {
                power *= (float) (distance / 8.0F);
            }
            if (BotaniaBridge.isRelicBoss(item)) {
                return;
            }
            motionTowards(item, target3, power);
            s = state(stack).withTarget(item.getId(), reDist);
        }
        if (item != null) {
            s = new TelekinesisState(s.target(), s.reDist(), 5, s.cooldown(), s.attackCooldown());
        }
        stack.set(FRDataComponents.TELEKINESIS.get(), s);
    }

    /** Original {@code setEntityMotionFromVector}: towards the point, at most length 1, times the power. */
    static void motionTowards(Entity entity, Vec3 point, float power) {
        Vec3 v = point.subtract(entity.getBoundingBox().getCenter());
        if (v.length() > 1) {
            v = v.normalize();
        }
        entity.setDeltaMovement(v.scale(power));
        entity.hurtMarked = true;
    }

    /** Original {@code searchForTarget}: boxes of the given range stepped 1, 3, 6, 10... blocks along the look. */
    static LivingEntity searchTarget(ServerLevel level, Player player, float range) {
        Vec3 t = player.getBoundingBox().getCenter();
        List<LivingEntity> entities = List.of();
        for (int distance = 1; entities.isEmpty() && distance < 25; distance++) {
            t = t.add(player.getLookAngle().scale(distance)).add(0, 0.5, 0);
            entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(t, t).inflate(range), e -> e != player);
        }
        return entities.isEmpty() ? null : entities.get(0);
    }

    /** Original {@code getExistingTarget}: the held entity, only if the first non-empty box along the look holds it. */
    static LivingEntity existingTarget(ServerLevel level, Player player, int id, float range) {
        Entity held = level.getEntity(id);
        Vec3 t = player.getBoundingBox().getCenter();
        List<LivingEntity> entities = List.of();
        for (int distance = 1; entities.isEmpty() && distance < 25; distance++) {
            t = t.add(player.getLookAngle().scale(distance)).add(0, 0.5, 0);
            entities = level.getEntitiesOfClass(LivingEntity.class, new AABB(t, t).inflate(range), e -> e != player);
            if (entities.contains(held)) {
                return (LivingEntity) held;
            }
        }
        return null;
    }

    /** Left-click (the swing packet reaches the server for clicks into the air too). */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
        if (entity instanceof Player player && player.level() instanceof ServerLevel level) {
            TelekinesisState s = state(stack);
            if (s.target() != -1 && level.getEntity(s.target()) != null) {
                LivingEntity held = existingTarget(level, player, s.target(), RANGE + 3);
                if (held != null) {
                    lightningAttack(level, player, held, stack);
                }
            }
        }
        return false;
    }

    public static void lightningAttack(ServerLevel level, Player player, LivingEntity target, ItemStack stack) {
        TelekinesisState s = state(stack);
        if (s.attackCooldown() != 0) {
            return;
        }
        if (player.distanceTo(target) <= 16 && VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, (int) (80 * mult()),
                TTAspects.ORDO, (int) (50 * mult()), TTAspects.IGNIS, (int) (200 * mult())))) {
            for (int i = 0; i <= 3; i++) {
                FRFx.lightning(level, player.position().add(0, 1, 0), target.getBoundingBox().getCenter(), 20,
                        (float) (0.225F + player.distanceToSqr(target) / 2000));
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.zap(), SoundSource.PLAYERS, 1F, 0.8F);
            double min = FRConfig.telekinesisTomeDamageMIN.getAsDouble();
            double max = FRConfig.telekinesisTomeDamageMAX.getAsDouble();
            target.hurtServer(level, FRDamageSources.telekineticLightning(level, player), (float) (min + Math.random() * (max - min)));
        }
        if (player.isShiftKeyDown() && VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, (int) (150 * mult()),
                TTAspects.ORDO, (int) (80 * mult())))) {
            Vec3 look = player.getLookAngle().normalize();
            target.setDeltaMovement(look.x * 3.0F, look.y * 1.5F, look.z * 3.0F);
            target.hurtMarked = true;
            s = new TelekinesisState(-1, -1, s.ticksTillExpire(), 10, s.attackCooldown());
        }
        stack.set(FRDataComponents.TELEKINESIS.get(), new TelekinesisState(s.target(), s.reDist(), s.ticksTillExpire(), s.cooldown(), 10));
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        double m = mult();
        String damage = (int) FRConfig.telekinesisTomeDamageMIN.getAsDouble() + "-" + (int) FRConfig.telekinesisTomeDamageMAX.getAsDouble();
        if (control() && !shift()) {
            lines.accept(lore("FRVisPerTick"));
            lines.accept(" " + lore("FRAerCost") + 20 * m / 100);
            lines.accept(" " + lore("FROrdoCost") + 20 * m / 100);
            lines.accept(lore("FREmpty"));
            lines.accept(lore("FRVisPerCast"));
            lines.accept(" " + lore("FRAerCost") + 80 * m / 100);
            lines.accept(" " + lore("FROrdoCost") + 50 * m / 100);
            lines.accept(" " + lore("FRIgnisCost") + 200 * m / 100);
            lines.accept(lore("FREmpty"));
            return;
        }
        tomeTooltip(lines, new String[]{"ItemTelekinesisTome1", "ItemTelekinesisTome2", "ItemTelekinesisTome3", "FREmpty", "ItemTelekinesisTome4",
                "ItemTelekinesisTome5", "ItemTelekinesisTome6", "FREmpty", "ItemTelekinesisTome7", "ItemTelekinesisTome8", "ItemTelekinesisTome9",
                "FREmpty", "ItemTelekinesisTome10", "FREmpty", loreValue("ItemTelekinesisTome11_1", damage, "ItemTelekinesisTome11_2")},
                "FRVisPerCast", new double[6]);
    }
}
