package com.craftcost.compat;

import com.craftcost.CraftCostClient;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.api.common.registry.ReloadStage;

/**
 * Hooks CraftCost into REI's reload lifecycle so recipe import happens
 * after REI has finished building its client-side displays.
 */
public class CraftCostReiPlugin implements REIClientPlugin {

    @Override
    public void postStage(PluginManager<REIClientPlugin> manager, ReloadStage stage) {
        if (stage != ReloadStage.END) {
            return;
        }

        CraftCostClient client = CraftCostClient.getInstance();
        if (client != null) {
            client.onReiReloadFinished();
        }
    }
}
