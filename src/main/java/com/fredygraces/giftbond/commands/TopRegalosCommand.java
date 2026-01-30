package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.FriendshipManager;

public class TopRegalosCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;

    public TopRegalosCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null) {
            return true; // Safe fallback for null sender
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessage("errors.no_permission_player_only", "{prefix}&cSolo los jugadores pueden usar este comando.")));
            return true;
        }

        Player player = (Player) sender;
        
        // Validate required components
        if (friendshipManager == null || plugin == null) {
            if (sender != null) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getMessage("errors.system_error", "{prefix}&cError del sistema: Componentes requeridos no disponibles.")));
            }
            return true;
        }
        
        // Get couples with most friendship points
        java.util.List<com.fredygraces.giftbond.managers.DatabaseManager.FriendshipPair> topPairs = 
            friendshipManager.getTopFriendshipPairs(10);
        
        // Send message to player
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.top_header", "&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.top_title", "&6&l🏆 TOP 10 PAREJAS")));
        player.sendMessage("");
        
        if (topPairs == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.top_error", "&cError al recuperar los datos de amistad.")));
            return true;
        }
        
        if (topPairs.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.top_no_data", "&7No hay puntos de amistad registrados aún.")));
        } else {
            for (int i = 0; i < topPairs.size(); i++) {
                com.fredygraces.giftbond.managers.DatabaseManager.FriendshipPair pair = topPairs.get(i);
                String player1Name = getPlayerName(pair.getPlayer1UUID());
                String player2Name = getPlayerName(pair.getPlayer2UUID());
                
                String entry = plugin.getMessage("info.top_entry", "&6{rank}. &f{player1} &4❤ &f{player2} &7- &a{points} puntos")
                        .replace("{rank}", String.valueOf(i + 1))
                        .replace("{player1}", player1Name)
                        .replace("{player2}", player2Name)
                        .replace("{points}", String.valueOf(pair.getPoints()));
                
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', entry));
            }
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.top_footer", "&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        
        return true;
    }
    
    private String getPlayerName(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return "Unknown"; // Safe fallback for invalid UUID
        }
        
        try {
            // Intentar obtener el nombre del jugador desde el servidor
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(
                java.util.UUID.fromString(uuid));
            return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
        } catch (IllegalArgumentException e) {
            // Handle invalid UUID format
            return "Invalid UUID";
        }
    }
    
    // Método que utiliza el plugin para acceder a funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
}