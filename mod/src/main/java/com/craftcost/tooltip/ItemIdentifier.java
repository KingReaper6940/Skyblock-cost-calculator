package com.craftcost.tooltip;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

/**
 * Extracts Hypixel SkyBlock item IDs from ItemStack NBT data.
 * SkyBlock items store their ID in ExtraAttributes.id
 */
public class ItemIdentifier {

    /**
     * Get the SkyBlock item ID from an ItemStack, or null if not a SB item.
     */
    public static String getItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        try {
            // SkyBlock items have custom data in ExtraAttributes
            CompoundTag nbt = stack.getOrDefault(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.EMPTY
            ).copyTag();

            if (nbt == null || nbt.isEmpty()) return null;

            // the item ID lives directly in the custom data
            if (nbt.contains("id")) {
                return nbt.getString("id");
            }

            if (nbt.contains("ExtraAttributes")) {
                CompoundTag extraAttributes = nbt.getCompound("ExtraAttributes");
                if (extraAttributes.contains("id")) {
                    return extraAttributes.getString("id");
                }
            }

            return null;
        } catch (Exception e) {
            // not a SB item or NBT parsing failed
            return null;
        }
    }
}
