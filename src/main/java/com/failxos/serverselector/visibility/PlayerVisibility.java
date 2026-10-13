package com.failxos.serverselector.visibility;

import com.failxos.serverselector.ServerSelectorPlugin;
import com.failxos.serverselector.item.ItemDef;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles the "hide all players" feature. */
public final class PlayerVisibility {

    private final ServerSelectorPlugin plugin;
    private final Set<UUID> hidden = ConcurrentHashMap.newKeySet();

    public PlayerVisibility(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isHidden(Player player) {
        return hidden.contains(player.getUniqueId());
    }

    private boolean remember() {
        return plugin.getConfig().getBoolean("settings.remember-player-visibility", true);
    }

    private boolean bypass(Player target) {
        return plugin.getConfig().getBoolean("settings.staff-bypass-hide", true)
                && target.hasPermission("serverselector.bypass.hide");
    }

    /** Hidden state that is really applied (the toggle may be disabled in the current world). */
    private boolean effective(Player viewer) {
        if (!isHidden(viewer)) {
            return false;
        }
        boolean onlyWhereToggle = plugin.getConfig().getBoolean("settings.show-players-where-toggle-disabled", true);
        return !onlyWhereToggle || plugin.joinItems().toggleAllowed(viewer);
    }

    /** Re-applies visibility for one viewer against every online player. */
    public void apply(Player viewer) {
        boolean hide = effective(viewer);
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(viewer)) {
                continue;
            }
            if (hide && !bypass(other)) {
                viewer.hidePlayer(plugin, other);
            } else {
                viewer.showPlayer(plugin, other);
            }
        }
    }

    public void onJoin(Player player) {
        if (remember() && player.getPersistentDataContainer().has(plugin.hiddenKey(), PersistentDataType.BYTE)) {
            hidden.add(player.getUniqueId());
        }
        apply(player);
        // players who hid everyone must not see the newcomer
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player) && effective(viewer) && !bypass(player)) {
                viewer.hidePlayer(plugin, player);
            }
        }
    }

    public void onQuit(Player player) {
        hidden.remove(player.getUniqueId());
    }

    public void toggle(Player player) {
        setHidden(player, !isHidden(player));
    }

    public void setHidden(Player player, boolean value) {
        if (value) {
            hidden.add(player.getUniqueId());
        } else {
            hidden.remove(player.getUniqueId());
        }
        if (remember()) {
            if (value) {
                player.getPersistentDataContainer().set(plugin.hiddenKey(), PersistentDataType.BYTE, (byte) 1);
            } else {
                player.getPersistentDataContainer().remove(plugin.hiddenKey());
            }
        }
        apply(player);
        plugin.joinItems().refresh(player);

        // actions of the state we just switched to
        ItemDef def = plugin.joinItems().toggleDef(value);
        if (def != null) {
            plugin.actions().run(player, def.actions);
        }
    }
}
