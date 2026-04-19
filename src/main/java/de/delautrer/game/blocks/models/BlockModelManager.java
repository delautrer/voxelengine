package de.delautrer.game.blocks.models;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.delautrer.Constants;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlockModelManager {

    public static void loadAllModels(TextureStitcher.AtlasResult atlas) {
        System.out.println("Loading BlockModels from JSONs...");

        TextureStitcher.AtlasRegion missingRegion = atlas.regions.values().iterator().next();
        if (atlas.regions.containsKey("dirt")) {
            missingRegion = atlas.regions.get("dirt");
        }

        for (Map.Entry<String, Block> entry : BlockRegistry.getAll().entrySet()) {
            String fullId = entry.getKey();
            Block block = entry.getValue();

            if (block == BlockRegistry.AIR) continue;

            String blockName = fullId.replace(Constants.NAMESPACE + ":", "");
            BlockModelData model = parseJson(blockName, atlas, missingRegion);

            block.setModel(model);
        }
    }

    private static BlockModelData parseJson(String blockName, TextureStitcher.AtlasResult atlas, TextureStitcher.AtlasRegion missingRegion) {
        BlockModelData model = new BlockModelData();
        String path = "/assets/models/block/" + blockName + ".json";

        try (InputStream is = BlockModelManager.class.getResourceAsStream(path)) {
            if (is == null) {
                System.err.println("WARNING: No JSON found for " + blockName);
                model.fillMissing(missingRegion);
                return model;
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
            if (json.has("textures")) {
                JsonObject textures = json.getAsJsonObject("textures");

                if (textures.has("all")) {
                    TextureStitcher.AtlasRegion r = atlas.regions.getOrDefault(textures.get("all").getAsString(), missingRegion);
                    model.top = r; model.bottom = r;
                    model.north = r; model.south = r; model.east = r; model.west = r;
                }

                if (textures.has("top")) {
                    model.top = atlas.regions.getOrDefault(textures.get("top").getAsString(), missingRegion);
                }
                if (textures.has("bottom")) {
                    model.bottom = atlas.regions.getOrDefault(textures.get("bottom").getAsString(), missingRegion);
                }
                if (textures.has("side")) {
                    TextureStitcher.AtlasRegion r = atlas.regions.getOrDefault(textures.get("side").getAsString(), missingRegion);
                    model.north = r; model.south = r; model.east = r; model.west = r;
                }

                if (textures.has("end")) {
                    TextureStitcher.AtlasRegion r = atlas.regions.getOrDefault(textures.get("end").getAsString(), missingRegion);
                    model.top = r; model.bottom = r;
                }

                if (textures.has("cross")) {
                    TextureStitcher.AtlasRegion r = atlas.regions.getOrDefault(textures.get("cross").getAsString(), missingRegion);
                    model.top = r; model.bottom = r;
                    model.north = r; model.south = r; model.east = r; model.west = r;
                }

                if (textures.has("torch")) {
                    TextureStitcher.AtlasRegion r = atlas.regions.getOrDefault(textures.get("torch").getAsString(), missingRegion);
                    model.top = r; model.bottom = r;
                    model.north = r; model.south = r; model.east = r; model.west = r;
                }
            }
        } catch (Exception e) {
            System.err.println("Error while parsing path: " + path);
            e.printStackTrace();
        }

        model.fillMissing(missingRegion);
        return model;
    }

    public static Set<String> getRequiredTextures() {
        Set<String> textures = new HashSet<>();

        // WICHTIG: Fallbacks und spezielle Texturen (wie die Wolken) manuell hinzufügen!
        textures.add("just_white");

        for (Map.Entry<String, Block> entry : BlockRegistry.getAll().entrySet()) {
            if (entry.getValue() == BlockRegistry.AIR) continue;

            String blockName = entry.getKey().replace(Constants.NAMESPACE + ":", "");
            String path = "/assets/models/block/" + blockName + ".json";

            try (InputStream is = BlockModelManager.class.getResourceAsStream(path)) {
                if (is != null) {
                    JsonObject json = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
                    if (json.has("textures")) {
                        JsonObject texObj = json.getAsJsonObject("textures");
                        for (String key : texObj.keySet()) {
                            textures.add(texObj.get(key).getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading the texture: " + path);
            }
        }
        return textures;
    }

}