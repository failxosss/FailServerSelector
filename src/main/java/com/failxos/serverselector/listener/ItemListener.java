package com.failxos.serverselector.listener;

import com.failxos.serverselector.ServerSelectorPlugin;
import com.failxos.serverselector.item.Click;
import com.failxos.serverselector.item.ItemDef;
import com.failxos.serverselector.item.JoinItemManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Handles clicking and protecting the hotbar items. */
public final class ItemListener implements Listener {

    private final ServerSelectorPlugin plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public ItemListener(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean protect(Player player, String option) {
        return plugin.getConfig().getBoolean("protection." + option, true)
                && !player.hasPermission("serverselector.bypass.protect");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        String id = plugin.joinItems().tagId(item);
        if (id == null) {
            return;
        }
        event.setCancelled(true);

        JoinItemManager.JoinItem joinItem = plugin.joinItems().get(id);
        if (joinItem == null) {
            return;
        }
        Player player = event.getPlayer();
        Click click = Click.of(event.getAction(), player.isSneaking());
        ItemDef def = joinItem.def;
        if (click == null || !def.clickTypes.contains(click)) {
            return;
        }
        if (!plugin.joinItems().isAllowed(joinItem, player)) {
            return;
        }

        String key = player.getUniqueId() + ":" + id;
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(key);
        if (last != null && now - last < def.cooldownTicks * 50L) {
            return;
        }
        cooldowns.put(key, now);

        if (joinItem.toggle) {
            plugin.visibility().toggle(player);
        } else {
            plugin.actions().run(player, def.actionsFor(click));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (plugin.joinItems().isTagged(player.getInventory().getItem(event.getHand()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !protect(player, "prevent-move")) {
            return;
        }
        JoinItemManager items = plugin.joinItems();
        boolean tagged = items.isTagged(event.getCurrentItem()) || items.isTagged(event.getCursor());
        if (!tagged && event.getClick() == ClickType.NUMBER_KEY && event.getHotbarButton() >= 0) {
            tagged = items.isTagged(player.getInventory().getItem(event.getHotbarButton()));
        }
        if (!tagged && event.getClick() == ClickType.SWAP_OFFHAND) {
            tagged = items.isTagged(player.getInventory().getItemInOffHand());
        }
        if (tagged) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && protect(player, "prevent-move")
                && plugin.joinItems().isTagged(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (protect(event.getPlayer(), "prevent-drop")
                && plugin.joinItems().isTagged(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (protect(player, "prevent-swap-hand")
                && (plugin.joinItems().isTagged(event.getMainHandItem())
                || plugin.joinItems().isTagged(event.getOffHandItem()))) {
            event.setCancelled(true);
        }
    }
}
