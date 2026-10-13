package com.failxos.serverselector.item;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/** Optional ItemsAdder support through reflection (no compile-time dependency). */
public final class ItemsAdderHook {

    private static final Set<String> WARNED = new HashSet<>();

    private ItemsAdderHook() {
    }

    public static void resetWarnings() {
        WARNED.clear();
    }

    /** Returns the ItemsAdder item or null when ItemsAdder is missing or the id is unknown. */
    public static ItemStack get(Logger log, String id) {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            warnOnce(log, "ItemsAdder is not installed/enabled - using the fallback material for '" + id + "'.");
            return null;
        }
        try {
            Class<?> clazz = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object stack = clazz.getMethod("getInstance", String.class).invoke(null, id);
            if (stack == null) {
                warnOnce(log, "Unknown ItemsAdder item '" + id + "' - using the fallback material.");
                return null;
            }
            return (ItemStack) clazz.getMethod("getItemStack").invoke(stack);
        } catch (Throwable t) {
            warnOnce(log, "Could not load ItemsAdder item '" + id + "': " + t.getMessage());
            return null;
        }
    }

    private static void warnOnce(Logger log, String msg) {
        if (WARNED.add(msg)) {
            log.warning(msg);
        }
    }
}
