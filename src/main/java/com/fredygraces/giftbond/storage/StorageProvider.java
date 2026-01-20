package com.fredygraces.giftbond.storage;

import java.util.List;
import java.util.Map;

import com.fredygraces.giftbond.managers.DatabaseManager;

/**
 * Interfaz para proveedores de almacenamiento de datos
 * Soporta múltiples backends: SQLite, MySQL, MongoDB, H2, PostgreSQL
 */
public interface StorageProvider {
    
    /**
     * Inicializa la conexión con el almacenamiento
     * @return true si la inicialización fue exitosa
     */
    boolean initialize();
    
    /**
     * Cierra la conexión con el almacenamiento
     */
    void close();
    
    /**
     * Verifica si la conexión está activa
     * @return true si está conectado
     */
    boolean isConnected();
    
    // ===== FRIENDSHIP POINTS =====
    
    void saveFriendshipPoints(String senderUUID, String receiverUUID, int points);
    
    int getFriendshipPoints(String senderUUID, String receiverUUID);
    
    Map<String, Integer> getPlayerFriendsWithPoints(String playerUUID);
    
    int getTotalFriendshipPoints(String playerUUID);
    
    List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit);
    
    // ===== PERSONAL POINTS =====
    
    void addPersonalPoints(String playerUUID, int points);
    
    int getPersonalPoints(String playerUUID);
    
    boolean spendPersonalPoints(String playerUUID, int amount);
    
    void setPersonalPoints(String playerUUID, int points);
    
    // ===== BOOSTS =====
    
    void setPersonalBoost(String playerUUID, double multiplier, long expiry);
    
    double getPersonalBoost(String playerUUID);
    
    // ===== GIFT HISTORY =====
    
    void saveGiftHistory(String senderUUID, String receiverUUID, String giftName, int points);
    
    List<DatabaseManager.GiftHistoryEntry> getGiftHistory(String playerUUID, int limit, int offset);
    
    int getGiftHistoryCount(String playerUUID);
    
    // ===== DAILY LIMIT =====
    
    int getDailyGiftCount(String playerUUID);
    
    void incrementDailyGiftCount(String playerUUID);
    
    // ===== BACKUP & MAINTENANCE =====
    
    void createManualBackup();
    
    /**
     * Obtiene el tipo de almacenamiento
     * @return Nombre del tipo (sqlite, mysql, mongodb, h2, postgresql)
     */
    String getType();
}
