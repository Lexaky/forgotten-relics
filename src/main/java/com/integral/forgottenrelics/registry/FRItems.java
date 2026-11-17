package com.integral.forgottenrelics.registry;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.FRConfig;
import com.integral.forgottenrelics.item.RelicItem;
import com.integral.forgottenrelics.item.bauble.AncientAegisItem;
import com.integral.forgottenrelics.item.passive.ChaosCoreItem;
import com.integral.forgottenrelics.item.tome.ApotheosisItem;
import com.integral.forgottenrelics.item.tome.ChaosTomeItem;
import com.integral.forgottenrelics.item.tome.CrimsonSpellItem;
import com.integral.forgottenrelics.item.tome.EldritchSpellItem;
import com.integral.forgottenrelics.item.tome.LunarFlaresItem;
import com.integral.forgottenrelics.item.tome.MissileTomeItem;
import com.integral.forgottenrelics.item.tome.SoulTomeItem;
import com.integral.forgottenrelics.item.tome.TelekinesisTomeItem;
import com.integral.forgottenrelics.item.utility.DimensionalMirrorItem;
import com.integral.forgottenrelics.item.utility.GhastlySkullItem;
import com.integral.forgottenrelics.item.utility.ObeliskDrainerItem;
import com.integral.forgottenrelics.item.utility.ParadoxItem;
import com.integral.forgottenrelics.item.utility.TeleportationTomeItem;
import com.integral.forgottenrelics.item.utility.WeatherStoneItem;
import com.integral.forgottenrelics.item.passive.FalseJusticeItem;
import com.integral.forgottenrelics.item.passive.FateTomeItem;
import com.integral.forgottenrelics.item.passive.OmegaCoreItem;
import com.integral.forgottenrelics.item.passive.XpTomeItem;
import com.integral.forgottenrelics.item.bauble.DarkSunRingItem;
import com.integral.forgottenrelics.item.bauble.DeificAmuletItem;
import com.integral.forgottenrelics.item.bauble.DormantNebulousCoreItem;
import com.integral.forgottenrelics.item.bauble.MiningCharmItem;
import com.integral.forgottenrelics.item.bauble.NebulousCoreItem;
import com.integral.forgottenrelics.item.bauble.OblivionAmuletItem;
import com.integral.forgottenrelics.item.bauble.ShinyStoneItem;
import com.integral.forgottenrelics.item.bauble.SuperpositionRingItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * All 29 relics, in the original registration order (that order is also the creative tab order). Each starts as a
 * plain {@link RelicItem} with the original rarity and gear warp; its behaviour class replaces the factory in the
 * milestone that ports it (PORTING_PLAN.md, M5-M8). Ids and textures are mirrored in tools/gen_assets.py.
 */
public final class FRItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ForgottenRelics.MODID);

    public static final DeferredItem<Item> FALSE_JUSTICE = ITEMS.registerItem("false_justice", props -> new FalseJusticeItem(epic(props)));
    public static final DeferredItem<Item> DEIFIC_AMULET = ITEMS.registerItem("deific_amulet",
            props -> new DeificAmuletItem(epic(props)));
    public static final DeferredItem<Item> PARADOX = ITEMS.registerItem("paradox",
            props -> new ParadoxItem(props.sword(FRMaterials.PARADOX, 4.0F, -2.4F).rarity(Rarity.EPIC)));
    public static final DeferredItem<Item> OBLIVION_AMULET = ITEMS.registerItem("oblivion_amulet",
            props -> new OblivionAmuletItem(epic(props)));
    public static final DeferredItem<Item> NEBULOUS_CORE = ITEMS.registerItem("nebulous_core",
            props -> new NebulousCoreItem(epic(props)));
    public static final DeferredItem<Item> NEBULOUS_CORE_DORMANT = ITEMS.registerItem("nebulous_core_dormant",
            props -> new DormantNebulousCoreItem(epic(props)));
    public static final DeferredItem<Item> FATE_TOME = ITEMS.registerItem("fate_tome", props -> new FateTomeItem(epic(props)));
    public static final DeferredItem<Item> DARK_SUN_RING = ITEMS.registerItem("dark_sun_ring",
            props -> new DarkSunRingItem(epic(props)));
    public static final DeferredItem<Item> CHAOS_CORE = ITEMS.registerItem("chaos_core", props -> new ChaosCoreItem(epic(props)));
    public static final DeferredItem<Item> MINING_CHARM = ITEMS.registerItem("mining_charm",
            props -> new MiningCharmItem(props.stacksTo(1).rarity(Rarity.UNCOMMON), FRConfig.miningCharmBoost, FRConfig.miningCharmReach, "ItemMiningCharm"));
    public static final DeferredItem<Item> ADVANCED_MINING_CHARM = ITEMS.registerItem("advanced_mining_charm",
            props -> new MiningCharmItem(epic(props), FRConfig.advancedMiningCharmBoost, FRConfig.advancedMiningCharmReach, "ItemAdvancedMiningCharm"));
    public static final DeferredItem<Item> TELEKINESIS_TOME = ITEMS.registerItem("telekinesis_tome", props -> new TelekinesisTomeItem(epic(props)));
    public static final DeferredItem<Item> ANCIENT_AEGIS = ITEMS.registerItem("ancient_aegis",
            props -> new AncientAegisItem(epic(props)));
    public static final DeferredItem<Item> MISSILE_TOME = ITEMS.registerItem("missile_tome", props -> new MissileTomeItem(epic(props)));
    public static final DeferredItem<Item> CRIMSON_SPELL = ITEMS.registerItem("crimson_spell", props -> new CrimsonSpellItem(epic(props)));
    public static final DeferredItem<Item> GHASTLY_SKULL = ITEMS.registerItem("ghastly_skull", props -> new GhastlySkullItem(epic(props)));
    public static final DeferredItem<Item> OBELISK_DRAINER = ITEMS.registerItem("obelisk_drainer", props -> new ObeliskDrainerItem(epic(props)));
    public static final DeferredItem<Item> ELDRITCH_SPELL = ITEMS.registerItem("eldritch_spell", props -> new EldritchSpellItem(epic(props)));
    public static final DeferredItem<Item> LUNAR_FLARES = ITEMS.registerItem("lunar_flares", props -> new LunarFlaresItem(epic(props)));
    public static final DeferredItem<Item> TELEPORTATION_TOME = ITEMS.registerItem("teleportation_tome", props -> new TeleportationTomeItem(epic(props)));
    public static final DeferredItem<Item> SHINY_STONE = ITEMS.registerItem("shiny_stone",
            props -> new ShinyStoneItem(epic(props)));
    public static final DeferredItem<Item> SUPERPOSITION_RING = ITEMS.registerItem("superposition_ring",
            props -> new SuperpositionRingItem(epic(props)));
    public static final DeferredItem<Item> SOUL_TOME = ITEMS.registerItem("soul_tome", props -> new SoulTomeItem(epic(props)));
    public static final DeferredItem<Item> DIMENSIONAL_MIRROR = ITEMS.registerItem("dimensional_mirror", props -> new DimensionalMirrorItem(epic(props)));
    public static final DeferredItem<Item> WEATHER_STONE = ITEMS.registerItem("weather_stone", props -> new WeatherStoneItem(epic(props)));
    public static final DeferredItem<Item> APOTHEOSIS = ITEMS.registerItem("apotheosis", props -> new ApotheosisItem(epic(props)));
    public static final DeferredItem<Item> CHAOS_TOME = ITEMS.registerItem("chaos_tome", props -> new ChaosTomeItem(epic(props)));
    public static final DeferredItem<Item> OMEGA_CORE = ITEMS.registerItem("omega_core", props -> new OmegaCoreItem(epic(props)));
    public static final DeferredItem<Item> XP_TOME = ITEMS.registerItem("xp_tome", props -> new XpTomeItem(epic(props)));

    /** Single-stack epic relic with the given gear warp (original {@code IWarpingGear#getWarp}). */
    private static DeferredItem<Item> relic(String id, int gearWarp) {
        return ITEMS.registerItem(id, props -> new RelicItem(epic(props), gearWarp));
    }

    private static Item.Properties epic(Item.Properties props) {
        return props.stacksTo(1).rarity(Rarity.EPIC);
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    private FRItems() {
    }
}
