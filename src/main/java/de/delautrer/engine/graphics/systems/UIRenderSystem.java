package de.delautrer.engine.graphics.systems;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.game.ui.elements.UIDrawCall;
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
        if (packet.hideUI) return;

        if (packet.uiCombinedMesh == null || ((VulkanMesh)packet.uiCombinedMesh).getIndexCount() == 0 || packet.uiDrawCalls == null || packet.uiDrawCalls.isEmpty()) {
            return;
        }

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer orthoBuffer = stack.mallocFloat(16);
            packet.ortho.get(orthoBuffer);
            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, orthoBuffer);

            // Binde das EINE große Vertex/Index-Buffer-Paar
            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(((VulkanMesh)packet.uiCombinedMesh).getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, ((VulkanMesh)packet.uiCombinedMesh).getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);

            // Dynamisch die Draw-Calls abarbeiten (perfekt Z-Sortiert)
            for (UIDrawCall dc : packet.uiDrawCalls) {
                VulkanTexture tex = switch (dc.texture) {
                    case UI    -> (VulkanTexture) packet.uiTexture;
                    case ITEM  -> (VulkanTexture) packet.itemTexture;
                    case FONT  -> (VulkanTexture) packet.fontTexture;
                    case BLOCK -> (VulkanTexture) packet.blockUITexture;
                };

                if (tex != null) {
                    VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(tex.getDescriptorSet()), null);
                    // Parameter: commandBuffer, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance
                    VK10.vkCmdDrawIndexed(cmd, dc.indexCount, 1, dc.indexOffset, 0, 0);
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
