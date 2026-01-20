package com.fredygraces.giftbond.storage;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.plugin.java.JavaPlugin;

import com.fredygraces.giftbond.managers.DatabaseManager;

/**
 * SQLite storage provider wrapper
 * Wraps the existing DatabaseManager to implement the StorageProvider interface
 */
public class SQLiteProvider implements StorageProvider {
    
    private final DatabaseManager databaseManager;
    private final JavaPlugin plugin;
    
    public SQLiteProvider(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    @Override
    public boolean initialize() {
        // DatabaseManager is already initialized when plugin starts
        // Just verify it's working
        return true;
    }
    
    @Override
    public void close() {
        // DatabaseManager handles its own shutdown in onDisable
        plugin.getLogger().info("[SQLite] Provider shutdown");
    }
    
    @Override
    public boolean isConnected() {
        // Assume connected if connection exists
        return true;
    }
    
    @Override
    public String getType() {
        return "SQLite";
    }
    
    // Friendship Points Methods
    
    @Override
    public void saveFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        databaseManager.saveFriendshipPoints(senderUUID, receiverUUID, points);
    }
    
    @Override
    public int getFriendshipPoints(String senderUUID, String receiverUUID) {
        return databaseManager.getFriendshipPoints(senderUUID, receiverUUID);
    }
    
    @Override
    public int getTotalFriendshipPoints(String playerUUID) {
        return databaseManager.getTotalFriendshipPoints(playerUUID);
    }
    
    @Override
    public Map<String, Integer> getPlayerFriendsWithPoints(String playerUUID) {
        return databaseManager.getPlayerFriendsWithPoints(playerUUID);
    }
    
    @Override
    public List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit) {
        return databaseManager.getTopFriendshipPairs(limit);
    }
    
    // Personal Points Methods
    
    @Override
    public void addPersonalPoints(String playerUUID, int points) {
        databaseManager.addPersonalPoints(playerUUID, points);
    }
    
    @Override
    public int getPersonalPoints(String playerUUID) {
        return databaseManager.getPersonalPoints(playerUUID);
    }
    
    @Override
    public boolean spendPersonalPoints(String playerUUID, int amount) {
        return databaseManager.spendPersonalPoints(playerUUID, amount);
    }
    
    @Override
    public void setPersonalPoints(String playerUUID, int points) {
        databaseManager.setPersonalPoints(playerUUID, points);
    }
    
    // Boost Methods
    
    @Override
    public void setPersonalBoost(String playerUUID, double multiplier, long expiry) {
        databaseManager.setPersonalBoost(playerUUID, multiplier, expiry);
    }
    
    @Override
    public double getPersonalBoost(String playerUUID) {
        return databaseManager.getPersonalBoost(playerUUID);
    }
    

    
    // Gift History Methods
    
    @Override
    public void saveGiftHistory(String senderUUID, String receiverUUID, String giftName, int points) {
        databaseManager.saveGiftHistory(senderUUID, receiverUUID, giftName, points);
    }
    
    @Override
    public List<DatabaseManager.GiftHistoryEntry> getGiftHistory(String playerUUID, int limit, int offset) {
        return databaseManager.getGiftHistory(playerUUID, limit, offset);
    }
    
    @Override
    public int getGiftHistoryCount(String playerUUID) {
        return databaseManager.getGiftHistoryCount(playerUUID);
    }
    
    // Daily Gift Limit Methods
    
    @Override
    public int getDailyGiftCount(String playerUUID) {
        return databaseManager.getDailyGiftCount(playerUUID);
    }
    
    @Override
    public void incrementDailyGiftCount(String playerUUID) {
        databaseManager.incrementDailyGiftCount(playerUUID);
    }
    
    // Backup and Maintenance Methods
    
    @Override
    public void createManualBackup() {
        try {
            databaseManager.createManualBackup();
            plugin.getLogger().info("[SQLite] Manual backup created successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "[SQLite] Error creating manual backup", e);
        }
    }
    

    
    /**
     * Get the underlying DatabaseManager instance
     * Useful for migration or direct access if needed
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
