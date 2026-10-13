package com.failxos.serverselector.menu;

import com.failxos.serverselector.ServerSelectorPlugin;
import com.failxos.serverselector.item.ItemDef;
import com.failxos.serverselector.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** A menu parsed from the config. */
public final class MenuDef {

    private final String id;
    private final String title;
    private final int rows;
    private final List<String> openActions;
    private final Map<Integer, ItemDef> slotItems;

    private MenuDef(String id, String title, int rows, List<String> openActions, Map<Integer, ItemDef> slotItems) {
        this.id = id;
        this.title = title;
        this.rows = rows;
        this.openActions = openActions;
        this.slotItems = slotItems;
    }

    public static MenuDef parse(String id, ConfigurationSection s, Logger log) {
        String title = s.getString("title", "Menu");
        int rows = Math.min(6, Math.max(1, s.getInt("rows", 3)));
        List<String> open = s.getStringList("open-actions");
        Map<Integer, ItemDef> map = new LinkedHashMap<>();

        // 1) filler layout
        ConfigurationSection fill = s.getConfigurationSection("fill");
        if (fill != null) {
            Map<Character, ItemDef> legend = new HashMap<>();
            ConfigurationSection legendSec = fill.getConfigurationSection("legend");
            if (legendSec != null) {
                for (String key : legendSec.getKeys(false)) {
                    ConfigurationSection ls = legendSec.getConfigurationSection(key);
                    if (ls != null && key.length() == 1) {
                        legend.put(key.charAt(0), ItemDef.parse(id + ".legend." + key, ls, log));
                    }
                }
            }
            List<String> layout = fill.getStringList("layout");
            for (int r = 0; r < layout.size() && r < rows; r++) {
                String row = layout.get(r);
                for (int c = 0; c < 9 && c < row.length(); c++) {
                    ItemDef def = legend.get(row.charAt(c));
                    if (def != null) {
                        map.put(r * 9 + c, def);
                    }
                }
            }
        }

        // 2) real items (override filler)
        ConfigurationSection items = s.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection is = items.getConfigurationSection(key);
                if (is == null) {
                    continue;
                }
                ItemDef def = ItemDef.parse(id + "." + key, is, log);
                if (!def.enabled) {
                    continue;
                }
                for (int slot : def.slots) {
                    if (slot >= 0 && slot < rows * 9) {
                        map.put(slot, def);
                    } else {
                        log.warning("[" + id + "." + key + "] Slot " + slot + " is outside the menu (0-" + (rows * 9 - 1) + ").");
                    }
                }
            }
        }
        return new MenuDef(id, title, rows, open, map);
    }

    public String id() {
        return id;
    }

    public List<String> openActions() {
        return openActions;
    }

    public ItemDef item(int slot) {
        return slotItems.get(slot);
    }

    public Collection<ItemDef> items() {
        return slotItems.values();
    }

    public Inventory create(ServerSelectorPlugin plugin, Player player) {
        MenuHolder holder = new MenuHolder(this);
        Inventory inv = Bukkit.createInventory(holder, rows * 9, Text.parse(Text.placeholders(player, title)));
        holder.setInventory(inv);
        for (Map.Entry<Integer, ItemDef> e : slotItems.entrySet()) {
            inv.setItem(e.getKey(), e.getValue().build(plugin, player));
        }
        return inv;
    }
}
