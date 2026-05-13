package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.Constants;

public class MultiNoiseChunkGenerator {

    private final MultiNoiseSampler sampler;
    private final NoiseGenerator shapeNoise3D;
    public static final int WATER_LEVEL = 64;

    public MultiNoiseChunkGenerator(long seed) {
        this.sampler = new MultiNoiseSampler(seed);
        this.shapeNoise3D = new NoiseGenerator(seed * 31);
    }

    public void generateBaseTerrain(Chunk chunk, int chunkX, int chunkZ) {
        byte stoneId = BlockRegistry.get(Constants.NAMESPACE + ":stone").getId();
        byte waterId = BlockRegistry.get(Constants.NAMESPACE + ":water").getId();
        byte waterSourceState = (byte) 8;

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {
                int worldX = chunkX * Chunk.SIZE + lx;
                int worldZ = chunkZ * Chunk.SIZE + lz;

                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                float[] params = MultiNoiseBiomeRegistry.getBlendedTerrainParameters(climate);
                float biomeBaseHeight = params[0];
                float biomeVar = params[1];

                // 1. BASIS-GELÄNDE
                float baseHeight = biomeBaseHeight + (climate.continentalness * biomeVar * 1.5f);
                float jaggedness = (biomeVar * 0.15f) + (climate.erosion * biomeVar * 0.25f);

                // Ozean-Forcing
                if (climate.continentalness < -0.2f) {
                    float oceanFactor = Math.min(1.0f, (-0.2f - climate.continentalness) * 5.0f);
                    baseHeight = (baseHeight * (1.0f - oceanFactor)) + (40.0f * oceanFactor);
                    jaggedness *= (1.0f - oceanFactor);
                }

                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    float density = baseHeight - y;
                    float noise3D = shapeNoise3D.getFractalNoise3D(worldX * 0.01f, y * 0.015f, worldZ * 0.01f, 3, 0.5f, 2.0f);
                    density += noise3D * jaggedness;

                    if (density > 0) {
                        // FESTES TERRAIN
                        chunk.setBlock(lx, y, lz, stoneId);
                    } else if (y <= WATER_LEVEL) {
                        // WASSER-GARANTIE: Alles unter Meeresspiegel, was kein Terrain ist, wird Wasser.
                        // (Verhindert Luftlöcher unter Wasser)
                        chunk.setBlock(lx, y, lz, waterId, waterSourceState);
                    }
                }
            }
        }
    }

    public MultiNoiseSampler getSampler() {
        return sampler;
    }
}