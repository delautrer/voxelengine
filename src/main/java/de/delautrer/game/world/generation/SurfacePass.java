package de.delautrer.game.world.generation;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;

public class SurfacePass implements IGenerationPass {

    private NoiseGenerator detailNoise;

    private final BlockState stone = BlockRegistry.get(Constants.NAMESPACE + ":stone").getDefaultState();
    private final BlockState air = BlockRegistry.get(Constants.NAMESPACE + ":air").getDefaultState();
    private final BlockState water = BlockRegistry.get(Constants.NAMESPACE + ":water").getDefaultState();
    private final BlockState dirt = BlockRegistry.get(Constants.NAMESPACE + ":dirt").getDefaultState();
    private final BlockState sand = BlockRegistry.get(Constants.NAMESPACE + ":sand").getDefaultState();
    private final BlockState gravel = BlockRegistry.get(Constants.NAMESPACE + ":gravel").getDefaultState();

    private static final int WATER_LEVEL = 60;

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (detailNoise == null) {
            detailNoise = new NoiseGenerator(seed * 3);
        }

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

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
                            Biome b = chunk.getBiome(x, z);

                            // 1. Ozeanboden & Strände
                            if (b == Biome.OCEAN && y < WATER_LEVEL) {
                                topMaterial = gravel; subMaterial = gravel;
                            } else if (isBeach) {
                                topMaterial = sand; subMaterial = sand;
                            }
                            // 2. Unterwasser-Struktur
                            else if (y < WATER_LEVEL - 2) {
                                float floorDetail = detailNoise.getNoise(realX * 0.1f, realZ * 0.1f);
                                if (floorDetail > 0.2f) { topMaterial = gravel; subMaterial = gravel; }
                                else if (floorDetail < -0.2f) { topMaterial = sand; subMaterial = sand; }
                                else { topMaterial = dirt; subMaterial = dirt; }
                            }
                            // 3. Biome-spezifische Oberfläche (Nutzt nun die Getter!)
                            else {
                                topMaterial = b.getSurfaceBlock();
                                subMaterial = b.getSubSurfaceBlock();
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
    }
}