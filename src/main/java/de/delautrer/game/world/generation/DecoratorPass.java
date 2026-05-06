package de.delautrer.game.world.generation;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockProperties;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import java.util.*;
import de.delautrer.game.blocks.LogBlock;

public class DecoratorPass implements IGenerationPass {

    private final NoiseGenerator floraNoise;
    private final NoiseGenerator blobNoise;

    private final BlockState air;
    private final BlockState leaves;
    private final BlockState uprightLog;
    private final BlockState water;
    private final byte gravelId;

    private static final int WATER_LEVEL = 60;

    public DecoratorPass(long seed) {
        this.floraNoise = new NoiseGenerator(seed * 5);
        this.blobNoise = new NoiseGenerator(seed * 8888L);
        this.air = BlockRegistry.get(Constants.NAMESPACE + ":air").getDefaultState();
        this.leaves = BlockRegistry.get(Constants.NAMESPACE + ":leaves").getDefaultState();
        this.uprightLog = BlockRegistry.get(Constants.NAMESPACE + ":log").getDefaultState().with(LogBlock.AXIS, BlockProperties.Axis.Y);
        this.water = BlockRegistry.get(Constants.NAMESPACE + ":water").getDefaultState();
        this.gravelId = BlockRegistry.get(Constants.NAMESPACE + ":gravel").getId();
    }

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (chunk == null) return;

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        long chunkSeed = seed ^ ((long)worldX * 8934571L + (long)worldZ * 4392871L);
        Random random = new Random(chunkSeed);

        Biome centerBiome = chunk.getBiome(Chunk.SIZE / 2, Chunk.SIZE / 2);
        if (centerBiome == null) return;

        int numTrees = 0;
        float treeChance = centerBiome.treeChance;

        if (treeChance >= 1.0f) {
            numTrees = (int) treeChance;
            numTrees += random.nextInt(3) - 1;
            if (random.nextFloat() < (treeChance - (int)treeChance)) numTrees++;
        } else {
            if (random.nextFloat() < treeChance) numTrees = 1;
        }
        numTrees = Math.max(0, numTrees);

        for (int i = 0; i < numTrees; i++) {
            // Bäume nicht ganz am Rand setzen, um Overflows abzumildern
            int tx = random.nextInt(Chunk.SIZE - 4) + 2;
            int tz = random.nextInt(Chunk.SIZE - 4) + 2;

            Biome localBiome = chunk.getBiome(tx, tz);
            if (localBiome == Biome.MOUNTAINS || localBiome == Biome.FLOWER_PLAINS || localBiome == null) {
                continue;
            }

            int ty = -1;
            for (int y = Chunk.HEIGHT - 1; y >= WATER_LEVEL; y--) {
                byte blockId = chunk.getBlock(tx, y, tz);

                BlockState surfaceState = localBiome.getSurfaceBlock();
                if (surfaceState == null) continue;

                byte surfaceId = surfaceState.getBlock().getId();
                if (blockId == surfaceId) {
                    ty = y + 1;
                    break;
                }
            }
            if (ty != -1) {
                boolean tooClose = false;
                for (int checkX = tx - 1; checkX <= tx + 1; checkX++) {
                    for (int checkZ = tz - 1; checkZ <= tz + 1; checkZ++) {
                        if (checkX >= 0 && checkX < Chunk.SIZE && checkZ >= 0 && checkZ < Chunk.SIZE) {
                            if (chunk.getBlock(checkX, ty, checkZ) == uprightLog.getBlock().getId()) {
                                tooClose = true;
                                break;
                            }
                        }
                    }
                }

                if (!tooClose) {
                    generateTree(chunk, random, tx, ty, tz);
                }
            }
        }

        List<String> flowerPlainsSelection = new ArrayList<>();
        if (centerBiome == Biome.FLOWER_PLAINS) {
            Map<String, Integer> floraWeights = centerBiome.getFloraWeights();
            if (floraWeights != null && !floraWeights.isEmpty()) {
                List<String> availableFlowers = new ArrayList<>(floraWeights.keySet());
                Collections.shuffle(availableFlowers, random);
                flowerPlainsSelection = availableFlowers.subList(0, Math.min(3, availableFlowers.size()));
            }
        }

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                Biome localBiome = chunk.getBiome(x, z);

                if (localBiome == null || localBiome == Biome.MOUNTAINS || localBiome.getFloraWeights() == null || localBiome.getFloraWeights().isEmpty() || localBiome.floraDensity <= 0.0f) continue;

                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);
                int soilDepth = 0;

                for (int y = Chunk.HEIGHT - 2; y >= 1; y--) {
                    byte blockAtPos = chunk.getBlock(x, y, z);

                    if (blockAtPos == uprightLog.getBlock().getId()) break;

                    if (blockAtPos == air.getBlock().getId() || blockAtPos == leaves.getBlock().getId() || blockAtPos == water.getBlock().getId()) {
                        continue;
                    }

                    if (soilDepth == 0 && y >= WATER_LEVEL) {
                        BlockState surfaceState = localBiome.getSurfaceBlock();
                        if (surfaceState == null) continue;
                        byte surfaceId = surfaceState.getBlock().getId();

                        if (blockAtPos == surfaceId) {
                            float patchNoise = floraNoise.getNoise(realX * 0.15f, realZ * 0.15f);

                            if (patchNoise > localBiome.floraPatchThreshold) {
                                if (random.nextFloat() < localBiome.floraDensity) {

                                    String plantName = null;
                                    if (localBiome == Biome.FLOWER_PLAINS && !flowerPlainsSelection.isEmpty()) {
                                        int patchTypeIndex = Math.abs((int)(patchNoise * 100)) % flowerPlainsSelection.size();
                                        if (random.nextBoolean()) {
                                            plantName = "grass";
                                        } else {
                                            plantName = flowerPlainsSelection.get(patchTypeIndex);
                                        }
                                    } else {
                                        plantName = getRandomWeightedPlant(localBiome.getFloraWeights(), random);
                                    }

                                    if (plantName != null) {
                                        BlockState plantToPlace = BlockRegistry.get(Constants.NAMESPACE + ":" + plantName).getDefaultState();
                                        if (plantToPlace != null) {
                                            chunk.setBlock(x, y + 1, z, plantToPlace.getBlock().getId(), plantToPlace.getStateId());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else if (soilDepth > 4) {
                        BlockState deepState = localBiome.getDeepMaterial();
                        if (deepState != null) {
                            byte deepMaterialId = deepState.getBlock().getId();
                            if (blockAtPos == deepMaterialId) {
                                float bNoise = blobNoise.getFractalNoise3D(realX * 0.1f, y * 0.1f, realZ * 0.1f, 2, 0.5f, 2.0f);
                                if (bNoise > 0.5f) {
                                    Map<String, Float> blobs = localBiome.getUndergroundBlobs();
                                    if (blobs != null) {
                                        for (Map.Entry<String, Float> entry : blobs.entrySet()) {
                                            if (random.nextFloat() < entry.getValue()) {
                                                BlockState blobBlock = BlockRegistry.get(Constants.NAMESPACE + ":" + entry.getKey()).getDefaultState();
                                                if (blobBlock != null) {
                                                    chunk.setBlock(x, y, z, blobBlock.getBlock().getId(), blobBlock.getStateId());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    soilDepth++;
                }
            }
        }
    }

    private String getRandomWeightedPlant(Map<String, Integer> weights, Random random) {
        if (weights == null || weights.isEmpty()) return null;

        int totalWeight = 0;
        for (int w : weights.values()) {
            totalWeight += w;
        }
        if (totalWeight <= 0) return null;

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomWeight < currentWeight) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void generateTree(Chunk chunk, Random random, int x, int y, int z) {
        int height = random.nextInt(3) + 4;
        if (y + height + 2 >= Chunk.HEIGHT) return;

        int leafStart = y + height - 2;
        int leafEnd = y + height + 1;

        for (int ly = leafStart; ly <= leafEnd; ly++) {
            int layer = ly - leafStart;
            int radius = (layer <= 1) ? 2 : 1;

            for (int lx = x - radius; lx <= x + radius; lx++) {
                for (int lz = z - radius; lz <= z + radius; lz++) {
                    int dx = Math.abs(lx - x);
                    int dz = Math.abs(lz - z);

                    if (dx == radius && dz == radius) {
                        if (layer <= 1 && random.nextInt(2) == 0) continue;
                        else if (layer == 3) continue;
                    }

                    // BOUNDS CHECK: Verhindert Absturz, wenn der Baum in Nachbarchunks wachsen will!
                    if (lx >= 0 && lx < Chunk.SIZE && lz >= 0 && lz < Chunk.SIZE) {
                        if (chunk.getBlock(lx, ly, lz) == air.getBlock().getId()) {
                            chunk.setBlock(lx, ly, lz, leaves.getBlock().getId(), leaves.getStateId());
                        }
                    }
                }
            }
        }

        for (int ty = 0; ty < height; ty++) {
            // BOUNDS CHECK (Eigentlich durch die x,z Limits am Anfang schon sicher, aber schadet nicht)
            if (x >= 0 && x < Chunk.SIZE && z >= 0 && z < Chunk.SIZE) {
                chunk.setBlock(x, y + ty, z, uprightLog.getBlock().getId(), uprightLog.getStateId());
            }
        }
    }
}
