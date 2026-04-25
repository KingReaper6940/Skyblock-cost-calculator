package com.craftcost.api;

import com.craftcost.CraftCostMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP client for the Coflnet SkyBlock API.
 * Handles all pricing requests (bazaar + auction).
 */
public class CoflnetClient {

    private static final String BASE_URL = "https://sky.coflnet.com/api";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public CoflnetClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(TIMEOUT)
                .build();
    }

    /**
     * Fetch active BIN auctions for an item tag.
     * Returns the lowest 10 active BIN listings.
     */
    public CompletableFuture<JsonArray> getActiveBinAuctions(String itemTag) {
        String url = BASE_URL + "/auctions/tag/" + itemTag + "/active/bin";
        return fetchJsonArray(url);
    }

    /**
     * Fetch bazaar item status/price.
     */
    public CompletableFuture<JsonObject> getBazaarPrice(String itemTag) {
        String url = BASE_URL + "/bazaar/" + itemTag + "/snapshot";
        return fetchJsonObject(url);
    }

    /**
     * Fetch item price summary (unified bazaar + AH).
     */
    public CompletableFuture<JsonObject> getItemPrice(String itemTag) {
        String url = BASE_URL + "/item/price/" + itemTag + "/current";
        return fetchJsonObject(url);
    }

    private CompletableFuture<JsonArray> fetchJsonArray(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonParser.parseString(response.body()).getAsJsonArray();
                    }
                    CraftCostMod.LOGGER.warn("[CraftCost] API returned {} for {}", response.statusCode(), url);
                    return new JsonArray();
                })
                .exceptionally(e -> {
                    CraftCostMod.LOGGER.error("[CraftCost] API error for {}: {}", url, e.getMessage());
                    return new JsonArray();
                });
    }

    private CompletableFuture<JsonObject> fetchJsonObject(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return JsonParser.parseString(response.body()).getAsJsonObject();
                    }
                    CraftCostMod.LOGGER.warn("[CraftCost] API returned {} for {}", response.statusCode(), url);
                    return new JsonObject();
                })
                .exceptionally(e -> {
                    CraftCostMod.LOGGER.error("[CraftCost] API error for {}: {}", url, e.getMessage());
                    return new JsonObject();
                });
    }
}
