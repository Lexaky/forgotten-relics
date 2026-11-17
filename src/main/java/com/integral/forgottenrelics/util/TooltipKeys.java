package com.integral.forgottenrelics.util;

import java.util.function.BooleanSupplier;

/**
 * Shift/Ctrl state for tooltips ({@code GuiScreen.isShiftKeyDown()} in 1.7). Common code must not touch client
 * classes, so the client replaces these suppliers at setup; on a dedicated server both stay false.
 */
public final class TooltipKeys {
    public static volatile BooleanSupplier shift = () -> false;
    public static volatile BooleanSupplier control = () -> false;

    private TooltipKeys() {
    }

    public static boolean shift() {
        return shift.getAsBoolean();
    }

    public static boolean control() {
        return control.getAsBoolean();
    }
}
