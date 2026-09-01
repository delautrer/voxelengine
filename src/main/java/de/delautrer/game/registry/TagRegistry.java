package de.delautrer.game.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.delautrer.Constants;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.items.Item;
import java.io.Reader;
import java.util.*;

public class TagRegistry {

    public static final Map<NamespacedKey, Tag<Block>> BLOCK_TAGS = new HashMap<>();
    public static final Map<NamespacedKey, Tag<Item>> ITEM_TAGS = new HashMap<>();
    private static final Gson GSON = new Gson();

    public static void loadTags() {
        BLOCK_TAGS.clear();
        ITEM_TAGS.clear();

        loadTagsForType("assets/data/veinstride/tags/blocks", BLOCK_TAGS);
        loadTagsForType("assets/data/veinstride/tags/items", ITEM_TAGS);

        resolveBlockTags();
        resolveItemTags();
    }

    private static <T> void loadTagsForType(String folderPath, Map<NamespacedKey, Tag<T>> targetMap) {
        List<String> files = ResourceUtils.listResources(folderPath, ".json");
        for (String file : files) {
            String pathWithoutExt = file.endsWith(".json") ? file.substring(0, file.length() - 5) : file;
            NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, pathWithoutExt);
            try {
                Reader reader = ResourceUtils.readResourceToReader(folderPath + "/" + file);
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                boolean replace = json.has("replace") && json.get("replace").getAsBoolean();
                Set<String> values = new HashSet<>();
                if (json.has("values")) {
                    JsonArray arr = json.getAsJsonArray("values");
                    for (JsonElement el : arr) {
                        values.add(el.getAsString());
                    }
                }
                targetMap.put(key, new Tag<>(key, replace, values));
            } catch (Exception e) {
                System.err.println("[TagRegistry] Failed to load tag: " + folderPath + "/" + file);
                e.printStackTrace();
            }
        }
    }

    private static void resolveBlockTags() {
        for (Map.Entry<NamespacedKey, Tag<Block>> entry : BLOCK_TAGS.entrySet()) {
            resolveBlockTag(entry.getValue(), new HashSet<>());
        }
    }

    private static void resolveBlockTag(Tag<Block> tag, Set<NamespacedKey> visited) {
        if (!visited.add(tag.getKey())) {
            throw new IllegalStateException("Circular dependency in block tag: " + tag.getKey());
        }
        for (String val : tag.getRawValues()) {
            if (val.startsWith("#")) {
                NamespacedKey parentKey = NamespacedKey.fromString(val.substring(1));
                Tag<Block> parentTag = BLOCK_TAGS.get(parentKey);
                if (parentTag != null) {
                    resolveBlockTag(parentTag, visited);
                    for (Block b : parentTag.getElements()) {
                        tag.addElement(b);
                    }
                } else {
                    throw new IllegalStateException("Block tag " + tag.getKey() + " references unknown parent tag: #" + parentKey);
                }
            } else {
                NamespacedKey blockKey = NamespacedKey.fromString(val);
                Block b = Registries.BLOCKS.get(blockKey);
                if (b != null) {
                    tag.addElement(b);
                } else {
                    throw new IllegalStateException("Block tag " + tag.getKey() + " references unknown block: " + blockKey);
                }
            }
        }
    }

    private static void resolveItemTags() {
        for (Map.Entry<NamespacedKey, Tag<Item>> entry : ITEM_TAGS.entrySet()) {
            resolveItemTag(entry.getValue(), new HashSet<>());
        }
    }

    private static void resolveItemTag(Tag<Item> tag, Set<NamespacedKey> visited) {
        if (!visited.add(tag.getKey())) {
            throw new IllegalStateException("Circular dependency in item tag: " + tag.getKey());
        }
        for (String val : tag.getRawValues()) {
            if (val.startsWith("#")) {
                NamespacedKey parentKey = NamespacedKey.fromString(val.substring(1));
                Tag<Item> parentTag = ITEM_TAGS.get(parentKey);
                if (parentTag != null) {
                    resolveItemTag(parentTag, visited);
                    for (Item item : parentTag.getElements()) {
                        tag.addElement(item);
                    }
                } else {
                    throw new IllegalStateException("Item tag " + tag.getKey() + " references unknown parent tag: #" + parentKey);
                }
            } else {
                NamespacedKey itemKey = NamespacedKey.fromString(val);
                Item item = Registries.ITEMS.get(itemKey);
                if (item != null) {
                    tag.addElement(item);
                } else {
                    throw new IllegalStateException("Item tag " + tag.getKey() + " references unknown item: " + itemKey);
                }
            }
        }
    }

    public static Tag<Block> getBlockTag(String tagString) {
        return BLOCK_TAGS.get(NamespacedKey.fromString(tagString));
    }

    public static Tag<Item> getItemTag(String tagString) {
        return ITEM_TAGS.get(NamespacedKey.fromString(tagString));
    }
}