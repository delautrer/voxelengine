package de.delautrer.game.world;

import de.delautrer.engine.graphics.MeshData;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.blocks.state.BlockState;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    private static final ThreadLocal<MeshBuffers> MESH_BUFFER = ThreadLocal.withInitial(MeshBuffers::new);

    public record ChunkMeshResult(MeshData opaque, MeshData water) {}

    private static final float[] highlightVertices = { 0,0,0, 1,0,0, 1,1,0, 0,1,0, 0,0,1, 1,0,1, 1,1,1, 0,1,1 };
    private static final int[] highlightIndices = { 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };

    public Chunk(int worldX, int worldZ) {
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.lastAccessedTime = System.currentTimeMillis();
    }


    private static class MeshBuffers {
        float[] opaqueVertices = new float[131072]; // Groß genug für 99% aller Chunks (512 KB)
        int[] opaqueIndices = new int[32768];
        int opaqueVertexCount = 0;
        int opaqueIndexCount = 0;

        float[] waterVertices = new float[32768];
        int[] waterIndices = new int[8192];
        int waterVertexCount = 0;
        int waterIndexCount = 0;

        void reset() {
            opaqueVertexCount = 0;
            opaqueIndexCount = 0;
            waterVertexCount = 0;
            waterIndexCount = 0;
        }

        void ensureOpaque(int v, int i) {
            if (opaqueVertexCount + v > opaqueVertices.length) {
                float[] newArr = new float[Math.max(opaqueVertices.length * 2, opaqueVertexCount + v)];
                System.arraycopy(opaqueVertices, 0, newArr, 0, opaqueVertexCount);
                opaqueVertices = newArr;
            }
            if (opaqueIndexCount + i > opaqueIndices.length) {
                int[] newArr = new int[Math.max(opaqueIndices.length * 2, opaqueIndexCount + i)];
                System.arraycopy(opaqueIndices, 0, newArr, 0, opaqueIndexCount);
                opaqueIndices = newArr;
            }
        }

        void ensureWater(int v, int i) {
            if (waterVertexCount + v > waterVertices.length) {
                float[] newArr = new float[Math.max(waterVertices.length * 2, waterVertexCount + v)];
                System.arraycopy(waterVertices, 0, newArr, 0, waterVertexCount);
                waterVertices = newArr;
            }
            if (waterIndexCount + i > waterIndices.length) {
                int[] newArr = new int[Math.max(waterIndices.length * 2, waterIndexCount + i)];
                System.arraycopy(waterIndices, 0, newArr, 0, waterIndexCount);
                waterIndices = newArr;
            }
        }

        ChunkMeshResult createResult() {
            // Nur hier ganz am Ende wird einmalig exakt kopiert, damit wir Vulkan den Buffer geben können.
            float[] oVerts = new float[opaqueVertexCount];
            System.arraycopy(opaqueVertices, 0, oVerts, 0, opaqueVertexCount);
            int[] oInds = new int[opaqueIndexCount];
            System.arraycopy(opaqueIndices, 0, oInds, 0, opaqueIndexCount);

            float[] wVerts = new float[waterVertexCount];
            System.arraycopy(waterVertices, 0, wVerts, 0, waterVertexCount);
            int[] wInds = new int[waterIndexCount];
            System.arraycopy(waterIndices, 0, wInds, 0, waterIndexCount);

            return new ChunkMeshResult(new MeshData(oVerts, oInds), new MeshData(wVerts, wInds));
        }
    }

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
        if (blockId == 0) return BlockRegistry.AIR.getDefaultState();

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

        if (side1 && side2) return 0.75f; // Vorher: 0.5f

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
                    if (block.getId() == BlockRegistry.WATER.getId()) {
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
        this.biomeMap[getBiomeIndex(x, z)] = biome;
    }

    public Biome getBiome(int x, int z) {
        return this.biomeMap[getBiomeIndex(x, z)];
    }

    // Rendering
    public void requestMeshUpdate() {
        this.needsMeshUpdate = true;
    }
    public ChunkMeshResult generateMeshData(ChunkManager cm) {
        MeshBuffers buf = MESH_BUFFER.get();
        buf.reset();

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = blocks[getIndex(x, y, z)];
                    if (id != 0) {
                        Block block = BlockRegistry.get(id);
                        block.generateMesh(x, y, z, this, cm);
                    }
                }
            }
        }
        return buf.createResult();
    }

    public void addFace(float x0, float y0, float z0, float ao0,
                        float x1, float y1, float z1, float ao1,
                        float x2, float y2, float z2, float ao2,
                        float x3, float y3, float z3, float ao3,
                        float u0, float v0, float u1, float v1,
                        float texLayer, float directionalLight, Block block,
                        float sl0, float sl1, float sl2, float sl3,
                        float bl0, float bl1, float bl2, float bl3) {

        MeshBuffers buf = MESH_BUFFER.get();
        boolean isWater = (block == BlockRegistry.WATER);
        //float ox = worldX * SIZE, oz = worldZ * SIZE;

        // Kapazitäten prüfen
        if (isWater) buf.ensureWater(48, 6);
        else buf.ensureOpaque(48, 6);

        // Aktuelle Daten auswählen
        float[] targetVertices = isWater ? buf.waterVertices : buf.opaqueVertices;
        int vIdx = isWater ? buf.waterVertexCount : buf.opaqueVertexCount;
        int offset = vIdx / 12;

        float r = 1.0f, g = 1.0f, b = 1.0f, alpha = 1.0f;
        if (isWater) {
            r = 0.2f; g = 0.5f; b = 1.0f; alpha = 0.7f;
            directionalLight = Math.min(1.0f, directionalLight * 1.2f);
        }

        float c0 = ao0 * directionalLight;
        float c1 = ao1 * directionalLight;
        float c2 = ao2 * directionalLight;
        float c3 = ao3 * directionalLight;

        // Vertex 0
        targetVertices[vIdx++] = x0; targetVertices[vIdx++] = y0; targetVertices[vIdx++] = z0;
        targetVertices[vIdx++] = c0 * r;  targetVertices[vIdx++] = c0 * g; targetVertices[vIdx++] = c0 * b; targetVertices[vIdx++] = alpha;
        targetVertices[vIdx++] = u0;      targetVertices[vIdx++] = v1; targetVertices[vIdx++] = texLayer;
        targetVertices[vIdx++] = sl0;     targetVertices[vIdx++] = bl0;

        // Vertex 1
        targetVertices[vIdx++] = x1; targetVertices[vIdx++] = y1; targetVertices[vIdx++] = z1;
        targetVertices[vIdx++] = c1 * r;  targetVertices[vIdx++] = c1 * g; targetVertices[vIdx++] = c1 * b; targetVertices[vIdx++] = alpha;
        targetVertices[vIdx++] = u1;      targetVertices[vIdx++] = v1; targetVertices[vIdx++] = texLayer;
        targetVertices[vIdx++] = sl1;     targetVertices[vIdx++] = bl1;

        // Vertex 2
        targetVertices[vIdx++] = x2; targetVertices[vIdx++] = y2; targetVertices[vIdx++] = z2;
        targetVertices[vIdx++] = c2 * r;  targetVertices[vIdx++] = c2 * g; targetVertices[vIdx++] = c2 * b; targetVertices[vIdx++] = alpha;
        targetVertices[vIdx++] = u1;      targetVertices[vIdx++] = v0; targetVertices[vIdx++] = texLayer;
        targetVertices[vIdx++] = sl2;     targetVertices[vIdx++] = bl2;

        // Vertex 3
        targetVertices[vIdx++] = x3; targetVertices[vIdx++] = y3; targetVertices[vIdx++] = z3;
        targetVertices[vIdx++] = c3 * r;  targetVertices[vIdx++] = c3 * g; targetVertices[vIdx++] = c3 * b; targetVertices[vIdx++] = alpha;
        targetVertices[vIdx++] = u0;      targetVertices[vIdx++] = v0; targetVertices[vIdx++] = texLayer;
        targetVertices[vIdx++] = sl3;     targetVertices[vIdx++] = bl3;

        // Zähler zurückschreiben
        if (isWater) buf.waterVertexCount = vIdx; else buf.opaqueVertexCount = vIdx;

        // Indices befüllen
        int[] targetIndices = isWater ? buf.waterIndices : buf.opaqueIndices;
        int iIdx = isWater ? buf.waterIndexCount : buf.opaqueIndexCount;

        if (ao0 + ao2 > ao1 + ao3) {
            targetIndices[iIdx++] = offset + 1; targetIndices[iIdx++] = offset + 2; targetIndices[iIdx++] = offset + 3;
            targetIndices[iIdx++] = offset + 3; targetIndices[iIdx++] = offset + 0; targetIndices[iIdx++] = offset + 1;
        } else {
            targetIndices[iIdx++] = offset + 0; targetIndices[iIdx++] = offset + 1; targetIndices[iIdx++] = offset + 2;
            targetIndices[iIdx++] = offset + 2; targetIndices[iIdx++] = offset + 3; targetIndices[iIdx++] = offset + 0;
        }

        if (isWater) buf.waterIndexCount = iIdx; else buf.opaqueIndexCount = iIdx;
    }

    public void clearMeshCache() {

    }

    public static float[] getHighlightVertices() { return highlightVertices; }
    public static int[] getHighlightIndices() { return highlightIndices; }

    // Persistence
    public byte[] serialize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos);
             DataOutputStream dos = new DataOutputStream(gzip)) {

            dos.writeInt(worldX);
            dos.writeInt(worldZ);

            dos.write(blocks);
            dos.write(states);
            dos.write(lightMap);
        }
        return baos.toByteArray();
    }

    public void deserialize(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (GZIPInputStream gzip = new GZIPInputStream(bais);
             DataInputStream dis = new java.io.DataInputStream(gzip)) {

            int savedX = dis.readInt();
            int savedZ = dis.readInt();
            if (savedX != this.worldX || savedZ != this.worldZ) {
                throw new IOException("Chunk-Coordinates are not equal!");
            }

            dis.readFully(blocks);
            dis.readFully(states);
            dis.readFully(lightMap);
        }
        this.clearDirty();
    }

    // Getter & Setter

    // X rückt um 12 bit nach links, Z um 8 bit, Y füllt die ersten 8 bit.
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
    public boolean isDirty() {
        return isDirty;
    }
    public void clearDirty() {
        this.isDirty = false;
    }
    public boolean needsMeshUpdate() { return needsMeshUpdate; }
    public void clearMeshUpdate() { this.needsMeshUpdate = false; }
    public void access() {
        this.lastAccessedTime = System.currentTimeMillis();
    }
    public long getLastAccessedTime() {
        return lastAccessedTime;
    }
}