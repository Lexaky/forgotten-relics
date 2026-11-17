package com.integral.forgottenrelics.gametest;

import static com.integral.forgottenrelics.gametest.FRGameTests.check;

import com.integral.forgottenrelics.ForgottenRelics;
import com.integral.forgottenrelics.compat.TTTestBridge;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanTarget;
import com.leclowndu93150.thaumaturge.api.research.scan.ScanningManager;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

/**
 * M3 in play: every infusion recipe crafted on a real Thaumaturge altar (matrix ticking, essentia pulled from jars,
 * items consumed), and the whole research tree walked the way a player does it (parents, knowledge per stage, scans).
 */
final class FRProgressionTests {
    static final List<String> RECIPES = List.of("advanced_mining_charm", "ancient_aegis", "apotheosis", "chaos_core", "chaos_tome",
            "crimson_spell", "dark_sun_ring", "deific_amulet", "dimensional_mirror", "eldritch_spell", "false_justice", "fate_tome",
            "lunar_flares", "mining_charm", "missile_tome", "nebulous_core", "obelisk_drainer", "oblivion_amulet", "paradox",
            "shiny_stone", "soul_tome", "superposition_ring", "telekinesis_tome", "teleportation_tome", "weather_stone", "xp_tome");
    private static final ResourceKey<IResearchCategory> CATEGORY = ResourceKey.create(IResearchCategory.REGISTRY_KEY, ForgottenRelics.id(ForgottenRelics.MODID));

    private FRProgressionTests() {
    }

    private static ServerPlayer player(GameTestHelper helper, String name) {
        UUID id = UUID.nameUUIDFromBytes(("forgottenrelics:" + name).getBytes());
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(id, name.length() > 16 ? name.substring(0, 16) : name));
    }

    private static List<Identifier> ourEntries(ServerLevel level) {
        List<Identifier> ids = new ArrayList<>();
        level.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY).listElementIds()
                .filter(k -> k.identifier().getNamespace().equals(ForgottenRelics.MODID)).forEach(k -> ids.add(k.identifier()));
        return ids;
    }

    static void register(FRGameTests.Registrar r) {
        for (String name : RECIPES) {
            // 6000 ticks: the largest recipe (Tome of Primal Chaos) needs about 1000 essentia
            r.addBig("altar/infusion_" + name, 6000, helper -> infusion(helper, name));
        }
        r.add("research/progression_and_scans", 40, FRProgressionTests::research);
        r.addIsolated("passive/fate_tome_catastrophe", 40, FRProgressionTests::catastrophe);
    }

    /** Explosions seen while {@link #recording}; they are recorded and cancelled so the test world stays intact. */
    private static final List<float[]> EXPLOSIONS = new ArrayList<>();
    private static boolean recording;
    private static boolean listening;

    private static void onExplosion(net.neoforged.neoforge.event.level.ExplosionEvent.Start event) {
        if (recording) {
            var e = event.getExplosion();
            EXPLOSIONS.add(new float[]{e.radius(), (float) e.center().x, (float) e.center().y, (float) e.center().z});
            event.setCanceled(true);
        }
    }

    /**
     * Two tomes in the inventory and the 1-in-62500 roll lost: every tome is gone, every living entity within 64 blocks
     * (the holder included) takes 40000 fate damage and a power-16 explosion, then a power-100 explosion at the holder.
     */
    private static void catastrophe(GameTestHelper helper) {
        if (!listening) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(FRProgressionTests::onExplosion);
            listening = true;
        }
        ServerPlayer player = player(helper, "fr_fate");
        player.setPos(helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(com.integral.forgottenrelics.registry.FRItems.FATE_TOME.get()));
        player.getInventory().add(new ItemStack(com.integral.forgottenrelics.registry.FRItems.FATE_TOME.get()));
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new BlockPos(4, 2, 4));
        EXPLOSIONS.clear();
        recording = true;
        try {
            com.integral.forgottenrelics.item.passive.FateTomeItem.insanelyDisastrousConsequences(player);
        } finally {
            recording = false;
        }
        check(helper, com.integral.forgottenrelics.item.passive.FateTomeItem.countTomes(player) == 0, "tomes left after the catastrophe");
        check(helper, zombie.isDeadOrDying(), "zombie survived 40000 fate damage");
        long small = EXPLOSIONS.stream().filter(x -> x[0] == 16F).count();
        long big = EXPLOSIONS.stream().filter(x -> x[0] == 100F && Math.abs(x[1] - player.getX()) < 2 && Math.abs(x[3] - player.getZ()) < 2).count();
        // the holder is a FakePlayer, which is not in the level's entity list, so only the zombie gets its own blast
        check(helper, small == 1, "power-16 explosions: " + small + " (expected one, at the zombie)");
        check(helper, big == 1, "power-100 explosions at the holder: " + big + "; seen "
                + EXPLOSIONS.stream().map(java.util.Arrays::toString).toList() + ", holder at " + player.position());
        helper.succeed();
    }

    private static void infusion(GameTestHelper helper, String name) {
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(8, 1, 8));
        var recipe = TTTestBridge.recipe(level, ForgottenRelics.id("infusion/" + name)).orElse(null);
        check(helper, recipe != null, "recipe forgottenrelics:infusion/" + name + " not loaded");
        TTTestBridge.buildAltar(level, center, TTTestBridge.componentCount(recipe));
        TTTestBridge.stage(level, center, recipe);
        ServerPlayer player = player(helper, "fr_altar");
        for (Identifier entry : ourEntries(level)) {
            TTTestBridge.completeResearch(player, entry);
        }
        TTTestBridge.completeResearch(player, Identifier.fromNamespaceAndPath("thaumaturge", "unlock_infusion"));
        check(helper, TTTestBridge.activate(level, center, player), "matrix did not accept the altar for " + name);
        ItemStack expected = TTTestBridge.result(recipe);
        // a player waits for the matrix to settle before starting; stability regenerates up to 25 while idle
        helper.runAfterDelay(300, () -> check(helper, TTTestBridge.startCraft(level, center, player),
                "matrix refused to start " + name + " (stability " + TTTestBridge.stability(level, center) + ")"));
        helper.succeedWhen(() -> {
            ItemStack now = TTTestBridge.centerItem(level, center);
            check(helper, now.is(expected.getItem()) && !TTTestBridge.crafting(level, center),
                    name + ": centre holds " + now + "; " + TTTestBridge.status(level, center));
        });
    }

    /**
     * From an empty research state: entries open only when their parents are done; each stage asks for its knowledge
     * (and spends it); hidden entries open only after their scan; at the end all entries are complete.
     */
    private static void research(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = player(helper, "fr_research");
        List<Identifier> entries = ourEntries(level);
        for (Identifier e : entries) {
            TTTestBridge.forget(player, e);
        }
        var lookup = level.registryAccess().lookupOrThrow(IResearchEntry.REGISTRY_KEY);
        Holder<IResearchCategory> category = level.registryAccess().lookupOrThrow(IResearchCategory.REGISTRY_KEY).getOrThrow(CATEGORY);
        // Thaumaturge's own parents of the category (infusion, metallurgy, elemental tools, ...) count as done
        for (Identifier id : entries) {
            for (var parent : lookup.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, id)).value().parents()) {
                Identifier p = parent.id();
                if (p.getNamespace().equals("thaumaturge")) {
                    TTTestBridge.completeResearch(player, p);
                }
            }
        }
        IResearchEntry mining = lookup.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, ForgottenRelics.id("mining_charm"))).value();
        IResearchEntry generic = lookup.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, ForgottenRelics.id("generic_theory"))).value();
        check(helper, TTTestBridge.parentsSatisfied(player, generic), "generic_theory closed with Thaumaturge parents done");
        check(helper, !TTTestBridge.parentsSatisfied(player, mining), "mining_charm open before generic_theory");
        TTTestBridge.completeResearch(player, ForgottenRelics.id("generic_theory"));
        check(helper, TTTestBridge.parentsSatisfied(player, mining), "mining_charm still closed after generic_theory");

        // stage 0 of mining_charm asks for 1 observation
        Identifier miningId = ForgottenRelics.id("mining_charm");
        check(helper, TTTestBridge.unlock(player, miningId) || TTTestBridge.known(player, miningId), "mining_charm could not be unlocked");
        // observation (difficulty < 2): paid from the aspect pool with the entry's note aspects
        check(helper, !TTTestBridge.stageMet(player, mining, miningId, 0), "stage 0 met with an empty aspect pool");
        TTTestBridge.fillAspectPool(player, 50);
        check(helper, TTTestBridge.stageMet(player, mining, miningId, 0), "stage 0 not met with a full aspect pool");
        int pool = TTTestBridge.aspectPoolTotal(player);
        check(helper, TTTestBridge.advance(player, miningId), "mining_charm did not advance");
        check(helper, TTTestBridge.aspectPoolTotal(player) < pool, "advancing mining_charm did not spend the aspect pool");

        // theory (difficulty >= 2): needs the research-table notes solved
        Identifier xpId = ForgottenRelics.id("xp_tome");
        IResearchEntry xp = lookup.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, xpId)).value();
        boolean xpTheory = xp.stages().get(0).requiredKnowledge().stream().anyMatch(k -> k.type() == com.leclowndu93150.thaumaturge.api.capability.KnowledgeType.THEORY);
        check(helper, xpTheory, "xp_tome should need theory (difficulty 2+)");
        check(helper, !TTTestBridge.stageMet(player, xp, xpId, 0), "xp_tome stage 0 met without solved notes");
        TTTestBridge.solveNotes(player, xpId, 4);
        check(helper, TTTestBridge.stageMet(player, xp, xpId, 0), "xp_tome stage 0 not met with solved notes");

        // the scanned parents: closed until the trigger item is scanned
        Identifier scannedKey = ForgottenRelics.id("scanned/advanced_mining_charm");
        check(helper, !TTTestBridge.known(player, scannedKey), "scan key known before scanning");
        ScanningManager.scan(player, ScanTarget.stack(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("botania", "life_essence")))));
        check(helper, TTTestBridge.known(player, scannedKey), "scanning botania:life_essence did not grant " + scannedKey);

        // walk the tree: repeatedly open what is open, pay the knowledge each stage asks for, advance to the end
        int rounds = 0;
        boolean scanned = false;
        List<Identifier> left = new ArrayList<>(entries);
        while (!left.isEmpty() && rounds++ < 40) {
            for (var it = left.iterator(); it.hasNext(); ) {
                Identifier id = it.next();
                IResearchEntry entry = lookup.getOrThrow(ResourceKey.create(IResearchEntry.REGISTRY_KEY, id)).value();
                if (!TTTestBridge.parentsSatisfied(player, entry)) {
                    continue;
                }
                TTTestBridge.unlock(player, id);
                for (int guard = 0; guard < 4 && !TTTestBridge.complete(player, id); guard++) {
                    int stage = Math.max(0, TTTestBridge.stage(player, id));
                    if (!TTTestBridge.stageMet(player, entry, id, stage)) {
                        TTTestBridge.fillAspectPool(player, 50);
                        TTTestBridge.solveNotes(player, id, 8);
                    }
                    check(helper, TTTestBridge.stageMet(player, entry, id, stage), id + " stage " + stage + " not met with knowledge");
                    check(helper, TTTestBridge.advance(player, id), id + " stage " + stage + " did not advance");
                }
                check(helper, TTTestBridge.complete(player, id), id + " not complete after advancing");
                it.remove();
            }
            // hidden entries open only through their scans
            if (!left.isEmpty() && !scanned) {
                for (String[] s : SCANS) {
                    // advanced_mining_charm's item was scanned above already
                    check(helper, s[0].equals("advanced_mining_charm") || left.contains(ForgottenRelics.id(s[0])), s[0] + " opened without its scan");
                }
                scanned = true;
                for (String[] s : SCANS) {
                    ScanningManager.scan(player, ScanTarget.stack(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(s[1])))));
                }
            }
        }
        check(helper, left.isEmpty(), "never opened: " + left);
        helper.succeed();
    }

    /** Scan triggers of the original (entry, item), as generated into data/forgottenrelics/thaumaturge/scan. */
    static final String[][] SCANS = {{"advanced_mining_charm", "botania:life_essence"}, {"ancient_aegis", "botania:dragonstone"}, {"crimson_spell", "thaumaturge:curio_rites"}, {"discord_tome", "botania:dragonstone"}, {"nebulous_core", "forgottenrelics:superposition_ring"}, {"oblivion_amulet", "minecraft:nether_star"}, {"shiny_stone", "minecraft:enchanted_golden_apple"}, {"soul_tome", "thaumaturge:pech_wand"}};
}
