package de.delautrer.game.items;

import de.delautrer.engine.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.CubeBlock;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {

    private static final Map<String, Item> ITEMS = new HashMap<>();

    public static final Item GRASS_BLOCK  = register("grass", new BlockItem("Grass", 0, BlockRegistry.GRASS_BLOCK));
    public static final Item DIRT_BLOCK   = register("dirt", new BlockItem("Dirt", 1, BlockRegistry.DIRT));
    public static final Item STONE_BLOCK  = register("stone", new BlockItem("Stone", 2, BlockRegistry.STONE));
    public static final Item WATER_BUCKET = register("water_bucket", new BlockItem("Water bucket", 3, BlockRegistry.WATER));
    public static final Item GLASS_BLOCK  = register("glass", new BlockItem("Glass", 4, BlockRegistry.GLASS));
    public static final Item LEAVES_BLOCK = register("leaves", new BlockItem("Leaves", 5, BlockRegistry.LEAVES));
    public static final Item TORCH = register("torch", new BlockItem("Torch", 6, BlockRegistry.TORCH));
    public static final Item BEDROCK = register("bedrock", new BlockItem("Bedrock", 7, BlockRegistry.BEDROCK));
    public static final Item GRAVEL = register("gravel", new BlockItem("Gravel", 8, BlockRegistry.GRAVEL));
    public static final Item SAND = register("sand", new BlockItem("Sand", 9, BlockRegistry.SAND));
    public static final Item LOG = register("log", new BlockItem("Log", 10, BlockRegistry.LOG));

    public static void init() {
        System.out.println("ItemRegistry initialized. " + ITEMS.size() + " Items loaded.");
    }

    private static Item register(String path, Item item) {
        String fullId = Constants.NAMESPACE + ":" + path;

        if (ITEMS.containsKey(fullId)) {
            throw new RuntimeException("Item-ID " + fullId + " already used!");
        }

        ITEMS.put(fullId, item);
        return item;
    }

    public static Item get(String fullId) {
        return ITEMS.get(fullId);
    }

    public static Map<String, Item> getAll() {
        return ITEMS;
    }
}