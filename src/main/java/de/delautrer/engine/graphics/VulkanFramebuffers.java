package de.delautrer.engine.graphics;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;

import java.nio.LongBuffer;

public class VulkanFramebuffers {

    private final VulkanContext context;
    private final long[] framebuffers;

    public VulkanFramebuffers(VulkanContext context, VulkanSwapchain swapchain, VulkanRenderPass renderPass, VulkanDepthBuffer depthBuffer) {
        this.context = context;
        long[] imageViews = swapchain.getImageViews();
        framebuffers = new long[imageViews.length];

        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.LongBuffer attachments = stack.mallocLong(2);
            java.nio.LongBuffer pFramebuffer = stack.mallocLong(1);

            for (int i = 0; i < imageViews.length; i++) {
                attachments.put(0, imageViews[i]);
                attachments.put(1, depthBuffer.getDepthImageView());

                org.lwjgl.vulkan.VkFramebufferCreateInfo framebufferInfo = org.lwjgl.vulkan.VkFramebufferCreateInfo.calloc(stack);
                framebufferInfo.sType(org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
                framebufferInfo.renderPass(renderPass.getHandle());
                framebufferInfo.pAttachments(attachments);
                framebufferInfo.width(swapchain.getExtent().width());
                framebufferInfo.height(swapchain.getExtent().height());
                framebufferInfo.layers(1);

                if (org.lwjgl.vulkan.VK10.vkCreateFramebuffer(context.getDevice(), framebufferInfo, null, pFramebuffer) != org.lwjgl.vulkan.VK10.VK_SUCCESS) {
                    throw new RuntimeException("Failed to create framebuffer");
                }
                framebuffers[i] = pFramebuffer.get(0);
            }
        }
    }

    public long[] getFramebuffers() {
        return framebuffers;
    }

    public void cleanup() {
        for (long framebuffer : framebuffers) {
            VK10.vkDestroyFramebuffer(context.getDevice(), framebuffer, null);
        }
    }
}