package de.delautrer.engine.graphics.utils;

import de.delautrer.engine.graphics.vulkan.core.VulkanContext;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.vulkan.*;
import org.lwjgl.PointerBuffer;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;

public class ScreenshotHelper {

    public static class PendingScreenshot {
        public long dstBuffer;
        public long dstMemory;
        public int width;
        public int height;
        public String filepath;
        public boolean isThumbnail;
    }

    public static PendingScreenshot queueScreenshotCopy(VulkanContext context, VkCommandBuffer cmd, long srcImage, int width, int height,
            String filepath, boolean isThumbnail) {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[ScreenshotHelper] queueScreenshotCopy: Queuing screenshot transfer to buffer. size: " + width + "x" + height + ", path: " + filepath);
        }
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
                    .memoryTypeIndex(findMemoryType(context, memReqs.memoryTypeBits(),
                            VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT));

            LongBuffer pMemory = stack.mallocLong(1);
            vkAllocateMemory(context.getDevice(), allocInfo, null, pMemory);
            long dstMemory = pMemory.get(0);
            vkBindBufferMemory(context.getDevice(), dstBuffer, dstMemory, 0);

            transitionImageLayout(cmd, srcImage, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).layerCount(1);
            region.imageExtent().set(width, height, 1);

            vkCmdCopyImageToBuffer(cmd, srcImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, dstBuffer, region);

            transitionImageLayout(cmd, srcImage, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            PendingScreenshot ps = new PendingScreenshot();
            ps.dstBuffer = dstBuffer;
            ps.dstMemory = dstMemory;
            ps.width = width;
            ps.height = height;
            ps.filepath = filepath;
            ps.isThumbnail = isThumbnail;
            return ps;
        }
    }

    public static void processScreenshotData(VulkanContext context, PendingScreenshot ps) {
        if (de.delautrer.Constants.VULKAN_DEBUG) {
            System.out.println("[ScreenshotHelper] processScreenshotData: Processing screenshot data for " + ps.filepath);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int width = ps.width;
            int height = ps.height;
            String filepath = ps.filepath;
            boolean isThumbnail = ps.isThumbnail;

            // 4. Daten auslesen
            PointerBuffer pData = stack.mallocPointer(1);
            vkMapMemory(context.getDevice(), ps.dstMemory, 0, width * height * 4, 0, pData);
            ByteBuffer vulkanData = pData.getByteBuffer(0, width * height * 4);

            // EIGENEN ARBEITSSPEICHER ALLOZIEREN (da wir den Vulkan-Speicher gleich
            // freigeben!)
            ByteBuffer ramData = org.lwjgl.system.MemoryUtil.memAlloc(width * height * 4);

            // SCHNELLES KOPIEREN (Natives Memcpy, < 1ms)
            org.lwjgl.system.MemoryUtil.memCopy(vulkanData, ramData);

            // Vulkan Speicher SOFORT freigeben, damit das Spiel flüssig weiterläuft!
            vkUnmapMemory(context.getDevice(), ps.dstMemory);
            vkFreeMemory(context.getDevice(), ps.dstMemory, null);
            vkDestroyBuffer(context.getDevice(), ps.dstBuffer, null);

            // 5. ASYNCHRON SPEICHERN (Verhindert den Lag!)
            new Thread(() -> {
                // JETZT ERST KONVERTIEREN WIR (Im asynchronen Thread!)
                for (int i = 0; i < width * height * 4; i += 4) {
                    byte b = ramData.get(i);
                    byte r = ramData.get(i + 2);
                    ramData.put(i, r);
                    ramData.put(i + 2, b);
                    ramData.put(i + 3, (byte) 255);
                }

                if (isThumbnail) {
                    try {
                        int size = Math.min(width, height);
                        int xOffset = (width - size) / 2;
                        int yOffset = (height - size) / 2;
                        
                        int outSize = 200;
                        ByteBuffer resizedData = org.lwjgl.system.MemoryUtil.memAlloc(outSize * outSize * 4);
                        
                        for (int y = 0; y < outSize; y++) {
                            for (int x = 0; x < outSize; x++) {
                                // Nearest neighbor mapping
                                int srcX = xOffset + (x * size / outSize);
                                int srcY = yOffset + (y * size / outSize);
                                
                                int srcIdx = (srcY * width + srcX) * 4;
                                int dstIdx = (y * outSize + x) * 4;
                                
                                resizedData.put(dstIdx, ramData.get(srcIdx));
                                resizedData.put(dstIdx + 1, ramData.get(srcIdx + 1));
                                resizedData.put(dstIdx + 2, ramData.get(srcIdx + 2));
                                resizedData.put(dstIdx + 3, ramData.get(srcIdx + 3));
                            }
                        }
                        
                        org.lwjgl.stb.STBImageWrite.stbi_write_png(filepath, outSize, outSize, 4, resizedData, outSize * 4);
                        System.out.println("Saved thumbnail: " + filepath);
                        org.lwjgl.system.MemoryUtil.memFree(resizedData);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    STBImageWrite.stbi_write_png(filepath, width, height, 4, ramData, width * 4);
                    System.out.println("Saved: " + filepath);
                }

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
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1)
                    .baseArrayLayer(0).layerCount(1);

            int srcStage;
            int dstStage;

            if (oldLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR && newLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL) {
                // Present -> Transfer read: wait for color attachment output to finish
                barrier.srcAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_PRESENT_SRC_KHR) {
                // Transfer read -> Present: wait for transfer to finish before present reads
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                dstStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
            } else {
                barrier.srcAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_MEMORY_READ_BIT);
                srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                dstStage = VK_PIPELINE_STAGE_BOTTOM_OF_PIPE_BIT;
            }

            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier);
        }
    }

    private static int findMemoryType(VulkanContext context, int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc();
        vkGetPhysicalDeviceMemoryProperties(context.getPhysicalDevice(), memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0
                    && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
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
