package de.delautrer.game.world.generation.feature.config;

public class ConfiguredFeatureDTO {
    public String type; // "standard_vein", "mega_vein", "tree"
    public String block;
    public int size; // Only for standard_vein
    public String carrier; // Only for mega_vein
    public double ore_chance; // Only for mega_vein

    // Tree feature fields
    public String shape;
    public String log;
    public String leaves;
    public int baseHeight = 4;
    public int heightVariation = 3;
}
