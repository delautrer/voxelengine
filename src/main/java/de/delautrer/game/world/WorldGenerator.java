package de.delautrer.game.world;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.WaterBlock;
import java.util.Random;

public class WorldGenerator {

    private final NoiseGenerator elevationNoise;
    private final NoiseGenerator roughnessNoise;
    private final NoiseGenerator detailNoise;
    private final long seed;

    private final BlockState air = BlockRegistry.AIR.getDefaultState();
    private final BlockState stone = BlockRegistry.STONE.getDefaultState();
    private final BlockState bedrock = BlockRegistry.BEDROCK.getDefaultState();
    private final BlockState grass = BlockRegistry.GRASS.getDefaultState();
    private final BlockState dirt = BlockRegistry.DIRT.getDefaultState();
    private final BlockState sand = BlockRegistry.SAND.getDefaultState();
    private final BlockState gravel = BlockRegistry.GRAVEL.getDefaultState(); // NEU: Kies
    private final BlockState water = BlockRegistry.WATER.getDefaultState().with(WaterBlock.LEVEL, 8);

    private static final int WATER_LEVEL = 60;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.elevationNoise = new NoiseGenerator(seed);
        this.roughnessNoise = new NoiseGenerator(seed * 2);
        this.detailNoise = new NoiseGenerator(seed * 3);
    }

    public void generate(Chunk chunk) {
        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        int[][] heightMap = new int[Chunk.SIZE][Chunk.SIZE];
        Biome[][] biomeMap = new Biome[Chunk.SIZE][Chunk.SIZE];

        // --- 1. KONTINUIERLICHES TERRAIN ---
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                float elevation = elevationNoise.getFractalNoise2D(realX * 0.0015f, realZ * 0.0015f, 4, 0.5f, 2.0f);
                float baseHeight = 64.0f + (elevation * 80.0f);

                float roughness = roughnessNoise.getFractalNoise2D(realX * 0.002f, realZ * 0.002f, 4, 0.5f, 2.0f);
                float localRoughness = Math.max(0, roughness + 0.2f) * 120.0f;

                float flattenFactor = Math.max(0.0f, Math.min(1.0f, (baseHeight - 50.0f) / 15.0f));
                localRoughness *= flattenFactor;

                // Biom für den Surface Builder merken
                if (elevation < -0.15f) biomeMap[x][z] = Biome.OCEAN;
                else if (elevation < 0.05f) biomeMap[x][z] = Biome.PLAINS;
                else if (roughness > 0.1f) biomeMap[x][z] = Biome.MOUNTAINS;
                else biomeMap[x][z] = Biome.HILLS;

                float detail = detailNoise.getFractalNoise2D(realX * 0.01f, realZ * 0.01f, 4, 0.5f, 2.0f);

                int terrainHeight = (int) (baseHeight + (detail * localRoughness));
                terrainHeight = Math.min(Chunk.HEIGHT - 2, Math.max(1, terrainHeight));
                heightMap[x][z] = terrainHeight;

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

        // --- 2. DIE ALPHA HÖHLEN (Breiter & mehr Eingänge) ---
        carveCaves(chunk, heightMap);

        // --- 3. SMARTER SURFACE BUILDER ---
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int soilDepth = 0;
                boolean hasHitSurface = false;

                // Wir speichern das Material ab, sobald wir die Oberfläche berühren!
                BlockState topMaterial = null;
                BlockState subMaterial = null;

                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                for (int y = Chunk.HEIGHT - 1; y >= 1; y--) {
                    byte currentBlockId = chunk.getBlock(x, y, z);

                    if (currentBlockId == stone.getBlock().getId()) {
                        hasHitSurface = true;

                        if (soilDepth == 0) {
                            // WIR HABEN DIE OBERFLÄCHE GETROFFEN! Material bestimmen:
                            Biome b = biomeMap[x][z];

                            if (b == Biome.OCEAN && y < WATER_LEVEL) {
                                // Tiefer Ozeanboden ist immer Gravel
                                topMaterial = gravel;
                                subMaterial = gravel;
                            } else if (y >= WATER_LEVEL - 2 && y <= WATER_LEVEL + 1) {
                                // Strand an der Küste
                                topMaterial = sand;
                                subMaterial = sand;
                            } else if (y < WATER_LEVEL - 2) {
                                // Flüsse / Seen (Unter Wasser im Landesinneren)
                                // Nutzt einen kleinen Noise, um fleckige Flussbetten zu erzeugen!
                                float floorDetail = detailNoise.getNoise(realX * 0.1f, realZ * 0.1f);
                                if (floorDetail > 0.2f) { topMaterial = gravel; subMaterial = gravel; }
                                else if (floorDetail < -0.2f) { topMaterial = sand; subMaterial = sand; }
                                else { topMaterial = dirt; subMaterial = dirt; }
                            } else {
                                // Normales Grasland/Berge
                                topMaterial = b.surfaceBlock;
                                subMaterial = b.subSurfaceBlock;
                            }

                            chunk.setBlock(x, y, z, topMaterial.getBlock().getId(), topMaterial.getStateId());
                        } else if (soilDepth < 4) {
                            // Die 3 Blöcke UNTER der Oberfläche nutzen stur das festgelegte Sub-Material!
                            chunk.setBlock(x, y, z, subMaterial.getBlock().getId(), subMaterial.getStateId());
                        }
                        soilDepth++;
                    } else if (currentBlockId == air.getBlock().getId() || currentBlockId == water.getBlock().getId()) {
                        if (hasHitSurface) {
                            break; // Höhle getroffen! Keine Erde im Inneren generieren.
                        }
                    }
                }
            }
        }
    }

    // =====================================================================
    // ALPHA CAVE GENERATOR LOGIK (BREITER & MEHR EINGÄNGE)
    // =====================================================================

    private void carveCaves(Chunk chunk, int[][] heightMap) {
        int currentChunkX = chunk.getWorldX();
        int currentChunkZ = chunk.getWorldZ();
        int range = 4;

        for (int cx = currentChunkX - range; cx <= currentChunkX + range; cx++) {
            for (int cz = currentChunkZ - range; cz <= currentChunkZ + range; cz++) {

                long randSeed = seed ^ ((long)cx * 341873128712L + (long)cz * 132897987541L);
                Random random = new Random(randSeed);

                // 1 in 8 Chunks spawnt Würmer
                if (random.nextInt(8) != 0) continue;

                int numNodes = random.nextInt(random.nextInt(random.nextInt(40) + 1) + 1) + 1;

                for (int i = 0; i < numNodes; i++) {
                    double startX = cx * Chunk.SIZE + random.nextInt(Chunk.SIZE);

                    // WICHTIGER FIX: Würmer spawnen jetzt gleichmäßig zwischen Y=20 und Y=200!
                    // Dadurch durchbrechen sie ständig die Hügel und Berge!
                    double startY = random.nextInt(180) + 20;

                    double startZ = cz * Chunk.SIZE + random.nextInt(Chunk.SIZE);

                    int numWorms = 1;

                    // Große Höhlenräume (Rooms)
                    if (random.nextInt(4) == 0) {
                        float radius = random.nextFloat() * 6.0f + 2.0f; // Vorher 1-5, jetzt 2-8 für riesige Hallen
                        generateCaveNode(chunk, random, startX, startY, startZ, radius, 0, 0, 0, 0, heightMap);
                        numWorms += random.nextInt(4);
                    }

                    for (int j = 0; j < numWorms; j++) {
                        float yaw = random.nextFloat() * (float)Math.PI * 2.0f;
                        float pitch = (random.nextFloat() - 0.5f) * 2.0f / 8.0f;

                        // WICHTIGER FIX: Höhlenradien erhöht. Viel breitere Tunnel!
                        float radius = random.nextFloat() * 2.5f + random.nextFloat() * 1.5f + 1.0f;
                        int length = random.nextInt(random.nextInt(150) + 40); // Längere Würmer

                        generateCaveNode(chunk, random, startX, startY, startZ, radius, yaw, pitch, 0, length, heightMap);
                    }
                }
            }
        }
    }

    private void generateCaveNode(Chunk chunk, Random random, double x, double y, double z, float radius, float yaw, float pitch, int startStep, int length, int[][] heightMap) {
        int currentChunkX = chunk.getWorldX();
        int currentChunkZ = chunk.getWorldZ();

        boolean isRoom = false;
        if (length == 0) {
            length = 1;
            isRoom = true;
        }

        for (int step = startStep; step < length; step++) {
            if (!isRoom) {
                x += Math.cos(yaw) * Math.cos(pitch);
                y += Math.sin(pitch);
                z += Math.sin(yaw) * Math.cos(pitch);

                pitch *= 0.9f;
                yaw += (random.nextFloat() - 0.5f) * 0.5f;
                pitch += (random.nextFloat() - 0.5f) * 0.5f;

                if (random.nextInt(4) == 0) continue;
            }

            float currentRadius = radius + (random.nextFloat() - 0.5f) * 0.5f;

            double dX = x - (currentChunkX * Chunk.SIZE + 8);
            double dZ = z - (currentChunkZ * Chunk.SIZE + 8);
            if (dX * dX + dZ * dZ > (16 + currentRadius + 8) * (16 + currentRadius + 8)) {
                continue;
            }

            int minX = Math.max(0, (int)(x - currentRadius) - currentChunkX * Chunk.SIZE);
            int maxX = Math.min(Chunk.SIZE - 1, (int)(x + currentRadius) - currentChunkX * Chunk.SIZE);
            int minY = Math.max(1, (int)(y - currentRadius));
            int maxY = Math.min(Chunk.HEIGHT - 1, (int)(y + currentRadius));
            int minZ = Math.max(0, (int)(z - currentRadius) - currentChunkZ * Chunk.SIZE);
            int maxZ = Math.min(Chunk.SIZE - 1, (int)(z + currentRadius) - currentChunkZ * Chunk.SIZE);

            for (int bx = minX; bx <= maxX; bx++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    double distX = (currentChunkX * Chunk.SIZE + bx + 0.5) - x;
                    double distZ = (currentChunkZ * Chunk.SIZE + bz + 0.5) - z;

                    if (distX * distX + distZ * distZ < currentRadius * currentRadius) {
                        for (int by = minY; by <= maxY; by++) {
                            double distY = (by + 0.5) - y;

                            if (distX * distX + distY * distY * 2.0 + distZ * distZ < currentRadius * currentRadius) {

                                if (by >= WATER_LEVEL - 2 && heightMap[bx][bz] <= WATER_LEVEL + 1) {
                                    continue;
                                }

                                byte blockId = chunk.getBlock(bx, by, bz);
                                if (blockId == stone.getBlock().getId()) {
                                    chunk.setBlock(bx, by, bz, air.getBlock().getId(), air.getStateId());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}