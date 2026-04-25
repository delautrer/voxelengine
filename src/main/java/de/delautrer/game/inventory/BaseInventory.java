package de.delautrer.game.inventory;

import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.gui.Slot;

public abstract class BaseInventory implements IInventory {
    private boolean sortable = false;
    protected final ItemStack[] slots;

    public BaseInventory(int size) {
        this.slots = new ItemStack[size];
    }

    @Override
    public int getSize() { return slots.length; }

    @Override
    public ItemStack getStack(int index) {
        if (index < 0 || index >= slots.length) return null;
        return slots[index];
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        if (index >= 0 && index < slots.length) {
            slots[index] = stack;
        }
    }

    @Override
    public int addItem(ItemStack stackToAdd) {
        if (stackToAdd == null || stackToAdd.type == null || stackToAdd.amount <= 0) return 0;

        // 1. Auffüllen von existierenden Stacks
        for (int i = 0; i < slots.length; i++) {
            ItemStack current = getStack(i);
            if (current != null && current.type == stackToAdd.type) {
                int spaceLeft = 64 - current.amount; // 64 = Max Stack
                if (spaceLeft > 0) {
                    int amountToAdd = Math.min(spaceLeft, stackToAdd.amount);
                    current.amount += amountToAdd;
                    stackToAdd.amount -= amountToAdd;
                    if (stackToAdd.amount == 0) return 0;
                }
            }
        }

        // 2. Leeren Slot suchen
        for (int i = 0; i < slots.length; i++) {
            if (getStack(i) == null) {
                setStack(i, new ItemStack(stackToAdd.type, stackToAdd.amount));
                return 0;
            }
        }
        return stackToAdd.amount; // Rest zurückgeben
    }

    @Override
    public void clear() {
        for (int i = 0; i < slots.length; i++) slots[i] = null;
    }

    public void setSortable(boolean sortable) { this.sortable = sortable; }

    @Override
    public boolean isSortable() { return sortable; }
    @Override
    public void sort() {
        if (!sortable) return;

        // TreeMap sortiert Items automatisch alphabetisch nach ihrer ID
        java.util.Map<de.delautrer.game.items.Item, Integer> counts = new java.util.TreeMap<>(
                java.util.Comparator.comparing(item -> {
                    String id = de.delautrer.game.items.ItemRegistry.getId(item);
                    return id != null ? id : "";
                })
        );

        // 1. Alle Items aufsammeln und stapeln
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null && slots[i].amount > 0) {
                counts.put(slots[i].type, counts.getOrDefault(slots[i].type, 0) + slots[i].amount);
                slots[i] = null;
            }
        }

        // 2. Sortiert und als maximale Stacks wieder einfügen
        int index = 0;
        for (java.util.Map.Entry<de.delautrer.game.items.Item, Integer> entry : counts.entrySet()) {
            int amount = entry.getValue();
            while (amount > 0 && index < slots.length) {
                int stackSize = Math.min(amount, 64);
                slots[index++] = new ItemStack(entry.getKey(), stackSize);
                amount -= stackSize;
            }
        }
    }
}