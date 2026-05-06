package de.delautrer.engine.graphics.vulkan.core;
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

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

public class VulkanRenderPass {

    private final VulkanContext context;
    private final VulkanSwapchain swapchain;
    private long renderPass;

    public VulkanRenderPass(VulkanContext context, VulkanSwapchain swapchain) {
        this.context = context;
        this.swapchain = swapchain;
        createRenderPass();
    }

    private void createRenderPass() {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            org.lwjgl.vulkan.VkAttachmentDescription.Buffer attachments = org.lwjgl.vulkan.VkAttachmentDescription.calloc(2, stack);

            org.lwjgl.vulkan.VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(swapchain.getImageFormat());
            colorAttachment.samples(org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT);
            colorAttachment.loadOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            org.lwjgl.vulkan.VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(org.lwjgl.vulkan.VK10.VK_FORMAT_D32_SFLOAT);
            depthAttachment.samples(org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT);
            depthAttachment.loadOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilStoreOp(org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            org.lwjgl.vulkan.VkAttachmentReference.Buffer colorAttachmentRef = org.lwjgl.vulkan.VkAttachmentReference.calloc(1, stack);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            org.lwjgl.vulkan.VkAttachmentReference depthAttachmentRef = org.lwjgl.vulkan.VkAttachmentReference.calloc(stack);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            org.lwjgl.vulkan.VkSubpassDescription.Buffer subpass = org.lwjgl.vulkan.VkSubpassDescription.calloc(1, stack);
            subpass.pipelineBindPoint(org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(colorAttachmentRef);
            subpass.pDepthStencilAttachment(depthAttachmentRef);

            org.lwjgl.vulkan.VkSubpassDependency.Buffer dependency = org.lwjgl.vulkan.VkSubpassDependency.calloc(1, stack);
            dependency.srcSubpass(org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT | org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT);
            dependency.dstAccessMask(org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT | org.lwjgl.vulkan.VK10.VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

            org.lwjgl.vulkan.VkRenderPassCreateInfo renderPassInfo = org.lwjgl.vulkan.VkRenderPassCreateInfo.calloc(stack);
            renderPassInfo.sType(org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            java.nio.LongBuffer pRenderPass = stack.mallocLong(1);
            if (org.lwjgl.vulkan.VK10.vkCreateRenderPass(context.getDevice(), renderPassInfo, null, pRenderPass) != org.lwjgl.vulkan.VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create render pass");
            }
            renderPass = pRenderPass.get(0);
        }
    }

    public long getHandle() {
        return renderPass;
    }

    public void cleanup() {
        VK10.vkDestroyRenderPass(context.getDevice(), renderPass, null);
    }
}
