package com.craftcost.tooltip;

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

    private final PriceCache priceCache;
    private final CraftCostEngine craftCostEngine;
    private final PriceFetcher priceFetcher;
    private final CraftCostConfig config;
    private String hoveredItemId;
    private long hoverStartedAt;

    public TooltipHandler(PriceCache priceCache, CraftCostEngine craftCostEngine, RecipeCache recipeCache,
                          PriceFetcher priceFetcher, CraftCostConfig config) {
        this.priceCache = priceCache;
        this.craftCostEngine = craftCostEngine;
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

            priceFetcher.requestPrice(itemId);

            PriceEntry price = priceCache.get(itemId);
            CraftCostEngine.CraftResult craft = craftCostEngine.calculate(itemId);

            if (price == null && craft == null) {
                lines.add(Component.empty());
                lines.add(Component.literal("§8§m                    §r"));
                lines.add(Component.literal(" §7CraftCost: §8Calculating..."));
                lines.add(Component.literal("§8§m                    §r"));
                return;
            }

            lines.add(Component.empty());
            lines.add(Component.literal("§8§m                    §r"));

            if (price != null && price.hasBin()) {
                lines.add(Component.literal(" §7Lowest BIN: §6§l" +
                        NumberFormatter.format(price.getLowestBin())));
            } else if (price != null && price.hasBazaar()) {
                lines.add(Component.literal(" §7Bazaar Buy: §6§l" +
                        NumberFormatter.format(price.getBazaarBuyPrice())));
            }

            if (craft != null && craft.hasCraftCost()) {
                boolean cheaper = craft.isCheaperToCraft();
                String color = cheaper ? "§a" : "§c";
                lines.add(Component.literal(" §7Raw Craft Cost: " + color + "§l" +
                        NumberFormatter.format(craft.getCraftCost())));

                if (config.isShowSavings() && craft.hasBuyPrice()) {
                    if (cheaper) {
                        lines.add(Component.literal("   §7§o-> §aCheaper to craft! Save §e" +
                                NumberFormatter.format(craft.getSavings())));
                    } else if (craft.isCheaperToBuy()) {
                        lines.add(Component.literal("   §7§o-> §6Cheaper to buy!"));
                    }
                }

                if (config.isShowBreakdown() && !craft.getBreakdown().isEmpty()) {
                    var entries = craft.getBreakdown().entrySet().stream().toList();
                    for (int i = 0; i < entries.size(); i++) {
                        var entry = entries.get(i);
                        var ing = entry.getValue();
                        String prefix = (i == entries.size() - 1) ? "  §8- " : "  §8| ";
                        lines.add(Component.literal(prefix + "§f" + ing.getCount() + "x " +
                                ing.getItemTag() + " §8- §e" +
                                NumberFormatter.format(ing.getTotalCost())));
                    }
                }
            }

            lines.add(Component.literal("§8§m                    §r"));
        });
    }

    private long updateHoverTimer(String itemId) {
        long now = System.currentTimeMillis();
        if (!itemId.equals(hoveredItemId)) {
            hoveredItemId = itemId;
            hoverStartedAt = now;
            return 0;
        }

        return now - hoverStartedAt;
    }

    private void addWaitTooltip(List<Component> lines, long remainingMs) {
        long remainingSeconds = Math.max(1, (remainingMs + 999) / 1000);
        lines.add(Component.empty());
        lines.add(Component.literal("§8§m                    §r"));
        lines.add(Component.literal(" §7CraftCost: §8Hold hover " + remainingSeconds + "s"));
        lines.add(Component.literal(" §8No price checks until then"));
        lines.add(Component.literal("§8§m                    §r"));
    }

    private void resetHoverTimer() {
        hoveredItemId = null;
        hoverStartedAt = 0L;
    }
}
