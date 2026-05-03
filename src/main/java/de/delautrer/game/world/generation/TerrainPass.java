package de.delautrer.game.world.generation;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.WaterBlock;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.BiomeProvider;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;

public class TerrainPass implements IGenerationPass {

    private BiomeProvider biomeProvider;
    private NoiseGenerator elevationNoise;
    private NoiseGenerator roughnessNoise;
    private NoiseGenerator detailNoise;

    private final BlockState air = BlockRegistry.get(Constants.NAMESPACE + ":air").getDefaultState();
    private final BlockState stone = BlockRegistry.get(Constants.NAMESPACE + ":stone").getDefaultState();
    private final BlockState bedrock = BlockRegistry.get(Constants.NAMESPACE + ":bedrock").getDefaultState();
    private final BlockState water = BlockRegistry.get(Constants.NAMESPACE + ":water").getDefaultState().with(WaterBlock.LEVEL, 8);

    private static final int WATER_LEVEL = 60;

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (biomeProvider == null) biomeProvider = new BiomeProvider(seed);
        if (elevationNoise == null) elevationNoise = new NoiseGenerator(seed);
        if (roughnessNoise == null) roughnessNoise = new NoiseGenerator(seed * 2);
        if (detailNoise == null) detailNoise = new NoiseGenerator(seed * 3);

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int realX = worldX * Chunk.SIZE + x;
                int realZ = worldZ * Chunk.SIZE + z;

                // 1. Die Form des Geländes berechnen (Dein bewährter Code)
                float elevation = elevationNoise.getFractalNoise2D(realX * 0.0015f, realZ * 0.0015f, 4, 0.5f, 2.0f);
                float baseHeight = 64.0f + (elevation * 80.0f);

                float roughness = roughnessNoise.getFractalNoise2D(realX * 0.002f, realZ * 0.002f, 4, 0.5f, 2.0f);
                float localRoughness = Math.max(0, roughness + 0.2f) * 120.0f;

                // Berge massiv verstärken
                if (roughness > 0.1f) {
                    // Ab einem bestimmten Rausch-Wert ziehen wir die Berge steil nach oben
                    float mountainBoost = (roughness - 0.1f) * 5.0f;
                    localRoughness *= (1.0f + mountainBoost);
                    baseHeight += mountainBoost * 50.0f;
                }

                float flattenFactor = Math.max(0.0f, Math.min(1.0f, (baseHeight - 50.0f) / 15.0f));
                localRoughness *= flattenFactor;

                float detail = detailNoise.getFractalNoise2D(realX * 0.01f, realZ * 0.01f, 4, 0.5f, 2.0f);

                int terrainHeight = (int) (baseHeight + (detail * localRoughness));
                terrainHeight = Math.min(Chunk.HEIGHT - 2, Math.max(1, terrainHeight));
                heightMap[x][z] = terrainHeight;

                // 2. Das Biome anhand der Form und dem Klima bestimmen
                Biome biome = biomeProvider.getBiome(realX, realZ, elevation, roughness);
                chunk.setBiome(x, z, biome);

                // 3. Chunk füllen (erstmal nur Stein, Bedrock, Wasser)
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (y == 0) {
                        chunk.setBlock(x, y, z, bedrock.getBlock().getId(), bedrock.getStateId());
                    } else if (y <= terrainHeight) {
                        chunk.setBlock(x, y, z, stone.getBlock().getId(), stone.getStateId());
                    } else if (y <= WATER_LEVEL) {
                        chunk.setBlock(x, y, z, water.getBlock().getId(), water.getStateId());
                    } else {
                        chunk.setBlock(x, y, z, air.getBlock().getId(), air.getStateId());
                    }
                }
            }
        }
    }
}