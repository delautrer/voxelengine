package de.delautrer.game.ui.gui;

import de.delautrer.engine.input.InputManager;

public abstract class Screen {
    protected int width, height;
    protected float pixelScale;

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixelScale = height >= 1440 ? 4.0f : (height >= 1080 ? 3.0f : 2.0f);
        onInit();
    }

    protected abstract void onInit();

    public abstract void render(UIMeshBuilder builder, float mouseX, float mouseY);
    public abstract int getHoveredSlot(float mouseX, float mouseY);
    protected abstract void mouseClicked(float mouseX, float mouseY, int button);

    public void handleInput(InputManager input) {
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            mouseClicked(input.getMouseX(), input.getMouseY(), 0);
        }
    }
}