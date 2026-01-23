package com.fredygraces.giftbond.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.fredygraces.giftbond.GiftBond;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Cache persistente en disco para protección de datos críticos
 */
public class PersistentCache {
    private static final Logger logger = Logger.getLogger(PersistentCache.class.getName());
    private final Gson gson;
    private final Path cacheDir;
    private final Map<String, Object> memoryMirror;
    
    public PersistentCache(GiftBond plugin) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.memoryMirror = new ConcurrentHashMap<>();
        
        // Crear directorio de cache
        this.cacheDir = Paths.get(plugin.getDataFolder().getPath(), "cache");
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            logger.severe(() -> "Failed to create cache directory: " + e.getMessage());
        }
        
        loadCache();
        logger.info("✓ Persistent Cache initialized");
    }
    
    /**
     * Obtener dato del cache persistente
     */
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = memoryMirror.get(key);
            if (value != null) {
                return type.cast(value);
            }
            
            // Cargar desde archivo si existe
            Path cacheFile = cacheDir.resolve(key + ".cache");
            if (Files.exists(cacheFile)) {
                String json = Files.readString(cacheFile);
                T result = gson.fromJson(json, type);
                if (result != null) {
                    memoryMirror.put(key, result);
                    return result;
                }
            }
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Error reading from persistent cache: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Guardar dato en cache persistente
     */
    public <T> void put(String key, T value) {
        try {
            // Guardar en memoria
            memoryMirror.put(key, value);
            
            // Guardar en disco
            Path cacheFile = cacheDir.resolve(key + ".cache");
            String json = gson.toJson(value);
            Files.writeString(cacheFile, json);
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Error writing to persistent cache: " + e.getMessage());
        }
    }
    
    /**
     * Remover dato del cache
     */
    public void remove(String key) {
        try {
            memoryMirror.remove(key);
            Path cacheFile = cacheDir.resolve(key + ".cache");
            if (Files.exists(cacheFile)) {
                Files.delete(cacheFile);
            }
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Error removing from persistent cache: " + e.getMessage());
        }
    }
    
    /**
     * Cargar cache desde disco al iniciar
     */
    private void loadCache() {
        try {
            if (!Files.exists(cacheDir)) return;
            
            Files.list(cacheDir)
                .filter(path -> path.toString().endsWith(".cache"))
                .forEach(path -> {
                    try {
                        String key = path.getFileName().toString().replace(".cache", "");
                        String json = Files.readString(path);
                        
                        // Intentar parsear como varios tipos comunes
                        Object value = tryParseJson(json);
                        if (value != null) {
                            memoryMirror.put(key, value);
                        }
                    } catch (IOException | RuntimeException e) {
                        logger.warning(() -> "Failed to load cache file " + path.getFileName() + ": " + e.getMessage());
                    }
                });
                
            logger.info(() -> "Loaded " + memoryMirror.size() + " entries from persistent cache");
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Error loading persistent cache: " + e.getMessage());
        }
    }
    
    /**
     * Intentar parsear JSON a tipos comunes
     */
    private Object tryParseJson(String json) {
        try {
            // Intentar como Map (para datos complejos)
            return gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        } catch (RuntimeException e1) {
            try {
                // Intentar como String
                return gson.fromJson(json, String.class);
            } catch (RuntimeException e2) {
                try {
                    // Intentar como Integer
                    return gson.fromJson(json, Integer.class);
                } catch (RuntimeException e3) {
                    try {
                        // Intentar como Boolean
                        return gson.fromJson(json, Boolean.class);
                    } catch (RuntimeException e4) {
                        logger.warning(() -> "Could not parse cache entry: " + json.substring(0, Math.min(50, json.length())));
                        return null;
                    }
                }
            }
        }
    }
    
    /**
     * Validar integridad del cache
     */
    public void validateIntegrity() {
        try {
            long corruptedFiles = Files.list(cacheDir)
                .filter(path -> path.toString().endsWith(".cache"))
                .mapToLong(path -> {
                    try {
                        String json = Files.readString(path);
                        gson.fromJson(json, Object.class);
                        return 0;
                    } catch (IOException | RuntimeException e) {
                        logger.warning(() -> "Corrupted cache file detected: " + path.getFileName());
                        try {
                            Files.delete(path);
                            return 1;
                        } catch (IOException ioException) {
                            logger.severe(() -> "Could not delete corrupted file: " + ioException.getMessage());
                            return 0;
                        }
                    }
                })
                .sum();
                
            if (corruptedFiles > 0) {
                logger.info(() -> "Cleaned " + corruptedFiles + " corrupted cache files");
            }
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Error validating cache integrity: " + e.getMessage());
        }
    }
    
    /**
     * Sincronizar cache con base de datos
     */
    public void syncToDatabase() {
        // Esta sería la implementación para sincronizar con la base de datos
        // Por ahora solo logueamos que se está haciendo
        logger.info("Cache synchronization would occur here");
    }
    
    /**
     * Limpiar todo el cache
     */
    public void clear() {
        try {
            memoryMirror.clear();
            if (Files.exists(cacheDir)) {
                Files.walk(cacheDir)
                    .filter(path -> path.toString().endsWith(".cache"))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            logger.warning(() -> "Could not delete cache file: " + path.getFileName());
                        }
                    });
            }
            logger.info("Persistent cache cleared");
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Error clearing persistent cache: " + e.getMessage());
        }
    }
    
    /**
     * Obtener tamaño del cache
     */
    public int getSize() {
        return memoryMirror.size();
    }
}