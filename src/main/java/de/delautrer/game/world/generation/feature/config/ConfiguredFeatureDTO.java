package de.delautrer.game.world.generation.feature.config;

public class ConfiguredFeatureDTO {
    public String type; // "standard_vein" or "mega_vein"
    public String block;
    public int size; // Only for standard_vein
    public String carrier; // Only for mega_vein
    public double ore_chance; // Only for mega_vein
}
