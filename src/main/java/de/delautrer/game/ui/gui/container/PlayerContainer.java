package de.delautrer.game.ui.gui.container;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.inventory.BaseInventory;
import de.delautrer.game.crafting.RecipeManager;
import de.delautrer.game.inventory.ResultInventory;
import de.delautrer.game.items.ItemStack;

public class PlayerContainer extends BaseContainer {
    private final PlayerInventory playerInventory;
    private final CraftingInventory craftingInventory;
    private final ResultInventory resultInventory;

    public PlayerContainer(PlayerInventory playerInventory) {
        this.playerInventory = playerInventory;
        this.craftingInventory = new CraftingInventory(2, 2);
        this.resultInventory = new ResultInventory(1);

        // 1. Hotbar ganz unten (Y = 0)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, col * 24, 0));
        }

        // 2. Inventar-Grid (Y invertiert)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                addSlot(new Slot(playerInventory, slotIndex, col * 24, 34 + ((2 - row) * 24)));
            }
        }

        // 3. Crafting Grid (2x2)
        // Die X/Y Werte hier sind Platzhalter. Die exakte Position passen wir im nächsten Schritt im InventoryScreen an!
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                int index = col + row * 2;
                addSlot(new Slot(craftingInventory, index, 120 + col * 24, 120 + (1 - row) * 24) {
                    @Override
                    public void onSlotChanged() {
                        super.onSlotChanged();
                        onCraftMatrixChanged(); // Rezept neu berechnen!
                    }
                });
            }
        }

        // 4. Crafting Result Slot
        addSlot(new CraftingResultSlot(resultInventory, 0, 190, 132, craftingInventory) {
            @Override
            public void onTake() {
                super.onTake();
                onCraftMatrixChanged(); // Wenn herausgenommen, Rezept für verbleibende Items prüfen
            }
        });
    }

    @Override
    public void onContainerClosed() {
        // 1. Zuerst das Mouse-Item zurückgeben (macht BaseContainer)
        super.onContainerClosed();

        // 2. Zutaten aus dem 2x2 Crafting-Feld zurückgeben
        for (int i = 0; i < craftingInventory.getSize(); i++) {
            ItemStack stack = craftingInventory.getStack(i);
            if (stack != null) {
                giveItemOrDrop(stack);
                craftingInventory.setStack(i, null);
            }
        }

        // 3. Output-Slot leeren, damit man nichts gratis bekommt!
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