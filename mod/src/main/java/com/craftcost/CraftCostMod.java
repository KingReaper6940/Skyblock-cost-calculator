package com.craftcost;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod initializer (runs on both client and server).
 * CraftCost is client-only, so this is mostly a no-op.
 */
public class CraftCostMod implements ModInitializer {

    public static final String MOD_ID = "craftcost";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[CraftCost] Mod initialized");
    }
}
