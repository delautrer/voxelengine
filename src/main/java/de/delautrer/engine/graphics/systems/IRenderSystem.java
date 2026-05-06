package de.delautrer.engine.graphics.systems;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.RenderPacket;
import org.lwjgl.vulkan.VkCommandBuffer;

public interface IRenderSystem {
    void render(VkCommandBuffer commandBuffer, RenderPacket packet);
    void cleanup();
}
