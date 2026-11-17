package com.integral.forgottenrelics.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

/** Tool materials. */
public final class FRMaterials {
    /**
     * Original {@code PARADOXICALSTUFF}: harvest level 4, 3000 uses, efficiency 16, damage -4, enchantability 100.
     * Level 4 is one step above diamond, so netherite's incorrect-blocks tag is the closest match.
     */
    public static final ToolMaterial PARADOX = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3000, 16.0F,
            -4.0F, 100, FRTags.PARADOX_REPAIR_MATERIALS);

    private FRMaterials() {
    }
}
