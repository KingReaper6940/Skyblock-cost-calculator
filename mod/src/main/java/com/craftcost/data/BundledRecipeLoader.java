package com.craftcost.data;

import com.craftcost.CraftCostMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a bundled normalized recipe snapshot shipped inside the mod jar.
 */
public class BundledRecipeLoader {

    private static final String RESOURCE_PATH = "/assets/craftcost/data/recipes_fallback.json";

    public int loadInto(RecipeCache recipeCache) {
        try (InputStream stream = BundledRecipeLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                CraftCostMod.LOGGER.warn("[CraftCost] Bundled recipe snapshot not found at {}", RESOURCE_PATH);
                return 0;
            }

            String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray recipes = root.getAsJsonArray("recipes");
            if (recipes == null) {
                return 0;
            }

            int loaded = 0;
            for (JsonElement element : recipes) {
                if (!element.isJsonObject()) {
                    continue;
                }

                RecipeCache.Recipe recipe = parseRecipe(element.getAsJsonObject());
                if (recipe == null) {
                    continue;
                }

                recipeCache.put(recipe.getOutputTag(), recipe);
                loaded++;
            }

            CraftCostMod.LOGGER.info("[CraftCost] Loaded {} recipes from bundled snapshot", loaded);
            return loaded;
        } catch (IOException | IllegalStateException e) {
            CraftCostMod.LOGGER.error("[CraftCost] Failed to load bundled recipe snapshot", e);
            return 0;
        }
    }

    private RecipeCache.Recipe parseRecipe(JsonObject json) {
        if (!json.has("o")) {
            return null;
        }

        String outputId = json.get("o").getAsString();
        int outputCount = json.has("n") ? json.get("n").getAsInt() : 1;
        double coinCost = json.has("m") ? json.get("m").getAsDouble() : 0;
        List<RecipeCache.Ingredient> ingredients = new ArrayList<>();

        if (json.has("g")) {
            for (JsonElement ingredientElement : json.getAsJsonArray("g")) {
                if (!ingredientElement.isJsonArray()) {
                    continue;
                }

                JsonArray ingredient = ingredientElement.getAsJsonArray();
                if (ingredient.size() < 2) {
                    continue;
                }

                ingredients.add(new RecipeCache.Ingredient(
                        ingredient.get(0).getAsString(),
                        ingredient.get(1).getAsInt()
                ));
            }
        }

        if (ingredients.isEmpty() && coinCost <= 0) {
            return null;
        }

        return new RecipeCache.Recipe(outputId, outputCount, ingredients, coinCost);
    }
}
