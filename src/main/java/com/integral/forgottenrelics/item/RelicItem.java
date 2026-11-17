package com.integral.forgottenrelics.item;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.util.TooltipKeys;
import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import java.util.function.Consumer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * Base of every relic: a single-stack item that may carry TC4 "warping gear" warp (Thaumaturge counts it while the
 * item is held, worn as armour or in a curio slot), plus the original tooltip conventions.
 */
public class RelicItem extends Item implements IWarpingGear {
    private final int gearWarp;

    public RelicItem(Properties properties, int gearWarp) {
        super(properties);
        this.gearWarp = gearWarp;
    }

    public RelicItem(Properties properties) {
        this(properties, 0);
    }

    @Override
    public int warp(ItemStack stack, LivingEntity entity) {
        return gearWarp;
    }

    /**
     * Original {@code StatCollector.translateToLocal("item.<key>.lore")}: the translated lore line as a plain string,
     * so concatenations with numbers keep the colour codes of the original text.
     */
    public static String lore(String key) {
        return Language.getInstance().getOrDefault("lore." + ForgottenRelics.MODID + "." + key);
    }

    /** Tooltip lines of the original {@code addInformation}; {@link #shift()} is the original Shift check. */
    protected void tooltip(ItemStack stack, TooltipContext context, Consumer<String> lines) {
    }

    /**
     * The original tome tooltip: Shift shows the description lines, Ctrl the vis cost (header + "Aer x 1.6" lines for
     * every non-zero primal), otherwise the two hints; always ends with an empty line.
     *
     * @param description lore keys, or ready strings prefixed with {@code "="}
     * @param header      FRVisPerCast / FRVisPerSecond / FRVisPerTick
     * @param vis         aer, terra, ignis, aqua, ordo, perditio in vis (already scaled as displayed)
     */
    protected static void tomeTooltip(Consumer<String> lines, String[] description, String header, double[] vis) {
        if (shift()) {
            for (String d : description) {
                lines.accept(d.startsWith("=") ? d.substring(1) : lore(d));
            }
        } else if (control()) {
            lines.accept(lore(header));
            String[] names = {"FRAerCost", "FRTerraCost", "FRIgnisCost", "FRAquaCost", "FROrdoCost", "FRPerditioCost"};
            for (int i = 0; i < 6; i++) {
                if (vis[i] != 0) {
                    lines.accept(" " + lore(names[i]) + vis[i]);
                }
            }
        } else {
            lines.accept(lore("FRShiftTooltip"));
            lines.accept(lore("FRViscostTooltip"));
        }
        lines.accept(lore("FREmpty"));
    }

    /** {@code lore(k1) + " " + value + " " + lore(k2)}: the original's "deals X damage" lines. */
    protected static String loreValue(String k1, Object value, String k2) {
        return "=" + lore(k1) + " " + value + " " + lore(k2);
    }

    protected static boolean shift() {
        return TooltipKeys.shift();
    }

    protected static boolean control() {
        return TooltipKeys.control();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        tooltip(stack, context, line -> builder.accept(Component.literal(line)));
    }
}
