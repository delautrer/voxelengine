package de.delautrer.game.blocks.entities;

import de.delautrer.Constants;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registry;

public class BlockEntityTypeRegistry {
    public static final Registry<BlockEntityType<?>> REGISTRY = new Registry<>();

    public static final BlockEntityType<ChestBlockEntity> CHEST = new BlockEntityType<>(
        new NamespacedKey(Constants.NAMESPACE, "chest"), ChestBlockEntity::new);
    public static final BlockEntityType<FurnaceBlockEntity> FURNACE = new BlockEntityType<>(
        new NamespacedKey(Constants.NAMESPACE, "furnace"), FurnaceBlockEntity::new);
    public static final BlockEntityType<StructureBlockEntity> STRUCTURE_BLOCK = new BlockEntityType<>(
        new NamespacedKey(Constants.NAMESPACE, "structure_block"), StructureBlockEntity::new);
    public static final BlockEntityType<JigsawBlockEntity> JIGSAW = new BlockEntityType<>(
        new NamespacedKey(Constants.NAMESPACE, "jigsaw"), JigsawBlockEntity::new);

    public static void init() {
        if (!REGISTRY.contains(CHEST.getKey())) {
            REGISTRY.register(CHEST.getKey(), CHEST);
        }
        if (!REGISTRY.contains(FURNACE.getKey())) {
            REGISTRY.register(FURNACE.getKey(), FURNACE);
        }
        if (!REGISTRY.contains(STRUCTURE_BLOCK.getKey())) {
            REGISTRY.register(STRUCTURE_BLOCK.getKey(), STRUCTURE_BLOCK);
        }
        if (!REGISTRY.contains(JIGSAW.getKey())) {
            REGISTRY.register(JIGSAW.getKey(), JIGSAW);
        }
    }
}