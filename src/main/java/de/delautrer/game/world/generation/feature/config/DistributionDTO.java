package de.delautrer.game.world.generation.feature.config;

public class DistributionDTO {
    public String type; // "uniform", "trapezoid"
    public int min_y;
    public int max_y;
    public int peak_y; // Only for trapezoid
}
