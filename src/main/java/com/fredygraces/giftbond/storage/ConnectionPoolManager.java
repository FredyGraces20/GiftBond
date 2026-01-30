package com.fredygraces.giftbond.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fredygraces.giftbond.GiftBond;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Sistema avanzado de connection pooling con HikariCP
 * Optimiza el rendimiento de conexiones a base de datos
 * 
 * @author GiftBond Team
 * @version 1.2.0
 */
public class ConnectionPoolManager {
    private final GiftBond plugin;
    private static final Logger logger = Logger.getLogger(ConnectionPoolManager.class.getName());
    
    private HikariDataSource dataSource;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final String DATABASE_NAME = "friendships.db";
    
    // Configuración optimizada
    private static final int MINIMUM_IDLE = 2;
    private static final int MAXIMUM_POOL_SIZE = 10;
    private static final long CONNECTION_TIMEOUT = 30000L; // 30 segundos
    private static final long IDLE_TIMEOUT = 600000L; // 10 minutos
    private static final long MAX_LIFETIME = 1800000L; // 30 minutos
    
    public ConnectionPoolManager(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inicializar el connection pool
     */
    public boolean initialize() {
        if (initialized.get()) {
            logger.warning("Connection pool ya inicializado");
            return true;
        }
        
        try {
            // logger.info("🔄 Inicializando connection pool HikariCP...");
            
            HikariConfig config = new HikariConfig();
            
            // Configuración de base de datos
            String dbPath = plugin.getDataFolder().toPath().resolve(DATABASE_NAME).toString();
            config.setJdbcUrl("jdbc:sqlite:" + dbPath);
            config.setDriverClassName("org.sqlite.JDBC");
            
            // Configuración de pool
            config.setMinimumIdle(MINIMUM_IDLE);
            config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
            config.setConnectionTimeout(CONNECTION_TIMEOUT);
            config.setIdleTimeout(IDLE_TIMEOUT);
            config.setMaxLifetime(MAX_LIFETIME);
            
            // Configuración de rendimiento
            config.setLeakDetectionThreshold(60000L); // 1 minuto
            config.setValidationTimeout(5000L); // 5 segundos
            
            // Propiedades de SQLite
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("journalMode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");
            
            // Nombre del pool
            config.setPoolName("GiftBond-Pool");
            
            // Crear datasource
            dataSource = new HikariDataSource(config);
            
            // Testear conexión
            try (Connection testConn = dataSource.getConnection()) {
                if (testConn.isValid(5)) {
                    initialized.set(true);
                    // logger.info("✅ Connection pool inicializado exitosamente");
                    // logPoolStatus();
                    return true;
                } else {
                    logger.severe("❌ Conexión de prueba fallida");
                    return false;
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "❌ Error inicializando connection pool", e);
            return false;
        }
    }
    
    /**
     * Obtener conexión del pool
     */
    public Connection getConnection() throws SQLException {
        if (!initialized.get()) {
            throw new SQLException("Connection pool no inicializado");
        }
        
        try {
            Connection conn = dataSource.getConnection();
            if (conn == null) {
                throw new SQLException("No se pudo obtener conexión del pool");
            }
            return conn;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error obteniendo conexión del pool", e);
            throw e;
        }
    }
    
    /**
     * Cerrar el connection pool
     */
    public void shutdown() {
        if (initialized.get() && dataSource != null) {
            // logger.info("🔄 Cerrando connection pool...");
            try {
                dataSource.close();
                initialized.set(false);
                // logger.info("✅ Connection pool cerrado exitosamente");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error cerrando connection pool", e);
            }
        }
    }
    
    /**
     * Obtener estadísticas del pool
     */
    public PoolStats getPoolStats() {
        if (!initialized.get() || dataSource == null) {
            return new PoolStats(0, 0, 0, 0);
        }
        
        return new PoolStats(
            dataSource.getHikariPoolMXBean().getTotalConnections(),
            dataSource.getHikariPoolMXBean().getActiveConnections(),
            dataSource.getHikariPoolMXBean().getIdleConnections(),
            dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection()
        );
    }
    
    /**
     * Verificar salud del pool
     */
    public boolean isHealthy() {
        if (!initialized.get() || dataSource == null) {
            return false;
        }
        
        try {
            PoolStats stats = getPoolStats();
            // Saludable si hay conexiones disponibles y no hay threads esperando
            return stats.getTotalConnections() > 0 && 
                   stats.getThreadsAwaitingConnection() == 0 &&
                   stats.getActiveConnections() < MAXIMUM_POOL_SIZE;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error verificando salud del pool", e);
            return false;
        }
    }
    
    /**
     * Forzar recreación del pool si hay problemas
     */
    public boolean recreatePool() {
        logger.warning("🔄 Recreando connection pool...");
        shutdown();
        
        try {
            Thread.sleep(1000); // Esperar 1 segundo
            return initialize();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Loggear estado actual del pool
     */
    public void logPoolStatus() {
        if (!initialized.get() || dataSource == null) {
            // logger.info(".Pool status: NOT INITIALIZED");
            return;
        }
        
        PoolStats stats = getPoolStats();
/*
        logger.info(String.format(
            "📊 Pool Status - Total: %d, Active: %d, Idle: %d, Waiting: %d",
            stats.getTotalConnections(),
            stats.getActiveConnections(),
            stats.getIdleConnections(),
            stats.getThreadsAwaitingConnection()
        ));
*/
        
        if (stats.getThreadsAwaitingConnection() > 0) {
            logger.warning("⚠ Hay threads esperando conexiones - considera aumentar el tamaño del pool");
        }
        
        if (stats.getActiveConnections() >= MAXIMUM_POOL_SIZE * 0.8) {
            logger.warning("⚠ Alto uso de conexiones (" + stats.getActiveConnections() + "/" + MAXIMUM_POOL_SIZE + ")");
        }
    }
    
    /**
     * Clase para estadísticas del pool
     */
    public static class PoolStats {
        private final int totalConnections;
        private final int activeConnections;
        private final int idleConnections;
        private final int threadsAwaitingConnection;
        
        public PoolStats(int totalConnections, int activeConnections, int idleConnections,
                        int threadsAwaitingConnection) {
            this.totalConnections = totalConnections;
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
        }
        
        public int getTotalConnections() { return totalConnections; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        
        public boolean isUnderPressure() {
            return threadsAwaitingConnection > 0 || 
                   activeConnections >= totalConnections * 0.9;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PoolStats{total=%d, active=%d, idle=%d, waiting=%d}",
                totalConnections, activeConnections, idleConnections, threadsAwaitingConnection
            );
        }
    }
}