package de.delautrer.engine.graphics;

import de.delautrer.Constants;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.BlockRegistry;
import de.delautrer.game.world.Chunk;
import de.delautrer.game.world.ChunkManager;

public class ChunkMesher {

    private static final ThreadLocal<MeshBuffers> MESH_BUFFER = ThreadLocal.withInitial(MeshBuffers::new);

    public record ChunkMeshResult(MeshData opaque, MeshData water) {}

    private static class MeshBuffers {
        float[] opaqueVertices = new float[131072];
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

    public static ChunkMeshResult generateMeshData(Chunk chunk, ChunkManager cm) {
        MeshBuffers buf = MESH_BUFFER.get();
        buf.reset();

        byte[] blocks = chunk.getBlocks();
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int y = 0; y < Chunk.HEIGHT; y++) {
                for (int z = 0; z < Chunk.SIZE; z++) {
                    int index = (x << 12) | (z << 8) | y;
                    byte id = blocks[index];
                    if (id != 0) {
                        Block block = BlockRegistry.get(id);
                        block.generateMesh(x, y, z, chunk, cm);
                    }
                }
            }
        }
        return buf.createResult();
    }

    public static void addFace(float x0, float y0, float z0, float ao0,
                               float x1, float y1, float z1, float ao1,
                               float x2, float y2, float z2, float ao2,
                               float x3, float y3, float z3, float ao3,
                               float u0, float v0, float u1, float v1,
                               float texLayer, float directionalLight, Block block,
                               float sl0, float sl1, float sl2, float sl3,
                               float bl0, float bl1, float bl2, float bl3) {

        MeshBuffers buf = MESH_BUFFER.get();
        boolean isWater = (block == BlockRegistry.get(Constants.NAMESPACE + ":water"));

        if (isWater) buf.ensureWater(48, 6);
        else buf.ensureOpaque(48, 6);

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

        if (isWater) buf.waterVertexCount = vIdx; else buf.opaqueVertexCount = vIdx;

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
}
