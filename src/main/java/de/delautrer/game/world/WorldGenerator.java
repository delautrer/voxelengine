package de.delautrer.game.world;

import de.delautrer.game.world.generation.biome.*;
import de.delautrer.game.world.generation.feature.FeatureRegistry;

public class WorldGenerator {

    private final long seed;
    private final MultiNoiseChunkGenerator terrainGenerator;
    private final MultiNoiseSurfaceBuilder surfaceBuilder;

    public WorldGenerator(long seed) {
        this.seed = seed;
        MultiNoiseBiomeRegistry.init();
        FeatureRegistry.init();

        // Neues Multi-Noise System initialisieren
        this.terrainGenerator = new MultiNoiseChunkGenerator(seed);
        this.surfaceBuilder = new MultiNoiseSurfaceBuilder(terrainGenerator.getSampler(), seed);
    }

    public void generate(Chunk chunk) {
        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();

        terrainGenerator.generateBaseTerrain(chunk, chunkX, chunkZ);
        CaveCarver.carve(chunk, seed, terrainGenerator.getSampler());
        surfaceBuilder.buildSurface(chunk, chunkX, chunkZ);
        
        // Generiere Erze (Features) nach der Oberfläche
        FeatureRegistry.generateOres(chunk, seed);
    }

    public MultiNoiseChunkGenerator getTerrainGenerator() {
        return terrainGenerator;
    }
}