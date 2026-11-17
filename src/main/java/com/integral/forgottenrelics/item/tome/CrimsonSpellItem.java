package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.CrimsonOrbEntity;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FREntities;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Crimson Spell (ItemCrimsonSpell): Right-click casts a Crimson Orb at the entity in sight (a box of 3/6/8 blocks
 * moved 1..31 blocks along the look), or at a random visible entity within 32 blocks; 4.8 ignis + 3.6 perditio vis
 * (x crimsonSpellVisMult), 30-tick cooldown.
 */
public class CrimsonSpellItem extends RelicItem {
    public static final float SEARCH_RANGE = 3F;

    public CrimsonSpellItem(Properties properties) {
        super(properties, 3);
    }

    static int ignis() {
        return (int) (480 * FRConfig.crimsonSpellVisMult.getAsDouble());
    }

    static int perditio() {
        return (int) (360 * FRConfig.crimsonSpellVisMult.getAsDouble());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (!VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.IGNIS, ignis(), TTAspects.PERDITIO, perditio()))) {
            return InteractionResult.PASS;
        }
        spawnOrb(server, player, findTarget(server, player));
        player.getCooldowns().addCooldown(stack, 30);
        return InteractionResult.SUCCESS;
    }

    /** Original target search of {@code onItemRightClick}. */
    public static LivingEntity findTarget(ServerLevel level, Player player) {
        Vec3 vec = player.getBoundingBox().getCenter();
        List<LivingEntity> entities = new ArrayList<>();
        for (int distance = 1; entities.isEmpty() && distance < 32; distance++) {
            float extra = distance > 20 ? 5F : distance > 10 ? 3F : 0F;
            vec = vec.add(player.getLookAngle().scale(distance)).add(0, 0.5, 0);
            entities = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(vec, vec).inflate(SEARCH_RANGE + extra), e -> e != player));
        }
        if (entities.isEmpty()) {
            entities = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32),
                    e -> e != player && e.hasLineOfSight(player)));
        }
        return entities.isEmpty() ? null : entities.get((int) ((entities.size() - 1) * Math.random()));
    }

    public static CrimsonOrbEntity spawnOrb(ServerLevel level, Player player, LivingEntity target) {
        Vec3 pos = player.getBoundingBox().getCenter().add(player.getLookAngle()).add(0, 0.5, 0);
        CrimsonOrbEntity orb = new CrimsonOrbEntity(FREntities.CRIMSON_ORB.get(), level);
        orb.setOwner(player);
        orb.setTarget(target);
        orb.setPos(pos);
        orb.setDeltaMovement(player.getLookAngle().scale(0.75F));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.egAttack(), SoundSource.PLAYERS, 0.6F, 0.8F + (float) Math.random() * 0.2F);
        level.addFreshEntity(orb);
        return orb;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        String damage = (int) FRConfig.crimsonSpellDamageMIN.getAsDouble() + "-" + (int) FRConfig.crimsonSpellDamageMAX.getAsDouble();
        tomeTooltip(lines, new String[]{"ItemCrimsonSpell1", "ItemCrimsonSpell2", "ItemCrimsonSpell3", "FREmpty", "ItemCrimsonSpell4",
                "ItemCrimsonSpell5", loreValue("ItemCrimsonSpell6_1", damage, "ItemCrimsonSpell6_2")},
                "FRVisPerCast", new double[]{0, 0, ignis() / 100.0, 0, 0, perditio() / 100.0});
    }
}
