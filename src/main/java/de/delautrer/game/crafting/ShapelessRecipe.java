package de.delautrer.game.crafting;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ShapelessRecipe implements IRecipe {
    private final List<List<Item>> ingredients;
    private final ItemStack result;

    public ShapelessRecipe(List<List<Item>> ingredients, ItemStack result) {
        this.ingredients = ingredients;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInventory inv) {
        List<List<Item>> toMatch = new ArrayList<>(ingredients);
        int invItemCount = 0;

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack != null && stack.type != null) {
                invItemCount++;
                boolean matched = false;
                for (int j = 0; j < toMatch.size(); j++) {
                    if (toMatch.get(j).contains(stack.type)) {
                        toMatch.remove(j);
                        matched = true;
                        break;
                    }
                }
                if (!matched) return false;
            }
        }
        return toMatch.isEmpty() && invItemCount == ingredients.size();
    }

    @Override
    public ItemStack getResult() {
        return new ItemStack(result.type, result.amount);
    }
}