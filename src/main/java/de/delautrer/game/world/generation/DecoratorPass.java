package de.delautrer.game.world.generation;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;

import java.util.Random;

public class DecoratorPass implements IGenerationPass {

    private NoiseGenerator grassNoise;

    private final BlockState air = BlockRegistry.AIR.getDefaultState();
    private final BlockState grass_block = BlockRegistry.GRASS_BLOCK.getDefaultState();
    private final BlockState sand = BlockRegistry.SAND.getDefaultState();
    private final BlockState grass = BlockRegistry.GRASS.getDefaultState();
    private final BlockState sandyGrass = BlockRegistry.SANDY_GRASS.getDefaultState();
    private final BlockState poppy = BlockRegistry.POPPY.getDefaultState();
    private final BlockState dandelion = BlockRegistry.DANDELION.getDefaultState();
    private final BlockState dotty = BlockRegistry.DOTTY.getDefaultState();
    private final BlockState fairy_bell = BlockRegistry.FAIRY_BELL.getDefaultState();
    private final BlockState red_tulip = BlockRegistry.RED_TULIP.getDefaultState();
    private final BlockState purple_tulip = BlockRegistry.PURPLE_TULIP.getDefaultState();

    private final BlockState log = BlockRegistry.LOG.getDefaultState();
    private final BlockState leaves = BlockRegistry.LEAVES.getDefaultState();

    private static final int WATER_LEVEL = 60;

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (grassNoise == null) {
            grassNoise = new NoiseGenerator(seed * 5);
        }

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        // --- 1. BÄUME ---
        long treeSeed = seed ^ ((long)worldX * 8934571L + (long)worldZ * 4392871L);
        Random treeRandom = new Random(treeSeed);

        int numTrees = 0;
        Biome centerBiome = chunk.getBiome(Chunk.SIZE / 2, Chunk.SIZE / 2);

        if (centerBiome == Biome.HILLS) numTrees = treeRandom.nextInt(5);
        else if (centerBiome == Biome.PLAINS) numTrees = treeRandom.nextInt(2);
        else if (centerBiome == Biome.MOUNTAINS && treeRandom.nextInt(3) == 0) numTrees = 1;

        for (int i = 0; i < numTrees; i++) {
            int tx = treeRandom.nextInt(Chunk.SIZE - 4) + 2;
            int tz = treeRandom.nextInt(Chunk.SIZE - 4) + 2;

            int ty = -1;
            for (int y = Chunk.HEIGHT - 1; y >= WATER_LEVEL; y--) {
                if (chunk.getBlock(tx, y, tz) == grass_block.getBlock().getId()) {
                    ty = y + 1;
                    break;
                }
            }
            if (ty != -1) {
                generateTree(chunk, treeRandom, tx, ty, tz);
            }
        }

        // --- 2. FLORA ---
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                for (int y = Chunk.HEIGHT - 2; y >= WATER_LEVEL; y--) {
                    byte blockAtPos = chunk.getBlock(x, y, z);

                    if (blockAtPos == air.getBlock().getId() || blockAtPos == leaves.getBlock().getId() || blockAtPos == log.getBlock().getId()) continue;
                    if (chunk.getBlock(x, y + 1, z) != air.getBlock().getId()) break;

                    if (blockAtPos == grass_block.getBlock().getId()) {
                        float patchNoise = grassNoise.getNoise(realX * 0.15f, realZ * 0.15f);
                        float spawnChance = (patchNoise > 0.0f) ? 0.85f : 0.02f;

                        if (treeRandom.nextFloat() < spawnChance) {
                            float plantType = treeRandom.nextFloat();
                            BlockState plantToPlace;

                            if (plantType < 0.0075f)     plantToPlace = dotty;
                            else if (plantType < 0.015f) plantToPlace = fairy_bell;
                            else if (plantType < 0.02f) plantToPlace = poppy;
                            else if (plantType < 0.04f) plantToPlace = red_tulip;
                            else if (plantType < 0.055f) plantToPlace = purple_tulip;
                            else if (plantType < 0.07f) plantToPlace = dandelion;
                            else plantToPlace = grass;

                            chunk.setBlock(x, y + 1, z, plantToPlace.getBlock().getId(), plantToPlace.getStateId());
                        }
                        break;
                    } else if (blockAtPos == sand.getBlock().getId()) {
                        float sandPatchNoise = grassNoise.getNoise(realX * 0.25f, realZ * 0.25f);
                        float spawnChance = (sandPatchNoise > 0.2f) ? 0.25f : 0.005f;

                        if (treeRandom.nextFloat() < spawnChance) {
                            chunk.setBlock(x, y + 1, z, sandyGrass.getBlock().getId(), sandyGrass.getStateId());
                        }
                        break;
                    } else {
                        break;
                    }
                }
            }
        }
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
}