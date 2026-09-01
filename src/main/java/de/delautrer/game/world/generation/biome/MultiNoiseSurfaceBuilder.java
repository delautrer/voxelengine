package de.delautrer.game.world.generation.biome;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.registry.NamespacedKey;
import de.delautrer.game.registry.Registries;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import de.delautrer.game.world.generation.feature.ConfiguredFeature;
import de.delautrer.game.world.generation.feature.FeatureRegistry;
import de.delautrer.game.world.persistence.WorldPalette;
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

    public void buildSurface(Chunk chunk, int chunkX, int chunkZ, de.delautrer.game.world.WorldGenerator wg) {
        WorldPalette palette = (wg != null) ? wg.getBlockPalette() : chunk.getPalette();
        Block air = Registries.BLOCKS.get("veinstride:air");
        Block water = Registries.BLOCKS.get("veinstride:water");
        Block sand = Registries.BLOCKS.get("veinstride:sand");
        Block dirt = Registries.BLOCKS.get("veinstride:dirt");
        Block stone = Registries.BLOCKS.get("veinstride:stone");
        Block grassBlock = Registries.BLOCKS.get("veinstride:grass_block");
        Block grass = Registries.BLOCKS.get("veinstride:grass");
        Block sandyGrass = Registries.BLOCKS.get("veinstride:sandy_grass");

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

                Block topBlock = biome.getTopBlock();
                Block underBlock = biome.getUnderBlock();
                Block underwaterBlock = biome.getUnderwaterBlock();
                Block deepBlock = biome.getDeepBlock();

                if ("veinstride:savanna".equals(biome.id)) {
                    float sNoise = blobNoise.getFractalNoise2D(worldX * 0.1f, worldZ * 0.1f, 2, 0.5f, 2.0f);
                    if (sNoise > 0) {
                        topBlock = sand;
                        underBlock = sand;
                    } else {
                        topBlock = dirt;
                        underBlock = dirt;
                    }
                }

                int currentSurfaceDepth = (topBlock == sand) ? (5 + random.nextInt(6)) : (3 + random.nextInt(2));
                if (topBlock == sand)
                    deepBlock = stone;

                boolean isUnderwater = false;
                boolean foundAbsoluteTop = false;
                int solidRun = -1;
                int expectedSurfaceY = myHeight;
                Block columnUnderBlock = underBlock;

                for (int y = Chunk.MAX_Y - 1; y >= Chunk.MIN_Y; y--) {
                    Block currentBlock = chunk.getBlock(lx, y, lz, palette);

                    if (currentBlock == air) {
                        solidRun = -1;
                        isUnderwater = false;
                        continue;
                    }

                    if (currentBlock == water) {
                        solidRun = -1;
                        isUnderwater = true;
                        continue;
                    }

                    if (currentBlock == stone) {
                        if (solidRun == -1) {
                            if (!foundAbsoluteTop) {
                                solidRun = 0;
                                foundAbsoluteTop = true;
                                heightCache[lx * Chunk.SIZE + lz] = y;

                                int depth = expectedSurfaceY - y;
                                // Tief in Höhle? (Falls Roof fehlt) -> Stein
                                if (depth > 12) {
                                    chunk.setBlock(lx, y, lz, deepBlock, (byte) 0, palette);
                                    solidRun = 999;
                                    continue;
                                }

                                if (isUnderwater) {
                                    Block currentBottom = underwaterBlock;
                                    if (biome.underwaterBlobs != null && !biome.underwaterBlobs.isEmpty()) {
                                        float bottomN = blobNoise.getFractalNoise2D(worldX * 0.08f, worldZ * 0.08f, 2, 0.5f, 2.0f);
                                        for (java.util.Map.Entry<String, Float> entry : biome.underwaterBlobs.entrySet()) {
                                            if (Math.abs(bottomN) > entry.getValue()) {
                                                Block patch = Registries.BLOCKS.get("veinstride:" + entry.getKey());
                                                if (patch != null) { currentBottom = patch; break; }
                                            }
                                        }
                                    }
                                    chunk.setBlock(lx, y, lz, currentBottom, (byte) 0, palette);
                                    columnUnderBlock = currentBottom;
                                } else {
                                    Block currentTop = topBlock;
                                    columnUnderBlock = underBlock;

                                    if (isNearWater && y <= MultiNoiseChunkGenerator.WATER_LEVEL + 2) {
                                        currentTop = biome.getShoreBlock();
                                        if (currentTop != grassBlock) columnUnderBlock = currentTop;
                                    }

                                    if (biome.surfaceBlobs != null && !biome.surfaceBlobs.isEmpty()) {
                                        float patchN = patchNoise.getFractalNoise2D(worldX * 0.06f, worldZ * 0.06f, 3, 0.5f, 2.0f);
                                        for (java.util.Map.Entry<String, Float> entry : biome.surfaceBlobs.entrySet()) {
                                            if (Math.abs(patchN) > entry.getValue()) {
                                                Block patch = Registries.BLOCKS.get("veinstride:" + entry.getKey());
                                                if (patch != null) {
                                                    currentTop = patch;
                                                    columnUnderBlock = patch;
                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    chunk.setBlock(lx, y, lz, currentTop, (byte) 0, palette);
                                    if (currentTop == grassBlock && y - 1 >= Chunk.MIN_Y && chunk.getBlock(lx, y - 1, lz, palette) == grassBlock) {
                                        chunk.setBlock(lx, y - 1, lz, dirt, (byte) 0, palette);
                                    }

                                    // FLORA (NUR OBEN)
                                    if (y + 1 < Chunk.MAX_Y && chunk.getBlock(lx, y + 1, lz, palette) == air) {
                                        if (currentTop == sand) {
                                            if (random.nextFloat() < 0.05f) chunk.setBlock(lx, y + 1, lz, sandyGrass, (byte) 0, palette);
                                        } else if (biome.floraProbability > 0 || "veinstride:savanna".equals(biome.id)) {
                                            float patchN = patchNoise.getFractalNoise2D(worldX * 0.12f, worldZ * 0.12f, 2, 0.5f, 2.0f);
                                            float patchThreshold = (biome.floraPatchThreshold != 0) ? biome.floraPatchThreshold : -0.4f;

                                            if (patchN > patchThreshold) {
                                                float density = (biome.floraDensity != 0) ? biome.floraDensity : 0.8f;
                                                if (random.nextFloat() < (density * (biome.floraProbability > 0 ? biome.floraProbability * 2.0f : 1.0f))) {
                                                    Block floraToSet = grass;
                                                    float flowerChance = "veinstride:flower_plains".equals(biome.id) ? 0.6f : 0.45f;
                                                    if (random.nextFloat() < flowerChance) {
                                                        String flowerName = WeightedRandomHelper.getRandom(biome.flora, random);
                                                        if (flowerName != null) {
                                                            Block fl = Registries.BLOCKS.get("veinstride:" + flowerName);
                                                            if (fl != null) floraToSet = fl;
                                                        }
                                                    }
                                                    chunk.setBlock(lx, y + 1, lz, floraToSet, (byte) 0, palette);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // BEREITS UNTER DER OBERFLÄCHE -> STEIN
                                solidRun = 999;
                                chunk.setBlock(lx, y, lz, deepBlock, (byte) 0, palette);
                                continue;
                            }
                        } else if (solidRun < currentSurfaceDepth) {
                            solidRun++;
                            chunk.setBlock(lx, y, lz, columnUnderBlock, (byte) 0, palette);
                        } else {
                            Block currentDeep = deepBlock;
                            if (biome.undergroundBlobs != null && !biome.undergroundBlobs.isEmpty()) {
                                float blobN = blobNoise.getFractalNoise3D(worldX * 0.04f, y * 0.04f, worldZ * 0.04f, 2, 0.5f, 2.0f);
                                for (java.util.Map.Entry<String, Float> entry : biome.undergroundBlobs.entrySet()) {
                                    if (Math.abs(blobN) > (1.0f - entry.getValue())) {
                                        Block blob = Registries.BLOCKS.get("veinstride:" + entry.getKey());
                                        if (blob != null) { currentDeep = blob; break; }
                                    }
                                }
                            }
                            chunk.setBlock(lx, y, lz, currentDeep, (byte) 0, palette);
                        }
                    }
                }
            }
        }
        // --- BÄUME ---
        // Wir suchen in einem Radius von 16 um den Chunk nach Baum-Startpunkten,
        // um sicherzustellen, dass überlappende Kronen (z. B. Baobab, Fichten, Mahagoni)
        // in allen benachbarten Chunks vollständig und deterministisch generiert werden.
        int searchRadius = 16;
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
                            Block topBlock = biome.getTopBlock();
                            Block sandBlock = Registries.BLOCKS.get("veinstride:sand");
                            Block dirtBlock = Registries.BLOCKS.get("veinstride:dirt");
                            
                            if (topBlock == grassBlock || topBlock == sandBlock || topBlock == dirtBlock) {
                                String treeFeatureKeyStr = WeightedRandomHelper.getRandom(biome.trees, checkRandom);
                                if (treeFeatureKeyStr != null) {
                                    NamespacedKey featKey = NamespacedKey.fromString(treeFeatureKeyStr);
                                    ConfiguredFeature feature = FeatureRegistry.getConfiguredFeature(featKey);
                                    if (feature == null) {
                                        System.err.println("Configured feature missing: " + featKey);
                                    } else {
                                        feature.generate(chunk, wg, worldX, surfaceY + 1, worldZ, seed);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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