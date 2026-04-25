package com.craftcost.tooltip;

import com.craftcost.CraftCostClient;
import com.craftcost.api.PriceFetcher;
import com.craftcost.config.CraftCostConfig;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.PriceEntry;
import com.craftcost.data.RecipeCache;
import com.craftcost.util.NumberFormatter;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Adds CraftCost tooltip lines after the player holds hover on one item.
 */
public class TooltipHandler {

    private static final long HOLD_TO_CALCULATE_MS = 10_000L;
    private static final String GRAY_RULE = "\u00A78\u00A7m                    \u00A7r";

    private final PriceCache priceCache;
    private final CraftCostEngine craftCostEngine;
    private final RecipeCache recipeCache;
    private final PriceFetcher priceFetcher;
    private final CraftCostConfig config;
    private String hoveredItemId;
    private String requestedItemId;
    private long hoverStartedAt;

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
            if (!config.isEnabled()) {
                resetHoverTimer();
                return;
            }

            String itemId = ItemIdentifier.getItemId(stack);
            if (itemId == null) {
                resetHoverTimer();
                return;
            }

            long hoveredForMs = updateHoverTimer(itemId);
            if (hoveredForMs < HOLD_TO_CALCULATE_MS) {
                addWaitTooltip(lines, HOLD_TO_CALCULATE_MS - hoveredForMs);
                return;
            }

            requestPricesForHover(itemId);

            PriceEntry price = priceCache.get(itemId);
            CraftCostEngine.CraftResult craft = craftCostEngine.calculate(itemId);
            boolean hasKnownRecipe = recipeCache.has(itemId);
            boolean craftTreeReady = !hasKnownRecipe || craftCostEngine.isFullyResolved(itemId);

            if (!hasKnownRecipe && CraftCostClient.getInstance() != null) {
                CraftCostClient.getInstance().requestRecipeFallbackLoad();
            }

            if (hasKnownRecipe && (!craftTreeReady || craft == null || !craft.hasCraftCost())) {
                addLoadingTooltip(lines, "Calculating direct craft cost...", "Waiting for direct ingredient pricing");
                return;
            }

            if (price == null && craft == null) {
                addLoadingTooltip(lines, "Calculating...", null);
                return;
            }

            lines.add(Component.empty());
            lines.add(Component.literal(GRAY_RULE));

            if (price != null && price.hasBin()) {
                lines.add(Component.literal(" \u00A77Lowest BIN: \u00A76\u00A7l" +
                        NumberFormatter.format(price.getLowestBin())));
            } else if (price != null && price.hasBazaar()) {
                lines.add(Component.literal(" \u00A77Bazaar Buy: \u00A76\u00A7l" +
                        NumberFormatter.format(price.getBazaarBuyPrice())));
            }

            if (craft != null && craft.hasCraftCost()) {
                boolean cheaper = craft.isCheaperToCraft();
                String color = cheaper ? "\u00A7a" : "\u00A7c";
                lines.add(Component.literal(" \u00A77Raw Craft Cost: " + color + "\u00A7l" +
                        NumberFormatter.format(craft.getCraftCost())));

                if (config.isShowSavings() && craft.hasBuyPrice()) {
                    if (cheaper) {
                        lines.add(Component.literal("   \u00A77Reason: \u00A7aCrafting saves \u00A7e" +
                                NumberFormatter.format(craft.getSavings())));
                    } else if (craft.isCheaperToBuy()) {
                        lines.add(Component.literal("   \u00A77Reason: \u00A7cCraft cost is higher, buy Lowest BIN"));
                    }
                }

                if (config.isShowBreakdown() && !craft.getBreakdown().isEmpty()) {
                    var entries = craft.getBreakdown().entrySet().stream().toList();
                    for (int i = 0; i < entries.size(); i++) {
                        var entry = entries.get(i);
                        var ing = entry.getValue();
                        String prefix = (i == entries.size() - 1) ? "  \u00A78- " : "  \u00A78| ";
                        lines.add(Component.literal(prefix + "\u00A7f" + ing.getCount() + "x " +
                                ing.getItemTag() + " \u00A78- \u00A7e" +
                                NumberFormatter.format(ing.getTotalCost())));
                    }
                }
            } else if (craft != null && craft.hasBuyPrice()) {
                lines.add(Component.literal(" \u00A77CraftCost: \u00A78No known recipe data"));
            }

            lines.add(Component.literal(GRAY_RULE));
        });
    }

    private long updateHoverTimer(String itemId) {
        long now = System.currentTimeMillis();
        if (!itemId.equals(hoveredItemId)) {
            hoveredItemId = itemId;
            requestedItemId = null;
            hoverStartedAt = now;
            return 0;
        }

        return now - hoverStartedAt;
    }

    private void addWaitTooltip(List<Component> lines, long remainingMs) {
        long remainingSeconds = Math.max(1, (remainingMs + 999) / 1000);
        lines.add(Component.empty());
        lines.add(Component.literal(GRAY_RULE));
        lines.add(Component.literal(" \u00A77CraftCost: \u00A78Hold hover " + remainingSeconds + "s"));
        lines.add(Component.literal(" \u00A78No price checks until then"));
        lines.add(Component.literal(GRAY_RULE));
    }

    private void addLoadingTooltip(List<Component> lines, String title, String detail) {
        lines.add(Component.empty());
        lines.add(Component.literal(GRAY_RULE));
        lines.add(Component.literal(" \u00A77CraftCost: \u00A78" + title));
        if (detail != null && !detail.isBlank()) {
            lines.add(Component.literal(" \u00A78" + detail));
        }
        lines.add(Component.literal(GRAY_RULE));
    }

    private void resetHoverTimer() {
        hoveredItemId = null;
        requestedItemId = null;
        hoverStartedAt = 0L;
    }

    private void requestPricesForHover(String itemId) {
        if (itemId.equals(requestedItemId)) {
            return;
        }

        requestedItemId = itemId;
        priceFetcher.requestDirectPrices(itemId, recipeCache);
    }
}
