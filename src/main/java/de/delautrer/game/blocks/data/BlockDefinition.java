package de.delautrer.game.blocks.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class BlockDefinition {
    public int id;
    public String name;
    public String type;

    @SerializedName(value = "isSolid", alternate = {"solid", "is_solid"})
    public boolean isSolid = true;

    @SerializedName(value = "isTransparent", alternate = {"transparent", "is_transparent"})
    public boolean isTransparent = false;

    @SerializedName(value = "isPassable", alternate = {"passable", "is_passable"})
    public boolean isPassable = false;

    @SerializedName(value = "isRaycastable", alternate = {"raycastable", "is_raycastable"})
    public boolean isRaycastable = true;

    public int opacity = -1;

    public float hardness = 1.0f;

    @SerializedName(value = "lightEmission", alternate = {"light_emission"})
    public int lightEmission = 0;

    @SerializedName(value = "soundMaterial", alternate = {"sound_material"})
    public String soundMaterial = null;

    @SerializedName(value = "customLootTable", alternate = {"custom_loot_table", "loot_table"})
    public String customLootTable = null;

    @SerializedName(value = "minGrowthTime", alternate = {"min_growth_time"})
    public int minGrowthTime = 60;

    @SerializedName(value = "maxGrowthTime", alternate = {"max_growth_time"})
    public int maxGrowthTime = 180;

    public String category = "misc";

    @SerializedName(value = "minToolTier", alternate = {"min_tool_tier"})
    public String minToolTier = "HAND";

    // Sapling specific fields
    public String tree = null;
    public String log = null;
    public String leaves = null;

    public List<String> tags = new ArrayList<>();
}