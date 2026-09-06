package de.delautrer.engine.graphics.vulkan.buffer;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

@SuppressWarnings("this-escape")
public class VulkanMesh implements de.delautrer.engine.graphics.IMesh {
    private final VulkanContext context;
    private VulkanBuffer vertexBuffer;
    private VulkanBuffer indexBuffer;
    private int indexCount;
    private long maxVertexBufferSize = 0;
    private long maxIndexBufferSize = 0;

    private static class PendingBufferToDelete {
        final VulkanBuffer buffer;
        int framesToLive;

        PendingBufferToDelete(VulkanBuffer buffer, int framesToLive) {
            this.buffer = buffer;
            this.framesToLive = framesToLive;
        }
    }

    private final java.util.List<PendingBufferToDelete> pendingBuffers = new java.util.ArrayList<>();

    public float chunkOffsetX = 0.0f;
    public float chunkOffsetZ = 0.0f;

    @Override
    public void setChunkOffset(float x, float z) {
        this.chunkOffsetX = x;
        this.chunkOffsetZ = z;
    }

    public VulkanMesh(VulkanContext context, float[] vertices, int[] indices) {
        this.context = context;
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[VulkanMesh] Creating new mesh (float[], int[]) with " + vertices.length + " elements (" + (vertices.length / 8) + " vertices) and " + indices.length + " indices");
        }
        updateMesh(vertices, indices);
    }

    public VulkanMesh(VulkanContext context, float[] vertices, int vertexCount, int[] indices, int indexCount) {
        this.context = context;
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[VulkanMesh] Creating new mesh (float[], count, int[], count) with " + vertexCount + " elements (" + (vertexCount / 8) + " vertices) and " + indexCount + " indices");
        }
        updateMesh(vertices, vertexCount, indices, indexCount);
    }

    public VulkanMesh(VulkanContext context, MeshData data) {
        this.context = context;
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[VulkanMesh] Creating new mesh (MeshData) with " + data.vertices().length + " elements (" + (data.vertices().length / 8) + " vertices) and " + data.indices().length + " indices");
        }
        updateMesh(data.vertices(), data.indices());
    }

    public final void updateMesh(MeshData data) {
        updateMesh(data.vertices(), data.indices());
    }

    public final void updateMesh(float[] vertices, int[] indices) {
        updateMesh(vertices, vertices.length, indices, indices.length);
    }

    public final void updateMesh(float[] vertices, int vertexCount, int[] indices, int indexCount) {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[VulkanMesh] Updating mesh 0x" + Integer.toHexString(hashCode()) + ": " + vertexCount + " elements (" + (vertexCount / 8) + " vertices), " + indexCount + " indices. Max sizes: vertex=" + maxVertexBufferSize + " bytes, index=" + maxIndexBufferSize + " bytes");
        }

        // Processing old pending buffers without blocking the thread
        for (int i = pendingBuffers.size() - 1; i >= 0; i--) {
            PendingBufferToDelete item = pendingBuffers.get(i);
            item.framesToLive--;
            if (item.framesToLive <= 0) {
                item.buffer.cleanup();
                pendingBuffers.remove(i);
            }
        }

        this.indexCount = indexCount;
        if (indexCount == 0)
            return;

        long requiredVertexSize = (long) vertexCount * Float.BYTES;
        long requiredIndexSize = (long) indexCount * Integer.BYTES;

        // --- 1. VERTEX BUFFER UPDATE ---
        if (requiredVertexSize > maxVertexBufferSize) {
            if (vertexBuffer != null) {
                pendingBuffers.add(new PendingBufferToDelete(vertexBuffer, 3));
                vertexBuffer = null;
            }

            maxVertexBufferSize = Math.max((long) (requiredVertexSize * 2.0), 128 * 1024L);

            vertexBuffer = new VulkanBuffer(context, maxVertexBufferSize,
                    VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), vertexBuffer.getBufferMemory(), 0, requiredVertexSize, 0, data);
            java.nio.FloatBuffer floatBuffer = MemoryUtil.memFloatBuffer(data.get(0), vertexCount);
            floatBuffer.put(vertices, 0, vertexCount);
            VK10.vkUnmapMemory(context.getDevice(), vertexBuffer.getBufferMemory());
        }

        // --- 2. INDEX BUFFER UPDATE ---
        if (requiredIndexSize > maxIndexBufferSize) {
            if (indexBuffer != null) {
                pendingBuffers.add(new PendingBufferToDelete(indexBuffer, 3));
                indexBuffer = null;
            }

            maxIndexBufferSize = Math.max((long) (requiredIndexSize * 2.0), 64 * 1024L);

            indexBuffer = new VulkanBuffer(context, maxIndexBufferSize,
                    VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), indexBuffer.getBufferMemory(), 0, requiredIndexSize, 0, data);
            java.nio.IntBuffer intBuffer = MemoryUtil.memIntBuffer(data.get(0), indexCount);
            intBuffer.put(indices, 0, indexCount);
            VK10.vkUnmapMemory(context.getDevice(), indexBuffer.getBufferMemory());
        }
    }

    public long getVertexBuffer() {
        return vertexBuffer != null ? vertexBuffer.getBuffer() : 0;
    }

    public long getIndexBuffer() {
        return indexBuffer != null ? indexBuffer.getBuffer() : 0;
    }

    @Override
    public int getIndexCount() {
        return indexCount;
    }

    public void cleanup() {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[VulkanMesh] Cleaning up mesh 0x" + Integer.toHexString(hashCode()) + " (vertexBuffer: 0x" + (vertexBuffer != null ? Long.toHexString(vertexBuffer.getBuffer()) : "null") + ", indexBuffer: 0x" + (indexBuffer != null ? Long.toHexString(indexBuffer.getBuffer()) : "null") + ")");
        }
        for (PendingBufferToDelete item : pendingBuffers) {
            item.buffer.cleanup();
        }
        pendingBuffers.clear();

        if (vertexBuffer != null) {
            vertexBuffer.cleanup();
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            indexBuffer.cleanup();
            indexBuffer = null;
        }
        indexCount = 0;
        maxVertexBufferSize = 0;
    }
}
