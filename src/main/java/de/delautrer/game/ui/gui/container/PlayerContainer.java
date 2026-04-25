package de.delautrer.game.ui.gui.container;


import de.delautrer.game.inventory.PlayerInventory;

public class PlayerContainer extends BaseContainer {
    private final PlayerInventory playerInventory;

    public PlayerContainer(de.delautrer.game.inventory.PlayerInventory playerInventory) {
        this.playerInventory = playerInventory;

        // 1. Hotbar ganz unten (Y = 0)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, col * 24, 0));
        }

        // 2. Inventar-Grid (Wir drehen die Y-Berechnung um: 2 - row)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                // Y-Achse invertiert, damit Slot 9 oben links ist
                addSlot(new Slot(playerInventory, slotIndex, col * 24, 34 + ((2 - row) * 24)));
            }
        }
    }

    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }
}