package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.FloatBuffer;

public class TerrainRenderSystem implements IRenderSystem {
    private final VulkanGraphicsPipeline pipeline;

    public TerrainRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.pipeline = new VulkanGraphicsPipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.visibleMeshes == null || packet.visibleMeshes.isEmpty()) return;

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.worldTexture.getDescriptorSet()), null);

            FloatBuffer mvpBuffer = stack.mallocFloat(17);
            packet.mvp.get(mvpBuffer);
            mvpBuffer.put(16, packet.globalLight);
            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, mvpBuffer);

            for (VulkanMesh chunkMesh : packet.visibleMeshes) {
                if (chunkMesh.getIndexCount() > 0) {
                    VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(chunkMesh.getVertexBuffer()), stack.longs(0));
                    VK10.vkCmdBindIndexBuffer(cmd, chunkMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                    VK10.vkCmdDrawIndexed(cmd, chunkMesh.getIndexCount(), 1, 0, 0, 0);
                }
            }
        }
    }

    public long getDescriptorSetLayout() { return pipeline.getDescriptorSetLayout(); }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}