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

/**
 * Client-side mod initializer - sets up the pricing pipeline and tooltip hooks.
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
    private RepoRecipeLoader repoRecipeLoader;
    private long lastFallbackRecipeLoadAt;
    private boolean localRepoRecipesLoaded;

    @Override
    public void onInitializeClient() {
        instance = this;

        config = CraftCostConfig.load();

        coflnetClient = new CoflnetClient();
        priceCache = new PriceCache();
        recipeCache = new RecipeCache();
        craftCostEngine = new CraftCostEngine(priceCache, recipeCache);

        priceFetcher = new PriceFetcher(coflnetClient, priceCache, config, craftCostEngine);
        priceFetcher.start();

        repoRecipeLoader = new RepoRecipeLoader();
        loadLocalRepoRecipes();
        reiCompat = new REICompat(recipeCache);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (priceFetcher != null) {
                priceFetcher.stop();
            }
        });

        tooltipHandler = new TooltipHandler(priceCache, craftCostEngine, recipeCache, priceFetcher, config);
        tooltipHandler.register();

        CraftCostMod.LOGGER.info("[CraftCost] Client initialized - tooltip overlay active");
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

    public void onReiReloadFinished() {
        if (reiCompat == null) {
            return;
        }

        int loaded = reiCompat.loadRecipes();
        if (loaded > 0) {
            craftCostEngine.invalidate();
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
        }
    }

    private void loadLocalRepoRecipes() {
        if (localRepoRecipesLoaded || repoRecipeLoader == null) {
            return;
        }

        localRepoRecipesLoaded = true;
        if (repoRecipeLoader.loadInto(recipeCache) > 0) {
            craftCostEngine.invalidate();
        }
    }
}
