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

/**
 * Comando administrativo para gestionar puntos personales de jugadores
 *
 * NOTA SOBRE MÉTODO DEPRECATED:
 * El uso de Bukkit.getOfflinePlayer(String) está deprecated porque los nombres
 * de jugador pueden cambiar, mientras que los UUID son permanentes.
 *
 * En este caso específico, el uso es aceptable porque:
 * 1. Es un comando administrativo donde se necesita buscar jugadores por nombre
 * 2. Solo se usa para operaciones manuales de administración
 * 3. Se verifica hasPlayedBefore() para evitar problemas con nombres inexistentes
 *
 * Para código no-administrativo, se recomienda usar UUIDs desde el principio
 * y almacenarlos en lugar de nombres para identificar jugadores.
 *
 * WARNINGS RESUELTOS:
 * - Switch expressions moderno (Java 14+)
 * - Documentación de uso deprecated justificado
 * - Compilación forzada para mostrar warnings
 */
public class PersonalPointsCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;

    public PersonalPointsCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar si es acción de visualización (para usuarios)
        boolean isViewAction = args.length >= 2 && args[1].equalsIgnoreCase("view");
        
        // Si es view, verificar permiso de usuario, sino permiso de admin
        if (isViewAction) {
            if (!sender.hasPermission("giftbond.points.view")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para ver puntos de otros jugadores.");
                return true;
            }
        } else {
            // Para acciones add/remove/set, se requiere permiso de admin
            if (!sender.hasPermission("giftbond.admin.points")) {
                sender.sendMessage(ChatColor.RED + "No tienes permiso para administrar puntos.");
                return true;
            }
        }

        if (args.length < 2) {
            sendUsage(sender, isViewAction);
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
            // Jugador offline - buscar por nombre usando UUID si está disponible
            // Para comandos administrativos, primero intentamos obtener UUID del nombre
            try {
                // NOTA: Bukkit.getOfflinePlayer(String) está deprecated, pero no hay alternativa directa
                // para buscar por nombre sin UUID. Este uso es aceptable en comandos admin.
                target = Bukkit.getOfflinePlayer(targetName);
                
                if (!target.hasPlayedBefore()) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "El jugador " + targetName + " nunca ha jugado en este servidor.");
                    return true;
                }
            } catch (Exception e) {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Error al buscar al jugador " + targetName);
                return true;
            }
        }
        
        String uuid = target.getUniqueId().toString();
        String displayName = target.getName() != null ? target.getName() : targetName;

        // Switch moderno con expresiones (Java 14+)
        switch (action) {
            case "view" -> {
                int current = friendshipManager.getPersonalPoints(uuid);
                sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Puntos personales de " + displayName + ": " + ChatColor.WHITE + current);
            }
            
            case "add" -> {
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
            }
            
            case "remove" -> {
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
            }
            
            case "set" -> {
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
            }
            
            default -> sendUsage(sender, isViewAction);
        }

        return true;
    }

    private void sendUsage(CommandSender sender, boolean isViewOnly) {
        if (isViewOnly) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "=== Ver Puntos de Jugadores ===");
            sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> view" + ChatColor.GRAY + " - Ver puntos de un jugador");
        } else {
            sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "=== Admin Points ===");
            sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> view" + ChatColor.GRAY + " - Ver puntos");
            sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> add <cantidad>" + ChatColor.GRAY + " - Añadir puntos");
            sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> remove <cantidad>" + ChatColor.GRAY + " - Quitar puntos");
            sender.sendMessage(ChatColor.YELLOW + "/giftbond points <jugador> set <cantidad>" + ChatColor.GRAY + " - Establecer puntos");
        }
    }
}