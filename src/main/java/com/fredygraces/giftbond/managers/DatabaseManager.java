package com.fredygraces.giftbond.managers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.models.MailboxGift;
import com.fredygraces.giftbond.storage.MailboxDAO;

public class DatabaseManager {
    private final GiftBond plugin;
    private Connection connection;
    private final String DATABASE_NAME = "friendships.db";
    private final String BACKUP_FOLDER = "backups";

    public DatabaseManager(GiftBond plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                initialize();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore database connection", e);
        }
        return connection;
    }

    /**
     * Verifica si la conexión está activa y reconecta si es necesario
     * @return true si la conexión está disponible, false si falla
     */
    private boolean ensureConnection() {
        try {
            // Verificar si la conexión existe y está abierta
            if (connection == null || connection.isClosed()) {
                plugin.getLogger().info("Database connection closed, attempting to reconnect...");
                return initialize(); // Reutilizar el método de inicialización
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error checking database connection", e);
            return false;
        }
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

            return true;
        } catch (ClassNotFoundException | SQLException e) {
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

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createTableSQL);
        }

        // Create indexes for better performance
        String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_receiver_points ON friendships(receiver_uuid, points)";
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createIndexSQL);
        }

        // Crear tabla de puntos personales
        String createPersonalTableSQL = """
            CREATE TABLE IF NOT EXISTS player_points (
                player_uuid VARCHAR(36) PRIMARY KEY,
                points INTEGER DEFAULT 0
            )
            """;
        try (Statement stmt = getConnection().createStatement()) {
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
        try (Statement stmt = getConnection().createStatement()) {
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
        try (Statement stmt = getConnection().createStatement()) {
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
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createDailyTableSQL);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Error closing database connection", e);
            }
        }
    }

    // Métodos para friendship points
    public void saveFriendshipPoints(String senderUUID, String receiverUUID, int points) {
        String sql = """
            INSERT INTO friendships (sender_uuid, receiver_uuid, points, last_interaction)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(sender_uuid, receiver_uuid)
            DO UPDATE SET points = points + excluded.points, last_interaction = excluded.last_interaction
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

        // Obtener todas las relaciones donde el jugador está involucrado
        String sql = """
            SELECT 
                CASE 
                    WHEN sender_uuid = ? THEN receiver_uuid 
                    ELSE sender_uuid 
                END as friend_uuid,
                points
            FROM friendships 
            WHERE sender_uuid = ? OR receiver_uuid = ?
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, playerUUID);
            pstmt.setString(3, playerUUID);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String friendUUID = rs.getString("friend_uuid");
                    int points = rs.getInt("points");
                    friends.merge(friendUUID, points, (oldValue, newValue) -> oldValue + newValue);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting friendship points for player", e);
        }

        return friends;
    }

    public int getTotalFriendshipPoints(String playerUUID) {
        // Sumar todos los puntos donde el jugador está involucrado (enviados o recibidos)
        String sql = """
            SELECT COALESCE(SUM(points), 0) as total 
            FROM friendships 
            WHERE sender_uuid = ? OR receiver_uuid = ?
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, playerUUID);

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

    public List<FriendshipPair> getTopFriendshipPairs(int limit) {
        List<FriendshipPair> pairs = new ArrayList<>();
        // Query modificada para combinar relaciones bidireccionales
        String sql = """
            SELECT 
                CASE 
                    WHEN sender_uuid < receiver_uuid THEN sender_uuid 
                    ELSE receiver_uuid 
                END as player1_uuid,
                CASE 
                    WHEN sender_uuid < receiver_uuid THEN receiver_uuid 
                    ELSE sender_uuid 
                END as player2_uuid,
                SUM(points) as total_points
            FROM friendships
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    pairs.add(new FriendshipPair(
                        rs.getString("player1_uuid"),
                        rs.getString("player2_uuid"),
                        rs.getInt("total_points")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting top friendship pairs", e);
        }

        return pairs;
    }

    // Métodos para puntos personales
    public void addPersonalPoints(String playerUUID, int points) {
        String sql = """
            INSERT INTO player_points (player_uuid, points)
            VALUES (?, ?)
            ON CONFLICT(player_uuid)
            DO UPDATE SET points = points + excluded.points
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error adding personal points", e);
        }
    }

    public int getPersonalPoints(String playerUUID) {
        String sql = "SELECT points FROM player_points WHERE player_uuid = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

    public void setPersonalPoints(String playerUUID, int points) {
        String sql = """
            INSERT INTO player_points (player_uuid, points)
            VALUES (?, ?)
            ON CONFLICT(player_uuid)
            DO UPDATE SET points = excluded.points
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setInt(2, points);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error setting personal points", e);
        }
    }

    public boolean spendPersonalPoints(String playerUUID, int points) {
        int currentPoints = getPersonalPoints(playerUUID);
        if (currentPoints >= points) {
            setPersonalPoints(playerUUID, currentPoints - points);
            return true;
        }
        return false;
    }

    // Métodos para boosts
    public void setPersonalBoost(String playerUUID, double multiplier, long expiry) {
        String sql = """
            INSERT INTO player_boosts (player_uuid, multiplier, expiry)
            VALUES (?, ?, ?)
            ON CONFLICT(player_uuid)
            DO UPDATE SET multiplier = excluded.multiplier, expiry = excluded.expiry
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

    // Métodos para historial de regalos
    public void saveGiftHistory(String senderUUID, String receiverUUID, String giftName, int pointsEarned) {
        String sql = """
            INSERT INTO gift_history (sender_uuid, receiver_uuid, gift_name, points_earned, timestamp)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, senderUUID);
            pstmt.setString(2, receiverUUID);
            pstmt.setString(3, giftName);
            pstmt.setInt(4, pointsEarned);
            pstmt.setLong(5, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error saving gift history", e);
        }
    }

    public List<GiftHistoryEntry> getGiftHistory(String playerUUID, int limit, int offset) {
        List<GiftHistoryEntry> entries = new ArrayList<>();
        String sql = """
            SELECT gh.sender_uuid, gh.receiver_uuid, gh.gift_name, gh.points_earned, gh.timestamp,
                   sp.name as sender_name, rp.name as receiver_name
            FROM gift_history gh
            LEFT JOIN (
                SELECT DISTINCT sender_uuid as uuid, sender_uuid as name FROM friendships
                UNION
                SELECT DISTINCT receiver_uuid as uuid, receiver_uuid as name FROM friendships
            ) sp ON gh.sender_uuid = sp.uuid
            LEFT JOIN (
                SELECT DISTINCT sender_uuid as uuid, sender_uuid as name FROM friendships
                UNION
                SELECT DISTINCT receiver_uuid as uuid, receiver_uuid as name FROM friendships
            ) rp ON gh.receiver_uuid = rp.uuid
            WHERE gh.sender_uuid = ? OR gh.receiver_uuid = ?
            ORDER BY gh.timestamp DESC
            LIMIT ? OFFSET ?
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, playerUUID);
            pstmt.setInt(3, limit);
            pstmt.setInt(4, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Si no tenemos los nombres, usar los UUIDs
                    String senderName = rs.getString("sender_name");
                    if (senderName == null || senderName.equals(rs.getString("sender_uuid"))) {
                        senderName = getPlayerName(rs.getString("sender_uuid"));
                    }

                    String receiverName = rs.getString("receiver_name");
                    if (receiverName == null || receiverName.equals(rs.getString("receiver_uuid"))) {
                        receiverName = getPlayerName(rs.getString("receiver_uuid"));
                    }

                    entries.add(new GiftHistoryEntry(
                        senderName,
                        receiverName,
                        rs.getString("gift_name"),
                        rs.getInt("points_earned"),
                        rs.getLong("timestamp")
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error getting gift history with names", e);
        }

        return entries;
    }

    private String getPlayerName(String uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
            return player.getName() != null ? player.getName() : "Desconocido";
        } catch (Exception e) {
            return "Desconocido";
        }
    }

    public int getGiftHistoryCount(String playerUUID) {
        String sql = "SELECT COUNT(*) as count FROM gift_history WHERE sender_uuid = ? OR receiver_uuid = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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

    // Métodos para límite diario
    public int getDailyGiftCount(String playerUUID) {
        String today = java.time.LocalDate.now().toString();
        String sql = "SELECT gift_count FROM daily_gifts WHERE player_uuid = ? AND date = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
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
        String today = java.time.LocalDate.now().toString();
        String sql = """
            INSERT INTO daily_gifts (player_uuid, date, gift_count)
            VALUES (?, ?, 1)
            ON CONFLICT(player_uuid, date)
            DO UPDATE SET gift_count = gift_count + 1
            """;

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, playerUUID);
            pstmt.setString(2, today);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error incrementing daily gift count", e);
        }
    }

    public void resetDailyGiftCounts() {
        String yesterday = java.time.LocalDate.now().minusDays(1).toString();
        String sql = "DELETE FROM daily_gifts WHERE date < ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, yesterday);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error resetting daily gift counts", e);
        }
    }

    public boolean createManualBackup() {
        try {
            File dataFolder = plugin.getDataFolder();
            File originalDB = new File(dataFolder, DATABASE_NAME);
            File backupFolder = new File(dataFolder, BACKUP_FOLDER);

            if (!originalDB.exists()) {
                plugin.getLogger().warning("Database file not found for backup");
                return false;
            }

            // Crear nombre de archivo con timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupFileName = DATABASE_NAME + "_manual_" + timestamp + ".bak";
            File backupFile = new File(backupFolder, backupFileName);

            // Copiar la base de datos
            Files.copy(originalDB.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            plugin.getLogger().info(() -> "Manual backup created successfully: " + backupFileName);
            return true;

        } catch (IOException | SecurityException e) {
            plugin.getLogger().log(Level.WARNING, "Error creating manual backup", e);
            return false;
        }
    }

    public boolean savePendingGift(String senderUUID, String senderName, String receiverUUID,
                                  String receiverName, String giftId, String giftName,
                                  List<ItemStack> originalItems, List<ItemStack> sharedItems,
                                  int basePoints, int pointsAwarded) {
        // Crear objeto MailboxGift
        MailboxGift gift = new MailboxGift(
            java.util.UUID.fromString(receiverUUID),
            receiverName,
            java.util.UUID.fromString(senderUUID),
            senderName,
            giftId,
            giftName,
            originalItems,
            sharedItems,
            0.0, // No money
            basePoints,
            pointsAwarded
        );

        // Usar MailboxDAO para guardar
        MailboxDAO mailboxDAO = new MailboxDAO(plugin);
        return mailboxDAO.saveGift(gift);
    }

    // Clases internas para representar datos
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

    public static class GiftRecord {
        private final String senderUUID;
        private final String receiverUUID;
        private final String giftName;
        private final int pointsEarned;
        private final long timestamp;

        public GiftRecord(String senderUUID, String receiverUUID, String giftName, int pointsEarned, long timestamp) {
            this.senderUUID = senderUUID;
            this.receiverUUID = receiverUUID;
            this.giftName = giftName;
            this.pointsEarned = pointsEarned;
            this.timestamp = timestamp;
        }

        public String getSenderUUID() { return senderUUID; }
        public String getReceiverUUID() { return receiverUUID; }
        public String getGiftName() { return giftName; }
        public int getPointsEarned() { return pointsEarned; }
        public long getTimestamp() { return timestamp; }
    }

    public static class GiftHistoryEntry {
        private final String senderName;
        private final String receiverName;
        private final String giftName;
        private final int points;
        private final long timestamp;

        public GiftHistoryEntry(String senderName, String receiverName, String giftName, int points, long timestamp) {
            this.senderName = senderName;
            this.receiverName = receiverName;
            this.giftName = giftName;
            this.points = points;
            this.timestamp = timestamp;
        }

        public String getSenderName() { return senderName; }
        public String getReceiverName() { return receiverName; }
        public String getGiftName() { return giftName; }
        public int getPoints() { return points; }
        public long getTimestamp() { return timestamp; }
    }
}
