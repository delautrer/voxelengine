package de.delautrer.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

public class VulkanMesh {
    private final VulkanContext context;
    private VulkanBuffer vertexBuffer;
    private VulkanBuffer indexBuffer;
    private int indexCount;

    public VulkanMesh(VulkanContext context, float[] vertices, int[] indices) {
        this.context = context;
        createBuffers(vertices, indices);
    }

    public VulkanMesh(VulkanContext context, MeshData data) {
        this.context = context;
        createBuffers(data.vertices, data.indices);
    }

    public void updateMesh(MeshData data) {
        cleanup(); // Zerstört alte Buffer
        createBuffers(data.vertices, data.indices); // Baut neue auf (falls vorhanden)
    }

    private void createBuffers(float[] vertices, int[] indices) {
        indexCount = indices.length;
        // Wenn kein Wasser/Mesh da ist, bricht er hier ab.
        if (indexCount == 0) return;

        long vertexBufferSize = (long) vertices.length * Float.BYTES;
        vertexBuffer = new VulkanBuffer(context, vertexBufferSize,
                VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), vertexBuffer.getBufferMemory(), 0, vertexBufferSize, 0, data);
            java.nio.FloatBuffer floatBuffer = MemoryUtil.memFloatBuffer(data.get(0), vertices.length);
            floatBuffer.put(vertices);
            VK10.vkUnmapMemory(context.getDevice(), vertexBuffer.getBufferMemory());
        }

        long indexBufferSize = (long) indices.length * Integer.BYTES;
        indexBuffer = new VulkanBuffer(context, indexBufferSize,
                VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), indexBuffer.getBufferMemory(), 0, indexBufferSize, 0, data);
            java.nio.IntBuffer intBuffer = MemoryUtil.memIntBuffer(data.get(0), indices.length);
            intBuffer.put(indices);
            VK10.vkUnmapMemory(context.getDevice(), indexBuffer.getBufferMemory());
        }
    }

    public long getVertexBuffer() { return vertexBuffer != null ? vertexBuffer.getBuffer() : 0; }
    public long getIndexBuffer() { return indexBuffer != null ? indexBuffer.getBuffer() : 0; }
    public int getIndexCount() { return indexCount; }

    public void cleanup() {
        if (vertexBuffer != null) {
            vertexBuffer.cleanup();
            vertexBuffer = null;
        }
        if (indexBuffer != null) {
            indexBuffer.cleanup();
            indexBuffer = null;
        }
        indexCount = 0; // Setzt die Vertices sicherheitshalber wieder auf 0
    }
}