package com.fredygraces.giftbond.managers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.fredygraces.giftbond.GiftBond;

public class DatabaseManager {
    private final GiftBond plugin;
    private Connection connection;
    private final String DATABASE_NAME = "friendships.db";
    private final String BACKUP_FOLDER = "backups";
    private java.util.Timer backupTimer;

    public DatabaseManager(GiftBond plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            
            // Asegurar que el directorio de datos existe
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            // Asegurar que el directorio de backups existe
            File backupFolder = new File(dataFolder, BACKUP_FOLDER);
            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }
            
            String url = "jdbc:sqlite:" + new File(dataFolder, DATABASE_NAME).getAbsolutePath();
            connection = DriverManager.getConnection(url);
            
            createTables();
            
            // Iniciar el sistema de backup automático
            startBackupSystem();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS friendships (
                sender_uuid VARCHAR(36) NOT NULL,
                receiver_uuid VARCHAR(36) NOT NULL,
                points INTEGER DEFAULT 0,
                last_interaction BIGINT DEFAULT 0,
                PRIMARY KEY (sender_uuid, receiver_uuid)
            )
            """;
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
        
        // Create indexes for better performance
        String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_receiver_points ON friendships(receiver_uuid, points)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL);
        }

        // Crear tabla de puntos personales
        String createPersonalTableSQL = """
            CREATE TABLE IF NOT EXISTS player_points (
                player_uuid VARCHAR(36) PRIMARY KEY,
                points INTEGER DEFAULT 0
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPersonalTableSQL);
        }

        // Crear tabla de boosts personales
        String createBoostTableSQL = """
            CREATE TABLE IF NOT EXISTS player_boosts (
                player_uuid VARCHAR(36) PRIMARY KEY,
                multiplier DOUBLE DEFAULT 1.0,
                expiry BIGINT DEFAULT 0
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createBoostTableSQL);
        }

        // Crear tabla de historial de regalos
        String createHistoryTableSQL = """
            CREATE TABLE IF NOT EXISTS gift_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                sender_uuid VARCHAR(36) NOT NULL,
                receiver_uuid VARCHAR(36) NOT NULL,
                gift_name VARCHAR(100) NOT NULL,
                points_earned INTEGER NOT NULL,
                timestamp BIGINT NOT NULL
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createHistoryTableSQL);
        }

        // Crear tabla de límite diario
        String createDailyTableSQL = """
            CREATE TABLE IF NOT EXISTS daily_gifts (
                player_uuid VARCHAR(36) NOT NULL,
                date VARCHAR(10) NOT NULL,
                gift_count INTEGER DEFAULT 0,
                PRIMARY KEY (player_uuid, date)
            )
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createDailyTableSQL);
        }
    }

    public void setPersonalBoost(String playerUUID, double multiplier, long expiry) {
        String sql = """
            INSERT INTO player_boosts (player_uuid, multiplier, expiry) 
            VALUES (?, ?, ?) 
            ON CONFLICT(player_uuid) 
            DO UPDATE SET multiplier = excluded.multiplier, expiry = excluded.expiry
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setDouble(2, multiplier);
            pstmt.setLong(3, expiry);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error setting personal boost", e);
        }
    }

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
            plugin.getLogger().log(Level.WARNING, "Error getting personal boost", e);
        }
        return 1.0;
    }

    public void addPersonalPoints(String playerUUID, int points) {
        String sql = """
            INSERT INTO player_points (player_uuid, points) 
            VALUES (?, ?) 
            ON CONFLICT(player_uuid) 
            DO UPDATE SET points = points + excluded.points
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error adding personal points", e);
        }
    }

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
            plugin.getLogger().log(Level.WARNING, "Error getting personal points", e);
        }
        return 0;
    }

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
            plugin.getLogger().log(Level.WARNING, "Error spending personal points", e);
            return false;
        }
    }

    public void setPersonalPoints(String playerUUID, int points) {
        String sql = """
            INSERT INTO player_points (player_uuid, points) 
            VALUES (?, ?) 
            ON CONFLICT(player_uuid) 
            DO UPDATE SET points = excluded.points
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error setting personal points", e);
        }
    }

    public void close() {
        // Crear un backup antes de cerrar la conexión
        createFinalBackup();
        
        // Detener el sistema de backup
        if (backupTimer != null) {
            backupTimer.cancel();
        }
        
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
        }
    }

    public void saveFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        String sql = """
            INSERT INTO friendships (sender_uuid, receiver_uuid, points, last_interaction) 
            VALUES (?, ?, ?, ?) 
            ON CONFLICT(sender_uuid, receiver_uuid) 
            DO UPDATE SET points = points + excluded.points, last_interaction = excluded.last_interaction
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, senderUUID);
            pstmt.setString(2, receiverUUID);
            pstmt.setInt(3, points);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving friendship points", e);
        }
    }

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
            plugin.getLogger().log(Level.WARNING, "Error getting friendship points", e);
        }
        return 0;
    }

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
            plugin.getLogger().log(Level.WARNING, "Error getting player friends", e);
        }
        return friends;
    }

    public List<DatabaseManager.FriendshipPair> getTopFriendshipPairs(int limit) {
        List<DatabaseManager.FriendshipPair> pairs = new ArrayList<>();
        String sql = """
            SELECT 
                CASE 
                    WHEN sender_uuid < receiver_uuid THEN sender_uuid 
                    ELSE receiver_uuid 
                END as player1,
                CASE 
                    WHEN sender_uuid < receiver_uuid THEN receiver_uuid 
                    ELSE sender_uuid 
                END as player2,
                SUM(points) as total_points
            FROM friendships 
            WHERE points > 0 
            GROUP BY 
                CASE 
                    WHEN sender_uuid < receiver_uuid THEN sender_uuid 
                    ELSE receiver_uuid 
                END,
                CASE 
                    WHEN sender_uuid < receiver_uuid THEN receiver_uuid 
                    ELSE sender_uuid 
                END
            ORDER BY total_points DESC 
            LIMIT ?
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pairs.add(new FriendshipPair(
                        rs.getString("player1"),
                        rs.getString("player2"),
                        rs.getInt("total_points")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting top friendship pairs", e);
        }
        return pairs;
    }

    public int getTotalFriendshipPoints(String playerUUID) {
        String sql = """
            SELECT SUM(points) as total FROM friendships 
            WHERE sender_uuid = ? AND points > 0
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting total friendship points", e);
        }
        return 0;
    }

    private void startBackupSystem() {
        backupTimer = new java.util.Timer();
        // Programar backup cada 1 hora (1 * 60 * 60 * 1000 milisegundos)
        long backupInterval = 1 * 60 * 60 * 1000L; // 1 hora en milisegundos
        
        backupTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                createBackup();
            }
        }, backupInterval, backupInterval); // Primera ejecución después de 1 hora, luego cada 1 hora
        
        plugin.getLogger().info("Sistema de backup iniciado. Se crearán backups cada 1 hora.");
    }

    private void createBackup() {
        try {
            File dataFolder = plugin.getDataFolder();
            File originalDB = new File(dataFolder, DATABASE_NAME);
            File backupFolder = new File(dataFolder, BACKUP_FOLDER);
            
            if (!originalDB.exists()) {
                plugin.getLogger().warning("La base de datos original no existe para hacer backup.");
                return;
            }
            
            // Crear nombre de archivo con timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupFileName = DATABASE_NAME + "_" + timestamp + ".bak";
            File backupFile = new File(backupFolder, backupFileName);
            
            // Copiar la base de datos
            Files.copy(originalDB.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            plugin.getLogger().info("Backup creado exitosamente: " + backupFileName);
            
            // Limpiar backups antiguos (mantener solo los últimos 5)
            cleanupOldBackups();
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando backup de la base de datos", e);
        }
    }
    
    public void createManualBackup() {
        try {
            File dataFolder = plugin.getDataFolder();
            File originalDB = new File(dataFolder, DATABASE_NAME);
            File backupFolder = new File(dataFolder, BACKUP_FOLDER);
            
            if (!originalDB.exists()) {
                plugin.getLogger().warning("La base de datos original no existe para hacer backup manual.");
                return;
            }
            
            // Crear nombre de archivo con timestamp para el backup manual
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupFileName = DATABASE_NAME + "_manual_" + timestamp + ".bak";
            File backupFile = new File(backupFolder, backupFileName);
            
            // Copiar la base de datos
            Files.copy(originalDB.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            plugin.getLogger().info("Backup manual creado exitosamente: " + backupFileName);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando backup manual de la base de datos", e);
        }
    }
    
    private void createFinalBackup() {
        try {
            File dataFolder = plugin.getDataFolder();
            File originalDB = new File(dataFolder, DATABASE_NAME);
            File backupFolder = new File(dataFolder, BACKUP_FOLDER);
            
            if (!originalDB.exists()) {
                plugin.getLogger().warning("La base de datos original no existe para hacer backup final.");
                return;
            }
            
            // Crear nombre de archivo con timestamp para el backup final
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupFileName = DATABASE_NAME + "_final_" + timestamp + ".bak";
            File backupFile = new File(backupFolder, backupFileName);
            
            // Copiar la base de datos
            Files.copy(originalDB.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            
            plugin.getLogger().info("Backup final creado exitosamente antes de cerrar: " + backupFileName);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error creando backup final de la base de datos", e);
        }
    }

    private void cleanupOldBackups() {
        try {
            File backupFolder = new File(plugin.getDataFolder(), BACKUP_FOLDER);
            File[] backupFiles = backupFolder.listFiles((dir, name) -> 
                name.startsWith(DATABASE_NAME) && name.endsWith(".bak") && !name.contains("_final_"));
            
            if (backupFiles != null && backupFiles.length > 5) {
                // Ordenar por fecha de modificación (más antiguos primero)
                Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
                
                // Eliminar los más antiguos, dejando solo los 5 más recientes
                for (int i = 0; i < backupFiles.length - 5; i++) {
                    if (!backupFiles[i].delete()) {
                        plugin.getLogger().warning("No se pudo eliminar el backup antiguo: " + backupFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error limpiando backups antiguos", e);
        }
    }

    public static class FriendshipPair {
        private final String player1UUID;
        private final String player2UUID;
        private final int points;

        public FriendshipPair(String player1UUID, String player2UUID, int points) {
            this.player1UUID = player1UUID;
            this.player2UUID = player2UUID;
            this.points = points;
        }

        public String getPlayer1UUID() { return player1UUID; }
        public String getPlayer2UUID() { return player2UUID; }
        public int getPoints() { return points; }
    }

    // =======================
    // Historial de Regalos
    // =======================

    public static class GiftHistoryEntry {
        private final String senderUUID;
        private final String receiverUUID;
        private final String giftName;
        private final int points;
        private final long timestamp;

        public GiftHistoryEntry(String senderUUID, String receiverUUID, String giftName, int points, long timestamp) {
            this.senderUUID = senderUUID;
            this.receiverUUID = receiverUUID;
            this.giftName = giftName;
            this.points = points;
            this.timestamp = timestamp;
        }

        public String getSenderUUID() { return senderUUID; }
        public String getReceiverUUID() { return receiverUUID; }
        public String getGiftName() { return giftName; }
        public int getPoints() { return points; }
        public long getTimestamp() { return timestamp; }
    }

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
            plugin.getLogger().log(Level.WARNING, "Error saving gift history", e);
        }
    }

    public List<GiftHistoryEntry> getGiftHistory(String playerUUID, int limit, int offset) {
        List<GiftHistoryEntry> history = new ArrayList<>();
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
                    history.add(new GiftHistoryEntry(
                        rs.getString("sender_uuid"),
                        rs.getString("receiver_uuid"),
                        rs.getString("gift_name"),
                        rs.getInt("points_earned"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting gift history", e);
        }
        return history;
    }

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
            plugin.getLogger().log(Level.WARNING, "Error getting history count", e);
        }
        return 0;
    }

    // =======================
    // Límite Diario
    // =======================

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
            plugin.getLogger().log(Level.WARNING, "Error getting daily gift count", e);
        }
        return 0;
    }

    public void incrementDailyGiftCount(String playerUUID) {
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());
        String sql = """
            INSERT INTO daily_gifts (player_uuid, date, gift_count) 
            VALUES (?, ?, 1) 
            ON CONFLICT(player_uuid, date) 
            DO UPDATE SET gift_count = gift_count + 1
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, today);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error incrementing daily gift count", e);
        }
    }
}