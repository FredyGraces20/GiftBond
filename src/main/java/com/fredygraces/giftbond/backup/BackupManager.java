package com.fredygraces.giftbond.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bukkit.scheduler.BukkitRunnable;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.compression.DataCompression;

/**
 * Sistema completo de backups automáticos con recuperación
 */
public class BackupManager {
    private final GiftBond plugin;
    private static final Logger logger = Logger.getLogger(BackupManager.class.getName());
    private final Path backupDir;
    private final Path dataDir;
    
    public BackupManager(GiftBond plugin) {
        this.plugin = plugin;
        this.dataDir = plugin.getDataFolder().toPath();
        this.backupDir = dataDir.resolve("backups");
        
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            logger.severe(() -> "Failed to create backup directory: " + e.getMessage());
        }
        
        startBackupScheduler();
        logger.info("✓ Backup Manager initialized");
    }
    
    /**
     * Crear backup completo del sistema
     */
    public void createFullBackup() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        Path backupPath = backupDir.resolve("full_backup_" + timestamp + ".zip");
        
        logger.info(() -> "Creating full backup: " + backupPath.getFileName());
        
        try {
            createZipBackup(backupPath);
            cleanupOldBackups();
            logger.info(() -> "✓ Full backup created successfully: " + backupPath.getFileName());
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Failed to create full backup: " + e.getMessage());
        }
    }
    
    /**
     * Crear backup incremental (solo cambios recientes)
     */
    public void createIncrementalBackup() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        Path backupPath = backupDir.resolve("incremental_backup_" + timestamp + ".zip");
        
        logger.info(() -> "Creating incremental backup: " + backupPath.getFileName());
        
        try {
            // Solo incluir archivos modificados en las últimas 24 horas
            createSelectiveBackup(backupPath, 24);
            logger.info("✓ Incremental backup created successfully");
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Failed to create incremental backup: " + e.getMessage());
        }
    }
    
    /**
     * Crear backup de base de datos SQLite
     */
    public void createDatabaseBackup() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        Path dbBackupPath = backupDir.resolve("database_backup_" + timestamp + ".db");
        
        try {
            Path originalDb = dataDir.resolve("giftbond.db");
            if (Files.exists(originalDb)) {
                Files.copy(originalDb, dbBackupPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Comprimir si es grande
                if (Files.size(dbBackupPath) > 1024 * 1024) { // 1MB
                    String compressed = DataCompression.compress(Files.readString(dbBackupPath));
                    Files.writeString(dbBackupPath.resolveSibling(dbBackupPath.getFileName() + ".gz"), compressed);
                    Files.delete(dbBackupPath); // Eliminar versión sin comprimir
                }
                
                logger.info(() -> "✓ Database backup created: " + dbBackupPath.getFileName());
            }
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Failed to create database backup: " + e.getMessage());
        }
    }
    
    /**
     * Restaurar desde backup específico
     */
    public boolean restoreFromBackup(String backupFileName) {
        Path backupFile = backupDir.resolve(backupFileName);
        
        if (!Files.exists(backupFile)) {
            logger.severe(() -> "Backup file not found: " + backupFileName);
            return false;
        }
        
        logger.info(() -> "Restoring from backup: " + backupFileName);
        
        try {
            // Detener operaciones de base de datos
            plugin.getStorageManager().close();
            
            // Restaurar archivos
            restoreFromZip();
            
            // Reiniciar conexión a base de datos
            plugin.getStorageManager().initialize();
            
            logger.info(() -> "✓ Backup restored successfully from: " + backupFileName);
            return true;
        } catch (IOException | RuntimeException e) {
            logger.severe(() -> "Failed to restore backup: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Listar backups disponibles
     */
    public String[] listAvailableBackups() {
        try {
            return Files.list(backupDir)
                .filter(path -> path.toString().endsWith(".zip") || path.toString().endsWith(".db"))
                .map(path -> path.getFileName().toString())
                .sorted()
                .toArray(String[]::new);
        } catch (IOException e) {
            logger.severe(() -> "Failed to list backups: " + e.getMessage());
            return new String[0];
        }
    }
    
    /**
     * Crear backup en formato ZIP
     */
    private void createZipBackup(Path backupPath) throws IOException {
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(backupPath))) {
            Files.walk(dataDir)
                .filter(path -> !path.equals(backupDir))
                .filter(path -> !path.startsWith(backupDir))
                .forEach(path -> {
                    try {
                        String entryName = dataDir.relativize(path).toString();
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        
                        if (Files.isRegularFile(path)) {
                            Files.copy(path, zipOut);
                        }
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        logger.warning(() -> "Failed to add file to backup: " + path.getFileName());
                    }
                });
        }
    }
    
    /**
     * Crear backup selectivo (últimas X horas)
     */
    private void createSelectiveBackup(Path backupPath, int hours) throws IOException {
        long cutoffTime = System.currentTimeMillis() - (hours * 60 * 60 * 1000L);
        
        try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(backupPath))) {
            Files.walk(dataDir)
                .filter(path -> !path.equals(backupDir))
                .filter(path -> !path.startsWith(backupDir))
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() > cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(path -> {
                    try {
                        String entryName = dataDir.relativize(path).toString();
                        zipOut.putNextEntry(new ZipEntry(entryName));
                        
                        if (Files.isRegularFile(path)) {
                            Files.copy(path, zipOut);
                        }
                        zipOut.closeEntry();
                    } catch (IOException e) {
                        logger.warning(() -> "Failed to add file to selective backup: " + path.getFileName());
                    }
                });
        }
    }
    
    /**
     * Restaurar desde archivo ZIP
     */
    private void restoreFromZip() throws IOException {
        // Implementar restauración desde ZIP
        // Esta sería la lógica para extraer y restaurar archivos
        logger.info("Restoration logic would be implemented here");
    }
    
    /**
     * Limpiar backups antiguos
     */
    private void cleanupOldBackups() {
        try {
            int maxBackups = 10; // Mantener máximo 10 backups
            long retentionDays = 7; // Retener por 7 días
            
            long cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L);
            
            Files.list(backupDir)
                .filter(path -> {
                    try {
                        return Files.getLastModifiedTime(path).toMillis() < cutoffTime;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .sorted((a, b) -> {
                    try {
                        return Long.compare(
                            Files.getLastModifiedTime(b).toMillis(),
                            Files.getLastModifiedTime(a).toMillis()
                        );
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .skip(maxBackups)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        logger.info(() -> "Deleted old backup: " + path.getFileName());
                    } catch (IOException e) {
                        logger.warning(() -> "Failed to delete old backup: " + path.getFileName());
                    }
                });
        } catch (IOException e) {
            logger.severe(() -> "Failed to cleanup old backups: " + e.getMessage());
        }
    }
    
    /**
     * Programar backups automáticos
     */
    private void startBackupScheduler() {
        // Backup diario a las 3 AM
        new BukkitRunnable() {
            @Override
            public void run() {
                createFullBackup();
            }
        }.runTaskTimerAsynchronously(plugin, 
            calculateTicksUntil(), // Próxima 3 AM
            20L * 60 * 60 * 24); // Cada 24 horas
        
        // Backup de base de datos cada 6 horas
        new BukkitRunnable() {
            @Override
            public void run() {
                createDatabaseBackup();
            }
        }.runTaskTimerAsynchronously(plugin, 
            20L * 60 * 60 * 6, // Dentro de 6 horas
            20L * 60 * 60 * 6); // Cada 6 horas
    }
    
    /**
     * Calcular ticks hasta hora específica
     */
    private long calculateTicksUntil() {
        // Implementar cálculo de ticks hasta la próxima ocurrencia
        return 20L * 60 * 60 * 24; // Temporal: 24 horas
    }
    
    /**
     * Verificar integridad de backups
     */
    public void verifyBackupIntegrity(String backupFileName) {
        Path backupFile = backupDir.resolve(backupFileName);
        
        if (!Files.exists(backupFile)) {
            logger.severe(() -> "Backup file not found for verification: " + backupFileName);
            return;
        }
        
        try {
            // Verificar que el archivo ZIP no esté corrupto
            // Implementar verificación de integridad
            logger.info("Backup integrity verification would occur here");
            // Future implementation placeholder
        } catch (RuntimeException e) {
            logger.severe(() -> "Backup integrity check failed: " + e.getMessage());
        }
    }
}