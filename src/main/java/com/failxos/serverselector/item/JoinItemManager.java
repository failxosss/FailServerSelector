package com.failxos.serverselector.item;

import com.failxos.serverselector.ServerSelectorPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Manages the items given to players (selector, player toggle, ...). */
public final class JoinItemManager {

    public static final class JoinItem {
        public final String id;
        public final boolean toggle;
        public final ItemDef def;
        public final ItemDef shown;
        public final ItemDef hidden;

        JoinItem(String id, boolean toggle, ItemDef def, ItemDef shown, ItemDef hidden) {
            this.id = id;
            this.toggle = toggle;
            this.def = def;
            this.shown = shown;
            this.hidden = hidden;
        }
    }

    private final ServerSelectorPlugin plugin;
    private final Map<String, JoinItem> items = new LinkedHashMap<>();
    private JoinItem toggleItem;

    public JoinItemManager(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(ConfigurationSection section) {
        items.clear();
        toggleItem = null;
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            ItemDef def = ItemDef.parse(key, s, plugin.getLogger());
            boolean toggle = "PLAYER_TOGGLE".equalsIgnoreCase(s.getString("type", "ITEM"));
            JoinItem item;
            if (toggle) {
                ConfigurationSection shown = s.getConfigurationSection("shown");
                ConfigurationSection hidden = s.getConfigurationSection("hidden");
                if (shown == null || hidden == null) {
                    plugin.getLogger().warning("[" + key + "] PLAYER_TOGGLE needs 'shown' and 'hidden' sections - skipped.");
                    continue;
                }
                item = new JoinItem(key, true,
                        def,
                        ItemDef.parse(key + ".shown", shown, plugin.getLogger()),
                        ItemDef.parse(key + ".hidden", hidden, plugin.getLogger()));
                if (toggleItem == null) {
                    toggleItem = item;
                }
            } else {
                item = new JoinItem(key, false, def, null, null);
            }
            items.put(key, item);
        }
    }

    public JoinItem get(String id) {
        return items.get(id);
    }

    public List<ItemDef> allDefs() {
        List<ItemDef> out = new ArrayList<>();
        for (JoinItem ji : items.values()) {
            out.add(ji.def);
            if (ji.shown != null) {
                out.add(ji.shown);
            }
            if (ji.hidden != null) {
                out.add(ji.hidden);
            }
        }
        return out;
    }

    /** The hidden/shown definition of the player toggle (or null if none configured). */
    public ItemDef toggleDef(boolean hidden) {
        if (toggleItem == null) {
            return null;
        }
        return hidden ? toggleItem.hidden : toggleItem.shown;
    }

    /** True if the player toggle is available in the player's current world. */
    public boolean toggleAllowed(Player player) {
        return toggleItem == null || isAllowed(toggleItem, player);
    }

    public boolean isAllowed(JoinItem item, Player player) {
        ItemDef d = item.def;
        return d.enabled
                && d.worlds.allows(player.getWorld().getName())
                && (d.permission == null || player.hasPermission(d.permission));
    }

    public String tagId(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta.getPersistentDataContainer().get(plugin.itemKey(), PersistentDataType.STRING);
    }

    public boolean isTagged(ItemStack stack) {
        return tagId(stack) != null;
    }

    private ItemStack stackFor(JoinItem item, Player player) {
        ItemDef d = item.def;
        if (item.toggle) {
            d = plugin.visibility().isHidden(player) ? item.hidden : item.shown;
        }
        ItemStack stack = d.build(plugin, player);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(plugin.itemKey(), PersistentDataType.STRING, item.id);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Removes all plugin items and gives back the ones allowed in the player's current world. */
    public void refresh(Player player) {
        PlayerInventory inv = player.getInventory();
        boolean force = plugin.getConfig().getBoolean("settings.force-slot", true);

        for (int i = 0; i < inv.getSize(); i++) {
            if (isTagged(inv.getItem(i))) {
                inv.setItem(i, null);
            }
        }

        List<ItemStack> displaced = new ArrayList<>();
        for (JoinItem item : items.values()) {
            if (!isAllowed(item, player)) {
                continue;
            }
            int slot = item.def.firstSlot();
            if (slot < 0 || slot > 40) {
                continue;
            }
            ItemStack existing = inv.getItem(slot);
            boolean occupied = existing != null && !existing.getType().isAir();
            if (occupied) {
                if (!force) {
                    continue;
                }
                displaced.add(existing.clone());
            }
            inv.setItem(slot, stackFor(item, player));
        }

        for (ItemStack stack : displaced) {
            for (ItemStack left : inv.addItem(stack).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }
    }
}
