package de.delautrer.engine.graphics;

import de.delautrer.engine.window.Window;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import org.lwjgl.glfw.GLFWVulkan;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.HashSet;
import java.util.Set;

public class VulkanContext implements IGraphicsContext {

    private VkInstance instance;
    private long surface;
    private VulkanDeviceManager deviceManager;
    private Window window;

    public VulkanContext(Window window) {
        this.window = window;
        createInstance();
        createSurface();
        deviceManager = new VulkanDeviceManager();
        deviceManager.pickPhysicalDevice(instance, surface);
        deviceManager.createLogicalDevice();
    }

    private void createInstance() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack);
            appInfo.sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO);
            appInfo.pApplicationName(stack.UTF8("Voxel Engine"));
            appInfo.applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.pEngineName(stack.UTF8("DelautrerEngine"));
            appInfo.engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
            appInfo.apiVersion(VK10.VK_API_VERSION_1_0);

            VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc(stack);
            createInfo.sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO);
            createInfo.pApplicationInfo(appInfo);

            PointerBuffer requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            createInfo.ppEnabledExtensionNames(requiredExtensions);
            createInfo.ppEnabledLayerNames(null);

            PointerBuffer pInstance = stack.mallocPointer(1);
            int err = VK10.vkCreateInstance(createInfo, null, pInstance);
            if (err != VK10.VK_SUCCESS) {
                throw new RuntimeException("Vulkan-Instanz konnte nicht erstellt werden: " + err);
            }
            instance = new VkInstance(pInstance.get(0), createInfo);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.mallocLong(1);
            if (GLFWVulkan.glfwCreateWindowSurface(instance, window.getHandle(), null, pSurface) != VK10.VK_SUCCESS) {
                throw new RuntimeException("Window Surface konnte nicht erstellt werden");
            }
            surface = pSurface.get(0);
        }
    }



    public VkInstance getInstance() { return instance; }
    public long getSurface() { return surface; }
    public VulkanDeviceManager getDeviceManager() { return deviceManager; }
    public VkPhysicalDevice getPhysicalDevice() { return deviceManager.getPhysicalDevice(); }
    public VkDevice getDevice() { return deviceManager.getDevice(); }
    public VkQueue getGraphicsQueue() { return deviceManager.getGraphicsQueue(); }
    public VkQueue getPresentQueue() { return deviceManager.getPresentQueue(); }
    public int getGraphicsQueueFamily() { return deviceManager.getGraphicsQueueFamily(); }
    public int getTransferQueueFamily() { return deviceManager.getTransferQueueFamily(); }
    public Window getWindow() { return window; }

    public int findMemoryType(int typeFilter, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            VK10.vkGetPhysicalDeviceMemoryProperties(deviceManager.getPhysicalDevice(), memProperties);

            for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
        }
        throw new RuntimeException("Passender Speichertyp wurde nicht gefunden");
    }

    @Override
    public void waitIdle() {
        if (deviceManager != null && deviceManager.getDevice() != null) {
            VK10.vkDeviceWaitIdle(deviceManager.getDevice());
        }
    }

    @Override
    public void cleanup() {
        if (deviceManager != null) deviceManager.cleanup();
        KHRSurface.vkDestroySurfaceKHR(instance, surface, null);
        VK10.vkDestroyInstance(instance, null);
    }
}