package de.delautrer.game.ui.gui.screens;

import de.delautrer.engine.input.InputManager;
import de.delautrer.game.ui.UIMeshBuilder;
import java.util.List;

public abstract class Screen {
    protected int width, height;
    protected float pixelScale;

    public void init(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixelScale = height >= 1440 ? 4.0f : (height >= 1080 ? 3.0f : 2.0f);
        onInit();
    }

    public void onClose() {}

    protected abstract void onInit();
    public abstract void render(UIMeshBuilder builder, float mouseX, float mouseY);
    public abstract int getHoveredSlot(float mouseX, float mouseY);
    protected abstract void mouseClicked(float mouseX, float mouseY, int button);

    protected void onKeyPressed(InputManager input) {}
    protected void onCharTyped(char c) {}

    public void handleInput(InputManager input) {
        if (input.isActionJustPressed("INTERACT_BREAK")) {
            mouseClicked(input.getMouseX(), input.getMouseY(), 0);
        }

        onKeyPressed(input);

        List<Character> typedChars = input.consumeTypedChars();
        for (char c : typedChars) {
            onCharTyped(c);
        }
    }
}