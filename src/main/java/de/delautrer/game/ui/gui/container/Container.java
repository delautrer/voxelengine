package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.items.ItemStack;

public class Container {
    private final PlayerInventory inventory;
    private ItemStack mouseStack = null;

    public Container(PlayerInventory inventory) {
        this.inventory = inventory;
    }

    public PlayerInventory getInventory() { return inventory; }
    public ItemStack getMouseStack() { return mouseStack; }
    public void setMouseStack(ItemStack stack) {
        this.mouseStack = stack;
    }

    public void handleSlotClick(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= PlayerInventory.TOTAL_SIZE) return;

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
