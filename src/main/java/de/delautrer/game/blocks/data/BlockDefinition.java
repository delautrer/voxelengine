package de.delautrer.game.blocks.data;

public class BlockDefinition {
    public int id;
    public String name;
    public String type;

    public boolean isSolid = true;
    public boolean isTransparent = false;
    public float hardness = 1.0f;
    public int lightEmission = 0;
    public String soundMaterial = null;
    public String customLootTable = null;
    public String category = "misc";
}