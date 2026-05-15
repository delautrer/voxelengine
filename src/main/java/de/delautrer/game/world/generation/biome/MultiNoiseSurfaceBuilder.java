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
        byte grassBlockId = BlockRegistry.get(Constants.NAMESPACE + ":grass_block").getId();
        byte grassId = BlockRegistry.get(Constants.NAMESPACE + ":grass").getId();
        byte sandyGrassId = BlockRegistry.get(Constants.NAMESPACE + ":sandy_grass").getId();

        long chunkSeed = seed ^ ((long) chunkX * 73128712L ^ (long) chunkZ * 12897541L);
        Random random = new Random(chunkSeed);

        int[] heightCache = new int[Chunk.SIZE * Chunk.SIZE];
        for (int i = 0; i < heightCache.length; i++)
            heightCache[i] = -1;

        for (int lx = 0; lx < Chunk.SIZE; lx++) {
            for (int lz = 0; lz < Chunk.SIZE; lz++) {
                int worldX = chunkX * Chunk.SIZE + lx;
                int worldZ = chunkZ * Chunk.SIZE + lz;

                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                Biome biome = MultiNoiseBiomeRegistry.getBiomeFor(climate);
                if (biome == null)
                    continue;

                chunk.setBiome(lx, lz, biome);

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

                int currentSurfaceDepth = (topBlock == sandId) ? (5 + random.nextInt(6)) : (3 + random.nextInt(2));
                if (topBlock == sandId)
                    deepBlock = stoneId;

                boolean isUnderwater = false;
                boolean foundAbsoluteTop = false;
                int solidRun = -1;
                int expectedSurfaceY = myHeight;
                byte columnUnderBlock = underBlock;

                for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
                    byte blockId = chunk.getBlock(lx, y, lz);

                    if (blockId == air) {
                        solidRun = -1;
                        isUnderwater = false;
                        continue;
                    }

                    if (blockId == waterId) {
                        solidRun = -1;
                        isUnderwater = true;
                        continue;
                    }

                    if (blockId == stoneId) {
                        if (solidRun == -1) {
                            if (!foundAbsoluteTop) {
                                solidRun = 0;
                                foundAbsoluteTop = true;
                                heightCache[lx * Chunk.SIZE + lz] = y;

                                int depth = expectedSurfaceY - y;
                                // Tief in Höhle? (Falls Roof fehlt) -> Stein
                                if (depth > 12) {
                                    chunk.setBlock(lx, y, lz, deepBlock);
                                    solidRun = 999;
                                    continue;
                                }

                                if (isUnderwater) {
                                    byte currentBottom = underwaterBlock;
                                    if (biome.underwaterBlobs != null && !biome.underwaterBlobs.isEmpty()) {
                                        float bottomN = blobNoise.getFractalNoise2D(worldX * 0.08f, worldZ * 0.08f, 2, 0.5f, 2.0f);
                                        for (java.util.Map.Entry<String, Float> entry : biome.underwaterBlobs.entrySet()) {
                                            if (Math.abs(bottomN) > entry.getValue()) {
                                                byte patchId = BlockRegistry.get(Constants.NAMESPACE + ":" + entry.getKey()).getId();
                                                if (patchId != 0) { currentBottom = patchId; break; }
                                            }
                                        }
                                    }
                                    chunk.setBlock(lx, y, lz, currentBottom);
                                    columnUnderBlock = currentBottom;
                                } else {
                                    byte currentTop = topBlock;
                                    columnUnderBlock = underBlock;

                                    if (isNearWater && y <= MultiNoiseChunkGenerator.WATER_LEVEL + 2) {
                                        currentTop = biome.getShoreBlockId();
                                        if (currentTop != grassBlockId) columnUnderBlock = currentTop;
                                    }

                                    if (biome.surfaceBlobs != null && !biome.surfaceBlobs.isEmpty()) {
                                        float patchN = patchNoise.getFractalNoise2D(worldX * 0.06f, worldZ * 0.06f, 3, 0.5f, 2.0f);
                                        for (java.util.Map.Entry<String, Float> entry : biome.surfaceBlobs.entrySet()) {
                                            if (Math.abs(patchN) > entry.getValue()) {
                                                byte patchId = BlockRegistry.get(Constants.NAMESPACE + ":" + entry.getKey()).getId();
                                                if (patchId != 0) {
                                                    currentTop = patchId;
                                                    columnUnderBlock = patchId;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    chunk.setBlock(lx, y, lz, currentTop);
                                    if (currentTop == grassBlockId && y - 1 >= Chunk.MIN_Y && chunk.getBlock(lx, y - 1, lz) == grassBlockId) {
                                        chunk.setBlock(lx, y - 1, lz, dirtId);
                                    }

                                    // FLORA (NUR OBEN)
                                    if (y + 1 < Chunk.MAX_Y && chunk.getBlock(lx, y + 1, lz) == air) {
                                        if (currentTop == sandId) {
                                            if (random.nextFloat() < 0.05f) chunk.setBlock(lx, y + 1, lz, sandyGrassId);
                                        } else if (biome.floraProbability > 0 || "SAVANNA".equals(biome.id)) {
                                            float patchN = patchNoise.getFractalNoise2D(worldX * 0.12f, worldZ * 0.12f, 2, 0.5f, 2.0f);
                                            float patchThreshold = (biome.floraPatchThreshold != 0) ? biome.floraPatchThreshold : -0.4f;

                                            if (patchN > patchThreshold) {
                                                float density = (biome.floraDensity != 0) ? biome.floraDensity : 0.8f;
                                                if (random.nextFloat() < (density * (biome.floraProbability > 0 ? biome.floraProbability * 2.0f : 1.0f))) {
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
                                // BEREITS UNTER DER OBERFLÄCHE -> STEIN
                                solidRun = 999;
                                chunk.setBlock(lx, y, lz, deepBlock);
                                continue;
                            }
                        } else if (solidRun < currentSurfaceDepth) {
                            solidRun++;
                            chunk.setBlock(lx, y, lz, columnUnderBlock);
                        } else {
                            byte currentDeep = deepBlock;
                            if (biome.undergroundBlobs != null && !biome.undergroundBlobs.isEmpty()) {
                                float blobN = blobNoise.getFractalNoise3D(worldX * 0.04f, y * 0.04f, worldZ * 0.04f, 2, 0.5f, 2.0f);
                                for (java.util.Map.Entry<String, Float> entry : biome.undergroundBlobs.entrySet()) {
                                    if (Math.abs(blobN) > (1.0f - entry.getValue())) {
                                        byte blobId = BlockRegistry.get(Constants.NAMESPACE + ":" + entry.getKey()).getId();
                                        if (blobId != 0) { currentDeep = blobId; break; }
                                    }
                                }
                            }
                            chunk.setBlock(lx, y, lz, currentDeep);
                        }
                    }
                }
            }
        }
        // --- BÄUME ---
        // Wir suchen in einem Radius von 8 um den Chunk nach Baum-Startpunkten,
        // um sicherzustellen, dass überlappende Kronen korrekt generiert werden.
        int searchRadius = 8;
        for (int ox = -searchRadius; ox < Chunk.SIZE + searchRadius; ox++) {
            for (int oz = -searchRadius; oz < Chunk.SIZE + searchRadius; oz++) {
                int worldX = chunkX * Chunk.SIZE + ox;
                int worldZ = chunkZ * Chunk.SIZE + oz;
                
                // Deterministischer Seed für diesen Punkt
                long treeSeed = seed ^ ((long) worldX * 341873128712L ^ (long) worldZ * 132897987541L);
                Random checkRandom = new Random(treeSeed);
                
                // Wir platzieren Bäume nur an jedem 2. Block (Schachbrett/Raster) um zu dichte Wälder zu vermeiden
                // und die Performance zu schonen.
                if (worldX % 2 != 0 || worldZ % 2 != 0) continue;

                Climate.TargetPoint climate = sampler.sample(worldX, worldZ);
                Biome biome = MultiNoiseBiomeRegistry.getBiomeFor(climate);

                if (biome != null && biome.treeProbability > 0) {
                    if (checkRandom.nextFloat() < biome.treeProbability) {
                        // WICHTIG: getExpectedHeight ist deterministisch für jeden Punkt in der Welt!
                        int surfaceY = getExpectedHeight(worldX, worldZ);

                        if (surfaceY != -1 && surfaceY > MultiNoiseChunkGenerator.WATER_LEVEL + 2) {
                            // DETERMINISTISCHER CHECK:
                            // Statt im Chunk nachzuschauen (was bei Nachbar-Chunks nicht geht),
                            // prüfen wir das Biome und die theoretische Beschaffenheit.
                            byte topBlock = biome.getTopBlockId();
                            
                            // Check ob der Boden an dieser Stelle geeignet ist
                            if (topBlock == grassBlockId || topBlock == sandId || topBlock == dirtId) {
                                String treeType = WeightedRandomHelper.getRandom(biome.trees, checkRandom);
                                byte[] ids = getIdsForTree(treeType);
                                if (ids != null) {
                                    // Diese Methode setzt nur Blöcke, die WIRKLICH in unseren Chunk fallen.
                                    TreeFeature.generate(chunk, worldX, surfaceY + 1, worldZ, seed, treeType, ids[0], ids[1]);
                                }
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
        if (logId != 0 && leavesId != 0)
            return new byte[] { logId, leavesId };
        return null;
    }

    private int getExpectedHeight(int worldX, int worldZ) {
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

        float rNoise = riverNoise.getFractalNoise2D((worldX + warpX) * 0.003f, (worldZ + warpZ) * 0.003f, 3, 0.5f,
                2.0f);
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

        // Optimierung: Wir starten die Suche nicht ganz oben, sondern etwas über der theoretischen Basishöhe.
        // Jaggedness kann das Terrain nach oben drücken, daher nehmen wir einen Puffer.
        int startY = Math.min(Chunk.MAX_Y - 1, (int) baseHeight + (int) Math.abs(jaggedness * 2.0f) + 16);
        
        for (int y = startY; y >= Chunk.MIN_Y; y--) {
            float density = baseHeight - y;
            float noise3D = shapeNoise3D.getFractalNoise3D(worldX * 0.01f, y * 0.015f, worldZ * 0.01f, 3, 0.5f, 2.0f);
            density += noise3D * jaggedness;
            if (density > 0)
                return y;
        }
        return Chunk.MIN_Y;
    }
}