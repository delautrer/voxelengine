package de.delautrer.game.items.data;

import com.google.gson.annotations.SerializedName;

public class ItemDefinition {
    public String id;
    public String type; // "block", "simple", "tool", "empty_bucket"
    public String name;

    @SerializedName(value = "textureName", alternate = {"texture_name"})
    public String textureName;

    @SerializedName(value = "maxStackSize", alternate = {"max_stack_size"})
    public int maxStackSize = 64;

    public String category = "misc";

    @SerializedName(value = "renderAsItem", alternate = {"render_as_item"})
    public Boolean renderAsItem = null;

    // BlockItem specific
    @SerializedName(value = "blockId", alternate = {"block_id"})
    public String blockId = null;

    // ToolItem specific
    @SerializedName(value = "toolType", alternate = {"tool_type"})
    public String toolType = null;

    @SerializedName(value = "toolTier", alternate = {"tool_tier"})
    public String toolTier = null;

    @SerializedName(value = "toolEfficiency", alternate = {"tool_efficiency"})
    public float toolEfficiency = 1.0f;

    @SerializedName(value = "toolMaxDurability", alternate = {"tool_max_durability"})
    public int toolMaxDurability = 1;
}
