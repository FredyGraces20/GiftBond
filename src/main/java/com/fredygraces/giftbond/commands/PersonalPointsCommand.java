package com.fredygraces.giftbond.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.FriendshipManager;

public class PersonalPointsCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;

    public PersonalPointsCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("giftbond.admin.points")) {
            sender.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String targetName = args[0];
        String action = args[1].toLowerCase();
        
        // Intentar obtener el jugador online primero
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target;
        
        if (targetPlayer != null) {
            // Jugador está online
            target = targetPlayer;
        } else {
            // Jugador offline - buscar por UUID desde nombre
            target = Bukkit.getOfflinePlayer(targetName);
            
            // Verificar que el jugador haya jugado antes
            if (!target.hasPlayedBefore()) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "El jugador " + targetName + " nunca ha jugado en este servidor.");
                return true;
            }
        }
        
        String uuid = target.getUniqueId().toString();
        String displayName = target.getName() != null ? target.getName() : targetName;

        switch (action) {
            case "view":
                int current = friendshipManager.getPersonalPoints(uuid);
                sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Puntos personales de " + displayName + ": " + ChatColor.WHITE + current);
                break;

            case "add":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /giftbond points <jugador> add <cantidad>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    friendshipManager.addPersonalPoints(uuid, amount);
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Se han añadido " + amount + " puntos a " + displayName + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Cantidad inválida.");
                }
                break;

            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /giftbond points <jugador> remove <cantidad>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    boolean success = friendshipManager.spendPersonalPoints(uuid, amount);
                    if (success) {
                        sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Se han quitado " + amount + " puntos a " + displayName + ".");
                    } else {
                        sender.sendMessage(ChatColor.RED + displayName + " no tiene suficientes puntos.");
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Cantidad inválida.");
                }
                break;

            case "set":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Uso: /giftbond points <jugador> set <cantidad>");
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    friendshipManager.setPersonalPoints(uuid, amount);
                    sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Puntos de " + displayName + " establecidos en " + amount + ".");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Cantidad inválida.");
                }
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "=== Admin Points ===");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> view" + ChatColor.GRAY + " - Ver puntos");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> add <cantidad>" + ChatColor.GRAY + " - Añadir puntos");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> remove <cantidad>" + ChatColor.GRAY + " - Quitar puntos");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> set <cantidad>" + ChatColor.GRAY + " - Establecer puntos");
    }
}
