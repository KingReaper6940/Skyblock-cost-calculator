package com.craftcost.data;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe price storage. Maps item tags to their current price data.
 */
public class PriceCache {

    // default TTL: 5 minutes
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, PriceEntry> cache = new ConcurrentHashMap<>();

    public void put(String itemTag, PriceEntry entry) {
        cache.put(itemTag, entry);
    }

    public void putBin(String itemTag, long lowestBin) {
        PriceEntry existing = cache.get(itemTag);
        if (existing != null && existing.hasBazaar()) {
            cache.put(itemTag, PriceEntry.combined(lowestBin, existing.getBazaarBuyPrice(), existing.getBazaarSellPrice()));
        } else {
            cache.put(itemTag, PriceEntry.fromBin(lowestBin));
        }
    }

    public void putBazaar(String itemTag, double buyPrice, double sellPrice) {
        PriceEntry existing = cache.get(itemTag);
        if (existing != null && existing.hasBin()) {
            // merge with existing BIN data
            cache.put(itemTag, PriceEntry.combined(existing.getLowestBin(), buyPrice, sellPrice));
        } else {
            cache.put(itemTag, PriceEntry.fromBazaar(buyPrice, sellPrice));
        }
    }

    public PriceEntry get(String itemTag) {
        return cache.get(itemTag);
    }

    public boolean has(String itemTag) {
        return cache.containsKey(itemTag);
    }

    public boolean isExpired(String itemTag) {
        PriceEntry entry = cache.get(itemTag);
        return entry == null || entry.isExpired(DEFAULT_TTL_MS);
    }

    /**
     * Get the best buy price for an item, or -1 if not cached.
     */
    public double getBuyPrice(String itemTag) {
        PriceEntry entry = cache.get(itemTag);
        if (entry == null) return -1;
        return entry.getBestBuyPrice();
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }
}
