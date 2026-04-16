package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;

import java.util.ArrayList;
import java.util.List;

public abstract class MenuScreen extends Screen {

    protected List<UIElement> elements = new ArrayList<>();
    protected VulkanFont font;

    public void setFont(VulkanFont font) {
        this.font = font;
    }

    @Override
    public void render(UIMeshBuilder builder, float mouseX, float mouseY) {
        for (UIElement element : elements) {
            element.render(builder, font, mouseX, mouseY);
        }
    }

    public void handleMenuInput(InputManager input, float uiMouseX, float uiMouseY) {
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            for (UIElement element : elements) {
                if (element instanceof UIButton && element.isHovered(uiMouseX, uiMouseY)) {
                    ((UIButton) element).click();
                    return;
                }
            }
        }
    }

    @Override
    public int getHoveredSlot(float mouseX, float mouseY) { return -1; }

    @Override
    protected void mouseClicked(float mouseX, float mouseY, int button) {}

    @Override
    protected void onInit() {}
}