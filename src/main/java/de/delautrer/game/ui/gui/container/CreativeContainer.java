package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.gui.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreativeContainer extends BaseContainer {

    private final PlayerInventory playerInventory;
    private final List<Item> allItems;
    private List<Item> filteredItems;

    private int scrollOffset = 0;
    private String searchText = "";

    public CreativeContainer(PlayerInventory playerInv) {
        this.playerInventory = playerInv;
        this.allItems = new ArrayList<>(ItemRegistry.getAll().values());

        this.allItems.sort(java.util.Comparator.comparing(item -> {
            String id = ItemRegistry.getId(item);
            return id != null ? id : "";
        }));

        this.filteredItems = new ArrayList<>(allItems);

        // 1. Creative-Grid Slots (9x5 sichtbare Slots)
        final int rows = 5;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(null, row * 9 + col, col * 24, 34 + (((rows-1) - row) * 24)));
            }
        }

        // 2. Spieler-Hotbar (Slots 0-8) ganz unten
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, col * 24, 0));
        }

        updateFilteredItems();
    }

    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
        this.scrollOffset = 0;
        updateFilteredItems();
    }

    private void updateFilteredItems() {
        if (searchText.isEmpty()) {
            filteredItems = new ArrayList<>(allItems);
        } else {
            filteredItems = allItems.stream()
                    .filter(item -> ItemRegistry.getId(item).toLowerCase().contains(searchText))
                    .collect(Collectors.toList());
        }
    }

    public void scrollTo(float progress) {
        int rows = (int) Math.ceil(filteredItems.size() / 9.0);
        int maxScroll = Math.max(0, rows - 5);
        this.scrollOffset = Math.round(progress * maxScroll);
    }

    public Item getItemInGrid(int slotIndex) {
        int actualIndex = (scrollOffset * 9) + slotIndex;
        if (actualIndex >= 0 && actualIndex < filteredItems.size()) {
            return filteredItems.get(actualIndex);
        }
        return null;
    }

    @Override
    public void clickSlot(Slot slot, int button, ClickType clickType) {
        if (slot.inventory == null) {
            Item item = getItemInGrid(slot.slotIndex);
            if (item != null) {
                if (clickType == ClickType.QUICK_MOVE) {
                    // Shift + Klick = Direkt 64 an die Maus (Max Stack)
                    setMouseStack(new ItemStack(item, 64));
                } else if (clickType == ClickType.PICKUP || clickType == ClickType.SPLIT) {
                    // Normaler Linksklick oder Rechtsklick
                    ItemStack currentMouse = getMouseStack();
                    if (currentMouse == null || currentMouse.type != item) {
                        // Leer oder anderes Item -> Komplett überschreiben mit 1 Item!
                        setMouseStack(new ItemStack(item, 1));
                    } else if (currentMouse.amount < 64) {
                        // Gleiches Item in der Hand -> Anzahl +1
                        currentMouse.amount++;
                    }
                }
            }
        } else {
            super.clickSlot(slot, button, clickType);
        }
    }

    public int getMaxRows() { return (int) Math.ceil(filteredItems.size() / 9.0); }
    public int getScrollOffset() { return scrollOffset; }
}