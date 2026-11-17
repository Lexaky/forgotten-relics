package com.integral.forgottenrelics.item.tome;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.entity.ChaoticOrbEntity;
import com.integral.forgottenrelics.registry.FREntities;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Tome of Primal Chaos (ItemChaosTome): while held, every second tick pays a random 0..1 vis of each primal
 * (x chaosTomeVisMult) and releases an Orb of Primal Energy (35 % seekers) around the caster.
 */
public class ChaosTomeItem extends HeldTomeItem {
    public ChaosTomeItem(Properties properties) {
        super(properties, 4);
    }

    static int primal() {
        return (int) (100 * FRConfig.chaosTomeVisMult.getAsDouble());
    }

    @Override
    protected void castTick(ServerLevel level, Player player, ItemStack stack, int remaining) {
        if (!everySecondTick(remaining)) {
            return;
        }
        var p = VisHelper.PRIMALS;
        int c = primal();
        var cost = VisHelper.cost(p.get(0), (int) (c * Math.random()), p.get(1), (int) (c * Math.random()), p.get(3), (int) (c * Math.random()),
                p.get(2), (int) (c * Math.random()), p.get(4), (int) (c * Math.random()), p.get(5), (int) (c * Math.random()));
        if (VisHelper.consumeFromInventory(player, cost)) {
            spawnOrb(level, player);
        }
    }

    public static ChaoticOrbEntity spawnOrb(ServerLevel level, Player player) {
        Vec3 center = player.getBoundingBox().getCenter();
        ChaoticOrbEntity orb = new ChaoticOrbEntity(FREntities.CHAOTIC_ORB.get(), level);
        orb.setOwner(player);
        orb.setSeeker(Math.random() <= 0.35D);
        Vec3 pos = center.add((Math.random() - 0.5D) * 3.0D, (Math.random() - 0.5D) * 1.0D, (Math.random() - 0.5D) * 3.0D);
        orb.setPos(pos);
        orb.setDeltaMovement(pos.subtract(center).scale(0.2D + Math.random() * 0.2D));
        level.addFreshEntity(orb);
        level.playSound(null, orb.getX(), orb.getY(), orb.getZ(), TTSounds.ice(), SoundSource.PLAYERS, 0.3F, 0.8F + level.getRandom().nextFloat() * 0.1F);
        return orb;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        double v = primal() / 100.0 * 10.0 / 2; // average of the random cost, ten casts per second
        tomeTooltip(lines, new String[]{"ItemChaosTome1", "FREmpty", "ItemChaosTome2", "ItemChaosTome3", "FREmpty", "ItemChaosTome4", "FREmpty",
                loreValue("ItemChaosTome5_1", "1-" + (int) FRConfig.chaosTomeDamageCap.getAsDouble(), "ItemChaosTome5_2"), "ItemChaosTome6"},
                "FRVisPerSecond", new double[]{v, v, v, v, v, v});
    }
}
