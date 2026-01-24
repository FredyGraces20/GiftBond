package com.fredygraces.giftbond.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import com.fredygraces.giftbond.GiftBond;

/**
 * Sistema avanzado de sincronización y control de concurrencia
 * Previene condiciones de carrera y garantiza acceso seguro a recursos compartidos
 * 
 * @author GiftBond Team
 * @version 1.2.0
 */
public class SynchronizationManager {
    private final GiftBond plugin;
    private static final Logger logger = Logger.getLogger(SynchronizationManager.class.getName());
    
    // Locks para diferentes tipos de recursos
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> resourceLocks;
    private final ConcurrentHashMap<String, Object> resourceMonitors;
    
    // Contadores para monitoreo
    private final ConcurrentHashMap<String, Integer> lockCounts;
    
    public SynchronizationManager(GiftBond plugin) {
        this.plugin = plugin;
        this.resourceLocks = new ConcurrentHashMap<>();
        this.resourceMonitors = new ConcurrentHashMap<>();
        this.lockCounts = new ConcurrentHashMap<>();
    }
    
    /**
     * Obtener lock de escritura para un recurso específico
     */
    public AutoCloseableLock writeLock(String resourceName) {
        ReentrantReadWriteLock lock = getResourceLock(resourceName);
        lock.writeLock().lock();
        
        // Incrementar contador
        lockCounts.merge(resourceName, 1, Integer::sum);
        
        logger.fine("Adquirido write lock para: " + resourceName + 
                   " (conteo: " + lockCounts.get(resourceName) + ")");
        
        return new AutoCloseableLock() {
            @Override
            public void close() {
                lock.writeLock().unlock();
                lockCounts.merge(resourceName, -1, Integer::sum);
                logger.fine("Liberado write lock para: " + resourceName);
            }
        };
    }
    
    /**
     * Obtener lock de lectura para un recurso específico
     */
    public AutoCloseableLock readLock(String resourceName) {
        ReentrantReadWriteLock lock = getResourceLock(resourceName);
        lock.readLock().lock();
        
        logger.fine("Adquirido read lock para: " + resourceName);
        
        return new AutoCloseableLock() {
            @Override
            public void close() {
                lock.readLock().unlock();
                logger.fine("Liberado read lock para: " + resourceName);
            }
        };
    }
    
    /**
     * Ejecutar operación con lock de escritura
     */
    public <T> T executeWithWriteLock(String resourceName, SynchronizedOperation<T> operation) {
        try (AutoCloseableLock lock = writeLock(resourceName)) {
            return operation.execute();
        } catch (Exception e) {
            logger.severe("Error en operación sincronizada: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Ejecutar operación con lock de lectura
     */
    public <T> T executeWithReadLock(String resourceName, SynchronizedOperation<T> operation) {
        try (AutoCloseableLock lock = readLock(resourceName)) {
            return operation.execute();
        } catch (Exception e) {
            logger.severe("Error en operación de lectura sincronizada: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Sincronizar acceso a objeto monitor
     */
    public synchronized Object getMonitor(String monitorName) {
        return resourceMonitors.computeIfAbsent(monitorName, k -> new Object());
    }
    
    /**
     * Ejecutar operación sincronizada con monitor
     */
    public <T> T executeSynchronized(String monitorName, SynchronizedOperation<T> operation) {
        Object monitor = getMonitor(monitorName);
        synchronized (monitor) {
            try {
                return operation.execute();
            } catch (Exception e) {
                logger.severe("Error en operación sincronizada con monitor '" + monitorName + "': " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * Verificar estado de locks para debugging
     */
    public SynchronizationStatus getStatus() {
        int totalLocks = resourceLocks.size();
        int activeLocks = (int) lockCounts.values().stream().mapToInt(Integer::intValue).sum();
        int monitors = resourceMonitors.size();
        
        return new SynchronizationStatus(totalLocks, activeLocks, monitors);
    }
    
    /**
     * Obtener lock para un recurso (crea si no existe)
     */
    private ReentrantReadWriteLock getResourceLock(String resourceName) {
        return resourceLocks.computeIfAbsent(resourceName, k -> {
            logger.fine("Creando nuevo lock para recurso: " + resourceName);
            return new ReentrantReadWriteLock(true); // fair locking
        });
    }
    
    /**
     * Interface para operaciones sincronizadas
     */
    @FunctionalInterface
    public interface SynchronizedOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * Interface para locks autocerrables
     */
    public interface AutoCloseableLock extends AutoCloseable {
        @Override
        void close();
    }
    
    /**
     * Clase para reporte de estado de sincronización
     */
    public static class SynchronizationStatus {
        private final int totalResourceLocks;
        private final int activeLocks;
        private final int monitors;
        
        public SynchronizationStatus(int totalResourceLocks, int activeLocks, int monitors) {
            this.totalResourceLocks = totalResourceLocks;
            this.activeLocks = activeLocks;
            this.monitors = monitors;
        }
        
        public int getTotalResourceLocks() { return totalResourceLocks; }
        public int getActiveLocks() { return activeLocks; }
        public int getMonitors() { return monitors; }
        
        public boolean isHealthy() {
            return activeLocks < 100 && totalResourceLocks < 1000;
        }
        
        @Override
        public String toString() {
            return String.format("SynchronizationStatus{locks=%d, active=%d, monitors=%d, healthy=%s}",
                totalResourceLocks, activeLocks, monitors, isHealthy());
        }
    }
}