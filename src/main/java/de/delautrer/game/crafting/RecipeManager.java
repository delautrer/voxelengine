package de.delautrer.game.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.delautrer.engine.utils.ResourceUtils;
import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RecipeManager {
    private static final List<IRecipe> RECIPES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static final String RECIPE_PATH = "/assets/recipes";

    public static void loadRecipes() {
        RECIPES.clear();
        List<String> fileNames = ResourceUtils.listResourceFolder(RECIPE_PATH);
        for (String fileName : fileNames) {
            if (fileName.endsWith(".json")) {
                loadRecipe(fileName);
            }
        }
        System.out.println("[RecipeManager] Loaded " + RECIPES.size() + " recipes.");
        de.delautrer.game.crafting.FurnaceRecipeManager.init();
    }

    public static void loadRecipe(String filename) {
        try (InputStream is = RecipeManager.class.getResourceAsStream(RECIPE_PATH + "/" + filename)) {
            if (is == null) return;
            JsonObject json = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            String type = json.get("type").getAsString();

            if (type.equals("shaped")) {
                loadShaped(json);
            } else if (type.equals("shapeless")) {
                loadShapeless(json);
            }
        } catch (Exception e) {
            System.err.println("[RecipeManager] Failed to load recipe: " + filename);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadShaped(JsonObject json) {
        JsonArray patternArray = json.getAsJsonArray("pattern");
        int height = patternArray.size();
        int width = patternArray.get(0).getAsString().length();

        JsonObject keysObj = json.getAsJsonObject("keys");
        Map<Character, List<Item>> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : keysObj.entrySet()) {
            keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue()));
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        List<Item>[] ingredients = (List<Item>[]) new List[width * height];
        for (int y = 0; y < height; y++) {
            String row = patternArray.get(y).getAsString();
            for (int x = 0; x < width; x++) {
                char c = row.charAt(x);
                ingredients[x + y * width] = (c == ' ') ? null : keyMap.get(c);
            }
        }

        RECIPES.add(new ShapedRecipe(width, height, ingredients, parseResult(json.getAsJsonObject("result"))));
    }

    private static void loadShapeless(JsonObject json) {
        JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
        List<List<Item>> ingredients = new ArrayList<>();
        for (JsonElement el : ingredientsArray) {
            ingredients.add(parseIngredient(el));
        }
        RECIPES.add(new ShapelessRecipe(ingredients, parseResult(json.getAsJsonObject("result"))));
    }

    private static List<Item> parseIngredient(JsonElement el) {
        List<Item> items = new ArrayList<>();
        if (el.isJsonArray()) {
            for (JsonElement subEl : el.getAsJsonArray()) {
                items.add(ItemRegistry.get(subEl.getAsString()));
            }
        } else {
            items.add(ItemRegistry.get(el.getAsString()));
        }
        return items;
    }

    private static ItemStack parseResult(JsonObject resultObj) {
        Item item = ItemRegistry.get(resultObj.get("item").getAsString());
        int count = resultObj.has("count") ? resultObj.get("count").getAsInt() : 1;
        return new ItemStack(item, count);
    }

    public static ItemStack getMatchingResult(CraftingInventory inv) {
        for (IRecipe recipe : RECIPES) {
            if (recipe.matches(inv)) {
                return recipe.getResult();
            }
        }
        return null;
    }
}