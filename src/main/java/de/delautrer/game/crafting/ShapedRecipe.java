package de.delautrer.game.crafting;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemStack;

public class ShapedRecipe implements IRecipe {
    private final int recipeWidth;
    private final int recipeHeight;
    private final Item[] ingredients;
    private final ItemStack result;

    public ShapedRecipe(int width, int height, Item[] ingredients, ItemStack result) {
        this.recipeWidth = width;
        this.recipeHeight = height;
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInventory inv) {
        for (int x = 0; x <= inv.getWidth() - this.recipeWidth; x++) {
            for (int y = 0; y <= inv.getHeight() - this.recipeHeight; y++) {
                if (checkMatch(inv, x, y, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean checkMatch(CraftingInventory inv, int startX, int startY, boolean mirror) {
        for (int x = 0; x < inv.getWidth(); x++) {
            for (int y = 0; y < inv.getHeight(); y++) {
                int subX = x - startX;
                int subY = y - startY;
                Item targetItem = null;

                if (subX >= 0 && subY >= 0 && subX < this.recipeWidth && subY < this.recipeHeight) {
                    if (mirror) {
                        targetItem = ingredients[this.recipeWidth - subX - 1 + subY * this.recipeWidth];
                    } else {
                        targetItem = ingredients[subX + subY * this.recipeWidth];
                    }
                }

                ItemStack invStack = inv.getStack(x + y * inv.getWidth());
                Item invItem = (invStack != null) ? invStack.type : null;

                if (targetItem != invItem) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack getResult() {
        return new ItemStack(result.type, result.amount);
    }
}