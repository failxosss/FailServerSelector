package com.failxos.serverselector.listener;

import com.failxos.serverselector.ServerSelectorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class PlayerListener implements Listener {

    private final ServerSelectorPlugin plugin;

    public PlayerListener(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("settings.clear-inventory-on-join", false)) {
            player.getInventory().clear();
        }
        plugin.visibility().onJoin(player);
        long delay = Math.max(0L, plugin.getConfig().getLong("settings.join-delay-ticks", 2L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.joinItems().refresh(player);
            }
        }, delay);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.visibility().onQuit(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.joinItems().refresh(player);
        plugin.visibility().apply(player);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                plugin.joinItems().refresh(player);
                plugin.visibility().apply(player);
            }
        }, 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("protection.remove-from-death-drops", true)) {
            event.getDrops().removeIf(plugin.joinItems()::isTagged);
        }
    }
}
