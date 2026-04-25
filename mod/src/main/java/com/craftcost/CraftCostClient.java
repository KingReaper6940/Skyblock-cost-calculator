package com.craftcost;

import com.craftcost.api.CoflnetClient;
import com.craftcost.api.PriceFetcher;
import com.craftcost.compat.REICompat;
import com.craftcost.config.CraftCostConfig;
import com.craftcost.data.BundledRecipeLoader;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.RecipeCache;
import com.craftcost.data.RepoRecipeLoader;
import com.craftcost.tooltip.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/**
 * Client-side mod initializer — sets up the pricing pipeline and tooltip hooks.
 */
public class CraftCostClient implements ClientModInitializer {

    private static final long REI_FALLBACK_COOLDOWN_MS = 10_000L;

    private static CraftCostClient instance;

    private CraftCostConfig config;
    private CoflnetClient coflnetClient;
    private PriceCache priceCache;
    private RecipeCache recipeCache;
    private PriceFetcher priceFetcher;
    private CraftCostEngine craftCostEngine;
    private TooltipHandler tooltipHandler;
    private REICompat reiCompat;
    private BundledRecipeLoader bundledRecipeLoader;
    private RepoRecipeLoader repoRecipeLoader;
    private long lastFallbackRecipeLoadAt;

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

        bundledRecipeLoader = new BundledRecipeLoader();
        bundledRecipeLoader.loadInto(recipeCache);

        repoRecipeLoader = new RepoRecipeLoader();
        repoRecipeLoader.loadInto(recipeCache);

        reiCompat = new REICompat(recipeCache);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (priceFetcher != null) {
                priceFetcher.stop();
            }
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

    public void requestRecipeFallbackLoad() {
        if (!REICompat.isLoaded() || reiCompat == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastFallbackRecipeLoadAt < REI_FALLBACK_COOLDOWN_MS) {
            return;
        }

        lastFallbackRecipeLoadAt = now;
        if (reiCompat.loadRecipes() > 0) {
            craftCostEngine.invalidate();
        }
    }
}
