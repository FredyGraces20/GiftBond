package com.fredygraces.giftbond.storage;

import java.util.List;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.DatabaseManager;

/**
 * Gestor de almacenamiento que coordina entre diferentes tipos de bases de datos
 * Actualmente solo soporta SQLite local
 */
public class StorageManager {
    private final GiftBond plugin;
    private final DatabaseManager databaseManager;
    
    // Solo soportamos SQLite local ahora
    private StorageProvider localProvider;
    private final boolean localEnabled = true;
    
    public StorageManager(GiftBond plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }
    
    public boolean initialize() {
        // Solo inicializamos SQLite local
        localProvider = new SQLiteProvider(plugin, databaseManager);
        if (!localProvider.initialize()) {
            plugin.getLogger().severe("Error al inicializar SQLite storage!");
            return false;
        }
        
        plugin.getLogger().info("=== Storage Configuration ===");
        plugin.getLogger().info("Local (SQLite): ENABLED");
        plugin.getLogger().info("Remote: DISABLED (H2 eliminado)");
        plugin.getLogger().info("✓ Local storage (SQLite) inicializado");
        plugin.getLogger().info("============================");
        
        return true;
    }
    
    public void close() {
        if (localProvider != null) {
            localProvider.close();
        }
    }
    
    public boolean isConnected() {
        return localProvider != null && localProvider.isConnected();
    }
    
    public String getType() {
        return "sqlite";
    }
    
    // Métodos delegados a SQLite
    public void saveFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        if (localProvider != null) {
            localProvider.saveFriendshipPoints(senderUUID, receiverUUID, points);
        }
    }
    
    public int getFriendshipPoints(String senderUUID, String receiverUUID) {
        return localProvider != null ? localProvider.getFriendshipPoints(senderUUID, receiverUUID) : 0;
    }
    
    public java.util.Map<String, Integer> getPlayerFriendsWithPoints(String playerUUID) {
        return localProvider != null ? localProvider.getPlayerFriendsWithPoints(playerUUID) : new java.util.HashMap<>();
    }
    
    public int getTotalFriendshipPoints(String playerUUID) {
        return localProvider != null ? localProvider.getTotalFriendshipPoints(playerUUID) : 0;
    }
    
    public List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit) {
        return localProvider != null ? localProvider.getTopFriendshipPairs(limit) : new java.util.ArrayList<>();
    }
    
    public void addPersonalPoints(String playerUUID, int points) {
        if (localProvider != null) {
            localProvider.addPersonalPoints(playerUUID, points);
        }
    }
    
    public int getPersonalPoints(String playerUUID) {
        return localProvider != null ? localProvider.getPersonalPoints(playerUUID) : 0;
    }
    
    // Métodos específicos para verificar estado
    public boolean isLocalEnabled() {
        return localEnabled;
    }
    
    public StorageProvider getLocalProvider() {
        return localProvider;
    }
}