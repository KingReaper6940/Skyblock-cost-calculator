package com.craftcost.api;

import com.craftcost.CraftCostMod;
import com.craftcost.config.CraftCostConfig;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.PriceEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that periodically refreshes prices for recently-viewed items.
 * Uses lazy loading — only fetches prices for items the player has hovered over.
 */
public class PriceFetcher {

    private final CoflnetClient client;
    private final PriceCache cache;
    private final CraftCostConfig config;
    private final CraftCostEngine craftCostEngine;
    private final ScheduledExecutorService scheduler;

    // items that have been requested and need price updates
    private final Set<String> trackedItems = ConcurrentHashMap.newKeySet();
    private final Set<String> inFlightItems = ConcurrentHashMap.newKeySet();

    public PriceFetcher(CoflnetClient client, PriceCache cache, CraftCostConfig config, CraftCostEngine craftCostEngine) {
        this.client = client;
        this.cache = cache;
        this.config = config;
        this.craftCostEngine = craftCostEngine;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CraftCost-PriceFetcher");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        int interval = config.getRefreshIntervalSeconds();
        scheduler.scheduleAtFixedRate(this::refreshAll, 0, interval, TimeUnit.SECONDS);
        CraftCostMod.LOGGER.info("[CraftCost] Price fetcher started ({}s interval)", interval);
    }

    public void stop() {
        scheduler.shutdown();
    }

    /**
     * Request price data for an item. Called when the player hovers over an item.
     * If the item isn't cached, fetches immediately. Also adds to tracked set for refresh.
     */
    public void requestPrice(String itemTag) {
        trackedItems.add(itemTag);

        if (!cache.has(itemTag) || cache.isExpired(itemTag)) {
            fetchPrice(itemTag);
        }
    }

    private void refreshAll() {
        for (String tag : trackedItems) {
            if (cache.isExpired(tag)) {
                fetchPrice(tag);
            }
        }
    }

    private void fetchPrice(String itemTag) {
        if (!inFlightItems.add(itemTag)) {
            return;
        }

        // try BIN auctions first
        CompletableFuture<Void> binFuture = client.getActiveBinAuctions(itemTag).thenAccept(auctions -> {
            long lowestBin = Long.MAX_VALUE;

            for (JsonElement elem : auctions) {
                JsonObject auction = elem.getAsJsonObject();
                long bid = auction.get("startingBid").getAsLong();
                if (bid < lowestBin) {
                    lowestBin = bid;
                }
            }

            if (lowestBin < Long.MAX_VALUE) {
                cache.putBin(itemTag, lowestBin);
                craftCostEngine.invalidate();
            }
        });

        // also try bazaar
        CompletableFuture<Void> bazaarFuture = client.getBazaarPrice(itemTag).thenAccept(bazaar -> {
            if (bazaar.has("buyPrice")) {
                double buyPrice = bazaar.get("buyPrice").getAsDouble();
                double sellPrice = bazaar.has("sellPrice") ? bazaar.get("sellPrice").getAsDouble() : 0;
                cache.putBazaar(itemTag, buyPrice, sellPrice);
                craftCostEngine.invalidate();
            }
        });

        CompletableFuture.allOf(binFuture, bazaarFuture)
                .whenComplete((ignored, throwable) -> inFlightItems.remove(itemTag));
    }
}
