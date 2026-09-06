package de.delautrer.game.world;

import de.delautrer.game.blocks.BlockRegistry;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public class LightEngine {

    private enum OpType {
        ADD_BLOCK_LIGHT,
        REMOVE_BLOCK_LIGHT,
        ADD_SKY_LIGHT_REMOVAL,
        ADD_SKY_LIGHT_UPDATE,
        NOTIFY_BLOCK_CHANGED
    }

    private static class PendingLightOp {
        final OpType type;
        final int x, y, z, param;

        PendingLightOp(OpType type, int x, int y, int z, int param) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.param = param;
        }
    }

    private final ChunkManager chunkManager;
    private final Set<Chunk> dirtiedChunks = new HashSet<>();
    private final ConcurrentLinkedQueue<PendingLightOp> pendingOps = new ConcurrentLinkedQueue<>();

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

    private final ReentrantLock lightLock = new ReentrantLock();

    public LightEngine(ChunkManager chunkManager) {
        this.chunkManager = chunkManager;
    }

    private void drainPendingOps() {
        PendingLightOp op;
        while ((op = pendingOps.poll()) != null) {
            switch (op.type) {
                case ADD_BLOCK_LIGHT -> {
                    setBlockLight(op.x, op.y, op.z, op.param);
                    blockLightQueue.add(pack(op.x, op.y, op.z, 0));
                }
                case REMOVE_BLOCK_LIGHT -> {
                    setBlockLight(op.x, op.y, op.z, 0);
                    blockLightRemovalQueue.add(pack(op.x, op.y, op.z, op.param));
                }
                case ADD_SKY_LIGHT_REMOVAL -> {
                    skyLightRemovalQueue.add(pack(op.x, op.y, op.z, op.param));
                }
                case ADD_SKY_LIGHT_UPDATE -> {
                    skyLightQueue.add(pack(op.x, op.y, op.z, 0));
                }
                case NOTIFY_BLOCK_CHANGED -> {
                    for (int[] dir : DIRS) {
                        int nx = op.x + dir[0];
                        int ny = op.y + dir[1];
                        int nz = op.z + dir[2];

                        if (getBlockLight(nx, ny, nz) > 0) {
                            blockLightQueue.add(pack(nx, ny, nz, 0));
                        }
                        if (getSkyLight(nx, ny, nz) > 0) {
                            skyLightQueue.add(pack(nx, ny, nz, 0));
                        }
                    }
                }
            }
        }
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
        // Vorzeichen-Erweiterung für 25-Bit (Bit 24 ist Vorzeichen)
        if ((x & 0x1000000) != 0) x |= 0xFE000000;
        return x;
    }

    private int unpackZ(long node) {
        int z = (int)((node >> 39) & 0x1FFFFFF);
        // Vorzeichen-Erweiterung für 25-Bit (Bit 24 ist Vorzeichen)
        if ((z & 0x1000000) != 0) z |= 0xFE000000;
        return z;
    }

    // --- 1. BLOCK LICHT (Fackeln etc.) ---
    public void addBlockLightSource(int x, int y, int z, int lightLevel) {
        if (lightLock.tryLock()) {
            try {
                drainPendingOps();
                setBlockLight(x, y, z, lightLevel);
                blockLightQueue.add(pack(x, y, z, 0));
            } finally {
                lightLock.unlock();
            }
        } else {
            pendingOps.add(new PendingLightOp(OpType.ADD_BLOCK_LIGHT, x, y, z, lightLevel));
        }
    }

    private int propagateBlockLight(int maxNodes) {
        int processed = 0;
        while (processed < maxNodes && !blockLightQueue.isEmpty()) {
            long node = blockLightQueue.poll();
            processed++;
            int nx = unpackX(node);
            int ny = unpackY(node);
            int nz = unpackZ(node);

            int currentLight = getBlockLight(nx, ny, nz);
            if (currentLight <= 1) continue;

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                int opacity = getOpacity(adjX, adjY, adjZ);
                if (opacity < 15) {
                    int neighborLight = getBlockLight(adjX, adjY, adjZ);
                    int attenuation = Math.max(1, opacity);
                    if (neighborLight + attenuation + 1 <= currentLight) {
                        setBlockLight(adjX, adjY, adjZ, currentLight - attenuation);
                        blockLightQueue.add(pack(adjX, adjY, adjZ, 0));
                    }
                }
            }
        }
        return processed;
    }

    public void initSkyLightForChunk(Chunk chunk) {
        lightLock.lock();
        try {
            drainPendingOps();
            int cx = chunk.getWorldX() * Chunk.SIZE;
            int cz = chunk.getWorldZ() * Chunk.SIZE;

            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                        int light = chunk.getSkyLight(x, y, z);
                        if (light > 0) {
                            boolean edge = false;
                            if (x > 0 && chunk.getSkyLight(x - 1, y, z) < light) edge = true;
                            else if (x < 15 && chunk.getSkyLight(x + 1, y, z) < light) edge = true;
                            else if (z > 0 && chunk.getSkyLight(x, y, z - 1) < light) edge = true;
                            else if (z < 15 && chunk.getSkyLight(x, y, z + 1) < light) edge = true;

                            if (edge) {
                                skyLightQueue.add(pack(cx + x, y, cz + z, 0));
                            }
                        }
                    }
                }
            }
            propagateSkyLight(Integer.MAX_VALUE);
        } finally {
            lightLock.unlock();
        }
    }

    public void stitchChunkBorders(Chunk chunk) {
        lightLock.lock();
        try {
            drainPendingOps();
            int cx = chunk.getWorldX() * Chunk.SIZE;
            int cz = chunk.getWorldZ() * Chunk.SIZE;

            // X Borders (x=0 und x=15) mit den Nachbar-Chunks abgleichen
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                    int sl1 = getSkyLight(cx, y, cz + z);
                    int sl2 = getSkyLight(cx - 1, y, cz + z);
                    if (sl1 > sl2) skyLightQueue.add(pack(cx, y, cz + z, 0));
                    else if (sl2 > sl1) skyLightQueue.add(pack(cx - 1, y, cz + z, 0));

                    int bl1 = getBlockLight(cx, y, cz + z);
                    int bl2 = getBlockLight(cx - 1, y, cz + z);
                    if (bl1 > bl2) blockLightQueue.add(pack(cx, y, cz + z, 0));
                    else if (bl2 > bl1) blockLightQueue.add(pack(cx - 1, y, cz + z, 0));

                    int sl3 = getSkyLight(cx + 15, y, cz + z);
                    int sl4 = getSkyLight(cx + 16, y, cz + z);
                    if (sl3 > sl4) skyLightQueue.add(pack(cx + 15, y, cz + z, 0));
                    else if (sl4 > sl3) skyLightQueue.add(pack(cx + 16, y, cz + z, 0));

                    int bl3 = getBlockLight(cx + 15, y, cz + z);
                    int bl4 = getBlockLight(cx + 16, y, cz + z);
                    if (bl3 > bl4) blockLightQueue.add(pack(cx + 15, y, cz + z, 0));
                    else if (bl4 > bl3) blockLightQueue.add(pack(cx + 16, y, cz + z, 0));
                }
            }
            // Z Borders (z=0 und z=15) mit den Nachbar-Chunks abgleichen
            for (int x = 0; x < Chunk.SIZE; x++) {
                for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                    int sl1 = getSkyLight(cx + x, y, cz);
                    int sl2 = getSkyLight(cx + x, y, cz - 1);
                    if (sl1 > sl2) skyLightQueue.add(pack(cx + x, y, cz, 0));
                    else if (sl2 > sl1) skyLightQueue.add(pack(cx + x, y, cz - 1, 0));

                    int bl1 = getBlockLight(cx + x, y, cz);
                    int bl2 = getBlockLight(cx + x, y, cz - 1);
                    if (bl1 > bl2) blockLightQueue.add(pack(cx + x, y, cz, 0));
                    else if (bl2 > bl1) blockLightQueue.add(pack(cx + x, y, cz - 1, 0));

                    int sl3 = getSkyLight(cx + x, y, cz + 15);
                    int sl4 = getSkyLight(cx + x, y, cz + 16);
                    if (sl3 > sl4) skyLightQueue.add(pack(cx + x, y, cz + 15, 0));
                    else if (sl4 > sl3) skyLightQueue.add(pack(cx + x, y, cz + 16, 0));

                    int bl3 = getBlockLight(cx + x, y, cz + 15);
                    int bl4 = getBlockLight(cx + x, y, cz + 16);
                    if (bl3 > bl4) blockLightQueue.add(pack(cx + x, y, cz + 15, 0));
                    else if (bl4 > bl3) blockLightQueue.add(pack(cx + x, y, cz + 16, 0));
                }
            }
            processLightUpdatesInternal(Integer.MAX_VALUE);
        } finally {
            lightLock.unlock();
        }
    }

    public void initSkyLightForColumn(int worldX, int worldZ) {
        lightLock.lock();
        try {
            drainPendingOps();
            for (int y = Chunk.MIN_Y; y < Chunk.MAX_Y; y++) {
                if (getSkyLight(worldX, y, worldZ) == 15) {
                    skyLightQueue.add(pack(worldX, y, worldZ, 0));
                }
            }
            propagateSkyLight(Integer.MAX_VALUE);
        } finally {
            lightLock.unlock();
        }
    }

    private int propagateSkyLight(int maxNodes) {
        int processed = 0;
        while (processed < maxNodes && !skyLightQueue.isEmpty()) {
            long node = skyLightQueue.poll();
            processed++;
            int nx = unpackX(node);
            int ny = unpackY(node);
            int nz = unpackZ(node);

            int currentLight = getSkyLight(nx, ny, nz);
            if (currentLight <= 1) continue;

            for (int[] dir : DIRS) {
                int adjX = nx + dir[0], adjY = ny + dir[1], adjZ = nz + dir[2];
                int opacity = getOpacity(adjX, adjY, adjZ);
                if (opacity < 15) {
                    int neighborLight = getSkyLight(adjX, adjY, adjZ);
                    // Sky light propagates downward without loss (like Minecraft)
                    // dir[1] == -1 means going down
                    int attenuation = (dir[1] == -1 && opacity == 0) ? 0 : Math.max(1, opacity);
                    int needed = currentLight - attenuation;
                    if (neighborLight < needed) {
                        setSkyLight(adjX, adjY, adjZ, needed);
                        skyLightQueue.add(pack(adjX, adjY, adjZ, 0));
                    }
                }
            }
        }
        return processed;
    }

    // --- 3. DYNAMISCHE UPDATES (Blöcke setzen / abbauen) ---
    public void addSkyLightRemoval(int x, int y, int z, int oldLightLevel) {
        if (lightLock.tryLock()) {
            try {
                drainPendingOps();
                skyLightRemovalQueue.add(pack(x, y, z, oldLightLevel));
            } finally {
                lightLock.unlock();
            }
        } else {
            pendingOps.add(new PendingLightOp(OpType.ADD_SKY_LIGHT_REMOVAL, x, y, z, oldLightLevel));
        }
    }

    public void addSkyLightUpdate(int x, int y, int z) {
        if (lightLock.tryLock()) {
            try {
                drainPendingOps();
                skyLightQueue.add(pack(x, y, z, 0));
            } finally {
                lightLock.unlock();
            }
        } else {
            pendingOps.add(new PendingLightOp(OpType.ADD_SKY_LIGHT_UPDATE, x, y, z, 0));
        }
    }

    public void removeBlockLight(int x, int y, int z, int oldLight) {
        if (lightLock.tryLock()) {
            try {
                drainPendingOps();
                setBlockLight(x, y, z, 0);
                blockLightRemovalQueue.add(pack(x, y, z, oldLight));
            } finally {
                lightLock.unlock();
            }
        } else {
            pendingOps.add(new PendingLightOp(OpType.REMOVE_BLOCK_LIGHT, x, y, z, oldLight));
        }
    }

    public void processLightUpdates() {
        processLightUpdates(Integer.MAX_VALUE);
    }

    public void processLightUpdates(int maxNodes) {
        if (!lightLock.tryLock()) {
            return;
        }
        try {
            processLightUpdatesInternal(maxNodes);
        } finally {
            lightLock.unlock();
        }
    }

    private void processLightUpdatesInternal(int maxNodes) {
        drainPendingOps();
        int processed = 0;
        processed += propagateSkyLightRemoval(maxNodes - processed);
        if (processed < maxNodes) processed += propagateSkyLight(maxNodes - processed);
        if (processed < maxNodes) processed += propagateBlockLightRemoval(maxNodes - processed);
        if (processed < maxNodes) processed += propagateBlockLight(maxNodes - processed);
    }

    private int propagateSkyLightRemoval(int maxNodes) {
        int processed = 0;
        while (processed < maxNodes && !skyLightRemovalQueue.isEmpty()) {
            long node = skyLightRemovalQueue.poll();
            processed++;
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
        return processed;
    }

    private int propagateBlockLightRemoval(int maxNodes) {
        int processed = 0;
        while (processed < maxNodes && !blockLightRemovalQueue.isEmpty()) {
            long node = blockLightRemovalQueue.poll();
            processed++;
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
        return processed;
    }

    public void notifyBlockChanged(int x, int y, int z) {
        if (lightLock.tryLock()) {
            try {
                drainPendingOps();
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
            } finally {
                lightLock.unlock();
            }
        } else {
            pendingOps.add(new PendingLightOp(OpType.NOTIFY_BLOCK_CHANGED, x, y, z, 0));
        }
    }

    // --- 4. HILFSMETHODEN (Getter / Setter) ---
    public Set<Chunk> getAndClearDirtiedChunks() {
        if (lightLock.tryLock()) {
            try {
                if (dirtiedChunks.isEmpty()) {
                    return Collections.emptySet();
                }
                Set<Chunk> copy = new HashSet<>(dirtiedChunks);
                dirtiedChunks.clear();
                return copy;
            } finally {
                lightLock.unlock();
            }
        }
        return Collections.emptySet();
    }

    public int getBlockLight(int worldX, int worldY, int worldZ) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return 0;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        return c != null ? c.getBlockLight(worldX & 15, worldY, worldZ & 15) : 0;
    }

    public void setBlockLight(int worldX, int worldY, int worldZ, int level) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c != null) {
            int lx = worldX & 15;
            int lz = worldZ & 15;
            int old = c.getBlockLight(lx, worldY, lz);
            if (old != level) {
                c.setBlockLight(lx, worldY, lz, level);
                c.markDirty();
                dirtiedChunks.add(c);
                
                // Nachbar-Chunks markieren, wenn an der Grenze (wegen Smooth Lighting/AO)
                if (lx == 0) markChunkDirty(worldX - 1, worldZ);
                else if (lx == 15) markChunkDirty(worldX + 1, worldZ);
                if (lz == 0) markChunkDirty(worldX, worldZ - 1);
                else if (lz == 15) markChunkDirty(worldX, worldZ + 1);

                if (lx == 0 && lz == 0) markChunkDirty(worldX - 1, worldZ - 1);
                if (lx == 0 && lz == 15) markChunkDirty(worldX - 1, worldZ + 1);
                if (lx == 15 && lz == 0) markChunkDirty(worldX + 1, worldZ - 1);
                if (lx == 15 && lz == 15) markChunkDirty(worldX + 1, worldZ + 1);
            }
        }
    }

    public int getSkyLight(int worldX, int worldY, int worldZ) {
        if (worldY >= Chunk.MAX_Y) return 15;
        if (worldY < Chunk.MIN_Y) return 0;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        return c != null ? c.getSkyLight(worldX & 15, worldY, worldZ & 15) : 0;
    }

    public void setSkyLight(int worldX, int worldY, int worldZ, int level) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c != null) {
            int lx = worldX & 15;
            int lz = worldZ & 15;
            int old = c.getSkyLight(lx, worldY, lz);
            if (old != level) {
                c.setSkyLight(lx, worldY, lz, level);
                c.markDirty();
                dirtiedChunks.add(c);
                
                // Nachbar-Chunks markieren
                if (lx == 0) markChunkDirty(worldX - 1, worldZ);
                else if (lx == 15) markChunkDirty(worldX + 1, worldZ);
                if (lz == 0) markChunkDirty(worldX, worldZ - 1);
                else if (lz == 15) markChunkDirty(worldX, worldZ + 1);

                if (lx == 0 && lz == 0) markChunkDirty(worldX - 1, worldZ - 1);
                if (lx == 0 && lz == 15) markChunkDirty(worldX - 1, worldZ + 1);
                if (lx == 15 && lz == 0) markChunkDirty(worldX + 1, worldZ - 1);
                if (lx == 15 && lz == 15) markChunkDirty(worldX + 1, worldZ + 1);
            }
        }
    }

    private void markChunkDirty(int worldX, int worldZ) {
        Chunk c = chunkManager.getChunkAtBlock(worldX, Chunk.MIN_Y, worldZ);
        if (c != null) {
            c.markDirty();
            dirtiedChunks.add(c);
        }
    }

    private int getOpacity(int worldX, int worldY, int worldZ) {
        if (worldY < Chunk.MIN_Y || worldY >= Chunk.MAX_Y) return 0;
        Chunk c = chunkManager.getChunkAtBlock(worldX, worldY, worldZ);
        if (c == null) return 15;
        int lx = worldX & 15;
        int lz = worldZ & 15;
        de.delautrer.game.world.persistence.WorldPalette palette = chunkManager.getWorld() != null ? chunkManager.getWorld().getBlockPalette() : null;
        return c.getBlock(lx, worldY, lz, palette).getOpacity(c.getBlockState(lx, worldY, lz, palette));
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