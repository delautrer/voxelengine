package de.delautrer.game.items;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.items.data.ItemDefinition;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import java.util.HashMap;
import java.util.Map;

public class ItemTypeRegistry {

    @FunctionalInterface
    public interface ItemFactory {
        Item create(ItemDefinition def, NamespacedKey key);
    }

    private static final Map<String, ItemFactory> FACTORIES = new HashMap<>();

    public static void register(String typeName, ItemFactory factory) {
        FACTORIES.put(typeName.toLowerCase(), factory);
    }

    public static Item create(String typeName, ItemDefinition def, NamespacedKey key) {
        if (typeName == null) {
            throw new IllegalArgumentException("Item type is null for item " + key);
        }
        ItemFactory factory = FACTORIES.get(typeName.toLowerCase());
        if (factory == null) {
            throw new IllegalArgumentException("Unknown item type '" + typeName + "' for item " + key);
        }
        return factory.create(def, key);
    }

    public static void initBuiltinTypes() {
        if (!FACTORIES.isEmpty()) return;

        register("block", (def, key) -> {
            String blockId = def.blockId != null ? def.blockId : key.getKey();
            Block block = Registries.BLOCKS.get(blockId);
            if (block == null) {
                block = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + blockId);
            }
            return new BlockItem(def.name, def.textureName, block);
        });

        register("simple", (def, key) -> new SimpleItem(def.name, def.textureName));

        register("empty_bucket", (def, key) -> new EmptyBucketItem(def.name, def.textureName));

        register("tool", (def, key) -> {
            if (def.toolType == null) throw new IllegalArgumentException("toolType missing for tool item: " + key);
            if (def.toolTier == null) throw new IllegalArgumentException("toolTier missing for tool item: " + key);
            ToolItem.ToolType tType = ToolItem.ToolType.valueOf(def.toolType.toUpperCase());
            ToolTier tTier = ToolTier.valueOf(def.toolTier.toUpperCase());
            return new ToolItem(def.name, def.textureName, tType, tTier, def.toolEfficiency, def.toolMaxDurability);
        });
    }
}