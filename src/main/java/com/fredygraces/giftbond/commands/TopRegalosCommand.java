package com.fredygraces.giftbond.commands;

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
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        
        // Validate required components
        if (friendshipManager == null || plugin == null) {
            if (sender != null) {
                sender.sendMessage("§cSystem error: Required components not available.");
            }
            return true;
        }
        
        // Get couples with most friendship points
        java.util.List<com.fredygraces.giftbond.managers.DatabaseManager.FriendshipPair> topPairs = 
            friendshipManager.getTopFriendshipPairs(10);
        
        // Send message to player
        String prefix = plugin.getPrefix();
        
        if (topPairs == null) {
            player.sendMessage(prefix + "§cError retrieving friendship data.");
            return true;
        }
        
        player.sendMessage(prefix + "§eTop 10 couples with most friendship points:");
        
        if (topPairs.isEmpty()) {
            player.sendMessage(prefix + "§7No friendship points registered yet.");
        } else {
            for (int i = 0; i < topPairs.size(); i++) {
                com.fredygraces.giftbond.managers.DatabaseManager.FriendshipPair pair = topPairs.get(i);
                String player1Name = getPlayerName(pair.getPlayer1UUID());
                String player2Name = getPlayerName(pair.getPlayer2UUID());
                
                player.sendMessage("§6" + (i + 1) + ". §f" + player1Name + " §4❤ §f" + player2Name + " §7- §a" + pair.getPoints() + " points");
            }
        }
        
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