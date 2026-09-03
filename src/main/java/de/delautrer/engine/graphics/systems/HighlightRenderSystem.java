package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.*;

import org.joml.Matrix4f;
import org.joml.Vector4f;
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
        boolean hasSelectedBlock = (packet.selectedBlockPos != null && packet.highlightMesh != null && ((VulkanMesh) packet.highlightMesh).getIndexCount() > 0);
        IMesh cubeMesh = packet.unitCubeMesh != null ? packet.unitCubeMesh : packet.highlightMesh;
        boolean hasStructureBoxes = (packet.structureBoxes != null && !packet.structureBoxes.isEmpty() && cubeMesh != null && ((VulkanMesh) cubeMesh).getIndexCount() > 0);

        if (!hasSelectedBlock && !hasStructureBoxes) return;

        VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline.getHandle());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Draw block selection outline
            if (hasSelectedBlock) {
                VulkanMesh blockMesh = (VulkanMesh) packet.highlightMesh;
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(blockMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, blockMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);

                Matrix4f model = new Matrix4f().identity()
                        .translate((float) (packet.selectedBlockPos.x - packet.cameraPos.x) - 0.001f,
                                   (float) (packet.selectedBlockPos.y - packet.cameraPos.y) - 0.001f,
                                   (float) (packet.selectedBlockPos.z - packet.cameraPos.z) - 0.001f)
                        .scale(1.002f);

                Matrix4f mvp = new Matrix4f(packet.mvp).mul(model);

                FloatBuffer pushBuffer = stack.mallocFloat(20);
                mvp.get(pushBuffer);
                pushBuffer.position(16);
                pushBuffer.put(0.0f).put(0.0f).put(0.0f).put(1.0f);
                pushBuffer.position(0);

                VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, pushBuffer);
                VK10.vkCmdDrawIndexed(cmd, blockMesh.getIndexCount(), 1, 0, 0, 0);
            }

            // 2. Draw structure boxes (always using 1x1x1 unit cube mesh, completely decoupled from selected block geometry)
            if (hasStructureBoxes) {
                VulkanMesh boxMesh = (VulkanMesh) cubeMesh;
                VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(boxMesh.getVertexBuffer()), stack.longs(0));
                VK10.vkCmdBindIndexBuffer(cmd, boxMesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);

                for (StructureBox box : packet.structureBoxes) {
                    float sizeX = (float) (box.maxX - box.minX);
                    float sizeY = (float) (box.maxY - box.minY);
                    float sizeZ = (float) (box.maxZ - box.minZ);

                    Matrix4f model = new Matrix4f().identity()
                            .translate((float) (box.minX - packet.cameraPos.x) - 0.001f,
                                       (float) (box.minY - packet.cameraPos.y) - 0.001f,
                                       (float) (box.minZ - packet.cameraPos.z) - 0.001f)
                            .scale(sizeX + 0.002f, sizeY + 0.002f, sizeZ + 0.002f);

                    Matrix4f mvp = new Matrix4f(packet.mvp).mul(model);

                    FloatBuffer pushBuffer = stack.mallocFloat(20);
                    mvp.get(pushBuffer);
                    pushBuffer.position(16);
                    Vector4f c = box.color != null ? box.color : new Vector4f(1.0f, 0.33f, 1.0f, 1.0f);
                    pushBuffer.put(c.x).put(c.y).put(c.z).put(c.w);
                    pushBuffer.position(0);

                    VK10.vkCmdPushConstants(cmd, pipeline.getPipelineLayout(), VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, pushBuffer);
                    VK10.vkCmdDrawIndexed(cmd, boxMesh.getIndexCount(), 1, 0, 0, 0);
                }
            }
        }
    }

    @Override
    public void cleanup() {
        pipeline.cleanup();
    }
}
