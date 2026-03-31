package com.skyblockexp.ezrtp.forge;

import com.skyblockexp.ezrtp.platform.PlatformGuiBridge;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Minimal GUI bridge scaffold; methods are conservative no-ops where appropriate.
 */
public final class ForgePlatformGuiBridge implements PlatformGuiBridge {

    @Override
    public Inventory createInventory(InventoryHolder holder, int size, Component title) {
        throw new UnsupportedOperationException("Forge GUI bridge not implemented in scaffold");
    }

    @Override
    public void setDisplayName(ItemMeta meta, Component displayName) {
        // no-op scaffold
    }

    @Override
    public void setLore(ItemMeta meta, List<Component> lore) {
        // no-op scaffold
    }

    @Override
    public void applyItemMeta(ItemStack icon, ItemMeta meta) {
        // no-op scaffold
    }
}
