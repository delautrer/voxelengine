package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.FurnaceInventory;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.blocks.entities.FurnaceBlockEntity;
import de.delautrer.game.items.ItemStack;

public class FurnaceContainer extends BaseContainer {
    private final PlayerInventory playerInventory;
    private final FurnaceInventory furnaceInventory;

    @SuppressWarnings("this-escape")
    public FurnaceContainer(PlayerInventory playerInventory, FurnaceInventory furnaceInventory) {
        this.playerInventory = playerInventory;
        this.furnaceInventory = furnaceInventory;

        // 1. Schmelz-Eingabe (Slot 0)
        addSlot(new Slot(furnaceInventory, 0, 54, 152));

        // 2. Brennstoff-Eingabe (Slot 1)
        addSlot(new Slot(furnaceInventory, 1, 54, 104));

        // 3. Schmelz-Ergebnis (Slot 2) - Kann nur entnommen werden!
        addSlot(new Slot(furnaceInventory, 2, 138, 128) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false; // Man darf hier nichts hineinlegen!
            }
        });

        // 4. Spieler-Grid (Slots 9-35), Mitte
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                addSlot(new Slot(playerInventory, slotIndex, col * 24, 30 + ((2 - row) * 24)));
            }
        }

        // 5. Spieler-Hotbar (Slots 0-8), Unten
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, col * 24, 0));
        }
    }

    public FurnaceInventory getFurnaceInventory() {
        return furnaceInventory;
    }

    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }
}
