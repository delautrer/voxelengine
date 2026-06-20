package de.delautrer.engine.graphics.vulkan.core;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

public class VulkanDeviceManager {

    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private VkQueue transferQueue;

    private int graphicsQueueFamily = -1;
    private int presentQueueFamily = -1;
    private int transferQueueFamily = -1;

    public void pickPhysicalDevice(VkInstance instance, long surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);
            VK10.vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Keine GPU mit Vulkan-Support gefunden");
            }

            PointerBuffer pPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            VK10.vkEnumeratePhysicalDevices(instance, deviceCount, pPhysicalDevices);

            VkPhysicalDevice selected = null;
            int bestScore = -1;

            for (int i = 0; i < pPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice dev = new VkPhysicalDevice(pPhysicalDevices.get(i), instance);
                VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
                VK10.vkGetPhysicalDeviceProperties(dev, props);

                if (isDeviceSuitable(dev, surface, stack)) {
                    int score = 0;
                    if (props.deviceType() == VK10.VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU)
                        score += 1000;

                    if (score > bestScore) {
                        bestScore = score;
                        selected = dev;
                    }
                }
            }

            if (selected == null)
                throw new RuntimeException("Keine passende GPU gefunden!");
            physicalDevice = selected;
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice device, long surface, MemoryStack stack) {
        IntBuffer queueFamilyCount = stack.mallocInt(1);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
        VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount.get(0), stack);
        VK10.vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);

        int graphics = -1;
        int present = -1;
        int transfer = -1;

        for (int i = 0; i < queueFamilies.capacity(); i++) {
            VkQueueFamilyProperties props = queueFamilies.get(i);

            if (graphics == -1 && (props.queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) != 0) {
                graphics = i;
            }

            IntBuffer presentSupport = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(device, i, surface, presentSupport);
            if (present == -1 && presentSupport.get(0) == VK10.VK_TRUE) {
                present = i;
            }

            if ((props.queueFlags() & VK10.VK_QUEUE_TRANSFER_BIT) != 0
                    && (props.queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) == 0) {
                transfer = i;
            }
        }

        if (transfer == -1)
            transfer = graphics;

        if (graphics != -1 && present != -1) {
            graphicsQueueFamily = graphics;
            presentQueueFamily = present;
            transferQueueFamily = transfer;

            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            VK10.vkGetPhysicalDeviceFeatures(device, supportedFeatures);
            return supportedFeatures.fillModeNonSolid();
        }

        return false;
    }

    public void createLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Set<Integer> uniqueIndices = new HashSet<>();
            uniqueIndices.add(graphicsQueueFamily);
            uniqueIndices.add(presentQueueFamily);
            uniqueIndices.add(transferQueueFamily);

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueIndices.size(),
                    stack);
            FloatBuffer priorities = stack.floats(1.0f);

            int i = 0;
            for (int familyIndex : uniqueIndices) {
                queueCreateInfos.get(i++)
                        .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(familyIndex)
                        .pQueuePriorities(priorities);
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            VK10.vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);
            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            VK10.vkGetPhysicalDeviceFeatures(physicalDevice, supportedFeatures);
            deviceFeatures.fillModeNonSolid(true);
            deviceFeatures.logicOp(supportedFeatures.logicOp());

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);

            PointerBuffer extensions = stack.pointers(stack.UTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            createInfo.ppEnabledExtensionNames(extensions);
            createInfo.ppEnabledLayerNames(null);

            PointerBuffer pDevice = stack.mallocPointer(1);
            int result = VK10.vkCreateDevice(physicalDevice, createInfo, null, pDevice);
            if (result != VK10.VK_SUCCESS) {
                throw new RuntimeException("Logical Device konnte nicht erstellt werden: Fehler " + result);
            }
            device = new VkDevice(pDevice.get(0), physicalDevice, createInfo);

            PointerBuffer pQueue = stack.mallocPointer(1);
            VK10.vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQueue);
            graphicsQueue = new VkQueue(pQueue.get(0), device);

            VK10.vkGetDeviceQueue(device, presentQueueFamily, 0, pQueue);
            presentQueue = new VkQueue(pQueue.get(0), device);

            VK10.vkGetDeviceQueue(device, transferQueueFamily, 0, pQueue);
            transferQueue = new VkQueue(pQueue.get(0), device);
        }
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkDevice getDevice() {
        return device;
    }

    public VkQueue getGraphicsQueue() {
        return graphicsQueue;
    }

    public VkQueue getPresentQueue() {
        return presentQueue;
    }

    public VkQueue getTransferQueue() {
        return transferQueue;
    }

    public int getGraphicsQueueFamily() {
        return graphicsQueueFamily;
    }

    public int getTransferQueueFamily() {
        return transferQueueFamily;
    }

    public int getPresentQueueFamily() {
        return presentQueueFamily;
    }

    public void cleanup() {
        if (device != null) {
            VK10.vkDestroyDevice(device, null);
        }
    }
}
