package de.delautrer.game.world.generation.biome;

import de.delautrer.game.world.Chunk;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.Constants;
import java.util.Random;

public class CaveCarver {
/*
    public static void carve(Chunk chunk, long seed) {
        int cx = chunk.getWorldX();
        int cz = chunk.getWorldZ();
        byte air = 0;
        byte waterId = BlockRegistry.get(Constants.NAMESPACE + ":water").getId();
        byte stoneId = BlockRegistry.get(Constants.NAMESPACE + ":stone").getId();

        // 8 Chunks Radius für fließende Übergänge
        int range = 8;
        for (int ox = -range; ox <= range; ox++) {
            for (int oz = -range; oz <= range; oz++) {
                int oX = cx + ox;
                int oZ = cz + oz;
                long cSeed = seed ^ ((long) oX * 341873128712L ^ (long) oZ * 132897987541L);
                Random rand = new Random(cSeed);

                if (rand.nextInt(15) != 0) continue; // Chance pro Chunk

                int numCaves = rand.nextInt(rand.nextInt(rand.nextInt(15) + 1) + 1);
                for (int i = 0; i < numCaves; i++) {
                    double x = oX * Chunk.SIZE + rand.nextInt(Chunk.SIZE);
                    double y = rand.nextInt(120); // Alpha/1.2.5 Höhe
                    double z = oZ * Chunk.SIZE + rand.nextInt(Chunk.SIZE);

                    int nodes = 112 - rand.nextInt(28);
                    float yaw = rand.nextFloat() * (float) Math.PI * 2.0f;
                    float pitch = (rand.nextFloat() - 0.5f) * 2.0f / 8.0f;
                    float thickness = rand.nextFloat() * 2.0f + rand.nextFloat();

                    carveWorm(chunk, cx, cz, rand, x, y, z, thickness, yaw, pitch, 0, nodes, waterId, stoneId, air);
                }
            }
        }
    }

    private static void carveWorm(Chunk chunk, int cx, int cz, Random rand, double x, double y, double z, float thick, float yaw, float pitch, int start, int nodes, byte water, byte stone, byte air) {
        double cMidX = cx * Chunk.SIZE + 8;
        double cMidZ = cz * Chunk.SIZE + 8;

        for (int i = start; i < nodes; i++) {
            double w = 1.5 + Math.sin(i * Math.PI / nodes) * thick;
            double h = w * 0.8;

            x += Math.cos(yaw) * Math.cos(pitch);
            y += Math.sin(pitch);
            z += Math.sin(yaw) * Math.cos(pitch);

            pitch *= 0.92f;
            pitch += (rand.nextFloat() - rand.nextFloat()) * 0.1f;
            yaw += (rand.nextFloat() - rand.nextFloat()) * 0.1f;

            // Verzweigungen
            if (i == nodes / 2 && rand.nextInt(4) == 0) {
                carveWorm(chunk, cx, cz, rand, x, y, z, thick, yaw - 1.0f, pitch, i, nodes, water, stone, air);
                carveWorm(chunk, cx, cz, rand, x, y, z, thick, yaw + 1.0f, pitch, i, nodes, water, stone, air);
                return;
            }

            if (rand.nextInt(4) == 0) continue;

            if (x < cMidX - 16 - w*2 || x > cMidX + 16 + w*2 || z < cMidZ - 16 - w*2 || z > cMidZ + 16 + w*2) continue;

            int minX = Math.max(0, (int) (x - w) - cx * Chunk.SIZE);
            int maxX = Math.min(Chunk.SIZE - 1, (int) (x + w) - cx * Chunk.SIZE);
            int minY = Math.max(3, (int) (y - h)); // Bedrock Schutz
            int maxY = Math.min(Chunk.HEIGHT - 1, (int) (y + h));
            int minZ = Math.max(0, (int) (z - w) - cz * Chunk.SIZE);
            int maxZ = Math.min(Chunk.SIZE - 1, (int) (z + w) - cz * Chunk.SIZE);

            for (int lx = minX; lx <= maxX; lx++) {
                double dX = ((lx + cx * Chunk.SIZE) + 0.5 - x) / w;
                for (int lz = minZ; lz <= maxZ; lz++) {
                    double dZ = ((lz + cz * Chunk.SIZE) + 0.5 - z) / w;
                    if (dX * dX + dZ * dZ >= 1.0) continue;

                    for (int ly = maxY; ly >= minY; ly--) {
                        double dY = (ly + 0.5 - y) / h;
                        if (dX * dX + dY * dY + dZ * dZ < 1.0) {
                            if (ly + 1 < Chunk.HEIGHT && chunk.getBlock(lx, ly + 1, lz) == water) continue; // Fliegendes Wasser verhindern
                            if (chunk.getBlock(lx, ly, lz) == stone) chunk.setBlock(lx, ly, lz, air);
                        }
                    }
                }
            }
        }
    }
*/
}