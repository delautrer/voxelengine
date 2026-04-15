package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;

public abstract class UIElement {
    protected float x, y;
    protected float width, height;
    protected boolean isVisible = true;

    public UIElement(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public abstract void render(UIMeshBuilder builder, VulkanFont font, float mouseX, float mouseY);

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }
}