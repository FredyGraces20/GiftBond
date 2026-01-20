package com.fredygraces.giftbond.managers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;

public class FriendshipManager {
    private final GiftBond plugin;
    private final DatabaseManager databaseManager;
    private String lastTop1Key = "";

    public FriendshipManager(GiftBond plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    public int addFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        double multiplier = getActiveMultiplier(senderUUID);
        int finalPoints = (int) (points * multiplier);
        
        databaseManager.saveFriendshipPoints(senderUUID, receiverUUID, finalPoints);
        
        // Otorgar puntos personales al emisor
        databaseManager.addPersonalPoints(senderUUID, finalPoints);
        
        // Otorgar puntos personales al receptor si está habilitado en la config
        if (plugin.getConfigManager().getMainConfig().getBoolean("settings.dual_personal_points", false)) {
            boolean boostReceiver = plugin.getConfigManager().getMainConfig().getBoolean("settings.boost_dual_personal_points", true);
            int receiverPoints = boostReceiver ? finalPoints : points;
            databaseManager.addPersonalPoints(receiverUUID, receiverPoints);
        }
        
        // Verificar si hay nuevo Top 1 y hacer broadcast
        checkAndBroadcastTop1();
        
        return finalPoints;
    }

    private void checkAndBroadcastTop1() {
        if (!plugin.getConfigManager().getMainConfig().getBoolean("settings.broadcast_top1", true)) {
            return;
        }
        
        List<DatabaseManager.FriendshipPair> topPairs = databaseManager.getTopFriendshipPairs(1);
        if (topPairs.isEmpty()) return;
        
        DatabaseManager.FriendshipPair currentTop = topPairs.get(0);
        String currentKey = currentTop.getPlayer1UUID() + "-" + currentTop.getPlayer2UUID() + "-" + currentTop.getPoints();
        
        // Si es diferente al anterior Top 1, hacer broadcast
        if (!currentKey.equals(lastTop1Key)) {
            lastTop1Key = currentKey;
            
            OfflinePlayer player1 = Bukkit.getOfflinePlayer(java.util.UUID.fromString(currentTop.getPlayer1UUID()));
            OfflinePlayer player2 = Bukkit.getOfflinePlayer(java.util.UUID.fromString(currentTop.getPlayer2UUID()));
            
            String name1 = player1.getName() != null ? player1.getName() : "Desconocido";
            String name2 = player2.getName() != null ? player2.getName() : "Desconocido";
            
            String message = plugin.getMessage("messages.top1_broadcast", 
                "&d✨ ¡{player1} y {player2} son ahora la pareja Nº1 con {points} puntos! ✨");
            message = message.replace("{player1}", name1)
                           .replace("{player2}", name2)
                           .replace("{points}", String.valueOf(currentTop.getPoints()));
            
            Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
            
            // Ejecutar comandos personalizados del config.yml
            executeTop1Commands(name1, name2, currentTop.getPoints());
        }
    }
    
    private void executeTop1Commands(String player1, String player2, int points) {
        if (!plugin.getConfigManager().getMainConfig().contains("settings.top1_commands")) {
            return;
        }
        
        java.util.List<String> commands = plugin.getConfigManager().getMainConfig().getStringList("settings.top1_commands");
        if (commands.isEmpty()) {
            return;
        }
        
        for (String command : commands) {
            // Reemplazar placeholders
            String processedCommand = command
                .replace("{player1}", player1)
                .replace("{player2}", player2)
                .replace("{points}", String.valueOf(points));
            
            // Ejecutar comando desde consola
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error ejecutando comando Top1: " + processedCommand);
                    plugin.getLogger().warning("Error: " + e.getMessage());
                }
            });
        }
    }

    public double getActiveMultiplier(String playerUUID) {
        // Obtener el jugador
        Player player = Bukkit.getPlayer(java.util.UUID.fromString(playerUUID));
        if (player == null) {
            return 1.0; // Jugador offline, sin boost
        }
        
        // Buscar el boost más alto de todos los configurados por permisos
        double highestMultiplier = 1.0;
        
        if (plugin.getConfigManager().getMainConfig().contains("boosts")) {
            for (String boostKey : plugin.getConfigManager().getMainConfig().getConfigurationSection("boosts").getKeys(false)) {
                String permission = plugin.getConfigManager().getMainConfig().getString("boosts." + boostKey + ".permission");
                double multiplier = plugin.getConfigManager().getMainConfig().getDouble("boosts." + boostKey + ".multiplier", 1.0);
                
                if (player.hasPermission(permission) && multiplier > highestMultiplier) {
                    highestMultiplier = multiplier;
                }
            }
        }
        
        // Verificar boost personal temporal de la base de datos
        double personalBoost = databaseManager.getPersonalBoost(playerUUID);
        if (personalBoost > 1.0) {
            highestMultiplier *= personalBoost; // MULTIPLICAR ambos boosts
        }
        
        return highestMultiplier;
    }

    public void setPersonalBoost(String playerUUID, double multiplier, int minutes) {
        long expiry = System.currentTimeMillis() + (minutes * 60 * 1000L);
        databaseManager.setPersonalBoost(playerUUID, multiplier, expiry);
    }

    public int getPersonalPoints(String playerUUID) {
        return databaseManager.getPersonalPoints(playerUUID);
    }

    public boolean spendPersonalPoints(String playerUUID, int amount) {
        return databaseManager.spendPersonalPoints(playerUUID, amount);
    }

    public void addPersonalPoints(String playerUUID, int points) {
        databaseManager.addPersonalPoints(playerUUID, points);
    }

    public void setPersonalPoints(String playerUUID, int points) {
        databaseManager.setPersonalPoints(playerUUID, points);
    }

    public int getFriendshipPoints(String senderUUID, String receiverUUID) {
        return databaseManager.getFriendshipPoints(senderUUID, receiverUUID);
    }

    public int getTotalPoints(String playerUUID) {
        return databaseManager.getTotalFriendshipPoints(playerUUID);
    }

    public List<Map.Entry<String, Integer>> getSortedFriends(String playerUUID) {
        Map<String, Integer> friends = databaseManager.getPlayerFriendsWithPoints(playerUUID);
        
        List<Map.Entry<String, Integer>> sortedFriends = new ArrayList<>(friends.entrySet());
        sortedFriends.sort(Map.Entry.<String, Integer>comparingByValue().reversed());
        
        return sortedFriends;
    }

    public List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit) {
        return databaseManager.getTopFriendshipPairs(limit);
    }
    
    // Método que utiliza el plugin para acceder a funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
}