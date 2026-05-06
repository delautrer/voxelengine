package de.delautrer.game.ui.elements;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.IFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import org.lwjgl.glfw.GLFW;
import java.util.function.Consumer;

public class UIKeybindButton extends UIElement {
    private int currentKey;
    private boolean isListening = false;
    private final Consumer<Integer> onRebind;

    private final UIHBox layout;
    private final UIButton button;

    public UIKeybindButton(float width, float height, String actionName, int currentKey, Consumer<Integer> onRebind) {
        super(0, 0, width, height); // X und Y setzt die ScrollList später
        this.currentKey = currentKey;
        this.onRebind = onRebind;

        this.layout = new UIHBox(0, 0, 10.0f); // 10px Abstand zwischen Text und Knopf

        float btnWidth = 140.0f; // Feste Breite für den klickbaren Knopf rechts
        float lblWidth = width - btnWidth - 10.0f; // Der Rest ist für den Text

        UILabel label = new UILabel(lblWidth, height, actionName);
        button = new UIButton(0, 0, btnWidth, height, getKeyName(currentKey), null);

        layout.addChild(label);
        layout.addChild(button);
    }

    // Wenn das Element verschoben wird, verschieben wir das interne Layout mit!
    @Override
    public void setPosition(float x, float y) {
        super.setPosition(x, y);
        layout.setPosition(x, y);
    }

    public void handleInput(InputManager input, float mouseX, float mouseY) {
        if (!isVisible) return;

        if (isListening) {
            int key = input.consumeLastKey();
            if (key != -1) {
                if (key != GLFW.GLFW_KEY_ESCAPE) {
                    this.currentKey = key;
                    if (onRebind != null) onRebind.accept(key);
                }
                isListening = false;
                button.setText(getKeyName(currentKey));
            }
        } else {
            // Wir horchen nur auf Klicks, wenn die Maus EXAKT über dem kleinen Button rechts ist!
            if (input.isActionJustPressed("INTERACT_BREAK") && button.isHovered(mouseX, mouseY)) {
                isListening = true;
                button.setText("> PRESS <");
                input.consumeLastKey();
            }
        }
    }

    private String getKeyName(int keycode) {
        if (keycode < 0) return "UNBOUND";
        if (keycode == GLFW.GLFW_KEY_SPACE) return "SPACE";
        if (keycode == GLFW.GLFW_KEY_LEFT_SHIFT) return "L-SHIFT";
        if (keycode == GLFW.GLFW_KEY_LEFT_CONTROL) return "L-CTRL";
        if (keycode == GLFW.GLFW_KEY_LEFT_ALT) return "L-ALT";

        if (keycode == GLFW.GLFW_KEY_F1) return "F1";
        if (keycode == GLFW.GLFW_KEY_F2) return "F2";
        if (keycode == GLFW.GLFW_KEY_F3) return "F3";
        if (keycode == GLFW.GLFW_KEY_F4) return "F4";
        if (keycode == GLFW.GLFW_KEY_F5) return "F5";
        if (keycode == GLFW.GLFW_KEY_F6) return "F6";
        if (keycode == GLFW.GLFW_KEY_F7) return "F7";
        if (keycode == GLFW.GLFW_KEY_F8) return "F8";
        if (keycode == GLFW.GLFW_KEY_F9) return "F9";
        if (keycode == GLFW.GLFW_KEY_F10) return "F10";
        if (keycode == GLFW.GLFW_KEY_F11) return "F11";
        if (keycode == GLFW.GLFW_KEY_F12) return "F12";
        if (keycode == GLFW.GLFW_KEY_F13) return "F13";
        if (keycode == GLFW.GLFW_KEY_F14) return "F14";
        if (keycode == GLFW.GLFW_KEY_F15) return "F15";
        if (keycode == GLFW.GLFW_KEY_F16) return "F16";
        if (keycode == GLFW.GLFW_KEY_F17) return "F17";
        if (keycode == GLFW.GLFW_KEY_F18) return "F18";
        if (keycode == GLFW.GLFW_KEY_F19) return "F19";
        if (keycode == GLFW.GLFW_KEY_F20) return "F20";
        if (keycode == GLFW.GLFW_KEY_F21) return "F21";
        if (keycode == GLFW.GLFW_KEY_F22) return "F22";
        if (keycode == GLFW.GLFW_KEY_F23) return "F23";
        if (keycode == GLFW.GLFW_KEY_F24) return "F24";
        if (keycode == GLFW.GLFW_KEY_F25) return "F25";

        String name = GLFW.glfwGetKeyName(keycode, 0);
        return name != null ? name.toUpperCase() : "KEY_" + keycode;
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible) return;
        // Wir lassen einfach die interne HBox das Zeichnen übernehmen!
        layout.render(builder, font, mouseX, mouseY);
    }
}
