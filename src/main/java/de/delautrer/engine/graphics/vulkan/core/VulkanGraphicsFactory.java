package de.delautrer.engine.graphics.vulkan.core;

import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.buffer.VulkanMesh;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.graphics.vulkan.texture.VulkanFont;
import de.delautrer.engine.graphics.vulkan.texture.VulkanTexture;
import de.delautrer.engine.graphics.vulkan.texture.VulkanTextureArray;
import de.delautrer.engine.graphics.utils.TextureStitcher;
import java.nio.ByteBuffer;

public class VulkanGraphicsFactory implements IGraphicsFactory {
    
    private final VulkanContext context;
    private final VulkanCommandBuffers commandBuffers;
    private final long graphicsLayout;
    private final long uiLayout;

    public VulkanGraphicsFactory(VulkanContext context, VulkanCommandBuffers commandBuffers, long graphicsLayout, long uiLayout) {
        this.context = context;
        this.commandBuffers = commandBuffers;
        this.graphicsLayout = graphicsLayout;
        this.uiLayout = uiLayout;
    }

    @Override
    public IMesh createMesh(float[] vertices, int[] indices) {
        return new VulkanMesh(context, vertices, indices);
    }

    @Override
    public IMesh createMesh(MeshData data) {
        return new VulkanMesh(context, data);
    }

    @Override
    public ITexture createTexture(String path) {
        return new VulkanTexture(context, commandBuffers, uiLayout, path);
    }

    @Override
    public ITexture createTexture(TextureStitcher.AtlasResult atlas) {
        return new VulkanTexture(context, commandBuffers, uiLayout, atlas);
    }

    @Override
    public ITexture createTexture(ByteBuffer rgbaPixels, int width, int height) {
        return new VulkanTexture(context, commandBuffers, uiLayout, rgbaPixels, width, height);
    }

    @Override
    public ITextureArray createTextureArray(TextureStitcher.AtlasResult atlas) {
        return new VulkanTextureArray(context, commandBuffers, graphicsLayout, atlas);
    }

    @Override
    public IFont createFont(String path, float size) {
        return new VulkanFont(path, size);
    }

    @Override
    public void waitIdle() {
        org.lwjgl.vulkan.VK10.vkQueueWaitIdle(context.getGraphicsQueue());
    }
}
