package de.delautrer.game.world.generation.biome;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.Constants;
import java.util.Map;

public class Biome {
    public String id;

    // JSON Arrays [min, max]
    public float[] temperature;
    public float[] humidity;
    public float[] continentalness;
    public float[] erosion;
    public float[] weirdness;

    // Blöcke
    public String topBlock;
    public String underBlock;
    public String underwaterBlock;
    public String shoreBlock; // beachBlockId
    public String deepBlock;  // deepMaterialId

    // Terrain-Parameter
    public float baseHeight = 64.0f;
    public float heightVariation = 15.0f;

    // Bäume
    public float treeProbability;
    public Map<String, Integer> trees;

    // Flora
    public float floraProbability;
    public float floraPatchThreshold = 0.0f; // patchThreshold
    public float floraDensity = 1.0f;        // density
    public Map<String, Integer> flora;

    // Blobs & Patches
    public Map<String, Float> undergroundBlobs; // Früher auch für Oberfläche genutzt
    public float underwaterBlobScale = 0.1f;
    public Map<String, Float> underwaterBlobs;

    // Helper für die Berechnung
    private float distanceTo(float[] range, float value) {
        if (range == null || range.length < 2) return 0f;
        if (value < range[0]) return range[0] - value;
        if (value > range[1]) return value - range[1];
        return 0f;
    }

    public float calculateFitness(Climate.TargetPoint point) {
        float distT = distanceTo(temperature, point.temperature);
        float distH = distanceTo(humidity, point.humidity);
        float distC = distanceTo(continentalness, point.continentalness);
        float distE = distanceTo(erosion, point.erosion);
        float distW = distanceTo(weirdness, point.weirdness);

        return (distT * distT) + (distH * distH) + (distC * distC) + (distE * distE) + (distW * distW);
    }

    public String getName() {
        return id;
    }

    public byte getTopBlockId() { return getBlockId(topBlock); }
    public byte getUnderBlockId() { return getBlockId(underBlock); }
    public byte getUnderwaterBlockId() { return getBlockId(underwaterBlock); }
    public byte getShoreBlockId() { return getBlockId(shoreBlock != null ? shoreBlock : topBlock); }
    public byte getDeepBlockId() { return getBlockId(deepBlock != null ? deepBlock : "stone"); }

    private byte getBlockId(String name) {
        if (name == null) return 0;
        return BlockRegistry.get(Constants.NAMESPACE + ":" + name).getId();
    }
}