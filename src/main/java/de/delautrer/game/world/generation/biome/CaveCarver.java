package de.delautrer.game.world.generation.biome;

import de.delautrer.Constants;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkSection;
import java.util.Random;

public class CaveCarver {

    private static byte AIR = 0;
    private static byte WATER = 0;
    private static byte BEDROCK = 0;

    private static boolean initialized = false;
    private static boolean[] whitelist = new boolean[256];

    private static de.delautrer.game.world.NoiseGenerator riverNoise;
    private static long currentSeed = -1;

    private static void init(long seed) {
        if (initialized && currentSeed == seed) return;
        AIR = BlockRegistry.get(Constants.NAMESPACE + ":air").getId();
        WATER = BlockRegistry.get(Constants.NAMESPACE + ":water").getId();
        BEDROCK = BlockRegistry.get(Constants.NAMESPACE + ":bedrock").getId();

        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":stone").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":dirt").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":grass_block").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":gravel").getId() & 0xFF] = true;
        whitelist[BlockRegistry.get(Constants.NAMESPACE + ":sand").getId() & 0xFF] = true;

        riverNoise = new de.delautrer.game.world.NoiseGenerator(seed * 234);
        currentSeed = seed;
        initialized = true;
    }

    public static void carve(Chunk targetChunk, long worldSeed, MultiNoiseSampler sampler) {
        init(worldSeed);
        int chunkX = targetChunk.getWorldX();
        int chunkZ = targetChunk.getWorldZ();
        int radius = 12;

        for (int nx = chunkX - radius; nx <= chunkX + radius; nx++) {
            for (int nz = chunkZ - radius; nz <= chunkZ + radius; nz++) {
                long neighborSeed = ((long) nx * 341873128712L) ^ ((long) nz * 132897987541L) ^ worldSeed;
                Random rand = new Random(neighborSeed);

                if (rand.nextInt(5) != 0) continue;

                int numCaves = rand.nextInt(rand.nextInt(rand.nextInt(40) + 1) + 1);

                for (int i = 0; i < numCaves; i++) {
                    double startX = nx * Chunk.SIZE + rand.nextInt(Chunk.SIZE);
                    double startZ = nz * Chunk.SIZE + rand.nextInt(Chunk.SIZE);

                    int range = (Chunk.MAX_Y / 2) - Chunk.MIN_Y;
                    double startY = Chunk.MIN_Y + 10 + rand.nextInt(range);

                    int numNodes = 1;
                    if (rand.nextInt(4) == 0) {
                        float roomRadius = 1.5f + rand.nextFloat() * 6.0f;
                        carveSphere(targetChunk, startX, startY, startZ, roomRadius, sampler);
                        numNodes += rand.nextInt(4);
                    }

                    for (int n = 0; n < numNodes; n++) {
                        float startYaw = rand.nextFloat() * (float) Math.PI * 2.0f;
                        float startPitch = (rand.nextFloat() - 0.5f) * 1.5f;

                        if (startY > 80 && rand.nextInt(3) == 0) startPitch = -1.0f - rand.nextFloat() * 0.5f;

                        float startRadius = 1.5f + rand.nextFloat() * 2.0f;
                        if (rand.nextInt(10) == 0) startRadius *= 2.0f;

                        int length = 100 + rand.nextInt(80);
                        derivePath(targetChunk, rand, startX, startY, startZ, startRadius, startYaw, startPitch, length, sampler);
                    }
                }
            }
        }
    }

    private static void derivePath(Chunk targetChunk, Random rand, double x, double y, double z, float baseRadius, float yaw, float pitch, int length, MultiNoiseSampler sampler) {
        for (int i = 0; i < length; i++) {
            float currentRadius = baseRadius + (rand.nextFloat() - 0.5f) * 0.5f;

            x += Math.cos(yaw) * Math.cos(pitch);
            y += Math.sin(pitch);
            z += Math.sin(yaw) * Math.cos(pitch);

            pitch *= 0.9f;
            yaw += (rand.nextFloat() - 0.5f) * 0.5f;
            pitch += (rand.nextFloat() - 0.5f) * 0.5f;

            if (i == length / 2 && rand.nextInt(4) == 0) {
                derivePath(targetChunk, rand, x, y, z, baseRadius, yaw - 1.0f, pitch, length / 2, sampler);
                derivePath(targetChunk, rand, x, y, z, baseRadius, yaw + 1.0f, pitch, length / 2, sampler);
                return;
            }

            double chunkMidX = targetChunk.getWorldX() * Chunk.SIZE + 8.0;
            double chunkMidZ = targetChunk.getWorldZ() * Chunk.SIZE + 8.0;

            if (x < chunkMidX - 16 - currentRadius * 2 || x > chunkMidX + 16 + currentRadius * 2 ||
                    z < chunkMidZ - 16 - currentRadius * 2 || z > chunkMidZ + 16 + currentRadius * 2) {
                continue;
            }
            carveSphere(targetChunk, x, y, z, currentRadius, sampler);
        }
    }

    private static void carveSphere(Chunk chunk, double cx, double cy, double cz, double radius, MultiNoiseSampler sampler) {
        // --- STARKER WASSER-SCHUTZ ---
        int wx = (int)cx;
        int wz = (int)cz;
        Climate.TargetPoint climate = sampler.sample(wx, wz);
        
        // River check
        float warpX = riverNoise.getFractalNoise2D(wx * 0.02f, wz * 0.02f, 2, 0.5f, 2.0f) * 25.0f;
        float warpZ = riverNoise.getFractalNoise2D(wx * 0.02f + 100, wz * 0.02f + 100, 2, 0.5f, 2.0f) * 25.0f;
        float rNoise = riverNoise.getFractalNoise2D((wx + warpX) * 0.003f, (wz + warpZ) * 0.003f, 3, 0.5f, 2.0f);
        float riverVal = Math.abs(rNoise);

        boolean inWetZone = (climate.continentalness < 0.05f) || (riverVal < 0.12f);
        
        if (inWetZone) {
            // In nassen Gebieten: Mindestens 20 Blöcke unter dem Meeresspiegel bleiben (Sicherheitspuffer)
            double surfaceSafetyY = 10; 
            if (cy > surfaceSafetyY - 25) return; 
        }

        double radiusY = radius * 0.7;
        int chunkOffX = chunk.getWorldX() * Chunk.SIZE;
        int chunkOffZ = chunk.getWorldZ() * Chunk.SIZE;

        int minX = Math.max(0, (int) Math.floor(cx - radius) - chunkOffX);
        int maxX = Math.min(Chunk.SIZE - 1, (int) Math.floor(cx + radius) - chunkOffX);
        int minY = Math.max(Chunk.MIN_Y + 4, (int) Math.floor(cy - radiusY));
        int maxY = Math.min(Chunk.MAX_Y - 1, (int) Math.floor(cy + radiusY));
        int minZ = Math.max(0, (int) Math.floor(cz - radius) - chunkOffZ);
        int maxZ = Math.min(Chunk.SIZE - 1, (int) Math.floor(cz + radius) - chunkOffZ);

        if (minX > maxX || minY > maxY || minZ > maxZ) return;

        ChunkSection[] sections = chunk.getSections();

        for (int lx = minX; lx <= maxX; lx++) {
            double dX = ((lx + chunkOffX) + 0.5 - cx) / radius;
            for (int lz = minZ; lz <= maxZ; lz++) {
                double dZ = ((lz + chunkOffZ) + 0.5 - cz) / radius;
                if (dX * dX + dZ * dZ >= 1.0) continue;

                for (int ly = maxY; ly >= minY; ly--) {
                    double dY = (ly + 0.5 - cy) / radiusY;

                    int secIdx = (ly - Chunk.MIN_Y) >> 4;
                    if (secIdx < 0 || secIdx >= sections.length) continue;
                    ChunkSection section = sections[secIdx];
                    if (section == null) continue;

                    byte[] blocks = section.getBlocks();
                    byte[] states = section.getStates();
                    int localY = (ly - Chunk.MIN_Y) & 15;
                    int idx = (lx << 8) | (lz << 4) | localY;
                    byte currentBlock = blocks[idx];

                    if (dX * dX + dY * dY + dZ * dZ < 1.0) {
                        if (currentBlock == BEDROCK) continue;
                        boolean canCarve = whitelist[currentBlock & 0xFF] || currentBlock == AIR || currentBlock == WATER;
                        if (canCarve) {
                            if (currentBlock != WATER) {
                                blocks[idx] = AIR;
                                states[idx] = 0;
                            }
                        }
                    }
                }
            }
        }
        for (ChunkSection sec : sections) if (sec != null) sec.recalculateAir();
    }
}