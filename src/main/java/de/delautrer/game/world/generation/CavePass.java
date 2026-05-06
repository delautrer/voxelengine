package de.delautrer.game.world.generation;

import de.delautrer.game.blocks.state.BlockState;
import de.delautrer.game.world.Chunk;
import java.util.Random;
import de.delautrer.Constants;
import de.delautrer.game.registry.Registries;

public class CavePass implements IGenerationPass {

    private final BlockState stone = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "stone").getDefaultState();
    private final BlockState air = Registries.BLOCKS.get(Constants.NAMESPACE + ":" + "air").getDefaultState();
    private static final int WATER_LEVEL = 60;

    @Override
    public void process(Chunk chunk, long seed, int[][] heightMap) {
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
