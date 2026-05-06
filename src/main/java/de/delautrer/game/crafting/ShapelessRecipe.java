package de.delautrer.game.crafting;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemStack;
import java.util.ArrayList;
import java.util.List;


public class ShapelessRecipe implements IRecipe {
    private final List<Item> ingredients;
    private final ItemStack result;

    public ShapelessRecipe(List<Item> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInventory inv) {
        List<Item> itemsToMatch = new ArrayList<>(ingredients);

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack != null && stack.type != null) {
                if (itemsToMatch.contains(stack.type)) {
                    itemsToMatch.remove(stack.type);
                } else {
                    return false;
                }
            }
        }
        return itemsToMatch.isEmpty();
    }

    @Override
    public ItemStack getResult() {
        return new ItemStack(result.type, result.amount);
    }
}