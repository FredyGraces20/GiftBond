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
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission", "{prefix}&cNo tienes permiso para usar este comando.")));
                return true;
            }
        } else {
            // Para acciones add/remove/set, se requiere permiso de admin
            if (!sender.hasPermission("giftbond.admin.points")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission", "{prefix}&cNo tienes permiso para usar este comando.")));
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
                // SUPPRESS-WARNING: Uso aceptable de método deprecated en contexto administrativo
                // Justificación: Comando admin donde se requiere búsqueda por nombre de jugador
                // Alternativas evaluadas:
                // 1. Bukkit.getPlayerExact() - solo para jugadores online
                // 2. Bukkit.getOfflinePlayer(UUID) - requiere conocer UUID previamente
                // 3. APIs externas - complejidad innecesaria para función administrativa
                // La validación hasPlayedBefore() mitiga riesgos de nombres inválidos
                @SuppressWarnings("deprecation")
                OfflinePlayer tempTarget = Bukkit.getOfflinePlayer(targetName);
                target = tempTarget;
                
                if (!target.hasPlayedBefore()) {
                    String msg = plugin.getMessage("errors.player_not_found", "{prefix}&cJugador no encontrado: {player}")
                            .replace("{player}", targetName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
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
                String msg = plugin.getMessage("info.personal_points_view", "{prefix}&ePuntos personales de &f{player}&e: &f{points}")
                        .replace("{player}", displayName)
                        .replace("{points}", String.valueOf(current));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
            
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_add", "&cUso: /giftbond points <jugador> add <cantidad>")));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    friendshipManager.addPersonalPoints(uuid, amount);
                    String msg = plugin.getMessage("success.personal_points_added", "{prefix}&aSe han añadido &f{points} &apuntos a &f{player}&a.")
                            .replace("{points}", String.valueOf(amount))
                            .replace("{player}", displayName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Cantidad inválida.");
                }
            }
            
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_remove", "&cUso: /giftbond points <jugador> remove <cantidad>")));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    boolean success = friendshipManager.spendPersonalPoints(uuid, amount);
                    if (success) {
                        String msg = plugin.getMessage("success.personal_points_removed", "{prefix}&aSe han quitado &f{points} &apuntos a &f{player}&a.")
                                .replace("{points}", String.valueOf(amount))
                                .replace("{player}", displayName);
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    } else {
                        String msg = plugin.getMessage("info.personal_points_insufficient_target", "{prefix}&c{player} no tiene suficientes puntos.")
                                .replace("{player}", displayName);
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Cantidad inválida.");
                }
            }
            
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_set", "&cUso: /giftbond points <jugador> set <cantidad>")));
                    return true;
                }
                try {
                    int amount = Integer.parseInt(args[2]);
                    friendshipManager.setPersonalPoints(uuid, amount);
                    String msg = plugin.getMessage("success.personal_points_set", "{prefix}&aPuntos de &f{player} &aestablecidos en &f{points}&a.")
                            .replace("{points}", String.valueOf(amount))
                            .replace("{player}", displayName);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Cantidad inválida.");
                }
            }
            
            default -> sendUsage(sender, isViewAction);
        }

        return true;
    }

    private void sendUsage(CommandSender sender, boolean isViewOnly) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.help_header", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        if (isViewOnly) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_view", "&cUso: /giftbond points <jugador> view")));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_view", "&cUso: /giftbond points <jugador> view")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_add", "&cUso: /giftbond points <jugador> add <cantidad>")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_remove", "&cUso: /giftbond points <jugador> remove <cantidad>")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_points_set", "&cUso: /giftbond points <jugador> set <cantidad>")));
        }
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.help_footer", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }
}