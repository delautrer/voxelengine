package de.delautrer.game.items;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.delautrer.Constants;
import de.delautrer.engine.graphics.utils.TextureStitcher;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemModelManager {

    public static Set<String> getRequiredTextures() {
        Set<String> textures = new HashSet<>();

        for (Map.Entry<String, Item> entry : ItemRegistry.getAll().entrySet()) {
            String itemName = entry.getKey().replace(Constants.NAMESPACE + ":", "");
            String path = "/assets/models/item/" + itemName + ".json";

            try (InputStream is = ItemModelManager.class.getResourceAsStream(path)) {
                if (is != null) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                    if (json.has("textures") && json.getAsJsonObject("textures").has("layer0")) {
                        textures.add(json.getAsJsonObject("textures").get("layer0").getAsString());
                    }
                }
            } catch (Exception e) {
                System.err.println("[ItemModelManager] Error reading texture file: " + path);
            }
        }
        return textures;
    }

    public static void loadAllModels(TextureStitcher.AtlasResult atlas) {
        System.out.println("[ItemModelManager] Loading item json models...");
        TextureStitcher.AtlasRegion missingRegion = atlas.regions.values().iterator().next();

        for (Map.Entry<String, Item> entry : ItemRegistry.getAll().entrySet()) {
            String itemName = entry.getKey().replace(Constants.NAMESPACE + ":", "");
            String path = "/assets/models/item/" + itemName + ".json";
            TextureStitcher.AtlasRegion iconRegion = missingRegion;

            try (InputStream is = ItemModelManager.class.getResourceAsStream(path)) {
                if (is != null) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                    if (json.has("textures") && json.getAsJsonObject("textures").has("layer0")) {
                        String texName = json.getAsJsonObject("textures").get("layer0").getAsString();
                        iconRegion = atlas.regions.getOrDefault(texName, missingRegion);
                    }
                } else {
                    System.err.println("[ItemModelManager] WARNING: No model found for item: " + itemName);
                }
            } catch (Exception e) {}

            entry.getValue().setIconRegion(iconRegion);
        }
    }
}
