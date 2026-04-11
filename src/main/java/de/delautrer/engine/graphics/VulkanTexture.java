package de.delautrer.engine.graphics;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanTexture {

    private final VulkanContext context;
    private long textureImage;
    private long textureImageMemory;
    private long textureImageView;
    private long textureSampler;
    private long descriptorPool;
    private long descriptorSet;

    // --- 1. KONSTRUKTOR (Für Dateipfade, z.B. gui.png) ---
    public VulkanTexture(VulkanContext context, VulkanCommandBuffers commandBuffers, long descriptorSetLayout, String path) {
        this.context = context;
        createTextureImage(commandBuffers, path);
        createTextureImageView();
        createTextureSampler();
        createDescriptorPool();
        createDescriptorSet(descriptorSetLayout);
    }

    // --- 2. NEUER KONSTRUKTOR (Für rohe Pixeldaten, z.B. Font) ---
    public VulkanTexture(VulkanContext context, VulkanCommandBuffers commandBuffers, long descriptorSetLayout, ByteBuffer pixels, int width, int height) {
        this.context = context;
        createTextureImage(commandBuffers, pixels, width, height);
        createTextureImageView();
        createTextureSampler();
        createDescriptorPool();
        createDescriptorSet(descriptorSetLayout);
    }

    // --- METHODE FÜR DATEIPFAD ---
    private void createTextureImage(VulkanCommandBuffers commandBuffers, String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load(path, pWidth, pHeight, pChannels, STBImage.STBI_rgb_alpha);

            if (pixels == null) {
                throw new RuntimeException("Failed to load texture image: " + path);
            }

            int texWidth = pWidth.get(0);
            int texHeight = pHeight.get(0);
            long imageSize = (long) texWidth * texHeight * 4;

            VulkanBuffer stagingBuffer = new VulkanBuffer(context, imageSize, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), stagingBuffer.getBufferMemory(), 0, imageSize, 0, data);
            long destAddress = data.get(0);
            org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(pixels), destAddress, imageSize);
            VK10.vkUnmapMemory(context.getDevice(), stagingBuffer.getBufferMemory());

            STBImage.stbi_image_free(pixels);

            createImage(texWidth, texHeight, VK10.VK_FORMAT_R8G8B8A8_SRGB, VK10.VK_IMAGE_TILING_OPTIMAL,
                    VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyBufferToImage(commandBuffers, stagingBuffer.getBuffer(), textureImage, texWidth, texHeight);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            stagingBuffer.cleanup();
        }
    }

    // --- NEUE METHODE FÜR PIXELDATEN (ByteBuffer) ---
    private void createTextureImage(VulkanCommandBuffers commandBuffers, ByteBuffer pixels, int texWidth, int texHeight) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long imageSize = (long) texWidth * texHeight * 4;

            VulkanBuffer stagingBuffer = new VulkanBuffer(context, imageSize, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), stagingBuffer.getBufferMemory(), 0, imageSize, 0, data);
            long destAddress = data.get(0);
            org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(pixels), destAddress, imageSize);
            VK10.vkUnmapMemory(context.getDevice(), stagingBuffer.getBufferMemory());

            // Pixel wurden kopiert, jetzt Vulkan-Image erstellen
            createImage(texWidth, texHeight, VK10.VK_FORMAT_R8G8B8A8_SRGB, VK10.VK_IMAGE_TILING_OPTIMAL,
                    VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            copyBufferToImage(commandBuffers, stagingBuffer.getBuffer(), textureImage, texWidth, texHeight);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

            stagingBuffer.cleanup();
        }
    }

    private void createImage(int width, int height, int format, int tiling, int usage, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK10.VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width).height(height).depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(VK10.VK_SAMPLE_COUNT_1_BIT);
            imageInfo.sharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pImage = stack.mallocLong(1);
            if (VK10.vkCreateImage(context.getDevice(), imageInfo, null, pImage) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create image");
            }
            textureImage = pImage.get(0);

            VkMemoryRequirements memRequirements = VkMemoryRequirements.calloc(stack);
            VK10.vkGetImageMemoryRequirements(context.getDevice(), textureImage, memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(context.findMemoryType(memRequirements.memoryTypeBits(), properties));

            LongBuffer pImageMemory = stack.mallocLong(1);
            if (VK10.vkAllocateMemory(context.getDevice(), allocInfo, null, pImageMemory) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate image memory");
            }
            textureImageMemory = pImageMemory.get(0);

            VK10.vkBindImageMemory(context.getDevice(), textureImage, textureImageMemory, 0);
        }
    }

    private void transitionImageLayout(VulkanCommandBuffers commandBuffers, long image, int format, int oldLayout, int newLayout) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.beginSingleTimeCommands();

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack);
            barrier.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK10.VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);
            barrier.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(1);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            int sourceStage;
            int destinationStage;

            if (oldLayout == VK10.VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                sourceStage = VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
            } else if (oldLayout == VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
                barrier.srcAccessMask(VK10.VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK10.VK_ACCESS_SHADER_READ_BIT);
                sourceStage = VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
            } else {
                throw new IllegalArgumentException("Unsupported layout transition!");
            }

            VK10.vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barrier);

            commandBuffers.endSingleTimeCommands(commandBuffer);
        }
    }

    private void copyBufferToImage(VulkanCommandBuffers commandBuffers, long buffer, long image, int width, int height) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.beginSingleTimeCommands();

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);
            region.bufferImageHeight(0);

            region.imageSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);

            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(width, height, 1);

            VK10.vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            commandBuffers.endSingleTimeCommands(commandBuffer);
        }
    }

    private void createTextureImageView() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(textureImage);
            viewInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(VK10.VK_FORMAT_R8G8B8A8_SRGB);
            viewInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);
            if (VK10.vkCreateImageView(context.getDevice(), viewInfo, null, pImageView) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture image view");
            }
            textureImageView = pImageView.get(0);
        }
    }

    private void createTextureSampler() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType(VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO);
            samplerInfo.magFilter(VK10.VK_FILTER_NEAREST);
            samplerInfo.minFilter(VK10.VK_FILTER_NEAREST);
            samplerInfo.addressModeU(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeV(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.addressModeW(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT);
            samplerInfo.anisotropyEnable(false);
            samplerInfo.borderColor(VK10.VK_BORDER_COLOR_INT_OPAQUE_BLACK);
            samplerInfo.unnormalizedCoordinates(false);
            samplerInfo.compareEnable(false);
            samplerInfo.compareOp(VK10.VK_COMPARE_OP_ALWAYS);
            samplerInfo.mipmapMode(VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR);

            LongBuffer pSampler = stack.mallocLong(1);
            if (VK10.vkCreateSampler(context.getDevice(), samplerInfo, null, pSampler) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create texture sampler");
            }
            textureSampler = pSampler.get(0);
        }
    }

    private void createDescriptorPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.calloc(1, stack);
            poolSize.type(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            poolSize.descriptorCount(1);

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSize);
            poolInfo.maxSets(1);

            LongBuffer pDescriptorPool = stack.mallocLong(1);
            if (VK10.vkCreateDescriptorPool(context.getDevice(), poolInfo, null, pDescriptorPool) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }
            descriptorPool = pDescriptorPool.get(0);
        }
    }

    private void createDescriptorSet(long descriptorSetLayout) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            org.lwjgl.vulkan.VkDescriptorSetAllocateInfo allocInfo = org.lwjgl.vulkan.VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType(org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            java.nio.LongBuffer pDescriptorSet = stack.mallocLong(1);
            if (org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets(context.getDevice(), allocInfo, pDescriptorSet) != org.lwjgl.vulkan.VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor set");
            }
            descriptorSet = pDescriptorSet.get(0);

            org.lwjgl.vulkan.VkDescriptorImageInfo.Buffer imageInfo = org.lwjgl.vulkan.VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.imageLayout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            imageInfo.imageView(textureImageView);
            imageInfo.sampler(textureSampler);

            org.lwjgl.vulkan.VkWriteDescriptorSet.Buffer descriptorWrite = org.lwjgl.vulkan.VkWriteDescriptorSet.calloc(1, stack);
            descriptorWrite.sType(org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            descriptorWrite.dstSet(descriptorSet);
            descriptorWrite.dstBinding(0);
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.descriptorType(org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);

            descriptorWrite.descriptorCount(1);

            descriptorWrite.pImageInfo(imageInfo);

            org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets(context.getDevice(), descriptorWrite, null);
        }
    }

    public long getDescriptorSet() {
        return descriptorSet;
    }

    public void cleanup() {
        VK10.vkDestroyDescriptorPool(context.getDevice(), descriptorPool, null);
        VK10.vkDestroySampler(context.getDevice(), textureSampler, null);
        VK10.vkDestroyImageView(context.getDevice(), textureImageView, null);
        VK10.vkDestroyImage(context.getDevice(), textureImage, null);
        VK10.vkFreeMemory(context.getDevice(), textureImageMemory, null);
    }
}