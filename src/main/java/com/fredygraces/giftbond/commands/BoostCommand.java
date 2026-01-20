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

public class BoostCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;

    public BoostCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("giftbond.admin.boost")) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        // Solo boost personal
        try {
            // Intentar obtener el jugador online primero
            Player targetPlayer = Bukkit.getPlayerExact(args[0]);
            OfflinePlayer offlinePlayer;
            
            if (targetPlayer != null) {
                // Jugador está online
                offlinePlayer = targetPlayer;
            } else {
                // Jugador offline - buscar por nombre
                // Usar método no-deprecated: iterar jugadores conocidos
                OfflinePlayer foundPlayer = null;
                for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                    if (player.getName() != null && player.getName().equalsIgnoreCase(args[0])) {
                        foundPlayer = player;
                        break;
                    }
                }
                
                if (foundPlayer == null) {
                    sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "El jugador " + args[0] + " nunca ha jugado en este servidor.");
                    return true;
                }
                
                offlinePlayer = foundPlayer;
            }
            
            double multiplier = Double.parseDouble(args[1]);
            int minutes = (args.length >= 3) ? Integer.parseInt(args[2]) : 60;
            
            friendshipManager.setPersonalBoost(offlinePlayer.getUniqueId().toString(), multiplier, minutes);
            sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "Boost personal para " + offlinePlayer.getName() + " activado: x" + 
                multiplier + " por " + minutes + " minutos.");
            
            if (offlinePlayer.isOnline()) {
                Player onlinePlayer = offlinePlayer.getPlayer();
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + 
                        "¡Has recibido un boost personal! Multiplicador x" + multiplier + " por " + minutes + " minutos.");
                }
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Uso: /giftbond boost <jugador> <multiplicador> [minutos]");
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "=== Boost Personal ===");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond boost <jugador> <multiplicador> [minutos]" + ChatColor.GRAY + " - Dar boost temporal");
        sender.sendMessage(ChatColor.GRAY + "Ejemplo: /giftbond boost Notch 2.0 30");
    }
}
