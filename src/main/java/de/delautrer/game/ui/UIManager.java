package de.delautrer.game.ui;
import de.delautrer.engine.graphics.*;
import de.delautrer.engine.graphics.vulkan.*;
import de.delautrer.engine.graphics.vulkan.core.*;
import de.delautrer.engine.graphics.vulkan.pipeline.*;
import de.delautrer.engine.graphics.vulkan.buffer.*;
import de.delautrer.engine.graphics.vulkan.texture.*;

import de.delautrer.engine.graphics.vulkan.texture.VulkanFont;
import de.delautrer.engine.input.InputManager;
import de.delautrer.game.entity.player.GameMode;
import de.delautrer.game.entity.player.PlayerInteraction;
import de.delautrer.game.inventory.ChestInventory;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.ui.gui.container.CreativeContainer;
import de.delautrer.game.ui.gui.container.ChestContainer;
import de.delautrer.game.ui.gui.screens.*;
import de.delautrer.game.ui.gui.container.PlayerContainer;

import de.delautrer.game.ui.gui.screens.ContainerScreen;
public class UIManager {
    private final HUD hud;
    private Screen currentScreen;
    private int lastWidth, lastHeight;

    public UIManager() {
        this.hud = new HUD();
    }

    public void update(InputManager input, PlayerInteraction interaction) {
        if (interaction.getPlayer().isDead()) {
            if (currentScreen != null) {
                currentScreen.onClose();
            }
            currentScreen = null;
            return;
        }
        IInventory externalInv = interaction.getPlayer().getOpenedInventory();
        boolean isPlayerInvOpen = interaction.getInventory().isOpen();

        if (externalInv != null) {
            if (!(currentScreen instanceof ChestScreen)) {
                if (currentScreen != null) {
                    currentScreen.onClose();
                }
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
                if (currentScreen != null) {
                    currentScreen.onClose();
                }

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
            if (currentScreen != null) {
                currentScreen.onClose(); // <--- WICHTIGSTES NEU: Hier wird das Inventar komplett geschlossen!
            }
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
        boolean showCursor = false;
        boolean isHovering = false;

        if (currentScreen != null) {
            showCursor = true;
            hoveredSlot = currentScreen.getHoveredSlot(mouseX, mouseY);
            isHovering = (hoveredSlot != -1);

            if (currentScreen instanceof ContainerScreen containerScreen) {
                if (containerScreen.getContainer().getMouseStack() != null) {
                    showCursor = false;
                }
            }

            input.setUICursorState(showCursor, isHovering);

        } else {
            input.setUICursorState(false, false);
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
