package de.delautrer.game.ui;

import de.delautrer.engine.graphics.VulkanFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.inventory.ChestInventory;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.ui.gui.container.CreativeContainer;
import de.delautrer.game.ui.gui.container.ChestContainer;
import de.delautrer.game.ui.gui.screens.ChestScreen;
import de.delautrer.game.ui.gui.screens.HUD;
import de.delautrer.game.ui.gui.container.PlayerContainer;
import de.delautrer.game.ui.gui.screens.CreativeInventoryScreen;
import de.delautrer.game.ui.gui.screens.InventoryScreen;
import de.delautrer.game.ui.gui.screens.MenuScreen;
import de.delautrer.game.ui.gui.screens.Screen;

public class UIManager {
    private final HUD hud;
    private Screen currentScreen;
    private int lastWidth, lastHeight;

    public UIManager() {
        this.hud = new HUD();
    }

    public void update(InputManager input, PlayerInteraction interaction) {
        IInventory externalInv = interaction.getPlayer().getOpenedInventory();
        boolean isPlayerInvOpen = interaction.getInventory().isOpen();

        // 1. Hat der Spieler eine Kiste (oder ein anderes externes Inventar) offen?
        if (externalInv != null) {
            // Wir prüfen, ob wir den ChestScreen neu initialisieren müssen
            if (!(currentScreen instanceof ChestScreen)) {
                if (externalInv instanceof ChestInventory) {
                    currentScreen = new ChestScreen(new ChestContainer(interaction.getInventory(), (ChestInventory) externalInv));
                }

                if (lastWidth > 0 && currentScreen != null) {
                    currentScreen.init(lastWidth, lastHeight);
                }
            }
            if (currentScreen != null) {
                currentScreen.handleInput(input);
            }

        }
        // 2. Hat der Spieler nur sein eigenes Inventar offen?
        else if (isPlayerInvOpen) {
            // Prüfen, ob wir den Screen neu laden müssen (falls er vorher null oder ein ChestScreen war)
            if (currentScreen == null || currentScreen instanceof ChestScreen) {
                if (interaction.getPlayer().getGameMode() == GameMode.CREATIVE) {
                    currentScreen = new CreativeInventoryScreen(new CreativeContainer(interaction.getInventory()));
                } else {
                    currentScreen = new InventoryScreen(new PlayerContainer(interaction.getInventory()));
                }

                if (lastWidth > 0) currentScreen.init(lastWidth, lastHeight);
            }
            currentScreen.handleInput(input);

        }
        // 3. Gar kein Inventar offen
        else {
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