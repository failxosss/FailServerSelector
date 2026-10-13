package com.failxos.serverselector;

import com.failxos.serverselector.action.ActionExecutor;
import com.failxos.serverselector.command.SelectorCommand;
import com.failxos.serverselector.item.HeadResolver;
import com.failxos.serverselector.item.ItemDef;
import com.failxos.serverselector.item.ItemsAdderHook;
import com.failxos.serverselector.item.JoinItemManager;
import com.failxos.serverselector.listener.ItemListener;
import com.failxos.serverselector.listener.MenuListener;
import com.failxos.serverselector.listener.PlayerListener;
import com.failxos.serverselector.menu.MenuDef;
import com.failxos.serverselector.util.Text;
import com.failxos.serverselector.visibility.PlayerVisibility;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ServerSelector - configurable lobby hotbar items, server selector GUI and player visibility toggle.
 *
 * @author Failxos
 */
public final class ServerSelectorPlugin extends JavaPlugin {

    private final Map<String, MenuDef> menus = new LinkedHashMap<>();
    private JoinItemManager joinItems;
    private PlayerVisibility visibility;
    private ActionExecutor actions;
    private NamespacedKey itemKey;
    private NamespacedKey hiddenKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        itemKey = new NamespacedKey(this, "item");
        hiddenKey = new NamespacedKey(this, "players_hidden");

        joinItems = new JoinItemManager(this);
        visibility = new PlayerVisibility(this);
        actions = new ActionExecutor(this);

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        PluginCommand command = getCommand("serverselector");
        if (command != null) {
            SelectorCommand handler = new SelectorCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        loadAll();
        getLogger().info("ServerSelector " + getPluginMeta().getVersion() + " enabled (by Failxos).");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    /** (Re)loads config, menus and join items and refreshes all online players. */
    public void loadAll() {
        reloadConfig();
        ItemsAdderHook.resetWarnings();

        menus.clear();
        ConfigurationSection menuSection = getConfig().getConfigurationSection("menus");
        if (menuSection != null) {
            for (String key : menuSection.getKeys(false)) {
                ConfigurationSection s = menuSection.getConfigurationSection(key);
                if (s != null) {
                    menus.put(key.toLowerCase(Locale.ROOT), MenuDef.parse(key, s, getLogger()));
                }
            }
        }
        joinItems.load(getConfig().getConfigurationSection("join-items"));

        // prefetch skins of all heads that use a skin name
        Set<String> names = new HashSet<>();
        for (MenuDef menu : menus.values()) {
            for (ItemDef def : menu.items()) {
                if (HeadResolver.isName(def.head)) {
                    names.add(def.head);
                }
            }
        }
        for (ItemDef def : joinItems.allDefs()) {
            if (HeadResolver.isName(def.head)) {
                names.add(def.head);
            }
        }
        names.forEach(name -> HeadResolver.prefetch(this, name));

        for (Player player : Bukkit.getOnlinePlayers()) {
            joinItems.refresh(player);
            visibility.apply(player);
        }
    }

    public boolean openMenu(Player player, String id) {
        MenuDef menu = menus.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) {
            return false;
        }
        player.openInventory(menu.create(this, player));
        actions.run(player, menu.openActions());
        return true;
    }

    public Set<String> menuIds() {
        return menus.keySet();
    }

    /** Sends a message from the "messages" config section. Pairs are placeholder, value. */
    public void msg(CommandSender target, String key, String... replacements) {
        String text = getConfig().getString("messages." + key, "");
        if (text.isEmpty()) {
            return;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        String prefix = getConfig().getString("messages.prefix", "");
        target.sendMessage(Text.parse(prefix + text));
    }

    public JoinItemManager joinItems() {
        return joinItems;
    }

    public PlayerVisibility visibility() {
        return visibility;
    }

    public ActionExecutor actions() {
        return actions;
    }

    public NamespacedKey itemKey() {
        return itemKey;
    }

    public NamespacedKey hiddenKey() {
        return hiddenKey;
    }
}
