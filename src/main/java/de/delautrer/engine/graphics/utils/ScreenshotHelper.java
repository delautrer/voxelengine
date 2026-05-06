package de.delautrer.engine.graphics.utils;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.vulkan.core.VulkanContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.vulkan.*;
import org.lwjgl.PointerBuffer;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

public class ScreenshotHelper {

    public static void saveScreenshot(VulkanContext context, long commandPool, long srcImage, int width, int height, String filepath) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // 1. Lineares Bild (Buffer) erstellen
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size((long) width * height * 4)
                    .usage(VK_BUFFER_USAGE_TRANSFER_DST_BIT);

            LongBuffer pBuffer = stack.mallocLong(1);
            vkCreateBuffer(context.getDevice(), bufferInfo, null, pBuffer);
            long dstBuffer = pBuffer.get(0);

            // 2. Speicher allozieren
            VkMemoryRequirements memReqs = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(context.getDevice(), dstBuffer, memReqs);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(memReqs.size())
                    .memoryTypeIndex(findMemoryType(context, memReqs.memoryTypeBits(), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            vkAllocateMemory(context.getDevice(), allocInfo, null, pMemory);
            long dstMemory = pMemory.get(0);
            vkBindBufferMemory(context.getDevice(), dstBuffer, dstMemory, 0);

            // 3. Bild in den Buffer kopieren (Synchron mit eigenen Helper-Methoden)
            VkCommandBuffer cmd = beginSingleTimeCommands(context, commandPool);

            transitionImageLayout(cmd, srcImage, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1);
            region.imageExtent().set(width, height, 1);

            vkCmdCopyImageToBuffer(cmd, srcImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, dstBuffer, region);

            transitionImageLayout(cmd, srcImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            endSingleTimeCommands(context, commandPool, cmd);

            // 4. Daten auslesen
            PointerBuffer pData = stack.mallocPointer(1);
            vkMapMemory(context.getDevice(), dstMemory, 0, width * height * 4, 0, pData);
            ByteBuffer vulkanData = pData.getByteBuffer(0, width * height * 4);

            // EIGENEN ARBEITSSPEICHER ALLOZIEREN (da wir den Vulkan-Speicher gleich freigeben!)
            ByteBuffer ramData = org.lwjgl.system.MemoryUtil.memAlloc(width * height * 4);

            // BGRA zu RGBA konvertieren UND gleichzeitig in RAM kopieren
            for (int i = 0; i < width * height * 4; i += 4) {
                ramData.put(i, vulkanData.get(i + 2));           // R <- B
                ramData.put(i + 1, vulkanData.get(i + 1)); // G <- G
                ramData.put(i + 2, vulkanData.get(i));     // B <- R
                ramData.put(i + 3, (byte) 255);            // A zwingend auf 1.0
            }

            // Vulkan Speicher SOFORT freigeben, damit das Spiel flüssig weiterläuft!
            vkUnmapMemory(context.getDevice(), dstMemory);
            vkFreeMemory(context.getDevice(), dstMemory, null);
            vkDestroyBuffer(context.getDevice(), dstBuffer, null);

            // 5. ASYNCHRON SPEICHERN (Verhindert den Lag!)
            new Thread(() -> {
                STBImageWrite.stbi_write_png(filepath, width, height, 4, ramData, width * 4);
                System.out.println("[Screenshot] Saved: " + filepath);

                // C-Speicher wieder freigeben, um Memory Leaks zu verhindern
                org.lwjgl.system.MemoryUtil.memFree(ramData);
            }).start();
        }
    }

    private static void transitionImageLayout(VkCommandBuffer cmd, long image, int oldLayout, int newLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            barrier.srcAccessMask(VK_ACCESS_MEMORY_READ_BIT);
            barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

            vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier);
        }
    }

    private static int findMemoryType(VulkanContext context, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(context.getPhysicalDevice(), memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                memProperties.free();
                return i;
            }
        }
        memProperties.free();
        throw new RuntimeException("Failed to find suitable memory type!");
    }

    // --- FIX: Wir managen die Commands jetzt selbst! ---
    private static VkCommandBuffer beginSingleTimeCommands(VulkanContext context, long commandPool) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandPool(commandPool)
                    .commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(context.getDevice(), allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), context.getDevice());

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);
            return commandBuffer;
        }
    }

    private static void endSingleTimeCommands(VulkanContext context, long commandPool, VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(commandBuffer));

            vkQueueSubmit(context.getGraphicsQueue(), submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(context.getGraphicsQueue());

            vkFreeCommandBuffers(context.getDevice(), commandPool, commandBuffer);
        }
    }
}
