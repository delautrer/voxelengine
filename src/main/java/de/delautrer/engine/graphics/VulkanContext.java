package de.delautrer.engine.graphics;

import de.delautrer.engine.window.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.glfw.GLFWVulkan;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class VulkanContext {

    private VkInstance instance;
    private long surface; // Non-dispatchable handles stay long
    private VkPhysicalDevice physicalDevice;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private VkQueue transferQueue;
    private int graphicsQueueFamily = -1;
    private int presentQueueFamily = -1;
    private int transferQueueFamily = -1;
    private Window window;

    public VulkanContext(Window window) {
        this.window = window;
        createInstance();
        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
    }

    private void createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8("Voxel Engine"));
            appInfo.applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8("No Engine"));
            appInfo.engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK10.VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);

            PointerBuffer requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            createInfo.ppEnabledExtensionNames(requiredExtensions);

            PointerBuffer pInstance = stack.mallocPointer(1);
            if (VK10.vkCreateInstance(createInfo, null, pInstance) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create Vulkan instance");
            }
            // Mache aus dem primitiven Pointer ein richtiges LWJGL Objekt
            instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            if (GLFWVulkan.glfwCreateWindowSurface(instance, window.getHandle(), null, pSurface) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create window surface");
            }
            surface = pSurface.get(0);
        }
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.mallocInt(1);
            VK10.vkEnumeratePhysicalDevices(instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer pPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            VK10.vkEnumeratePhysicalDevices(instance, deviceCount, pPhysicalDevices);

            for (int i = 0; i < pPhysicalDevices.capacity(); i++) {
                VkPhysicalDevice device = new VkPhysicalDevice(pPhysicalDevices.get(i), instance);
                if (isDeviceSuitable(device, stack)) {
                    physicalDevice = device;
                    break;
                }
            }

            if (physicalDevice == null) {
                throw new RuntimeException("Failed to find a suitable GPU");
            }
        }
    }

    private boolean isDeviceSuitable(VkPhysicalDevice device, MemoryStack stack) {
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

            if ((props.queueFlags() & VK10.VK_QUEUE_TRANSFER_BIT) != 0 && (props.queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) == 0) {
                transfer = i;
            }
        }

        if (transfer == -1) transfer = graphics;

        if (graphics != -1 && present != -1 && transfer != -1) {
            graphicsQueueFamily = graphics;
            presentQueueFamily = present;
            transferQueueFamily = transfer;

            VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            VK10.vkGetPhysicalDeviceFeatures(device, supportedFeatures);
            // Require non-solid fill mode (line drawing)
            if (!supportedFeatures.fillModeNonSolid()) {
                return false;
            }

            return true;
        }

        return false;
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int[] uniqueFamilies = transferQueueFamily != graphicsQueueFamily && transferQueueFamily != presentQueueFamily
                    ? new int[] {graphicsQueueFamily, presentQueueFamily, transferQueueFamily}
                    : (graphicsQueueFamily != presentQueueFamily
                    ? new int[] {graphicsQueueFamily, presentQueueFamily}
                    : new int[] {graphicsQueueFamily});

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(uniqueFamilies.length, stack);
            for (int i = 0; i < uniqueFamilies.length; i++) {
                queueCreateInfos.get(i).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfos.get(i).queueFamilyIndex(uniqueFamilies[i]);
                queueCreateInfos.get(i).pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc(stack);
            deviceFeatures.fillModeNonSolid(true);
            deviceFeatures.logicOp(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            createInfo.pEnabledFeatures(deviceFeatures);

            PointerBuffer extensions = stack.pointers(stack.UTF8(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME));
            createInfo.ppEnabledExtensionNames(extensions);

            PointerBuffer pDevice = stack.mallocPointer(1);
            if (VK10.vkCreateDevice(physicalDevice, createInfo, null, pDevice) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Failed to create logical device");
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

    public VkInstance getInstance() { return instance; }
    public long getSurface() { return surface; }
    public VkPhysicalDevice getPhysicalDevice() { return physicalDevice; }
    public VkDevice getDevice() { return device; }
    public VkQueue getGraphicsQueue() { return graphicsQueue; }
    public VkQueue getPresentQueue() { return presentQueue; }
    public int getGraphicsQueueFamily() { return graphicsQueueFamily; }
    public int getTransferQueueFamily() { return transferQueueFamily; }
    public Window getWindow() { return window; }

    public int findMemoryType(int typeFilter, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceMemoryProperties(physicalDevice, memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    public void cleanup() {
        VK10.vkDestroyDevice(device, null);
        KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        VK10.vkDestroyInstance(instance, null);
    }
}