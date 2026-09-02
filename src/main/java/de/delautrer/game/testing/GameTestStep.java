package de.delautrer.game.testing;

import com.google.gson.JsonElement;

public class GameTestStep {
    public String type;
    public int[] pos;
    public int[] from;
    public int[] to;
    public String block;
    public int state = 0;
    public int count = 1;
    public String template;
    public String be_type;
    public String type_alt; // JSON "type" property overload for assert_be / assert_entity
    public JsonElement nbt;

    // Phase 4b fields
    public String face;
    public String prop;
    public JsonElement value;
    public int slot = 0;
    public String item;
    public int min_count = 0;

    public String getBeType() {
        if (be_type != null && !be_type.isEmpty()) return be_type;
        if (type_alt != null && !type_alt.isEmpty()) return type_alt;
        return "chest";
    }

    public String getValueAsString() {
        if (value == null) return "";
        if (value.isJsonPrimitive()) {
            return value.getAsString();
        }
        return value.toString();
    }
}
