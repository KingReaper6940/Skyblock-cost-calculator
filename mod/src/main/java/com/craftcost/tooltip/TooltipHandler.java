package com.craftcost.tooltip;

import com.craftcost.config.CraftCostConfig;
import com.craftcost.api.PriceFetcher;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.PriceEntry;
import com.craftcost.data.RecipeCache;
import com.craftcost.util.NumberFormatter;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

/**
 * Hooks into Fabric's ItemTooltipCallback to inject craft cost data.
 * Always-on — displays craft cost and lowest BIN on every SkyBlock item.
 */
public class TooltipHandler {

    private final PriceCache priceCache;
    private final CraftCostEngine craftCostEngine;
    private final RecipeCache recipeCache;
    private final PriceFetcher priceFetcher;
    private final CraftCostConfig config;

    public TooltipHandler(PriceCache priceCache, CraftCostEngine craftCostEngine, RecipeCache recipeCache,
                          PriceFetcher priceFetcher, CraftCostConfig config) {
        this.priceCache = priceCache;
        this.craftCostEngine = craftCostEngine;
        this.recipeCache = recipeCache;
        this.priceFetcher = priceFetcher;
        this.config = config;
    }

    public void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!config.isEnabled()) return;

            String itemId = ItemIdentifier.getItemId(stack);
            if (itemId == null) return;

            priceFetcher.requestPrice(itemId);
            for (String ingredientId : recipeCache.collectIngredientTags(itemId, 10)) {
                priceFetcher.requestPrice(ingredientId);
            }

            PriceEntry price = priceCache.get(itemId);
            CraftCostEngine.CraftResult craft = craftCostEngine.calculate(itemId);

            if (price == null && craft == null) {
                // no data yet — show loading
                lines.add(Component.empty());
                lines.add(Component.literal("§8§m                    §r"));
                lines.add(Component.literal(" §7CraftCost: §8Loading..."));
                lines.add(Component.literal("§8§m                    §r"));
                return;
            }

            // separator
            lines.add(Component.empty());
            lines.add(Component.literal("§8§m                    §r"));

            // lowest BIN
            if (price != null && price.hasBin()) {
                lines.add(Component.literal(" §7Lowest BIN: §6§l" +
                        NumberFormatter.format(price.getLowestBin())));
            } else if (price != null && price.hasBazaar()) {
                lines.add(Component.literal(" §7Bazaar Buy: §6§l" +
                        NumberFormatter.format(price.getBazaarBuyPrice())));
            }

            // craft cost
            if (craft != null && craft.hasCraftCost()) {
                boolean cheaper = craft.isCheaperToCraft();
                String color = cheaper ? "§a" : "§c";
                lines.add(Component.literal(" §7Raw Craft Cost: " + color + "§l" +
                        NumberFormatter.format(craft.getCraftCost())));

                // buy vs craft hint
                if (config.isShowSavings() && craft.hasBuyPrice()) {
                    if (cheaper) {
                        lines.add(Component.literal("   §7§o⮕ §aCheaper to craft! Save §e" +
                                NumberFormatter.format(craft.getSavings())));
                    } else if (craft.isCheaperToBuy()) {
                        lines.add(Component.literal("   §7§o⮕ §6Cheaper to buy!"));
                    }
                }

                // ingredient breakdown
                if (config.isShowBreakdown() && !craft.getBreakdown().isEmpty()) {
                    var entries = craft.getBreakdown().entrySet().stream().toList();
                    for (int i = 0; i < entries.size(); i++) {
                        var entry = entries.get(i);
                        var ing = entry.getValue();
                        String prefix = (i == entries.size() - 1) ? "  §8└ " : "  §8├ ";
                        lines.add(Component.literal(prefix + "§f" + ing.getCount() + "x " +
                                ing.getItemTag() + " §8- §e" +
                                NumberFormatter.format(ing.getTotalCost())));
                    }
                }
            }

            // closing separator
            lines.add(Component.literal("§8§m                    §r"));
        });
    }
}
