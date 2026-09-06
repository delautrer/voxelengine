package de.delautrer.game.worldgen.pool;

import java.util.List;

public class TemplatePoolDTO {
    public String name;
    public String fallback;
    public List<ElementEntryDTO> elements;

    public static class ElementEntryDTO {
        public int weight = 1;
        public ElementDTO element;
    }

    public static class ElementDTO {
        public String element_type = "single_pool_element";
        public String template;
        public String projection = "rigid";
        public List<Object> processors;
    }
}
