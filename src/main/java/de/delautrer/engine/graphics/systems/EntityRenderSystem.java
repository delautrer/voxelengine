package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.utils.TextureStitcher.AtlasRegion;
import de.delautrer.engine.physics.AABB;

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
import java.util.List;

public class EntityRenderSystem implements IRenderSystem {
    private static class FloatList {
        float[] data = new float[1024];
        int size = 0;
        void add(float v) {
            if (size == data.length) {
                float[] n = new float[data.length * 2];
                System.arraycopy(data, 0, n, 0, size);
                data = n;
            }
            data[size++] = v;
        }
        void clear() { size = 0; }
        boolean isEmpty() { return size == 0; }
    }

    private static class IntList {
        int[] data = new int[1024];
        int size = 0;
        void add(int v) {
            if (size == data.length) {
                int[] n = new int[data.length * 2];
                System.arraycopy(data, 0, n, 0, size);
                data = n;
            }
            data[size++] = v;
        }
        void clear() { size = 0; }
        boolean isEmpty() { return size == 0; }
    }

    private final VulkanContext context;
    private final VulkanGraphicsPipeline blockPipeline;

    private VulkanMesh[] blockMeshes = new VulkanMesh[de.delautrer.engine.graphics.vulkan.core.VulkanSync.MAX_FRAMES_IN_FLIGHT];
    private VulkanMesh[] itemMeshes = new VulkanMesh[de.delautrer.engine.graphics.vulkan.core.VulkanSync.MAX_FRAMES_IN_FLIGHT];

    private final FloatList blockVerts = new FloatList();
    private final IntList blockInds = new IntList();
    private final FloatList itemVerts = new FloatList();
    private final IntList itemInds = new IntList();

    public EntityRenderSystem(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass) {
        this.context = context;
        this.blockPipeline = new VulkanGraphicsPipeline(context, swapchain, renderPass);
    }

    @Override
    public void render(VkCommandBuffer cmd, RenderPacket packet) {
        if (packet.entities == null || packet.entities.isEmpty()) {
            return;
        }

        blockVerts.clear();
        blockInds.clear();
        int blockOffset = 0;

        itemVerts.clear();
        itemInds.clear();
        int itemOffset = 0;

        double t = System.currentTimeMillis() / 1000.0;

        for (Entity e : packet.entities) {
            if (e.isDead()) continue;

            if (e instanceof ItemEntity itemEntity) {
                Item itemType = itemEntity.stack.type;
                int count = itemEntity.stack.amount;

                int visualCount = 1;
                if (count > 1) visualCount = 2;
                if (count > 15) visualCount = 3;
                if (count > 31) visualCount = 4;

                float phaseOffset = itemEntity.renderPhase * 0.5f; // 4 Phasen à 10 Ticks (0.5s)
                float hoverY = (float) Math.sin((t + phaseOffset) * Math.PI) * 0.1f + 0.15f;

                // Lighting from world
                float sl = itemEntity.skyLightBrightness;
                float bl = itemEntity.blockLightBrightness;

                for (int v = 0; v < visualCount; v++) {
                    float pileOffsetX = v * 0.04f;
                    float pileOffsetY = v * 0.04f;
                    float pileOffsetZ = v * -0.04f;

                    Matrix4f modelMat = new Matrix4f()
                            .translate((float) (e.position.x - packet.cameraPos.x) + pileOffsetX,
                                    (float) (e.position.y - packet.cameraPos.y) + hoverY + pileOffsetY,
                                    (float) (e.position.z - packet.cameraPos.z) + pileOffsetZ)
                            .rotateY((float) ((t + phaseOffset) * 1.0));

                    if (itemType instanceof BlockItem blockItem && blockItem.getBlock() instanceof CubeBlock cubeBlock
                            && !(cubeBlock instanceof TorchBlock)) {
                        modelMat.scale(0.25f);
                        build3DBlock(blockVerts, blockInds, blockOffset, modelMat, cubeBlock, sl, bl);
                        blockOffset = blockVerts.size / 12;
                    } else {
                        AtlasRegion reg = itemType.getIconRegion();
                        if (reg != null) {
                            modelMat.scale(0.5f);
                            buildThickItem(itemVerts, itemInds, itemOffset, modelMat, reg, sl, bl);
                            itemOffset = itemVerts.size / 12;
                        }
                    }
                }
            } else if (e instanceof de.delautrer.game.entity.FallingBlockEntity fallingBlock) {
                de.delautrer.game.blocks.Block block = fallingBlock.getBlock();
                if (block instanceof CubeBlock cubeBlock) {
                    Matrix4f modelMat = new Matrix4f()
                            .translate((float) (e.position.x - packet.cameraPos.x),
                                    (float) (e.position.y - packet.cameraPos.y + 0.5f),
                                    (float) (e.position.z - packet.cameraPos.z));
                    float sl = e.skyLightBrightness;
                    float bl = e.blockLightBrightness;
                    build3DBlock(blockVerts, blockInds, blockOffset, modelMat, cubeBlock, sl, bl);
                    blockOffset = blockVerts.size / 12;
                }
            }
        }

        if (blockVerts.isEmpty() && itemVerts.isEmpty())
            return;

        int frameIndex = packet.frameIndex;

        if (!blockVerts.isEmpty()) {
            if (blockMeshes[frameIndex] == null) {
                blockMeshes[frameIndex] = new VulkanMesh(context, blockVerts.data, blockVerts.size, blockInds.data, blockInds.size);
            } else {
                blockMeshes[frameIndex].updateMesh(blockVerts.data, blockVerts.size, blockInds.data, blockInds.size);
            }
            VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, blockPipeline.getHandle());
            bindAndDraw(cmd, packet, blockPipeline.getPipelineLayout(), blockMeshes[frameIndex],
                    ((VulkanTextureArray) packet.worldTexture).getDescriptorSet());
        }

        if (!itemVerts.isEmpty()) {
            if (itemMeshes[frameIndex] == null) {
                itemMeshes[frameIndex] = new VulkanMesh(context, itemVerts.data, itemVerts.size, itemInds.data, itemInds.size);
            } else {
                itemMeshes[frameIndex].updateMesh(itemVerts.data, itemVerts.size, itemInds.data, itemInds.size);
            }
            VK10.vkCmdBindPipeline(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, blockPipeline.getHandle());
            bindAndDraw(cmd, packet, blockPipeline.getPipelineLayout(), itemMeshes[frameIndex],
                    ((VulkanTextureArray) packet.itemTextureArray).getDescriptorSet());
        }
    }

    private void bindAndDraw(VkCommandBuffer cmd, RenderPacket packet, long pipelineLayout, VulkanMesh mesh,
            long descriptorSet) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f mvp = new Matrix4f(packet.mvp);

            FloatBuffer buf = stack.callocFloat(32);
            mvp.get(buf);
            buf.put(16, packet.globalLight);
            buf.put(17, packet.renderDistance);
            buf.put(18, 1.0f);
            buf.put(19, (float) packet.cameraPos.x);
            buf.put(20, (float) packet.cameraPos.y);
            buf.put(21, (float) packet.cameraPos.z);
            buf.put(22, 0.0f);
            buf.put(23, 0.0f);
            buf.put(24, 0.0f);
            buf.put(25, 0.0f);
            buf.put(26, packet.isUnderwater ? 1.0f : 0.0f);
            buf.put(27, packet.clipY);

            VK10.vkCmdPushConstants(cmd, pipelineLayout,
                    VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT, 0, buf);

            VK10.vkCmdBindDescriptorSets(cmd, VK10.VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0,
                    stack.longs(descriptorSet), null);
            VK10.vkCmdBindVertexBuffers(cmd, 0, stack.longs(mesh.getVertexBuffer()), stack.longs(0));
            VK10.vkCmdBindIndexBuffer(cmd, mesh.getIndexBuffer(), 0, VK10.VK_INDEX_TYPE_UINT32);
            VK10.vkCmdDrawIndexed(cmd, mesh.getIndexCount(), 1, 0, 0, 0);
        }
    }

    private void buildThickItem(FloatList verts, IntList inds, int offset, Matrix4f transform,
            AtlasRegion reg, float sl, float bl) {
        float thickness = 0.03f;
        Vector3f[] posUp = {
                new Vector3f(-0.5f, thickness, -0.5f), new Vector3f(0.5f, thickness, -0.5f),
                new Vector3f(0.5f, thickness, 0.5f), new Vector3f(-0.5f, thickness, 0.5f)
        };
        Vector3f[] posDown = {
                new Vector3f(-0.5f, 0.0f, 0.5f), new Vector3f(0.5f, 0.0f, 0.5f),
                new Vector3f(0.5f, 0.0f, -0.5f), new Vector3f(-0.5f, 0.0f, -0.5f)
        };

        float[] u = { reg.u0, reg.u1, reg.u1, reg.u0 };
        float[] v = { reg.v1, reg.v1, reg.v0, reg.v0 };

        for (int i = 0; i < 4; i++) {
            Vector3f p = new Vector3f(posUp[i]).mulPosition(transform);
            addVertex(verts, p.x, p.y, p.z, 1.0f, 1.0f, 1.0f, 1.0f, u[i], v[i], reg.layer, sl, bl);
        }
        addIndices(inds, offset);
        offset += 4;

        for (int i = 0; i < 4; i++) {
            Vector3f p = new Vector3f(posDown[i]).mulPosition(transform);
            addVertex(verts, p.x, p.y, p.z, 0.6f, 0.6f, 0.6f, 1.0f, u[i], v[i], reg.layer, sl, bl);
        }
        addIndices(inds, offset);
        offset += 4;

        Vector3f[][] edges = {
                { posUp[3], posUp[2], posDown[1], posDown[0] },
                { posUp[1], posUp[0], posDown[3], posDown[2] },
                { posUp[2], posUp[1], posDown[2], posDown[1] },
                { posUp[0], posUp[3], posDown[0], posDown[3] }
        };

        for (int e = 0; e < 4; e++) {
            for (int i = 0; i < 4; i++) {
                Vector3f p = new Vector3f(edges[e][i]).mulPosition(transform);
                addVertex(verts, p.x, p.y, p.z, 0.3f, 0.3f, 0.3f, 1.0f, reg.u0, reg.v0, reg.layer, sl, bl);
            }
            addIndices(inds, offset);
            offset += 4;
        }
    }

    private void build3DBlock(FloatList verts, IntList inds, int offset, Matrix4f transform, CubeBlock block, float sl, float bl) {
        BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP,
                BlockFace.DOWN };
        float[] shades = { 0.8f, 0.8f, 0.65f, 0.65f, 1.0f, 0.4f };
        List<AABB> boxes = block.getBoundingBoxes(block.getDefaultState());

        for (AABB box : boxes) {
            float bx0 = box.min.x;
            float by0 = box.min.y;
            float bz0 = box.min.z;
            float bx1 = box.max.x;
            float by1 = box.max.y;
            float bz1 = box.max.z;
            float minX = bx0 - 0.5f;
            float minY = by0 - 0.5f;
            float minZ = bz0 - 0.5f;
            float maxX = bx1 - 0.5f;
            float maxY = by1 - 0.5f;
            float maxZ = bz1 - 0.5f;

            float sideV0, sideV1;
            if (block.getModel() != null && block.getModel().directional_textures) {
                sideV0 = 1.0f - by1;
                sideV1 = 1.0f - by0;
            } else {
                if (bx1 - bx0 > 0.99f && by1 - by0 > 0.99f && bz1 - bz0 > 0.99f) {
                    sideV0 = 0.0f;
                    sideV1 = 1.0f;
                } else {
                    if (by1 <= 0.5f) {
                        sideV0 = 0.5f;
                        sideV1 = 1.0f;
                    } else {
                        sideV0 = 0.0f;
                        sideV1 = 0.5f;
                    }
                }
            }

            Vector3f[][] coords = {
                    { new Vector3f(maxX, minY, minZ), new Vector3f(minX, minY, minZ), new Vector3f(minX, maxY, minZ),
                            new Vector3f(maxX, maxY, minZ) },
                    { new Vector3f(minX, minY, maxZ), new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, maxY, maxZ),
                            new Vector3f(minX, maxY, maxZ) },
                    { new Vector3f(maxX, minY, maxZ), new Vector3f(maxX, minY, minZ), new Vector3f(maxX, maxY, minZ),
                            new Vector3f(maxX, maxY, maxZ) },
                    { new Vector3f(minX, minY, minZ), new Vector3f(minX, minY, maxZ), new Vector3f(minX, maxY, maxZ),
                            new Vector3f(minX, maxY, minZ) },
                    { new Vector3f(minX, maxY, minZ), new Vector3f(minX, maxY, maxZ), new Vector3f(maxX, maxY, maxZ),
                            new Vector3f(maxX, maxY, minZ) },
                    { new Vector3f(minX, minY, maxZ), new Vector3f(minX, minY, minZ), new Vector3f(maxX, minY, minZ),
                            new Vector3f(maxX, minY, maxZ) }
            };

            float[][] u_faces = {
                    { 1.0f - bx1, 1.0f - bx0, 1.0f - bx0, 1.0f - bx1 }, { bx0, bx1, bx1, bx0 },
                    { 1.0f - bz1, 1.0f - bz0, 1.0f - bz0, 1.0f - bz1 }, { bz0, bz1, bz1, bz0 },
                    { bx0, bx1, bx1, bx0 }, { bx0, bx1, bx1, bx0 }
            };
            float[][] v_faces = {
                    { sideV1, sideV1, sideV0, sideV0 }, { sideV1, sideV1, sideV0, sideV0 },
                    { sideV1, sideV1, sideV0, sideV0 }, { sideV1, sideV1, sideV0, sideV0 },
                    { bz1, bz1, bz0, bz0 }, { bz1, bz1, bz0, bz0 }
            };

            for (int i = 0; i < 6; i++) {
                AtlasRegion reg = block.getTextureForFace(block.getDefaultState(), faces[i]);
                if (reg == null)
                    continue;

                Vector3f[] p = coords[i];
                float[] u = u_faces[i];
                float[] v = v_faces[i];
                float s = shades[i];

                for (int j = 0; j < 4; j++) {
                    Vector3f tv = new Vector3f(p[j]).mulPosition(transform);
                    float finalU = reg.u0 + (u[j] * (reg.u1 - reg.u0));
                    float finalV = reg.v0 + (v[j] * (reg.v1 - reg.v0));
                    addVertex(verts, tv.x, tv.y, tv.z, s, s, s, 1.0f, finalU, finalV, reg.layer, sl, bl);
                }
                addIndices(inds, offset);
                offset += 4;
            }
        }
    }

    private void addVertex(FloatList verts, float x, float y, float z, float r, float g, float b, float a, float u,
            float v, float layer, float sl, float bl) {
        verts.add(x);
        verts.add(y);
        verts.add(z);
        verts.add(r);
        verts.add(g);
        verts.add(b);
        verts.add(a);
        verts.add(u);
        verts.add(v);
        verts.add(layer);
        verts.add(sl);
        verts.add(bl);
    }

    private void addIndices(IntList inds, int offset) {
        inds.add(offset + 0);
        inds.add(offset + 1);
        inds.add(offset + 2);
        inds.add(offset + 2);
        inds.add(offset + 3);
        inds.add(offset + 0);
    }

    private void cleanupMeshes() {
        for (int i = 0; i < blockMeshes.length; i++) {
            if (blockMeshes[i] != null) {
                blockMeshes[i].cleanup();
                blockMeshes[i] = null;
            }
        }
        for (int i = 0; i < itemMeshes.length; i++) {
            if (itemMeshes[i] != null) {
                itemMeshes[i].cleanup();
                itemMeshes[i] = null;
            }
        }
    }

    @Override
    public void cleanup() {
        cleanupMeshes();
        blockPipeline.cleanup();
    }
}
