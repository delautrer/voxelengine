package de.delautrer.game.world.generation.structure.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

public class StructureDTO {
    public String type;
    public String template;
    public String step;
    public JsonElement biomes;
    public List<StructureProcessorDTO> processors;

    // Optional wrapped object
    public StructureDTO structure;

    public String getTemplate() {
        if (structure != null && structure.template != null) return structure.template;
        return template;
    }

    public String getStep() {
        if (structure != null && structure.step != null) return structure.step;
        return step;
    }

    public List<StructureProcessorDTO> getProcessors() {
        if (structure != null && structure.processors != null) return structure.processors;
        return processors;
    }

    public List<String> getBiomesList() {
        JsonElement target = (structure != null && structure.biomes != null) ? structure.biomes : biomes;
        if (target == null || target.isJsonNull()) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        if (target.isJsonPrimitive()) {
            result.add(target.getAsString());
        } else if (target.isJsonArray()) {
            JsonArray arr = target.getAsJsonArray();
            for (JsonElement el : arr) {
                if (el != null && !el.isJsonNull()) {
                    result.add(el.getAsString());
                }
            }
        }
        return result;
    }
}
