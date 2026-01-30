package com.fredygraces.giftbond.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.DatabaseManager;
import com.fredygraces.giftbond.models.MailboxGift;
import com.fredygraces.giftbond.utils.DebugLogger;

/**
 * DAO para manejar operaciones de mailbox con SQLite
 */
public class MailboxDAO {
    private final GiftBond plugin;
    private final DebugLogger debugLogger;
    private final DatabaseManager databaseManager;

    public MailboxDAO(GiftBond plugin) {
        this.plugin = plugin;
        this.debugLogger = new DebugLogger(plugin);
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * Inicializar tablas de mailbox en la base de datos SQLite
     */
    public void initializeTables() {
        try {
            Connection conn = databaseManager.getConnection();

            // Crear tabla de regalos pendientes
            String createGiftsTable = """
                CREATE TABLE IF NOT EXISTS pending_gifts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    receiver_uuid TEXT NOT NULL,
                    receiver_name TEXT NOT NULL,
                    sender_uuid TEXT NOT NULL,
                    sender_name TEXT NOT NULL,
                    gift_id TEXT NOT NULL,
                    gift_name TEXT NOT NULL,
                    items_serialized TEXT,
                    shared_items_serialized TEXT,
                    money REAL DEFAULT 0,
                    base_points INTEGER NOT NULL DEFAULT 0,    -- Puntos base del regalo
                    points_awarded INTEGER NOT NULL DEFAULT 0, -- Puntos con boost del momento de envío
                    timestamp INTEGER NOT NULL,
                    claimed INTEGER DEFAULT 0,
                    claim_timestamp INTEGER
                )
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createGiftsTable);
                debugLogger.debug("✅ Tabla pending_gifts creada/verificada en SQLite");
            }

            // Crear tabla de estadísticas
            String createStatsTable = """
                CREATE TABLE IF NOT EXISTS mailbox_stats (
                    player_uuid TEXT PRIMARY KEY,
                    total_received INTEGER DEFAULT 0,
                    total_claimed INTEGER DEFAULT 0,
                    total_expired INTEGER DEFAULT 0,
                    last_activity INTEGER
                )
                """;

            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createStatsTable);
                debugLogger.debug("✅ Tabla mailbox_stats creada/verificada en SQLite");
            }

        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "❌ Error inicializando tablas de mailbox: " + e.getMessage());
        }
    }

    /**
     * Guardar un nuevo regalo en el mailbox
     */
    public boolean saveGift(MailboxGift gift) {
        // Usar transacción atómica con retry
        try {
            return plugin.getTransactionManager().executeInTransaction(conn -> {
                String sql = """
                    INSERT INTO pending_gifts
                    (receiver_uuid, receiver_name, sender_uuid, sender_name, gift_id, gift_name,
                     items_serialized, shared_items_serialized, money, base_points, points_awarded, timestamp, claimed)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setString(1, gift.getReceiverUUID().toString());
                    pstmt.setString(2, gift.getReceiverName());
                    pstmt.setString(3, gift.getSenderUUID().toString());
                    pstmt.setString(4, gift.getSenderName());
                    pstmt.setString(5, gift.getGiftId());
                    pstmt.setString(6, gift.getGiftName());
                    pstmt.setString(7, serializeItems(gift.getOriginalItems()));
                    pstmt.setString(8, serializeItems(gift.getSharedItems()));
                    pstmt.setDouble(9, gift.getMoney());
                    pstmt.setInt(10, gift.getBasePoints());
                    pstmt.setInt(11, gift.getPointsAwarded());
                    pstmt.setLong(12, gift.getTimestamp());
                    pstmt.setInt(13, gift.isClaimed() ? 1 : 0);
                    
                    int affectedRows = pstmt.executeUpdate();
                    
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                gift.setId(generatedKeys.getInt(1));
                                
                                // Actualizar estadísticas con sincronización
                                plugin.getSynchronizationManager().executeSynchronized(
                                    "stats_" + gift.getReceiverUUID().toString(), 
                                    () -> {
                                        updateStats(gift.getReceiverUUID(), "received");
                                        return null;
                                    }
                                );
                                
                                debugLogger.debug("✅ Regalo guardado en mailbox (ID: " + gift.getId() + ")");
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }, "save_gift_" + gift.getGiftId());
            
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "❌ Error guardando regalo en mailbox (transacción fallida): " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtener resumen de regalos pendientes por remitente
     */
    public List<GiftSummary> getPendingGiftSummaries(UUID receiverUUID) {
        String sql = """
            SELECT sender_name, COUNT(*) as gift_count, MAX(timestamp) as last_gift
            FROM pending_gifts
            WHERE receiver_uuid = ? AND claimed = 0
            GROUP BY sender_name
            ORDER BY last_gift DESC
            """;

        List<GiftSummary> summaries = new ArrayList<>();

        try {
            Connection conn = databaseManager.getConnection();
            if (conn == null) return summaries;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, receiverUUID.toString());

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        summaries.add(new GiftSummary(
                            rs.getString("sender_name"),
                            rs.getInt("gift_count"),
                            rs.getLong("last_gift")
                        ));
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "❌ Error obteniendo resumen de mailbox: " + e.getMessage());
        }

        return summaries;
    }

    /**
     * Obtener regalos pendientes de un remitente específico
     */
    public List<MailboxGift> getPendingGiftsFromSender(UUID receiverUUID, String senderName) {
        String sql = """
            SELECT * FROM pending_gifts
            WHERE receiver_uuid = ? AND sender_name = ? AND claimed = 0
            ORDER BY timestamp ASC
            """;

        return getGiftsByQuery(sql, receiverUUID.toString(), senderName);
    }

    /**
     * Obtener todos los regalos pendientes de un jugador
     */
    public List<MailboxGift> getAllPendingGifts(UUID receiverUUID) {
        String sql = """
            SELECT * FROM pending_gifts
            WHERE receiver_uuid = ? AND claimed = 0
            ORDER BY timestamp ASC
            """;

        return getGiftsByQuery(sql, receiverUUID.toString());
    }

    /**
     * Marcar regalo como reclamado
     */
    public boolean markAsClaimed(int giftId) {
        // Usar transacción atómica
        try {
            return plugin.getTransactionManager().executeInTransaction(conn -> {
                String sql = "UPDATE pending_gifts SET claimed = 1, claim_timestamp = ? WHERE id = ?";
                
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, System.currentTimeMillis());
                    pstmt.setInt(2, giftId);
                    
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected > 0) {
                        debugLogger.debug("✅ Regalo marcado como reclamado (ID: " + giftId + ")");
                        return true;
                    }
                    return false;
                }
            }, "mark_claimed_" + giftId);
            
        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "❌ Error marcando regalo como reclamado (transacción fallida): " + e.getMessage());
            return false;
        }
    }

    /**
     * Eliminar regalo completamente (después de reclamar)
     */
    public boolean deleteGift(int giftId) {
        String sql = "DELETE FROM pending_gifts WHERE id = ?";

        try {
            Connection conn = databaseManager.getConnection();
            if (conn == null) return false;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, giftId);

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    debugLogger.debug("✅ Regalo eliminado del mailbox (ID: " + giftId + ")");
                    return true;
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "❌ Error eliminando regalo: " + e.getMessage());
        }

        return false;
    }

    // Métodos utilitarios privados
    private List<MailboxGift> getGiftsByQuery(String sql, Object... params) {
        List<MailboxGift> gifts = new ArrayList<>();

        try {
            Connection conn = databaseManager.getConnection();
            if (conn == null) return gifts;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        gifts.add(mapResultSetToGift(rs));
                    }
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().severe(() -> "❌ Error ejecutando consulta de regalos: " + e.getMessage());
        }

        return gifts;
    }

    private MailboxGift mapResultSetToGift(ResultSet rs) throws SQLException {
        // Primero intentamos obtener base_points, si no existe usamos points_awarded como fallback
        int basePoints;
        try {
            basePoints = rs.getInt("base_points");
        } catch (SQLException e) {
            // Columna no existe, usar points_awarded como base
            basePoints = rs.getInt("points_awarded");
        }

        return new MailboxGift(
            rs.getInt("id"),
            UUID.fromString(rs.getString("receiver_uuid")),
            rs.getString("receiver_name"),
            UUID.fromString(rs.getString("sender_uuid")),
            rs.getString("sender_name"),
            rs.getString("gift_id"),
            rs.getString("gift_name"),
            deserializeItems(rs.getString("items_serialized")),
            deserializeItems(rs.getString("shared_items_serialized")),
            rs.getDouble("money"),
            basePoints,  // base_points
            rs.getInt("points_awarded"), // points_awarded
            rs.getLong("timestamp"),
            rs.getInt("claimed") == 1,
            rs.getObject("claim_timestamp") != null ? rs.getLong("claim_timestamp") : null
        );
    }

    private String serializeItems(List<ItemStack> items) {
        try {
            // Convertir ItemStack a formato serializable
            List<Map<String, Object>> serializableItems = new ArrayList<>();

            for (ItemStack item : items) {
                if (item != null) {
                    // Crear una copia para asegurar que sea serializable
                    ItemStack copy = item.clone();
                    Map<String, Object> itemData = copy.serialize();
                    serializableItems.add(itemData);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(serializableItems);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "❌ Error serializando items: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> deserializeItems(String serialized) {
        if (serialized == null || serialized.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            byte[] data = Base64.getDecoder().decode(serialized);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                List<Map<String, Object>> itemDataList = (List<Map<String, Object>>) ois.readObject();
                List<ItemStack> items = new ArrayList<>();

                for (Map<String, Object> itemData : itemDataList) {
                    try {
                        ItemStack item = ItemStack.deserialize(itemData);
                        items.add(item);
                    } catch (Exception e) {
                        plugin.getLogger().warning(() -> "⚠ Error recreando item: " + e.getMessage());
                    }
                }

                return items;
            }
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().severe(() -> "❌ Error deserializando items: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private void updateStats(UUID playerUUID, String action) {
        String sql = """
            INSERT OR REPLACE INTO mailbox_stats
            (player_uuid, total_received, total_claimed, last_activity)
            VALUES (?,
                   COALESCE((SELECT total_received FROM mailbox_stats WHERE player_uuid = ?), 0) +
                   CASE WHEN ? = 'received' THEN 1 ELSE 0 END,
                   COALESCE((SELECT total_claimed FROM mailbox_stats WHERE player_uuid = ?), 0) +
                   CASE WHEN ? = 'claimed' THEN 1 ELSE 0 END,
                   ?)
            """;

        try {
            Connection conn = databaseManager.getConnection();
            if (conn == null) return;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, playerUUID.toString());
                pstmt.setString(3, action);
                pstmt.setString(4, playerUUID.toString());
                pstmt.setString(5, action);
                pstmt.setLong(6, System.currentTimeMillis());
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            debugLogger.debugWarning("No se pudieron actualizar estadísticas: " + e.getMessage());
        }
    }

    // Clase interna para resumen de regalos
    public static class GiftSummary {
        private final String senderName;
        private final int count;
        private final long lastGiftTimestamp;

        public GiftSummary(String senderName, int count, long lastGiftTimestamp) {
            this.senderName = senderName;
            this.count = count;
            this.lastGiftTimestamp = lastGiftTimestamp;
        }

        public String getSenderName() { return senderName; }
        public int getCount() { return count; }
        public long getLastGiftTimestamp() { return lastGiftTimestamp; }
    }
}
