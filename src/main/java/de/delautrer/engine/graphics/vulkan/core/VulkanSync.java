package de.delautrer.engine.graphics.vulkan.core;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;


public class VulkanSync {
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    private final VulkanContext context;
    private long[] imageAvailableSemaphores;
    private long[] renderFinishedSemaphores;
    private long[] inFlightFences;

    public VulkanSync(VulkanContext context, VulkanSwapchain swapchain) {
        this.context = context;
        createSyncObjects(swapchain.getImageCount());
    }

    public void createSyncObjects(int imageCount) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK10.VK_FENCE_CREATE_SIGNALED_BIT);

            imageAvailableSemaphores = new long[MAX_FRAMES_IN_FLIGHT];
            renderFinishedSemaphores = new long[imageCount]; // Abhängig von Bildern!
            inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];

            LongBuffer pSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
                VK10.vkCreateSemaphore(context.getDevice(), semaphoreInfo, null, pSemaphore);
                imageAvailableSemaphores[i] = pSemaphore.get(0);
                VK10.vkCreateFence(context.getDevice(), fenceInfo, null, pFence);
                inFlightFences[i] = pFence.get(0);
            }

            for (int i = 0; i < imageCount; i++) {
                VK10.vkCreateSemaphore(context.getDevice(), semaphoreInfo, null, pSemaphore);
                renderFinishedSemaphores[i] = pSemaphore.get(0);
            }
        }
    }

    public long getImageAvailableSemaphore(int frame) { return imageAvailableSemaphores[frame]; }
    public long getRenderFinishedSemaphore(int index) { return renderFinishedSemaphores[index]; }
    public long getInFlightFence(int frame) { return inFlightFences[frame]; }

    public void cleanup() {
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            VK10.vkDestroySemaphore(context.getDevice(), imageAvailableSemaphores[i], null);
            VK10.vkDestroyFence(context.getDevice(), inFlightFences[i], null);
        }
        for (long semaphore : renderFinishedSemaphores) {
            VK10.vkDestroySemaphore(context.getDevice(), semaphore, null);
        }
    }
}
