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
 * REI integration that tries to read recipe displays without hard-linking the API.
 * Any unreadable displays are skipped so the client never crashes on a bad REI shape.
 */
public class REICompat {

    private final RecipeCache recipeCache;
    private boolean permanentlyDisabled;

    public REICompat(RecipeCache recipeCache) {
        this.recipeCache = recipeCache;
    }

    public static boolean isLoaded() {
        try {
            Class.forName("me.shedaniel.rei.api.client.REIRuntime");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public int loadRecipes() {
        if (permanentlyDisabled) {
            return 0;
        }

        if (!isLoaded()) {
            CraftCostMod.LOGGER.warn("[CraftCost] REI not found - recipe data unavailable");
            return 0;
        }

        CraftCostMod.LOGGER.info("[CraftCost] Loading recipes from REI...");

        try {
            Class<?> registryClass = Class.forName("me.shedaniel.rei.api.client.registry.display.DisplayRegistry");
            Class<?> displayClass = Class.forName("me.shedaniel.rei.api.common.display.Display");
            Class<?> entryStackClass = Class.forName("me.shedaniel.rei.api.common.entry.EntryStack");

            Method getInstance = registryClass.getMethod("getInstance");
            Method getAll = registryClass.getMethod("getAll");
            Method getInputEntries = displayClass.getMethod("getInputEntries");
            Method getOutputEntries = displayClass.getMethod("getOutputEntries");
            Method fallbackGetValue = entryStackClass.getMethod("getValue");

            Object registry = getInstance.invoke(null);
            Map<?, ?> displaysByCategory = (Map<?, ?>) getAll.invoke(registry);

            int loaded = 0;
            for (Object displays : displaysByCategory.values()) {
                if (!(displays instanceof Iterable<?> iterable)) {
                    continue;
                }

                for (Object display : iterable) {
                    LoadedRecipe loadedRecipe;
                    try {
                        loadedRecipe = readDisplay(display, getInputEntries, getOutputEntries, fallbackGetValue);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        continue;
                    }

                    if (loadedRecipe == null || loadedRecipe.ingredients().isEmpty()) {
                        continue;
                    }

                    recipeCache.put(loadedRecipe.output().itemTag(), new RecipeCache.Recipe(
                            loadedRecipe.output().itemTag(),
                            loadedRecipe.output().count(),
                            loadedRecipe.ingredients()
                    ));
                    loaded++;
                }
            }

            CraftCostMod.LOGGER.info("[CraftCost] Loaded {} recipes from REI", loaded);
            return loaded;
        } catch (ReflectiveOperationException | RuntimeException e) {
            permanentlyDisabled = true;
            CraftCostMod.LOGGER.error("[CraftCost] Failed to load recipes from REI", e);
            return 0;
        }
    }

    private LoadedRecipe readDisplay(Object display, Method getInputEntries, Method getOutputEntries, Method fallbackGetValue)
            throws ReflectiveOperationException {
        List<?> outputs = (List<?>) getOutputEntries.invoke(display);
        StackInfo output = firstStack(outputs, fallbackGetValue);
        if (output == null) {
            return null;
        }

        List<?> inputs = (List<?>) getInputEntries.invoke(display);
        Map<String, Integer> countsByItem = new LinkedHashMap<>();

        for (Object ingredient : inputs) {
            StackInfo input = firstStack(ingredient, fallbackGetValue);
            if (input == null || input.itemTag().equals(output.itemTag())) {
                continue;
            }

            countsByItem.merge(input.itemTag(), input.count(), Integer::sum);
        }

        List<RecipeCache.Ingredient> ingredients = new ArrayList<>();
        countsByItem.forEach((itemTag, count) -> ingredients.add(new RecipeCache.Ingredient(itemTag, count)));
        return new LoadedRecipe(output, ingredients);
    }

    private StackInfo firstStack(Object ingredient, Method fallbackGetValue) throws ReflectiveOperationException {
        if (!(ingredient instanceof Iterable<?> stacks)) {
            return null;
        }

        for (Object entryStack : stacks) {
            Object value;
            try {
                value = invokeEntryValue(entryStack, fallbackGetValue);
            } catch (ReflectiveOperationException | RuntimeException e) {
                continue;
            }

            if (!(value instanceof ItemStack stack) || stack.isEmpty()) {
                continue;
            }

            String itemTag = ItemIdentifier.getItemId(stack);
            if (itemTag != null) {
                return new StackInfo(itemTag, Math.max(1, stack.getCount()));
            }
        }

        return null;
    }

    private Object invokeEntryValue(Object entryStack, Method fallbackGetValue) throws ReflectiveOperationException {
        if (entryStack == null) {
            return null;
        }

        if (fallbackGetValue.getDeclaringClass().isInstance(entryStack)) {
            return fallbackGetValue.invoke(entryStack);
        }

        Method dynamicGetValue = entryStack.getClass().getMethod("getValue");
        return dynamicGetValue.invoke(entryStack);
    }

    private record StackInfo(String itemTag, int count) {
    }

    private record LoadedRecipe(StackInfo output, List<RecipeCache.Ingredient> ingredients) {
    }
}
