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
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission", "{prefix}&cNo tienes permiso para usar este comando.")));
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
                @SuppressWarnings("deprecation")
                OfflinePlayer foundPlayer = Bukkit.getOfflinePlayer(args[0]);
                
                if (!foundPlayer.hasPlayedBefore()) {
                    String msg = plugin.getMessage("errors.player_not_found", "{prefix}&cJugador no encontrado: {player}")
                            .replace("{player}", args[0]);
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                    return true;
                }
                
                offlinePlayer = foundPlayer;
            }
            
            double multiplier = Double.parseDouble(args[1]);
            int minutes = (args.length >= 3) ? Integer.parseInt(args[2]) : 60;
            
            friendshipManager.setPersonalBoost(offlinePlayer.getUniqueId().toString(), multiplier, minutes);
            
            String msg = plugin.getMessage("success.boost_applied", "{prefix}&aBoost aplicado a {player}: x{multiplier} por {duration} minutos")
                    .replace("{player}", offlinePlayer.getName() != null ? offlinePlayer.getName() : args[0])
                    .replace("{multiplier}", String.valueOf(multiplier))
                    .replace("{duration}", String.valueOf(minutes));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            
            if (offlinePlayer.isOnline()) {
                Player onlinePlayer = offlinePlayer.getPlayer();
                if (onlinePlayer != null) {
                    onlinePlayer.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_boost", "&cUso: /giftbond boost <jugador> <multiplicador> <duración_minutos>")));
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.help_header", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_boost", "&cUso: /giftbond boost <jugador> <multiplicador> <duración_minutos>")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_boost_example", "&7Ejemplo: /giftbond boost {player} 2.0 30")
                .replace("{player}", sender.getName())));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.help_footer", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }
}
