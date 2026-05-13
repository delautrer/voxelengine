package de.delautrer.game.world.generation.biome;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkSection;
import java.util.Random;

public class CaveCarver {

    private static byte AIR = 0;
    private static byte WATER = 0;
    private static byte LAVA = 0;
    private static byte BEDROCK = 0;

    private static boolean initialized = false;
    private static boolean[] whitelist = new boolean[256];

    private static void init() {
        if (initialized) return;
        AIR = BlockRegistry.get(Constants.NAMESPACE + ":air").getId();
        WATER = BlockRegistry.get(Constants.NAMESPACE + ":water").getId();
        LAVA = BlockRegistry.get(Constants.NAMESPACE + ":lava").getId();
        BEDROCK = BlockRegistry.get(Constants.NAMESPACE + ":bedrock").getId();

        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":stone").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":dirt").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":grass_block").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":gravel").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":sand").getId() & 0xFF] = true;

        initialized = true;
    }

    public static void carve(Chunk targetChunk, long worldSeed) {
        init();
        int chunkX = targetChunk.getWorldX();
        int chunkZ = targetChunk.getWorldZ();

        int radius = 8;

        for (int nx = chunkX - radius; nx <= chunkX + radius; nx++) {
            for (int nz = chunkZ - radius; nz <= chunkZ + radius; nz++) {
                long neighborSeed = ((long) nx * 341873128712L) ^ ((long) nz * 132897987541L) ^ worldSeed;
                Random rand = new Random(neighborSeed);

                if (rand.nextInt(5) != 0) continue;

                int numCaves = rand.nextInt(rand.nextInt(rand.nextInt(40) + 1) + 1);

                for (int i = 0; i < numCaves; i++) {
                    double startX = nx * Chunk.SIZE + rand.nextInt(Chunk.SIZE);
                    double startY = Chunk.MIN_Y + 10 + rand.nextInt(238); 
                    double startZ = nz * Chunk.SIZE + rand.nextInt(Chunk.SIZE);

                    int numNodes = 1;
                    
                    if (rand.nextInt(4) == 0) {
                        float roomRadius = 1.5f + rand.nextFloat() * 6.0f;
                        carveSphere(targetChunk, startX, startY, startZ, roomRadius);
                        numNodes += rand.nextInt(4);
                    }

                    for (int n = 0; n < numNodes; n++) {
                        float startYaw = rand.nextFloat() * (float) Math.PI * 2.0f;
                        float startPitch = (rand.nextFloat() - 0.5f) * 1.5f; // Deutlich steiler, damit sie öfter die Oberfläche durchbrechen!
                        float startRadius = 1.5f + rand.nextFloat() * 2.0f;

                        if (rand.nextInt(10) == 0) {
                            startRadius *= 2.0f; 
                        }

                        int length = 112 - rand.nextInt(28); 
                        derivePath(targetChunk, rand, startX, startY, startZ, startRadius, startYaw, startPitch, length);
                    }
                }
            }
        }
    }

    private static void derivePath(Chunk targetChunk, Random rand, double x, double y, double z, float baseRadius, float yaw, float pitch, int length) {
        for (int i = 0; i < length; i++) {
            float currentRadius = baseRadius + (float)(Math.sin(i * Math.PI / length) * baseRadius * 0.5f);

            double dx = Math.cos(yaw) * Math.cos(pitch);
            double dy = Math.sin(pitch);
            double dz = Math.sin(yaw) * Math.cos(pitch);

            x += dx;
            y += dy;
            z += dz;

            pitch *= 0.9f;
            pitch += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 0.5f;
            yaw += (rand.nextFloat() - rand.nextFloat()) * rand.nextFloat() * 1.0f;

            if (i == length / 2 && rand.nextInt(4) == 0) {
                derivePath(targetChunk, rand, x, y, z, baseRadius, yaw - 1.0f, pitch, length / 2);
                derivePath(targetChunk, rand, x, y, z, baseRadius, yaw + 1.0f, pitch, length / 2);
                return; 
            }

            if (rand.nextInt(4) == 0) continue; 

            double chunkMidX = targetChunk.getWorldX() * Chunk.SIZE + 8.0;
            double chunkMidZ = targetChunk.getWorldZ() * Chunk.SIZE + 8.0;
            
            if (x < chunkMidX - 16 - currentRadius*2 || x > chunkMidX + 16 + currentRadius*2 || 
                z < chunkMidZ - 16 - currentRadius*2 || z > chunkMidZ + 16 + currentRadius*2) {
                continue; 
            }

            carveSphere(targetChunk, x, y, z, currentRadius);
        }
    }

    private static void carveSphere(Chunk chunk, double cx, double cy, double cz, double radius) {
        double radiusY = radius * 0.7; 

        int minX = Math.max(0, (int) Math.floor(cx - radius) - chunk.getWorldX() * Chunk.SIZE);
        int maxX = Math.min(Chunk.SIZE - 1, (int) Math.floor(cx + radius) - chunk.getWorldX() * Chunk.SIZE);
        int minY = Math.max(Chunk.MIN_Y + 4, (int) Math.floor(cy - radiusY)); 
        int maxY = Math.min(Chunk.MAX_Y - 1, (int) Math.floor(cy + radiusY));
        int minZ = Math.max(0, (int) Math.floor(cz - radius) - chunk.getWorldZ() * Chunk.SIZE);
        int maxZ = Math.min(Chunk.SIZE - 1, (int) Math.floor(cz + radius) - chunk.getWorldZ() * Chunk.SIZE);

        if (minX > maxX || minY > maxY || minZ > maxZ) return;

        boolean hasWaterAbove = false;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (maxY + 1 < Chunk.MAX_Y) {
                    byte above = chunk.getBlock(x, maxY + 1, z);
                    if (above == WATER || above == LAVA) {
                        hasWaterAbove = true;
                        break;
                    }
                }
            }
            if (hasWaterAbove) break;
        }

        if (hasWaterAbove) return;

        ChunkSection[] sections = chunk.getSections();

        for (int ly = minY; ly <= maxY; ly++) {
            int secIdx = (ly - Chunk.MIN_Y) >> 4;
            ChunkSection section = sections[secIdx];
            if (section == null) continue; // Skip air sections completely

            byte[] blocks = section.getBlocks();
            byte[] states = section.getStates();
            int localY = (ly - Chunk.MIN_Y) & 15;
            double dY = (ly + 0.5 - cy) / radiusY;

            for (int lx = minX; lx <= maxX; lx++) {
                double dX = ((lx + chunk.getWorldX() * Chunk.SIZE) + 0.5 - cx) / radius;
                if (dX * dX + dY * dY >= 1.0) continue;

                for (int lz = minZ; lz <= maxZ; lz++) {
                    double dZ = ((lz + chunk.getWorldZ() * Chunk.SIZE) + 0.5 - cz) / radius;
                    if (dX * dX + dY * dY + dZ * dZ < 1.0) {
                        
                        int idx = (lx << 8) | (lz << 4) | localY;
                        byte currentBlock = blocks[idx];
                        
                        if (currentBlock != AIR && currentBlock != BEDROCK) {
                            if (whitelist[currentBlock & 0xFF]) {
                                blocks[idx] = AIR;
                                states[idx] = 0;
                            }
                        }
                    }
                }
            }
            section.recalculateAir();
        }
    }
}