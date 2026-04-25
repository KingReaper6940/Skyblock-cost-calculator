package com.craftcost.data;

import com.craftcost.CraftCostMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads SkyBlock recipes from the local repo cache downloaded by other SkyBlock mods.
 */
public class RepoRecipeLoader {

    private static final String RECIPE_CACHE_PATH = "skyblock-repo-cache/recipes.min.json";

    public int loadInto(RecipeCache recipeCache) {
        Path path = FabricLoader.getInstance().getGameDir().resolve(RECIPE_CACHE_PATH);
        if (!Files.exists(path)) {
            CraftCostMod.LOGGER.warn("[CraftCost] Local recipe cache not found at {}", path);
            return 0;
        }

        try {
            JsonArray recipes = JsonParser.parseString(Files.readString(path)).getAsJsonArray();
            int loaded = 0;

            for (JsonElement element : recipes) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject recipe = element.getAsJsonObject();
                RecipeCache.Recipe parsed = parseRecipe(recipe);
                if (parsed == null) {
                    continue;
                }

                recipeCache.put(parsed.getOutputTag(), parsed);
                loaded++;
            }

            CraftCostMod.LOGGER.info("[CraftCost] Loaded {} recipes from local repo cache", loaded);
            return loaded;
        } catch (IOException | IllegalStateException e) {
            CraftCostMod.LOGGER.error("[CraftCost] Failed to load local recipe cache", e);
            return 0;
        }
    }

    private RecipeCache.Recipe parseRecipe(JsonObject recipe) {
        if (!recipe.has("type") || !recipe.has("result")) {
            return null;
        }

        String type = recipe.get("type").getAsString();
        JsonObject result = recipe.getAsJsonObject("result");
        if (!result.has("id")) {
            return null;
        }

        String outputId = result.get("id").getAsString();
        int outputCount = result.has("count") ? result.get("count").getAsInt() : 1;

        return switch (type) {
            case "crafting" -> parseCraftingRecipe(recipe, outputId, outputCount);
            case "forge" -> parseForgeRecipe(recipe, outputId, outputCount);
            default -> null;
        };
    }

    private RecipeCache.Recipe parseCraftingRecipe(JsonObject recipe, String outputId, int outputCount) {
        if (!recipe.has("keys") || !recipe.has("pattern")) {
            return null;
        }

        JsonArray keys = recipe.getAsJsonArray("keys");
        JsonArray pattern = recipe.getAsJsonArray("pattern");
        Map<Integer, RecipeCache.Ingredient> keyMap = new LinkedHashMap<>();

        for (int i = 0; i < keys.size(); i++) {
            JsonObject key = keys.get(i).getAsJsonObject();
            if (!key.has("id")) {
                continue;
            }

            int count = key.has("count") ? key.get("count").getAsInt() : 1;
            keyMap.put(i, new RecipeCache.Ingredient(key.get("id").getAsString(), count));
        }

        Map<String, Integer> merged = new LinkedHashMap<>();
        for (JsonElement cell : pattern) {
            int keyIndex = cell.getAsInt();
            if (keyIndex < 0) {
                continue;
            }

            RecipeCache.Ingredient ingredient = keyMap.get(keyIndex);
            if (ingredient == null) {
                continue;
            }

            merged.merge(ingredient.getItemTag(), ingredient.getCount(), Integer::sum);
        }

        if (merged.isEmpty()) {
            return null;
        }

        List<RecipeCache.Ingredient> ingredients = new ArrayList<>();
        merged.forEach((itemId, count) -> ingredients.add(new RecipeCache.Ingredient(itemId, count)));
        return new RecipeCache.Recipe(outputId, outputCount, ingredients);
    }

    private RecipeCache.Recipe parseForgeRecipe(JsonObject recipe, String outputId, int outputCount) {
        if (!recipe.has("inputs")) {
            return null;
        }

        JsonArray inputs = recipe.getAsJsonArray("inputs");
        List<RecipeCache.Ingredient> ingredients = new ArrayList<>();
        for (JsonElement inputElement : inputs) {
            if (!inputElement.isJsonObject()) {
                continue;
            }

            JsonObject input = inputElement.getAsJsonObject();
            if (!input.has("id")) {
                continue;
            }

            int count = input.has("count") ? input.get("count").getAsInt() : 1;
            ingredients.add(new RecipeCache.Ingredient(input.get("id").getAsString(), count));
        }

        if (ingredients.isEmpty()) {
            return null;
        }

        double coinCost = recipe.has("coins") ? recipe.get("coins").getAsDouble() : 0;
        return new RecipeCache.Recipe(outputId, outputCount, ingredients, coinCost);
    }
}
