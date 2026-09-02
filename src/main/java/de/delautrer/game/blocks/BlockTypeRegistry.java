package de.delautrer.game.blocks;

import de.delautrer.game.blocks.data.BlockDefinition;
import de.delautrer.game.registry.NamespacedKey;
import java.util.HashMap;
import java.util.Map;

public class BlockTypeRegistry {

    @FunctionalInterface
    public interface BlockFactory {
        Block create(BlockDefinition def, NamespacedKey key);
    }

    private static final Map<String, BlockFactory> FACTORIES = new HashMap<>();

    public static void register(String typeName, BlockFactory factory) {
        FACTORIES.put(typeName.toLowerCase(), factory);
    }

    public static Block create(String typeName, BlockDefinition def, NamespacedKey key) {
        if (typeName == null) {
            throw new IllegalArgumentException("Block type is null for block " + key);
        }
        BlockFactory factory = FACTORIES.get(typeName.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown block type '" + typeName + "' for block " + key);
        }
        return factory.create(def, key);
    }

    public static void initBuiltinTypes() {
        if (!FACTORIES.isEmpty()) return;

        register("cube", (def, key) -> new CubeBlock(def.isSolid, def.isTransparent));
        register("slab", (def, key) -> new SlabBlock(def.isSolid, true));
        register("stair", (def, key) -> new StairBlock(def.isSolid, true));
        register("plant", (def, key) -> new PlantBlock());
        register("water", (def, key) -> new WaterBlock());
        register("leaves", (def, key) -> new LeavesBlock());
        register("torch", (def, key) -> new TorchBlock());
        register("log", (def, key) -> new LogBlock());
        register("chest", (def, key) -> new ChestBlock());
        register("trapdoor", (def, key) -> new TrapdoorBlock());
        register("door", (def, key) -> new DoorBlock());
        register("gravity", (def, key) -> new GravityBlock());
        register("crafting_table", (def, key) -> new CraftingTableBlock());
        register("stonecutter", (def, key) -> new StonecutterBlock());
        register("furnace", (def, key) -> new FurnaceBlock());
        register("structure_void", (def, key) -> new StructureVoidBlock());
        register("sapling", (def, key) -> {
            if (def.tree == null || def.tree.trim().isEmpty() ||
                def.log == null || def.log.trim().isEmpty() ||
                def.leaves == null || def.leaves.trim().isEmpty()) {
                throw new IllegalStateException("Sapling block definition " + key + " is missing mandatory 'tree', 'log', or 'leaves' property!");
            }
            return new SaplingBlock(def.tree, def.log, def.leaves, def.minGrowthTime, def.maxGrowthTime);
        });
    }
}