package de.delautrer.game.world;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import java.util.Map;

public enum Biome {
    //                                Surface        Sub-Surface    Beach    Deep Material  BaseH  Var.   Tree  Patch   Dens.  Flora Weights                                    Underground Blobs                          UW-Scale   Underwater Blobs
    OCEAN(                           "gravel",      "gravel",      "gravel", "stone",       35.0f, 10.0f, 0.0f,  1.0f,   0.0f,  Map.of(),                                        Map.of("dirt", 0.05f),                     0.05f,     Map.of("sand", 0.7f, "gravel", 0.3f)),

    PLAINS(                          "grass_block", "dirt",        "sand",   "stone",       64.0f, 10.0f, 0.3f,  0.15f,  0.7f,  Map.of("grass", 200, "poppy", 5, "dandelion", 5), Map.of("dirt", 0.08f, "gravel", 0.04f),    0.1f,      Map.of("dirt", 0.6f, "sand", 0.4f)),

    FLOWER_PLAINS(                   "grass_block", "dirt",        "sand",   "stone",       64.0f, 12.0f, 0.0f, -0.2f,   0.9f,  Map.of("red_tulip", 20, "purple_tulip", 20, "mavvinilia", 20), Map.of("dirt", 0.08f, "gravel", 0.04f), 0.1f, Map.of("dirt", 0.6f, "sand", 0.4f)),

    // FOREST FIX: Mehr und dichteres Gras
    FOREST(                          "grass_block", "dirt",        "grass_block", "stone",  66.0f, 15.0f, 6.0f, -0.1f,   0.95f, Map.of("grass", 300, "dotty", 10, "mavvinilia", 10, "fairy_bell", 10), Map.of("dirt", 0.1f, "gravel", 0.03f), 0.12f, Map.of("dirt", 0.8f, "gravel", 0.2f)),

    DESERT(                          "sand",        "sand",        "sand",   "sand",        65.0f, 12.0f, 0.0f,  0.3f,   0.2f,  Map.of("sandy_grass", 100),                      Map.of("gravel", 0.05f),                   0.1f,      Map.of("sand", 1.0f)),
    DESERT_HILLS(                    "sand",        "sand",        "sand",   "sand",        80.0f, 30.0f, 0.0f,  0.35f,  0.15f, Map.of("sandy_grass", 100),                      Map.of("gravel", 0.05f),                   0.1f,      Map.of("sand", 1.0f)),

    // HILLS FIX: Patch Threshold auf 0.0f (viel mehr Grasstellen), Density auf 0.9f (viel dichter)
    HILLS(                           "grass_block", "dirt",        "sand",   "stone",       80.0f, 40.0f, 1.2f,  0.0f,   0.9f,  Map.of("grass", 100, "dandelion", 2, "poppy", 2),Map.of("dirt", 0.05f, "gravel", 0.05f),    0.1f,      Map.of("dirt", 0.5f, "gravel", 0.5f)),

    MOUNTAINS(                       "stone",       "stone",       "stone",  "stone",       100.0f, 120.0f,0.0f,  1.0f,   0.0f,  Map.of(),                                        Map.of("gravel", 0.1f),                    0.08f,     Map.of("gravel", 0.8f, "stone", 0.2f));

    private final String surfaceBlockId;
    private final String subSurfaceBlockId;
    private final String beachBlockId;
    private final String deepMaterialId;

    public final float baseHeight;
    public final float heightVariation;

    public final float treeChance;
    public final float floraPatchThreshold;
    public final float floraDensity;
    private final Map<String, Integer> floraWeights;
    private final Map<String, Float> undergroundBlobs;

    public final float underwaterBlobScale;
    private final Map<String, Float> underwaterBlobs;

    Biome(String surfaceBlockId, String subSurfaceBlockId, String beachBlockId, String deepMaterialId, float baseHeight, float heightVariation, float treeChance, float floraPatchThreshold, float floraDensity, Map<String, Integer> floraWeights, Map<String, Float> undergroundBlobs, float underwaterBlobScale, Map<String, Float> underwaterBlobs) {
        this.surfaceBlockId = surfaceBlockId;
        this.subSurfaceBlockId = subSurfaceBlockId;
        this.beachBlockId = beachBlockId;
        this.deepMaterialId = deepMaterialId;
        this.baseHeight = baseHeight;
        this.heightVariation = heightVariation;
        this.treeChance = treeChance;
        this.floraPatchThreshold = floraPatchThreshold;
        this.floraDensity = floraDensity;
        this.floraWeights = floraWeights;
        this.undergroundBlobs = undergroundBlobs;
        this.underwaterBlobScale = underwaterBlobScale;
        this.underwaterBlobs = underwaterBlobs;
    }

    public BlockState getSurfaceBlock() { return BlockRegistry.get(Constants.NAMESPACE + ":" + surfaceBlockId).getDefaultState(); }
    public BlockState getSubSurfaceBlock() { return BlockRegistry.get(Constants.NAMESPACE + ":" + subSurfaceBlockId).getDefaultState(); }
    public BlockState getBeachBlock() { return BlockRegistry.get(Constants.NAMESPACE + ":" + beachBlockId).getDefaultState(); }
    public BlockState getDeepMaterial() { return BlockRegistry.get(Constants.NAMESPACE + ":" + deepMaterialId).getDefaultState(); }
    public Map<String, Integer> getFloraWeights() { return floraWeights; }
    public Map<String, Float> getUndergroundBlobs() { return undergroundBlobs; }
    public Map<String, Float> getUnderwaterBlobs() { return underwaterBlobs; }
}