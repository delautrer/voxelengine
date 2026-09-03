package de.delautrer.game.world.generation.structure.dto;

import com.google.gson.JsonElement;
import java.util.List;

public class StructureTemplateDTO {
    public int[] size;
    public List<String> palette;
    public List<BlockElementDTO> blocks;

    public static class BlockElementDTO {
        public int[] pos;
        public int state;
        public int stateId = 0;
        public JsonElement nbt;
    }
}
