package com.fredygraces.giftbond.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.fredygraces.giftbond.GiftBond;

/**
 * Comando principal unificado de GiftBond con todos los subcomandos
 * /giftbond <amistad|top|reedem|reload|savedata|points|boost|debug>
 */
public class GiftBondUnifiedCommand implements CommandExecutor, TabCompleter {
    private final GiftBond plugin;
    private final AmistadCommand amistadCommand;
    private final TopRegalosCommand topRegalosCommand;
    private final MailboxCommand mailboxCommand;
    private final RegaloCommand regaloCommand;
    private final ReloadCommand reloadCommand;
    private final SaveDataCommand saveDataCommand;
    private final PersonalPointsCommand personalPointsCommand;
    private final BoostCommand boostCommand;
    private final DebugCommand debugCommand;

    public GiftBondUnifiedCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.amistadCommand = new AmistadCommand(plugin);
        this.topRegalosCommand = new TopRegalosCommand(plugin);
        this.mailboxCommand = new MailboxCommand(plugin);
        this.regaloCommand = new RegaloCommand(plugin);
        this.reloadCommand = new ReloadCommand(plugin);
        this.saveDataCommand = new SaveDataCommand(plugin);
        this.personalPointsCommand = new PersonalPointsCommand(plugin);
        this.boostCommand = new BoostCommand(plugin);
        this.debugCommand = new DebugCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Si no hay argumentos, mostrar ayuda
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);

        switch (subCommand) {
            case "friends" -> {
                if (!sender.hasPermission("giftbond.friends")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return amistadCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "top" -> {
                if (!sender.hasPermission("giftbond.top")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return topRegalosCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "send" -> {
                if (!sender.hasPermission("giftbond.send")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return regaloCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "redeem" -> {
                if (!sender.hasPermission("giftbond.redeem")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return mailboxCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "reload" -> {
                if (!sender.hasPermission("giftbond.admin.reload")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return reloadCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "savedata" -> {
                if (!sender.hasPermission("giftbond.savedata")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return saveDataCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "points" -> {
                if (!sender.hasPermission("giftbond.admin.points")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return personalPointsCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "boost" -> {
                if (!sender.hasPermission("giftbond.admin.boost")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return boostCommand.onCommand(sender, command, label, subArgs);
            }
                
            case "debug" -> {
                if (!sender.hasPermission("giftbond.admin.debug")) {
                    sender.sendMessage(plugin.getPrefix() + "§cNo tienes permiso para usar este comando.");
                    return true;
                }
                return debugCommand.onCommand(sender, command, label, subArgs);
            }
                
            default -> {
                sender.sendMessage(plugin.getPrefix() + "§cUnknown subcommand: " + subCommand);
                showHelp(sender);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = Arrays.asList(
                "friends", "top", "send", "redeem", "reload", "savedata", "points", "boost", "debug"
            );
            
            List<String> matches = new ArrayList<>();
            String search = args[0].toLowerCase();
            
            for (String completion : completions) {
                if (completion.startsWith(search)) {
                    matches.add(completion);
                }
            }
            
            return matches;
        }
        
        // Tab completion para subcomandos específicos
        if (args.length > 1) {
            String subCommand = args[0].toLowerCase();
            
            // Solo algunos comandos tienen tab completion
            switch (subCommand) {
                case "points", "boost", "debug", "redeem" -> {
                    // Estos comandos no implementan TabCompleter, retornar vacío
                    return Collections.emptyList();
                }
            }
        }
        
        return Collections.emptyList();
    }

    private void showHelp(CommandSender sender) {
        String prefix = plugin.getPrefix();
        sender.sendMessage(prefix + "§e=== GiftBond Commands ===");
        sender.sendMessage("§6/giftbond friends §7- View your friendship points");
        sender.sendMessage("§6/giftbond top §7- View couples leaderboard");
        sender.sendMessage("§6/giftbond send §7- Send gifts to other players");
        sender.sendMessage("§6/giftbond redeem §7- Claim pending gifts");
        
        if (sender.hasPermission("giftbond.admin")) {
            sender.sendMessage("§6/giftbond reload §7- Reload configuration");
            sender.sendMessage("§6/giftbond savedata §7- Create data backup");
            sender.sendMessage("§6/giftbond points §7- Manage points");
            sender.sendMessage("§6/giftbond boost §7- Give boosts to players");
            sender.sendMessage("§6/giftbond debug §7- Control debug mode");
        }
        
        sender.sendMessage(prefix + "§7Use §f/giftbond <command>§7 for more information");
    }

    // Método que utiliza el plugin para acceder a funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
}