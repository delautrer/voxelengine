package de.delautrer.game.crafting;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.ItemStack;

public interface IRecipe {
    /**
     * Prüft, ob die Items im Crafting-Feld diesem Rezept entsprechen.
     */
    boolean matches(CraftingInventory inv);

    /**
     * Gibt eine NEUE Instanz des resultierenden ItemStacks zurück.
     */
    ItemStack getResult();
}