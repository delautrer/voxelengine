package de.delautrer.game.items.data;

public class ItemDefinition {
    public String id;
    public String type; // "block", "simple", "tool", "empty_bucket"
    public String name;
    public String textureName;
    
    public int maxStackSize = 64;
    public String category = "misc";
    public Boolean renderAsItem = null; // null means auto-detect based on type

    // BlockItem specific
    public String blockId = null;

    // ToolItem specific
    public String toolType = null; // "PICKAXE", "AXE", "SHOVEL"
    public String toolTier = null; // "WOOD", "STONE", "COPPER", "IRON", "GOLD", "DIAMOND"
    public float toolEfficiency = 1.0f;
    public int toolMaxDurability = 1;
}
