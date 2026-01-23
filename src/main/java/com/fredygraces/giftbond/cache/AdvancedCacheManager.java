package com.fredygraces.giftbond.cache;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.scheduler.BukkitRunnable;

import com.fredygraces.giftbond.GiftBond;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Sistema de caching avanzado con persistencia para proteger datos críticos
 */
public class AdvancedCacheManager {
    private final GiftBond plugin;
    private static final Logger logger = Logger.getLogger(AdvancedCacheManager.class.getName());
    
    // Cache L1: Memoria rápida
    private final Cache<String, Object> memoryCache;
    
    // Cache L2: Persistente en disco
    private final PersistentCache persistentCache;
    
    public AdvancedCacheManager(GiftBond plugin) {
        this.plugin = plugin;
        this.persistentCache = new PersistentCache(plugin);
        
        // Configurar cache en memoria
        this.memoryCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .removalListener(notification -> {
                // Persistir datos importantes antes de eliminarlos
                if (shouldPersist((String) notification.getKey())) {
                    persistentCache.put((String) notification.getKey(), notification.getValue());
                }
            })
            .build();
            
        startCacheMaintenance();
        logger.info("✓ Advanced Cache Manager initialized");
    }
    
    /**
     * Obtener dato del cache (L1 → L2 → Base de datos)
     */
    public <T> T get(String key, Class<T> type) {
        try {
            // Intentar desde cache en memoria
            if (key != null) {
                Object cached = memoryCache.getIfPresent(key);
                if (cached != null) {
                    return type.cast(cached);
                }
            }
            
            // Intentar desde cache persistente
            T persistent = persistentCache.get(key, type);
            if (persistent != null) {
                // Promover a cache en memoria
                if (key != null) {
                    memoryCache.put(key, persistent);
                    return persistent;
                }
            }
            
            return null;
        } catch (Exception e) {
            logger.severe(() -> "Error getting from cache: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Guardar dato en ambos niveles de cache
     */
    public <T> void put(String key, T value) {
        try {
            // Guardar en cache en memoria
            if (key != null && value != null) {
                memoryCache.put(key, value);
            }
            
            // Persistir datos críticos inmediatamente
            if (shouldPersistImmediately(key)) {
                persistentCache.put(key, value);
            }
        } catch (Exception e) {
            logger.severe(() -> "Error putting to cache: " + e.getMessage());
        }
    }
    
    /**
     * Invalidar cache y forzar sincronización con base de datos
     */
    public void invalidate(String key) {
        if (key != null) {
            memoryCache.invalidate(key);
            persistentCache.remove(key);
        }
    }
    
    /**
     * Limpiar todo el cache (usar con cuidado)
     */
    public void clear() {
        memoryCache.invalidateAll();
        persistentCache.clear();
        logger.info("Cache cleared and synchronized with database");
    }
    
    /**
     * Determinar si un dato debe persistirse
     */
    private boolean shouldPersist(String key) {
        return key.startsWith("friendship:") || 
               key.startsWith("points:") || 
               key.startsWith("pending_gift:");
    }
    
    /**
     * Determinar si un dato debe persistirse inmediatamente
     */
    private boolean shouldPersistImmediately(String key) {
        return key.startsWith("pending_gift:") || // Regalos pendientes son críticos
               key.startsWith("points:");         // Puntos personales son importantes
    }
    
    /**
     * Tarea de mantenimiento del cache
     */
    private void startCacheMaintenance() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    // Limpiar entradas expiradas
                    memoryCache.cleanUp();
                    
                    // Verificar integridad del cache persistente
                    persistentCache.validateIntegrity();
                    
                    // Log de estadísticas
                    logger.info(() -> String.format(
                        "Cache Stats - Memory: %d entries, Persistent: %d entries",
                        memoryCache.size(),
                        persistentCache.getSize()
                    ));
                } catch (Exception e) {
                    logger.severe(() -> "Cache maintenance error: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(plugin, 1200L, 1200L); // Cada minuto
    }
    
    /**
     * Forzar sincronización de cache con base de datos
     */
    public void forceSync() {
        logger.info("Forcing cache synchronization...");
        persistentCache.syncToDatabase();
    }
}