package de.delautrer.game.world;

import de.delautrer.game.blocks.BlockRegistry;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class LightEngine {

    private final ChunkManager chunkManager;
    private final Queue<int[]> blockLightQueue = new LinkedList<>();
    private final Queue<int[]> skyLightQueue = new LinkedList<>();

    private final Set<Chunk> dirtiedChunks = new HashSet<>();

    private final Queue<int[]> skyLightRemovalQueue = new LinkedList<>();
    private final Queue<int[]> blockLightRemovalQueue = new LinkedList<>();

    private static final int[][] DIRS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    public LightEngine(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    // --- 1. BLOCK LICHT (Fackeln etc.) ---
    public void addBlockLightSource(int x, int y, int z, int lightLevel) {
        setBlockLight(x, y, z, lightLevel);
        blockLightQueue.add(new int[]{x, y, z});
        propagateBlockLight();
    }

    private void propagateBlockLight() {
        while (!blockLightQueue.isEmpty()) {
            int[] node = blockLightQueue.poll();
            int nx = node[0], ny = node[1], nz = node[2];
            int currentLight = getBlockLight(nx, ny, nz);

            if (currentLight <= 1) continue;

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                if (isTransparent(adjX, adjY, adjZ)) {
                    int neighborLight = getBlockLight(adjX, adjY, adjZ);
                    if (neighborLight + 2 <= currentLight) {
                        setBlockLight(adjX, adjY, adjZ, currentLight - 1);
                        blockLightQueue.add(new int[]{adjX, adjY, adjZ});
                    }
                }
            }
        }
    }

    // --- 2. SONNENLICHT (Smooth Lighting) ---
    public void initSkyLightForChunk(Chunk chunk) {
        int cx = chunk.getWorldX() * Chunk.SIZE;
        int cz = chunk.getWorldZ() * Chunk.SIZE;

        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int y = 0; y < Chunk.HEIGHT; y++) {
                    if (chunk.getSkyLight(x, y, z) == 15) {
                        skyLightQueue.add(new int[]{cx + x, y, cz + z});
                    }
                }
            }
        }
        propagateSkyLight();
    }

    public void initSkyLightForColumn(int worldX, int worldZ) {
        for (int y = 0; y < Chunk.HEIGHT; y++) {
            if (getSkyLight(worldX, y, worldZ) == 15) {
                skyLightQueue.add(new int[]{worldX, y, worldZ});
            }
        }
        propagateSkyLight();
    }

    private void propagateSkyLight() {
        while (!skyLightQueue.isEmpty()) {
            int[] node = skyLightQueue.poll();
            int nx = node[0], ny = node[1], nz = node[2];
            int currentLight = getSkyLight(nx, ny, nz);

            if (currentLight <= 1) continue;

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                if (isTransparent(adjX, adjY, adjZ)) {
                    int neighborLight = getSkyLight(adjX, adjY, adjZ);
                    if (neighborLight + 2 <= currentLight) {
                        setSkyLight(adjX, adjY, adjZ, currentLight - 1);
                        skyLightQueue.add(new int[]{adjX, adjY, adjZ});
                    }
                }
            }
        }
    }

    // --- 3. HILFSMETHODEN ---
    public Set<Chunk> getAndClearDirtiedChunks() {
        Set<Chunk> copy = new HashSet<>(dirtiedChunks);
        dirtiedChunks.clear();
        return copy;
    }

    public int getBlockLight(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) return 0;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        return c != null ? c.getBlockLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE)) : 0;
    }

    public void setBlockLight(int worldX, int worldY, int worldZ, int level) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) return;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c != null) {
            int old = c.getBlockLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
            if (old != level) {
                c.setBlockLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE), level);
                dirtiedChunks.add(c);
            }
        }
    }

    public int getSkyLight(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) return 15;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        return c != null ? c.getSkyLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE)) : 15;
    }

    public void setSkyLight(int worldX, int worldY, int worldZ, int level) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) return;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c != null) {
            int old = c.getSkyLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
            if (old != level) {
                c.setSkyLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE), level);
                dirtiedChunks.add(c);
            }
        }
    }

    private boolean isTransparent(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) return true;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c == null) return true;
        byte id = c.getBlock(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
        return BlockRegistry.get(id).isTransparent;
    }

    // Sammelt alle Blöcke, die plötzlich dunkler geworden sind
    public void addSkyLightRemoval(int x, int y, int z, int oldLightLevel) {
        skyLightRemovalQueue.add(new int[]{x, y, z, oldLightLevel});
    }

    public void addSkyLightUpdate(int x, int y, int z) {
        skyLightQueue.add(new int[]{x, y, z});
    }

    public void processLightUpdates() {
        propagateSkyLightRemoval();
        propagateSkyLight();
        propagateBlockLightRemoval();
        propagateBlockLight();
    }

    private void propagateSkyLightRemoval() {
        while (!skyLightRemovalQueue.isEmpty()) {
            int[] node = skyLightRemovalQueue.poll();
            int nx = node[0], ny = node[1], nz = node[2], lightLevel = node[3];

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                int neighborLight = getSkyLight(adjX, adjY, adjZ);

                if (neighborLight != 0 && neighborLight < lightLevel) {
                    setSkyLight(adjX, adjY, adjZ, 0);
                    skyLightRemovalQueue.add(new int[]{adjX, adjY, adjZ, neighborLight});
                }
                else if (neighborLight >= lightLevel) {
                    skyLightQueue.add(new int[]{adjX, adjY, adjZ});
                }
            }
        }
    }

    public void removeBlockLight(int x, int y, int z, int oldLight) {
        setBlockLight(x, y, z, 0);
        blockLightRemovalQueue.add(new int[]{x, y, z, oldLight});
    }

    public void notifyBlockChanged(int x, int y, int z) {
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];

            if (getBlockLight(nx, ny, nz) > 0) {
                blockLightQueue.add(new int[]{nx, ny, nz});
            }
            if (getSkyLight(nx, ny, nz) > 0) {
                skyLightQueue.add(new int[]{nx, ny, nz});
            }
        }
    }

    private void propagateBlockLightRemoval() {
        while (!blockLightRemovalQueue.isEmpty()) {
            int[] node = blockLightRemovalQueue.poll();
            int nx = node[0], ny = node[1], nz = node[2], lightLevel = node[3];

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                int neighborLight = getBlockLight(adjX, adjY, adjZ);

                if (neighborLight != 0 && neighborLight < lightLevel) {
                    setBlockLight(adjX, adjY, adjZ, 0);
                    blockLightRemovalQueue.add(new int[]{adjX, adjY, adjZ, neighborLight});
                } else if (neighborLight >= lightLevel) {
                    blockLightQueue.add(new int[]{adjX, adjY, adjZ});
                }
            }
        }
    }
}