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

        // CRITICAL: Breite und Höhe bei Resize aktualisieren!
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