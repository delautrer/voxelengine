package de.delautrer.game.items;

import de.delautrer.engine.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class ItemRegistry {
    private static final Map<String, Item> ITEMS = new HashMap<>();

    public static final Item GRASS_BLOCK  = register("grass_block", new BlockItem("Grass", "grass_block", BlockRegistry.GRASS_BLOCK));
    public static final Item DIRT_BLOCK   = register("dirt", new BlockItem("Dirt", "dirt", BlockRegistry.DIRT));
    public static final Item STONE_BLOCK  = register("stone", new BlockItem("Stone", "stone", BlockRegistry.STONE));
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