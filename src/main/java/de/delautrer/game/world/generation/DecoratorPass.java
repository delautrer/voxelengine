package de.delautrer.game.world.generation;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockProperties;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;

import java.util.Random;

public class DecoratorPass implements IGenerationPass {

    private NoiseGenerator grassNoise;

    private final BlockState air = BlockRegistry.get(Constants.NAMESPACE + ":air").getDefaultState();
    private final BlockState grass_block = BlockRegistry.get(Constants.NAMESPACE + ":grass_block").getDefaultState();
    private final BlockState sand = BlockRegistry.get(Constants.NAMESPACE + ":sand").getDefaultState();
    private final BlockState grass = BlockRegistry.get(Constants.NAMESPACE + ":grass").getDefaultState();
    private final BlockState sandyGrass = BlockRegistry.get(Constants.NAMESPACE + ":sandy_grass").getDefaultState();
    private final BlockState poppy = BlockRegistry.get(Constants.NAMESPACE + ":poppy").getDefaultState();
    private final BlockState dandelion = BlockRegistry.get(Constants.NAMESPACE + ":dandelion").getDefaultState();
    private final BlockState dotty = BlockRegistry.get(Constants.NAMESPACE + ":dotty").getDefaultState();
    private final BlockState fairy_bell = BlockRegistry.get(Constants.NAMESPACE + ":fairy_bell").getDefaultState();
    private final BlockState red_tulip = BlockRegistry.get(Constants.NAMESPACE + ":red_tulip").getDefaultState();
    private final BlockState purple_tulip = BlockRegistry.get(Constants.NAMESPACE + ":purple_tulip").getDefaultState();
    private final BlockState mavvinilia = BlockRegistry.get(Constants.NAMESPACE + ":mavvinilia").getDefaultState();

    private final BlockState uprightLog = BlockRegistry.get(Constants.NAMESPACE + ":log").getDefaultState().with(de.delautrer.game.blocks.LogBlock.AXIS, BlockProperties.Axis.Y);
    private final BlockState leaves = BlockRegistry.get(Constants.NAMESPACE + ":leaves").getDefaultState();

    private static final int WATER_LEVEL = 60;

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (grassNoise == null) {
            grassNoise = new NoiseGenerator(seed * 5);
        }

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        // --- 1. BÄUME GENERIEREN ---
        long treeSeed = seed ^ ((long)worldX * 8934571L + (long)worldZ * 4392871L);
        Random treeRandom = new Random(treeSeed);

        // Wir prüfen das Biome in der Mitte des Chunks für die grobe Baumdichte
        Biome centerBiome = chunk.getBiome(Chunk.SIZE / 2, Chunk.SIZE / 2);
        int numTrees = 0;

        switch (centerBiome) {
            case FOREST:
                numTrees = treeRandom.nextInt(6) + 2; // 2 bis 7 Bäume pro Chunk (Dichter Wald)
                break;
            case PLAINS:
            case HILLS:
                numTrees = treeRandom.nextInt(2); // 0 bis 1 Baum (Offene Ebene)
                break;
            case MOUNTAINS:
                if (treeRandom.nextInt(4) == 0) numTrees = 1; // Sehr selten ein Baum
                break;
            case DESERT:
            case OCEAN:
                numTrees = 0; // Keine Bäume in der Wüste oder im Ozean
                break;
        }

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

        // --- 2. FLORA GENERIEREN ---
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                Biome localBiome = chunk.getBiome(x, z);

                for (int y = Chunk.HEIGHT - 2; y >= WATER_LEVEL; y--) {
                    byte blockAtPos = chunk.getBlock(x, y, z);

                    if (blockAtPos == air.getBlock().getId() || blockAtPos == leaves.getBlock().getId() || blockAtPos == uprightLog.getBlock().getId()) continue;
                    if (chunk.getBlock(x, y + 1, z) != air.getBlock().getId()) break;

                    // GRAS & BLUMEN (Nur in Plains, Forest, Hills)
                    if (blockAtPos == grass_block.getBlock().getId()) {
                        float patchNoise = grassNoise.getNoise(realX * 0.15f, realZ * 0.15f);

                        // In Plains spawnen deutlich mehr Blumen als im Wald
                        float spawnChance = (patchNoise > 0.0f) ? (localBiome == Biome.PLAINS ? 0.95f : 0.85f) : 0.02f;

                        if (treeRandom.nextFloat() < spawnChance) {
                            float plantType = treeRandom.nextFloat();
                            BlockState plantToPlace;

                            if (plantType < 0.0075f)        plantToPlace = dotty;
                            else if (plantType < 0.015f)    plantToPlace = fairy_bell;
                            else if (plantType < 0.02f)     plantToPlace = poppy;
                            else if (plantType < 0.04f)     plantToPlace = red_tulip;
                            else if (plantType < 0.055f)    plantToPlace = purple_tulip;
                            else if (plantType < 0.07f)     plantToPlace = mavvinilia;
                            else if (plantType < 0.08f)     plantToPlace = dandelion;
                            else plantToPlace = grass;

                            chunk.setBlock(x, y + 1, z, plantToPlace.getBlock().getId(), plantToPlace.getStateId());
                        }
                        break;
                    }
                    // WÜSTEN-VEGETATION
                    else if (blockAtPos == sand.getBlock().getId() && localBiome == Biome.DESERT) {
                        float sandPatchNoise = grassNoise.getNoise(realX * 0.25f, realZ * 0.25f);
                        float spawnChance = (sandPatchNoise > 0.2f) ? 0.05f : 0.001f;

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
            chunk.setBlock(x, y + ty, z, uprightLog.getBlock().getId(), uprightLog.getStateId());
        }
    }
}