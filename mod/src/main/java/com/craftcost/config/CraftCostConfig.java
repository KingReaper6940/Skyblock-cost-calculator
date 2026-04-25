package com.craftcost.config;

import com.craftcost.CraftCostMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON-based config for CraftCost.
 * Saved to config/craftcost.json
 */
public class CraftCostConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // config fields with defaults
    private boolean enabled = true;
    private int refreshIntervalSeconds = 300; // 5 minutes
    private boolean showBreakdown = true;
    private boolean showSavings = true;

    public static CraftCostConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("craftcost.json");

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                CraftCostConfig config = GSON.fromJson(json, CraftCostConfig.class);
                CraftCostMod.LOGGER.info("[CraftCost] Config loaded from {}", configPath);
                return config;
            } catch (IOException e) {
                CraftCostMod.LOGGER.error("[CraftCost] Failed to load config, using defaults", e);
            }
        }

        // create default config
        CraftCostConfig config = new CraftCostConfig();
        config.save();
        return config;
    }

    public void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("craftcost.json");
        try {
            Files.writeString(configPath, GSON.toJson(this));
        } catch (IOException e) {
            CraftCostMod.LOGGER.error("[CraftCost] Failed to save config", e);
        }
    }

    // getters

    public boolean isEnabled() {
        return enabled;
    }

    public int getRefreshIntervalSeconds() {
        return refreshIntervalSeconds;
    }

    public boolean isShowBreakdown() {
        return showBreakdown;
    }

    public boolean isShowSavings() {
        return showSavings;
    }
}
