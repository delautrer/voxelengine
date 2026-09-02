package de.delautrer.game.crafting;

import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.registry.Registries;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

public class StonecutterRecipeManagerTest {

    @BeforeAll
    public static void setup() {
        Registries.init();
    }

    @Test
    public void testStonecutterRecipesForCobblestone() {
        Item cobbleItem = ItemRegistry.get("veinstride:cobblestone");
        Assertions.assertNotNull(cobbleItem, "cobblestone item must exist!");

        List<StonecutterRecipe> recipes = StonecutterRecipeManager.getRecipesFor(cobbleItem);
        Assertions.assertTrue(recipes.size() >= 2, "Expected at least 2 recipes for cobblestone in stonecutter, found: " + recipes.size());
    }

    @Test
    public void testStonecutterRecipesForStoneContainsBricks() {
        Item stoneItem = ItemRegistry.get("veinstride:stone");
        Assertions.assertNotNull(stoneItem, "stone item must exist!");

        List<StonecutterRecipe> stoneRecipes = StonecutterRecipeManager.getRecipesFor(stoneItem);
        boolean containsStoneBricks = false;
        for (StonecutterRecipe recipe : stoneRecipes) {
            if (recipe.result != null && recipe.result.type != null) {
                String id = ItemRegistry.getId(recipe.result.type);
                if (id != null && id.contains("stone_bricks")) {
                    containsStoneBricks = true;
                    break;
                }
            }
        }
        Assertions.assertTrue(containsStoneBricks, "Stonecutter recipes for stone should contain stone_bricks!");
    }
}
