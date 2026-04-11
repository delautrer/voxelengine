package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.FloatBuffer;

public class UIRenderSystem implements IRenderSystem {
    private final VulkanUIPipeline pipeline;

    public UIRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.pipeline = new VulkanUIPipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.uiMesh == null || packet.uiMesh.getIndexCount() == 0) return;

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.guiTexture.getDescriptorSet()), null);

            FloatBuffer orthoBuffer = stack.mallocFloat(16);
            packet.ortho.get(orthoBuffer);
            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, orthoBuffer);

            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.uiMesh.getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, packet.uiMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);

            VK10.vkCmdDrawIndexed(cmd, packet.uiMesh.getIndexCount(), 1, 0, 0, 0);
        }
    }

    public long getDescriptorSetLayout() { return pipeline.getDescriptorSetLayout(); }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}