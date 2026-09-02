package de.delautrer.game.world.generation.feature.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

public class ModifiersDTO {
    public JsonElement target_blocks;
    public double air_exposure_chance;
    public JsonElement biomes;

    public List<String> getTargetBlocksList() {
        return parseStringOrArray(target_blocks);
    }

    public List<String> getBiomesList() {
        return parseStringOrArray(biomes);
    }

    private static List<String> parseStringOrArray(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        if (element.isJsonPrimitive()) {
            list.add(element.getAsString());
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (JsonElement el : arr) {
                if (el != null && !el.isJsonNull()) {
                    list.add(el.getAsString());
                }
            }
        }
        return list;
    }
}
