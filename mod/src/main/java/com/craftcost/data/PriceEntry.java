package com.craftcost.data;

/**
 * Immutable price entry for a single item.
 */
public class PriceEntry {

    private final long lowestBin;
    private final double bazaarBuyPrice;
    private final double bazaarSellPrice;
    private final long timestamp;

    private PriceEntry(long lowestBin, double bazaarBuyPrice, double bazaarSellPrice) {
        this.lowestBin = lowestBin;
        this.bazaarBuyPrice = bazaarBuyPrice;
        this.bazaarSellPrice = bazaarSellPrice;
        this.timestamp = System.currentTimeMillis();
    }

    public static PriceEntry fromBin(long lowestBin) {
        return new PriceEntry(lowestBin, -1, -1);
    }

    public static PriceEntry fromBazaar(double buyPrice, double sellPrice) {
        return new PriceEntry(-1, buyPrice, sellPrice);
    }

    public static PriceEntry combined(long lowestBin, double bazaarBuy, double bazaarSell) {
        return new PriceEntry(lowestBin, bazaarBuy, bazaarSell);
    }

    /**
     * Get the best buy price — uses bazaar instant-buy if available, else lowest BIN.
     */
    public double getBestBuyPrice() {
        if (bazaarBuyPrice > 0) return bazaarBuyPrice;
        if (lowestBin > 0) return lowestBin;
        return -1;
    }

    public long getLowestBin() {
        return lowestBin;
    }

    public double getBazaarBuyPrice() {
        return bazaarBuyPrice;
    }

    public double getBazaarSellPrice() {
        return bazaarSellPrice;
    }

    public boolean hasBazaar() {
        return bazaarBuyPrice > 0;
    }

    public boolean hasBin() {
        return lowestBin > 0;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired(long ttlMillis) {
        return System.currentTimeMillis() - timestamp > ttlMillis;
    }
}
