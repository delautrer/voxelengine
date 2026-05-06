package de.delautrer.engine.graphics.vulkan.buffer;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.system.MemoryStack;
import java.nio.LongBuffer;

public class VulkanBuffer {
    private final VulkanContext context;
    private final long buffer;
    private final long bufferMemory;

    public VulkanBuffer(VulkanContext context, long size, int usage, int properties) {
        this.context = context;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack);
            bufferInfo.sType(VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            bufferInfo.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuffer = stack.mallocLong(1);
            if (VK10.vkCreateBuffer(context.getDevice(), bufferInfo, null, pBuffer) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create buffer");
            }
            buffer = pBuffer.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            VK10.vkGetBufferMemoryRequirements(context.getDevice(), buffer, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(context.findMemoryType(memRequirements.memoryTypeBits(), properties));

            LongBuffer pBufferMemory = stack.mallocLong(1);
            if (VK10.vkAllocateMemory(context.getDevice(), allocInfo, null, pBufferMemory) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate buffer memory");
            }
            bufferMemory = pBufferMemory.get(0);

            VK10.vkBindBufferMemory(context.getDevice(), buffer, bufferMemory, 0);
        }
    }

    public long getBuffer() { return buffer; }
    public long getBufferMemory() { return bufferMemory; }

    public void cleanup() {
        VK10.vkDestroyBuffer(context.getDevice(), buffer, null);
        VK10.vkFreeMemory(context.getDevice(), bufferMemory, null);
    }
}
