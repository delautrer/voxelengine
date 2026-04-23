package de.delautrer.game.player;

import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.persistence.PlayerData;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int INV_SIZE = 27;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + INV_SIZE;

    private final ItemStack[] slots = new ItemStack[TOTAL_SIZE];
    private int selectedHotbarSlot = 0;
    private boolean isOpen = false;

    public Inventory() {
        ItemRegistry.init();
    }

    public void toggle() { isOpen = !isOpen; }
    public boolean isOpen() { return isOpen; }

    public ItemStack getStack(int i) { return slots[i]; }
    public void setStack(int i, ItemStack stack) { slots[i] = stack; }

    // Persistence
    public PlayerData.SavedSlot[] exportToSavedData() {
        PlayerData.SavedSlot[] savedSlots =
                new PlayerData.SavedSlot[TOTAL_SIZE];

        for (int i = 0; i < TOTAL_SIZE; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.type != null && stack.amount > 0) {
                String id = ItemRegistry.getId(stack.type);
                if (id != null) {
                    savedSlots[i] = new PlayerData.SavedSlot(id, stack.amount);
                }
            }
        }
        return savedSlots;
    }

    public void importFromSavedData(PlayerData.SavedSlot[] savedSlots) {
        if (savedSlots == null) return;

        // Erstmal alles leeren
        for (int i = 0; i < TOTAL_SIZE; i++) {
            slots[i] = null;
        }

        // Dann aus der Datei laden
        for (int i = 0; i < Math.min(TOTAL_SIZE, savedSlots.length); i++) {
            PlayerData.SavedSlot saved = savedSlots[i];
            if (saved != null && saved.id != null) {
                Item item = ItemRegistry.get(saved.id);
                if (item != null) {
                    slots[i] = new ItemStack(item, saved.amount);
                } else {
                    System.err.println("Warnung: Unbekanntes Item in Speicherdatei gefunden: " + saved.id);
                }
            }
        }
    }

    public int addItem(ItemStack stackToAdd) {
        if (stackToAdd == null || stackToAdd.type == null || stackToAdd.amount <= 0) return 0;

        // 1. Versuche, existierende Stacks desselben Typs aufzufüllen
        for (int i = 0; i < TOTAL_SIZE; i++) {
            ItemStack current = getStack(i);
            if (current != null && current.type == stackToAdd.type) {
                int spaceLeft = 64 - current.amount; // 64 = Max Stack Size
                if (spaceLeft > 0) {
                    int amountToAdd = Math.min(spaceLeft, stackToAdd.amount);
                    current.amount += amountToAdd;
                    stackToAdd.amount -= amountToAdd;
                    if (stackToAdd.amount == 0) return 0; // Komplett hinzugefügt!
                }
            }
        }

        // 2. Wenn noch Reste da sind, suche den ersten leeren Slot
        for (int i = 0; i < TOTAL_SIZE; i++) {
            if (getStack(i) == null) {
                setStack(i, new ItemStack(stackToAdd.type, stackToAdd.amount));
                return 0; // Komplett hinzugefügt!
            }
        }

        // 3. Inventar ist voll: Gib die Menge zurück, die nicht passte
        return stackToAdd.amount;
    }

    public ItemStack getSelectedHotbarStack() { return slots[selectedHotbarSlot]; }
    public void setSelectedSlot(int s) { this.selectedHotbarSlot = s; }
    public int getSelectedSlot() { return selectedHotbarSlot; }

    public void setOpen(boolean open) {
        isOpen = open;
    }
}