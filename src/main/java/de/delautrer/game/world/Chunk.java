package de.delautrer.game.world;

import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import java.util.ArrayList;
import java.util.List;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 64;

    private final byte[][][] blocks = new byte[SIZE][HEIGHT][SIZE];
    private final byte[][][] states = new byte[SIZE][HEIGHT][SIZE];

    private final int worldX, worldZ;
    private final List<Float> vertices = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();

    private static final float[] highlightVertices = { 0,0,0, 1,0,0, 1,1,0, 0,1,0, 0,0,1, 1,0,1, 1,1,1, 0,1,1 };
    private static final int[] highlightIndices = { 0,1, 1,2, 2,3, 3,0, 4,5, 5,6, 6,7, 7,4, 0,4, 1,5, 2,6, 3,7 };

    public Chunk(int worldX, int worldZ) {
        this.worldX = worldX;
        this.worldZ = worldZ;
    }

    public void rebuildMesh(ChunkManager cm) {
        vertices.clear();
        indices.clear();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int z = 0; z < SIZE; z++) {
                    byte id = blocks[x][y][z];
                    if (id != 0) {
                        // HIER PASSIERT DIE MAGIE: Dynamischer Aufruf der Block-Klasse
                        Block block = BlockRegistry.get(id);
                        block.generateMesh(x, y, z, this, cm);
                    }
                }
            }
        }
    }

    // Von den Blöcken aufgerufen, um Daten ins Mesh zu schreiben
    public void addFace(float x0, float y0, float z0, float ao0,
                        float x1, float y1, float z1, float ao1,
                        float x2, float y2, float z2, float ao2,
                        float x3, float y3, float z3, float ao3,
                        int texLayer, float directionalLight, Block block) {

        float ox = worldX * SIZE, oz = worldZ * SIZE;
        float u0 = 0.0f, v0 = 0.0f, u1 = 1.0f, v1 = 1.0f;
        int offset = vertices.size() / 10;
        float r = 1.0f, g = 1.0f, b = 1.0f, alpha = 1.0f;

        // Wasser Farben/Transparenz
        if (block == BlockRegistry.WATER) {
            r = 0.2f; g = 0.5f; b = 1.0f; alpha = 0.7f;
            directionalLight = Math.min(1.0f, directionalLight * 1.2f);
        }

        float c0 = ao0 * directionalLight;
        float c1 = ao1 * directionalLight;
        float c2 = ao2 * directionalLight;
        float c3 = ao3 * directionalLight;

        vertices.addAll(List.of(
                x0 + ox, y0, z0 + oz, c0 * r, c0 * g, c0 * b, alpha, u0, v1, (float)texLayer,
                x1 + ox, y1, z1 + oz, c1 * r, c1 * g, c1 * b, alpha, u1, v1, (float)texLayer,
                x2 + ox, y2, z2 + oz, c2 * r, c2 * g, c2 * b, alpha, u1, v0, (float)texLayer,
                x3 + ox, y3, z3 + oz, c3 * r, c3 * g, c3 * b, alpha, u0, v0, (float)texLayer
        ));

        if (ao0 + ao2 > ao1 + ao3) {
            indices.addAll(List.of(offset + 1, offset + 2, offset + 3, offset + 3, offset + 0, offset + 1));
        } else {
            indices.addAll(List.of(offset + 0, offset + 1, offset + 2, offset + 2, offset + 3, offset + 0));
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

    public void clearMeshCache() { vertices.clear(); indices.clear(); }
    public float[] getVertices() {
        float[] arr = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) arr[i] = vertices.get(i);
        return arr;
    }
    public int[] getIndices() {
        int[] arr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) arr[i] = indices.get(i);
        return arr;
    }
    public static float[] getHighlightVertices() { return highlightVertices; }
    public static int[] getHighlightIndices() { return highlightIndices; }
    public int getWorldX() { return worldX; }
    public int getWorldZ() { return worldZ; }
}