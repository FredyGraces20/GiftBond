package com.fredygraces.giftbond.storage;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.DatabaseManager;

/**
 * Gestor central de almacenamiento dual
 * Maneja local storage (SQLite) y remote storage (MySQL/H2/MongoDB/PostgreSQL)
 * Soporta sincronización bidireccional automática
 */
public class StorageManager implements StorageProvider {
    
    private final GiftBond plugin;
    private StorageProvider localProvider;
    private StorageProvider remoteProvider;
    
    private boolean localEnabled;
    private boolean remoteEnabled;
    private boolean syncEnabled;
    private String syncDirection;
    private int syncInterval;
    
    private BukkitRunnable syncTask;
    
    public StorageManager(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean initialize() {
        // Leer configuración de database.yml
        FileConfiguration dbConfig = plugin.getConfigManager().getDatabaseConfig();
        
        // Configuración local (SQLite)
        localEnabled = dbConfig.getBoolean("local.enabled", true);
        
        // Configuración remota (H2)
        remoteEnabled = dbConfig.getBoolean("remote.enabled", false);
        
        // Configuración de sincronización
        syncEnabled = dbConfig.getBoolean("sync.enabled", true);
        syncDirection = dbConfig.getString("sync.direction", "bidirectional");
        syncInterval = dbConfig.getInt("sync.interval", 5);
        
        plugin.getLogger().info("=== Storage Configuration ===");
        plugin.getLogger().info("Local (SQLite): " + (localEnabled ? "ENABLED" : "DISABLED"));
        plugin.getLogger().info("Remote: " + (remoteEnabled ? "ENABLED" : "DISABLED"));
        
        // Validar que al menos uno esté habilitado
        if (!localEnabled && !remoteEnabled) {
            plugin.getLogger().severe("ERROR: Al menos un almacenamiento debe estar habilitado!");
            plugin.getLogger().severe("Habilitando local storage por defecto...");
            localEnabled = true;
        }
        
        // Inicializar local storage
        if (localEnabled) {
            // Wrap existing DatabaseManager to implement StorageProvider interface
            localProvider = new SQLiteProvider(plugin, plugin.getDatabaseManager());
            if (!localProvider.initialize()) {
                plugin.getLogger().severe("Error al inicializar local storage!");
                if (!remoteEnabled) {
                    return false;
                }
            } else {
                plugin.getLogger().info("✓ Local storage (SQLite) inicializado");
            }
        }
        
        // Inicializar remote storage
        if (remoteEnabled) {
            String type = plugin.getConfigManager().getMainConfig().getString("storage.remote.type", "mysql").toLowerCase();
            plugin.getLogger().info("Remote Type: " + type.toUpperCase());
            
            remoteProvider = createRemoteProvider(type);
            
            if (remoteProvider == null) {
                plugin.getLogger().severe("Tipo de almacenamiento remoto no válido: " + type);
                plugin.getLogger().severe("Tipo disponible: h2");
                remoteEnabled = false;
            } else {
                if (!remoteProvider.initialize()) {
                    plugin.getLogger().severe("Error al inicializar remote storage!");
                    remoteEnabled = false;
                } else {
                    plugin.getLogger().info("✓ Remote storage (" + type.toUpperCase() + ") inicializado");
                }
            }
        }
        
        // Configurar sincronización
        if (localEnabled && remoteEnabled && syncEnabled && syncInterval > 0) {
            startSyncTask();
            plugin.getLogger().info("✓ Sincronización automática activada (" + syncDirection + ", cada " + syncInterval + " minutos)");
        }
        
        plugin.getLogger().info("============================");
        
        return localEnabled || remoteEnabled;
    }
    
    private StorageProvider createRemoteProvider(String type) {
        return switch (type) {
            case "h2" -> new H2Provider(plugin);
            default -> null;
        };
    }
    
    private void startSyncTask() {
        syncTask = new BukkitRunnable() {
            @Override
            public void run() {
                performSync();
            }
        };
        
        // Ejecutar cada X minutos
        long intervalTicks = syncInterval * 60 * 20L; // minutos a ticks
        syncTask.runTaskTimerAsynchronously(plugin, intervalTicks, intervalTicks);
    }
    
    private void performSync() {
        if (!localEnabled || !remoteEnabled || !syncEnabled) return;
        
        plugin.getLogger().info("============================");
        plugin.getLogger().info("Iniciando sincronización de datos...");
        plugin.getLogger().info("Dirección: " + syncDirection);
        
        long startTime = System.currentTimeMillis();
        int syncedRecords = 0;
        
        try {
            switch (syncDirection.toLowerCase()) {
                case "local_to_remote":
                    syncedRecords = syncLocalToRemote();
                    plugin.getLogger().info("✓ Sincronizado Local → Remote: " + syncedRecords + " registros");
                    break;
                    
                case "remote_to_local":
                    syncedRecords = syncRemoteToLocal();
                    plugin.getLogger().info("✓ Sincronizado Remote → Local: " + syncedRecords + " registros");
                    break;
                    
                case "bidirectional":
                    int toRemote = syncLocalToRemote();
                    int toLocal = syncRemoteToLocal();
                    syncedRecords = toRemote + toLocal;
                    plugin.getLogger().info("✓ Sincronizado Bidireccional: " + toRemote + " → Remote, " + toLocal + " → Local");
                    break;
            }
            
            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("Sincronización completada en " + duration + "ms");
            plugin.getLogger().info("============================");
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error durante sincronización", e);
        }
    }
    
    /**
     * Sincroniza datos de Local a Remote
     * Copia todos los datos de SQLite a MySQL/H2/etc
     */
    private int syncLocalToRemote() {
        if (localProvider == null || remoteProvider == null) return 0;
        
        int count = 0;
        
        try {
            // Obtener todos los pares de amistad del local
            List<DatabaseManager.FriendshipPair> localPairs = localProvider.getTopFriendshipPairs(10000);
            for (DatabaseManager.FriendshipPair pair : localPairs) {
                // Verificar si existe en remote
                int remotePoints1 = remoteProvider.getFriendshipPoints(pair.getPlayer1UUID(), pair.getPlayer2UUID());
                int remotePoints2 = remoteProvider.getFriendshipPoints(pair.getPlayer2UUID(), pair.getPlayer1UUID());
                int totalRemotePoints = remotePoints1 + remotePoints2;
                
                // Si local tiene más puntos, actualizar remote
                if (pair.getPoints() > totalRemotePoints) {
                    int diff = pair.getPoints() - totalRemotePoints;
                    remoteProvider.saveFriendshipPoints(pair.getPlayer1UUID(), pair.getPlayer2UUID(), diff);
                    count++;
                }
            }
            
            // Sincronizar boosts activos
            // (Los boosts expirados se limpian automáticamente al leerlos)
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sincronizando local → remote", e);
        }
        
        return count;
    }
    
    /**
     * Sincroniza datos de Remote a Local
     * Copia todos los datos de MySQL/H2/etc a SQLite
     */
    private int syncRemoteToLocal() {
        if (localProvider == null || remoteProvider == null) return 0;
        
        int count = 0;
        
        try {
            // Obtener todos los pares de amistad del remote
            List<DatabaseManager.FriendshipPair> remotePairs = remoteProvider.getTopFriendshipPairs(10000);
            for (DatabaseManager.FriendshipPair pair : remotePairs) {
                // Verificar si existe en local
                int localPoints1 = localProvider.getFriendshipPoints(pair.getPlayer1UUID(), pair.getPlayer2UUID());
                int localPoints2 = localProvider.getFriendshipPoints(pair.getPlayer2UUID(), pair.getPlayer1UUID());
                int totalLocalPoints = localPoints1 + localPoints2;
                
                // Si remote tiene más puntos, actualizar local
                if (pair.getPoints() > totalLocalPoints) {
                    int diff = pair.getPoints() - totalLocalPoints;
                    localProvider.saveFriendshipPoints(pair.getPlayer1UUID(), pair.getPlayer2UUID(), diff);
                    count++;
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error sincronizando remote → local", e);
        }
        
        return count;
    }
    
    @Override
    public void close() {
        if (syncTask != null) {
            syncTask.cancel();
        }
        
        if (localProvider != null) {
            localProvider.close();
        }
        
        if (remoteProvider != null) {
            remoteProvider.close();
        }
    }
    
    @Override
    public boolean isConnected() {
        if (localEnabled && remoteEnabled) {
            return localProvider.isConnected() || remoteProvider.isConnected();
        } else if (localEnabled) {
            return localProvider.isConnected();
        } else if (remoteEnabled) {
            return remoteProvider.isConnected();
        }
        return false;
    }
    
    @Override
    public String getType() {
        if (localEnabled && remoteEnabled) {
            return "dual(" + localProvider.getType() + "+" + remoteProvider.getType() + ")";
        } else if (localEnabled) {
            return localProvider.getType();
        } else if (remoteEnabled) {
            return remoteProvider.getType();
        }
        return "none";
    }
    
    // ===== MÉTODOS DELEGADOS =====
    // Delega las operaciones al provider activo (local o remote)
    // Si ambos están activos, escribe en ambos y lee del primario
    
    private StorageProvider getPrimaryProvider() {
        if (remoteEnabled && remoteProvider != null && remoteProvider.isConnected()) {
            return remoteProvider;
        } else if (localEnabled && localProvider != null && localProvider.isConnected()) {
            return localProvider;
        }
        return null;
    }
    
    @Override
    public void saveFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        if (localEnabled && localProvider != null) {
            localProvider.saveFriendshipPoints(senderUUID, receiverUUID, points);
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.saveFriendshipPoints(senderUUID, receiverUUID, points);
        }
    }
    
    @Override
    public int getFriendshipPoints(String senderUUID, String receiverUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getFriendshipPoints(senderUUID, receiverUUID) : 0;
    }
    
    @Override
    public Map<String, Integer> getPlayerFriendsWithPoints(String playerUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getPlayerFriendsWithPoints(playerUUID) : new java.util.HashMap<>();
    }
    
    @Override
    public int getTotalFriendshipPoints(String playerUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getTotalFriendshipPoints(playerUUID) : 0;
    }
    
    @Override
    public List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getTopFriendshipPairs(limit) : new java.util.ArrayList<>();
    }
    
    @Override
    public void addPersonalPoints(String playerUUID, int points) {
        if (localEnabled && localProvider != null) {
            localProvider.addPersonalPoints(playerUUID, points);
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.addPersonalPoints(playerUUID, points);
        }
    }
    
    @Override
    public int getPersonalPoints(String playerUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getPersonalPoints(playerUUID) : 0;
    }
    
    @Override
    public boolean spendPersonalPoints(String playerUUID, int amount) {
        boolean success = false;
        if (localEnabled && localProvider != null) {
            success = localProvider.spendPersonalPoints(playerUUID, amount);
        }
        if (remoteEnabled && remoteProvider != null) {
            success = remoteProvider.spendPersonalPoints(playerUUID, amount) || success;
        }
        return success;
    }
    
    @Override
    public void setPersonalPoints(String playerUUID, int points) {
        if (localEnabled && localProvider != null) {
            localProvider.setPersonalPoints(playerUUID, points);
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.setPersonalPoints(playerUUID, points);
        }
    }
    
    @Override
    public void setPersonalBoost(String playerUUID, double multiplier, long expiry) {
        if (localEnabled && localProvider != null) {
            localProvider.setPersonalBoost(playerUUID, multiplier, expiry);
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.setPersonalBoost(playerUUID, multiplier, expiry);
        }
    }
    
    @Override
    public double getPersonalBoost(String playerUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getPersonalBoost(playerUUID) : 1.0;
    }
    
    @Override
    public void saveGiftHistory(String senderUUID, String receiverUUID, String giftName, int points) {
        if (localEnabled && localProvider != null) {
            localProvider.saveGiftHistory(senderUUID, receiverUUID, giftName, points);
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.saveGiftHistory(senderUUID, receiverUUID, giftName, points);
        }
    }
    
    @Override
    public List<DatabaseManager.GiftHistoryEntry> getGiftHistory(String playerUUID, int limit, int offset) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getGiftHistory(playerUUID, limit, offset) : new java.util.ArrayList<>();
    }
    
    @Override
    public int getGiftHistoryCount(String playerUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getGiftHistoryCount(playerUUID) : 0;
    }
    
    @Override
    public int getDailyGiftCount(String playerUUID) {
        StorageProvider provider = getPrimaryProvider();
        return provider != null ? provider.getDailyGiftCount(playerUUID) : 0;
    }
    
    @Override
    public void incrementDailyGiftCount(String playerUUID) {
        if (localEnabled && localProvider != null) {
            localProvider.incrementDailyGiftCount(playerUUID);
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.incrementDailyGiftCount(playerUUID);
        }
    }
    
    @Override
    public void createManualBackup() {
        if (localEnabled && localProvider != null) {
            localProvider.createManualBackup();
        }
        if (remoteEnabled && remoteProvider != null) {
            remoteProvider.createManualBackup();
        }
    }
    
    // ===== GETTERS =====
    
    public StorageProvider getLocalProvider() {
        return localProvider;
    }
    
    public StorageProvider getRemoteProvider() {
        return remoteProvider;
    }
    
    public boolean isLocalEnabled() {
        return localEnabled;
    }
    
    public boolean isRemoteEnabled() {
        return remoteEnabled;
    }
}
