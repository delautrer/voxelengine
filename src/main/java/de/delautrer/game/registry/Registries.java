package de.delautrer.game.registry;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.items.Item;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.items.ItemRegistry;

public class Registries {

    public static final Registry<Block> BLOCKS = BlockRegistry.REGISTRY;
    public static final Registry<Item> ITEMS = ItemRegistry.REGISTRY;

    // In the future, more registries can be added here:
    // public static final Registry<Biome> BIOMES = new Registry<>();
    // public static final Registry<EntityDefinition> ENTITIES = new Registry<>();

    public static void init() {
        // Ensure that the static blocks inside BlockRegistry and ItemRegistry are loaded.
        BlockRegistry.init();
        ItemRegistry.init();
    }
}
