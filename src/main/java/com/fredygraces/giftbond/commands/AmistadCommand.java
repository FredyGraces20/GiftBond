package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.FriendshipManager;

public class AmistadCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;

    public AmistadCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (sender != null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getMessage("errors.no_permission_player_only", "{prefix}&cSolo los jugadores pueden usar este comando.")));
            }
            return true;
        }

        Player player = (Player) sender;
        
        // Si hay argumentos, verificar subcomando "historial"
        if (args.length > 0 && args[0].equalsIgnoreCase("historial")) {
            plugin.getHistoryMenu().openHistoryMenu(player, 0);
            return true;
        }
        
        String playerUUID = player.getUniqueId().toString();
        
        // Obtener puntos de amistad del jugador
        int totalPoints = friendshipManager.getTotalPoints(playerUUID);
        int personalPoints = friendshipManager.getPersonalPoints(playerUUID);
        java.util.List<java.util.Map.Entry<String, Integer>> friends = 
            friendshipManager.getSortedFriends(playerUUID);
        
        // Enviar mensaje al jugador
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_header", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_title", "&d&l💕 PUNTOS DE AMISTAD")));
        player.sendMessage("");
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_personal_points", "&ePuntos personales (para canjear): &a{points}")
                .replace("{points}", String.valueOf(personalPoints))));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_your_points", "&eTus puntos de amistad totales: &f{points}")
                .replace("{points}", String.valueOf(totalPoints))));
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_friendships_title", "&eTus amistades:")));
        
        if (friends.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_no_friends", "&7No tienes puntos de amistad con nadie aún.")));
        } else {
            // Mostrar solo top 5 como solicitado
            int maxDisplay = Math.min(5, friends.size());
            for (int i = 0; i < maxDisplay; i++) {
                java.util.Map.Entry<String, Integer> friend = friends.get(i);
                String friendName = getFriendName(friend.getKey());
                String entry = plugin.getMessage("info.friendship_friend_entry", "&f{friend}: &a{points}")
                        .replace("{friend}", friendName)
                        .replace("{points}", String.valueOf(friend.getValue()));
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', entry));
            }
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.friendship_footer", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        
        return true;
    }
    
    private String getFriendName(String uuid) {
        // Intentar obtener el nombre del jugador desde el servidor
        org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(
            java.util.UUID.fromString(uuid));
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Desconocido";
    }
    
    // Método que utiliza el plugin para acceder a funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
}