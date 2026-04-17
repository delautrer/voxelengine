package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;

public class UIInputField extends UIElement {
    private String text = "";
    private String placeholder;
    private boolean isFocused = false;
    private int maxLength;

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
        float bg = isFocused ? 0.1f : 0.2f;
        builder.addRect(x, y, 0.1f, width, height, bg, bg, bg, 1.0f);

        // Rahmen (Weiß wenn fokussiert, sonst Grau)
        float border = isFocused ? 0.9f : 0.4f;
        builder.addRect(x, y, 0.15f, width, 2, border, border, border, 1.0f); // Top
        builder.addRect(x, y + height - 2, 0.15f, width, 2, border, border, border, 1.0f); // Bottom
        builder.addRect(x, y, 0.15f, 2, height, border, border, border, 1.0f); // Left
        builder.addRect(x + width - 2, y, 0.15f, 2, height, border, border, border, 1.0f); // Right

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
}