package de.delautrer.game.crafting;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.delautrer.game.inventory.CraftingInventory;
import de.delautrer.game.items.Item;
import de.delautrer.game.items.ItemRegistry;
import de.delautrer.game.items.ItemStack;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class RecipeManager {
    private static final List<IRecipe> RECIPES = new ArrayList<>();
    private static final Gson GSON = new Gson();
    private static final String RECIPE_PATH = "assets/recipes";

    public static void loadRecipes() {
        RECIPES.clear();

        try {
            // Wir suchen den "Ordner" im Classpath
            URL url = RecipeManager.class.getResource("/" + RECIPE_PATH);

            if (url == null) {
                System.err.println("Recipe directory not found: " + RECIPE_PATH);
                return;
            }

            if (url.getProtocol().equals("file")) {
                // --- IDE MODUS (Normales Dateisystem) ---
                File folder = new File(url.toURI());
                File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
                if (files != null) {
                    for (File f : files) {
                        loadRecipe(f.getName());
                    }
                }
            } else if (url.getProtocol().equals("jar")) {
                // --- JAR MODUS (Innerhalb der .jar Datei) ---
                // Der URL-Pfad sieht in einer JAR so aus: jar:file:/Pfad/zu/VoxelEngine.jar!/assets/recipes
                String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());

                try (ZipInputStream zip = new ZipInputStream(new FileInputStream(jarPath))) {
                    ZipEntry entry;
                    while ((entry = zip.getNextEntry()) != null) {
                        String name = entry.getName();
                        // Wir prüfen, ob der Pfad im ZIP mit unserem Rezeptordner beginnt
                        if (name.startsWith(RECIPE_PATH + "/") && name.endsWith(".json")) {
                            String fileName = name.substring(RECIPE_PATH.length() + 1);
                            // Nur Dateien im Hauptordner laden (keine Unterordner)
                            if (!fileName.contains("/")) {
                                loadRecipe(fileName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to automatically load recipes from " + RECIPE_PATH);
            e.printStackTrace();
        }

        System.out.println("[RecipeManager] Loaded " + RECIPES.size() + " recipes.");
    }

    public static void loadRecipe(String filename) {
        try (InputStream is = RecipeManager.class.getResourceAsStream("/" + RECIPE_PATH + "/" + filename)) {
            if (is == null) {
                System.err.println("Recipe file not found: " + filename);
                return;
            }
            JsonObject json = GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            String type = json.get("type").getAsString();

            if (type.equals("shaped")) {
                loadShaped(json);
            } else if (type.equals("shapeless")) {
                loadShapeless(json);
            }
        } catch (Exception e) {
            System.err.println("Failed to load recipe: " + filename);
            e.printStackTrace();
        }
    }

    private static void loadShaped(JsonObject json) {
        JsonArray patternArray = json.getAsJsonArray("pattern");
        int height = patternArray.size();
        int width = patternArray.get(0).getAsString().length();

        JsonObject keysObj = json.getAsJsonObject("keys");
        Map<Character, Item> keyMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : keysObj.entrySet()) {
            keyMap.put(entry.getKey().charAt(0), ItemRegistry.get(entry.getValue().getAsString()));
        }

        Item[] ingredients = new Item[width * height];
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
        List<Item> ingredients = new ArrayList<>();
        for (JsonElement el : ingredientsArray) {
            ingredients.add(ItemRegistry.get(el.getAsString()));
        }
        RECIPES.add(new ShapelessRecipe(ingredients, parseResult(json.getAsJsonObject("result"))));
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