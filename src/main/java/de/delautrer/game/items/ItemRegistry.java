package de.delautrer.game.items;

import com.google.gson.Gson;
import de.delautrer.Constants;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.items.data.ItemDefinition;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.registry.Registry;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import java.io.Reader;
import java.util.*;
import java.util.stream.Collectors;

public class ItemRegistry {
    public static final Registry<Item> REGISTRY = new Registry<>();
    private static final Gson GSON = new Gson();
    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        isInitialized = true;

        ItemTypeRegistry.initBuiltinTypes();
        loadItemsFromJson();
        generateAutoBlockItems();

        System.out.println("" + REGISTRY.size() + " Items loaded.");
    }

    private static void loadItemsFromJson() {
        List<String> files = ResourceUtils.listResources("assets/data/veinstride/items", ".json");
        for (String file : files) {
            String path = file.substring(0, file.length() - 5).replace('\\', '/');
            NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, path);
            try {
                Reader reader = ResourceUtils.readResourceToReader("assets/data/veinstride/items/" + file);
                ItemDefinition def = GSON.fromJson(reader, ItemDefinition.class);
                if (def.id == null) def.id = path;

                Item item = ItemTypeRegistry.create(def.type, def, key);
                if (item != null) {
                    if (item instanceof ToolItem) {
                        item.setMaxStackSize(def.maxStackSize != 64 ? def.maxStackSize : 1);
                    } else {
                        item.setMaxStackSize(def.maxStackSize);
                    }
                    item.setCategory(def.category);

                    if (def.renderAsItem != null) {
                        item.setRenderAsItem(def.renderAsItem);
                    } else if ("tool".equalsIgnoreCase(def.type) || "simple".equalsIgnoreCase(def.type)) {
                        item.setRenderAsItem(true);
                    }

                    REGISTRY.register(key, item);
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Laden von Item: " + file);
                throw new IllegalStateException("Failed to load item file: " + file, e);
            }
        }
    }

    private static void generateAutoBlockItems() {
        for (Map.Entry<NamespacedKey, Block> entry : Registries.BLOCKS.entrySet()) {
            NamespacedKey blockKey = entry.getKey();
            Block block = entry.getValue();
            if (blockKey.getKey().equals("air") || blockKey.getKey().equals("water") || block instanceof de.delautrer.game.blocks.WaterBlock) continue;

            if (!REGISTRY.contains(blockKey)) {
                BlockItem blockItem = new BlockItem(blockKey.getKey(), blockKey.getKey(), block);
                if ("structure_void".equals(blockKey.getKey()) || "structure_block".equals(blockKey.getKey())) {
                    blockItem.setRenderAsItem(true);
                }
                REGISTRY.register(blockKey, blockItem);
            }
        }
    }

    public static Set<String> getRequiredTextures() {
        Set<String> textures = new HashSet<>();
        for (Item item : REGISTRY.values()) {
            if (item.textureName != null) {
                textures.add(item.textureName);
            }
        }
        return textures;
    }

    public static String getId(Item targetItem) {
        if (targetItem == null) return null;
        NamespacedKey key = REGISTRY.getKey(targetItem);
        return key != null ? key.toString() : null;
    }

    public static Item get(String fullId) {
        return REGISTRY.get(fullId);
    }

    public static Map<String, Item> getAll() {
        return REGISTRY.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
    }
}
