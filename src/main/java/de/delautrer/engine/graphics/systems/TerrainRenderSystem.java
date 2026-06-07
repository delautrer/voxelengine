package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

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
        renderOpaque(cmd, packet);
        renderWater(cmd, packet);
    }

    public void renderOpaque(VkCommandBuffer cmd, RenderPacket packet) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Push Constants für den Shader vorbereiten (28 Floats / 112 Bytes)
            FloatBuffer pcBuffer = stack.callocFloat(32);
            packet.mvp.get(pcBuffer);
            pcBuffer.put(16, packet.globalLight);
            pcBuffer.put(17, packet.renderDistance);
            pcBuffer.put(18, 1.0f); // fogMultiplier

            pcBuffer.put(19, (float) packet.cameraPos.x);
            pcBuffer.put(20, (float) packet.cameraPos.y);
            pcBuffer.put(21, (float) packet.cameraPos.z);

            pcBuffer.put(22, 0.0f); // offsetX
            pcBuffer.put(23, 0.0f); // offsetY
            pcBuffer.put(24, 0.0f); // offsetZ
            pcBuffer.put(25, 0.0f); // isCloud
            pcBuffer.put(26, packet.isUnderwater ? 1.0f : 0.0f);
            pcBuffer.put(27, packet.clipY);

            // 1. SOLIDE BLÖCKE ZEICHNEN
            if (packet.opaqueMeshes != null && !packet.opaqueMeshes.isEmpty()) {
                VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0,
                        stack.longs(((VulkanTextureArray) packet.worldTexture).getDescriptorSet()), null);

                for (IMesh imesh : packet.opaqueMeshes) {
                    VulkanMesh mesh = (VulkanMesh) imesh;
                    // NEU: Offset pro Chunk in den Buffer schreiben
                    float relX = (float) ((double) mesh.chunkOffsetX - packet.cameraPos.x);
                    float relY = (float) (0.0 - packet.cameraPos.y); // Chunks starten absolut bei Y=0
                    float relZ = (float) ((double) mesh.chunkOffsetZ - packet.cameraPos.z);
                    pcBuffer.put(22, relX);
                    pcBuffer.put(23, relY);
                    pcBuffer.put(24, relZ);
                    // NEU: Push Constants FÜR DIESES MESH senden
                    VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(),
                            VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pcBuffer);

                    drawMesh(cmd, stack, mesh);
                }
            }

            // 2. OVERLAY ZEICHNEN (Block-Risse) VOR DEM WASSER!
            if (packet.overlayMesh != null && ((VulkanMesh) packet.overlayMesh).getIndexCount() > 0) {
                VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getTransparentHandle());
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0,
                        stack.longs(((VulkanTextureArray) packet.worldTexture).getDescriptorSet()), null);

                // Das Overlay wird in MasterRenderer bereits in absoluten Weltkoordinaten
                // gebaut,
                // daher muss der Offset hier wieder auf 0.0f stehen (plus Kamera offset)
                pcBuffer.put(22, (float) -packet.cameraPos.x);
                pcBuffer.put(23, (float) -packet.cameraPos.y);
                pcBuffer.put(24, (float) -packet.cameraPos.z);
                VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(),
                        VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pcBuffer);

                drawMesh(cmd, stack, (VulkanMesh) packet.overlayMesh);
            }

            // 3. WASSER ZEICHNEN (TRANSPARENT)
        }
    }

    public void renderWater(VkCommandBuffer cmd, RenderPacket packet) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer pcBuffer = stack.callocFloat(32);
            packet.mvp.get(pcBuffer);
            pcBuffer.put(16, packet.globalLight);
            pcBuffer.put(17, packet.renderDistance);
            pcBuffer.put(18, 1.0f); // fogMultiplier
            pcBuffer.put(19, (float) packet.cameraPos.x);
            pcBuffer.put(20, (float) packet.cameraPos.y);
            pcBuffer.put(21, (float) packet.cameraPos.z);
            pcBuffer.put(22, 0.0f); // offsetX
            pcBuffer.put(23, 0.0f); // offsetY
            pcBuffer.put(24, 0.0f); // offsetZ
            pcBuffer.put(25, 0.0f); // isCloud
            pcBuffer.put(26, packet.isUnderwater ? 1.0f : 0.0f);
            pcBuffer.put(27, packet.clipY);

            // 3. WASSER ZEICHNEN (TRANSPARENT)
            if (packet.waterMeshes != null && !packet.waterMeshes.isEmpty()) {
                VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getTransparentHandle());
                VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getPipelineLayout(), 0,
                        stack.longs(((VulkanTextureArray) packet.worldTexture).getDescriptorSet()), null);

                for (IMesh imesh : packet.waterMeshes) {
                    VulkanMesh mesh = (VulkanMesh) imesh;
                    // NEU: Offset pro Chunk in den Buffer schreiben
                    float relX = (float) ((double) mesh.chunkOffsetX - packet.cameraPos.x);
                    float relY = (float) (0.0 - packet.cameraPos.y); // Chunks starten absolut bei Y=0
                    float relZ = (float) ((double) mesh.chunkOffsetZ - packet.cameraPos.z);
                    pcBuffer.put(22, relX);
                    pcBuffer.put(23, relY);
                    pcBuffer.put(24, relZ);
                    // NEU: Push Constants FÜR DIESES MESH senden
                    VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(),
                            VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, pcBuffer);

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

    public long getDescriptorSetLayout() {
        return pipeline.getDescriptorSetLayout();
    }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}
