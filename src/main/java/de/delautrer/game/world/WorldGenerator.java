package de.delautrer.game.world;

import de.delautrer.game.world.generation.biome.CaveCarver;
import de.delautrer.game.world.generation.biome.MultiNoiseBiomeRegistry;
import de.delautrer.game.world.generation.biome.MultiNoiseChunkGenerator;
import de.delautrer.game.world.generation.biome.MultiNoiseSurfaceBuilder;

public class WorldGenerator {

    private final long seed;
    private final MultiNoiseChunkGenerator terrainGenerator;
    private final MultiNoiseSurfaceBuilder surfaceBuilder;

    public WorldGenerator(long seed) {
        this.seed = seed;

        // GANZ WICHTIG: Die Biome-Registry initialisieren,
        // damit unsere Biome im Speicher geladen sind!
        MultiNoiseBiomeRegistry.init();

        // Neues Multi-Noise System initialisieren
        this.terrainGenerator = new MultiNoiseChunkGenerator(seed);
        this.surfaceBuilder = new MultiNoiseSurfaceBuilder(terrainGenerator.getSampler(), seed);
    }

    public void generate(Chunk chunk) {
        int chunkX = chunk.getWorldX();
        int chunkZ = chunk.getWorldZ();

        terrainGenerator.generateBaseTerrain(chunk, chunkX, chunkZ);

        CaveCarver.carve(chunk, seed);

        surfaceBuilder.buildSurface(chunk, chunkX, chunkZ);
    }

    public MultiNoiseChunkGenerator getTerrainGenerator() {
        return terrainGenerator;
    }
}