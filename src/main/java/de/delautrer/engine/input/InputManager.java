package de.delautrer.engine.input;

import org.lwjgl.glfw.GLFW;
import de.delautrer.game.settings.SettingsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InputManager {
    private final long windowHandle;

    private final List<Character> typedChars = new ArrayList<>();
    private boolean typingMode = false;

    private final Map<String, Integer> keyBindings = new HashMap<>();
    private final Map<String, Integer> mouseBindings = new HashMap<>();
    private final Map<String, Boolean> previousActionStates = new HashMap<>();

    private int lastKeyPressed = -1;
    private double scrollY = 0;
    private float mouseX, mouseY;
    private int windowWidth, windowHeight;

    private final long normalCursor;
    private final long handCursor;

    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;

        this.normalCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR);
        this.handCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_HAND_CURSOR);

        // Wir rufen direkt am Start unsere dynamische Lade-Funktion auf
        reloadBindings();

        GLFW.glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            scrollY = yoffset;
        });

        GLFW.glfwSetCharCallback(windowHandle, (window, codepoint) -> {
            typedChars.add((char) codepoint);
        });

        GLFW.glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (action == GLFW.GLFW_PRESS) {
                lastKeyPressed = key;
            }
        });
    }

    public final void reloadBindings() {
        keyBindings.clear();
        mouseBindings.clear();

        // 1. Feste System- & UI-Tasten (Können später natürlich auch ins Menü wandern)
        keyBindings.put("PAUSE", GLFW.GLFW_KEY_ESCAPE);
        keyBindings.put("UI_BACKSPACE", GLFW.GLFW_KEY_BACKSPACE);
        keyBindings.put("MOD_ALT", GLFW.GLFW_KEY_LEFT_ALT);
        keyBindings.put("CHAT_SEND", GLFW.GLFW_KEY_ENTER);
        keyBindings.put("CHAT_OPEN_SLASH", GLFW.GLFW_KEY_SLASH);
        keyBindings.put("UI_UP", GLFW.GLFW_KEY_UP);
        keyBindings.put("UI_DOWN", GLFW.GLFW_KEY_DOWN);
        keyBindings.put("UI_TAB", GLFW.GLFW_KEY_TAB);

        for (int i = 0; i < 9; i++) {
            keyBindings.put("SLOT_" + (i + 1), GLFW.GLFW_KEY_1 + i);
        }

        // 2. DYNAMISCHE TASTEN: Wir holen uns die Config aus den Settings!
        Map<String, Integer> userBinds = SettingsManager.get().keyBinds;
        for (Map.Entry<String, Integer> entry : userBinds.entrySet()) {
            keyBindings.put(entry.getKey(), entry.getValue());
        }

        // 3. Maus-Bindings
        mouseBindings.put("INTERACT_BREAK", GLFW.GLFW_MOUSE_BUTTON_LEFT);
        mouseBindings.put("INTERACT_PLACE", GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        mouseBindings.put("PICK_BLOCK", GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
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

    public int consumeLastKey() {
        int key = lastKeyPressed;
        lastKeyPressed = -1;
        return key;
    }
    public double consumeScroll() {
        double temp = scrollY;
        scrollY = 0;
        return temp;
    }

    public List<Character> consumeTypedChars() {
        List<Character> copy = new ArrayList<>(typedChars);
        typedChars.clear();
        return copy;
    }

    public void setUICursorState(boolean showCursor, boolean isHovering) {
        int currentMode = GLFW.glfwGetInputMode(windowHandle, GLFW.GLFW_CURSOR);

        if (currentMode == GLFW.GLFW_CURSOR_DISABLED) return;

        if (showCursor) {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            GLFW.glfwSetCursor(windowHandle, isHovering ? handCursor : normalCursor);
        } else {
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_HIDDEN);
        }
    }

    public boolean isActionActive(String action) {
        if (typingMode) {
            if (!action.equals("UI_BACKSPACE") &&
                    !action.equals("PAUSE") &&
                    !action.equals("CHAT_SEND") &&
                    !action.equals("UI_TAB") &&
                    !action.equals("UI_UP") &&
                    !action.equals("UI_DOWN") &&
                    !mouseBindings.containsKey(action)) {
                return false;
            }
        }

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
    public void setTypingMode(boolean typingMode) { this.typingMode = typingMode; }

    public void consumeAction(String action) {
        previousActionStates.put(action, true);
    }
}
