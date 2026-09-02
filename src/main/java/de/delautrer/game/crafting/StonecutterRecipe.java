package de.delautrer.game.crafting;

import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemStack;

public class StonecutterRecipe {
    public final Item ingredient;
    public final ItemStack result;

    public StonecutterRecipe(Item ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }
}
