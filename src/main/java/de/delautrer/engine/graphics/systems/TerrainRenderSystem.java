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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Push Constants für den Shader vorbereiten (26 Floats / 104 Bytes)
            FloatBuffer pcBuffer = stack.mallocFloat(27);
            packet.mvp.get(pcBuffer);
            pcBuffer.put(16, packet.globalLight);
            pcBuffer.put(17, packet.renderDistance);
            pcBuffer.put(18, 1.0f); // fogMultiplier

            pcBuffer.put(19, packet.cameraPos.x);
            pcBuffer.put(20, packet.cameraPos.y);
            pcBuffer.put(21, packet.cameraPos.z);

            pcBuffer.put(22, 0.0f); // offsetX
            pcBuffer.put(23, 0.0f); // offsetY
            pcBuffer.put(24, 0.0f); // offsetZ
            pcBuffer.put(25, 0.0f); // isCloud
            pcBuffer.put(26, packet.isUnderwater ? 1.0f : 0.0f);

            // 1. SOLIDE BLÖCKE ZEICHNEN
            if (packet.opaqueMeshes != null && !packet.opaqueMeshes.isEmpty()) {
                VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.worldTexture.getDescriptorSet()), null);
                VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pcBuffer);

                for (VulkanMesh mesh : packet.opaqueMeshes) {
                    drawMesh(cmd, stack, mesh);
                }
            }

            // 2. OVERLAY ZEICHNEN (Block-Risse) VOR DEM WASSER!
            if (packet.overlayMesh != null && packet.overlayMesh.getIndexCount() > 0) {
                VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getTransparentHandle());
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.worldTexture.getDescriptorSet()), null);
                VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pcBuffer);

                drawMesh(cmd, stack, packet.overlayMesh);
            }

            // 3. WASSER ZEICHNEN (TRANSPARENT)
            if (packet.waterMeshes != null && !packet.waterMeshes.isEmpty()) {
                VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getTransparentHandle());
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.worldTexture.getDescriptorSet()), null);
                VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pcBuffer);

                for (VulkanMesh mesh : packet.waterMeshes) {
                    drawMesh(cmd, stack, mesh);
                }
            }
        }
    }

    private void drawMesh(VkCommandBuffer cmd, MemoryStack stack, VulkanMesh mesh) {
        if (mesh != null && mesh.getIndexCount() > 0) {
            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(mesh.getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, mesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
            VK10.vkCmdDrawIndexed(cmd, mesh.getIndexCount(), 1, 0, 0, 0);
        }
    }

    public long getDescriptorSetLayout() { return pipeline.getDescriptorSetLayout(); }

    @Override
    public void cleanup() { pipeline.cleanup(); }
}