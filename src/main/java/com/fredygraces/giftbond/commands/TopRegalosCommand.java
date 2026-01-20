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
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            return true;
        }

        Player player = (Player) sender;
        
        // Obtener las parejas con más puntos de amistad
        java.util.List<com.fredygraces.giftbond.managers.DatabaseManager.FriendshipPair> topPairs = 
            friendshipManager.getTopFriendshipPairs(10);
        
        // Enviar mensaje al jugador
        String prefix = plugin.getPrefix();
        player.sendMessage(prefix + "§eTop 10 parejas con más puntos de amistad:");
        
        if (topPairs.isEmpty()) {
            player.sendMessage(prefix + "§7No hay puntos de amistad registrados aún.");
        } else {
            for (int i = 0; i < topPairs.size(); i++) {
                com.fredygraces.giftbond.managers.DatabaseManager.FriendshipPair pair = topPairs.get(i);
                String player1Name = getPlayerName(pair.getPlayer1UUID());
                String player2Name = getPlayerName(pair.getPlayer2UUID());
                
                player.sendMessage("§6" + (i + 1) + ". §f" + player1Name + " §4❤ §f" + player2Name + " §7- §a" + pair.getPoints() + " puntos");
            }
        }
        
        return true;
    }
    
    private String getPlayerName(String uuid) {
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