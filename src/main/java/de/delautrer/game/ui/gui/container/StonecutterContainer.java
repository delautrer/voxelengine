package de.delautrer.game.ui.gui.container;

import de.delautrer.game.crafting.StonecutterRecipe;
import de.delautrer.game.crafting.StonecutterRecipeManager;
import de.delautrer.game.inventory.PlayerInventory;
import de.delautrer.game.inventory.ResultInventory;
import de.delautrer.game.inventory.StonecutterInventory;
import de.delautrer.game.items.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StonecutterContainer extends BaseContainer {
    private final PlayerInventory playerInventory;
    private final StonecutterInventory inputInventory;
    private final ResultInventory resultInventory;

    private List<StonecutterRecipe> currentRecipes = new ArrayList<>();
    private int selectedRecipeIndex = -1;

    @SuppressWarnings("this-escape")
    public StonecutterContainer(PlayerInventory playerInventory, StonecutterInventory inputInventory) {
        this.playerInventory = playerInventory;
        this.inputInventory = inputInventory;
        this.resultInventory = new ResultInventory(1);

        // 1. Input-Slot (Slot index 0 in inputInventory, oben links)
        addSlot(new Slot(inputInventory, 0, 12, 162) {
            @Override
            public void onSlotChanged() {
                super.onSlotChanged();
                onInputMatrixChanged();
            }
        });

        // 2. Output / Result Slot (Slot index 0 in resultInventory, unten links)
        addSlot(new CraftingResultSlot(resultInventory, 0, 12, 110, null) {
            @Override
            public boolean isItemValid(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake() {
                // Decrement input by 1
                ItemStack inputStack = inputInventory.getStack(0);
                if (inputStack != null) {
                    inputStack.amount--;
                    if (inputStack.amount <= 0) {
                        inputInventory.setStack(0, null);
                    }
                }
                onInputMatrixChanged();
            }
        });

        // 3. Player Grid (Slots 9-35)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = 9 + (row * 9) + col;
                addSlot(new Slot(playerInventory, slotIndex, col * 24, 30 + ((2 - row) * 24)));
            }
        }

        // 4. Player Hotbar (Slots 0-8)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, col * 24, 0));
        }

        onInputMatrixChanged();
    }

    public void onInputMatrixChanged() {
        ItemStack inputStack = inputInventory.getStack(0);
        if (inputStack != null && inputStack.type != null && inputStack.amount > 0) {
            currentRecipes = StonecutterRecipeManager.getRecipesFor(inputStack.type);
            if (!currentRecipes.isEmpty()) {
                if (selectedRecipeIndex < 0 || selectedRecipeIndex >= currentRecipes.size()) {
                    selectedRecipeIndex = 0;
                }
            } else {
                selectedRecipeIndex = -1;
            }
        } else {
            currentRecipes = new ArrayList<>();
            selectedRecipeIndex = -1;
        }
        updateResult();
    }

    public void selectRecipe(int index) {
        if (index >= 0 && index < currentRecipes.size()) {
            this.selectedRecipeIndex = index;
            updateResult();
        }
    }

    private void updateResult() {
        ItemStack inputStack = inputInventory.getStack(0);
        if (inputStack != null && selectedRecipeIndex >= 0 && selectedRecipeIndex < currentRecipes.size()) {
            StonecutterRecipe recipe = currentRecipes.get(selectedRecipeIndex);
            if (recipe != null && recipe.result != null) {
                resultInventory.setStack(0, new ItemStack(recipe.result.type, recipe.result.amount));
                return;
            }
        }
        resultInventory.setStack(0, null);
    }

    @Override
    public void onContainerClosed() {
        super.onContainerClosed();

        // Zutaten aus dem Input-Feld an Spieler zurückgeben
        ItemStack inputStack = inputInventory.getStack(0);
        if (inputStack != null) {
            giveItemOrDrop(inputStack);
            inputInventory.setStack(0, null);
        }

        // Output-Slot leeren (Output verwerfen, nicht zurückgeben)
        resultInventory.setStack(0, null);
    }

    public List<StonecutterRecipe> getCurrentRecipes() {
        return currentRecipes;
    }

    public int getSelectedRecipeIndex() {
        return selectedRecipeIndex;
    }

    public PlayerInventory getPlayerInventory() {
        return playerInventory;
    }
}
