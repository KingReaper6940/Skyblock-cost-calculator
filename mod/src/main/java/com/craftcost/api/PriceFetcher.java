package com.craftcost.api;

import com.craftcost.CraftCostMod;
import com.craftcost.config.CraftCostConfig;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.RecipeCache;

import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that periodically refreshes prices for recently-viewed items.
 * Uses lazy loading — only fetches prices for items the player has hovered over.
 */
public class PriceFetcher {

    private static final int MAX_QUEUE_SIZE = 128;
    private static final long FETCH_COOLDOWN_MS = 5 * 60 * 1000L;

    private final CoflnetClient client;
    private final PriceCache cache;
    private final CraftCostConfig config;
    private final CraftCostEngine craftCostEngine;
    private final ScheduledExecutorService scheduler;

    // items that have been requested and need price updates
    private final Set<String> trackedItems = ConcurrentHashMap.newKeySet();
    private final Set<String> inFlightItems = ConcurrentHashMap.newKeySet();
    private final Set<String> queuedItems = ConcurrentHashMap.newKeySet();
    private final Queue<String> pendingItems = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, Long> lastFetchAttempt = new ConcurrentHashMap<>();

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
        scheduler.scheduleWithFixedDelay(this::processNextQueuedItem, 500, 750, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::refreshAll, interval, interval, TimeUnit.SECONDS);
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
            queueFetch(itemTag);
        }
    }

    /**
     * Request pricing for an item and its direct recipe ingredients.
     * This stays lightweight because the underlying queue deduplicates and rate limits requests.
     */
    public void requestDirectPrices(String itemTag, RecipeCache recipeCache) {
        requestPrice(itemTag);

        if (recipeCache == null) {
            return;
        }

        for (String ingredientTag : recipeCache.collectDirectIngredientTags(itemTag)) {
            requestPrice(ingredientTag);
        }
    }

    private void refreshAll() {
        for (String tag : trackedItems) {
            if (cache.isExpired(tag)) {
                queueFetch(tag);
            }
        }
    }

    private void queueFetch(String itemTag) {
        long now = System.currentTimeMillis();
        long lastAttempt = lastFetchAttempt.getOrDefault(itemTag, 0L);
        if (now - lastAttempt < FETCH_COOLDOWN_MS) {
            return;
        }

        if (pendingItems.size() >= MAX_QUEUE_SIZE || !queuedItems.add(itemTag)) {
            return;
        }

        pendingItems.offer(itemTag);
    }

    private void processNextQueuedItem() {
        String itemTag = pendingItems.poll();
        if (itemTag == null) return;

        queuedItems.remove(itemTag);
        fetchPrice(itemTag);
    }

    private void fetchPrice(String itemTag) {
        if (!inFlightItems.add(itemTag)) {
            return;
        }
        lastFetchAttempt.put(itemTag, System.currentTimeMillis());

        client.getCurrentPrice(itemTag)
                .thenAccept(result -> {
                    if (!result.hasPrice()) {
                        return;
                    }

                    if (cache.put(itemTag, result.price())) {
                        craftCostEngine.invalidate();
                    }
                })
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        CraftCostMod.LOGGER.debug("[CraftCost] Price fetch failed for {}", itemTag, throwable);
                    }
                    inFlightItems.remove(itemTag);
                });
    }
}
