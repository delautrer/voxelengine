package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import java.util.function.Consumer;

public class UIToggleButton extends UIElement {
    private String[] options;
    private int currentIndex = 0;
    private String prefix;
    private Consumer<String> onChange;

    public UIToggleButton(float x, float y, float width, float height, String prefix, String[] options, Consumer<String> onChange) {
        super(x, y, width, height);
        this.prefix = prefix;
        this.options = options;
        this.onChange = onChange;
    }

    public void click() {
        currentIndex = (currentIndex + 1) % options.length;
        if (onChange != null) {
            onChange.accept(options[currentIndex]);
        }
    }

    public String getCurrentValue() {
        return options[currentIndex];
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        int gridX = isHovered(mouseX, mouseY) ? 0 : 1;
        int gridY = 8;

        // Hintergrund (Z = 0.1)
        builder.addAtlasQuad(x, y, 0.1f, width, height, gridX, gridY, 1, 1, false);

        // Text zentriert (Z = 0.2)
        String displayText = prefix + ": " + options[currentIndex];
        float textWidth = builder.getTextWidth(displayText, font);
        float textX = x + (width / 2.0f) - (textWidth / 2.0f);
        float textY = y + (height / 2.0f) - 10.0f;

        builder.drawText(displayText, textX, textY, 0.2f, font);
    }
}