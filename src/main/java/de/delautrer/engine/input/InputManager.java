package de.delautrer.engine.input;

import org.lwjgl.glfw.GLFW;
import java.util.HashMap;
import java.util.Map;

public class InputManager {
    private final long windowHandle;

    private final Map<String, Integer> keyBindings = new HashMap<>();
    private final Map<String, Integer> mouseBindings = new HashMap<>();
    private final Map<String, Boolean> previousActionStates = new HashMap<>();

    private float mouseX, mouseY;
    private int windowWidth, windowHeight;

    // NEU: Cursor Handles
    private final long normalCursor;
    private final long handCursor;

    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;

        // Cursors vom Betriebssystem laden
        this.normalCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
        this.handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);

        setupDefaultBindings();
    }

    private void setupDefaultBindings() {
        keyBindings.put("MOVE_FORWARD", GLFW.GLFW_KEY_W);
        keyBindings.put("MOVE_BACKWARD", GLFW.GLFW_KEY_S);
        keyBindings.put("MOVE_LEFT", GLFW.GLFW_KEY_A);
        keyBindings.put("MOVE_RIGHT", GLFW.GLFW_KEY_D);
        keyBindings.put("JUMP", GLFW.GLFW_KEY_SPACE);
        keyBindings.put("SNEAK", GLFW.GLFW_KEY_LEFT_SHIFT);

        keyBindings.put("INVENTORY", GLFW.GLFW_KEY_E);
        for (int i = 0; i < 9; i++) {
            keyBindings.put("SLOT_" + (i + 1), GLFW.GLFW_KEY_1 + i);
        }

        mouseBindings.put("INTERACT_BREAK", GLFW.GLFW_MOUSE_BUTTON_LEFT);
        mouseBindings.put("INTERACT_PLACE", GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    }

    public void update() {
        for (String action : keyBindings.keySet()) previousActionStates.put(action, isActionActive(action));
        for (String action : mouseBindings.keySet()) previousActionStates.put(action, isActionActive(action));

        double[] xpos = new double[1];
        double[] ypos = new double[1];
        GLFW.glfwGetCursorPos(windowHandle, xpos, ypos);
        mouseX = (float) xpos[0];
        mouseY = (float) ypos[0];

        int[] w = new int[1];
        int[] h = new int[1];
        GLFW.glfwGetWindowSize(windowHandle, w, h);
        windowWidth = w[0];
        windowHeight = h[0];
    }

    // NEU: Ändert das Aussehen des Mauszeigers
    public void setCursorHover(boolean isHovering) {
        GLFW.glfwSetCursor(windowHandle, isHovering ? handCursor : normalCursor);
    }

    public boolean isActionActive(String action) {
        if (keyBindings.containsKey(action)) return GLFW.glfwGetKey(windowHandle, keyBindings.get(action)) == GLFW.GLFW_PRESS;
        if (mouseBindings.containsKey(action)) return GLFW.glfwGetMouseButton(windowHandle, mouseBindings.get(action)) == GLFW.GLFW_PRESS;
        return false;
    }

    public boolean isActionJustPressed(String action) {
        return isActionActive(action) && !previousActionStates.getOrDefault(action, false);
    }

    public float getMouseX() { return mouseX; }
    public float getMouseY() { return mouseY; }
    public int getWindowWidth() { return windowWidth; }
    public int getWindowHeight() { return windowHeight; }
}