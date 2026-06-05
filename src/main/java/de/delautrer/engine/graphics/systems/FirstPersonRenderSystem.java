package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.RenderPacket;
import de.delautrer.engine.graphics.vulkan.buffer.VulkanMesh;
import de.delautrer.engine.graphics.vulkan.core.VulkanContext;
import de.delautrer.engine.graphics.vulkan.core.VulkanRenderPass;
import de.delautrer.engine.graphics.vulkan.core.VulkanSwapchain;
import de.delautrer.engine.graphics.vulkan.pipeline.VulkanGraphicsPipeline;
import de.delautrer.engine.graphics.vulkan.texture.VulkanTexture;
import de.delautrer.engine.graphics.vulkan.texture.VulkanTextureArray;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import java.nio.FloatBuffer;

public class FirstPersonRenderSystem implements IRenderSystem {
    private final VulkanGraphicsPipeline pipeline;

    public FirstPersonRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.pipeline = new VulkanGraphicsPipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.hideUI || packet.firstPersonMesh == null) return;
        VulkanMesh mesh = (VulkanMesh) packet.firstPersonMesh;

        // Wir manipulieren die View-Matrix, um das Objekt immer fest vor der Kamera zu haben
        Matrix4f proj = new Matrix4f(packet.proj);
        Matrix4f view = new Matrix4f().identity(); // Kamera fix am Ursprung

        // Animation aus dem RenderPacket holen (Zeit-basiert oder Interaction-basiert)
        float t = (System.currentTimeMillis() % 2000) / 2000.0f;
        float breatheY = (float) Math.sin(t * Math.PI * 2) * 0.02f;
        
        float swing = packet.swingProgress;
        float swingAnim = (float) Math.sin(swing * Math.PI); // 0 -> 1 -> 0

        Matrix4f model = new Matrix4f();
        
        if (packet.firstPersonIsItem) {
            // Item rechts unten in der Hand
            model.translate(0.55f, -0.5f + breatheY - swingAnim * 0.1f, -0.8f);
            
            // Rotationen für Minecraft-ähnliche Haltung (obere rechte Ecke nach vorne)
            model.rotateY((float) Math.toRadians(-100f)); // Zeigt nach vorne/links <--
            model.rotateZ((float) Math.toRadians(swingAnim * 80.0f)); // Schlag-Animation nach vorne
            model.rotateZ((float) Math.toRadians(30f)); // Schräg halten
            model.rotateX((float) Math.toRadians(0f)); // Grundneigung
            
            model.scale(0.4f);
        } else if (packet.isEmptyHand) {
            // Leere Hand: Arm von rechts unten (0.25 x 0.75 x 0.25 Mesh aus MasterRenderer)
            model.translate(0.55f, -0.6f + breatheY - swingAnim * 0.1f, -0.4f);
            
            // Um den Ankerpunkt (0,0,0) des Arms für die Drehung zu nutzen, 
            // drehen wir ihn so, dass er nach vorne-links zeigt.
            // Der Arm in MasterRenderer wächst in +Y Richtung (0.75 hoch). 
            // Wir kippen ihn nach vorne (-Z), indem wir um X rotieren.
            model.rotateX((float) Math.toRadians(-70.0f - swingAnim * 60.0f)); 
            model.rotateY((float) Math.toRadians(0.0f));
            model.rotateZ((float) Math.toRadians(0.0f));

            // Scale ist 1, da die Größe schon im MasterRenderer (0.25x0.75x0.25) definiert ist.
            model.scale(0.75f);
        } else {
            // Block rechts unten
            model.translate(0.55f, -0.5f + breatheY - swingAnim * 0.1f, -0.8f);
            model.rotateY((float) Math.toRadians(-76f));
            model.rotateZ(0.2f + swingAnim * 1.5f);
            model.scale(0.25f);
        }

        Matrix4f mvp = proj.mul(view).mul(model);

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.vulkan.VkClearAttachment.Buffer clearAttachments = org.lwjgl.vulkan.VkClearAttachment.calloc(1, stack);
            clearAttachments.get(0).aspectMask(VK10.VK_IMAGE_ASPECT_DEPTH_BIT);
            clearAttachments.get(0).clearValue().depthStencil().depth(1.0f);

            org.lwjgl.vulkan.VkClearRect.Buffer clearRects = org.lwjgl.vulkan.VkClearRect.calloc(1, stack);
            clearRects.get(0).rect().offset().set(0, 0);
            clearRects.get(0).rect().extent().set(8192, 8192);
            clearRects.get(0).baseArrayLayer(0);
            clearRects.get(0).layerCount(1);

            VK10.vkCmdClearAttachments(cmd, clearAttachments, clearRects);

            FloatBuffer buf = stack.mallocFloat(32);
            mvp.get(buf);
            buf.put(16, packet.globalLight); // The normal globalLight goes here
            buf.put(17, packet.renderDistance);
            buf.put(18, 1.0f);
            buf.put(19, 0.0f); // camX
            buf.put(20, 0.0f); // camY
            buf.put(21, 0.0f); // camZ
            buf.put(22, 0.0f);
            buf.put(23, 0.0f);
            buf.put(24, 0.0f);
            buf.put(25, 0.0f);
            buf.put(26, 0.0f);
            buf.put(27, packet.clipY);
            buf.put(28, packet.isEmptyHand ? 1.0f : 0.0f); // useVertexColorOnly
            buf.put(29, 1.0f); // isFirstPerson
            buf.put(30, packet.playerSkyLight);
            buf.put(31, packet.playerBlockLight);

            VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(),
                    VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf);

            long descriptorSet = packet.firstPersonIsItem ? 
                    ((VulkanTextureArray) packet.itemTextureArray).getDescriptorSet() : 
                    ((VulkanTextureArray) packet.worldTexture).getDescriptorSet();

            VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0,
                    stack.longs(descriptorSet), null);
            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(mesh.getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, mesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
            
            // Um Overlap-Probleme mit der Welt zu umgehen, könnten wir Tiefe deaktivieren.
            // VulkanGraphicsPipeline hat Depth-Test an, aber da das Modell so nah ist (-0.8 Z), wird es meist nicht clippen.
            VK10.vkCmdDrawIndexed(cmd, mesh.getIndexCount(), 1, 0, 0, 0);
        }
    }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}
