package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.items.ItemStack;

public class CraftingResultSlot extends Slot {
    private final CraftingInventory craftingMatrix;

    public CraftingResultSlot(IInventory inventory, int slotIndex, int x, int y, CraftingInventory craftingMatrix) {
        super(inventory, slotIndex, x, y);
        this.craftingMatrix = craftingMatrix;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return false;
    }

    @Override
    public void onTake() {
        for (int i = 0; i < craftingMatrix.getSize(); i++) {
            ItemStack stack = craftingMatrix.getStack(i);
            if (stack != null) {
                stack.amount--;
                if (stack.amount <= 0) {
                    craftingMatrix.setStack(i, null);
                }
            }
        }
        // Das Resultat updaten wir später über den Container
    }
}
