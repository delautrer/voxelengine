package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.ui.UIMeshBuilder;

import de.delautrer.engine.input.InputManager;
public class UIInputField extends UIElement {
    private String text = "";
    private String placeholder;
    private boolean isFocused = false;
    private int maxLength;

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
    }
    public String getText() {
        return text;
    }

    // Wird vom InputManager aufgerufen, wenn Buchstaben getippt werden
    public void typeChar(char c) {
        if (isFocused && text.length() < maxLength) {
            text += c;
        }
    }

    // Wird vom InputManager aufgerufen bei "Backspace"
    public void backspace() {
        if (isFocused && text.length() > 0) {
            text = text.substring(0, text.length() - 1);
        }
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

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
            float textWidth = builder.getTextWidth(text, font);
            builder.addRect(x + 12.0f + textWidth, textY, 0.2f, 2.0f, 20.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public void handleInput(InputManager input) {
        if (!isFocused) return;

        for (char c : input.consumeTypedChars()) {
            typeChar(c);
        }

        if (input.isActionJustPressed("UI_BACKSPACE")) {
            backspace();
        }
    }
}
