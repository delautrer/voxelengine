package de.delautrer.engine.window;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

public class Window {

    private final long handle;
    private boolean framebufferResized = false;
    private int width, height;

    public Window(int width, int height, String title) {
        this.width = width;
        this.height = height;

        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);

        handle = GLFW.glfwCreateWindow(width, height, title, 0, 0);
        if (handle == 0) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.IntBuffer fw = stack.mallocInt(1);
            java.nio.IntBuffer fh = stack.mallocInt(1);
            org.lwjgl.glfw.GLFW.glfwGetFramebufferSize(handle, fw, fh);
            this.width = fw.get(0);
            this.height = fh.get(0);
        }

        GLFW.glfwSetFramebufferSizeCallback(handle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            this.framebufferResized = true;
        });
    }

    public long getHandle() {
        return handle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setIcon(String assetPath) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            java.nio.IntBuffer w = stack.mallocInt(1);
            java.nio.IntBuffer h = stack.mallocInt(1);
            java.nio.IntBuffer comp = stack.mallocInt(1);

            java.nio.ByteBuffer fileBuffer;
            try {
                fileBuffer = de.delautrer.engine.utils.AssetManager.loadResource(assetPath);
            } catch (Exception e) {
                System.err.println("Failed to load icon resource: " + assetPath);
                return;
            }

            java.nio.ByteBuffer image = org.lwjgl.stb.STBImage.stbi_load_from_memory(fileBuffer, w, h, comp, 4);
            if (image == null) {
                System.err.println("Failed to parse icon image: " + org.lwjgl.stb.STBImage.stbi_failure_reason());
                return;
            }

            org.lwjgl.glfw.GLFWImage.Buffer iconBuffer = org.lwjgl.glfw.GLFWImage.malloc(1);
            org.lwjgl.glfw.GLFWImage icon = org.lwjgl.glfw.GLFWImage.malloc();
            icon.set(w.get(0), h.get(0), image);
            iconBuffer.put(0, icon);

            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("mac")) {
                org.lwjgl.glfw.GLFW.glfwSetWindowIcon(handle, iconBuffer);
            }

            icon.free();
            iconBuffer.free();
            org.lwjgl.stb.STBImage.stbi_image_free(image);
        }
    }

    public void disableCursor() {
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    public void enableCursor() {
        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public boolean isFramebufferResized() {
        return framebufferResized;
    }

    public void setFramebufferResized(boolean resized) {
        this.framebufferResized = resized;
    }

    public void cleanup() {
        org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(handle);
        org.lwjgl.glfw.GLFW.glfwDestroyWindow(handle);

        org.lwjgl.glfw.GLFWErrorCallback callback = org.lwjgl.glfw.GLFW.glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }

        org.lwjgl.glfw.GLFW.glfwTerminate();
    }
}