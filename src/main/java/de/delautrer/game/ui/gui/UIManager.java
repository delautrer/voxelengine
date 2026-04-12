package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.interaction.PlayerInteraction;
import de.delautrer.game.ui.DebugOverlay;

public class UIManager {
    private final HUD hud;
    private Screen currentScreen;
    private int lastWidth, lastHeight;

    public UIManager() {
        this.hud = new HUD();
    }

    public void update(InputManager input, PlayerInteraction interaction) {
        if (interaction.getInventory().isOpen()) {
            if (currentScreen == null) { // Öffnen
                currentScreen = new InventoryScreen(new Container(interaction.getInventory()));
                if (lastWidth > 0) currentScreen.init(lastWidth, lastHeight);
            }
            currentScreen.handleInput(input);
        } else {
            currentScreen = null; // Schließen
        }
    }

    public void buildMeshes(UIMeshBuilder builder, int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, VulkanFont font) {
        builder.clear();

        if (width != lastWidth || height != lastHeight) {
            lastWidth = width; lastHeight = height;
            if (currentScreen != null) currentScreen.init(width, height);
        }

        int hoveredSlot = -1;
        if (currentScreen != null) {
            hoveredSlot = currentScreen.getHoveredSlot(mouseX, mouseY);
            input.setCursorHover(hoveredSlot != -1);
        } else {
            input.setCursorHover(false);
        }

        hud.render(builder, width, height, interaction, hoveredSlot, debugOverlay, font);

        if (currentScreen != null) {
            currentScreen.render(builder, mouseX, mouseY);
        }
    }
}