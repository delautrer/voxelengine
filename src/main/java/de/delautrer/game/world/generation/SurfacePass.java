package de.delautrer.game.world.generation;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Biome;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.NoiseGenerator;
import java.util.Map;

public class SurfacePass implements IGenerationPass {

    private final NoiseGenerator detailNoise;
    private final NoiseGenerator elevationNoise;
    private final NoiseGenerator underwaterBlobNoise;

    private final BlockState air;
    private final BlockState water;
    private final BlockState gravel;

    private static final int WATER_LEVEL = 60;

    public SurfacePass(long seed) {
        this.detailNoise = new NoiseGenerator(seed * 3);
        this.elevationNoise = new NoiseGenerator(seed);
        this.underwaterBlobNoise = new NoiseGenerator(seed * 7777L);

        this.air = BlockRegistry.get(Constants.NAMESPACE + ":air").getDefaultState();
        this.water = BlockRegistry.get(Constants.NAMESPACE + ":water").getDefaultState();
        this.gravel = BlockRegistry.get(Constants.NAMESPACE + ":gravel").getDefaultState();
    }

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
        if (chunk == null)
            return;

        int worldX = chunk.getWorldX();
        int worldZ = chunk.getWorldZ();

        // long chunkSeed = seed ^ ((long) worldX * 8934571L + (long) worldZ *
        // 4392871L);
        // Random random = new Random(chunkSeed);

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int soilDepth = 0;
                boolean hasHitSurface = false;
                BlockState topMaterial = null;
                BlockState subMaterial = null;

                float realX = (worldX * Chunk.SIZE + x);
                float realZ = (worldZ * Chunk.SIZE + z);

                Biome b = chunk.getBiome(x, z);
                if (b == null)
                    b = Biome.PLAINS; // Fallback

                float elevation = elevationNoise.getFractalNoise2D(realX * 0.0015f, realZ * 0.0015f, 4, 0.5f, 2.0f);
                boolean isRealCoastline = (elevation < -0.02f);

                BlockState deepMaterialState = b.getDeepMaterial();
                if (deepMaterialState == null)
                    continue; // Wenn das Biome keinen validen Block liefert, abbrechen
                byte deepMaterialId = deepMaterialState.getBlock().getId();

                for (int y = Chunk.HEIGHT - 1; y >= 1; y--) {
                    byte currentBlockId = chunk.getBlock(x, y, z);

                    if (currentBlockId == deepMaterialId) {
                        hasHitSurface = true;

                        if (soilDepth == 0) {
                            boolean isWaterLevel = (y >= WATER_LEVEL - 2 && y <= WATER_LEVEL + 1);

                            if (y < WATER_LEVEL) {
                                topMaterial = gravel;
                                subMaterial = gravel;

                                Map<String, Float> uwBlobs = b.getUnderwaterBlobs();
                                if (uwBlobs != null && !uwBlobs.isEmpty()) {
                                    float rawNoise = underwaterBlobNoise.getFractalNoise2D(
                                            realX * b.underwaterBlobScale, realZ * b.underwaterBlobScale, 2, 0.5f,
                                            2.0f);
                                    float normalizedNoise = Math.max(0.0f, Math.min(1.0f, (rawNoise + 1.0f) / 2.0f));

                                    float currentWeight = 0.0f;
                                    for (Map.Entry<String, Float> entry : uwBlobs.entrySet()) {
                                        currentWeight += entry.getValue();
                                        if (normalizedNoise <= currentWeight) {
                                            BlockState newMat = BlockRegistry
                                                    .get(Constants.NAMESPACE + ":" + entry.getKey()).getDefaultState();
                                            if (newMat != null) {
                                                topMaterial = newMat;
                                                subMaterial = newMat;
                                            }
                                            break;
                                        }
                                    }
                                }
                            } else if (isWaterLevel && isRealCoastline && b != Biome.MOUNTAINS) {
                                topMaterial = b.getBeachBlock();
                                subMaterial = b.getBeachBlock();
                            } else {
                                topMaterial = b.getSurfaceBlock();
                                subMaterial = b.getSubSurfaceBlock();
                            }

                            if (b == Biome.MOUNTAINS) {
                                float gravelNoise = detailNoise.getNoise(realX * 0.15f, realZ * 0.15f);
                                if (gravelNoise > 0.15f) {
                                    topMaterial = gravel;
                                    subMaterial = gravel;
                                }
                            }

                            // Letzter Sicherheitscheck, bevor geschrieben wird
                            if (topMaterial != null && subMaterial != null) {
                                chunk.setBlock(x, y, z, topMaterial.getBlock().getId(), topMaterial.getStateId());
                            }
                        } else if (soilDepth < 4) {
                            if (subMaterial != null) {
                                chunk.setBlock(x, y, z, subMaterial.getBlock().getId(), subMaterial.getStateId());
                            }
                        }
                        soilDepth++;
                    } else if (currentBlockId == air.getBlock().getId() || currentBlockId == water.getBlock().getId()) {
                        if (hasHitSurface)
                            break;
                    }
                }
            }
        }
    }
}