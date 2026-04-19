package de.delautrer.game.ui.gui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.ui.ChatOverlay;
import de.delautrer.game.ui.DebugOverlay;
import de.delautrer.game.ui.gui.screens.CreativeInventoryScreen;
import de.delautrer.game.ui.gui.screens.InventoryScreen;
import de.delautrer.game.ui.gui.screens.MenuScreen;

public class UIManager {
    private final HUD hud;
    private Screen currentScreen;
    private int lastWidth, lastHeight;

    public UIManager() {
        this.hud = new HUD();
    }

    public void update(InputManager input, PlayerInteraction interaction) {
        if (interaction.getInventory().isOpen()) {
            if (currentScreen == null) {
                if (interaction.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    currentScreen = new CreativeInventoryScreen(new Container(interaction.getInventory()));
                } else {
                    currentScreen = new InventoryScreen(new Container(interaction.getInventory()));
                }

                if (lastWidth > 0) currentScreen.init(lastWidth, lastHeight);
            }
            currentScreen.handleInput(input);
        } else {
            currentScreen = null;
        }
    }

    public void buildMeshes(UIMeshBuilder builder, int width, int height, InputManager input, PlayerInteraction interaction, float mouseX, float mouseY, DebugOverlay debugOverlay, ChatOverlay chatOverlay, VulkanFont font, int blockAtlasWidth) {
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

        hud.render(builder, width, height, interaction, hoveredSlot, debugOverlay, chatOverlay, font, blockAtlasWidth);

        if (currentScreen != null) {
            if (currentScreen instanceof MenuScreen) {
                ((MenuScreen) currentScreen).setFont(font);
            }
            currentScreen.render(builder, mouseX, mouseY);
        }
    }
}