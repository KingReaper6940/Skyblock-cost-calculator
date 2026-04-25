package com.craftcost.util;

import java.text.DecimalFormat;

/**
 * Formats coin values into human-readable strings.
 * Examples: 1,500 → "1,500" | 1,500,000 → "1.5M" | 23,456 → "23.5K"
 */
public class NumberFormatter {

    private static final DecimalFormat DECIMAL = new DecimalFormat("#,##0");
    private static final DecimalFormat SHORT = new DecimalFormat("#,##0.#");

    /**
     * Format a coin value. Uses compact notation for large numbers.
     */
    public static String format(double value) {
        if (value < 0) return "N/A";

        if (value >= 1_000_000_000) {
            return SHORT.format(value / 1_000_000_000) + "B";
        } else if (value >= 1_000_000) {
            return SHORT.format(value / 1_000_000) + "M";
        } else if (value >= 100_000) {
            return SHORT.format(value / 1_000) + "K";
        } else {
            return DECIMAL.format(value);
        }
    }

    public static String format(long value) {
        return format((double) value);
    }
}
