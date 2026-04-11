package de.delautrer.game.items;

import de.delautrer.game.world.BlockType;
import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    private static final Map<Integer, ItemType> ITEMS = new HashMap<>();

    public static void init() {
        register(1, new ItemType("Grass", 0, BlockType.GRASS));
        register(2, new ItemType("Dirt", 1, BlockType.DIRT));
        register(3, new ItemType("Stone", 2, BlockType.STONE));
        register(4, new ItemType("Water bucket", 3, BlockType.WATER));
    }

    private static void register(int id, ItemType item) {
        ITEMS.put(id, item);
    }

    public static ItemType get(int id) { return ITEMS.get(id); }
    public static Map<Integer, ItemType> getAll() { return ITEMS; }
}