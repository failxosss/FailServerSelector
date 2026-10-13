package com.failxos.serverselector.command;

import com.failxos.serverselector.ServerSelectorPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /serverselector (alias /ss): reload, open, refresh. */
public final class SelectorCommand implements CommandExecutor, TabCompleter {

    private final ServerSelectorPlugin plugin;

    public SelectorCommand(ServerSelectorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.msg(sender, "usage");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                if (!sender.hasPermission("serverselector.admin")) {
                    plugin.msg(sender, "no-permission");
                    return true;
                }
                plugin.loadAll();
                plugin.msg(sender, "reload");
            }
            case "open" -> {
                if (args.length < 2) {
                    plugin.msg(sender, "usage");
                    return true;
                }
                Player target = resolveTarget(sender, args, 2, "serverselector.open");
                if (target == null) {
                    return true;
                }
                if (!plugin.openMenu(target, args[1])) {
                    plugin.msg(sender, "unknown-menu", "%menu%", args[1]);
                }
            }
            case "refresh" -> {
                Player target = resolveTarget(sender, args, 1, "serverselector.admin");
                if (target == null) {
                    return true;
                }
                plugin.joinItems().refresh(target);
                plugin.visibility().apply(target);
                plugin.msg(sender, "refreshed", "%player%", target.getName());
            }
            default -> plugin.msg(sender, "usage");
        }
        return true;
    }

    /** Self if no name was given, otherwise the named player (admin only). */
    private Player resolveTarget(CommandSender sender, String[] args, int nameIndex, String selfPermission) {
        if (args.length > nameIndex) {
            if (!sender.hasPermission("serverselector.admin")) {
                plugin.msg(sender, "no-permission");
                return null;
            }
            Player target = Bukkit.getPlayerExact(args[nameIndex]);
            if (target == null) {
                plugin.msg(sender, "player-not-found", "%player%", args[nameIndex]);
            }
            return target;
        }
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return null;
        }
        if (!sender.hasPermission(selfPermission)) {
            plugin.msg(sender, "no-permission");
            return null;
        }
        return player;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("open", "refresh", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open")) {
            for (String id : plugin.menuIds()) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(id);
                }
            }
        } else if ((args.length == 3 && args[0].equalsIgnoreCase("open"))
                || (args.length == 2 && args[0].equalsIgnoreCase("refresh"))) {
            String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
