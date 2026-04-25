package com.craftcost.data;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe recipe storage. Maps item tags to their recipe (ingredients list).
 * Recipes are populated from REI's DisplayRegistry when available.
 */
public class RecipeCache {

    private final ConcurrentHashMap<String, List<Recipe>> cache = new ConcurrentHashMap<>();

    public void put(String itemTag, Recipe recipe) {
        cache.compute(itemTag, (key, recipes) -> {
            List<Recipe> next = recipes == null ? new ArrayList<>() : new ArrayList<>(recipes);
            next.add(recipe);
            return next;
        });
    }

    public Recipe get(String itemTag) {
        List<Recipe> recipes = cache.get(itemTag);
        return recipes == null || recipes.isEmpty() ? null : recipes.getFirst();
    }

    public List<Recipe> getAll(String itemTag) {
        List<Recipe> recipes = cache.get(itemTag);
        return recipes == null ? List.of() : recipes;
    }

    public boolean has(String itemTag) {
        return cache.containsKey(itemTag);
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

    public Set<String> collectIngredientTags(String itemTag, int maxDepth) {
        Set<String> tags = ConcurrentHashMap.newKeySet();
        collectIngredientTags(itemTag, maxDepth, tags, ConcurrentHashMap.newKeySet());
        return tags;
    }

    private void collectIngredientTags(String itemTag, int depth, Set<String> tags, Set<String> visited) {
        if (depth <= 0 || !visited.add(itemTag)) return;

        for (Recipe recipe : getAll(itemTag)) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                String ingredientTag = ingredient.getItemTag();
                tags.add(ingredientTag);
                collectIngredientTags(ingredientTag, depth - 1, tags, visited);
            }
        }
    }

    /**
     * Simple recipe representation.
     */
    public static class Recipe {
        private final String outputTag;
        private final int outputCount;
        private final List<Ingredient> ingredients;
        private final double coinCost;

        public Recipe(String outputTag, int outputCount, List<Ingredient> ingredients) {
            this(outputTag, outputCount, ingredients, 0);
        }

        public Recipe(String outputTag, int outputCount, List<Ingredient> ingredients, double coinCost) {
            this.outputTag = outputTag;
            this.outputCount = outputCount;
            this.ingredients = ingredients;
            this.coinCost = coinCost;
        }

        public String getOutputTag() {
            return outputTag;
        }

        public int getOutputCount() {
            return outputCount;
        }

        public List<Ingredient> getIngredients() {
            return ingredients;
        }

        public double getCoinCost() {
            return coinCost;
        }
    }

    /**
     * Single ingredient in a recipe.
     */
    public static class Ingredient {
        private final String itemTag;
        private final int count;

        public Ingredient(String itemTag, int count) {
            this.itemTag = itemTag;
            this.count = count;
        }

        public String getItemTag() {
            return itemTag;
        }

        public int getCount() {
            return count;
        }
    }
}
