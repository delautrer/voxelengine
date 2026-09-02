package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.persistence.WorldPalette;

import java.util.Random;

public class MultiNoiseChunkGenerator {

    private final MultiNoiseSampler sampler;
    private final NoiseGenerator shapeNoise3D;
    private final long seed;

    private final NoiseGenerator riverNoise;

    public static final int WATER_LEVEL = 0;

    public MultiNoiseChunkGenerator(long seed) {
        this.seed = seed;
        this.sampler = new MultiNoiseSampler(seed);
        this.shapeNoise3D = new NoiseGenerator(seed * 31);
        this.riverNoise = new NoiseGenerator(seed * 234);
    }

    public void generateBaseTerrain(Chunk chunk, int chunkX, int chunkZ, WorldPalette palette) {
        Block stone = Registries.BLOCKS.get("veinstride:stone");
        Block water = Registries.BLOCKS.get("veinstride:water");
        Block bedrock = Registries.BLOCKS.get("veinstride:bedrock");
        WorldPalette usePalette = (palette != null) ? palette : chunk.getPalette();
        byte waterSourceState = (byte) 8;

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {
                int worldX = chunkX * Chunk.SIZE + lx;
                int worldZ = chunkZ * Chunk.SIZE + lz;

                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                float[] params = MultiNoiseBiomeRegistry.getBlendedTerrainParameters(climate);
                float biomeBaseHeight = params[0];
                float biomeVar = params[1];

                float cont = climate.continentalness;
                float erosion = climate.erosion;

                float baseHeight = biomeBaseHeight;
                if (cont > 0.0f) {
                    float mountainFactor = cont * Math.max(0.0f, erosion);
                    baseHeight += (mountainFactor * mountainFactor * 120.0f) * (biomeVar / 10.0f);
                } else {
                    baseHeight += cont * biomeVar * 1.5f;
                }

                float jaggedness = (biomeVar * 0.15f) + (erosion * biomeVar * 0.8f);
                if (cont > 0.4f && erosion > 0.5f) {
                    jaggedness += (cont - 0.4f) * (erosion - 0.5f) * 150.0f;
                }

                float warpX = riverNoise.getFractalNoise2D(worldX * 0.02f, worldZ * 0.02f, 2, 0.5f, 2.0f) * 25.0f;
                float warpZ = riverNoise.getFractalNoise2D(worldX * 0.02f + 100, worldZ * 0.02f + 100, 2, 0.5f, 2.0f) * 25.0f;

                float rNoise = riverNoise.getFractalNoise2D((worldX + warpX) * 0.003f, (worldZ + warpZ) * 0.003f, 3, 0.5f, 2.0f);
                float riverVal = Math.abs(rNoise);
                float riverThreshold = 0.06f;

                if (riverVal < riverThreshold) {
                    float riverBlend = 1.0f - (riverVal / riverThreshold);
                    riverBlend = riverBlend * riverBlend * (3.0f - 2.0f * riverBlend);

                    baseHeight = (baseHeight * (1.0f - riverBlend)) + ((WATER_LEVEL - 3) * riverBlend);
                    jaggedness *= (1.0f - riverBlend);
                }

                if (climate.continentalness < -0.2f) {
                    float oceanFactor = Math.min(1.0f, (-0.2f - climate.continentalness) * 5.0f);
                    baseHeight = (baseHeight * (1.0f - oceanFactor)) + (-24.0f * oceanFactor);
                    jaggedness *= (1.0f - oceanFactor);
                }

                long brSeed = seed ^ ((long) worldX * 314159L ^ (long) worldZ * 271828L);
                Random brRand = new Random(brSeed);
                int bedrockLimit = Chunk.MIN_Y + brRand.nextInt(4);

                int waterProtection = 0;

                for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
                    float baseDensity = baseHeight - y;
                    float noise3D = shapeNoise3D.getFractalNoise3D(worldX * 0.01f, y * 0.015f, worldZ * 0.01f, 3, 0.5f, 2.0f);
                    baseDensity += noise3D * jaggedness;

                    float finalDensity = baseDensity;

                    boolean isWater = (y <= WATER_LEVEL && baseDensity <= 0);
                    if (riverVal < riverThreshold && y <= (MultiNoiseChunkGenerator.WATER_LEVEL - 3)) isWater = true;

                    if (isWater) {
                        waterProtection = 5;
                    } else if (waterProtection > 0) {
                        waterProtection--;
                    }

                    if (y <= bedrockLimit) {
                        chunk.setBlock(lx, y, lz, bedrock, (byte) 0, usePalette);
                    } else if (finalDensity > 0) {
                        chunk.setBlock(lx, y, lz, stone, (byte) 0, usePalette);
                    } else if (isWater && baseDensity <= 0) {
                        chunk.setBlock(lx, y, lz, water, waterSourceState, usePalette);
                    }
                }
            }
        }
    }
    public MultiNoiseSampler getSampler() {
        return sampler;
    }

    public int getSolidTopY(int worldX, int worldZ) {
        Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
        float[] params = MultiNoiseBiomeRegistry.getBlendedTerrainParameters(climate);
        float biomeBaseHeight = params[0];
        float biomeVar = params[1];

        float cont = climate.continentalness;
        float erosion = climate.erosion;

        float baseHeight = biomeBaseHeight;
        if (cont > 0.0f) {
            float mountainFactor = cont * Math.max(0.0f, erosion);
            baseHeight += (mountainFactor * mountainFactor * 120.0f) * (biomeVar / 10.0f);
        } else {
            baseHeight += cont * biomeVar * 1.5f;
        }

        float jaggedness = (biomeVar * 0.15f) + (erosion * biomeVar * 0.8f);
        if (cont > 0.4f && erosion > 0.5f) {
            jaggedness += (cont - 0.4f) * (erosion - 0.5f) * 150.0f;
        }

        float warpX = riverNoise.getFractalNoise2D(worldX * 0.02f, worldZ * 0.02f, 2, 0.5f, 2.0f) * 25.0f;
        float warpZ = riverNoise.getFractalNoise2D(worldX * 0.02f + 100, worldZ * 0.02f + 100, 2, 0.5f, 2.0f) * 25.0f;

        float rNoise = riverNoise.getFractalNoise2D((worldX + warpX) * 0.003f, (worldZ + warpZ) * 0.003f, 3, 0.5f, 2.0f);
        float riverVal = Math.abs(rNoise);
        float riverThreshold = 0.06f;

        if (riverVal < riverThreshold) {
            float riverBlend = 1.0f - (riverVal / riverThreshold);
            riverBlend = riverBlend * riverBlend * (3.0f - 2.0f * riverBlend);
            baseHeight = (baseHeight * (1.0f - riverBlend)) + ((WATER_LEVEL - 3) * riverBlend);
            jaggedness *= (1.0f - riverBlend);
        }

        if (climate.continentalness < -0.2f) {
            float oceanFactor = Math.min(1.0f, (-0.2f - climate.continentalness) * 5.0f);
            baseHeight = (baseHeight * (1.0f - oceanFactor)) + (-24.0f * oceanFactor);
            jaggedness *= (1.0f - oceanFactor);
        }

        for (int y = de.delautrer.game.world.Chunk.MAX_Y - 1; y >= de.delautrer.game.world.Chunk.MIN_Y; y--) {
            float baseDensity = baseHeight - y;
            float noise3D = shapeNoise3D.getFractalNoise3D(worldX * 0.01f, y * 0.015f, worldZ * 0.01f, 3, 0.5f, 2.0f);
            baseDensity += noise3D * jaggedness;

            boolean isWater = (y <= WATER_LEVEL && baseDensity <= 0);
            if (riverVal < riverThreshold && y <= (WATER_LEVEL - 3)) isWater = true;

            if (!isWater && baseDensity > 0) {
                return y;
            }
        }
        return de.delautrer.game.world.Chunk.MIN_Y;
    }
}