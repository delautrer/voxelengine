package de.delautrer.engine.graphics;

import de.delautrer.engine.graphics.systems.*;
import de.delautrer.engine.window.Window;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class VulkanRenderer {
    private final VulkanContext context;
    private VulkanSwapchain swapchain;
    private VulkanDepthBuffer depthBuffer;
    private VulkanRenderPass renderPass;
    private VulkanFramebuffers framebuffers;
    private VulkanCommandBuffers commandBuffers;
    private VulkanSync sync;

    // Das neue Herzstück: Unsere Sub-Renderer Liste
    private final List<IRenderSystem> renderSystems = new ArrayList<>();
    private SkyRenderSystem skyRenderSystem;
    private TerrainRenderSystem terrainSystem;
    private CloudRenderSystem cloudSystem;
    private HighlightRenderSystem highlightSystem;
    private UIRenderSystem uiSystem;

    private int currentFrame = 0;

    public VulkanRenderer(VulkanContext context, Window window) {
        this.context = context;
        this.swapchain = new VulkanSwapchain(context);
        this.depthBuffer = new VulkanDepthBuffer(context, swapchain);
        this.renderPass = new VulkanRenderPass(context, swapchain);

        this.framebuffers = new VulkanFramebuffers(context, swapchain, renderPass, depthBuffer);
        this.commandBuffers = new VulkanCommandBuffers(context, framebuffers);
        this.sync = new VulkanSync(context, swapchain);

        // Render Systeme registrieren! Die Reihenfolge (Terrain -> Highlight -> UI) bestimmt den Depth-Test/Overlap
        skyRenderSystem = new SkyRenderSystem(context, swapchain, renderPass);
        terrainSystem = new TerrainRenderSystem(context, swapchain, renderPass);
        cloudSystem = new CloudRenderSystem(context, swapchain, renderPass);
        highlightSystem = new HighlightRenderSystem(context, swapchain, renderPass);
        uiSystem = new UIRenderSystem(context, swapchain, renderPass);

        renderSystems.add(skyRenderSystem);
        renderSystems.add(terrainSystem);
        renderSystems.add(cloudSystem);
        renderSystems.add(highlightSystem);
        renderSystems.add(uiSystem);
    }

    public boolean render(RenderPacket packet) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long inFlightFence = sync.getInFlightFence(currentFrame);
            long imageAvailableSemaphore = sync.getImageAvailableSemaphore(currentFrame);

            VK10.vkWaitForFences(context.getDevice(), inFlightFence, true, Long.MAX_VALUE);
            IntBuffer pImageIndex = stack.mallocInt(1);
            int acquireResult = KHRSwapchain.vkAcquireNextImageKHR(context.getDevice(), swapchain.getSwapchain(), Long.MAX_VALUE, imageAvailableSemaphore, 0, pImageIndex);

            if (acquireResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR) {
                return false;
            } else if (acquireResult != VK10.VK_SUCCESS && acquireResult != KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                throw new RuntimeException("Failed to acquire swapchain image");
            }

            int imageIndex = pImageIndex.get(0);
            VK10.vkResetFences(context.getDevice(), inFlightFence);

            /*VkCommandBuffer cmd = commandBuffers.beginRecording(imageIndex, swapchain, renderPass, framebuffers, packet.skyR, packet.skyG, packet.skyB);
            for (IRenderSystem system : renderSystems) {
                system.render(cmd, packet);
            }
            commandBuffers.endRecording(cmd);*/

            VkCommandBuffer cmd = commandBuffers.beginRecording(imageIndex, swapchain, renderPass, framebuffers, packet.skyR, packet.skyG, packet.skyB);
            //VkCommandBuffer cmd = commandBuffers.beginRecording(imageIndex, swapchain, renderPass, framebuffers, clearR, clearG, clearB);

            org.lwjgl.vulkan.VkViewport.Buffer viewport = org.lwjgl.vulkan.VkViewport.calloc(1, stack)
                    .x(0.0f)
                    .y(0.0f)
                    .width((float) swapchain.getExtent().width())
                    .height((float) swapchain.getExtent().height())
                    .minDepth(0.0f)
                    .maxDepth(1.0f);
            VK10.vkCmdSetViewport(cmd, 0, viewport);

            org.lwjgl.vulkan.VkRect2D.Buffer scissor = org.lwjgl.vulkan.VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent(swapchain.getExtent());
            VK10.vkCmdSetScissor(cmd, 0, scissor);

            for (IRenderSystem system : renderSystems) {
                system.render(cmd, packet);
            }
            commandBuffers.endRecording(cmd);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphore))
                    .pWaitDstStageMask(stack.ints(VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(cmd.address()))
                    .pSignalSemaphores(stack.longs(sync.getRenderFinishedSemaphore(imageIndex)));

            VK10.vkQueueSubmit(context.getGraphicsQueue(), submitInfo, inFlightFence);

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(stack.longs(sync.getRenderFinishedSemaphore(imageIndex)))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(swapchain.getSwapchain()))
                    .pImageIndices(pImageIndex);

            int presentResult = KHRSwapchain.vkQueuePresentKHR(context.getPresentQueue(), presentInfo);

            if (presentResult == KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR || presentResult == KHRSwapchain.VK_SUBOPTIMAL_KHR) {
                return false;
            } else if (presentResult != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to present swapchain image");
            }

            currentFrame = (currentFrame + 1) % VulkanSync.MAX_FRAMES_IN_FLIGHT;
            return true;
        }
    }

    public void recreate(Window window) {
        int[] width = new int[1];
        int[] height = new int[1];
        org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(window.getHandle(), width, height);
        while (width[0] == 0 || height[0] == 0) {
            org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(window.getHandle(), width, height);
            org.lwjgl.glfw.GLFW.glfwWaitEvents();
        }

        VK10.vkDeviceWaitIdle(context.getDevice());
        cleanupSwapchainOnly();

        this.swapchain = new VulkanSwapchain(context);
        this.depthBuffer = new VulkanDepthBuffer(context, swapchain);
        this.framebuffers = new VulkanFramebuffers(context, swapchain, renderPass, depthBuffer);
        this.commandBuffers = new VulkanCommandBuffers(context, framebuffers);
        this.sync = new VulkanSync(context, swapchain);
        renderSystems.clear();

        skyRenderSystem = new SkyRenderSystem(context, swapchain, renderPass);
        terrainSystem = new TerrainRenderSystem(context, swapchain, renderPass);
        cloudSystem = new CloudRenderSystem(context, swapchain, renderPass);
        highlightSystem = new HighlightRenderSystem(context, swapchain, renderPass);
        uiSystem = new UIRenderSystem(context, swapchain, renderPass);

        renderSystems.add(skyRenderSystem);
        renderSystems.add(terrainSystem);
        renderSystems.add(cloudSystem);
        renderSystems.add(highlightSystem);
        renderSystems.add(uiSystem);
    }

    private void cleanupSwapchainOnly() {
        sync.cleanup();
        commandBuffers.cleanup();
        framebuffers.cleanup();
        depthBuffer.cleanup();
        swapchain.cleanup();
        for (IRenderSystem system : renderSystems) {
            system.cleanup();
        }
    }

    public void cleanup() {
        cleanupSwapchainOnly();
        renderPass.cleanup();
    }

    public long getGraphicsLayout() { return terrainSystem.getDescriptorSetLayout(); }
    public long getUiLayout() { return uiSystem.getDescriptorSetLayout(); }

    public VulkanSwapchain getSwapchain() { return swapchain; }
    public VulkanCommandBuffers getCommandBuffers() { return commandBuffers; }
    public int getWidth() { return swapchain.getExtent().width(); }
    public int getHeight() { return swapchain.getExtent().height(); }

    private float clearR = 0.5f, clearG = 0.7f, clearB = 1.0f; // Standard-Himmel

    public void setClearColor(float r, float g, float b) {
        this.clearR = r;
        this.clearG = g;
        this.clearB = b;
    }
}