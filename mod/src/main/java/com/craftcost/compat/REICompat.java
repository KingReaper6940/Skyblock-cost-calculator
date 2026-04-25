package com.craftcost.compat;

import com.craftcost.CraftCostMod;
import com.craftcost.data.RecipeCache;
import com.craftcost.tooltip.ItemIdentifier;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REI (Roughly Enough Items) integration.
 * Reads crafting recipes from REI's DisplayRegistry to populate our RecipeCache.
 *
 * NOTE: This requires the player to have REI + a SkyBlock provider mod
 * (Skyblocker or Firmament) installed for SkyBlock recipes to be available.
 *
 * Uses reflection so CraftCost can still load when REI is not installed.
 */
public class REICompat {

    private final RecipeCache recipeCache;

    public REICompat(RecipeCache recipeCache) {
        this.recipeCache = recipeCache;
    }

    /**
     * Check if REI is loaded.
     */
    public static boolean isLoaded() {
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Populate the recipe cache from REI's display registry.
     * Called after the game has fully loaded and REI has initialized.
     *
     * - Get DisplayRegistry instance
     * - Iterate all registered displays
     * - Filter for crafting-type displays
     * - Extract input/output items and counts
     * - Map Minecraft item IDs to SkyBlock item IDs
     * - Store in recipeCache
     */
    public void loadRecipes() {
        if (!isLoaded()) {
            CraftCostMod.LOGGER.warn("[CraftCost] REI not found — recipe data unavailable");
            return;
        }

        CraftCostMod.LOGGER.info("[CraftCost] Loading recipes from REI...");

        try {
            recipeCache.clear();

            Class<?> registryClass = Class.forName("me.shedaniel.rei.api.client.registry.display.DisplayRegistry");
            Class<?> displayClass = Class.forName("me.shedaniel.rei.api.common.display.Display");
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");

            Method getInstance = registryClass.getMethod("getInstance");
            Method getAll = registryClass.getMethod("getAll");
            Method getInputEntries = displayClass.getMethod("getInputEntries");
            Method getOutputEntries = displayClass.getMethod("getOutputEntries");
            Method getValue = entryStackClass.getMethod("getValue");

            Object registry = getInstance.invoke(null);
            Map<?, ?> displaysByCategory = (Map<?, ?>) getAll.invoke(registry);

            int loaded = 0;
            for (Object displays : displaysByCategory.values()) {
                if (!(displays instanceof Iterable<?> iterable)) continue;

                for (Object display : iterable) {
                    LoadedRecipe loadedRecipe = readDisplay(display, getInputEntries, getOutputEntries, getValue);
                    if (loadedRecipe == null || loadedRecipe.ingredients().isEmpty()) continue;

                    recipeCache.put(loadedRecipe.output().itemTag(), new RecipeCache.Recipe(
                            loadedRecipe.output().itemTag(),
                            loadedRecipe.output().count(),
                            loadedRecipe.ingredients()
                    ));
                    loaded++;
                }
            }

            CraftCostMod.LOGGER.info("[CraftCost] Loaded {} recipes from REI", loaded);
            return;
        } catch (ReflectiveOperationException | ClassCastException e) {
            CraftCostMod.LOGGER.error("[CraftCost] Failed to load recipes from REI", e);
        }

        CraftCostMod.LOGGER.info("[CraftCost] Loaded {} recipes from REI", recipeCache.size());
    }

    private LoadedRecipe readDisplay(Object display, Method getInputEntries, Method getOutputEntries, Method getValue)
            throws ReflectiveOperationException {
        List<?> outputs = (List<?>) getOutputEntries.invoke(display);
        StackInfo output = firstStack(outputs, getValue);
        if (output == null) return null;

        List<?> inputs = (List<?>) getInputEntries.invoke(display);
        Map<String, Integer> countsByItem = new LinkedHashMap<>();

        for (Object ingredient : inputs) {
            StackInfo input = firstStack(ingredient, getValue);
            if (input == null || input.itemTag().equals(output.itemTag())) continue;
            countsByItem.merge(input.itemTag(), input.count(), Integer::sum);
        }

        List<RecipeCache.Ingredient> ingredients = new ArrayList<>();
        countsByItem.forEach((itemTag, count) -> ingredients.add(new RecipeCache.Ingredient(itemTag, count)));
        return new LoadedRecipe(output, ingredients);
    }

    private StackInfo firstStack(Object ingredient, Method getValue) throws ReflectiveOperationException {
        if (!(ingredient instanceof Iterable<?> stacks)) return null;

        for (Object entryStack : stacks) {
            Object value = getValue.invoke(entryStack);
            if (!(value instanceof ItemStack stack) || stack.isEmpty()) continue;

            String itemTag = ItemIdentifier.getItemId(stack);
            if (itemTag != null) {
                return new StackInfo(itemTag, Math.max(1, stack.getCount()));
            }
        }

        return null;
    }

    private record StackInfo(String itemTag, int count) {
    }

    private record LoadedRecipe(StackInfo output, List<RecipeCache.Ingredient> ingredients) {
    }
}
