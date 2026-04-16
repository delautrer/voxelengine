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
                (packet.menuMesh == null || packet.menuMesh.getIndexCount() == 0) &&
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

            // --- 1. GUI RENDERN (Hotbar, Inventar, Fadenkreuz -> gui.png) ---
            if (packet.uiMesh != null && packet.uiMesh.getIndexCount() > 0 && packet.guiTexture != null) {
                // Gui-Textur binden
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.guiTexture.getDescriptorSet()), null);

                // Gui-Mesh binden und zeichnen
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.uiMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, packet.uiMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                VK10.vkCmdDrawIndexed(cmd, packet.uiMesh.getIndexCount(), 1, 0, 0, 0);
            }

            // --- 2. PAUSE / MENÜ RENDERN (Buttons, Menü-Hintergrund -> menu_gui.png) ---
            if (packet.menuMesh != null && packet.menuMesh.getIndexCount() > 0 && packet.menuTexture != null) {
                // Menü-Textur binden
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.menuTexture.getDescriptorSet()), null);

                // Menü-Mesh binden und zeichnen
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(packet.menuMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, packet.menuMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
                VK10.vkCmdDrawIndexed(cmd, packet.menuMesh.getIndexCount(), 1, 0, 0, 0);
            }

            // --- 3. TEXT RENDERN (F3 Menü, Button-Texte -> Font-Textur) ---
            if (packet.textMesh != null && packet.textMesh.getIndexCount() > 0 && packet.fontTexture != null) {
                // Font-Textur binden
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0, stack.longs(packet.fontTexture.getDescriptorSet()), null);

                // Text-Mesh binden und zeichnen
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