package com.craftcost.api;

import com.craftcost.CraftCostMod;
import com.craftcost.data.PriceEntry;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for the Coflnet SkyBlock API.
 * Handles all pricing requests (bazaar + auction).
 */
public class CoflnetClient {

    private static final String BASE_URL = "https://sky.coflnet.com/api";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final String USER_AGENT = "CraftCost/1.0.3 (+https://github.com/KingReaper6940/Skyblock-cost-calculator)";

    private final HttpClient httpClient;

    public CoflnetClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Fetch the best current market price using the lowest request count path.
     * The current-price endpoint normally covers both Bazaar and AH items in one call.
     * If it misses, fall back to documented Bazaar snapshot, then documented active BIN.
     */
    public CompletableFuture<PriceLookup> getCurrentPrice(String itemTag) {
        return getItemPrice(itemTag).thenCompose(current -> {
            if (current.statusCode() == 429) {
                return CompletableFuture.completedFuture(PriceLookup.empty());
            }

            PriceLookup parsedCurrent = parseCurrentPrice(current);
            if (parsedCurrent.hasPrice()) {
                return CompletableFuture.completedFuture(parsedCurrent);
            }

            return getBazaarPrice(itemTag).thenCompose(bazaar -> {
                if (bazaar.statusCode() == 429) {
                    return CompletableFuture.completedFuture(PriceLookup.empty());
                }

                PriceLookup parsedBazaar = parseBazaarPrice(bazaar);
                if (parsedBazaar.hasPrice()) {
                    return CompletableFuture.completedFuture(parsedBazaar);
                }

                return getActiveBinAuctions(itemTag).thenApply(this::parseLowestBin);
            });
        });
    }

    /**
     * Fetch active BIN auctions for an item tag.
     * Returns the lowest 10 active BIN listings.
     */
    public CompletableFuture<ApiResponse<JsonArray>> getActiveBinAuctions(String itemTag) {
        String url = BASE_URL + "/auctions/tag/" + encodePathSegment(itemTag) + "/active/bin";
        return fetchJsonArray(url);
    }

    /**
     * Fetch bazaar item status/price.
     */
    public CompletableFuture<ApiResponse<JsonObject>> getBazaarPrice(String itemTag) {
        String url = BASE_URL + "/bazaar/" + encodePathSegment(itemTag) + "/snapshot";
        return fetchJsonObject(url);
    }

    /**
     * Fetch item price summary (unified bazaar + AH).
     */
    public CompletableFuture<ApiResponse<JsonObject>> getItemPrice(String itemTag) {
        String url = BASE_URL + "/item/price/" + encodePathSegment(itemTag) + "/current";
        return fetchJsonObject(url);
    }

    private CompletableFuture<ApiResponse<JsonArray>> fetchJsonArray(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return ApiResponse.success(response.statusCode(), JsonParser.parseString(response.body()).getAsJsonArray());
                    }
                    logHttpMiss(response.statusCode(), url);
                    return ApiResponse.empty(response.statusCode(), new JsonArray());
                })
                .exceptionally(e -> {
                    CraftCostMod.LOGGER.error("[CraftCost] API error for {}: {}", url, e.getMessage());
                    return ApiResponse.empty(0, new JsonArray());
                });
    }

    private CompletableFuture<ApiResponse<JsonObject>> fetchJsonObject(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return ApiResponse.success(response.statusCode(), JsonParser.parseString(response.body()).getAsJsonObject());
                    }
                    logHttpMiss(response.statusCode(), url);
                    return ApiResponse.empty(response.statusCode(), new JsonObject());
                })
                .exceptionally(e -> {
                    CraftCostMod.LOGGER.error("[CraftCost] API error for {}: {}", url, e.getMessage());
                    return ApiResponse.empty(0, new JsonObject());
                });
    }

    private PriceLookup parseCurrentPrice(ApiResponse<JsonObject> response) {
        if (!response.isSuccess()) {
            return PriceLookup.empty();
        }

        JsonObject json = response.body();
        if (!json.has("buy")) {
            return PriceLookup.empty();
        }

        double buy = json.get("buy").getAsDouble();
        if (buy <= 0) {
            return PriceLookup.empty();
        }

        boolean isAh = json.has("isAh") && json.get("isAh").getAsBoolean();
        if (isAh) {
            return PriceLookup.found(PriceEntry.fromBin(Math.round(buy)));
        }

        double sell = json.has("sell") ? json.get("sell").getAsDouble() : -1;
        return PriceLookup.found(PriceEntry.fromBazaar(buy, sell));
    }

    private PriceLookup parseBazaarPrice(ApiResponse<JsonObject> response) {
        if (!response.isSuccess()) {
            return PriceLookup.empty();
        }

        JsonObject bazaar = response.body();
        if (!bazaar.has("buyPrice")) {
            return PriceLookup.empty();
        }

        double buyPrice = bazaar.get("buyPrice").getAsDouble();
        if (buyPrice <= 0) {
            return PriceLookup.empty();
        }

        double sellPrice = bazaar.has("sellPrice") ? bazaar.get("sellPrice").getAsDouble() : -1;
        return PriceLookup.found(PriceEntry.fromBazaar(buyPrice, sellPrice));
    }

    private PriceLookup parseLowestBin(ApiResponse<JsonArray> response) {
        if (!response.isSuccess()) {
            return PriceLookup.empty();
        }

        long lowestBin = Long.MAX_VALUE;
        for (int i = 0; i < response.body().size(); i++) {
            JsonObject auction = response.body().get(i).getAsJsonObject();
            if (!auction.has("startingBid")) {
                continue;
            }

            long bid = auction.get("startingBid").getAsLong();
            if (bid > 0 && bid < lowestBin) {
                lowestBin = bid;
            }
        }

        if (lowestBin == Long.MAX_VALUE) {
            return PriceLookup.empty();
        }

        return PriceLookup.found(PriceEntry.fromBin(lowestBin));
    }

    private void logHttpMiss(int statusCode, String url) {
        if (statusCode == 204 || statusCode == 404) {
            return;
        }

        if (statusCode == 429) {
            CraftCostMod.LOGGER.warn("[CraftCost] Coflnet rate limit hit for {}", url);
            return;
        }

        CraftCostMod.LOGGER.debug("[CraftCost] Coflnet returned {} for {}", statusCode, url);
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public record ApiResponse<T>(int statusCode, T body) {
        public static <T> ApiResponse<T> success(int statusCode, T body) {
            return new ApiResponse<>(statusCode, body);
        }

        public static <T> ApiResponse<T> empty(int statusCode, T body) {
            return new ApiResponse<>(statusCode, body);
        }

        public boolean isSuccess() {
            return statusCode == 200;
        }
    }

    public record PriceLookup(PriceEntry price) {
        public static PriceLookup found(PriceEntry price) {
            return new PriceLookup(price);
        }

        public static PriceLookup empty() {
            return new PriceLookup(null);
        }

        public boolean hasPrice() {
            return price != null;
        }
    }
}
