package de.delautrer.game.items;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.delautrer.Constants;
import de.delautrer.game.entity.player.LocalPlayer;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.world.World;
import org.joml.Vector3i;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registry;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.items.data.ItemDefinition;

public class ItemRegistry {
    public static final Registry<Item> REGISTRY = new Registry<>();
    private static final Gson GSON = new Gson();

    private static boolean isInitialized = false;

    public static void init() {
        if (isInitialized) return;
        isInitialized = true;
        System.out.println("Initializing...");

        loadItemsFromJson();

        System.out.println("" + REGISTRY.size() + " Items loaded.");
    }

    private static void loadItemsFromJson() {
        try {
            InputStream is = ItemRegistry.class.getResourceAsStream("/assets/data/items.json");
            if (is == null) {
                System.err.println("Error: /assets/data/items.json nicht gefunden!");
                return;
            }

            Type listType = new TypeToken<List<ItemDefinition>>() {
            }.getType();
            List<ItemDefinition> definitions = GSON.fromJson(new InputStreamReader(is), listType);

            for (ItemDefinition def : definitions) {
                Item item = createItemInstance(def);
                if (item != null) {
                    item.setMaxStackSize(def.maxStackSize);
                    item.setCategory(def.category);
                    
                    if (def.renderAsItem != null) {
                        item.setRenderAsItem(def.renderAsItem);
                    } else if (def.type.equalsIgnoreCase("tool") || def.type.equalsIgnoreCase("simple")) {
                        item.setRenderAsItem(true);
                    }
                    
                    register(def.id, item);
                }
            }
            
            // Hardcoded sticks just in case it wasn't caught by the script perfectly (though it was)
            if (get(Constants.NAMESPACE + ":sticks") == null) {
                register("sticks", new Item("Sticks", "sticks") {
                    @Override
                    public boolean onUseRightClick(World world, LocalPlayer localPlayer, Vector3i targetBlock, Vector3i adjacentBlock, PlayerInteraction interaction) {
                        return false;
                    }
                }.setCategory("misc"));
            }

        } catch (Exception e) {
            System.err.println("Fehler beim Laden der items.json");
            e.printStackTrace();
        }
    }

    private static Item createItemInstance(ItemDefinition def) {
        if (def.type == null) {
            System.err.println("Item '" + def.id + "' hat keinen type!");
            return null;
        }

        switch (def.type.toLowerCase()) {
            case "block":
                String blockId = def.blockId != null ? def.blockId : def.id;
                return new BlockItem(def.name, def.textureName, Registries.BLOCKS.get(Constants.NAMESPACE + ":" + blockId));
            case "simple":
                return new SimpleItem(def.name, def.textureName);
            case "empty_bucket":
                return new EmptyBucketItem(def.name, def.textureName);
            case "tool":
                ToolItem.ToolType tType = ToolItem.ToolType.valueOf(def.toolType);
                ToolTier tTier = ToolTier.valueOf(def.toolTier);
                return new ToolItem(def.name, def.textureName, tType, tTier, def.toolEfficiency, def.toolMaxDurability);
            default:
                System.err.println("Unbekannter Item Type: " + def.type + " bei " + def.id);
                return null;
        }
    }

    private static Item register(String path, Item item) {
        NamespacedKey key = new NamespacedKey(Constants.NAMESPACE, path);
        REGISTRY.register(key, item);
        return item;
    }

    public static Set<String> getRequiredTextures() {
        Set<String> textures = new HashSet<>();
        for (Item item : REGISTRY.values()) {
            textures.add(item.textureName);
        }
        return textures;
    }

    public static String getId(Item targetItem) {
        if (targetItem == null) return null;
        NamespacedKey key = REGISTRY.getKey(targetItem);
        return key != null ? key.toString() : null;
    }

    public static Item get(String fullId) { return REGISTRY.get(fullId); }
    public static Map<String, Item> getAll() {
        return REGISTRY.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
    }
}
