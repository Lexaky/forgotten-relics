package com.integral.forgottenrelics.item.utility;

import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.compat.BotaniaBridge;
import com.integral.forgottenrelics.compat.TTSounds;
import com.integral.forgottenrelics.compat.VisHelper;
import com.integral.forgottenrelics.item.RelicItem;
import com.leclowndu93150.thaumaturge.api.aspect.TTAspects;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Runic Stone (ItemWeatherStone): hold Right-click for 60 ticks while it rains; on the last tick, for 25 vis of aer,
 * terra and aqua (x weatherStoneVisMult), the rain stops for 24000..1000000 ticks.
 */
public class WeatherStoneItem extends RelicItem {
    public static final int USE_TICKS = 60;

    public WeatherStoneItem(Properties properties) {
        super(properties, 0);
    }

    /** Original {@code VisCost}: 2500 centivis x weatherStoneVisMult. */
    public static int visCost() {
        return (int) (2500 * FRConfig.weatherStoneVisMult.getAsDouble());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isRaining()) {
            return InteractionResult.PASS;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (remaining == 1 && level instanceof ServerLevel server && entity instanceof Player player && level.isRaining()) {
            dispel(server, player);
        }
    }

    /** @return whether the rain was dispelled (vis paid) */
    public static boolean dispel(ServerLevel level, Player player) {
        int cost = visCost();
        if (!VisHelper.consumeFromInventory(player, VisHelper.cost(TTAspects.AER, cost, TTAspects.TERRA, cost, TTAspects.AQUA, cost))) {
            return false;
        }
        Vec3 c = player.getBoundingBox().getCenter();
        for (int i = 0; i <= 24; i++) {
            float g = 0.3F + (float) Math.random() * 0.5F;
            float b = 0.8F + (float) Math.random() * 0.2F;
            float s = 0.2F + (float) Math.random() * 0.2F;
            float m = 0.15F;
            BotaniaBridge.wisp(level, c.x, c.y, c.z, 0.0F, g, b, s, ((float) Math.random() - 0.5F) * m,
                    ((float) Math.random() - 0.5F) * m, ((float) Math.random() - 0.5F) * m, 1.0F);
        }
        level.playSound(null, c.x, c.y, c.z, TTSounds.altarCraft(), SoundSource.PLAYERS, 1.0F, (float) (0.8F + Math.random() * 0.2F));
        // original: setRaining(false) + setRainTime(24000..1000000), i.e. no rain until that time runs out
        level.getServer().setWeatherParameters(0, 24000 + (int) (Math.random() * 976000), false, false);
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
        if (shift()) {
            lines.accept(lore("ItemWeatherStone1"));
            lines.accept(lore("FREmpty"));
            lines.accept(lore("ItemWeatherStone2_1") + " " + visCost() / 100 + " " + lore("ItemWeatherStone2_2"));
            lines.accept(lore("ItemWeatherStone3"));
        } else {
            lines.accept(lore("FRShiftTooltip"));
        }
    }
}
