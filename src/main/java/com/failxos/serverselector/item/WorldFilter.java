package com.failxos.serverselector.item;

import org.bukkit.configuration.ConfigurationSection;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Decides in which worlds an item is available. Empty list = every world. */
public final class WorldFilter {

    private final boolean whitelist;
    private final Set<String> worlds;

    private WorldFilter(boolean whitelist, Set<String> worlds) {
        this.whitelist = whitelist;
        this.worlds = worlds;
    }

    public static WorldFilter parse(ConfigurationSection section) {
        if (section == null) {
            return new WorldFilter(true, new HashSet<>());
        }
        boolean whitelist = !"BLACKLIST".equalsIgnoreCase(section.getString("mode", "WHITELIST"));
        Set<String> set = new HashSet<>();
        for (String w : section.getStringList("list")) {
            set.add(w.toLowerCase(Locale.ROOT));
        }
        return new WorldFilter(whitelist, set);
    }

    public boolean allows(String worldName) {
        if (worlds.isEmpty()) {
            return true;
        }
        boolean contained = worlds.contains(worldName.toLowerCase(Locale.ROOT));
        return whitelist == contained;
    }
}
