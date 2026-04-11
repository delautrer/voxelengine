package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.FloatBuffer;

public class HighlightRenderSystem implements IRenderSystem {
    private final VulkanLinePipeline pipeline;

    public HighlightRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.pipeline = new VulkanLinePipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.selectedBlockPos == null || packet.highlightMesh == null || packet.highlightMesh.getIndexCount() == 0) return;

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f highlightModel = new Matrix4f().identity()
                    .translate(packet.selectedBlockPos.x - 0.001f, packet.selectedBlockPos.y - 0.001f, packet.selectedBlockPos.z - 0.001f)
                    .scale(1.002f);
            Matrix4f highlightMvp = new Matrix4f(packet.proj).mul(packet.view).mul(highlightModel);

            FloatBuffer highlightMvpBuffer = stack.mallocFloat(16);
            highlightMvp.get(highlightMvpBuffer);
            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, highlightMvpBuffer);

            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.highlightMesh.getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, packet.highlightMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);

            VK10.vkCmdDrawIndexed(cmd, packet.highlightMesh.getIndexCount(), 1, 0, 0, 0);
        }
    }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}