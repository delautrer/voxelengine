package de.delautrer.game.crafting;

import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.registry.Registries;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RecipeManagerTest {

    @BeforeAll
    public static void setup() {
        Registries.init();
    }

    @Test
    public void testShapelessMossyCobblestoneRecipe() {
        Item cobble = ItemRegistry.get("veinstride:cobblestone");
        Item moss = ItemRegistry.get("veinstride:moss");
        Assertions.assertNotNull(cobble, "cobblestone item must exist!");
        Assertions.assertNotNull(moss, "moss item must exist!");

        CraftingInventory inv = new CraftingInventory(3, 3);
        inv.setStack(0, new ItemStack(cobble, 1));
        inv.setStack(1, new ItemStack(moss, 1));

        ItemStack result = RecipeManager.getMatchingResult(inv);
        Assertions.assertNotNull(result, "Expected a matching recipe result for cobblestone + moss!");
        Assertions.assertNotNull(result.type, "Recipe result item cannot be null!");

        String resultId = ItemRegistry.getId(result.type);
        Assertions.assertEquals("veinstride:mossy_cobblestone", resultId, "Expected mossy_cobblestone from cobble + moss recipe!");
    }

    @Test
    public void testShapedStairsRecipe() {
        Item cobble = ItemRegistry.get("veinstride:cobblestone");
        Assertions.assertNotNull(cobble, "cobblestone item must exist!");

        CraftingInventory inv = new CraftingInventory(3, 3);
        // Shaped 3x3 pattern for stairs:
        // C . .
        // C C .
        // C C C
        inv.setStack(0, new ItemStack(cobble, 1));
        inv.setStack(3, new ItemStack(cobble, 1));
        inv.setStack(4, new ItemStack(cobble, 1));
        inv.setStack(6, new ItemStack(cobble, 1));
        inv.setStack(7, new ItemStack(cobble, 1));
        inv.setStack(8, new ItemStack(cobble, 1));

        ItemStack result = RecipeManager.getMatchingResult(inv);
        Assertions.assertNotNull(result, "Expected a matching recipe result for cobblestone stairs pattern!");
        Assertions.assertNotNull(result.type, "Recipe result item cannot be null!");

        String resultId = ItemRegistry.getId(result.type);
        Assertions.assertEquals("veinstride:cobblestone_stairs", resultId, "Expected cobblestone_stairs from 6 cobblestone shaped pattern!");
    }
}
