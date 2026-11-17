package com.integral.forgottenrelics.compat;

import com.leclowndu93150.thaumaturge.api.aspect.AspectInstance;
import com.leclowndu93150.thaumaturge.api.capability.KnowledgeType;
import com.leclowndu93150.thaumaturge.api.research.IResearchCategory;
import com.leclowndu93150.thaumaturge.api.research.IResearchEntry;
import com.leclowndu93150.thaumaturge.content.essentia.jar.BlockEntityJarCreative;
import com.leclowndu93150.thaumaturge.content.infusion.BlockEntityInfusionMatrix;
import com.leclowndu93150.thaumaturge.content.infusion.BlockEntityPedestal;
import com.leclowndu93150.thaumaturge.content.infusion.InfusionRecipe;
import com.leclowndu93150.thaumaturge.content.research.ResearchManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Test support for GameTests and the client check: a real Thaumaturge infusion altar and research progression. Uses
 * Thaumaturge internals ({@code content}), so it lives in {@code compat}. Not used by gameplay code.
 */
public final class TTTestBridge {
    /** Component pedestals around the altar centre, in 180°-symmetric pairs (symmetry keeps the matrix stable). */
    private static final int[][] RING = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, -2}, {2, -2}, {-2, 2},
            {3, 1}, {-3, -1}, {-1, 3}, {1, -3}, {1, 3}, {-1, -3}, {3, -1}, {-3, 1}};

    private TTTestBridge() {
    }

    private static Block block(String id) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("thaumaturge", id));
    }

    /**
     * Thaumaturge's {@code /thaumaturge build infusion_altar} layout around {@code center} (the central pedestal):
     * matrix two blocks above, four arcane pillars on the corners, plus {@code pedestals} component pedestals,
     * stabilisers and a stone floor below.
     */
    public static void buildAltar(ServerLevel level, BlockPos center, int pedestals) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-8, -1, -8), center.offset(8, -1, 8))) {
            level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        }
        level.setBlockAndUpdate(center, block("pedestal_arcane").defaultBlockState());
        level.setBlockAndUpdate(center.above(2), block("infusion_matrix").defaultBlockState());
        pillar(level, center.offset(-1, 0, -1), Direction.NORTH);
        pillar(level, center.offset(-1, 0, 1), Direction.WEST);
        pillar(level, center.offset(1, 0, -1), Direction.EAST);
        pillar(level, center.offset(1, 0, 1), Direction.SOUTH);
        for (int i = 0; i < pedestals; i++) {
            level.setBlockAndUpdate(center.offset(RING[i][0], 0, RING[i][1]), block("pedestal_arcane").defaultBlockState());
        }
        stabilisers(level, center);
    }

    /**
     * Stabilisers in mirrored pairs on the squares 4 and 5 blocks out: Thaumaturge counts each distinct block identity
     * at full strength once and halves repeats (×0.75 per rank), so every pair is a different candle colour or head.
     */
    private static void stabilisers(ServerLevel level, BlockPos center) {
        List<BlockState> kinds = new ArrayList<>();
        for (String c : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
                "purple", "blue", "brown", "green", "red", "black")) {
            kinds.add(block("candle_" + c).defaultBlockState());
        }
        for (Block head : List.of(Blocks.SKELETON_SKULL, Blocks.WITHER_SKELETON_SKULL, Blocks.ZOMBIE_HEAD, Blocks.CREEPER_HEAD,
                Blocks.PIGLIN_HEAD, Blocks.DRAGON_HEAD, Blocks.PLAYER_HEAD)) {
            kinds.add(head.defaultBlockState());
        }
        List<int[]> half = new ArrayList<>();
        for (int r = 4; r <= 5; r++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.max(Math.abs(x), Math.abs(z)) == r && (x > 0 || (x == 0 && z > 0))) {
                        half.add(new int[]{x, z});
                    }
                }
            }
        }
        for (int i = 0; i < Math.min(kinds.size(), half.size()); i++) {
            int[] p = half.get(i);
            level.setBlock(center.offset(p[0], 0, p[1]), kinds.get(i), Block.UPDATE_CLIENTS);
            level.setBlock(center.offset(-p[0], 0, -p[1]), kinds.get(i), Block.UPDATE_CLIENTS);
        }
    }

    private static void pillar(ServerLevel level, BlockPos pos, Direction facing) {
        BlockState state = block("pillar_arcane").defaultBlockState();
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        level.setBlockAndUpdate(pos, state);
        level.removeBlock(pos.above(), false);
    }

    /** The infusion recipe with this id, or empty. */
    public static Optional<InfusionRecipe> recipe(ServerLevel level, Identifier id) {
        Optional<RecipeHolder<?>> holder = level.getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id));
        return holder.map(RecipeHolder::value).filter(InfusionRecipe.class::isInstance).map(InfusionRecipe.class::cast);
    }

    public static int componentCount(InfusionRecipe recipe) {
        return recipe.components().size();
    }

    public static ItemStack result(InfusionRecipe recipe) {
        return recipe.resultItem();
    }

    /** A stack that matches the ingredient (its first displayed stack, so component ingredients keep their data). */
    public static ItemStack example(ServerLevel level, Ingredient ingredient) {
        return ingredient.display().resolveForFirstStack(SlotDisplayContext.fromLevel(level));
    }

    /**
     * Puts the recipe on the altar: catalyst on the central pedestal, one component per ring pedestal, and one creative
     * jar per essentia aspect in a ring six blocks out. Returns the essentia aspects (for the report).
     */
    public static List<String> stage(ServerLevel level, BlockPos center, InfusionRecipe recipe) {
        ((BlockEntityPedestal) level.getBlockEntity(center)).setItem(example(level, recipe.catalyst()));
        List<Ingredient> components = recipe.components();
        for (int i = 0; i < components.size(); i++) {
            ((BlockEntityPedestal) level.getBlockEntity(center.offset(RING[i][0], 0, RING[i][1]))).setItem(example(level, components.get(i)));
        }
        List<String> aspects = new ArrayList<>();
        List<AspectInstance> entries = recipe.aspects().entries();
        for (int i = 0; i < entries.size(); i++) {
            double angle = Math.PI * 2 * i / entries.size();
            // set into the floor, out of the stabiliser rings
            BlockPos jar = center.offset((int) Math.round(Math.cos(angle) * 6), -1, (int) Math.round(Math.sin(angle) * 6));
            level.setBlockAndUpdate(jar, block("jar_creative").defaultBlockState());
            var key = entries.get(i).aspect().unwrapKey().orElseThrow();
            ((BlockEntityJarCreative) level.getBlockEntity(jar)).setStorageContents(key, 1000);
            aspects.add(key.identifier().getPath());
        }
        return aspects;
    }

    /** The first wand click: validates the altar and switches the matrix on (it then regenerates stability). */
    public static boolean activate(ServerLevel level, BlockPos center, ServerPlayer player) {
        BlockEntityInfusionMatrix matrix = (BlockEntityInfusionMatrix) level.getBlockEntity(center.above(2));
        matrix.onRightClick(level, player);
        return matrix.isActive();
    }

    public static float stability(ServerLevel level, BlockPos center) {
        return ((BlockEntityInfusionMatrix) level.getBlockEntity(center.above(2))).stability();
    }

    /** What the matrix's wand activation does: validate the altar and start the craft for this player. */
    public static boolean startCraft(ServerLevel level, BlockPos center, ServerPlayer player) {
        // the first wand click activates the matrix, the second starts the craft (BlockEntityInfusionMatrix.onRightClick)
        BlockEntityInfusionMatrix matrix = (BlockEntityInfusionMatrix) level.getBlockEntity(center.above(2));
        matrix.onRightClick(level, player);
        if (!matrix.isActive()) {
            return false;
        }
        matrix.onRightClick(level, player);
        return matrix.isCrafting();
    }

    public static boolean crafting(ServerLevel level, BlockPos center) {
        return level.getBlockEntity(center.above(2)) instanceof BlockEntityInfusionMatrix m && m.isCrafting();
    }

    /** Crafting state for failure messages: essentia still owed, stability, items left on the ring pedestals. */
    public static String status(ServerLevel level, BlockPos center) {
        if (!(level.getBlockEntity(center.above(2)) instanceof BlockEntityInfusionMatrix m)) {
            return "no matrix";
        }
        int items = 0;
        for (int[] r : RING) {
            if (level.getBlockEntity(center.offset(r[0], 0, r[1])) instanceof BlockEntityPedestal p && !p.getItem().isEmpty()) {
                items++;
            }
        }
        return "crafting " + m.isCrafting() + ", essentia left " + m.remainingEssentia().totalAmount() + ", stability " + m.stability()
                + ", component items left " + items;
    }

    public static ItemStack centerItem(ServerLevel level, BlockPos center) {
        return level.getBlockEntity(center) instanceof BlockEntityPedestal p ? p.getItem() : ItemStack.EMPTY;
    }

    // ---- research ---------------------------------------------------------------------------------------------

    public static void completeResearch(ServerPlayer player, Identifier entry) {
        ResearchManager.complete(player, entry);
    }

    public static void forget(ServerPlayer player, Identifier entry) {
        ResearchManager.of(player).removeResearch(entry);
    }

    public static boolean unlock(ServerPlayer player, Identifier entry) {
        return ResearchManager.unlock(player, entry);
    }

    public static boolean known(ServerPlayer player, Identifier entry) {
        return ResearchManager.of(player).isResearchKnown(entry);
    }

    public static boolean complete(ServerPlayer player, Identifier entry) {
        return ResearchManager.of(player).isResearchComplete(entry);
    }

    public static int stage(ServerPlayer player, Identifier entry) {
        return ResearchManager.of(player).researchStage(entry);
    }

    public static boolean parentsSatisfied(ServerPlayer player, IResearchEntry entry) {
        return ResearchManager.parentsSatisfied(ResearchManager.of(player), entry);
    }

    public static boolean stageMet(ServerPlayer player, IResearchEntry entry, Identifier id, int stage) {
        return ResearchManager.stageRequirementsMet(player, ResearchManager.of(player), entry, id, stage);
    }

    public static boolean advance(ServerPlayer player, Identifier entry) {
        return ResearchManager.advanceStage(player, entry);
    }

    public static void giveKnowledge(ServerPlayer player, net.minecraft.core.Holder<IResearchCategory> category, boolean theory, int amount) {
        ResearchManager.gainKnowledge(player, theory ? KnowledgeType.THEORY : KnowledgeType.OBSERVATION, category, amount);
    }

    /** Fills the player's aspect pool (what scanning gives) with {@code amount} of every aspect. */
    public static void fillAspectPool(ServerPlayer player, int amount) {
        player.level().registryAccess().lookupOrThrow(com.leclowndu93150.thaumaturge.api.aspect.IAspect.REGISTRY_KEY).listElements()
                .forEach(h -> com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess.grant(player, h, amount));
    }

    /** Total of the player's aspect pool. */
    public static int aspectPoolTotal(ServerPlayer player) {
        return player.level().registryAccess().lookupOrThrow(com.leclowndu93150.thaumaturge.api.aspect.IAspect.REGISTRY_KEY).listElements()
                .mapToInt(h -> com.leclowndu93150.thaumaturge.api.research.pool.AspectPoolAccess.amount(player, h)).sum();
    }

    /** The research table's result: theory rows {@code 0..rows-1} of the entry's notes solved. */
    public static void solveNotes(ServerPlayer player, Identifier entry, int rows) {
        for (int row = 0; row < rows; row++) {
            ResearchManager.of(player).addResearch(com.leclowndu93150.thaumaturge.content.research.note.ResearchNoteData.learnKey(entry, row));
        }
    }

    public static int rawKnowledge(ServerPlayer player, ResourceKey<IResearchCategory> category, boolean theory) {
        return ResearchManager.of(player).rawKnowledge(theory ? KnowledgeType.THEORY : KnowledgeType.OBSERVATION, category);
    }

    public static int knowledge(ServerPlayer player, ResourceKey<IResearchCategory> category, boolean theory) {
        return ResearchManager.of(player).knowledge(theory ? KnowledgeType.THEORY : KnowledgeType.OBSERVATION, category);
    }
}
