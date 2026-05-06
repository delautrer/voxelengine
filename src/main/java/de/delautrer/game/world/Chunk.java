package de.delautrer.game.world;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;
import java.io.*;
import de.delautrer.engine.graphics.ChunkMesher;




public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 256;

    public static final int VOLUME = SIZE * HEIGHT * SIZE; // 65.536 Blöcke
    private final Biome[] biomeMap = new Biome[SIZE * SIZE];
    private final byte[] blocks = new byte[VOLUME];
    private final byte[] states = new byte[VOLUME];
    private final byte[] lightMap = new byte[VOLUME];

    private final int worldX, worldZ;
    private boolean isDirty = false;
    private boolean needsMeshUpdate = false;
    private long lastAccessedTime;

    private static final float[] highlightVertices = { 0,0,0, 1,0,0, 1,1,0, 0,1,0, 0,0,1, 1,0,1, 1,1,1, 0,1,1 };
    private static final int[] highlightIndices = { 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };

    public Chunk(int worldX, int worldZ) {
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.lastAccessedTime = System.currentTimeMillis();
    }

    public byte[] getBlocks() { return blocks; }
    public byte[] getStates() { return states; }
    public byte[] getLightMap() { return lightMap; }
    public Biome[] getBiomeMap() { return biomeMap; }

    // Blocks
    public void setBlock(int x, int y, int z, byte type, byte state) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        int idx = getIndex(x, y, z);
        if (blocks[idx] != type || states[idx] != state) {
            blocks[idx] = type;
            states[idx] = state;
            this.isDirty = true;
            this.needsMeshUpdate = true;
        }
    }
    public void setBlock(int x, int y, int z, byte type) {
        setBlock(x, y, z, type, (byte)0);
    }
    public byte getBlockAt(int x, int y, int z, ChunkManager cm) {
        if (y < 0 || y >= HEIGHT) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return blocks[getIndex(x, y, z)];
        if (cm == null) return 0;
        Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
        if (neighbor == null) return 0;
        return neighbor.getBlock(Math.floorMod(worldX * SIZE + x, SIZE), y, Math.floorMod(worldZ * SIZE + z, SIZE));
    }
    public byte getBlock(int x, int y, int z) { return getBlockAt(x,y,z, null); }
    public byte getStateAt(int x, int y, int z, ChunkManager cm) {
        if (y < 0 || y >= HEIGHT) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return states[getIndex(x, y, z)];
        if (cm == null) return 0;
        Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
        if (neighbor == null) return 0;
        return neighbor.getState(Math.floorMod(worldX * SIZE + x, SIZE), y, Math.floorMod(worldZ * SIZE + z, SIZE));
    }
    public byte getState(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return 0;
        return states[getIndex(x, y, z)];
    }
    public BlockState getBlockState(int x, int y, int z) {
        byte blockId = getBlock(x, y, z);
        if (blockId == 0) return BlockRegistry.get((byte)0).getDefaultState();

        byte stateId = getState(x, y, z);
        return BlockRegistry.get(blockId).getStateForId(stateId);
    }

    // Block - Light
    public void setBlockLight(int x, int y, int z, int val) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        int idx = getIndex(x, y, z);
        lightMap[idx] = (byte) ((lightMap[idx] & 0xF0) | (val & 0x0F));
    }
    public void setSkyLight(int x, int y, int z, int val) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        int idx = getIndex(x, y, z);
        lightMap[idx] = (byte) ((lightMap[idx] & 0x0F) | ((val & 0x0F) << 4));
    }
    public int getBlockLight(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return 0;
        return lightMap[getIndex(x, y, z)] & 0x0F;
    }
    public int getSkyLight(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return 15;
        return (lightMap[getIndex(x, y, z)] >> 4) & 0x0F;
    }
    public int getSkyLightAt(int x, int y, int z, ChunkManager cm) {
        if (y >= HEIGHT) return 15;
        if (y < 0) return 0;

        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return getSkyLight(x, y, z);

        if (cm != null) {
            Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
            if (neighbor != null) {
                return neighbor.getSkyLight(Math.floorMod(worldX * SIZE + x, SIZE), y, Math.floorMod(worldZ * SIZE + z, SIZE));
            }
        }

        int clampX = Math.max(0, Math.min(SIZE - 1, x));
        int clampZ = Math.max(0, Math.min(SIZE - 1, z));
        return getSkyLight(clampX, y, clampZ);
    }

    public int getBlockLightAt(int x, int y, int z, ChunkManager cm) {
        if (y < 0 || y >= HEIGHT) return 0;

        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return getBlockLight(x, y, z);

        if (cm != null) {
            Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
            if (neighbor != null) {
                return neighbor.getBlockLight(Math.floorMod(worldX * SIZE + x, SIZE), y, Math.floorMod(worldZ * SIZE + z, SIZE));
            }
        }

        int clampX = Math.max(0, Math.min(SIZE - 1, x));
        int clampZ = Math.max(0, Math.min(SIZE - 1, z));
        return getBlockLight(clampX, y, clampZ);
    }
    public float getSmoothSkyLight(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        float center = lightToBrightness(getSkyLightAt(x, y, z, cm));
        float side1 = lightToBrightness(getSkyLightAt(x + dx1, y + dy1, z + dz1, cm));
        float side2 = lightToBrightness(getSkyLightAt(x + dx2, y + dy2, z + dz2, cm));
        float corner = lightToBrightness(getSkyLightAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm));

        return (center + side1 + side2 + corner) / 4.0f;
    }
    public float getSmoothBlockLight(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        float center = lightToBrightness(getBlockLightAt(x, y, z, cm));
        float side1 = lightToBrightness(getBlockLightAt(x + dx1, y + dy1, z + dz1, cm));
        float side2 = lightToBrightness(getBlockLightAt(x + dx2, y + dy2, z + dz2, cm));
        float corner = lightToBrightness(getBlockLightAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm));

        return (center + side1 + side2 + corner) / 4.0f;
    }
    public float getAO(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        boolean side1 = !BlockRegistry.get(getBlockAt(x + dx1, y + dy1, z + dz1, cm)).isTransparent;
        boolean side2 = !BlockRegistry.get(getBlockAt(x + dx2, y + dy2, z + dz2, cm)).isTransparent;
        boolean corner = !BlockRegistry.get(getBlockAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm)).isTransparent;

        if (side1 && side2) return 0.75f;

        int count = (side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0);
        return switch (count) {
            case 0 -> 1.0f;
            case 1 -> 0.90f;
            case 2 -> 0.82f;
            default -> 0.75f;
        };
    }
    private float lightToBrightness(float lightLevel) {
        if (lightLevel <= 0) return 0.0f;
        return (float) Math.pow(0.8f, 15.0f - lightLevel);
    }
    public void recalculateSunlightColumn(int x, int z, LightEngine lightEngine) {
        int currentLight = 15;
        for (int y = HEIGHT - 1; y >= 0; y--) {
            byte blockId = blocks[getIndex(x, y, z)];
            int oldLight = getSkyLight(x, y, z);

            if (blockId != 0) {
                Block block = BlockRegistry.get(blockId);
                if (block != null && block.isTransparent) {
                    if (block.getId() == BlockRegistry.get(Constants.NAMESPACE + ":water").getId()) {
                        currentLight = Math.max(0, currentLight - 2);
                    }
                } else {
                    currentLight = 0;
                }
            }

            if (currentLight != oldLight) {
                setSkyLight(x, y, z, currentLight);

                if (lightEngine != null) {
                    int globalX = this.worldX * SIZE + x;
                    int globalZ = this.worldZ * SIZE + z;

                    if (currentLight < oldLight) {
                        lightEngine.addSkyLightRemoval(globalX, y, globalZ, oldLight);
                    } else {
                        lightEngine.addSkyLightUpdate(globalX, y, globalZ);
                    }
                }
            }
        }
    }
    public void calculateSunlight() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                recalculateSunlightColumn(x, z, null);
            }
        }
    }

    // Biome
    public void setBiome(int x, int z, Biome biome) {
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) return;
        this.biomeMap[getBiomeIndex(x, z)] = biome;
    }

    public Biome getBiome(int x, int z) {
        // Sicherstellen, dass die Koordinaten lokal sind, um IndexOutOfBounds zu vermeiden
        if (x < 0 || x >= SIZE || z < 0 || z >= SIZE) {
            return Biome.PLAINS;
        }
        Biome b = this.biomeMap[getBiomeIndex(x, z)];
        return b != null ? b : Biome.PLAINS;
    }

    // Rendering
    public void requestMeshUpdate() {
        this.needsMeshUpdate = true;
    }
    public ChunkMesher.ChunkMeshResult generateMeshData(ChunkManager cm) {
        return ChunkMesher.generateMeshData(this, cm);
    }

    public void addFace(float x0, float y0, float z0, float ao0,
                        float x1, float y1, float z1, float ao1,
                        float x2, float y2, float z2, float ao2,
                        float x3, float y3, float z3, float ao3,
                        float u0, float v0, float u1, float v1,
                        float texLayer, float directionalLight, Block block,
                        float sl0, float sl1, float sl2, float sl3,
                        float bl0, float bl1, float bl2, float bl3) {
        ChunkMesher.addFace(x0, y0, z0, ao0, x1, y1, z1, ao1, x2, y2, z2, ao2, x3, y3, z3, ao3, u0, v0, u1, v1, texLayer, directionalLight, block, sl0, sl1, sl2, sl3, bl0, bl1, bl2, bl3);
    }

    public void clearMeshCache() {}

    public static float[] getHighlightVertices() { return highlightVertices; }
    public static int[] getHighlightIndices() { return highlightIndices; }

    // Getter & Setter
    private int getIndex(int x, int y, int z) {
        return (x << 12) | (z << 8) | y;
    }

    private int getBiomeIndex(int x, int z) {
        return (x << 4) | z;
    }
    public int getWorldX() { return worldX; }
    public int getWorldZ() { return worldZ; }
    public void markDirty() {
        this.isDirty = true;
        this.needsMeshUpdate = true;
    }
    public boolean isDirty() { return isDirty; }
    public void clearDirty() { this.isDirty = false; }
    public boolean needsMeshUpdate() { return needsMeshUpdate; }
    public void clearMeshUpdate() { this.needsMeshUpdate = false; }
    public void access() { this.lastAccessedTime = System.currentTimeMillis(); }
    public long getLastAccessedTime() { return lastAccessedTime; }
}
