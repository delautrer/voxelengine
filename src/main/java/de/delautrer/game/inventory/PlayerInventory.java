package de.delautrer.game.inventory;

import de.delautrer.game.crafting.RecipeManager;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.world.persistence.PlayerData;

public class PlayerInventory extends BaseInventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int INV_SIZE = 27;
    public static final int TOTAL_SIZE = HOTBAR_SIZE + INV_SIZE;

    private int selectedHotbarSlot = 0;
    private boolean isOpen = false;

    public PlayerInventory() {
        super(TOTAL_SIZE);
        RecipeManager.loadRecipes();
        this.setSortable(true);
    }

    public void toggle() { isOpen = !isOpen; }
    public boolean isOpen() { return isOpen; }
    public void setOpen(boolean open) { isOpen = open; }

    public ItemStack getSelectedHotbarStack() { return slots[selectedHotbarSlot]; }
    public void setSelectedSlot(int s) { this.selectedHotbarSlot = s; }
    public int getSelectedSlot() { return selectedHotbarSlot; }

    // --- Persistence (Speichern & Laden) ---
    public PlayerData.SavedSlot[] exportToSavedData() {
        PlayerData.SavedSlot[] savedSlots = new PlayerData.SavedSlot[TOTAL_SIZE];
        for (int i = 0; i < TOTAL_SIZE; i++) {
            ItemStack stack = slots[i];
            if (stack != null && stack.type != null && stack.amount > 0) {
                String id = ItemRegistry.getId(stack.type);
                if (id != null) savedSlots[i] = new PlayerData.SavedSlot(id, stack.amount);
            }
        }
        return savedSlots;
    }

    public void importFromSavedData(PlayerData.SavedSlot[] savedSlots) {
        if (savedSlots == null) return;
        clear();
        for (int i = 0; i < Math.min(TOTAL_SIZE, savedSlots.length); i++) {
            PlayerData.SavedSlot saved = savedSlots[i];
            if (saved != null && saved.id != null) {
                Item item = ItemRegistry.get(saved.id);
                if (item != null) slots[i] = new ItemStack(item, saved.amount);
            }
        }
    }
}