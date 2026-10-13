package com.failxos.serverselector.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

/** Text helpers: MiniMessage + legacy (&) colors + placeholders. */
public final class Text {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern HEX = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Map<Character, String> LEGACY = Map.ofEntries(
            Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"), Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"), Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"), Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"), Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"), Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"), Map.entry('r', "<reset>")
    );

    private static Method papiMethod;

    private Text() {
    }

    private static String legacyToMini(String input) {
        String s = HEX.matcher(input).replaceAll("<#$1>");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c == '&' || c == '\u00A7') && i + 1 < s.length()) {
                String tag = LEGACY.get(Character.toLowerCase(s.charAt(i + 1)));
                if (tag != null) {
                    sb.append(tag);
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Parses text to a component with italic disabled by default. */
    public static Component parse(String input) {
        if (input == null) {
            return Component.empty();
        }
        return Component.empty()
                .decoration(TextDecoration.ITALIC, false)
                .append(MM.deserialize(legacyToMini(input)));
    }

    /** Replaces built-in placeholders and PlaceholderAPI (if installed). */
    public static String placeholders(Player player, String input) {
        if (input == null) {
            return "";
        }
        String s = input;
        if (player != null) {
            s = s.replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%world%", player.getWorld().getName())
                    .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
            s = papi(player, s);
        }
        return s;
    }

    private static String papi(Player player, String s) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return s;
        }
        try {
            if (papiMethod == null) {
                papiMethod = Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                        .getMethod("setPlaceholders", Player.class, String.class);
            }
            return (String) papiMethod.invoke(null, player, s);
        } catch (Throwable ignored) {
            return s;
        }
    }
}
