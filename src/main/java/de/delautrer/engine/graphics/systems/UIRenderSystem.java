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
        // Wenn alle drei Meshes leer sind, brechen wir direkt ab
        if ((packet.uiMesh == null || packet.uiMesh.getIndexCount() == 0) &&
                (packet.itemMesh == null || packet.itemMesh.getIndexCount() == 0) &&
                (packet.textMesh == null || packet.textMesh.getIndexCount() == 0)) {
            return;
        }

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Push Constants (Ortho-Matrix) gelten für die gesamte Pipeline,
            // müssen also nur einmal pro Frame gesetzt werden.
            FloatBuffer orthoBuffer = stack.mallocFloat(16);
            packet.ortho.get(orthoBuffer);
            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, orthoBuffer);

            // --- 0. OVERLAYS (Ersticken, Schaden, Portale -> blockUITexture) ---
            if (packet.overlayMesh != null && packet.overlayMesh.getIndexCount() > 0 && packet.blockUITexture != null) {
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.blockUITexture.getDescriptorSet()), null);
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.overlayMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, packet.overlayMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                VK10.vkCmdDrawIndexed(cmd, packet.overlayMesh.getIndexCount(), 1, 0, 0, 0);
            }

            // --- 1. UI RENDERN (Hotbar, Fenster, Fadenkreuz -> menu_gui.png) ---
            if (packet.uiMesh != null && packet.uiMesh.getIndexCount() > 0 && packet.uiTexture != null) {
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.uiTexture.getDescriptorSet()), null);
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.uiMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, packet.uiMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                VK10.vkCmdDrawIndexed(cmd, packet.uiMesh.getIndexCount(), 1, 0, 0, 0);
            }

            // --- 2. ITEMS RENDERN (Blöcke, Werkzeuge -> gui.png / items.png) ---
            if (packet.itemMesh != null && packet.itemMesh.getIndexCount() > 0 && packet.itemTexture != null) {
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.itemTexture.getDescriptorSet()), null);
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.itemMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, packet.itemMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                VK10.vkCmdDrawIndexed(cmd, packet.itemMesh.getIndexCount(), 1, 0, 0, 0);
            }

            // --- 3. TEXT RENDERN (Font-Textur) ---
            if (packet.textMesh != null && packet.textMesh.getIndexCount() > 0 && packet.fontTexture != null) {
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.fontTexture.getDescriptorSet()), null);
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.textMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, packet.textMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                VK10.vkCmdDrawIndexed(cmd, packet.textMesh.getIndexCount(), 1, 0, 0, 0);
            }
        }
    }

    public long getDescriptorSetLayout() { return pipeline.getDescriptorSetLayout(); }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}