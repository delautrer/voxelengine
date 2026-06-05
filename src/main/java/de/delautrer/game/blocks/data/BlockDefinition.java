package de.delautrer.game.blocks.data;

public class BlockDefinition {
    public int id;
    public String name;
    public String type;

    public boolean isSolid = true;
    public boolean isTransparent = false;
    public int opacity = -1; // -1 means use default based on isTransparent
    public float hardness = 1.0f;
    public int lightEmission = 0;
    public String soundMaterial = null;
    public String customLootTable = null;
    public int minGrowthTime = 60;
    public int maxGrowthTime = 180;
    public String category = "misc";
    public String minToolTier = "HAND";
}