package com.failxos.serverselector.item;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies a head spec to a skull. Supported specs:
 * SkinName, texture:BASE64, url:http://textures.minecraft.net/texture/..., uuid:xxxxxxxx-...
 */
public final class HeadResolver {

    private static final Map<String, PlayerProfile> CACHE = new ConcurrentHashMap<>();

    private HeadResolver() {
    }

    /** True if the spec is a plain skin name (so it can be prefetched). */
    public static boolean isName(String spec) {
        if (spec == null) {
            return false;
        }
        String l = spec.toLowerCase(Locale.ROOT);
        return !l.startsWith("texture:") && !l.startsWith("url:") && !l.startsWith("uuid:") && spec.length() <= 32;
    }

    public static void prefetch(Plugin plugin, String name) {
        if (CACHE.containsKey(name.toLowerCase(Locale.ROOT))) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerProfile profile = Bukkit.createProfile(name);
                if (profile.complete(true) && profile.hasTextures()) {
                    CACHE.put(name.toLowerCase(Locale.ROOT), profile);
                }
            } catch (Exception ignored) {
                // lookup failed, the client will try to resolve the name itself
            }
        });
    }

    public static void apply(SkullMeta meta, String rawSpec) {
        String spec = rawSpec.trim();
        String lower = spec.toLowerCase(Locale.ROOT);
        if (lower.startsWith("texture:")) {
            texture(meta, spec.substring(8).trim());
        } else if (lower.startsWith("url:")) {
            String url = spec.substring(4).trim();
            String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url + "\"}}}";
            texture(meta, Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
        } else if (lower.startsWith("uuid:")) {
            try {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(UUID.fromString(spec.substring(5).trim())));
            } catch (IllegalArgumentException ignored) {
                // invalid uuid
            }
        } else if (spec.length() > 32) {
            texture(meta, spec);
        } else {
            PlayerProfile cached = CACHE.get(lower);
            meta.setPlayerProfile(cached != null ? cached : Bukkit.createProfile(spec));
        }
    }

    private static void texture(SkullMeta meta, String base64) {
        PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64.getBytes(StandardCharsets.UTF_8)), null);
        profile.setProperty(new ProfileProperty("textures", base64));
        meta.setPlayerProfile(profile);
    }
}
