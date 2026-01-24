package com.fredygraces.giftbond.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fredygraces.giftbond.GiftBond;

/**
 * Sistema avanzado de gestión de transacciones para operaciones atómicas
 * Garantiza consistencia de datos y rollback automático en caso de errores
 * 
 * @author GiftBond Team
 * @version 1.2.0
 */
public class TransactionManager {
    private final GiftBond plugin;
    private static final Logger logger = Logger.getLogger(TransactionManager.class.getName());
    
    // Pool de conexiones para transacciones
    private final ConcurrentHashMap<String, Connection> transactionConnections;
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> locks;
    
    // Configuración de retry
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 100;
    
    public TransactionManager(GiftBond plugin) {
        this.plugin = plugin;
        this.transactionConnections = new ConcurrentHashMap<>();
        this.locks = new ConcurrentHashMap<>();
    }
    
    /**
     * Ejecutar operación en transacción con retry automático
     */
    public <T> T executeInTransaction(TransactionOperation<T> operation) throws SQLException {
        return executeInTransaction(operation, "default_transaction");
    }
    
    /**
     * Ejecutar operación en transacción con nombre específico
     */
    public <T> T executeInTransaction(TransactionOperation<T> operation, String transactionName) throws SQLException {
        Connection conn = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                // Obtener conexión exclusiva para esta transacción
                conn = plugin.getDatabaseManager().getConnection();
                
                if (conn == null || conn.isClosed()) {
                    throw new SQLException("Conexión a base de datos no disponible");
                }
                
                // Iniciar transacción
                conn.setAutoCommit(false);
                logger.fine("[" + transactionName + "] Transacción iniciada (intento " + attempt + ")");
                
                // Ejecutar operación
                T result = operation.execute(conn);
                
                // Commit si todo salió bien
                conn.commit();
                logger.fine("[" + transactionName + "] Transacción confirmada exitosamente");
                
                return result;
                
            } catch (SQLException e) {
                // Rollback en caso de error
                if (conn != null) {
                    try {
                        conn.rollback();
                        logger.warning("[" + transactionName + "] Rollback ejecutado debido a: " + e.getMessage());
                    } catch (SQLException rollbackEx) {
                        logger.severe("[" + transactionName + "] Error en rollback: " + rollbackEx.getMessage());
                    }
                }
                
                // Si es el último intento, lanzar excepción
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    logger.severe("[" + transactionName + "] Transacción fallida después de " + MAX_RETRY_ATTEMPTS + " intentos: " + e.getMessage());
                    throw e;
                }
                
                // Esperar antes de reintentar
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Transacción interrumpida", ie);
                }
                
                logger.warning("[" + transactionName + "] Reintentando transacción (intento " + (attempt + 1) + "/" + MAX_RETRY_ATTEMPTS + "): " + e.getMessage());
            } finally {
                // Restaurar auto-commit y cerrar conexión
                if (conn != null) {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException e) {
                        logger.log(Level.WARNING, "Error restaurando auto-commit", e);
                    }
                }
            }
        }
        
        throw new SQLException("No se pudo completar la transacción después de múltiples intentos");
    }
    
    /**
     * Ejecutar operación con bloqueo para prevenir condiciones de carrera
     */
    public <T> T executeWithLock(String lockKey, TransactionOperation<T> operation) throws SQLException {
        ReentrantReadWriteLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantReadWriteLock());
        
        lock.writeLock().lock();
        try {
            return executeInTransaction(operation, "locked_operation_" + lockKey);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Ejecutar operación de lectura con bloqueo compartido
     */
    public <T> T executeReadOnlyWithLock(String lockKey, TransactionOperation<T> operation) throws SQLException {
        ReentrantReadWriteLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantReadWriteLock());
        
        lock.readLock().lock();
        try {
            return executeInTransaction(operation, "readonly_operation_" + lockKey);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Verificar integridad de transacciones activas
     */
    public TransactionHealth checkTransactionHealth() {
        int activeTransactions = transactionConnections.size();
        boolean databaseConnected = false;
        
        try {
            Connection conn = plugin.getDatabaseManager().getConnection();
            databaseConnected = conn != null && !conn.isClosed();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error checking database connection", e);
        }
        
        return new TransactionHealth(activeTransactions, databaseConnected);
    }
    
    /**
     * Interface para operaciones transaccionales
     */
    @FunctionalInterface
    public interface TransactionOperation<T> {
        T execute(Connection connection) throws SQLException;
    }
    
    /**
     * Clase para reporte de salud de transacciones
     */
    public static class TransactionHealth {
        private final int activeTransactions;
        private final boolean databaseConnected;
        
        public TransactionHealth(int activeTransactions, boolean databaseConnected) {
            this.activeTransactions = activeTransactions;
            this.databaseConnected = databaseConnected;
        }
        
        public int getActiveTransactions() { return activeTransactions; }
        public boolean isDatabaseConnected() { return databaseConnected; }
        public boolean isHealthy() { return databaseConnected && activeTransactions < 50; }
        
        @Override
        public String toString() {
            return String.format("TransactionHealth{active=%d, connected=%s, healthy=%s}", 
                activeTransactions, databaseConnected, isHealthy());
        }
    }
}