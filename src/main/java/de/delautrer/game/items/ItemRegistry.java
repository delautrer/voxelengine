package de.delautrer.game.items;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ItemRegistry {
    private static final Map<String, Item> ITEMS = new HashMap<>();

    public static final Item GRASS_BLOCK  = register("grass_block", new BlockItem("Grass", "grass_block", BlockRegistry.GRASS_BLOCK));
    public static final Item GRASS_BLOCK_SLABS  = register("grass_block_slabs", new BlockItem("Grass Slabs", "grass_block_slabs", BlockRegistry.GRASS_BLOCK_SLABS));
    public static final Item GRASS_BLOCK_STAIRS  = register("grass_block_stairs", new BlockItem("Grass Stairs", "grass_block_stairs", BlockRegistry.GRASS_BLOCK_STAIRS));

    public static final Item DIRT   = register("dirt", new BlockItem("Dirt", "dirt", BlockRegistry.DIRT));
    public static final Item DIRT_SLABS  = register("dirt_slabs", new BlockItem("Dirt Slabs", "dirt_slabs", BlockRegistry.DIRT_SLABS));
    public static final Item DIRT_STAIRS  = register("dirt_stairs", new BlockItem("Dirt Stairs", "dirt_stairs", BlockRegistry.DIRT_STAIRS));

    public static final Item STONE  = register("stone", new BlockItem("Stone", "stone", BlockRegistry.STONE));
    public static final Item STONE_SLABS  = register("stone_slabs", new BlockItem("Stone Slabs", "stone_slabs", BlockRegistry.STONE_SLABS));
    public static final Item STONE_STAIRS  = register("stone_stairs", new BlockItem("Stone Stairs", "stone_stairs", BlockRegistry.STONE_STAIRS));

    public static final Item WATER_BUCKET = register("water_bucket", new BlockItem("Water bucket", "water_bucket", BlockRegistry.WATER));
    public static final Item GLASS_BLOCK  = register("glass", new BlockItem("Glass", "glass", BlockRegistry.GLASS));
    public static final Item LEAVES_BLOCK = register("leaves", new BlockItem("Leaves", "leaves", BlockRegistry.LEAVES));
    public static final Item TORCH        = register("torch", new BlockItem("Torch", "torch", BlockRegistry.TORCH));
    public static final Item BEDROCK      = register("bedrock", new BlockItem("Bedrock", "bedrock", BlockRegistry.BEDROCK));
    public static final Item GRAVEL       = register("gravel", new BlockItem("Gravel", "gravel", BlockRegistry.GRAVEL));
    public static final Item SAND         = register("sand", new BlockItem("Sand", "sand", BlockRegistry.SAND));
    public static final Item LOG          = register("log", new BlockItem("Log", "log", BlockRegistry.LOG));

    public static final Item PLANKS       = register("planks", new BlockItem("Planks", "planks", BlockRegistry.PLANKS));
    public static final Item STAIRS       = register("stairs", new BlockItem("Stairs", "stairs", BlockRegistry.STAIRS));
    public static final Item SLABS        = register("slabs", new BlockItem("Slabs", "slabs", BlockRegistry.SLABS));

    public static final Item BRICKS         = register("bricks", new BlockItem("Bricks", "bricks_block", BlockRegistry.BRICKS));
    public static final Item BRICKS_STAIRS  = register("bricks_stairs", new BlockItem("Bricks Stairs", "bricks_stairs", BlockRegistry.BRICKS_STAIRS));
    public static final Item BRICKS_SLABS   = register("bricks_slabs", new BlockItem("Bricks Slabs", "bricks_slabs", BlockRegistry.BRICKS_SLABS));

    public static void init() {
        System.out.println("ItemRegistry initialized. " + ITEMS.size() + " Items loaded.");
    }

    private static Item register(String path, Item item) {
        String fullId = Constants.NAMESPACE + ":" + path;
        ITEMS.put(fullId, item);
        return item;
    }

    public static Set<String> getRequiredTextures() {
        Set<String> textures = new HashSet<>();
        for (Item item : ITEMS.values()) {
            textures.add(item.textureName);
        }
        return textures;
    }

    public static String getId(Item targetItem) {
        if (targetItem == null) return null;
        for (Map.Entry<String, Item> entry : ITEMS.entrySet()) {
            if (entry.getValue() == targetItem) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static Item get(String fullId) { return ITEMS.get(fullId); }
    public static Map<String, Item> getAll() { return ITEMS; }
}