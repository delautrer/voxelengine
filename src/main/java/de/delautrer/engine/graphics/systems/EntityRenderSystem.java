package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import de.delautrer.game.blocks.Block;
import de.delautrer.game.blocks.CubeBlock;
import de.delautrer.game.blocks.TorchBlock;
import de.delautrer.game.blocks.state.BlockProperties.BlockFace;
import de.delautrer.game.entity.Entity;
import de.delautrer.game.entity.ItemEntity;
import de.delautrer.game.items.BlockItem;
import de.delautrer.game.items.Item;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class EntityRenderSystem implements IRenderSystem {
    private final VulkanContext context;
    private final VulkanGraphicsPipeline blockPipeline;

    private VulkanMesh blockMesh;
    private VulkanMesh itemMesh;

    public EntityRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.context = context;
        this.blockPipeline = new VulkanGraphicsPipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.entities == null || packet.entities.isEmpty()) {
            cleanupMeshes();
            return;
        }

        List<Float> blockVerts = new ArrayList<>();
        List<Integer> blockInds = new ArrayList<>();
        int blockOffset = 0;

        List<Float> itemVerts = new ArrayList<>();
        List<Integer> itemInds = new ArrayList<>();
        int itemOffset = 0;

        double t = System.currentTimeMillis() / 1000.0;

        for (Entity e : packet.entities) {
            if (!(e instanceof ItemEntity itemEntity) || itemEntity.isDead()) continue;

            Item itemType = itemEntity.stack.type;

            // NEU: Wir lesen den Count aus dem Stack aus (falls deine Variable anders heißt, hier anpassen)
            int count = itemEntity.stack.amount;

            // Berechnen, wie viele Meshes wir für den "Haufen" zeichnen
            int visualCount = 1;
            if (count > 1)  visualCount = 2; // Der 2er Haufen
            if (count > 15) visualCount = 3; // Der 3er Haufen
            if (count > 31) visualCount = 4; // Der 4er Haufen

            float hoverY = (float) Math.sin(t * 3.0) * 0.1f + 0.15f;

            // NEU: Wir iterieren über den visualCount und zeichnen das Item mehrfach versetzt
            for (int v = 0; v < visualCount; v++) {

                // Leichter 3D-Versatz für jedes weitere Item im Haufen (Minecraft-Style)
                float pileOffsetX = v * 0.04f;
                float pileOffsetY = v * 0.04f;
                float pileOffsetZ = v * -0.04f;

                Matrix4f modelMat = new Matrix4f()
                        .translate(e.position.x + pileOffsetX, e.position.y + hoverY + pileOffsetY, e.position.z + pileOffsetZ)
                        .rotateY((float)(t * 1.5));

                if (itemType instanceof BlockItem blockItem && blockItem.getBlock() instanceof CubeBlock cubeBlock && !(cubeBlock instanceof TorchBlock)) {
                    modelMat.scale(0.25f);
                    build3DBlock(blockVerts, blockInds, blockOffset, modelMat, cubeBlock);
                    blockOffset = blockVerts.size() / 12;
                } else {
                    AtlasRegion reg = itemType.getIconRegion();
                    if (reg != null) {
                        modelMat.scale(0.5f);
                        buildThickItem(itemVerts, itemInds, itemOffset, modelMat, reg);
                        itemOffset += 24;
                    }
                }
            }
        }

        if (blockVerts.isEmpty() && itemVerts.isEmpty()) return;

        if (!blockVerts.isEmpty()) {
            if (blockMesh == null) {
                blockMesh = new VulkanMesh(context, toFloatArray(blockVerts), toIntArray(blockInds));
            } else {
                blockMesh.updateMesh(toFloatArray(blockVerts), toIntArray(blockInds));
            }
            VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, blockPipeline.getHandle());
            bindAndDraw(cmd, packet, blockPipeline.getPipelineLayout(), blockMesh, packet.worldTexture.getDescriptorSet());
        } else if (blockMesh != null) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            blockMesh.cleanup();
            blockMesh = null;
        }

        if (!itemVerts.isEmpty()) {
            if (itemMesh == null) {
                itemMesh = new VulkanMesh(context, toFloatArray(itemVerts), toIntArray(itemInds));
            } else {
                itemMesh.updateMesh(toFloatArray(itemVerts), toIntArray(itemInds));
            }
            VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, blockPipeline.getHandle());
            bindAndDraw(cmd, packet, blockPipeline.getPipelineLayout(), itemMesh, packet.itemTexture.getDescriptorSet());
        } else if (itemMesh != null) {
            VK10.vkDeviceWaitIdle(context.getDevice());
            itemMesh.cleanup();
            itemMesh = null;
        }
    }

    private void bindAndDraw(VkCommandBuffer cmd, RenderPacket packet, long pipelineLayout, VulkanMesh mesh, long descriptorSet) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f mvp = new Matrix4f(packet.proj).mul(packet.view);
            FloatBuffer buf = stack.mallocFloat(16);
            mvp.get(buf);
            VK10.vkCmdPushConstants(cmd, pipelineLayout, VK10.VK_SHADER_STAGE_VERTEX_BIT, 0, buf);

            VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, stack.longs(descriptorSet), null);
            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(mesh.getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, mesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
            VK10.vkCmdDrawIndexed(cmd, mesh.getIndexCount(), 1, 0, 0, 0);
        }
    }

    private void buildThickItem(List<Float> verts, List<Integer> inds, int offset, Matrix4f transform, AtlasRegion reg) {
        float thickness = 0.03f;

        Vector3f[] posUp = {
                new Vector3f(-0.5f, thickness, -0.5f), new Vector3f( 0.5f, thickness, -0.5f),
                new Vector3f( 0.5f, thickness,  0.5f), new Vector3f(-0.5f, thickness,  0.5f)
        };
        Vector3f[] posDown = {
                new Vector3f(-0.5f, 0.0f,  0.5f), new Vector3f( 0.5f, 0.0f,  0.5f),
                new Vector3f( 0.5f, 0.0f, -0.5f), new Vector3f(-0.5f, 0.0f, -0.5f)
        };

        float[] u = {reg.u0, reg.u1, reg.u1, reg.u0};
        float[] v = {reg.v1, reg.v1, reg.v0, reg.v0};

        for (int i = 0; i < 4; i++) {
            Vector3f p = new Vector3f(posUp[i]).mulPosition(transform);
            addVertex(verts, p.x, p.y, p.z, 1.0f, 1.0f, 1.0f, 1.0f, u[i], v[i], (float)reg.layer, 1.0f, 0.0f);
        }
        addIndices(inds, offset);
        offset += 4;

        for (int i = 0; i < 4; i++) {
            Vector3f p = new Vector3f(posDown[i]).mulPosition(transform);
            addVertex(verts, p.x, p.y, p.z, 0.6f, 0.6f, 0.6f, 1.0f, u[i], v[i], (float)reg.layer, 1.0f, 0.0f);
        }
        addIndices(inds, offset);
        offset += 4;

        Vector3f[][] edges = {
                {posUp[3], posUp[2], posDown[1], posDown[0]},
                {posUp[1], posUp[0], posDown[3], posDown[2]},
                {posUp[2], posUp[1], posDown[2], posDown[1]},
                {posUp[0], posUp[3], posDown[0], posDown[3]}
        };

        for (int e = 0; e < 4; e++) {
            for(int i = 0; i < 4; i++) {
                Vector3f p = new Vector3f(edges[e][i]).mulPosition(transform);
                addVertex(verts, p.x, p.y, p.z, 0.3f, 0.3f, 0.3f, 1.0f, reg.u0, reg.v0, (float)reg.layer, 1.0f, 0.0f);
            }
            addIndices(inds, offset);
            offset += 4;
        }
    }

    private void build3DBlock(List<Float> verts, List<Integer> inds, int offset, Matrix4f transform, CubeBlock block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        float[] shades = {0.8f, 0.8f, 0.65f, 0.65f, 1.0f, 0.4f};

        List<de.delautrer.engine.physics.AABB> boxes = block.getBoundingBoxes(block.getDefaultState());

        for (de.delautrer.engine.physics.AABB box : boxes) {
            float bx0 = box.min.x; float by0 = box.min.y; float bz0 = box.min.z;
            float bx1 = box.max.x; float by1 = box.max.y; float bz1 = box.max.z;

            float minX = bx0 - 0.5f; float minY = by0 - 0.5f; float minZ = bz0 - 0.5f;
            float maxX = bx1 - 0.5f; float maxY = by1 - 0.5f; float maxZ = bz1 - 0.5f;

            float sideV0, sideV1;
            if (block.getModel() != null && block.getModel().directional_textures) {
                sideV0 = 1.0f - by1;
                sideV1 = 1.0f - by0;
            } else {
                if (bx1 - bx0 > 0.99f && by1 - by0 > 0.99f && bz1 - bz0 > 0.99f) {
                    sideV0 = 0.0f; sideV1 = 1.0f;
                } else {
                    if (by1 <= 0.5f) { sideV0 = 0.5f; sideV1 = 1.0f; }
                    else { sideV0 = 0.0f; sideV1 = 0.5f; }
                }
            }

            Vector3f[][] coords = {
                    {new Vector3f(maxX, minY, minZ), new Vector3f(minX, minY, minZ), new Vector3f(minX, maxY, minZ), new Vector3f(maxX, maxY, minZ)}, // North
                    {new Vector3f(minX, minY, maxZ), new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(minX, maxY, maxZ)}, // South
                    {new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, minY, minZ), new Vector3f(maxX, maxY, minZ), new Vector3f(maxX, maxY, maxZ)}, // East
                    {new Vector3f(minX, minY, minZ), new Vector3f(minX, minY, maxZ), new Vector3f(minX, maxY, maxZ), new Vector3f(minX, maxY, minZ)}, // West
                    {new Vector3f(minX, maxY, minZ), new Vector3f(minX, maxY, maxZ), new Vector3f(maxX, maxY, maxZ), new Vector3f(maxX, maxY, minZ)}, // Up
                    {new Vector3f(minX, minY, maxZ), new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ), new Vector3f(maxX, minY, maxZ)}  // Down
            };

            float[][] u_faces = {
                    {1.0f-bx1, 1.0f-bx0, 1.0f-bx0, 1.0f-bx1}, // North
                    {bx0, bx1, bx1, bx0},                     // South
                    {1.0f-bz1, 1.0f-bz0, 1.0f-bz0, 1.0f-bz1}, // East
                    {bz0, bz1, bz1, bz0},                     // West
                    {bx0, bx1, bx1, bx0},                     // Up
                    {bx0, bx1, bx1, bx0}                      // Down
            };
            float[][] v_faces = {
                    {sideV1, sideV1, sideV0, sideV0}, // North
                    {sideV1, sideV1, sideV0, sideV0}, // South
                    {sideV1, sideV1, sideV0, sideV0}, // East
                    {sideV1, sideV1, sideV0, sideV0}, // West
                    {bz1, bz1, bz0, bz0},             // Up
                    {bz1, bz1, bz0, bz0}              // Down
            };

            for (int i = 0; i < 6; i++) {
                AtlasRegion reg = null;

                if (block instanceof CubeBlock cb) {
                    reg = cb.getTextureForFace(cb.getDefaultState(), faces[i]);
                } else {
                    try {
                        java.lang.reflect.Method m = block.getClass().getMethod("getTextureForFace", de.delautrer.game.blocks.state.BlockState.class, BlockFace.class);
                        reg = (AtlasRegion) m.invoke(block, block.getDefaultState(), faces[i]);
                    } catch (Exception e) {
                        if (block.getModel() != null) reg = block.getModel().top;
                    }
                }

                if (reg == null) continue;

                Vector3f[] p = coords[i];
                float[] u = u_faces[i];
                float[] v = v_faces[i];
                float s = shades[i];

                for (int j = 0; j < 4; j++) {
                    Vector3f tv = new Vector3f(p[j]).mulPosition(transform);
                    float finalU = reg.u0 + (u[j] * (reg.u1 - reg.u0));
                    float finalV = reg.v0 + (v[j] * (reg.v1 - reg.v0));

                    addVertex(verts, tv.x, tv.y, tv.z, s, s, s, 1.0f, finalU, finalV, (float)reg.layer, 1.0f, 0.0f);
                }
                addIndices(inds, offset);
                offset += 4;
            }
        }
    }

    private void addVertex(List<Float> verts, float x, float y, float z, float r, float g, float b, float a, float u, float v, float layer, float sl, float bl) {
        verts.add(x); verts.add(y); verts.add(z);
        verts.add(r); verts.add(g); verts.add(b); verts.add(a);
        verts.add(u); verts.add(v);
        verts.add(layer);
        verts.add(sl); verts.add(bl);
    }

    private void addIndices(List<Integer> inds, int offset) {
        inds.add(offset + 0); inds.add(offset + 1); inds.add(offset + 2);
        inds.add(offset + 2); inds.add(offset + 3); inds.add(offset + 0);
    }

    private float[] toFloatArray(List<Float> list) { float[] a = new float[list.size()]; for(int i=0;i<list.size();i++) a[i]=list.get(i); return a; }
    private int[] toIntArray(List<Integer> list) { int[] a = new int[list.size()]; for(int i=0;i<list.size();i++) a[i]=list.get(i); return a; }

    private void cleanupMeshes() {
        if (blockMesh != null) { blockMesh.cleanup(); blockMesh = null; }
        if (itemMesh != null) { itemMesh.cleanup(); itemMesh = null; }
    }

    @Override public void cleanup() { cleanupMeshes(); blockPipeline.cleanup(); }
}