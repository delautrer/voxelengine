package de.delautrer.game.world.generation.feature.config;

public class PlacedFeatureDTO {
    public String id;
    public String feature;

    // Optional wrapped structure
    public PlacementDTO placement;

    // Direct flat structure
    public int count;
    public DistributionDTO distribution;
    public ModifiersDTO modifiers;

    public int getCount() {
        if (placement != null) {
            return placement.count;
        }
        return count;
    }

    public DistributionDTO getDistribution() {
        if (placement != null && placement.distribution != null) {
            return placement.distribution;
        }
        return distribution;
    }

    public ModifiersDTO getModifiers() {
        if (placement != null && placement.modifiers != null) {
            return placement.modifiers;
        }
        return modifiers;
    }
}
