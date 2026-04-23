package de.delautrer.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

public class VulkanMesh {
    private final VulkanContext context;
    private VulkanBuffer vertexBuffer;
    private VulkanBuffer indexBuffer;
    private int indexCount;

    // Wir merken uns die maximal allokierte Größe in Bytes
    private long maxVertexBufferSize = 0;
    private long maxIndexBufferSize = 0;

    public VulkanMesh(VulkanContext context, float[] vertices, int[] indices) {
        this.context = context;
        updateMesh(vertices, indices); // Nutzt direkt die neue, smarte Logik
    }

    public VulkanMesh(VulkanContext context, MeshData data) {
        this.context = context;
        updateMesh(data.vertices, data.indices);
    }

    public void updateMesh(MeshData data) {
        updateMesh(data.vertices, data.indices);
    }

    // Die neue, dynamische Update-Methode
    public void updateMesh(float[] vertices, int[] indices) {
        indexCount = indices.length;
        if (indexCount == 0) return;

        long requiredVertexSize = (long) vertices.length * Float.BYTES;
        long requiredIndexSize = (long) indices.length * Integer.BYTES;

        // 1. Vertex Buffer prüfen und updaten
        if (requiredVertexSize > maxVertexBufferSize) {
            // Buffer zu klein: Zerstören und neu (größer) bauen
            if (vertexBuffer != null) vertexBuffer.cleanup();
            // Wir reservieren direkt ein bisschen mehr Puffer, um nicht sofort wieder neu zu bauen (z.B. 1.5x)
            maxVertexBufferSize = (long) (requiredVertexSize * 1.5);
            vertexBuffer = new VulkanBuffer(context, maxVertexBufferSize,
                    VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        // Daten flink reinschreiben (ohne den Puffer zu zerstören!)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), vertexBuffer.getBufferMemory(), 0, requiredVertexSize, 0, data);
            java.nio.FloatBuffer floatBuffer = MemoryUtil.memFloatBuffer(data.get(0), vertices.length);
            floatBuffer.put(vertices);
            VK10.vkUnmapMemory(context.getDevice(), vertexBuffer.getBufferMemory());
        }

        // 2. Index Buffer prüfen und updaten
        if (requiredIndexSize > maxIndexBufferSize) {
            if (indexBuffer != null) indexBuffer.cleanup();
            maxIndexBufferSize = (long) (requiredIndexSize * 1.5);
            indexBuffer = new VulkanBuffer(context, maxIndexBufferSize,
                    VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        // Daten flink reinschreiben
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), indexBuffer.getBufferMemory(), 0, requiredIndexSize, 0, data);
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
        indexCount = 0;
        maxVertexBufferSize = 0;
        maxIndexBufferSize = 0;
    }
}