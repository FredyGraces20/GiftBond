package com.fredygraces.giftbond.health;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.logging.GiftBondLogger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Sistema de health checks simplificado para GiftBond
 * Monitorea el estado del plugin y sus dependencias
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class HealthCheckManager {
    
    private final GiftBond plugin;
    
    public HealthCheckManager(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Realiza todos los health checks
     */
    public CompletableFuture<HealthReport> performFullHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            List<HealthCheckResult> results = new ArrayList<>();
            
            // Verificar estado del plugin
            results.add(checkPluginStatus());
            
            // Verificar archivos de configuración
            results.add(checkConfigFiles());
            
            // Verificar permisos
            results.add(checkPermissions());
            
            // Verificar memoria
            results.add(checkMemoryUsage());
            
            // Verificar threads
            results.add(checkThreadPool());
            
            return new HealthReport(results);
        });
    }
    
    /**
     * Verifica el estado básico del plugin
     */
    private HealthCheckResult checkPluginStatus() {
        try {
            boolean enabled = plugin.isEnabled();
            boolean initialized = plugin.getGiftManager() != null;
            
            HealthStatus status = (enabled && initialized) ? HealthStatus.HEALTHY : HealthStatus.CRITICAL;
            String message = enabled ? 
                (initialized ? "Plugin is fully operational" : "Plugin enabled but not fully initialized") :
                "Plugin is disabled";
                
            return new HealthCheckResult("Plugin Status", status, message);
            
        } catch (Exception e) {
            GiftBondLogger.error("Error checking plugin status", e);
            return new HealthCheckResult("Plugin Status", HealthStatus.CRITICAL, 
                "Failed to check plugin status: " + e.getMessage());
        }
    }
    
    /**
     * Verifica archivos de configuración
     */
    private HealthCheckResult checkConfigFiles() {
        List<String> missingFiles = new ArrayList<>();
        List<String> configFileNames = List.of("config.yml", "messages.yml", "gifts.yml", "database.yml");
        
        for (String fileName : configFileNames) {
            File configFile = new File(plugin.getDataFolder(), fileName);
            if (!configFile.exists()) {
                missingFiles.add(fileName);
            }
        }
        
        if (missingFiles.isEmpty()) {
            return new HealthCheckResult("Config Files", HealthStatus.HEALTHY, 
                "All configuration files present");
        } else {
            return new HealthCheckResult("Config Files", HealthStatus.WARNING, 
                "Missing config files: " + String.join(", ", missingFiles));
        }
    }
    
    /**
     * Verifica permisos del plugin
     */
    private HealthCheckResult checkPermissions() {
        try {
            // Verificar permisos básicos
            String[] requiredPerms = {
                "giftbond.send", "giftbond.redeem", "giftbond.amistad"
            };
            
            List<String> missingPerms = new ArrayList<>();
            
            for (String perm : requiredPerms) {
                if (plugin.getServer().getPluginManager().getPermission(perm) == null) {
                    missingPerms.add(perm);
                }
            }
            
            if (missingPerms.isEmpty()) {
                return new HealthCheckResult("Permissions", HealthStatus.HEALTHY, 
                    "All required permissions registered");
            } else {
                return new HealthCheckResult("Permissions", HealthStatus.WARNING, 
                    "Missing permissions: " + String.join(", ", missingPerms));
            }
            
        } catch (Exception e) {
            GiftBondLogger.error("Error checking permissions", e);
            return new HealthCheckResult("Permissions", HealthStatus.WARNING, 
                "Error checking permissions: " + e.getMessage());
        }
    }
    
    /**
     * Verifica uso de memoria
     */
    private HealthCheckResult checkMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usagePercentage = (double) usedMemory / maxMemory * 100;
            
            HealthStatus status;
            String message;
            
            if (usagePercentage > 90) {
                status = HealthStatus.CRITICAL;
                message = String.format("High memory usage: %.1f%% (%d MB used of %d MB max)", 
                    usagePercentage, usedMemory / 1024 / 1024, maxMemory / 1024 / 1024);
            } else if (usagePercentage > 75) {
                status = HealthStatus.WARNING;
                message = String.format("Moderate memory usage: %.1f%%", usagePercentage);
            } else {
                status = HealthStatus.HEALTHY;
                message = String.format("Memory usage normal: %.1f%%", usagePercentage);
            }
            
            return new HealthCheckResult("Memory Usage", status, message);
            
        } catch (Exception e) {
            GiftBondLogger.error("Error checking memory usage", e);
            return new HealthCheckResult("Memory Usage", HealthStatus.WARNING, 
                "Error checking memory: " + e.getMessage());
        }
    }
    
    /**
     * Verifica el estado del thread pool
     */
    private HealthCheckResult checkThreadPool() {
        try {
            ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
            
            if (threadGroup != null) {
                Thread[] threads = new Thread[threadGroup.activeCount() * 2];
                int threadCount = threadGroup.enumerate(threads, true);
                
                if (threadCount > 50) {
                    return new HealthCheckResult("Thread Pool", HealthStatus.WARNING, 
                        String.format("High thread count: %d threads", threadCount));
                } else {
                    return new HealthCheckResult("Thread Pool", HealthStatus.HEALTHY, 
                        String.format("Thread count normal: %d threads", threadCount));
                }
            } else {
                return new HealthCheckResult("Thread Pool", HealthStatus.WARNING, 
                    "Could not access thread group information");
            }
            
        } catch (Exception e) {
            GiftBondLogger.error("Error checking thread pool", e);
            return new HealthCheckResult("Thread Pool", HealthStatus.WARNING, 
                "Error checking threads: " + e.getMessage());
        }
    }
    
    /**
     * Estados de salud
     */
    public enum HealthStatus {
        HEALTHY("✓ Healthy"),
        WARNING("⚠ Warning"),
        CRITICAL("✗ Critical");
        
        private final String displayName;
        
        HealthStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Resultado individual de health check
     */
    public static class HealthCheckResult {
        public final String checkName;
        public final HealthStatus status;
        public final String message;
        
        public HealthCheckResult(String checkName, HealthStatus status, String message) {
            this.checkName = checkName;
            this.status = status;
            this.message = message;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s: %s", status.getDisplayName(), checkName, message);
        }
    }
    
    /**
     * Reporte completo de salud
     */
    public static class HealthReport {
        public final List<HealthCheckResult> results;
        public final HealthStatus overallStatus;
        public final String timestamp;
        
        public HealthReport(List<HealthCheckResult> results) {
            this.results = results;
            this.overallStatus = calculateOverallStatus(results);
            this.timestamp = java.time.LocalDateTime.now().toString();
        }
        
        private HealthStatus calculateOverallStatus(List<HealthCheckResult> results) {
            boolean hasCritical = results.stream().anyMatch(r -> r.status == HealthStatus.CRITICAL);
            boolean hasWarning = results.stream().anyMatch(r -> r.status == HealthStatus.WARNING);
            
            if (hasCritical) return HealthStatus.CRITICAL;
            if (hasWarning) return HealthStatus.WARNING;
            return HealthStatus.HEALTHY;
        }
        
        public String getFormattedReport() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== GiftBond Health Check Report ===\n");
            sb.append("Timestamp: ").append(timestamp).append("\n");
            sb.append("Overall Status: ").append(overallStatus.getDisplayName()).append("\n\n");
            
            for (HealthCheckResult result : results) {
                sb.append(result.toString()).append("\n");
            }
            
            return sb.toString();
        }
    }
}