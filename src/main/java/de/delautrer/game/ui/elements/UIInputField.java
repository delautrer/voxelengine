package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.IFont;
import de.delautrer.game.ui.UIMeshBuilder;
import de.delautrer.engine.input.InputManager;

public class UIInputField extends UIElement {
    private String text = "";
    private String placeholder;
    private boolean isFocused = false;
    private int maxLength;
    private int cursorIndex = 0;

    private static final int GRID_X_NORMAL = 0;
    private static final int GRID_Y_NORMAL = 0;
    private static final int GRID_X_HOVER = 1;
    private static final int GRID_Y_HOVER = 0;
    private static final float CORNER_SIZE = 8.0f;

    public UIInputField(float x, float y, float width, float height, String placeholder, int maxLength) {
        super(x, y, width, height);
        this.placeholder = placeholder;
        this.maxLength = maxLength;
    }

    public void setFocused(boolean focused) {
        this.isFocused = focused;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public void setText(String text) {
        this.text = text;
        this.cursorIndex = text.length();
    }

    public String getText() {
        return text;
    }

    // Wird vom InputManager aufgerufen, wenn Buchstaben getippt werden
    public void typeChar(char c) {
        if (isFocused && text.length() < maxLength) {
            text = text.substring(0, cursorIndex) + c + text.substring(cursorIndex);
            cursorIndex++;
        }
    }

    // Wird vom InputManager aufgerufen bei "Backspace"
    public void backspace() {
        if (isFocused && cursorIndex > 0) {
            text = text.substring(0, cursorIndex - 1) + text.substring(cursorIndex);
            cursorIndex--;
        }
    }

    public void delete() {
        if (isFocused && cursorIndex < text.length()) {
            text = text.substring(0, cursorIndex) + text.substring(cursorIndex + 1);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, IFont font, float mouseX, float mouseY) {
        if (!isVisible)
            return;

        // Hintergrund (dunkler, wenn fokussiert)
        int gridX = isFocused ? GRID_X_HOVER : GRID_X_NORMAL;
        int gridY = isFocused ? GRID_Y_HOVER : GRID_Y_NORMAL;

        builder.add9Slice(x, y, 0.1f, width, height, gridX, gridY, CORNER_SIZE);

        // Text rendern
        String displayText = text.isEmpty() && !isFocused ? placeholder : text;
        float textY = y + (height / 2.0f) - 10.0f;

        builder.drawText(displayText, x + 10.0f, textY, 0.2f, font);

        // Blinkender Cursor (Nur wenn fokussiert)
        if (isFocused && (System.currentTimeMillis() % 1000 < 500)) {
            String textBeforeCursor = text.substring(0, cursorIndex);
            float cursorOffset = builder.getTextWidth(textBeforeCursor, font);
            builder.addRect(x + 10.0f + cursorOffset, textY - 2.0f, 0.5f, 2.0f, 22.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public void handleInput(InputManager input) {
        if (!isFocused)
            return;

        for (char c : input.consumeTypedChars()) {
            typeChar(c);
        }

        if (input.isActionJustPressed("UI_BACKSPACE")) {
            backspace();
        }

        // Kopieren & Einfügen
        if (input.isControlDown()) {
            int lastKey = input.consumeLastKey();
            if (lastKey == org.lwjgl.glfw.GLFW.GLFW_KEY_V) {
                String clipboard = input.getClipboardString();
                if (clipboard != null) {
                    for (char c : clipboard.toCharArray()) {
                        typeChar(c);
                    }
                }
            } else if (lastKey == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
                input.setClipboardString(text);
            }
        }

        // Pfeiltasten & Delete
        int lastKey = input.consumeLastKey();
        if (lastKey == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT) {
            if (cursorIndex > 0) cursorIndex--;
        } else if (lastKey == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT) {
            if (cursorIndex < text.length()) cursorIndex++;
        } else if (lastKey == org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE) {
            delete();
        }
    }
}
