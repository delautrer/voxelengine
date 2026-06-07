package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.CraftingTableInventory;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.crafting.RecipeManager;
import de.delautrer.game.inventory.ResultInventory;
import de.delautrer.game.items.ItemStack;

public class CraftingTableContainer extends BaseContainer {
    private final PlayerInventory playerInventory;
    private final CraftingTableInventory craftingInventory;
    private final ResultInventory resultInventory;

    @SuppressWarnings("this-escape")
    public CraftingTableContainer(PlayerInventory playerInventory, CraftingTableInventory craftingInventory) {
        this.playerInventory = playerInventory;
        this.craftingInventory = craftingInventory;
        this.resultInventory = new ResultInventory(1);

        // 1. 3x3 Crafting-Grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                addSlot(new Slot(craftingInventory, index, 42 + col * 24, 116 + ((2 - row) * 24)) {
                    @Override
                    public void onSlotChanged() {
                        super.onSlotChanged();
                        onCraftMatrixChanged(); // Rezept neu berechnen!
                    }
                });
            }
        }

        // 2. Crafting Result Slot
        addSlot(new CraftingResultSlot(resultInventory, 0, 150, 140, craftingInventory) {
            @Override
            public void onTake() {
                super.onTake();
                onCraftMatrixChanged(); // Wenn herausgenommen, Rezept für verbleibende Items prüfen
            }
        });

        // 3. Spieler-Grid (Slots 9-35), Mitte
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                addSlot(new Slot(playerInventory, slotIndex, col * 24, 30 + ((2 - row) * 24)));
            }
        }

        // 4. Spieler-Hotbar (Slots 0-8), Unten
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, col * 24, 0));
        }
    }

    @Override
    public void onContainerClosed() {
        // 1. Mouse-Item zurückgeben
        super.onContainerClosed();

        // 2. Zutaten aus dem Crafting-Feld zurückgeben
        for (int i = 0; i < craftingInventory.getSize(); i++) {
            ItemStack stack = craftingInventory.getStack(i);
            if (stack != null) {
                giveItemOrDrop(stack);
                craftingInventory.setStack(i, null);
            }
        }

        // 3. Output-Slot leeren
        resultInventory.setStack(0, null);
    }

    public void onCraftMatrixChanged() {
        ItemStack result = RecipeManager.getMatchingResult(craftingInventory);
        resultInventory.setStack(0, result);
    }

    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }
}
