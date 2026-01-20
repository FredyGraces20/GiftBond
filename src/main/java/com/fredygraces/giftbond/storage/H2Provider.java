package com.fredygraces.giftbond.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.DatabaseManager;

/**
 * Proveedor de almacenamiento H2 Database
 * Base de datos embebida de alto rendimiento (más rápida que SQLite)
 * Soporta modo file (persistente) y memoria (temporal)
 */
public class H2Provider implements StorageProvider {
    
    private final GiftBond plugin;
    private Connection connection;
    private boolean connected = false;
    
    public H2Provider(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean initialize() {
        try {
            Class.forName("org.h2.Driver");
            
            String fileName = plugin.getConfigManager().getMainConfig().getString("storage.remote.h2.file", "friendships");
            String mode = plugin.getConfigManager().getMainConfig().getString("storage.remote.h2.mode", "file");
            
            String url;
            if (mode.equalsIgnoreCase("mem")) {
                // Modo memoria (se pierde al reiniciar)
                url = "jdbc:h2:mem:" + fileName + ";DB_CLOSE_DELAY=-1";
                plugin.getLogger().info("H2 Database inicializado en modo MEMORIA (los datos se perderán al reiniciar)");
            } else {
                // Modo archivo (persistente)
                java.io.File dataFolder = plugin.getDataFolder();
                if (!dataFolder.exists()) {
                    dataFolder.mkdirs();
                }
                url = "jdbc:h2:file:" + new java.io.File(dataFolder, fileName).getAbsolutePath() + 
                      ";AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=FALSE";
                plugin.getLogger().info("H2 Database inicializado en modo ARCHIVO: " + fileName);
            }
            
            connection = DriverManager.getConnection(url);
            
            // Configurar H2 para mejor rendimiento
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET CACHE_SIZE 8192"); // 8MB cache
                stmt.execute("SET LOG 0"); // Deshabilitar logging para mayor velocidad
                stmt.execute("SET UNDO_LOG 0"); // Deshabilitar undo log
            }
            
            createTables();
            
            connected = true;
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error al inicializar H2 Database", e);
            connected = false;
            return false;
        }
    }
    
    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            
            // Tabla de puntos de amistad
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS friendships (
                    sender_uuid VARCHAR(36) NOT NULL,
                    receiver_uuid VARCHAR(36) NOT NULL,
                    points INT DEFAULT 0,
                    last_interaction BIGINT DEFAULT 0,
                    PRIMARY KEY (sender_uuid, receiver_uuid)
                )
                """);
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_receiver_points ON friendships(receiver_uuid, points)");
            
            // Tabla de puntos personales
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_points (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    points INT DEFAULT 0
                )
                """);
            
            // Tabla de boosts personales
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_boosts (
                    player_uuid VARCHAR(36) PRIMARY KEY,
                    multiplier DOUBLE DEFAULT 1.0,
                    expiry BIGINT DEFAULT 0
                )
                """);
            
            // Tabla de historial de regalos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS gift_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    sender_uuid VARCHAR(36) NOT NULL,
                    receiver_uuid VARCHAR(36) NOT NULL,
                    gift_name VARCHAR(100) NOT NULL,
                    points_earned INT NOT NULL,
                    timestamp BIGINT NOT NULL
                )
                """);
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_history ON gift_history(sender_uuid, receiver_uuid)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON gift_history(timestamp)");
            
            // Tabla de límite diario
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS daily_gifts (
                    player_uuid VARCHAR(36) NOT NULL,
                    date VARCHAR(10) NOT NULL,
                    gift_count INT DEFAULT 0,
                    PRIMARY KEY (player_uuid, date)
                )
                """);
        }
    }
    
    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connected = false;
                plugin.getLogger().info("H2 Database cerrado");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error al cerrar H2 Database", e);
        }
    }
    
    @Override
    public boolean isConnected() {
        try {
            return connected && connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public String getType() {
        return "h2";
    }
    
    // ===== FRIENDSHIP POINTS =====
    
    @Override
    public void saveFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        String sql = """
            MERGE INTO friendships (sender_uuid, receiver_uuid, points, last_interaction) 
            KEY(sender_uuid, receiver_uuid)
            VALUES (?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderUUID);
            pstmt.setString(2, receiverUUID);
            pstmt.setInt(3, getFriendshipPoints(senderUUID, receiverUUID) + points);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving friendship points (H2)", e);
        }
    }
    
    @Override
    public int getFriendshipPoints(String senderUUID, String receiverUUID) {
        String sql = "SELECT points FROM friendships WHERE sender_uuid = ? AND receiver_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderUUID);
            pstmt.setString(2, receiverUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("points");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting friendship points (H2)", e);
        }
        return 0;
    }
    
    @Override
    public Map<String, Integer> getPlayerFriendsWithPoints(String playerUUID) {
        Map<String, Integer> friends = new HashMap<>();
        String sql = """
            SELECT receiver_uuid, points FROM friendships 
            WHERE sender_uuid = ? AND points > 0 
            ORDER BY points DESC
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    friends.put(rs.getString("receiver_uuid"), rs.getInt("points"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting player friends (H2)", e);
        }
        return friends;
    }
    
    @Override
    public int getTotalFriendshipPoints(String playerUUID) {
        String sql = "SELECT SUM(points) as total FROM friendships WHERE sender_uuid = ? AND points > 0";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting total points (H2)", e);
        }
        return 0;
    }
    
    @Override
    public List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit) {
        List<DatabaseManager.FriendshipPair> pairs = new ArrayList<>();
        String sql = """
            SELECT 
                CASE WHEN sender_uuid < receiver_uuid THEN sender_uuid ELSE receiver_uuid END as player1,
                CASE WHEN sender_uuid < receiver_uuid THEN receiver_uuid ELSE sender_uuid END as player2,
                SUM(points) as total_points
            FROM friendships 
            WHERE points > 0 
            GROUP BY player1, player2
            ORDER BY total_points DESC 
            LIMIT ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pairs.add(new DatabaseManager.FriendshipPair(
                        rs.getString("player1"),
                        rs.getString("player2"),
                        rs.getInt("total_points")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting top pairs (H2)", e);
        }
        return pairs;
    }
    
    // ===== PERSONAL POINTS =====
    
    @Override
    public void addPersonalPoints(String playerUUID, int points) {
        String sql = "MERGE INTO player_points (player_uuid, points) KEY(player_uuid) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, getPersonalPoints(playerUUID) + points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error adding personal points (H2)", e);
        }
    }
    
    @Override
    public int getPersonalPoints(String playerUUID) {
        String sql = "SELECT points FROM player_points WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("points");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting personal points (H2)", e);
        }
        return 0;
    }
    
    @Override
    public boolean spendPersonalPoints(String playerUUID, int amount) {
        int currentPoints = getPersonalPoints(playerUUID);
        if (currentPoints < amount) return false;
        
        String sql = "UPDATE player_points SET points = points - ? WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, playerUUID);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error spending personal points (H2)", e);
            return false;
        }
    }
    
    @Override
    public void setPersonalPoints(String playerUUID, int points) {
        String sql = "MERGE INTO player_points (player_uuid, points) KEY(player_uuid) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error setting personal points (H2)", e);
        }
    }
    
    // ===== BOOSTS =====
    
    @Override
    public void setPersonalBoost(String playerUUID, double multiplier, long expiry) {
        String sql = "MERGE INTO player_boosts (player_uuid, multiplier, expiry) KEY(player_uuid) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setDouble(2, multiplier);
            pstmt.setLong(3, expiry);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error setting personal boost (H2)", e);
        }
    }
    
    @Override
    public double getPersonalBoost(String playerUUID) {
        String sql = "SELECT multiplier, expiry FROM player_boosts WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    long expiry = rs.getLong("expiry");
                    if (expiry > System.currentTimeMillis()) {
                        return rs.getDouble("multiplier");
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting personal boost (H2)", e);
        }
        return 1.0;
    }
    
    // ===== GIFT HISTORY =====
    
    @Override
    public void saveGiftHistory(String senderUUID, String receiverUUID, String giftName, int points) {
        String sql = "INSERT INTO gift_history (sender_uuid, receiver_uuid, gift_name, points_earned, timestamp) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderUUID);
            pstmt.setString(2, receiverUUID);
            pstmt.setString(3, giftName);
            pstmt.setInt(4, points);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving gift history (H2)", e);
        }
    }
    
    @Override
    public List<DatabaseManager.GiftHistoryEntry> getGiftHistory(String playerUUID, int limit, int offset) {
        List<DatabaseManager.GiftHistoryEntry> history = new ArrayList<>();
        String sql = """
            SELECT sender_uuid, receiver_uuid, gift_name, points_earned, timestamp 
            FROM gift_history 
            WHERE sender_uuid = ? OR receiver_uuid = ? 
            ORDER BY timestamp DESC 
            LIMIT ? OFFSET ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, playerUUID);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    history.add(new DatabaseManager.GiftHistoryEntry(
                        rs.getString("sender_uuid"),
                        rs.getString("receiver_uuid"),
                        rs.getString("gift_name"),
                        rs.getInt("points_earned"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting gift history (H2)", e);
        }
        return history;
    }
    
    @Override
    public int getGiftHistoryCount(String playerUUID) {
        String sql = "SELECT COUNT(*) as count FROM gift_history WHERE sender_uuid = ? OR receiver_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, playerUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting history count (H2)", e);
        }
        return 0;
    }
    
    // ===== DAILY LIMIT =====
    
    @Override
    public int getDailyGiftCount(String playerUUID) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String sql = "SELECT gift_count FROM daily_gifts WHERE player_uuid = ? AND date = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, today);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("gift_count");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting daily gift count (H2)", e);
        }
        return 0;
    }
    
    @Override
    public void incrementDailyGiftCount(String playerUUID) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String sql = "MERGE INTO daily_gifts (player_uuid, date, gift_count) KEY(player_uuid, date) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, today);
            pstmt.setInt(3, getDailyGiftCount(playerUUID) + 1);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error incrementing daily gift count (H2)", e);
        }
    }
    
    // ===== BACKUP & MAINTENANCE =====
    
    @Override
    public void createManualBackup() {
        try {
            String backupPath = new java.io.File(plugin.getDataFolder(), "backups").getAbsolutePath();
            java.io.File backupFolder = new java.io.File(backupPath);
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupFile = backupPath + "/h2_backup_" + timestamp + ".zip";
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("BACKUP TO '" + backupFile + "'");
            }
            
            plugin.getLogger().info("H2 backup creado: " + backupFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando backup de H2", e);
        }
    }
}
