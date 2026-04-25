package de.delautrer.game.ui.gui.container;

public class ChestContainer extends BaseContainer {

    public ChestContainer(de.delautrer.game.inventory.PlayerInventory playerInv, de.delautrer.game.inventory.ChestInventory chestInv) {

        // 1. Kisten-Grid (Slots 0-26), ganz Oben
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(chestInv, row * 9 + col, col * 24, 116 + ((2 - row) * 24)));
            }
        }

        // 2. Spieler-Grid (Slots 9-35), Mitte
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                addSlot(new Slot(playerInv, slotIndex, col * 24, 30 + ((2 - row) * 24)));
            }
        }

        // 3. Spieler-Hotbar (Slots 0-8), Unten
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, col * 24, 0));
        }
    }
}