package com.fredygraces.giftbond.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import com.fredygraces.giftbond.GiftBond;

public class GiftBondCommand implements CommandExecutor, TabCompleter {
    private final GiftBond plugin;
    private final ReloadCommand reloadHandler;
    private final SaveDataCommand saveDataHandler;
    private final PersonalPointsCommand pointsHandler;
    private final BoostCommand boostHandler;
    private final DebugCommand debugHandler;

    public GiftBondCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.reloadHandler = new ReloadCommand(plugin);
        this.saveDataHandler = new SaveDataCommand(plugin);
        this.pointsHandler = new PersonalPointsCommand(plugin);
        this.boostHandler = new BoostCommand(plugin);
        this.debugHandler = new DebugCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "reload":
                return reloadHandler.onCommand(sender, command, label, subArgs);
            case "savedata":
                return saveDataHandler.onCommand(sender, command, label, subArgs);
            case "points":
                return pointsHandler.onCommand(sender, command, label, subArgs);
            case "boost":
                return boostHandler.onCommand(sender, command, label, subArgs);
            case "debug":
                return debugHandler.onCommand(sender, command, label, subArgs);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "=== Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond reload " + ChatColor.GRAY + "- Recargar configuración");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond savedata " + ChatColor.GRAY + "- Backup manual de BD");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> <view|add|remove|set> [cantidad] " + ChatColor.GRAY + "- Gestionar puntos");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond boost <jugador> <multiplicador> [minutos] " + ChatColor.GRAY + "- Boost temporal");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond debug <on|off> " + ChatColor.GRAY + "- Activar/desactivar mensajes debug");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> cmds = Arrays.asList("reload", "savedata", "points", "boost", "debug");
            StringUtil.copyPartialMatches(args[0], cmds, completions);
            Collections.sort(completions);
            return completions;
        }
        
        if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            // Implementación básica de tab complete para subcomandos si es necesario
            // Por simplicidad, retornamos null para usar el comportamiento por defecto (nombres de jugadores)
            // excepto para 'points' que tiene argumentos específicos
            if (subCommand.equals("points") && args.length == 3) {
                List<String> completions = new ArrayList<>();
                List<String> actions = Arrays.asList("view", "add", "remove", "set");
                StringUtil.copyPartialMatches(args[2], actions, completions);
                return completions;
            }
        }

        return null;
    }
}
