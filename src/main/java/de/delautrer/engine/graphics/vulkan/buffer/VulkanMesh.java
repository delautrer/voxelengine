package de.delautrer.engine.graphics.vulkan.buffer;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

public class VulkanMesh implements de.delautrer.engine.graphics.IMesh {
    private final VulkanContext context;
    private VulkanBuffer vertexBuffer;
    private VulkanBuffer indexBuffer;
    private int indexCount;
    private long maxVertexBufferSize = 0;
    private long maxIndexBufferSize = 0;

    public float chunkOffsetX = 0.0f;
    public float chunkOffsetZ = 0.0f;

    public VulkanMesh(VulkanContext context, float[] vertices, int[] indices) {
        this.context = context;
        updateMesh(vertices, indices);
    }

    public VulkanMesh(VulkanContext context, MeshData data) {
        this.context = context;
        updateMesh(data.vertices(), data.indices());
    }

    public void updateMesh(MeshData data) {
        updateMesh(data.vertices(), data.indices());
    }

    public void updateMesh(float[] vertices, int[] indices) {
        indexCount = indices.length;
        if (indexCount == 0)
            return;

        long requiredVertexSize = (long) vertices.length * Float.BYTES;
        long requiredIndexSize = (long) indices.length * Integer.BYTES;

        // --- 1. VERTEX BUFFER UPDATE ---
        if (requiredVertexSize > maxVertexBufferSize) {
            // FIX: Wir warten auf die GPU, BEVOR wir ihr den Speicher unter den Füßen
            // wegziehen!
            if (vertexBuffer != null) {
                VK10.vkDeviceWaitIdle(context.getDevice());
                vertexBuffer.cleanup();
            }

            // ANTI-STUTTER FIX: Wenn wir vergrößern, dann direkt massiv!
            // Min. 128 KB vorab-allozieren. Dadurch muss die Engine beim Droppen von Items
            // nie wieder warten!
            maxVertexBufferSize = Math.max((long) (requiredVertexSize * 2.0), 128 * 1024L);

            vertexBuffer = new VulkanBuffer(context, maxVertexBufferSize,
                    VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        // Daten flink in den reservierten (und garantiert ausreichend großen) Speicher
        // schreiben
        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), vertexBuffer.getBufferMemory(), 0, requiredVertexSize, 0, data);
            java.nio.FloatBuffer floatBuffer = MemoryUtil.memFloatBuffer(data.get(0), vertices.length);
            floatBuffer.put(vertices);
            VK10.vkUnmapMemory(context.getDevice(), vertexBuffer.getBufferMemory());
        }

        // --- 2. INDEX BUFFER UPDATE ---
        if (requiredIndexSize > maxIndexBufferSize) {
            // FIX: Auch hier auf die GPU warten
            if (indexBuffer != null) {
                VK10.vkDeviceWaitIdle(context.getDevice());
                indexBuffer.cleanup();
            }

            // ANTI-STUTTER FIX: Min. 64 KB für Indices reservieren
            maxIndexBufferSize = Math.max((long) (requiredIndexSize * 2.0), 64 * 1024L);

            indexBuffer = new VulkanBuffer(context, maxIndexBufferSize,
                    VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), indexBuffer.getBufferMemory(), 0, requiredIndexSize, 0, data);
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

    @Override
    public int getIndexCount() {
        return indexCount;
    }

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
