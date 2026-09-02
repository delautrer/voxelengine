package de.delautrer.game.world.generation.biome;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.Constants;
import java.util.List;
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
    public float baseHeight = 0.0f;
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
    public Map<String, Float> surfaceBlobs;     // Für Oberflächen-Patches (z.B. Gravel in Bergen)
    public Map<String, Float> undergroundBlobs; // Für Taschen im Stein (Erze, Erde, Kies)
    public float underwaterBlobScale = 0.1f;
    public Map<String, Float> underwaterBlobs;

    // Phase 3 optional fields
    public String precipitation = "rain";
    public Map<String, String> effects;
    public List<String> features;
    public List<String> structureIds;

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

    private float getRangeSpan(float[] range) {
        if (range == null || range.length < 2) return 2.0f;
        return Math.max(0.0f, range[1] - range[0]);
    }

    public float calculateVolume() {
        float tempSpan = getRangeSpan(temperature);
        float humSpan = getRangeSpan(humidity);
        float contSpan = getRangeSpan(continentalness);
        float eroSpan = getRangeSpan(erosion);
        float wrdSpan = getRangeSpan(weirdness);

        float rawVolume = tempSpan * humSpan * contSpan * eroSpan * wrdSpan;
        return Math.max(1e-4f, rawVolume);
    }

    public String getName() {
        return id;
    }

    public de.delautrer.game.blocks.Block getTopBlock() { return getBlock(topBlock); }
    public de.delautrer.game.blocks.Block getUnderBlock() { return getBlock(underBlock); }
    public de.delautrer.game.blocks.Block getUnderwaterBlock() { return getBlock(underwaterBlock); }
    public de.delautrer.game.blocks.Block getShoreBlock() { return getBlock(shoreBlock != null ? shoreBlock : topBlock); }
    public de.delautrer.game.blocks.Block getDeepBlock() { return getBlock(deepBlock != null ? deepBlock : "stone"); }

    private de.delautrer.game.blocks.Block getBlock(String name) {
        if (name == null) return de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
        if ("grass".equalsIgnoreCase(name)) name = "grass_block";
        if (!name.contains(":")) name = "veinstride:" + name;
        de.delautrer.game.blocks.Block b = de.delautrer.game.registry.Registries.BLOCKS.get(de.delautrer.game.registry.NamespacedKey.fromString(name));
        return b != null ? b : de.delautrer.game.registry.Registries.BLOCKS.get("veinstride:air");
    }
}