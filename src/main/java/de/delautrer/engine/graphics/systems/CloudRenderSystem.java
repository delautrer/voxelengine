package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.*;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import java.nio.FloatBuffer;

public class CloudRenderSystem implements IRenderSystem {
    private final VulkanGraphicsPipeline pipeline;

    public CloudRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.pipeline = new VulkanGraphicsPipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.cloudMesh == null || ((VulkanMesh) packet.cloudMesh).getIndexCount() == 0)
            return;

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getTransparentHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0,
                    stack.longs(((VulkanTextureArray) packet.worldTexture).getDescriptorSet()), null);

            Matrix4f modelMatrix = new Matrix4f().translate(packet.cloudOffset);
            Matrix4f finalMvp = new Matrix4f(packet.mvp).mul(modelMatrix);

            // NEU: 26 Floats
            FloatBuffer mvpBuffer = stack.mallocFloat(28);
            finalMvp.get(mvpBuffer);
            mvpBuffer.put(16, packet.globalLight);
            mvpBuffer.put(17, packet.renderDistance);
            mvpBuffer.put(18, 0.0f);
            mvpBuffer.put(19, (float) packet.cameraPos.x);
            mvpBuffer.put(20, (float) packet.cameraPos.y);
            mvpBuffer.put(21, (float) packet.cameraPos.z);
            mvpBuffer.put(22, packet.cloudOffset.x);
            mvpBuffer.put(23, packet.cloudOffset.y);
            mvpBuffer.put(24, packet.cloudOffset.z);
            mvpBuffer.put(25, 1.0f);
            mvpBuffer.put(26, 0.0f);
            mvpBuffer.put(27, -999.0f);

            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(),
                    VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, mvpBuffer);

            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(((VulkanMesh) packet.cloudMesh).getVertexBuffer()),
                    stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, ((VulkanMesh) packet.cloudMesh).getIndexBuffer(), 0,
                    VK10.VK_INDEX_TYPE_UINT32);
            VK10.vkCmdDrawIndexed(cmd, ((VulkanMesh) packet.cloudMesh).getIndexCount(), 1, 0, 0, 0);
        }
    }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}
