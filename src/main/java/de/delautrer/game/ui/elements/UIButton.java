package de.delautrer.game.ui.elements;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.game.ui.UIMeshBuilder;

public class UIButton extends UIElement {
    private final String text;
    private final Runnable onClick;

    private static final int GRID_X_NORMAL = 0;
    private static final int GRID_Y_NORMAL = 0;
    private static final int GRID_X_HOVER = 1;
    private static final int GRID_Y_HOVER = 0;
    private static final float CORNER_SIZE = 8.0f;

    public UIButton(float x, float y, float width, float height, String text, Runnable onClick) {
        super(x, y, width, height);
        this.text = text;
        this.onClick = onClick;
    }

    @Override
    public void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY) {
        if (!isVisible) return;

        boolean hovered = isHovered(mouseX, mouseY);

        int gridX = hovered ? GRID_X_HOVER : GRID_X_NORMAL;
        int gridY = hovered ? GRID_Y_HOVER : GRID_Y_NORMAL;

        builder.add9Slice(x, y, 0.1f, width, height, gridX, gridY, CORNER_SIZE);

        if (font != null) {
            float textWidth = builder.getTextWidth(text, font);
            float textX = x + (width / 2.0f) - (textWidth / 2.0f);
            float textY = y + (height - 24.0f + 10f) / 2.0f;

            builder.drawText(text, textX, textY, 0.3f, font);
        }
    }

    public void click() {
        if (isVisible && onClick != null) {
            onClick.run();
        }
    }
}