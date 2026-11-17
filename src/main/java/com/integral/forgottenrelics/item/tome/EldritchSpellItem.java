package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.DarkMatterOrbEntity;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.registry.FREntities;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Eldritch Spell (ItemEldritchSpell): Right-click fires a Dark Matter Orb for 4 perditio vis, 20-tick cooldown. */
public class EldritchSpellItem extends RelicItem {
    public EldritchSpellItem(Properties properties) {
        super(properties, 4);
    }

    static int perditio() {
        return (int) (400 * FRConfig.eldritchSpellVisMult.getAsDouble());
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
        if (!VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.PERDITIO, perditio()))) {
            return InteractionResult.PASS;
        }
        spawnOrb(server, player);
        player.getCooldowns().addCooldown(stack, 20);
        return InteractionResult.SUCCESS;
    }

    public static DarkMatterOrbEntity spawnOrb(ServerLevel level, Player player) {
        Vec3 pos = player.getBoundingBox().getCenter().add(player.getLookAngle()).add(0, 0.5, 0);
        DarkMatterOrbEntity orb = new DarkMatterOrbEntity(FREntities.DARK_MATTER_ORB.get(), level);
        orb.setOwner(player);
        orb.setPos(pos);
        orb.setDeltaMovement(player.getLookAngle().scale(1.25F));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), TTSounds.egAttack(), SoundSource.PLAYERS, 0.6F, 0.8F + (float) Math.random() * 0.2F);
        level.addFreshEntity(orb);
        return orb;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        tomeTooltip(lines, new String[]{"ItemEldritchSpell1", "ItemEldritchSpell2", "ItemEldritchSpell3", "FREmpty", "ItemEldritchSpell4",
                loreValue("ItemEldritchSpell5_1", FRConfig.eldritchSpellDamage.getAsDouble(), "ItemEldritchSpell5_2"), "ItemEldritchSpell6",
                "FREmpty", "ItemEldritchSpell7"}, "FRVisPerCast", new double[]{0, 0, 0, 0, 0, perditio() / 100.0});
    }
}
