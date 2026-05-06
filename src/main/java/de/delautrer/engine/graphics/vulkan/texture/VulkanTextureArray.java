package de.delautrer.engine.graphics.vulkan.texture;
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

import de.delautrer.engine.graphics.utils.TextureStitcher;
import de.delautrer.engine.utils.AssetManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanTextureArray {

    private final VulkanContext context;
    private long textureImage;
    private long textureImageMemory;
    private long textureImageView;
    private long textureSampler;
    private long descriptorPool;
    private long descriptorSet;

    // --- ALTE METHODE (Bleibt als Fallback, falls du sie noch woanders brauchst) ---
    public VulkanTextureArray(VulkanContext context, VulkanCommandBuffers commandBuffers, long descriptorSetLayout, String path) {
        this.context = context;
        int layerCount = 256;
        createTextureArray(commandBuffers, path, layerCount);
        createTextureImageView(layerCount);
        createTextureSampler();
        createDescriptorPool();
        createDescriptorSet(descriptorSetLayout);
    }

    // --- NEUE METHODE: Direkt aus dem Arbeitsspeicher (TextureStitcher) ---
    public VulkanTextureArray(VulkanContext context, VulkanCommandBuffers commandBuffers, long descriptorSetLayout, TextureStitcher.AtlasResult atlasResult) {
        this.context = context;

        // Berechne, wie viele Bilder im Atlas sind
        int tilesX = atlasResult.atlasWidth / TextureStitcher.TEXTURE_SIZE;
        int tilesY = atlasResult.atlasHeight / TextureStitcher.TEXTURE_SIZE;
        int layerCount = tilesX * tilesY; // Z.B. 4x4 Grid = 16 Layers

        createTextureArrayFromMemory(commandBuffers, atlasResult.atlasPixels, atlasResult.atlasWidth, atlasResult.atlasHeight, tilesX, tilesY, layerCount);

        createTextureImageView(layerCount);
        createTextureSampler();
        createDescriptorPool();
        createDescriptorSet(descriptorSetLayout);
    }

    // --- NEUE HILFSMETHODE: Kopiert die reinen Bytes ohne STBImage! ---
    private void createTextureArrayFromMemory(VulkanCommandBuffers commandBuffers, ByteBuffer pixels, int texWidth, int texHeight, int tilesX, int tilesY, int layerCount) {
        long imageSize = (long) texWidth * texHeight * 4;
        int tileSizeX = texWidth / tilesX; // Sollte immer 16 sein
        int tileSizeY = texHeight / tilesY;

        VulkanBuffer stagingBuffer = new VulkanBuffer(context, imageSize, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), stagingBuffer.getBufferMemory(), 0, imageSize, 0, data);
            long destAddress = data.get(0);

            // Pixel direkt rüberkopieren!
            org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(pixels), destAddress, imageSize);
            VK10.vkUnmapMemory(context.getDevice(), stagingBuffer.getBufferMemory());

            createImageArray(tileSizeX, tileSizeY, layerCount, VK10.VK_FORMAT_R8G8B8A8_SRGB, VK10.VK_IMAGE_TILING_OPTIMAL,
                    VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, layerCount);

            copyBufferToImageLayers(commandBuffers, stagingBuffer.getBuffer(), textureImage, texWidth, texHeight, tileSizeX, tileSizeY, tilesX, layerCount);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, layerCount);
        }
        stagingBuffer.cleanup();
    }

    private void createTextureArray(VulkanCommandBuffers commandBuffers, String path, int layerCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            IntBuffer pChannels = stack.mallocInt(1);

            ByteBuffer fileBuffer = AssetManager.loadResource(path);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(fileBuffer, pWidth, pHeight, pChannels, STBImage.STBI_rgb_alpha);

            if (pixels == null) {
                throw new RuntimeException("Failed to load texture image array: " + path);
            }

            int texWidth = pWidth.get(0);
            int texHeight = pHeight.get(0);
            long imageSize = (long) texWidth * texHeight * 4;

            int tilesX = 16;
            int tilesY = 16;
            int tileSizeX = texWidth / tilesX;
            int tileSizeY = texHeight / tilesY;

            VulkanBuffer stagingBuffer = new VulkanBuffer(context, imageSize, VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);

            org.lwjgl.PointerBuffer data = stack.mallocPointer(1);
            VK10.vkMapMemory(context.getDevice(), stagingBuffer.getBufferMemory(), 0, imageSize, 0, data);
            long destAddress = data.get(0);
            org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(pixels), destAddress, imageSize);
            VK10.vkUnmapMemory(context.getDevice(), stagingBuffer.getBufferMemory());

            STBImage.stbi_image_free(pixels);

            createImageArray(tileSizeX, tileSizeY, layerCount, VK10.VK_FORMAT_R8G8B8A8_SRGB, VK10.VK_IMAGE_TILING_OPTIMAL,
                    VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK10.VK_IMAGE_USAGE_SAMPLED_BIT,
                    VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_UNDEFINED, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, layerCount);

            copyBufferToImageLayers(commandBuffers, stagingBuffer.getBuffer(), textureImage, texWidth, texHeight, tileSizeX, tileSizeY, tilesX, layerCount);

            transitionImageLayout(commandBuffers, textureImage, VK10.VK_FORMAT_R8G8B8A8_SRGB,
                    VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, layerCount);

            stagingBuffer.cleanup();
        }
    }

    private void createImageArray(int width, int height, int layerCount, int format, int tiling, int usage, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.calloc(stack);
            imageInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK10.VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width).height(height).depth(1);
            imageInfo.mipLevels(1);
            imageInfo.arrayLayers(layerCount); // Hier definieren wir das Array!
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

    private void transitionImageLayout(VulkanCommandBuffers commandBuffers, long image, int format, int oldLayout, int newLayout, int layerCount) {
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
            barrier.subresourceRange().layerCount(layerCount); // Alle 256 Layers überführen!

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

    private void copyBufferToImageLayers(VulkanCommandBuffers commandBuffers, long buffer, long image, int texWidth, int texHeight, int tileSizeX, int tileSizeY, int tilesX, int layerCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBuffer commandBuffer = commandBuffers.beginSingleTimeCommands();

            VkBufferImageCopy.Buffer regions = VkBufferImageCopy.calloc(layerCount, stack);
            for (int layer = 0; layer < layerCount; layer++) {
                int tx = layer % tilesX;
                int ty = layer / tilesX;

                VkBufferImageCopy region = regions.get(layer);
                // Hier sagen wir Vulkan, an welcher Pixel-Position es im Staging-Buffer anfangen soll zu lesen
                region.bufferOffset((ty * tileSizeY * texWidth + tx * tileSizeX) * 4L);
                region.bufferRowLength(texWidth);
                region.bufferImageHeight(texHeight);

                region.imageSubresource().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
                region.imageSubresource().mipLevel(0);
                region.imageSubresource().baseArrayLayer(layer); // Ab in diesen Layer!
                region.imageSubresource().layerCount(1);

                region.imageOffset().set(0, 0, 0);
                region.imageExtent().set(tileSizeX, tileSizeY, 1);
            }

            VK10.vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);

            commandBuffers.endSingleTimeCommands(commandBuffer);
        }
    }

    private void createTextureImageView(int layerCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.calloc(stack);
            viewInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(textureImage);
            viewInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D_ARRAY); // NEU: Dies ist ein Array!
            viewInfo.format(VK10.VK_FORMAT_R8G8B8A8_SRGB);
            viewInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(1);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(layerCount); // Alle Layers in der View!

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
            samplerInfo.addressModeU(VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT); // Jetzt können wir sicher REPEAT nutzen!
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(stack.longs(descriptorSetLayout));

            LongBuffer pDescriptorSet = stack.mallocLong(1);
            if (VK10.vkAllocateDescriptorSets(context.getDevice(), allocInfo, pDescriptorSet) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor set");
            }
            descriptorSet = pDescriptorSet.get(0);

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
            imageInfo.imageLayout(VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            imageInfo.imageView(textureImageView);
            imageInfo.sampler(textureSampler);

            VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc(1, stack);
            descriptorWrite.sType(VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
            descriptorWrite.dstSet(descriptorSet);
            descriptorWrite.dstBinding(0);
            descriptorWrite.dstArrayElement(0);
            descriptorWrite.descriptorType(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            descriptorWrite.descriptorCount(1);
            descriptorWrite.pImageInfo(imageInfo);

            VK10.vkUpdateDescriptorSets(context.getDevice(), descriptorWrite, null);
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
