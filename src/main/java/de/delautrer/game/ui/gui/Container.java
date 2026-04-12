package de.delautrer.game.ui.gui;

import de.delautrer.game.items.ItemStack;
import de.delautrer.game.player.Inventory;

public class Container {
    private final Inventory inventory;
    private ItemStack mouseStack = null;

    public Container(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() { return inventory; }
    public ItemStack getMouseStack() { return mouseStack; }

    public void handleSlotClick(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= Inventory.TOTAL_SIZE) return;

        ItemStack clicked = inventory.getStack(slotIndex);

        if (mouseStack == null) {
            inventory.setStack(slotIndex, null);
            mouseStack = clicked;
        } else {
            if (clicked == null) {
                inventory.setStack(slotIndex, mouseStack);
                mouseStack = null;
            } else if (clicked.type == mouseStack.type) {
                clicked.amount += mouseStack.amount;
                mouseStack = null;
            } else {
                inventory.setStack(slotIndex, mouseStack);
                mouseStack = clicked;
            }
        }
    }
}