package com.craftcost.data;

import com.craftcost.CraftCostMod;

import java.util.HashMap;
import java.util.Map;

/**
 * Recursive craft cost calculator.
 * For each item, determines the cheapest way to obtain it:
 * either buy it directly or craft it from ingredients (recursively).
 */
public class CraftCostEngine {

    private final PriceCache priceCache;
    private final RecipeCache recipeCache;

    // memoization to avoid recalculating the same item
    private final Map<String, CraftResult> resultCache = new HashMap<>();

    public CraftCostEngine(PriceCache priceCache, RecipeCache recipeCache) {
        this.priceCache = priceCache;
        this.recipeCache = recipeCache;
    }

    /**
     * Calculate the cheapest way to obtain an item.
     * Returns null if no price data is available.
     */
    public CraftResult calculate(String itemTag) {
        // check memo cache
        CraftResult cached = resultCache.get(itemTag);
        if (cached != null && !cached.isStale()) {
            return cached;
        }

        CraftResult result = doCalculate(itemTag, 0);
        if (result != null) {
            resultCache.put(itemTag, result);
        }
        return result;
    }

    private CraftResult doCalculate(String itemTag, int depth) {
        // prevent infinite recursion
        if (depth > 10) {
            return null;
        }

        double buyPrice = priceCache.getBuyPrice(itemTag);
        RecipeCache.Recipe recipe = recipeCache.get(itemTag);

        // no recipe — can only buy
        if (recipe == null) {
            if (buyPrice > 0) {
                return CraftResult.buyOnly(itemTag, buyPrice);
            }
            return null; // no data at all
        }

        // has recipe — calculate craft cost
        double craftCost = 0;
        boolean allIngredientsAvailable = true;
        Map<String, IngredientCost> breakdown = new HashMap<>();

        for (RecipeCache.Ingredient ingredient : recipe.getIngredients()) {
            String ingTag = ingredient.getItemTag();
            int count = ingredient.getCount();

            // recursively find cheapest way to get this ingredient
            CraftResult ingResult = doCalculate(ingTag, depth + 1);
            double ingPrice;

            if (ingResult != null) {
                ingPrice = ingResult.getCheapestPrice() * count;
            } else {
                double ingBuyPrice = priceCache.getBuyPrice(ingTag);
                if (ingBuyPrice > 0) {
                    ingPrice = ingBuyPrice * count;
                } else {
                    allIngredientsAvailable = false;
                    break;
                }
            }

            craftCost += ingPrice;
            breakdown.put(ingTag, new IngredientCost(ingTag, count, ingPrice));
        }

        if (!allIngredientsAvailable) {
            // can't calculate craft cost — just show buy price
            if (buyPrice > 0) {
                return CraftResult.buyOnly(itemTag, buyPrice);
            }
            return null;
        }

        // divide by output count (some recipes produce multiple items)
        craftCost = craftCost / recipe.getOutputCount();

        return new CraftResult(itemTag, buyPrice, craftCost, breakdown);
    }

    /**
     * Clear the memo cache (called on price refresh).
     */
    public void invalidate() {
        resultCache.clear();
    }

    // -- result types --

    public static class CraftResult {
        private final String itemTag;
        private final double buyPrice; // -1 if not available
        private final double craftCost; // -1 if not available
        private final Map<String, IngredientCost> breakdown;
        private final long timestamp;

        CraftResult(String itemTag, double buyPrice, double craftCost, Map<String, IngredientCost> breakdown) {
            this.itemTag = itemTag;
            this.buyPrice = buyPrice;
            this.craftCost = craftCost;
            this.breakdown = breakdown;
            this.timestamp = System.currentTimeMillis();
        }

        static CraftResult buyOnly(String itemTag, double buyPrice) {
            return new CraftResult(itemTag, buyPrice, -1, Map.of());
        }

        public double getCheapestPrice() {
            if (craftCost > 0 && buyPrice > 0) return Math.min(craftCost, buyPrice);
            if (craftCost > 0) return craftCost;
            return buyPrice;
        }

        public boolean isCheaperToCraft() {
            return craftCost > 0 && buyPrice > 0 && craftCost < buyPrice;
        }

        public boolean isCheaperToBuy() {
            return craftCost > 0 && buyPrice > 0 && buyPrice < craftCost;
        }

        public double getSavings() {
            if (craftCost > 0 && buyPrice > 0) {
                return Math.abs(buyPrice - craftCost);
            }
            return 0;
        }

        public boolean hasCraftCost() {
            return craftCost > 0;
        }

        public boolean hasBuyPrice() {
            return buyPrice > 0;
        }

        public double getBuyPrice() {
            return buyPrice;
        }

        public double getCraftCost() {
            return craftCost;
        }

        public Map<String, IngredientCost> getBreakdown() {
            return breakdown;
        }

        boolean isStale() {
            return System.currentTimeMillis() - timestamp > 60_000; // 1 min cache
        }
    }

    public static class IngredientCost {
        private final String itemTag;
        private final int count;
        private final double totalCost;

        public IngredientCost(String itemTag, int count, double totalCost) {
            this.itemTag = itemTag;
            this.count = count;
            this.totalCost = totalCost;
        }

        public String getItemTag() {
            return itemTag;
        }

        public int getCount() {
            return count;
        }

        public double getTotalCost() {
            return totalCost;
        }
    }
}
