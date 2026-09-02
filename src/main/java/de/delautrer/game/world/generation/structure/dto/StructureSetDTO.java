package de.delautrer.game.world.generation.structure.dto;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class StructureSetDTO {

    public static class StructureEntryDTO {
        public String structure;
        public int weight = 1;
    }

    public static class PlacementDTO {
        public String type = "random_spread";
        public int spacing = 16;
        public int separation = 8;
        public long salt = 0L;
    }

    public JsonElement structures;
    public PlacementDTO placement;

    // Flat placement fields
    public String type;
    public int spacing = 0;
    public int separation = 0;
    public long salt = 0L;

    public List<StructureEntryDTO> getStructureEntries() {
        List<StructureEntryDTO> list = new ArrayList<>();
        if (structures == null || structures.isJsonNull()) return list;

        if (structures.isJsonPrimitive()) {
            StructureEntryDTO entry = new StructureEntryDTO();
            entry.structure = structures.getAsString();
            entry.weight = 1;
            list.add(entry);
        } else if (structures.isJsonArray()) {
            JsonArray arr = structures.getAsJsonArray();
            for (JsonElement el : arr) {
                if (el == null || el.isJsonNull()) continue;
                if (el.isJsonPrimitive()) {
                    StructureEntryDTO entry = new StructureEntryDTO();
                    entry.structure = el.getAsString();
                    entry.weight = 1;
                    list.add(entry);
                } else if (el.isJsonObject()) {
                    JsonObject obj = el.getAsJsonObject();
                    StructureEntryDTO entry = new StructureEntryDTO();
                    entry.structure = obj.has("structure") ? obj.get("structure").getAsString() : "";
                    entry.weight = obj.has("weight") ? obj.get("weight").getAsInt() : 1;
                    list.add(entry);
                }
            }
        }
        return list;
    }

    public PlacementDTO getPlacement() {
        if (placement != null) return placement;

        PlacementDTO dto = new PlacementDTO();
        dto.type = (type != null) ? type : "random_spread";
        dto.spacing = (spacing > 0) ? spacing : 16;
        dto.separation = (separation > 0) ? separation : 8;
        dto.salt = salt;
        return dto;
    }
}
