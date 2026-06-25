package de.delautrer.engine.graphics.vulkan.core;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;

public class VulkanCommandBuffers {

    private final VulkanContext context;
    private long commandPool;
    private VkCommandBuffer[] commandBuffers;

    public VulkanCommandBuffers(VulkanContext context) {
        this.context = context;
        createCommandPool();
        allocateCommandBuffers(de.delautrer.engine.graphics.vulkan.core.VulkanSync.MAX_FRAMES_IN_FLIGHT);
    }

    private void createCommandPool() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(0);
            poolInfo.flags(VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer pCommandPool = stack.mallocLong(1);
            if (VK10.vkCreateCommandPool(context.getDevice(), poolInfo, null, pCommandPool) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create command pool");
            }
            commandPool = pCommandPool.get(0);
        }
    }

    private void allocateCommandBuffers(int count) {
        commandBuffers = new VkCommandBuffer[count];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(commandPool);
            allocInfo.level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffers.length);

            org.lwjgl.PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffers.length);
            if (VK10.vkAllocateCommandBuffers(context.getDevice(), allocInfo, pCommandBuffers) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers");
            }

            for (int i = 0; i < commandBuffers.length; i++) {
                commandBuffers[i] = new VkCommandBuffer(pCommandBuffers.get(i), context.getDevice());
            }
        }
    }

    public VkCommandBuffer beginRecording(int frameIndex, int imageIndex, VulkanSwapchain swapchain, VulkanRenderPass renderPass,
            VulkanFramebuffers framebuffers, float skyR, float skyG, float skyB) {
        VkCommandBuffer cmd = commandBuffers[frameIndex];
        VK10.vkResetCommandBuffer(cmd, 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            if (VK10.vkBeginCommandBuffer(cmd, beginInfo) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer");
            }

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc(stack);
            renderPassInfo.sType(VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);
            renderPassInfo.renderPass(renderPass.getHandle());
            renderPassInfo.framebuffer(framebuffers.getFramebuffers()[imageIndex]);
            renderPassInfo.renderArea().offset().set(0, 0);
            renderPassInfo.renderArea().extent(swapchain.getExtent());

            VkClearValue.Buffer clearValues = VkClearValue.calloc(2, stack);
            clearValues.get(0).color().float32(stack.floats(skyR, skyG, skyB, 1.0f));
            clearValues.get(1).depthStencil().set(1.0f, 0);
            renderPassInfo.pClearValues(clearValues);

            VK10.vkCmdBeginRenderPass(cmd, renderPassInfo, VK10.VK_SUBPASS_CONTENTS_INLINE);

            VkViewport.Buffer viewport = VkViewport.calloc(1, stack);
            viewport.x(0.0f);
            viewport.y(0.0f);
            viewport.width((float) swapchain.getExtent().width());
            viewport.height((float) swapchain.getExtent().height());
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);
            VK10.vkCmdSetViewport(cmd, 0, viewport);

            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset(VkOffset2D.calloc(stack).set(0, 0));
            scissor.extent(swapchain.getExtent());
            VK10.vkCmdSetScissor(cmd, 0, scissor);

            return cmd;
        }
    }

    public void endRenderPass(VkCommandBuffer cmd) {
        VK10.vkCmdEndRenderPass(cmd);
    }

    public void endRecording(VkCommandBuffer cmd) {
        if (VK10.vkEndCommandBuffer(cmd) != VK10.VK_SUCCESS) {
            throw new RuntimeException("Failed to record command buffer");
        }
    }

    public VkCommandBuffer beginSingleTimeCommands() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(commandPool);
            allocInfo.commandBufferCount(1);

            org.lwjgl.PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            VK10.vkAllocateCommandBuffers(context.getDevice(), allocInfo, pCommandBuffer);

            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), context.getDevice());

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
            beginInfo.sType(VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            VK10.vkBeginCommandBuffer(commandBuffer, beginInfo);

            return commandBuffer;
        }
    }

    public void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VK10.vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            VK10.vkQueueSubmit(context.getGraphicsQueue(), submitInfo, VK10.VK_NULL_HANDLE);
            VK10.vkQueueWaitIdle(context.getGraphicsQueue());

            VK10.vkFreeCommandBuffers(context.getDevice(), commandPool, commandBuffer);
        }
    }

    public long getCommandPool() {
        return commandPool;
    }

    public VkCommandBuffer[] getCommandBuffers() {
        return commandBuffers;
    }

    public void cleanup() {
        VK10.vkDestroyCommandPool(context.getDevice(), commandPool, null);
    }
}
