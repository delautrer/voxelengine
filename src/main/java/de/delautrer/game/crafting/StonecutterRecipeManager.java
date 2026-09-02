package de.delautrer.game.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import de.delautrer.game.registry.NamespacedKey;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StonecutterRecipeManager {
    private static final Map<Item, List<StonecutterRecipe>> RECIPES = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final String STONECUTTER_RECIPE_PATH = "assets/data/veinstride/recipes/stonecutter";

    public static void init() {
        RECIPES.clear();
        List<String> files = ResourceUtils.listResources(STONECUTTER_RECIPE_PATH, ".json");
        for (String fileName : files) {
            loadRecipe(fileName);
        }
        System.out.println("Loaded Stonecutter recipes for " + RECIPES.size() + " input items.");
    }

    private static void loadRecipe(String filename) {
        String fullPath = STONECUTTER_RECIPE_PATH + "/" + filename;
        try {
            Reader reader = ResourceUtils.readResourceToReader(fullPath);
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            String ingredientStr = json.get("ingredient").getAsString();
            Item ingredient = getItem(ingredientStr);
            if (ingredient == null) {
                throw new IllegalStateException("Stonecutter recipe " + filename + " references unknown ingredient item: " + ingredientStr);
            }

            JsonObject resultObj = json.getAsJsonObject("result");
            String resultStr = resultObj.get("item").getAsString();
            int count = resultObj.has("count") ? resultObj.get("count").getAsInt() : 1;
            Item resultItem = getItem(resultStr);
            if (resultItem == null) {
                throw new IllegalStateException("Stonecutter recipe " + filename + " references unknown result item: " + resultStr);
            }

            StonecutterRecipe recipe = new StonecutterRecipe(ingredient, new ItemStack(resultItem, count));
            RECIPES.computeIfAbsent(ingredient, k -> new ArrayList<>()).add(recipe);
        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new IllegalStateException("Failed to load stonecutter recipe: " + filename, e);
        }
    }

    private static Item getItem(String name) {
        if (name == null) return null;
        if (!name.contains(":")) name = "veinstride:" + name;
        return ItemRegistry.get(name);
    }

    public static List<StonecutterRecipe> getRecipesFor(Item input) {
        if (input == null) return new ArrayList<>();
        List<StonecutterRecipe> list = RECIPES.get(input);
        if (list != null && !list.isEmpty()) {
            return new ArrayList<>(list);
        }

        String inputId = ItemRegistry.getId(input);
        if (inputId != null) {
            for (Map.Entry<Item, List<StonecutterRecipe>> entry : RECIPES.entrySet()) {
                String keyId = ItemRegistry.getId(entry.getKey());
                if (inputId.equals(keyId)) {
                    return new ArrayList<>(entry.getValue());
                }
            }
        }
        return new ArrayList<>();
    }
}
