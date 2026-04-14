package de.delautrer.game.world;

import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.blocks.WaterBlock;
import java.util.Random;

public class WorldGenerator {

    private final NoiseGenerator elevationNoise;
    private final NoiseGenerator roughnessNoise;
    private final NoiseGenerator detailNoise;
    private final NoiseGenerator grassNoise;
    private final long seed;

    private final BlockState air = BlockRegistry.AIR.getDefaultState();
    private final BlockState stone = BlockRegistry.STONE.getDefaultState();
    private final BlockState bedrock = BlockRegistry.BEDROCK.getDefaultState();
    private final BlockState grass_block = BlockRegistry.GRASS_BLOCK.getDefaultState();
    private final BlockState dirt = BlockRegistry.DIRT.getDefaultState();
    private final BlockState sand = BlockRegistry.SAND.getDefaultState();
    private final BlockState gravel = BlockRegistry.GRAVEL.getDefaultState();
    private final BlockState water = BlockRegistry.WATER.getDefaultState().with(WaterBlock.LEVEL, 8);
    private final BlockState grass = BlockRegistry.GRASS.getDefaultState();
    private final BlockState sandyGrass = BlockRegistry.SANDY_GRASS.getDefaultState();
    private final BlockState poppy = BlockRegistry.POPPY.getDefaultState();
    private final BlockState dandelion = BlockRegistry.DANDELION.getDefaultState();

    private final BlockState log = BlockRegistry.LOG.getDefaultState();
    private final BlockState leaves = BlockRegistry.LEAVES.getDefaultState();

    private Biome[][] biomeMap;

    private static final int WATER_LEVEL = 60;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.elevationNoise = new NoiseGenerator(seed);
        this.roughnessNoise = new NoiseGenerator(seed * 2);
        this.detailNoise = new NoiseGenerator(seed * 3);
        this.grassNoise = new NoiseGenerator(seed * 5);
    }

    public void generate(Chunk chunk) {
        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        int[][] heightMap = new int[Chunk.SIZE][Chunk.SIZE];
        biomeMap = new Biome[Chunk.SIZE][Chunk.SIZE];

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

        // --- 2. DIE ALPHA HÖHLEN ---
        carveCaves(chunk, heightMap);

        // --- 3. SMARTER SURFACE BUILDER ---
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int soilDepth = 0;
                boolean hasHitSurface = false;
                BlockState topMaterial = null;
                BlockState subMaterial = null;
                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                for (int y = Chunk.HEIGHT - 1; y >= 1; y--) {
                    byte currentBlockId = chunk.getBlock(x, y, z);

                    if (currentBlockId == stone.getBlock().getId()) {
                        hasHitSurface = true;
                        boolean isBeach = (y >= WATER_LEVEL - 2 && y <= WATER_LEVEL + 1);

                        if (soilDepth == 0) {
                            Biome b = biomeMap[x][z];
                            if (b == Biome.OCEAN && y < WATER_LEVEL) {
                                topMaterial = gravel; subMaterial = gravel;
                            } else if (isBeach) {
                                topMaterial = sand; subMaterial = sand;
                            } else if (y < WATER_LEVEL - 2) {
                                float floorDetail = detailNoise.getNoise(realX * 0.1f, realZ * 0.1f);
                                if (floorDetail > 0.2f) { topMaterial = gravel; subMaterial = gravel; }
                                else if (floorDetail < -0.2f) { topMaterial = sand; subMaterial = sand; }
                                else { topMaterial = dirt; subMaterial = dirt; }
                            } else {
                                topMaterial = b.surfaceBlock; subMaterial = b.subSurfaceBlock;
                            }
                            chunk.setBlock(x, y, z, topMaterial.getBlock().getId(), topMaterial.getStateId());
                        } else if (soilDepth < 4) {
                            chunk.setBlock(x, y, z, subMaterial.getBlock().getId(), subMaterial.getStateId());
                        }
                        soilDepth++;
                    } else if (currentBlockId == air.getBlock().getId() || currentBlockId == water.getBlock().getId()) {
                        if (hasHitSurface) break;
                    }
                }
            }
        }

        // --- 4. DEKORATION: BÄUME GENERIEREN ---
        long treeSeed = seed ^ ((long)worldX * 8934571L + (long)worldZ * 4392871L);
        Random treeRandom = new Random(treeSeed);

        // Bestimme, wie viele Bäume in diesem Chunk wachsen sollen
        int numTrees = 0;
        Biome centerBiome = biomeMap[Chunk.SIZE / 2][Chunk.SIZE / 2];

        // Hills haben mehr Bäume, Plains wenige, Ocean gar keine
        if (centerBiome == Biome.HILLS) numTrees = treeRandom.nextInt(5);
        else if (centerBiome == Biome.PLAINS) numTrees = treeRandom.nextInt(2);
        else if (centerBiome == Biome.MOUNTAINS && treeRandom.nextInt(3) == 0) numTrees = 1;

        for (int i = 0; i < numTrees; i++) {
            // Wir lassen einen kleinen Rand, damit die Bäume nicht zu hart abgeschnitten werden
            int tx = treeRandom.nextInt(Chunk.SIZE - 4) + 2;
            int tz = treeRandom.nextInt(Chunk.SIZE - 4) + 2;

            // Finde den höchsten Punkt
            int ty = -1;
            for (int y = Chunk.HEIGHT - 1; y >= WATER_LEVEL; y--) {
                if (chunk.getBlock(tx, y, tz) == grass_block.getBlock().getId()) {
                    ty = y + 1; // Baum spawnt AUF dem Gras
                    break;
                }
            }

            // Wenn wir einen validen Grasblock gefunden haben, bauen wir den Baum!
            if (ty != -1) {
                generateTree(chunk, treeRandom, tx, ty, tz);
            }
        }

        // --- 5. DEKORATION: FLORA (Gras, Sandgras & Blumen) ---
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                // Wir suchen von oben nach unten nach dem Boden
                for (int y = Chunk.HEIGHT - 2; y >= WATER_LEVEL; y--) {
                    byte blockAtPos = chunk.getBlock(x, y, z);

                    // Wir ignorieren Luft, Blätter und Holz, während wir nach unten scannen
                    if (blockAtPos == air.getBlock().getId() ||
                            blockAtPos == leaves.getBlock().getId() ||
                            blockAtPos == log.getBlock().getId()) {
                        continue;
                    }

                    // Wir brauchen zwingend Luft direkt über dem Boden, sonst können wir nichts pflanzen!
                    if (chunk.getBlock(x, y + 1, z) != air.getBlock().getId()) {
                        break;
                    }

                    // --- FALL 1: WIR SIND AUF DER WIESE ---
                    if (blockAtPos == grass_block.getBlock().getId()) {
                        float patchNoise = grassNoise.getNoise(realX * 0.15f, realZ * 0.15f);
                        float spawnChance = (patchNoise > 0.0f) ? 0.85f : 0.02f;

                        if (treeRandom.nextFloat() < spawnChance) {
                            // Pflanze wird gesetzt! Welche wird es?
                            float plantType = treeRandom.nextFloat();
                            BlockState plantToPlace;

                            if (plantType < 0.02f) {
                                plantToPlace = poppy;      // 2% Chance für Rose
                            } else if (plantType < 0.04f) {
                                plantToPlace = dandelion;  // 2% Chance für Löwenzahn
                            } else {
                                plantToPlace = grass;      // 96% Chance für normales Gras
                            }

                            chunk.setBlock(x, y + 1, z, plantToPlace.getBlock().getId(), plantToPlace.getStateId());
                        }
                        break; // Boden abgehandelt, weiter zur nächsten X/Z Koordinate
                    }

                    // --- FALL 2: WIR SIND AM STRAND ---
                    else if (blockAtPos == sand.getBlock().getId()) {
                        // Ein höherer Schwellenwert (0.2f) macht die Sand-Büschel viel kleiner als Gras-Büschel
                        float sandPatchNoise = grassNoise.getNoise(realX * 0.25f, realZ * 0.25f);

                        // Nur 25% Spawn-Chance in den kleinen Büscheln und winzige 0.5% außerhalb
                        float spawnChance = (sandPatchNoise > 0.2f) ? 0.25f : 0.005f;

                        if (treeRandom.nextFloat() < spawnChance) {
                            chunk.setBlock(x, y + 1, z, sandyGrass.getBlock().getId(), sandyGrass.getStateId());
                        }
                        break; // Boden abgehandelt
                    }

                    // --- FALL 3: STEIN, KIES ODER DRECK (Hier wächst nichts) ---
                    else {
                        break;
                    }
                }
            }
        }
    }

    // =====================================================================
    // TREE GENERATOR LOGIK
    // =====================================================================

    private void generateTree(Chunk chunk, Random random, int x, int y, int z) {
        int height = random.nextInt(3) + 4; // Stammhöhe: 4 bis 6 Blöcke

        // Sicherheitscheck, damit der Baum nicht oben aus der Welt wächst
        if (y + height + 2 >= Chunk.HEIGHT) return;

        // 1. BLÄTTERKRONE generieren
        // Die Krone fängt 2 Blöcke unter der Spitze an und ragt 1 Block über den Stamm hinaus
        int leafStart = y + height - 2;
        int leafEnd = y + height + 1;

        for (int ly = leafStart; ly <= leafEnd; ly++) {
            int layer = ly - leafStart; // Wird 0, 1, 2, 3

            // Die unteren zwei Schichten sind breit (Radius 2), die oberen zwei sind schmaler (Radius 1)
            int radius = (layer <= 1) ? 2 : 1;

            for (int lx = x - radius; lx <= x + radius; lx++) {
                for (int lz = z - radius; lz <= z + radius; lz++) {
                    int dx = Math.abs(lx - x);
                    int dz = Math.abs(lz - z);

                    // Wir runden die Ecken ab, damit die Krone wie eine Kugel wirkt
                    if (dx == radius && dz == radius) {
                        if (layer <= 1) {
                            // Untere breite Schichten: Ecken zufällig weglassen, für einen organischen Look
                            if (random.nextInt(2) == 0) continue;
                        } else if (layer == 3) {
                            // Allerhöchste Schicht: IMMER die Ecken weglassen (bildet ein Plus-Zeichen)
                            continue;
                        }
                        // Schicht 2 (die über dem breiten Teil) behält ihre Ecken (solides 3x3 Quadrat)
                    }

                    // Blätter nur setzen, wenn dort Luft ist (so wachsen Bäume auch dicht aneinander)
                    if (chunk.getBlock(lx, ly, lz) == air.getBlock().getId()) {
                        chunk.setBlock(lx, ly, lz, leaves.getBlock().getId(), leaves.getStateId());
                    }
                }
            }
        }

        for (int ty = 0; ty < height; ty++) {
            chunk.setBlock(x, y + ty, z, log.getBlock().getId(), log.getStateId());
        }
    }

    // =====================================================================
    // CAVE GENERATOR LOGIK
    // =====================================================================

    private void carveCaves(Chunk chunk, int[][] heightMap) {
        int currentChunkX = chunk.getWorldX();
        int currentChunkZ = chunk.getWorldZ();
        int range = 4;

        for (int cx = currentChunkX - range; cx <= currentChunkX + range; cx++) {
            for (int cz = currentChunkZ - range; cz <= currentChunkZ + range; cz++) {
                long randSeed = seed ^ ((long)cx * 341873128712L + (long)cz * 132897987541L);
                Random random = new Random(randSeed);

                if (random.nextInt(8) != 0) continue;

                int numNodes = random.nextInt(random.nextInt(random.nextInt(40) + 1) + 1) + 1;

                for (int i = 0; i < numNodes; i++) {
                    double startX = cx * Chunk.SIZE + random.nextInt(Chunk.SIZE);
                    double startY = random.nextInt(180) + 20;
                    double startZ = cz * Chunk.SIZE + random.nextInt(Chunk.SIZE);
                    int numWorms = 1;

                    if (random.nextInt(4) == 0) {
                        float radius = random.nextFloat() * 6.0f + 2.0f;
                        generateCaveNode(chunk, random, startX, startY, startZ, radius, 0, 0, 0, 0, heightMap);
                        numWorms += random.nextInt(4);
                    }

                    for (int j = 0; j < numWorms; j++) {
                        float yaw = random.nextFloat() * (float)Math.PI * 2.0f;
                        float pitch = (random.nextFloat() - 0.5f) * 2.0f / 8.0f;
                        float radius = random.nextFloat() * 2.5f + random.nextFloat() * 1.5f + 1.0f;
                        int length = random.nextInt(random.nextInt(150) + 40);

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
            if (dX * dX + dZ * dZ > (16 + currentRadius + 8) * (16 + currentRadius + 8)) continue;

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
                                if (by >= WATER_LEVEL - 2 && heightMap[bx][bz] <= WATER_LEVEL + 1) continue;
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