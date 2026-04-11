package de.delautrer.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class VulkanDepthBuffer {

    private final VulkanContext context;
    private long depthImage;
    private long depthImageMemory;
    private long depthImageView;

    public VulkanDepthBuffer(VulkanContext context, VulkanSwapchain swapchain) {
        this.context = context;
        createDepthImage(swapchain.getExtent().width(), swapchain.getExtent().height());
        createDepthImageView();
    }

    private void createDepthImage(int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK10.VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width).height(height).depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(VK10.VK_FORMAT_D32_SFLOAT);
            imageInfo.tiling(VK10.VK_IMAGE_TILING_OPTIMAL);
            imageInfo.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(VK10.VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT);
            imageInfo.samples(VK10.VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pImage = stack.mallocLong(1);
            if (VK10.vkCreateImage(context.getDevice(), imageInfo, null, pImage) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create depth image");
            }
            depthImage = pImage.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            VK10.vkGetImageMemoryRequirements(context.getDevice(), depthImage, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            // local device memory for maximum performance
            allocInfo.memoryTypeIndex(context.findMemoryType(memRequirements.memoryTypeBits(), VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));

            LongBuffer pImageMemory = stack.mallocLong(1);
            if (VK10.vkAllocateMemory(context.getDevice(), allocInfo, null, pImageMemory) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate depth image memory");
            }
            depthImageMemory = pImageMemory.get(0);

            VK10.vkBindImageMemory(context.getDevice(), depthImage, depthImageMemory, 0);
        }
    }

    private void createDepthImageView() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(depthImage);
            viewInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(VK10.VK_FORMAT_D32_SFLOAT);
            viewInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_DEPTH_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);
            if (VK10.vkCreateImageView(context.getDevice(), viewInfo, null, pImageView) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create depth image view");
            }
            depthImageView = pImageView.get(0);
        }
    }

    public long getDepthImageView() {
        return depthImageView;
    }

    public void cleanup() {
        VK10.vkDestroyImageView(context.getDevice(), depthImageView, null);
        VK10.vkDestroyImage(context.getDevice(), depthImage, null);
        VK10.vkFreeMemory(context.getDevice(), depthImageMemory, null);
    }
}