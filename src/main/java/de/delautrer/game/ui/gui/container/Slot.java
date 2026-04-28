package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.IInventory;
import de.delautrer.game.items.ItemStack;

public class Slot {
    public final IInventory inventory;
    public final int slotIndex;
    public int x;
    public int y;

    public Slot(IInventory inventory, int slotIndex, int x, int y) {
        this.inventory = inventory;
        this.slotIndex = slotIndex;
        this.x = x;
        this.y = y;
    }

    public ItemStack getStack() {
        if (inventory == null) return null;
        return inventory.getStack(slotIndex);
    }

    public void putStack(ItemStack stack) {
        if (inventory == null) return;
        inventory.setStack(slotIndex, stack);
        onSlotChanged();
    }

    public boolean hasItem() {
        return getStack() != null;
    }

    public boolean isItemValid(ItemStack stack) {
        return true;
    }

    public void onTake() {
    }

    public void onSlotChanged() {
    }
}