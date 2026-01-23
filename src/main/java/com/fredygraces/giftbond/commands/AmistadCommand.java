package com.fredygraces.giftbond.commands;

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
                sender.sendMessage("§cOnly players can use this command.");
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
        String prefix = plugin.getPrefix();
        player.sendMessage(prefix + "§ePuntos personales (para canjear): §a" + personalPoints);
        player.sendMessage(prefix + "§eTus puntos de amistad totales: §f" + totalPoints);
        player.sendMessage(prefix + "§eTus amistades:");
        
        if (friends.isEmpty()) {
            player.sendMessage(prefix + "§7No tienes puntos de amistad con nadie aún.");
        } else {
            // Mostrar solo top 5 como solicitado
            int maxDisplay = Math.min(5, friends.size());
            for (int i = 0; i < maxDisplay; i++) {
                java.util.Map.Entry<String, Integer> friend = friends.get(i);
                String friendName = getFriendName(friend.getKey());
                player.sendMessage("§f" + friendName + ": §a" + friend.getValue());
            }
        }
        
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