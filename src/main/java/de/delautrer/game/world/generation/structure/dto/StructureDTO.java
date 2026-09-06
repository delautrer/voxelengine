package de.delautrer.game.world.generation.structure.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;

public class StructureDTO {
    public String type;
    public String template;
    public String start_pool;
    public int size = 3;
    public String step;
    public JsonElement biomes;
    public List<StructureProcessorDTO> processors;

    // Optional wrapped object
    public StructureDTO structure;

    public String getType() {
        if (structure != null && structure.type != null) return structure.type;
        return type;
    }

    public String getStartPool() {
        if (structure != null && structure.start_pool != null) return structure.start_pool;
        return start_pool;
    }

    public int getSize() {
        if (structure != null && structure.size > 0) return structure.size;
        return size > 0 ? size : 3;
    }

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
