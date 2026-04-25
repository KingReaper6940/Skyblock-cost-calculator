package com.craftcost;

import com.craftcost.api.CoflnetClient;
import com.craftcost.api.PriceFetcher;
import com.craftcost.compat.REICompat;
import com.craftcost.config.CraftCostConfig;
import com.craftcost.data.CraftCostEngine;
import com.craftcost.data.PriceCache;
import com.craftcost.data.RecipeCache;
import com.craftcost.data.RepoRecipeLoader;
import com.craftcost.tooltip.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * Client-side mod initializer — sets up the pricing pipeline and tooltip hooks.
 */
public class CraftCostClient implements ClientModInitializer {

    private static final long REI_FALLBACK_COOLDOWN_MS = 10_000L;
    private static final int REI_RETRY_INTERVAL_TICKS = 20;
    private static final int REI_MAX_LOAD_ATTEMPTS = 45;

    private static CraftCostClient instance;

    private CraftCostConfig config;
    private CoflnetClient coflnetClient;
    private PriceCache priceCache;
    private RecipeCache recipeCache;
    private PriceFetcher priceFetcher;
    private CraftCostEngine craftCostEngine;
    private TooltipHandler tooltipHandler;
    private REICompat reiCompat;
    private RepoRecipeLoader repoRecipeLoader;
    private long lastFallbackRecipeLoadAt;
    private int recipeLoadTicks;
    private int recipeLoadAttempts;
    private boolean localRepoFallbackLoaded;

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

        repoRecipeLoader = new RepoRecipeLoader();
        reiCompat = new REICompat(recipeCache);

        ClientTickEvents.END_CLIENT_TICK.register(client -> loadRecipesFromReiWhenReady());

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

    private void loadRecipesFromReiWhenReady() {
        if (recipeCache.size() > 0 || reiCompat == null) {
            return;
        }

        recipeLoadTicks++;
        if (recipeLoadTicks < REI_RETRY_INTERVAL_TICKS) {
            return;
        }

        recipeLoadTicks = 0;
        if (!REICompat.isLoaded()) {
            return;
        }

        recipeLoadAttempts++;
        if (reiCompat.loadRecipes() > 0) {
            craftCostEngine.invalidate();
            return;
        }

        if (recipeLoadAttempts >= REI_MAX_LOAD_ATTEMPTS) {
            loadLocalRepoFallback();
        }
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
        int loaded = reiCompat.loadRecipes();
        if (loaded > 0) {
            craftCostEngine.invalidate();
        } else if (recipeCache.size() == 0) {
            loadLocalRepoFallback();
        }
    }

    private void loadLocalRepoFallback() {
        if (localRepoFallbackLoaded || repoRecipeLoader == null) {
            return;
        }

        localRepoFallbackLoaded = true;
        int loaded = repoRecipeLoader.loadInto(recipeCache);
        if (loaded > 0) {
            craftCostEngine.invalidate();
        }
    }
}
