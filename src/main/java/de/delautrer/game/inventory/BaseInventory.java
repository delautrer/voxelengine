package de.delautrer.game.inventory;

import de.delautrer.game.items.ItemStack;

import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
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

                // HIER GEÄNDERT: Statt 64 fragen wir das Item nach seiner maxStackSize
                int maxStack = current.type.getMaxStackSize();
                int spaceLeft = maxStack - current.amount;

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
                // Falls wir mehr hinzufügen wollen, als in einen Stack passt,
                // splitten wir es auf.
                int maxStack = stackToAdd.type.getMaxStackSize();
                int amountToAdd = Math.min(maxStack, stackToAdd.amount);

                setStack(i, new ItemStack(stackToAdd.type, amountToAdd));
                stackToAdd.amount -= amountToAdd;

                if (stackToAdd.amount == 0) return 0;
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
        java.util.Map<Item, Integer> counts = new java.util.TreeMap<>(
                java.util.Comparator.comparing(item -> {
                    String id = ItemRegistry.getId(item);
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
        for (java.util.Map.Entry<Item, Integer> entry : counts.entrySet()) {
            int amount = entry.getValue();
            Item item = entry.getKey();

            while (amount > 0 && index < slots.length) {
                int stackSize = Math.min(amount, item.getMaxStackSize());
                slots[index++] = new ItemStack(item, stackSize);
                amount -= stackSize;
            }
        }
    }
}
