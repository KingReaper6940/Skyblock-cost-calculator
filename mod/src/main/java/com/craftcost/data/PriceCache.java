package com.craftcost.data;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe price storage. Maps item tags to their current price data.
 */
public class PriceCache {

    // default TTL: 5 minutes
    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, PriceEntry> cache = new ConcurrentHashMap<>();

    public boolean put(String itemTag, PriceEntry entry) {
        PriceEntry previous = cache.put(itemTag, entry);
        return previous == null || !previous.sameValues(entry.getLowestBin(), entry.getBazaarBuyPrice(), entry.getBazaarSellPrice());
    }

    public boolean putBin(String itemTag, long lowestBin) {
        PriceEntry existing = cache.get(itemTag);
        if (existing != null && existing.hasBazaar()) {
            if (existing.sameValues(lowestBin, existing.getBazaarBuyPrice(), existing.getBazaarSellPrice())) {
                return false;
            }

            cache.put(itemTag, PriceEntry.combined(lowestBin, existing.getBazaarBuyPrice(), existing.getBazaarSellPrice()));
            return true;
        }

        if (existing != null && existing.sameValues(lowestBin, -1, -1)) {
            return false;
        }

        cache.put(itemTag, PriceEntry.fromBin(lowestBin));
        return true;
    }

    public boolean putBazaar(String itemTag, double buyPrice, double sellPrice) {
        PriceEntry existing = cache.get(itemTag);
        if (existing != null && existing.hasBin()) {
            if (existing.sameValues(existing.getLowestBin(), buyPrice, sellPrice)) {
                return false;
            }

            cache.put(itemTag, PriceEntry.combined(existing.getLowestBin(), buyPrice, sellPrice));
            return true;
        }

        if (existing != null && existing.sameValues(-1, buyPrice, sellPrice)) {
            return false;
        }

        cache.put(itemTag, PriceEntry.fromBazaar(buyPrice, sellPrice));
        return true;
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
