package com.craftcost;

import com.craftcost.api.CoflnetClient;
import com.craftcost.api.PriceFetcher;
import com.craftcost.compat.REICompat;
import com.craftcost.config.CraftCostConfig;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.RecipeCache;
import com.craftcost.tooltip.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client-side mod initializer — sets up the pricing pipeline and tooltip hooks.
 */
public class CraftCostClient implements ClientModInitializer {

    private static CraftCostClient instance;

    private CraftCostConfig config;
    private CoflnetClient coflnetClient;
    private PriceCache priceCache;
    private RecipeCache recipeCache;
    private PriceFetcher priceFetcher;
    private CraftCostEngine craftCostEngine;
    private TooltipHandler tooltipHandler;
    private REICompat reiCompat;
    private int recipeLoadAttempts;

    @Override
    public void onInitializeClient() {
        instance = this;

        // load config
        config = CraftCostConfig.load();

        // init API client + caches
        coflnetClient = new CoflnetClient();
        priceCache = new PriceCache();
        recipeCache = new RecipeCache();

        // init pricing engine
        craftCostEngine = new CraftCostEngine(priceCache, recipeCache);

        // start background price fetcher
        priceFetcher = new PriceFetcher(coflnetClient, priceCache, config, craftCostEngine);
        priceFetcher.start();

        reiCompat = new REICompat(recipeCache);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> reiCompat.loadRecipes());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (recipeCache.size() > 0 || recipeLoadAttempts >= 30 || !REICompat.isLoaded()) return;
            if (client.level == null || client.level.getGameTime() % 20 != 0) return;

            recipeLoadAttempts++;
            reiCompat.loadRecipes();
        });

        // hook into tooltips
        tooltipHandler = new TooltipHandler(priceCache, craftCostEngine, recipeCache, priceFetcher, config);
        tooltipHandler.register();

        CraftCostMod.LOGGER.info("[CraftCost] Client initialized — tooltip overlay active");
    }

    public static CraftCostClient getInstance() {
        return instance;
    }

    public CraftCostConfig getConfig() {
        return config;
    }

    public PriceCache getPriceCache() {
        return priceCache;
    }

    public RecipeCache getRecipeCache() {
        return recipeCache;
    }

    public CoflnetClient getCoflnetClient() {
        return coflnetClient;
    }
}
