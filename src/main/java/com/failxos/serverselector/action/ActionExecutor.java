package com.failxos.serverselector.action;

import com.failxos.serverselector.ServerSelectorPlugin;
import com.failxos.serverselector.util.Text;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Runs action lists from the config.
 * Formats: [player] cmd, [console] cmd, [op] cmd, [message] text, [broadcast] text,
 * [sound] key;volume;pitch, [title] title;subtitle, [actionbar] text, [server] name,
 * [open] menu, [close], [toggle-players], [delay] ticks. No tag = player command.
 */
public final class ActionExecutor {

    private final ServerSelectorPlugin plugin;

    public ActionExecutor(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    public void run(Player player, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            return;
        }
        long delay = 0;
        for (String raw : actions) {
            String line = Text.placeholders(player, raw.trim());
            if (line.regionMatches(true, 0, "[delay]", 0, 7)) {
                try {
                    delay += Long.parseLong(line.substring(7).trim());
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("Invalid [delay] value: " + line);
                }
                continue;
            }
            if (delay <= 0) {
                execute(player, line);
            } else {
                final String l = line;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        execute(player, l);
                    }
                }, delay);
            }
        }
    }

    private void execute(Player player, String line) {
        String tag = "";
        String arg = line;
        if (line.startsWith("[")) {
            int end = line.indexOf(']');
            if (end > 0) {
                tag = line.substring(1, end).toLowerCase(Locale.ROOT);
                arg = line.substring(end + 1).trim();
            }
        }

        switch (tag) {
            case "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(arg));
            case "op" -> {
                if (!plugin.getConfig().getBoolean("settings.allow-op-actions", false)) {
                    plugin.getLogger().warning("[op] actions are disabled (settings.allow-op-actions).");
                    return;
                }
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(stripSlash(arg));
                } finally {
                    if (!wasOp) {
                        player.setOp(false);
                    }
                }
            }
            case "message" -> player.sendMessage(Text.parse(arg));
            case "broadcast" -> Bukkit.broadcast(Text.parse(arg));
            case "actionbar" -> player.sendActionBar(Text.parse(arg));
            case "title" -> {
                String[] parts = arg.split(";", 2);
                player.showTitle(Title.title(Text.parse(parts[0]), Text.parse(parts.length > 1 ? parts[1] : "")));
            }
            case "sound" -> playSound(player, arg);
            case "server" -> {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("Connect");
                out.writeUTF(arg);
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
            case "open" -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.openMenu(player, arg)) {
                    plugin.msg(player, "unknown-menu", "%menu%", arg);
                }
            });
            case "close" -> Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
            case "toggle-players" -> plugin.visibility().toggle(player);
            case "", "player" -> player.performCommand(stripSlash(arg));
            default -> plugin.getLogger().warning("Unknown action tag: [" + tag + "]");
        }
    }

    private void playSound(Player player, String arg) {
        String[] parts = arg.split(";");
        String key = parts[0].trim();
        if (!key.contains(".") && !key.contains(":")) {
            key = key.toLowerCase(Locale.ROOT).replace('_', '.');
        }
        float volume = 1f;
        float pitch = 1f;
        try {
            if (parts.length > 1) {
                volume = Float.parseFloat(parts[1].trim());
            }
            if (parts.length > 2) {
                pitch = Float.parseFloat(parts[2].trim());
            }
        } catch (NumberFormatException ignored) {
            // keep defaults
        }
        player.playSound(player.getLocation(), key, SoundCategory.MASTER, volume, pitch);
    }

    private static String stripSlash(String cmd) {
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }
}
