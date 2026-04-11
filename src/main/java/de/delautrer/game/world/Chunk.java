package de.delautrer.game.world;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 64;

    private final byte[][][] blocks = new byte[SIZE][HEIGHT][SIZE];
    private final byte[][][] states = new byte[SIZE][HEIGHT][SIZE];

    private final int worldX, worldZ;

    // --- NEU: Primitive Arrays statt ArrayLists! ---
    // Wir starten mit einer moderaten Größe und vergrößern sie bei Bedarf
    private float[] vertices = new float[4096];
    private int vertexCount = 0;

    private int[] indices = new int[1024];
    private int indexCount = 0;

    private static final float[] highlightVertices = { 0,0,0, 1,0,0, 1,1,0, 0,1,0, 0,0,1, 1,0,1, 1,1,1, 0,1,1 };
    private static final int[] highlightIndices = { 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };

    public Chunk(int worldX, int worldZ) {
        this.worldX = worldX;
        this.worldZ = worldZ;
    }

    public void rebuildMesh(ChunkManager cm) {
        // Statt eine Liste zu leeren, setzen wir einfach die Zähler auf 0.
        // Die alten Daten im Array werden später einfach überschrieben! (0 Garbage!)
        vertexCount = 0;
        indexCount = 0;

        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = blocks[x][y][z];
                    if (id != 0) {
                        Block block = BlockRegistry.get(id);
                        block.generateMesh(x, y, z, this, cm);
                    }
                }
            }
        }
    }

    // --- NEU: Array-Resizing Logik ---
    private void ensureVertexCapacity(int additionalSize) {
        if (vertexCount + additionalSize > vertices.length) {
            float[] newArr = new float[Math.max(vertices.length * 2, vertexCount + additionalSize)];
            System.arraycopy(vertices, 0, newArr, 0, vertexCount);
            vertices = newArr;
        }
    }

    private void ensureIndexCapacity(int additionalSize) {
        if (indexCount + additionalSize > indices.length) {
            int[] newArr = new int[Math.max(indices.length * 2, indexCount + additionalSize)];
            System.arraycopy(indices, 0, newArr, 0, indexCount);
            indices = newArr;
        }
    }

    public void addFace(float x0, float y0, float z0, float ao0,
                        float x1, float y1, float z1, float ao1,
                        float x2, float y2, float z2, float ao2,
                        float x3, float y3, float z3, float ao3,
                        int texLayer, float directionalLight, Block block) {

        float ox = worldX * SIZE, oz = worldZ * SIZE;
        float u0 = 0.0f, v0 = 0.0f, u1 = 1.0f, v1 = 1.0f;

        // Da jeder Vertex 10 Floats hat, ist der Offset für den Index:
        int offset = vertexCount / 10;

        float r = 1.0f, g = 1.0f, b = 1.0f, alpha = 1.0f;

        if (block == BlockRegistry.WATER) {
            r = 0.2f; g = 0.5f; b = 1.0f; alpha = 0.7f;
            directionalLight = Math.min(1.0f, directionalLight * 1.2f);
        }

        float c0 = ao0 * directionalLight;
        float c1 = ao1 * directionalLight;
        float c2 = ao2 * directionalLight;
        float c3 = ao3 * directionalLight;

        // Wir brauchen 40 Floats für 4 Vertices (4 * 10)
        ensureVertexCapacity(40);

        // Vertex 0
        vertices[vertexCount++] = x0 + ox; vertices[vertexCount++] = y0; vertices[vertexCount++] = z0 + oz;
        vertices[vertexCount++] = c0 * r;  vertices[vertexCount++] = c0 * g; vertices[vertexCount++] = c0 * b; vertices[vertexCount++] = alpha;
        vertices[vertexCount++] = u0;      vertices[vertexCount++] = v1; vertices[vertexCount++] = texLayer;

        // Vertex 1
        vertices[vertexCount++] = x1 + ox; vertices[vertexCount++] = y1; vertices[vertexCount++] = z1 + oz;
        vertices[vertexCount++] = c1 * r;  vertices[vertexCount++] = c1 * g; vertices[vertexCount++] = c1 * b; vertices[vertexCount++] = alpha;
        vertices[vertexCount++] = u1;      vertices[vertexCount++] = v1; vertices[vertexCount++] = texLayer;

        // Vertex 2
        vertices[vertexCount++] = x2 + ox; vertices[vertexCount++] = y2; vertices[vertexCount++] = z2 + oz;
        vertices[vertexCount++] = c2 * r;  vertices[vertexCount++] = c2 * g; vertices[vertexCount++] = c2 * b; vertices[vertexCount++] = alpha;
        vertices[vertexCount++] = u1;      vertices[vertexCount++] = v0; vertices[vertexCount++] = texLayer;

        // Vertex 3
        vertices[vertexCount++] = x3 + ox; vertices[vertexCount++] = y3; vertices[vertexCount++] = z3 + oz;
        vertices[vertexCount++] = c3 * r;  vertices[vertexCount++] = c3 * g; vertices[vertexCount++] = c3 * b; vertices[vertexCount++] = alpha;
        vertices[vertexCount++] = u0;      vertices[vertexCount++] = v0; vertices[vertexCount++] = texLayer;


        // Wir brauchen 6 Integers für 2 Dreiecke
        ensureIndexCapacity(6);

        if (ao0 + ao2 > ao1 + ao3) {
            indices[indexCount++] = offset + 1; indices[indexCount++] = offset + 2; indices[indexCount++] = offset + 3;
            indices[indexCount++] = offset + 3; indices[indexCount++] = offset + 0; indices[indexCount++] = offset + 1;
        } else {
            indices[indexCount++] = offset + 0; indices[indexCount++] = offset + 1; indices[indexCount++] = offset + 2;
            indices[indexCount++] = offset + 2; indices[indexCount++] = offset + 3; indices[indexCount++] = offset + 0;
        }
    }

    public byte getBlockAt(int x, int y, int z, ChunkManager cm) {
        if (y < 0 || y >= HEIGHT) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return blocks[x][y][z];
        if (cm == null) return 0;
        Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
        if (neighbor == null) return 0;
        return neighbor.getBlock(Math.floorMod(worldX * SIZE + x, SIZE), y, Math.floorMod(worldZ * SIZE + z, SIZE));
    }

    public byte getStateAt(int x, int y, int z, ChunkManager cm) {
        if (y < 0 || y >= HEIGHT) return 0;
        if (x >= 0 && x < SIZE && z >= 0 && z < SIZE) return states[x][y][z];
        if (cm == null) return 0;
        Chunk neighbor = cm.getChunkAtBlock(worldX * SIZE + x, y, worldZ * SIZE + z);
        if (neighbor == null) return 0;
        return neighbor.getState(Math.floorMod(worldX * SIZE + x, SIZE), y, Math.floorMod(worldZ * SIZE + z, SIZE));
    }

    public float getAO(int x, int y, int z, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, ChunkManager cm) {
        boolean side1 = !BlockRegistry.get(getBlockAt(x + dx1, y + dy1, z + dz1, cm)).isTransparent;
        boolean side2 = !BlockRegistry.get(getBlockAt(x + dx2, y + dy2, z + dz2, cm)).isTransparent;
        boolean corner = !BlockRegistry.get(getBlockAt(x + dx1 + dx2, y + dy1 + dy2, z + dz1 + dz2, cm)).isTransparent;

        if (side1 && side2) return 0.5f;
        int count = (side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0);
        return switch (count) {
            case 0 -> 1.0f;
            case 1 -> 0.8f;
            case 2 -> 0.6f;
            default -> 0.5f;
        };
    }

    public byte getBlock(int x, int y, int z) { return getBlockAt(x,y,z, null); }
    public byte getState(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return 0;
        return states[x][y][z];
    }

    public void setBlock(int x, int y, int z, byte type, byte state) {
        if (x < 0 || x >= SIZE || y < 0 || y >= HEIGHT || z < 0 || z >= SIZE) return;
        blocks[x][y][z] = type;
        states[x][y][z] = state;
    }
    public void setBlock(int x, int y, int z, byte type) { setBlock(x, y, z, type, (byte)0); }

    public void clearMeshCache() {
        vertexCount = 0;
        indexCount = 0;
    }

    // --- NEU: Wir geben eine Kopie genau in der benötigten Größe zurück ---
    // Das ist die einzige Stelle pro Chunk-Update, an der noch ein Objekt (das passgenaue Array)
    // für Vulkan erstellt wird. Das ist extrem speichereffizient!
    public float[] getVertices() {
        float[] arr = new float[vertexCount];
        System.arraycopy(vertices, 0, arr, 0, vertexCount);
        return arr;
    }

    public int[] getIndices() {
        int[] arr = new int[indexCount];
        System.arraycopy(indices, 0, arr, 0, indexCount);
        return arr;
    }

    public static float[] getHighlightVertices() { return highlightVertices; }
    public static int[] getHighlightIndices() { return highlightIndices; }
    public int getWorldX() { return worldX; }
    public int getWorldZ() { return worldZ; }
}