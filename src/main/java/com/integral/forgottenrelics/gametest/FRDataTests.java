package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.registry.FRItems;
import com.leclowndu93150.thaumaturge.api.aspect.AspectIndexAccess;
import com.leclowndu93150.thaumaturge.api.aspect.AspectList;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** M3: Thaumaturge data (research, infusion recipes, item aspects) loads through Thaumaturge's codecs. */
final class FRDataTests {
    static final List<String> RESEARCH = List.of("generic_theory", "superposition_ring", "weather_stone", "mining_charm",
            "advanced_mining_charm", "dimensional_mirror", "ancient_aegis", "xp_tome", "spellbook_theory", "discord_tome",
            "fate_tome", "telekinesis_tome", "obelisk_drainer", "eldritch_spell", "crimson_spell", "chaos_tome",
            "nuclear_fury", "soul_tome", "lunar_flares", "apotheosis", "chaos_core", "the_paradox", "dark_sun_ring",
            "deific_amulet", "shiny_stone", "oblivion_amulet", "nebulous_core", "false_justice");

    /** Every relic with an infusion recipe in the original (all but the dormant core, Omega Core and Ghastly Skull). */
    static final List<String> RECIPES = List.of("superposition_ring", "weather_stone", "mining_charm",
            "dimensional_mirror", "advanced_mining_charm", "ancient_aegis", "dark_sun_ring", "deific_amulet", "chaos_core",
            "paradox", "shiny_stone", "oblivion_amulet", "xp_tome", "nebulous_core", "teleportation_tome",
            "obelisk_drainer", "telekinesis_tome", "soul_tome", "fate_tome", "eldritch_spell", "chaos_tome", "missile_tome",
            "crimson_spell", "lunar_flares", "apotheosis", "false_justice");

    private FRDataTests() {
    }

    static void register(FRGameTests.Registrar r) {
        r.add("data/research_loaded", helper -> {
            var entries = helper.getLevel().registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY);
            for (String id : RESEARCH) {
                check(helper, entries.get(ResourceKey.create(IResearchEntry.REGISTRY_KEY, ForgottenRelics.id(id))).isPresent(),
                        "research forgottenrelics:" + id + " missing");
            }
            long ours = entries.listElementIds().filter(k -> k.identifier().getNamespace().equals(ForgottenRelics.MODID)).count();
            check(helper, ours == RESEARCH.size(), "expected " + RESEARCH.size() + " research entries, found " + ours);
            var categories = helper.getLevel().registryAccess().lookupOrThrow(IResearchCategory.REGISTRY_KEY);
            check(helper, categories.get(ResourceKey.create(IResearchCategory.REGISTRY_KEY, ForgottenRelics.id(ForgottenRelics.MODID))).isPresent(),
                    "research category forgottenrelics:forgottenrelics missing");
            helper.succeed();
        });

        r.add("data/recipes_loaded", helper -> {
            var recipes = helper.getLevel().getServer().getRecipeManager();
            for (String id : RECIPES) {
                var holder = recipes.byKey(ResourceKey.create(Registries.RECIPE, ForgottenRelics.id("infusion/" + id)));
                check(helper, holder.isPresent(), "recipe forgottenrelics:infusion/" + id + " missing");
                check(helper, holder.get().value().getType().toString().contains("infusion"),
                        id + " is not an infusion recipe: " + holder.get().value().getType());
            }
            helper.succeed();
        });

        r.add("data/item_aspects", helper -> {
            var access = helper.getLevel().registryAccess();
            // Runic (Weather) Stone: TC4 ENERGY 12, WEATHER 10, EXCHANGE 8, AIR 7, WATER 7, MAGIC 5, x2 with WEATHER -> aer
            AspectList stone = AspectIndexAccess.of(FRItems.WEATHER_STONE.get());
            check(helper, stone.amountOf(aspect("aer"), access) == 34 && stone.amountOf(aspect("potentia"), access) == 24
                            && stone.amountOf(aspect("aqua"), access) == 14 && stone.amountOf(aspect("praecantatio"), access) == 10,
                    "weather stone aspects: " + stone);
            // The ring had no aspects in the original: Thaumaturge derives them from its infusion recipe.
            AspectList ring = AspectIndexAccess.of(FRItems.SUPERPOSITION_RING.get());
            check(helper, !ring.isEmpty(), "superposition ring has no derived aspects");
            helper.succeed();
        });
    }

    private static ResourceKey<IAspect> aspect(String tag) {
        return ResourceKey.create(IAspect.REGISTRY_KEY, Identifier.fromNamespaceAndPath("thaumaturge", tag));
    }
}
