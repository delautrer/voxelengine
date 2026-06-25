package de.delautrer.engine.graphics.vulkan.core;

import de.delautrer.engine.window.Window;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import static org.lwjgl.system.MemoryUtil.NULL;

public class VulkanSwapchain {

    private final VulkanContext context;
    private long swapchain;
    private long[] images;
    private long[] imageViews;
    private int imageFormat;
    private VkExtent2D extent;

    public VulkanSwapchain(VulkanContext context) {
        this.context = context;
        createSwapchain();
        createImageViews();
    }

    private void createSwapchain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(context.getPhysicalDevice(), context.getSurface(),
                    capabilities);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(stack);
            int presentMode = chooseSwapPresentMode(stack);
            VkExtent2D swapExtent = chooseSwapExtent(capabilities);

            int imageCount = capabilities.minImageCount() + 1;
            if (capabilities.maxImageCount() > 0 && imageCount > capabilities.maxImageCount()) {
                imageCount = capabilities.maxImageCount();
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc(stack);
            createInfo.sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(context.getSurface());
            createInfo.minImageCount(imageCount);
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(swapExtent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK10.VK_IMAGE_USAGE_TRANSFER_SRC_BIT);

            createInfo.imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            createInfo.preTransform(capabilities.currentTransform());
            createInfo.compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);
            createInfo.oldSwapchain(NULL);

            LongBuffer pSwapchain = stack.longs(VK10.VK_NULL_HANDLE);
            if (KHRSwapchain.vkCreateSwapchainKHR(context.getDevice(), createInfo, null,
                    pSwapchain) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create swapchain");
            }
            swapchain = pSwapchain.get(0);

            IntBuffer pImageCount = stack.mallocInt(1);
            KHRSwapchain.vkGetSwapchainImagesKHR(context.getDevice(), swapchain, pImageCount, null);
            LongBuffer pSwapchainImages = stack.mallocLong(pImageCount.get(0));
            KHRSwapchain.vkGetSwapchainImagesKHR(context.getDevice(), swapchain, pImageCount, pSwapchainImages);

            images = new long[pSwapchainImages.capacity()];
            for (int i = 0; i < images.length; i++) {
                images[i] = pSwapchainImages.get(i);
            }

            imageFormat = surfaceFormat.format();
            extent = VkExtent2D.create().set(swapExtent);

        }
    }

    private void createImageViews() {
        imageViews = new long[images.length];

        try (MemoryStack stack = MemoryStack.stackPush()) {
            for (int i = 0; i < images.length; i++) {
                VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc(stack);
                createInfo.sType(VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
                createInfo.image(images[i]);
                createInfo.viewType(VK10.VK_IMAGE_VIEW_TYPE_2D);
                createInfo.format(imageFormat);

                createInfo.components().r(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().g(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().b(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);
                createInfo.components().a(VK10.VK_COMPONENT_SWIZZLE_IDENTITY);

                createInfo.subresourceRange().aspectMask(VK10.VK_IMAGE_ASPECT_COLOR_BIT);
                createInfo.subresourceRange().baseMipLevel(0);
                createInfo.subresourceRange().levelCount(1);
                createInfo.subresourceRange().baseArrayLayer(0);
                createInfo.subresourceRange().layerCount(1);

                LongBuffer pImageView = stack.mallocLong(1);
                if (VK10.vkCreateImageView(context.getDevice(), createInfo, null, pImageView) != VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create image views");
                }
                imageViews[i] = pImageView.get(0);
            }
        }
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(MemoryStack stack) {
        IntBuffer formatCount = stack.ints(0);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(context.getPhysicalDevice(), context.getSurface(), formatCount,
                null);
        VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc(formatCount.get(0), stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(context.getPhysicalDevice(), context.getSurface(), formatCount,
                formats);

        for (int i = 0; i < formats.capacity(); i++) {
            VkSurfaceFormatKHR format = formats.get(i);
            if (format.format() == VK10.VK_FORMAT_B8G8R8A8_SRGB
                    && format.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                return format;
            }
        }
        return formats.get(0);
    }

    private int chooseSwapPresentMode(MemoryStack stack) {
        IntBuffer presentModeCount = stack.ints(0);
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(context.getPhysicalDevice(), context.getSurface(),
                presentModeCount, null);
        IntBuffer presentModes = stack.mallocInt(presentModeCount.get(0));
        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(context.getPhysicalDevice(), context.getSurface(),
                presentModeCount, presentModes);

        for (int i = 0; i < presentModes.capacity(); i++) {
            if (presentModes.get(i) == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR) {
                return presentModes.get(i);
            }
        }
        return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != 0xFFFFFFFF) {
            return capabilities.currentExtent();
        }

        Window window = context.getWindow();
        int[] width = new int[1];
        int[] height = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(window.getHandle(), width, height);

        VkExtent2D actualExtent = VkExtent2D.malloc().set(width[0], height[0]);
        actualExtent.width(Math.clamp(actualExtent.width(), capabilities.minImageExtent().width(),
                capabilities.maxImageExtent().width()));
        actualExtent.height(Math.clamp(actualExtent.height(), capabilities.minImageExtent().height(),
                capabilities.maxImageExtent().height()));

        return actualExtent;
    }

    public long getSwapchain() {
        return swapchain;
    }

    public int getImageFormat() {
        return imageFormat;
    }

    public VkExtent2D getExtent() {
        return extent;
    }

    public long[] getImages() {
        return images;
    }

    public long[] getImageViews() {
        return imageViews;
    }

    public int getImageCount() {
        return images.length;
    }

    public void cleanup() {
        if (imageViews != null) {
            for (long imageView : imageViews) {
                VK10.vkDestroyImageView(context.getDevice(), imageView, null);
            }
        }
        KHRSwapchain.vkDestroySwapchainKHR(context.getDevice(), swapchain, null);
        /*
         * if (extent != null) {
         * extent.free();
         * }
         */
    }
}
