package de.delautrer.game.ui.gui.container;

import de.delautrer.Constants;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.ui.gui.ClickType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreativeContainer extends BaseContainer {

    public enum CreativeTab {
        INVENTORY("Player Inventory", "grass_block"),
        NATURAL("Natural Blocks", "grass_block"),
        WOOD("Wood & Forestry", "oak_log"),
        BUILDING("Building Blocks", "bricks"),
        TOOLS("Tools", "iron_pickaxe"),
        MISC("Miscellaneous", "sticks"),

        SEARCH("Search Items", "torch");

        public final String title;
        public final String iconId;
        CreativeTab(String title, String iconId) {
            this.title = title;
            this.iconId = iconId;
        }
    }

    private static CreativeTab lastTab = CreativeTab.NATURAL;

    private final PlayerInventory playerInventory;
    private final List<Item> allItems;
    private List<Item> filteredItems;

    private int scrollOffset = 0;
    private String searchText = "";
    private CreativeTab currentTab;

    private final List<Slot> creativeGridSlots = new ArrayList<>();
    private final List<Slot> playerInvSlots = new ArrayList<>();

    @SuppressWarnings("this-escape")
    public CreativeContainer(PlayerInventory playerInv) {
        this.playerInventory = playerInv;
        this.currentTab = lastTab; // Merk dir den letzten Tab!
        
        this.allItems = ItemRegistry.getAll().values().stream()
                .filter(item -> {
                    if ("hidden".equalsIgnoreCase(item.getCategory())) return false;
                    if ("technical".equalsIgnoreCase(item.getCategory())) return false;
                    String id = ItemRegistry.getId(item);
                    if (id != null) {
                        if (id.equals("water") || id.equals(Constants.NAMESPACE + ":water")) return false;
                        if (id.equals("structure_void") || id.equals(Constants.NAMESPACE + ":structure_void")) return false;
                        if (id.equals("structure_block") || id.equals(Constants.NAMESPACE + ":structure_block")) return false;
                        if (id.equals("jigsaw") || id.equals(Constants.NAMESPACE + ":jigsaw")) return false;
                    }
                    if (item instanceof de.delautrer.game.items.BlockItem blockItem) {
                        if (blockItem.getBlock() instanceof de.delautrer.game.blocks.WaterBlock) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        this.allItems.sort(java.util.Comparator.comparing(item -> {
            String id = ItemRegistry.getId(item);
            return id != null ? id : "";
        }));

        this.filteredItems = new ArrayList<>(allItems);

        // 1. Creative-Grid Slots (9x6 sichtbare Slots)
        final int rows = 6;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                // Y-Offset auf 34 angepasst (wie im Survival Inventar)
                Slot s = new Slot(null, row * 9 + col, col * 24, 34 + (((rows - 1) - row) * 24));
                addSlot(s);
                creativeGridSlots.add(s);
            }
        }

        // 2. Spieler-Hotbar (Slots 0-8) ganz unten
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, col * 24, 0));
        }
        
        // 3. Delete-Slot
        addSlot(new Slot(null, -1, 9 * 24 + 4, 0));

        // 4. Spieler-Inventar Slots (9x3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                Slot s = new Slot(playerInv, 9 + row * 9 + col, -1000, -1000);
                addSlot(s);
                playerInvSlots.add(s);
            }
        }

        updateFilteredItems();
        updateSlotPositions();
    }

    private void updateSlotPositions() {
        boolean isInv = currentTab == CreativeTab.INVENTORY;
        
        for (Slot s : creativeGridSlots) {
            if (isInv) {
                s.x = -1000;
            } else {
                int row = s.slotIndex / 9;
                int col = s.slotIndex % 9;
                s.x = col * 24;
                s.y = 34 + ((5 - row) * 24);
            }
        }

        for (int i = 0; i < playerInvSlots.size(); i++) {
            Slot s = playerInvSlots.get(i);
            if (!isInv) {
                s.x = -1000;
            } else {
                int row = i / 9;
                int col = i % 9;
                s.x = col * 24;
                // Im Inventory-Tab nutzen wir die unteren 3 Reihen des 6er-Grids
                s.y = 34 + ((2 - row) * 24);
            }
        }
    }

    public void setTab(CreativeTab tab) {
        this.currentTab = tab;
        lastTab = tab; // Speichere für das nächste Mal
        this.scrollOffset = 0;
        updateFilteredItems();
        updateSlotPositions();
    }

    public CreativeTab getCurrentTab() {
        return currentTab;
    }

    public void setSearchText(String text) {
        this.searchText = text.toLowerCase();
        if (currentTab == CreativeTab.SEARCH) {
            this.scrollOffset = 0;
            updateFilteredItems();
        }
    }

    private void updateFilteredItems() {
        if (currentTab == CreativeTab.SEARCH) {
            if (searchText.isEmpty()) {
                filteredItems = new ArrayList<>(allItems);
            } else {
                filteredItems = allItems.stream()
                        .filter(item -> {
                            String id = ItemRegistry.getId(item);
                            return id != null && id.toLowerCase().contains(searchText);
                        })
                        .collect(Collectors.toList());
            }
        } else if (currentTab == CreativeTab.INVENTORY) {
            filteredItems = new ArrayList<>();
        } else {
            String cat = currentTab.name().toLowerCase();
            filteredItems = allItems.stream()
                    .filter(item -> {
                        String itemCat = item.getCategory();
                        if (itemCat == null) return false;
                        itemCat = itemCat.toLowerCase();
                        if (itemCat.equals(cat)) return true;
                        if (cat.equals("building") && (itemCat.equals("building_blocks") || itemCat.equals("decoration"))) return true;
                        if (cat.equals("natural") && (itemCat.equals("decoration") || itemCat.equals("nature"))) return true;
                        return false;
                    })
                    .collect(Collectors.toList());
        }
    }

    public void setScrollOffset(int rows) {
        if (currentTab == CreativeTab.INVENTORY) return;
        int totalRows = (int) Math.ceil(filteredItems.size() / 9.0);
        int maxScroll = Math.max(0, totalRows - 6);
        this.scrollOffset = Math.max(0, Math.min(maxScroll, rows));
    }

    public Item getItemInGrid(int slotIndex) {
        if (currentTab == CreativeTab.INVENTORY) return null;
        int actualIndex = (scrollOffset * 9) + slotIndex;
        if (actualIndex >= 0 && actualIndex < filteredItems.size()) {
            return filteredItems.get(actualIndex);
        }
        return null;
    }

    @Override
    public void clickSlot(Slot slot, int button, ClickType clickType) {
        if (slot.slotIndex == -1) {
            setMouseStack(null);
            return;
        }

        if (slot.inventory == null) {
            Item item = getItemInGrid(slot.slotIndex);
            if (item != null) {
                if (clickType == ClickType.QUICK_MOVE) {
                    ItemStack fullStack = new ItemStack(item, item.getMaxStackSize());
                    pushToPlayerInventory(fullStack);
                } else if (clickType == ClickType.PICKUP || clickType == ClickType.SPLIT) {
                    ItemStack currentMouse = getMouseStack();
                    if (currentMouse == null || currentMouse.type != item) {
                        setMouseStack(new ItemStack(item, 1));
                    } else if (currentMouse.amount < 64) {
                        currentMouse.amount++;
                    }
                }
            }
        } else {
            super.clickSlot(slot, button, clickType);
        }
    }

    @Override
    protected void quickMove(Slot clickedSlot) {
        // Im Creative-Inventar wollen wir das Quick-Moving nur erlauben, wenn beide Regionen sichtbar sind
        // oder wenn von/zu der Hotbar verschoben wird.
        super.quickMove(clickedSlot);
    }

    public int getMaxRows() {
        return (int) Math.ceil(filteredItems.size() / 9.0);
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public List<Item> getAllItems() {
        return allItems;
    }
}