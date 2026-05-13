package de.delautrer.game.world;

import de.delautrer.game.blocks.BlockRegistry;
import java.util.HashSet;
import java.util.Set;

public class LightEngine {

    private final ChunkManager chunkManager;
    private final Set<Chunk> dirtiedChunks = new HashSet<>();

    // Unsere Zero-Allocation Queues!
    private final LongQueue blockLightQueue = new LongQueue(1024);
    private final LongQueue skyLightQueue = new LongQueue(4096);
    private final LongQueue skyLightRemovalQueue = new LongQueue(1024);
    private final LongQueue blockLightRemovalQueue = new LongQueue(1024);

    private static final int[][] DIRS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    public LightEngine(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    // ==========================================
    // ZERO-ALLOCATION BIT PACKING (64 bit 'long')
    // ==========================================
    // Bits 0-3  : Light Level (4 bit)
    // Bits 4-13 : Y-Coord (10 bit)
    // Bits 14-38: X-Coord (25 bit, signed)
    // Bits 39-63: Z-Coord (25 bit, signed)
    // ==========================================

    private long pack(int x, int y, int z, int light) {
        return ((long)(light & 0xF)) |
                (((long)((y - Chunk.MIN_Y) & 0x3FF)) << 4) |
                (((long)(x & 0x1FFFFFF)) << 14) |
                (((long)(z & 0x1FFFFFF)) << 39);
    }

    private int unpackLight(long node) {
        return (int)(node & 0xF);
    }

    private int unpackY(long node) {
        return (int)((node >> 4) & 0x3FF) + Chunk.MIN_Y;
    }

    private int unpackX(long node) {
        int x = (int)((node >> 14) & 0x1FFFFFF);
        // Vorzeichen-Erweiterung für negative Koordinaten
        if ((x & 0x1000000) != 0) x |= 0xFE000000;
        return x;
    }

    private int unpackZ(long node) {
        int z = (int)((node >> 39) & 0x1FFFFFF);
        // Vorzeichen-Erweiterung für negative Koordinaten
        if ((z & 0x1000000) != 0) z |= 0xFE000000;
        return z;
    }

    // --- 1. BLOCK LICHT (Fackeln etc.) ---
    public void addBlockLightSource(int x, int y, int z, int lightLevel) {
        setBlockLight(x, y, z, lightLevel);
        blockLightQueue.add(pack(x, y, z, 0));
        propagateBlockLight();
    }

    private void propagateBlockLight() {
        while (!blockLightQueue.isEmpty()) {
            long node = blockLightQueue.poll();
            int nx = unpackX(node);
            int ny = unpackY(node);
            int nz = unpackZ(node);

            int currentLight = getBlockLight(nx, ny, nz);
            if (currentLight <= 1) continue;

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                if (isTransparent(adjX, adjY, adjZ)) {
                    int neighborLight = getBlockLight(adjX, adjY, adjZ);
                    if (neighborLight + 2 <= currentLight) {
                        setBlockLight(adjX, adjY, adjZ, currentLight - 1);
                        blockLightQueue.add(pack(adjX, adjY, adjZ, 0));
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
                for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                    if (chunk.getSkyLight(x, y, z) == 15) {
                        skyLightQueue.add(pack(cx + x, y, cz + z, 0));
                    }
                }
            }
        }
        propagateSkyLight();
    }

    public void stitchChunkBorders(Chunk chunk) {
        int cx = chunk.getWorldX() * Chunk.SIZE;
        int cz = chunk.getWorldZ() * Chunk.SIZE;

        // X Borders (x=0 und x=15) mit den Nachbar-Chunks abgleichen
        for (int z = 0; z < Chunk.SIZE; z++) {
            for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                if (getSkyLight(cx, y, cz + z) > 0) skyLightQueue.add(pack(cx, y, cz + z, 0));
                if (getSkyLight(cx - 1, y, cz + z) > 0) skyLightQueue.add(pack(cx - 1, y, cz + z, 0));
                if (getBlockLight(cx, y, cz + z) > 0) blockLightQueue.add(pack(cx, y, cz + z, 0));
                if (getBlockLight(cx - 1, y, cz + z) > 0) blockLightQueue.add(pack(cx - 1, y, cz + z, 0));

                if (getSkyLight(cx + 15, y, cz + z) > 0) skyLightQueue.add(pack(cx + 15, y, cz + z, 0));
                if (getSkyLight(cx + 16, y, cz + z) > 0) skyLightQueue.add(pack(cx + 16, y, cz + z, 0));
                if (getBlockLight(cx + 15, y, cz + z) > 0) blockLightQueue.add(pack(cx + 15, y, cz + z, 0));
                if (getBlockLight(cx + 16, y, cz + z) > 0) blockLightQueue.add(pack(cx + 16, y, cz + z, 0));
            }
        }
        // Z Borders (z=0 und z=15) mit den Nachbar-Chunks abgleichen
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                if (getSkyLight(cx + x, y, cz) > 0) skyLightQueue.add(pack(cx + x, y, cz, 0));
                if (getSkyLight(cx + x, y, cz - 1) > 0) skyLightQueue.add(pack(cx + x, y, cz - 1, 0));
                if (getBlockLight(cx + x, y, cz) > 0) blockLightQueue.add(pack(cx + x, y, cz, 0));
                if (getBlockLight(cx + x, y, cz - 1) > 0) blockLightQueue.add(pack(cx + x, y, cz - 1, 0));

                if (getSkyLight(cx + x, y, cz + 15) > 0) skyLightQueue.add(pack(cx + x, y, cz + 15, 0));
                if (getSkyLight(cx + x, y, cz + 16) > 0) skyLightQueue.add(pack(cx + x, y, cz + 16, 0));
                if (getBlockLight(cx + x, y, cz + 15) > 0) blockLightQueue.add(pack(cx + x, y, cz + 15, 0));
                if (getBlockLight(cx + x, y, cz + 16) > 0) blockLightQueue.add(pack(cx + x, y, cz + 16, 0));
            }
        }
        processLightUpdates();
    }

    public void initSkyLightForColumn(int worldX, int worldZ) {
        for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
            if (getSkyLight(worldX, y, worldZ) == 15) {
                skyLightQueue.add(pack(worldX, y, worldZ, 0));
            }
        }
        propagateSkyLight();
    }

    private void propagateSkyLight() {
        while (!skyLightQueue.isEmpty()) {
            long node = skyLightQueue.poll();
            int nx = unpackX(node);
            int ny = unpackY(node);
            int nz = unpackZ(node);

            int currentLight = getSkyLight(nx, ny, nz);
            if (currentLight <= 1) continue;

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                if (isTransparent(adjX, adjY, adjZ)) {
                    int neighborLight = getSkyLight(adjX, adjY, adjZ);
                    if (neighborLight + 2 <= currentLight) {
                        setSkyLight(adjX, adjY, adjZ, currentLight - 1);
                        skyLightQueue.add(pack(adjX, adjY, adjZ, 0));
                    }
                }
            }
        }
    }

    // --- 3. DYNAMISCHE UPDATES (Blöcke setzen / abbauen) ---
    public void addSkyLightRemoval(int x, int y, int z, int oldLightLevel) {
        skyLightRemovalQueue.add(pack(x, y, z, oldLightLevel));
    }

    public void addSkyLightUpdate(int x, int y, int z) {
        skyLightQueue.add(pack(x, y, z, 0));
    }

    public void removeBlockLight(int x, int y, int z, int oldLight) {
        setBlockLight(x, y, z, 0);
        blockLightRemovalQueue.add(pack(x, y, z, oldLight));
    }

    public void processLightUpdates() {
        propagateSkyLightRemoval();
        propagateSkyLight();
        propagateBlockLightRemoval();
        propagateBlockLight();
    }

    private void propagateSkyLightRemoval() {
        while (!skyLightRemovalQueue.isEmpty()) {
            long node = skyLightRemovalQueue.poll();
            int nx = unpackX(node), ny = unpackY(node), nz = unpackZ(node);
            int lightLevel = unpackLight(node);

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                int neighborLight = getSkyLight(adjX, adjY, adjZ);

                if (neighborLight != 0 && neighborLight < lightLevel) {
                    setSkyLight(adjX, adjY, adjZ, 0);
                    skyLightRemovalQueue.add(pack(adjX, adjY, adjZ, neighborLight));
                } else if (neighborLight >= lightLevel) {
                    skyLightQueue.add(pack(adjX, adjY, adjZ, 0));
                }
            }
        }
    }

    private void propagateBlockLightRemoval() {
        while (!blockLightRemovalQueue.isEmpty()) {
            long node = blockLightRemovalQueue.poll();
            int nx = unpackX(node), ny = unpackY(node), nz = unpackZ(node);
            int lightLevel = unpackLight(node);

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                int neighborLight = getBlockLight(adjX, adjY, adjZ);

                if (neighborLight != 0 && neighborLight < lightLevel) {
                    setBlockLight(adjX, adjY, adjZ, 0);
                    blockLightRemovalQueue.add(pack(adjX, adjY, adjZ, neighborLight));
                } else if (neighborLight >= lightLevel) {
                    blockLightQueue.add(pack(adjX, adjY, adjZ, 0));
                }
            }
        }
    }

    public void notifyBlockChanged(int x, int y, int z) {
        for (int[] dir : DIRS) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            int nz = z + dir[2];

            if (getBlockLight(nx, ny, nz) > 0) {
                blockLightQueue.add(pack(nx, ny, nz, 0));
            }
            if (getSkyLight(nx, ny, nz) > 0) {
                skyLightQueue.add(pack(nx, ny, nz, 0));
            }
        }
    }

    // --- 4. HILFSMETHODEN (Getter / Setter) ---
    public Set<Chunk> getAndClearDirtiedChunks() {
        Set<Chunk> copy = new HashSet<>(dirtiedChunks);
        dirtiedChunks.clear();
        return copy;
    }

    public int getBlockLight(int worldX, int worldY, int worldZ) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return 0;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        return c != null ? c.getBlockLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE)) : 0;
    }

    public void setBlockLight(int worldX, int worldY, int worldZ, int level) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c != null) {
            int old = c.getBlockLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
            if (old != level) {
                c.setBlockLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE), level);
                c.markDirty();
                dirtiedChunks.add(c);
            }
        }
    }

    public int getSkyLight(int worldX, int worldY, int worldZ) {
        if (worldY >= Chunk.MAX_Y) return 15;
        if (worldY < Chunk.MIN_Y) return 0;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        return c != null ? c.getSkyLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE)) : 0;
    }

    public void setSkyLight(int worldX, int worldY, int worldZ, int level) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c != null) {
            int old = c.getSkyLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
            if (old != level) {
                c.setSkyLight(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE), level);
                c.markDirty();
                dirtiedChunks.add(c);
            }
        }
    }

    private boolean isTransparent(int worldX, int worldY, int worldZ) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return true;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c == null) return false;
        byte id = c.getBlock(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
        return BlockRegistry.get(id).isTransparent;
    }

    // ==========================================
    // INTERNE ZERO-ALLOCATION QUEUE (Ring-Buffer)
    // ==========================================
    private static class LongQueue {
        private long[] data;
        private int head = 0;
        private int tail = 0;
        private int size = 0;

        public LongQueue(int initialCapacity) {
            data = new long[initialCapacity];
        }

        public void add(long value) {
            if (size == data.length) resize();
            data[tail] = value;
            tail = (tail + 1) % data.length;
            size++;
        }

        public long poll() {
            long val = data[head];
            head = (head + 1) % data.length;
            size--;
            return val;
        }

        public boolean isEmpty() {
            return size == 0;
        }

        private void resize() {
            long[] newData = new long[data.length * 2];
            for (int i = 0; i < size; i++) {
                newData[i] = data[(head + i) % data.length];
            }
            data = newData;
            head = 0;
            tail = size;
        }
    }
}