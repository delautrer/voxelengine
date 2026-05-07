package de.delautrer.engine.graphics.systems;

import de.delautrer.engine.graphics.RenderPacket;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface IRenderSystem {
    void render(VkCommandBuffer commandBuffer, RenderPacket packet);

    void cleanup();
}
