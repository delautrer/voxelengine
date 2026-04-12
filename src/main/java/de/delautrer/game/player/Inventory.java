package de.delautrer.game.player;

import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int INV_SIZE = 27;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + INV_SIZE;

    private final ItemStack[] slots = new ItemStack[TOTAL_SIZE];
    private int selectedHotbarSlot = 0;
    private boolean isOpen = false;

    public Inventory() {
        ItemRegistry.init();
        int i = 0;
        for (de.delautrer.game.items.Item item : ItemRegistry.getAll().values()) {
            slots[i++] = new ItemStack(item, 64);
            if (i >= TOTAL_SIZE) break;
        }
    }

    public void toggle() { isOpen = !isOpen; }
    public boolean isOpen() { return isOpen; }

    public ItemStack getStack(int i) { return slots[i]; }
    public void setStack(int i, ItemStack stack) { slots[i] = stack; }

    public ItemStack getSelectedHotbarStack() { return slots[selectedHotbarSlot]; }
    public void setSelectedSlot(int s) { this.selectedHotbarSlot = s; }
    public int getSelectedSlot() { return selectedHotbarSlot; }
}