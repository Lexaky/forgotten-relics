package com.integral.forgottenrelics.gametest;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.registry.FRCreativeTabs;
import com.integral.forgottenrelics.registry.FRDamageTypes;
import com.integral.forgottenrelics.registry.FRItems;
import com.integral.forgottenrelics.registry.FRSounds;
import com.leclowndu93150.thaumaturge.api.items.IWarpingGear;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/**
 * Headless checks run by {@code gradlew runGameTestServer}. Each milestone adds its tests here or in a sibling class
 * and names them in PROGRESS.md.
 */
public final class FRGameTests {
    /** Empty test structure shipped by Thaumaturge (referenced by id, not copied). */
    private static final Identifier STRUCTURE = Identifier.fromNamespaceAndPath("thaumaturge", "empty_5x5x7");
    /** Room for a full infusion altar (tools/gen_test_structures.py). */
    private static final Identifier STRUCTURE_BIG = ForgottenRelics.id("empty_17x8x17");

    /** Every relic id of the original, in registration order. */
    static final List<String> ITEM_IDS = List.of("false_justice", "deific_amulet", "paradox", "oblivion_amulet",
            "nebulous_core", "nebulous_core_dormant", "fate_tome", "dark_sun_ring", "chaos_core", "mining_charm",
            "advanced_mining_charm", "telekinesis_tome", "ancient_aegis", "missile_tome", "crimson_spell", "ghastly_skull",
            "obelisk_drainer", "eldritch_spell", "lunar_flares", "teleportation_tome", "shiny_stone", "superposition_ring",
            "soul_tome", "dimensional_mirror", "weather_stone", "apotheosis", "chaos_tome", "omega_core", "xp_tome");

    /** Original {@code IWarpingGear#getWarp} values; items not listed carry no warp. */
    static final Map<String, Integer> GEAR_WARP = Map.ofEntries(Map.entry("apotheosis", 5), Map.entry("chaos_core", 2),
            Map.entry("chaos_tome", 4), Map.entry("crimson_spell", 3), Map.entry("eldritch_spell", 4),
            Map.entry("false_justice", 4), Map.entry("fate_tome", 7), Map.entry("ghastly_skull", 3),
            Map.entry("lunar_flares", 3), Map.entry("missile_tome", 5), Map.entry("obelisk_drainer", 4),
            Map.entry("oblivion_amulet", 4), Map.entry("paradox", 8), Map.entry("soul_tome", 3),
            Map.entry("telekinesis_tome", 4), Map.entry("teleportation_tome", 2));

    private FRGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(ForgottenRelics.id("default"),
                new TestEnvironmentDefinition.AllOf(List.of()));
        Registrar r = new Registrar(event, env);
        registerItemTests(r);
        FRDataTests.register(r);
        FRSystemsTests.register(r);
        FRBaubleTests.register(r);
        FRPassiveTests.register(r);
        FRUtilityTests.register(r);
        FRTomeTests.register(r);
        FRProgressionTests.register(r);
    }

    private static void registerItemTests(Registrar r) {
        r.add("items/registered", helper -> {
            check(helper, FRItems.ITEMS.getEntries().size() == ITEM_IDS.size(),
                    "expected " + ITEM_IDS.size() + " items, registered " + FRItems.ITEMS.getEntries().size());
            for (String id : ITEM_IDS) {
                Item item = BuiltInRegistries.ITEM.getValue(ForgottenRelics.id(id));
                check(helper, item != null && BuiltInRegistries.ITEM.getKey(item).equals(ForgottenRelics.id(id)),
                        "item " + id + " missing");
                ItemStack stack = new ItemStack(item);
                check(helper, stack.getMaxStackSize() == 1, id + " stacks to " + stack.getMaxStackSize());
                Rarity expected = id.equals("mining_charm") ? Rarity.UNCOMMON : Rarity.EPIC;
                check(helper, stack.getRarity() == expected, id + " rarity " + stack.getRarity());
            }
            helper.succeed();
        });

        r.add("items/gear_warp", helper -> {
            for (String id : ITEM_IDS) {
                Item item = BuiltInRegistries.ITEM.getValue(ForgottenRelics.id(id));
                int expected = GEAR_WARP.getOrDefault(id, 0);
                int actual = item instanceof IWarpingGear gear ? gear.warp(new ItemStack(item), null) : 0;
                check(helper, actual == expected, id + " warp " + actual + ", expected " + expected);
            }
            helper.succeed();
        });

        r.add("items/paradox_sword", helper -> {
            ItemStack paradox = new ItemStack(FRItems.PARADOX.get());
            check(helper, paradox.getMaxDamage() == 3000, "paradox durability " + paradox.getMaxDamage());
            check(helper, paradox.is(net.minecraft.tags.ItemTags.SWORDS) || paradox.has(net.minecraft.core.component.DataComponents.WEAPON),
                    "paradox is not a weapon");
            helper.succeed();
        });

        r.add("items/creative_tab", helper -> {
            var tab = FRCreativeTabs.MAIN.get();
            check(helper, tab.getIconItem().is(FRItems.APOTHEOSIS.get()), "tab icon is " + tab.getIconItem());
            helper.succeed();
        });

        r.add("items/curio_slots", helper -> {
            checkTag(helper, "ring", List.of("superposition_ring", "mining_charm", "advanced_mining_charm", "dark_sun_ring"));
            checkTag(helper, "necklace", List.of("deific_amulet", "shiny_stone", "oblivion_amulet", "nebulous_core", "nebulous_core_dormant"));
            checkTag(helper, "belt", List.of("ancient_aegis"));
            helper.succeed();
        });

        r.add("data/damage_types", helper -> {
            var lookup = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
            for (ResourceKey<DamageType> key : List.of(FRDamageTypes.TRUE_DAMAGE, FRDamageTypes.TRUE_DAMAGE_UNDEFINED,
                    FRDamageTypes.SOUL_DRAIN, FRDamageTypes.OBLIVION, FRDamageTypes.FATE, FRDamageTypes.TELEKINETIC_LIGHTNING,
                    FRDamageTypes.DARK_MATTER)) {
                check(helper, lookup.get(key).isPresent(), "damage type " + key.identifier() + " missing");
            }
            Holder<DamageType> trueDamage = lookup.getOrThrow(FRDamageTypes.TRUE_DAMAGE);
            check(helper, trueDamage.is(DamageTypeTags.BYPASSES_ARMOR) && trueDamage.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                    && trueDamage.is(DamageTypeTags.BYPASSES_RESISTANCE) && trueDamage.is(Tags.DamageTypes.IS_MAGIC),
                    "true_damage bypass tags incomplete");
            Holder<DamageType> oblivion = lookup.getOrThrow(FRDamageTypes.OBLIVION);
            check(helper, !oblivion.is(DamageTypeTags.BYPASSES_ARMOR) && oblivion.is(Tags.DamageTypes.IS_MAGIC),
                    "oblivion must be magic only");
            Holder<DamageType> darkMatter = lookup.getOrThrow(FRDamageTypes.DARK_MATTER);
            check(helper, darkMatter.is(DamageTypeTags.BYPASSES_ARMOR) && !darkMatter.is(DamageTypeTags.BYPASSES_INVULNERABILITY),
                    "dark_matter tags wrong");
            helper.succeed();
        });

        r.add("data/sounds", helper -> {
            for (var sound : List.of(FRSounds.DISCHARGE, FRSounds.LUNAR_FLARE, FRSounds.STARFALL)) {
                check(helper, BuiltInRegistries.SOUND_EVENT.containsKey(sound.getId()), "sound " + sound.getId() + " missing");
            }
            helper.succeed();
        });
    }

    private static void checkTag(GameTestHelper helper, String slot, List<String> ids) {
        TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("curios", slot));
        for (String id : ids) {
            check(helper, new ItemStack(BuiltInRegistries.ITEM.getValue(ForgottenRelics.id(id))).is(tag),
                    id + " is not in #curios:" + slot);
        }
    }

    static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            throw helper.assertionException(Component.literal(message));
        }
    }

    private static Holder<TestEnvironmentDefinition<?>> bigEnvironment;

    record Registrar(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> env) {
        void add(String name, Consumer<GameTestHelper> body) {
            add(name, 40, body);
        }

        void add(String name, int maxTicks, Consumer<GameTestHelper> body) {
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, STRUCTURE, maxTicks, 0, true, Rotation.NONE);
            event.registerTest(ForgottenRelics.id(name.replace('/', '_')), new InlineTest(data, body));
        }

        /** A test in the 17×8×17 structure (infusion altars), batched with the other big ones. */
        void addBig(String name, int maxTicks, Consumer<GameTestHelper> body) {
            if (bigEnvironment == null) {
                bigEnvironment = event.registerEnvironment(ForgottenRelics.id("altar"), new TestEnvironmentDefinition.AllOf(List.of()));
            }
            Holder<TestEnvironmentDefinition<?>> big = bigEnvironment;
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(big, STRUCTURE_BIG, maxTicks, 0, true, Rotation.NONE);
            event.registerTest(ForgottenRelics.id(name.replace('/', '_')), new InlineTest(data, body));
        }

        /** Runs in its own batch, away from entities other tests place. */
        void addIsolated(String name, int maxTicks, Consumer<GameTestHelper> body) {
            Holder<TestEnvironmentDefinition<?>> alone = event.registerEnvironment(
                    ForgottenRelics.id("isolated_" + name.replace('/', '_')), new TestEnvironmentDefinition.AllOf(List.of()));
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(alone, STRUCTURE, maxTicks, 0, true, Rotation.NONE);
            event.registerTest(ForgottenRelics.id(name.replace('/', '_')), new InlineTest(data, body));
        }
    }

    private static final class InlineTest extends GameTestInstance {
        private static final MapCodec<InlineTest> CODEC = MapCodec.unit((InlineTest) null);
        private final Consumer<GameTestHelper> body;

        InlineTest(TestData<Holder<TestEnvironmentDefinition<?>>> data, Consumer<GameTestHelper> body) {
            super(data);
            this.body = body;
        }

        @Override
        public void run(GameTestHelper helper) {
            body.accept(helper);
        }

        @Override
        public MapCodec<? extends GameTestInstance> codec() {
            return CODEC;
        }

        @Override
        protected MutableComponent typeDescription() {
            return Component.literal("forgottenrelics:inline");
        }
    }
}
