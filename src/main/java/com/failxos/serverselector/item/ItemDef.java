package com.failxos.serverselector.item;

import com.failxos.serverselector.ServerSelectorPlugin;
import com.failxos.serverselector.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/** One configurable item (hotbar item, menu item or filler). */
public final class ItemDef {

    public final String id;
    public final boolean enabled;
    public final Material material;
    public final int amount;
    public final String name;
    public final List<String> lore;
    public final String head;
    public final String itemsAdder;
    public final int customModelData;
    public final String itemModel;
    public final boolean glow;
    public final boolean unbreakable;
    public final boolean hideTooltip;
    public final List<ItemFlag> flags;
    public final List<Integer> slots;
    public final WorldFilter worlds;
    public final String permission;
    public final Set<Click> clickTypes;
    public final long cooldownTicks;
    public final List<String> actions;
    public final List<String> leftActions;
    public final List<String> rightActions;
    public final List<String> shiftLeftActions;
    public final List<String> shiftRightActions;

    private ItemDef(String id, ConfigurationSection s, Logger log) {
        this.id = id;
        this.enabled = s.getBoolean("enabled", true);

        String headSpec = s.getString("head");
        if (headSpec != null && headSpec.isBlank()) {
            headSpec = null;
        }
        this.head = headSpec;

        Material mat = Material.STONE;
        if (s.contains("material")) {
            Material parsed = Material.matchMaterial(s.getString("material", "STONE"));
            if (parsed == null || !parsed.isItem()) {
                log.warning("[" + id + "] Invalid material '" + s.getString("material") + "', using STONE.");
            } else {
                mat = parsed;
            }
        }
        if (this.head != null) {
            mat = Material.PLAYER_HEAD;
        }
        this.material = mat;

        this.amount = Math.max(1, s.getInt("amount", 1));
        this.name = s.getString("name");
        this.lore = new ArrayList<>(s.getStringList("lore"));
        String ia = s.getString("itemsadder");
        this.itemsAdder = (ia == null || ia.isBlank()) ? null : ia.trim();
        this.customModelData = s.getInt("custom-model-data", -1);
        String model = s.getString("item-model");
        this.itemModel = (model == null || model.isBlank()) ? null : model.trim();
        this.glow = s.getBoolean("glow", false);
        this.unbreakable = s.getBoolean("unbreakable", false);
        this.hideTooltip = s.getBoolean("hide-tooltip", false);

        List<ItemFlag> flagList = new ArrayList<>();
        for (String f : s.getStringList("flags")) {
            try {
                flagList.add(ItemFlag.valueOf(f.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                log.warning("[" + id + "] Unknown item flag: " + f);
            }
        }
        this.flags = flagList;

        List<Integer> slotList = new ArrayList<>();
        if (s.isInt("slot")) {
            slotList.add(s.getInt("slot"));
        }
        slotList.addAll(s.getIntegerList("slots"));
        this.slots = slotList;

        this.worlds = WorldFilter.parse(s.getConfigurationSection("worlds"));
        String perm = s.getString("permission");
        this.permission = (perm == null || perm.isBlank()) ? null : perm.trim();

        Set<Click> clicks = EnumSet.noneOf(Click.class);
        List<String> configured = s.getStringList("click-types");
        if (configured.isEmpty()) {
            clicks.add(Click.RIGHT);
            clicks.add(Click.SHIFT_RIGHT);
        } else {
            for (String c : configured) {
                try {
                    clicks.add(Click.valueOf(c.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    log.warning("[" + id + "] Unknown click type: " + c);
                }
            }
        }
        this.clickTypes = clicks;
        this.cooldownTicks = s.getLong("cooldown-ticks", 10L);

        this.actions = s.getStringList("actions");
        this.leftActions = s.getStringList("left-click-actions");
        this.rightActions = s.getStringList("right-click-actions");
        this.shiftLeftActions = s.getStringList("shift-left-click-actions");
        this.shiftRightActions = s.getStringList("shift-right-click-actions");
    }

    public static ItemDef parse(String id, ConfigurationSection section, Logger log) {
        return new ItemDef(id, section, log);
    }

    public int firstSlot() {
        return slots.isEmpty() ? 0 : slots.get(0);
    }

    /** Actions to run for a given click (general actions + click specific ones). */
    public List<String> actionsFor(Click click) {
        List<String> out = new ArrayList<>(actions);
        switch (click) {
            case LEFT -> out.addAll(leftActions);
            case RIGHT -> out.addAll(rightActions);
            case SHIFT_LEFT -> out.addAll(shiftLeftActions);
            case SHIFT_RIGHT -> out.addAll(shiftRightActions);
        }
        return out;
    }

    public ItemStack build(ServerSelectorPlugin plugin, Player player) {
        ItemStack stack = null;
        if (itemsAdder != null) {
            stack = ItemsAdderHook.get(plugin.getLogger(), itemsAdder);
        }
        boolean fromItemsAdder = stack != null;
        if (!fromItemsAdder) {
            stack = new ItemStack(material, amount);
        } else {
            stack.setAmount(amount);
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        if (!fromItemsAdder && head != null && meta instanceof SkullMeta skull) {
            HeadResolver.apply(skull, head);
        }
        if (name != null) {
            meta.displayName(Text.parse(Text.placeholders(player, name)));
        }
        if (!lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String line : lore) {
                lines.add(Text.parse(Text.placeholders(player, line)));
            }
            meta.lore(lines);
        }
        if (hideTooltip) {
            meta.setHideTooltip(true);
        }
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (unbreakable) {
            meta.setUnbreakable(true);
        }
        for (ItemFlag flag : flags) {
            meta.addItemFlags(flag);
        }
        if (customModelData >= 0) {
            meta.setCustomModelData(customModelData);
        }
        if (itemModel != null) {
            NamespacedKey key = NamespacedKey.fromString(itemModel);
            if (key != null) {
                meta.setItemModel(key);
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }
}
