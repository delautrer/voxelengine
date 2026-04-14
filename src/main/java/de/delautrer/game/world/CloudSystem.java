package de.delautrer.game.world;

import de.delautrer.engine.graphics.MeshData;
import java.util.ArrayList;
import java.util.List;

public class CloudSystem {

    public static float CLOUD_ALPHA = 0.60f;
    private static final int MAP_SIZE = 128;
    private static final float CLOUD_SCALE = 12.0f;
    private static final float CLOUD_HEIGHT = 160.0f;
    private static final float SPEED = 1.5f;

    private float offsetX = 0.0f;

    public void update(float deltaTime) {
        offsetX -= SPEED * deltaTime;
    }

    public MeshData generateCloudMesh(long seed) {
        NoiseGenerator noise = new NoiseGenerator(seed * 777L);
        List<Float> verticesList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        // 2D-Array speichert die Höhe der Wolke (0 bis 3 Segmente dick)
        int[][] cloudMap = new int[MAP_SIZE][MAP_SIZE];

        /*
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                float n = noise.getFractalNoise2D(x * 0.06f, z * 0.06f, 3, 0.5f, 2.0f);
                int h = 0;
                if (n > 0.0f) h = 1;
                if (n > 0.18f) h = 2;
                if (n > 0.35f) h = 3;
                cloudMap[x][z] = h;
            }
        }
        */
        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                float n = noise.getFractalNoise2D(x * 0.05f, z * 0.05f, 3, 0.5f, 2.0f);

                int h = 0;
                if (n > 0.15f) {
                    h = 1;
                }

                cloudMap[x][z] = h;
            }
        }

        int indexOffset = 0;
        int texIndex = 4; // Dein gewünschter Textur-Index

        for (int x = 0; x < MAP_SIZE; x++) {
            for (int z = 0; z < MAP_SIZE; z++) {
                int height = cloudMap[x][z];
                if (height == 0) continue;

                for (int cy = 0; cy < height; cy++) {
                    float wx = x * CLOUD_SCALE;
                    float wy = CLOUD_HEIGHT + (cy * CLOUD_SCALE);
                    float wz = z * CLOUD_SCALE;

                    // Face Culling: Nur sichtbare Außenseiten zeichnen
                    boolean drawTop = (cy == height - 1);
                    boolean drawBottom = (cy == 0);
                    boolean drawLeft = (x == 0 || cloudMap[x - 1][z] <= cy);
                    boolean drawRight = (x == MAP_SIZE - 1 || cloudMap[x + 1][z] <= cy);
                    boolean drawFront = (z == MAP_SIZE - 1 || cloudMap[x][z + 1] <= cy);
                    boolean drawBack = (z == 0 || cloudMap[x][z - 1] <= cy);

                    if (drawTop) {
                        float ao0 = calcAO(x, cy + 1, z, -1, 0, -1, -1, 0, 0, 0, 0, -1, cloudMap);
                        float ao1 = calcAO(x, cy + 1, z, -1, 0, 1, -1, 0, 0, 0, 0, 1, cloudMap);
                        float ao2 = calcAO(x, cy + 1, z, 1, 0, 1, 1, 0, 0, 0, 0, 1, cloudMap);
                        float ao3 = calcAO(x, cy + 1, z, 1, 0, -1, 1, 0, 0, 0, 0, -1, cloudMap);
                        indexOffset = addCloudQuad(verticesList, indicesList, indexOffset,
                                wx, wy + CLOUD_SCALE, wz, ao0, wx, wy + CLOUD_SCALE, wz + CLOUD_SCALE, ao1,
                                wx + CLOUD_SCALE, wy + CLOUD_SCALE, wz + CLOUD_SCALE, ao2, wx + CLOUD_SCALE, wy + CLOUD_SCALE, wz, ao3,
                                texIndex, 1.0f);
                    }
                    if (drawBottom) {
                        float ao0 = calcAO(x, cy - 1, z, -1, 0, 1, -1, 0, 0, 0, 0, 1, cloudMap);
                        float ao1 = calcAO(x, cy - 1, z, -1, 0, -1, -1, 0, 0, 0, 0, -1, cloudMap);
                        float ao2 = calcAO(x, cy - 1, z, 1, 0, -1, 1, 0, 0, 0, 0, -1, cloudMap);
                        float ao3 = calcAO(x, cy - 1, z, 1, 0, 1, 1, 0, 0, 0, 0, 1, cloudMap);
                        indexOffset = addCloudQuad(verticesList, indicesList, indexOffset,
                                wx, wy, wz + CLOUD_SCALE, ao0, wx, wy, wz, ao1,
                                wx + CLOUD_SCALE, wy, wz, ao2, wx + CLOUD_SCALE, wy, wz + CLOUD_SCALE, ao3,
                                texIndex, 0.6f);
                    }
                    if (drawLeft) {
                        float ao0 = calcAO(x - 1, cy, z, 0, -1, -1, 0, -1, 0, 0, 0, -1, cloudMap);
                        float ao1 = calcAO(x - 1, cy, z, 0, -1, 1, 0, -1, 0, 0, 0, 1, cloudMap);
                        float ao2 = calcAO(x - 1, cy, z, 0, 1, 1, 0, 1, 0, 0, 0, 1, cloudMap);
                        float ao3 = calcAO(x - 1, cy, z, 0, 1, -1, 0, 1, 0, 0, 0, -1, cloudMap);
                        indexOffset = addCloudQuad(verticesList, indicesList, indexOffset,
                                wx, wy, wz, ao0, wx, wy, wz + CLOUD_SCALE, ao1,
                                wx, wy + CLOUD_SCALE, wz + CLOUD_SCALE, ao2, wx, wy + CLOUD_SCALE, wz, ao3,
                                texIndex, 0.8f);
                    }
                    if (drawRight) {
                        float ao0 = calcAO(x + 1, cy, z, 0, -1, 1, 0, -1, 0, 0, 0, 1, cloudMap);
                        float ao1 = calcAO(x + 1, cy, z, 0, -1, -1, 0, -1, 0, 0, 0, -1, cloudMap);
                        float ao2 = calcAO(x + 1, cy, z, 0, 1, -1, 0, 1, 0, 0, 0, -1, cloudMap);
                        float ao3 = calcAO(x + 1, cy, z, 0, 1, 1, 0, 1, 0, 0, 0, 1, cloudMap);
                        indexOffset = addCloudQuad(verticesList, indicesList, indexOffset,
                                wx + CLOUD_SCALE, wy, wz + CLOUD_SCALE, ao0, wx + CLOUD_SCALE, wy, wz, ao1,
                                wx + CLOUD_SCALE, wy + CLOUD_SCALE, wz, ao2, wx + CLOUD_SCALE, wy + CLOUD_SCALE, wz + CLOUD_SCALE, ao3,
                                texIndex, 0.8f);
                    }
                    if (drawFront) {
                        float ao0 = calcAO(x, cy, z + 1, -1, -1, 0, -1, 0, 0, 0, -1, 0, cloudMap);
                        float ao1 = calcAO(x, cy, z + 1, 1, -1, 0, 1, 0, 0, 0, -1, 0, cloudMap);
                        float ao2 = calcAO(x, cy, z + 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, cloudMap);
                        float ao3 = calcAO(x, cy, z + 1, -1, 1, 0, -1, 0, 0, 0, 1, 0, cloudMap);
                        indexOffset = addCloudQuad(verticesList, indicesList, indexOffset,
                                wx, wy, wz + CLOUD_SCALE, ao0, wx + CLOUD_SCALE, wy, wz + CLOUD_SCALE, ao1,
                                wx + CLOUD_SCALE, wy + CLOUD_SCALE, wz + CLOUD_SCALE, ao2, wx, wy + CLOUD_SCALE, wz + CLOUD_SCALE, ao3,
                                texIndex, 0.9f);
                    }
                    if (drawBack) {
                        float ao0 = calcAO(x, cy, z - 1, 1, -1, 0, 1, 0, 0, 0, -1, 0, cloudMap);
                        float ao1 = calcAO(x, cy, z - 1, -1, -1, 0, -1, 0, 0, 0, -1, 0, cloudMap);
                        float ao2 = calcAO(x, cy, z - 1, -1, 1, 0, -1, 0, 0, 0, 1, 0, cloudMap);
                        float ao3 = calcAO(x, cy, z - 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, cloudMap);
                        indexOffset = addCloudQuad(verticesList, indicesList, indexOffset,
                                wx + CLOUD_SCALE, wy, wz, ao0, wx, wy, wz, ao1,
                                wx, wy + CLOUD_SCALE, wz, ao2, wx + CLOUD_SCALE, wy + CLOUD_SCALE, wz, ao3,
                                texIndex, 0.9f);
                    }
                }
            }
        }

        float[] vertices = new float[verticesList.size()];
        for (int i = 0; i < verticesList.size(); i++) vertices[i] = verticesList.get(i);
        int[] indices = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) indices[i] = indicesList.get(i);

        return new MeshData(vertices, indices);
    }

    private float calcAO(int cx, int cy, int cz, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, int dx3, int dy3, int dz3, int[][] cloudMap) {
        boolean s1 = hasCloud(cx + dx1, cy + dy1, cz + dz1, cloudMap);
        boolean s2 = hasCloud(cx + dx2, cy + dy2, cz + dz2, cloudMap);
        boolean s3 = hasCloud(cx + dx3, cy + dy3, cz + dz3, cloudMap);

        int count = (s1 ? 1 : 0) + (s2 ? 1 : 0) + (s3 ? 1 : 0);
        /*return switch (count) {
            case 0 -> 1.0f;
            case 1 -> 0.85f;
            case 2 -> 0.7f;
            default -> 0.55f;
        };
         */
        return 1;
    }

    private boolean hasCloud(int x, int y, int z, int[][] cloudMap) {
        if (x < 0 || x >= MAP_SIZE || z < 0 || z >= MAP_SIZE) return false;
        return cloudMap[x][z] > y && y >= 0;
    }

    private int addCloudQuad(List<Float> v, List<Integer> i, int offset,
                             float x0, float y0, float z0, float ao0,
                             float x1, float y1, float z1, float ao1,
                             float x2, float y2, float z2, float ao2,
                             float x3, float y3, float z3, float ao3,
                             int texIndex, float lightMult) {

        // Vertex 0
        v.add(x0); v.add(y0); v.add(z0);
        v.add(lightMult * ao0); v.add(lightMult * ao0); v.add(lightMult * ao0); v.add(CLOUD_ALPHA);
        v.add(0.0f); v.add(1.0f); v.add((float) texIndex);
        v.add(1.0f); v.add(0.0f);

        // Vertex 1
        v.add(x1); v.add(y1); v.add(z1);
        v.add(lightMult * ao1); v.add(lightMult * ao1); v.add(lightMult * ao1); v.add(CLOUD_ALPHA);
        v.add(1.0f); v.add(1.0f); v.add((float) texIndex);
        v.add(1.0f); v.add(0.0f);

        // Vertex 2
        v.add(x2); v.add(y2); v.add(z2);
        v.add(lightMult * ao2); v.add(lightMult * ao2); v.add(lightMult * ao2); v.add(CLOUD_ALPHA);
        v.add(1.0f); v.add(0.0f); v.add((float) texIndex);
        v.add(1.0f); v.add(0.0f);

        // Vertex 3
        v.add(x3); v.add(y3); v.add(z3);
        v.add(lightMult * ao3); v.add(lightMult * ao3); v.add(lightMult * ao3); v.add(CLOUD_ALPHA);
        v.add(0.0f); v.add(0.0f); v.add((float) texIndex);
        v.add(1.0f); v.add(0.0f);

        i.add(offset); i.add(offset + 1); i.add(offset + 2);
        i.add(offset + 2); i.add(offset + 3); i.add(offset);

        return offset + 4;
    }

    public org.joml.Vector3f getRenderOffset(float playerX, float playerZ) {
        float totalSize = MAP_SIZE * CLOUD_SCALE;
        float nX = Math.round((playerX - offsetX) / totalSize);
        float renderX = offsetX + (nX * totalSize) - (totalSize / 2f);
        float nZ = Math.round(playerZ / totalSize);
        float renderZ = (nZ * totalSize) - (totalSize / 2f);
        return new org.joml.Vector3f(renderX, 0, renderZ);
    }
}