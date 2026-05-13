package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.Constants;
import de.delautrer.game.world.NoiseGenerator;
import java.util.Map;
import java.util.Random;

public class MultiNoiseSurfaceBuilder {

    private final MultiNoiseSampler sampler;
    private final NoiseGenerator patchNoise;
    private final NoiseGenerator blobNoise;
    private final NoiseGenerator shapeNoise3D;
    private final NoiseGenerator riverNoise;
    private final long seed;

    public MultiNoiseSurfaceBuilder(MultiNoiseSampler sampler, long seed) {
        this.sampler = sampler;
        this.seed = seed;
        this.patchNoise = new NoiseGenerator(seed * 99);
        this.blobNoise = new NoiseGenerator(seed * 123);
        this.shapeNoise3D = new NoiseGenerator(seed * 31);
        this.riverNoise = new NoiseGenerator(seed * 234);
    }

    public void buildSurface(Chunk chunk, int chunkX, int chunkZ) {
        byte air = 0;
        byte waterId = BlockRegistry.get(Constants.NAMESPACE + ":water").getId();
        byte sandId = BlockRegistry.get(Constants.NAMESPACE + ":sand").getId();
        byte dirtId = BlockRegistry.get(Constants.NAMESPACE + ":dirt").getId();
        byte stoneId = BlockRegistry.get(Constants.NAMESPACE + ":stone").getId();
        byte grassId = BlockRegistry.get(Constants.NAMESPACE + ":grass").getId();
        byte sandyGrassId = BlockRegistry.get(Constants.NAMESPACE + ":sandy_grass").getId();

        long chunkSeed = seed ^ ((long) chunkX * 73128712L ^ (long) chunkZ * 12897541L);
        Random random = new Random(chunkSeed);

        int[] heightCache = new int[Chunk.SIZE * Chunk.SIZE];

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {
                int worldX = chunkX * Chunk.SIZE + lx;
                int worldZ = chunkZ * Chunk.SIZE + lz;

                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                Biome biome = MultiNoiseBiomeRegistry.getBiomeFor(climate);
                if (biome == null) continue;

                chunk.setBiome(lx, lz, biome);

                // --- DYNAMISCHE STRAND-BREITE ---
                float beachNoise = patchNoise.getFractalNoise2D(worldX * 0.05f, worldZ * 0.05f, 2, 0.5f, 2.0f);
                int beachRadius = 1 + (int) ((beachNoise + 1.0f) * 3.5f); 
                
                boolean isNearWater = false;
                int myHeight = getExpectedHeight(worldX, worldZ);
                if (myHeight <= MultiNoiseChunkGenerator.WATER_LEVEL) {
                    isNearWater = true;
                } else {
                    for (int r = 1; r <= beachRadius; r += 2) {
                        if (getExpectedHeight(worldX + r, worldZ) <= MultiNoiseChunkGenerator.WATER_LEVEL ||
                            getExpectedHeight(worldX - r, worldZ) <= MultiNoiseChunkGenerator.WATER_LEVEL ||
                            getExpectedHeight(worldX, worldZ + r) <= MultiNoiseChunkGenerator.WATER_LEVEL ||
                            getExpectedHeight(worldX, worldZ - r) <= MultiNoiseChunkGenerator.WATER_LEVEL) {
                            isNearWater = true;
                            break;
                        }
                    }
                }

                int solidBlocksHit = 0;
                boolean isUnderwater = false;
                boolean surfaceFound = false;
                boolean forceSand = false;

                byte topBlock = biome.getTopBlockId();
                byte underBlock = biome.getUnderBlockId();
                byte underwaterBlock = biome.getUnderwaterBlockId();
                byte deepBlock = biome.getDeepBlockId();

                if ("SAVANNA".equals(biome.id)) {
                    float sNoise = blobNoise.getFractalNoise2D(worldX * 0.1f, worldZ * 0.1f, 2, 0.5f, 2.0f);
                    if (sNoise > 0) {
                        topBlock = sandId;
                        underBlock = sandId;
                    } else {
                        topBlock = dirtId;
                        underBlock = dirtId;
                    }
                }

                int currentSurfaceDepth;
                if (topBlock == sandId) {
                    currentSurfaceDepth = 5 + random.nextInt(6);
                    deepBlock = stoneId;
                } else {
                    currentSurfaceDepth = 3 + random.nextInt(2);
                }

                for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
                    byte blockId = chunk.getBlock(lx, y, lz);

                    // Skip Air & Reset solidBlocksHit
                    if (blockId == air) {
                        continue;
                    }

                    if (blockId == waterId && !surfaceFound) {
                        isUnderwater = true;
                        continue;
                    }

                    if (blockId == stoneId) {
                        if (!surfaceFound) {
                            // ERSTER TREFFER = OBERFLÄCHE
                            surfaceFound = true;
                            heightCache[lx * Chunk.SIZE + lz] = y;

                            if (isUnderwater) {
                                chunk.setBlock(lx, y, lz, underwaterBlock);
                            } else {
                                byte currentTop = topBlock;
                                if (isNearWater && y <= MultiNoiseChunkGenerator.WATER_LEVEL + 2) {
                                    currentTop = sandId;
                                    forceSand = true;
                                    currentSurfaceDepth = 3 + random.nextInt(2);
                                }

                                chunk.setBlock(lx, y, lz, currentTop);

                                // --- FLORA LOGIK ---
                                if (currentTop == sandId) {
                                    if (random.nextFloat() < 0.05f && y + 1 < Chunk.MAX_Y && chunk.getBlock(lx, y + 1, lz) == air) {
                                        chunk.setBlock(lx, y + 1, lz, sandyGrassId);
                                    }
                                } else if (biome.floraProbability > 0 || "SAVANNA".equals(biome.id)) {
                                    float patchN = patchNoise.getFractalNoise2D(worldX * 0.12f, worldZ * 0.12f, 2, 0.5f, 2.0f);
                                    float patchThreshold = (biome.floraPatchThreshold != 0) ? biome.floraPatchThreshold : -0.4f;

                                    if (patchN > patchThreshold) {
                                        float density = (biome.floraDensity != 0) ? biome.floraDensity : 0.8f;
                                        if (random.nextFloat() < (density * (biome.floraProbability > 0 ? biome.floraProbability * 2.0f : 1.0f))) {
                                            if (y + 1 < Chunk.MAX_Y && chunk.getBlock(lx, y + 1, lz) == air) {
                                                byte floraToSet = grassId;
                                                float flowerChance = "FLOWER_PLAINS".equals(biome.id) ? 0.6f : 0.45f;

                                                if (random.nextFloat() < flowerChance) {
                                                    String flowerName = WeightedRandomHelper.getRandom(biome.flora, random);
                                                    if (flowerName != null) floraToSet = BlockRegistry.get(Constants.NAMESPACE + ":" + flowerName).getId();
                                                }
                                                chunk.setBlock(lx, y + 1, lz, floraToSet);
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // WIR SIND UNTER DER OBERFLÄCHE
                            int depth = heightCache[lx * Chunk.SIZE + lz] - y;
                            if (depth < currentSurfaceDepth) {
                                // DIRT SCHICHT UNTER OBERFLÄCHE
                                byte currentUnder = isUnderwater ? underwaterBlock : (forceSand ? sandId : underBlock);
                                chunk.setBlock(lx, y, lz, currentUnder);
                            } else {
                                // TIEFE HÖHLE -> BLEIBT DEEPBLOCK (Oder Stein)
                                chunk.setBlock(lx, y, lz, deepBlock);
                            }
                        }
                    }
                }
            }
        }

        // BÄUME PASS
        for (int ox = -6; ox < Chunk.SIZE + 6; ox++) {
            for (int oz = -6; oz < Chunk.SIZE + 6; oz++) {
                int worldX = chunkX * Chunk.SIZE + ox;
                int worldZ = chunkZ * Chunk.SIZE + oz;
                if (worldX % 2 != 0 || worldZ % 2 != 0) continue;

                long treeSeed = seed ^ ((long) worldX * 341873128712L ^ (long) worldZ * 132897987541L);
                Random checkRandom = new Random(treeSeed);
                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                Biome biome = MultiNoiseBiomeRegistry.getBiomeFor(climate);

                if (biome != null && biome.treeProbability > 0) {
                    if (checkRandom.nextFloat() < biome.treeProbability) {
                        int surfaceY = (ox >= 0 && ox < Chunk.SIZE && oz >= 0 && oz < Chunk.SIZE) ? heightCache[ox * Chunk.SIZE + oz] : getExpectedHeight(worldX, worldZ);
                        if (surfaceY != -1 && surfaceY > MultiNoiseChunkGenerator.WATER_LEVEL + 2) {
                            if (ox >= 0 && ox < Chunk.SIZE && oz >= 0 && oz < Chunk.SIZE) {
                                if (chunk.getBlock(ox, surfaceY + 1, oz) == waterId) continue;
                            }
                            String treeType = WeightedRandomHelper.getRandom(biome.trees, checkRandom);
                            byte[] ids = getIdsForTree(treeType);
                            if (ids != null) {
                                TreeFeature.generate(chunk, worldX, surfaceY + 1, worldZ, seed, treeType, ids[0], ids[1]);
                            }
                        }
                    }
                }
            }
        }
    }

    private byte[] getIdsForTree(String treeType) {
        String base = treeType.replace("alpha_tall_", "alpha_").replace("alpha_", "");
        byte logId = BlockRegistry.get(Constants.NAMESPACE + ":" + base + "_log").getId();
        byte leavesId = BlockRegistry.get(Constants.NAMESPACE + ":" + base + "_leaves").getId();
        if (logId != 0 && leavesId != 0) return new byte[]{logId, leavesId};
        return null;
    }

    private int getExpectedHeight(int worldX, int worldZ) {
        Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
        float[] params = MultiNoiseBiomeRegistry.getBlendedTerrainParameters(climate);
        float biomeBaseHeight = params[0];
        float biomeVar = params[1];

        float baseHeight = biomeBaseHeight + (climate.continentalness * biomeVar * 1.5f);
        float jaggedness = (biomeVar * 0.15f) + (climate.erosion * biomeVar * 0.25f);

        // --- FLUSS CARVE SYNC ---
        float warpX = riverNoise.getFractalNoise2D(worldX * 0.02f, worldZ * 0.02f, 2, 0.5f, 2.0f) * 25.0f;
        float warpZ = riverNoise.getFractalNoise2D(worldX * 0.02f + 100, worldZ * 0.02f + 100, 2, 0.5f, 2.0f) * 25.0f;

        float rNoise = riverNoise.getFractalNoise2D((worldX + warpX) * 0.003f, (worldZ + warpZ) * 0.003f, 3, 0.5f, 2.0f);
        float riverVal = Math.abs(rNoise);
        float riverThreshold = 0.06f;

        if (riverVal < riverThreshold) {
            float riverBlend = 1.0f - (riverVal / riverThreshold);
            riverBlend = riverBlend * riverBlend * (3.0f - 2.0f * riverBlend);
            baseHeight = (baseHeight * (1.0f - riverBlend)) + ((MultiNoiseChunkGenerator.WATER_LEVEL - 3) * riverBlend);
            jaggedness *= (1.0f - riverBlend);
        }

        if (climate.continentalness < -0.2f) {
            float oceanFactor = Math.min(1.0f, (-0.2f - climate.continentalness) * 5.0f);
            baseHeight = (baseHeight * (1.0f - oceanFactor)) + (-24.0f * oceanFactor);
            jaggedness *= (1.0f - oceanFactor);
        }

        for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
            float density = baseHeight - y;
            float noise3D = shapeNoise3D.getFractalNoise3D(worldX * 0.01f, y * 0.015f, worldZ * 0.01f, 3, 0.5f, 2.0f);
            density += noise3D * jaggedness;
            if (density > 0) return y;
        }
        return Chunk.MIN_Y;
    }
}