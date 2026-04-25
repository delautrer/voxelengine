package de.delautrer.game.inventory;

import de.delautrer.game.items.ItemStack;

public interface IInventory {
    boolean isSortable();
    void sort();
    int getSize();
    ItemStack getStack(int index);
    void setStack(int index, ItemStack stack);
    int addItem(ItemStack stack);
    void clear();
}