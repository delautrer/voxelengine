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
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RecipeManager {
    private static final List<IRecipe> RECIPES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static final String RECIPE_PATH = "assets/data/veinstride/recipes";

    public static void loadRecipes() {
        RECIPES.clear();
        List<String> fileNames = ResourceUtils.listResources(RECIPE_PATH, ".json");
        for (String fileName : fileNames) {
            loadRecipe(fileName);
        }
        System.out.println("Loaded " + RECIPES.size() + " recipes.");
        de.delautrer.game.crafting.FurnaceRecipeManager.init();
        de.delautrer.game.crafting.StonecutterRecipeManager.init();
    }

    public static void loadRecipe(String filename) {
        String fullPath = RECIPE_PATH + "/" + filename;
        try {
            Reader reader = ResourceUtils.readResourceToReader(fullPath);
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            String type = json.get("type").getAsString();

            if (type.equals("stonecutter")) {
                return;
            } else if (type.equals("shaped")) {
                loadShaped(json, filename);
            } else if (type.equals("shapeless")) {
                loadShapeless(json, filename);
            }
        } catch (Exception e) {
            System.err.println("Failed to load recipe: " + filename);
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new IllegalStateException("Failed to load recipe " + filename, e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadShaped(JsonObject json, String filename) {
        JsonArray patternArray = json.getAsJsonArray("pattern");
        int height = patternArray.size();
        int width = patternArray.get(0).getAsString().length();

        JsonObject keysObj = json.getAsJsonObject("keys");
        Map<Character, List<Item>> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : keysObj.entrySet()) {
            keyMap.put(entry.getKey().charAt(0), parseIngredient(entry.getValue(), filename));
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

    private static void loadShapeless(JsonObject json, String filename) {
        JsonArray ingredientsArray = json.getAsJsonArray("ingredients");
        List<List<Item>> ingredients = new ArrayList<>();
        for (JsonElement el : ingredientsArray) {
            ingredients.add(parseIngredient(el, filename));
        }
        RECIPES.add(new ShapelessRecipe(ingredients, parseResult(json.getAsJsonObject("result"))));
    }

    private static List<Item> parseIngredient(JsonElement el, String filename) {
        List<Item> items = new ArrayList<>();
        if (el == null || el.isJsonNull()) {
            throw new IllegalStateException("Recipe " + filename + " contains null ingredient!");
        }

        if (el.isJsonArray()) {
            for (JsonElement subEl : el.getAsJsonArray()) {
                items.addAll(parseIngredient(subEl, filename));
            }
        } else if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("tag")) {
                String tagStr = obj.get("tag").getAsString();
                de.delautrer.game.registry.Tag<Item> tag = de.delautrer.game.registry.TagRegistry.getItemTag(tagStr);
                if (tag != null && !tag.getElements().isEmpty()) {
                    items.addAll(tag.getElements());
                } else {
                    throw new IllegalStateException("Recipe " + filename + " references unknown or empty item tag: " + tagStr);
                }
            } else if (obj.has("item")) {
                Item item = ItemRegistry.get(obj.get("item").getAsString());
                if (item != null) {
                    items.add(item);
                } else {
                    throw new IllegalStateException("Recipe " + filename + " references unknown item: " + obj.get("item").getAsString());
                }
            }
        } else if (el.isJsonPrimitive()) {
            String str = el.getAsString();
            if (str.startsWith("#")) {
                de.delautrer.game.registry.Tag<Item> tag = de.delautrer.game.registry.TagRegistry.getItemTag(str.substring(1));
                if (tag != null && !tag.getElements().isEmpty()) {
                    items.addAll(tag.getElements());
                } else {
                    throw new IllegalStateException("Recipe " + filename + " references unknown or empty item tag: " + str);
                }
            } else {
                Item item = ItemRegistry.get(str);
                if (item != null) {
                    items.add(item);
                } else {
                    throw new IllegalStateException("Recipe " + filename + " references unknown item: " + str);
                }
            }
        }

        if (items.isEmpty()) {
            throw new IllegalStateException("Recipe " + filename + " has an ingredient with 0 resolved items!");
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