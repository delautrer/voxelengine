package de.delautrer.engine.graphics;

import de.delautrer.game.world.Chunk;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

public class VulkanMesh {

    private final VulkanContext context;
    private VulkanBuffer vertexBuffer;
    private VulkanBuffer indexBuffer;
    private int indexCount;

    public VulkanMesh(VulkanContext context, Chunk chunk) {
        this.context = context;
        createBuffers(chunk.getVertices(), chunk.getIndices());
    }

    public VulkanMesh(VulkanContext context, float[] vertices, int[] indices) {
        this.context = context;
        createBuffers(vertices, indices);
    }

    public VulkanMesh(VulkanContext context, MeshData data) {
        this.context = context;
        createBuffers(data.vertices, data.indices);
    }

    // --- DIE NEUE UPDATE METHODE ---
    public void updateMesh(MeshData data) {
        VK10.vkQueueWaitIdle(context.getGraphicsQueue());
        cleanup();
        createBuffers(data.vertices, data.indices);
    }

    // Falls du die alte Methode noch irgendwo hast (kann eigentlich weg):
    public void updateMesh(Chunk chunk) {
        VK10.vkQueueWaitIdle(context.getGraphicsQueue());
        cleanup();
        createBuffers(chunk.getVertices(), chunk.getIndices());
    }

    private void createBuffers(float[] vertices, int[] indices) {
        indexCount = indices.length;

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

    public long getVertexBuffer() {
        return vertexBuffer != null ? vertexBuffer.getBuffer() : 0;
    }

    public long getIndexBuffer() {
        return indexBuffer != null ? indexBuffer.getBuffer() : 0;
    }

    public int getIndexCount() {
        return indexCount;
    }

    public void cleanup() {
        if (vertexBuffer != null) vertexBuffer.cleanup();
        if (indexBuffer != null) indexBuffer.cleanup();
    }
}