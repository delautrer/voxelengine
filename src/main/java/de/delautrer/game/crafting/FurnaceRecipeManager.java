package de.delautrer.game.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FurnaceRecipeManager {
    private static final Map<Item, SmeltingRecipe> RECIPES = new HashMap<>();
    private static final Map<Item, Integer> FUELS = new HashMap<>();

    public static class SmeltingRecipe {
        public final ItemStack result;
        public final int cookTime;

        public SmeltingRecipe(ItemStack result, int cookTime) {
            this.result = result;
            this.cookTime = cookTime;
        }
    }

    public static void init() {
        loadRecipes();
        loadFuels();
    }

    private static void loadRecipes() {
        RECIPES.clear();
        try (InputStream is = FurnaceRecipeManager.class.getResourceAsStream("/assets/data/furnace_recipes.json")) {
            if (is == null) {
                System.err.println("[FurnaceRecipeManager] Warning: furnace_recipes.json not found!");
                return;
            }
            Gson gson = new Gson();
            JsonArray array = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                Item input = ItemRegistry.get(obj.get("input").getAsString());
                Item result = ItemRegistry.get(obj.get("result").getAsString());
                int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
                int cookTime = obj.has("cook_time") ? obj.get("cook_time").getAsInt() : 200;
                if (input != null && result != null) {
                    RECIPES.put(input, new SmeltingRecipe(new ItemStack(result, count), cookTime));
                }
            }
        } catch (Exception e) {
            System.err.println("[FurnaceRecipeManager] Error loading furnace recipes:");
            e.printStackTrace();
        }
    }

    private static void loadFuels() {
        FUELS.clear();
        try (InputStream is = FurnaceRecipeManager.class.getResourceAsStream("/assets/data/furnace_fuels.json")) {
            if (is == null) {
                System.err.println("[FurnaceRecipeManager] Warning: furnace_fuels.json not found!");
                return;
            }
            Gson gson = new Gson();
            JsonArray array = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonArray.class);
            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();
                Item item = ItemRegistry.get(obj.get("item").getAsString());
                int burnTime = obj.get("burn_time").getAsInt();
                if (item != null) {
                    FUELS.put(item, burnTime);
                }
            }
        } catch (Exception e) {
            System.err.println("[FurnaceRecipeManager] Error loading furnace fuels:");
            e.printStackTrace();
        }
    }

    public static SmeltingRecipe getRecipe(Item input) {
        if (input == null) return null;
        return RECIPES.get(input);
    }

    public static int getBurnTime(Item item) {
        if (item == null) return 0;
        return FUELS.getOrDefault(item, 0);
    }
}
