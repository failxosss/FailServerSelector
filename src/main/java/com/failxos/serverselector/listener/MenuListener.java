package com.failxos.serverselector.listener;

import com.failxos.serverselector.ServerSelectorPlugin;
import com.failxos.serverselector.item.Click;
import com.failxos.serverselector.item.ItemDef;
import com.failxos.serverselector.menu.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Handles clicks inside our menus. */
public final class MenuListener implements Listener {

    private final ServerSelectorPlugin plugin;

    public MenuListener(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        ItemDef def = holder.menu().item(event.getSlot());
        if (def == null) {
            return;
        }
        Click click = Click.of(event.getClick());
        if (click == null) {
            return;
        }
        if (def.permission != null && !player.hasPermission(def.permission)) {
            plugin.msg(player, "no-permission");
            return;
        }
        plugin.actions().run(player, def.actionsFor(click));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }
}
