package com.fredygraces.giftbond.commands;

import java.sql.SQLException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.storage.SynchronizationManager;
import com.fredygraces.giftbond.storage.TransactionManager;

/**
 * Comando para demostrar el sistema mejorado de guardado de datos
 * Muestra el estado de transacciones y sincronización
 * 
 * @author GiftBond Team
 * @version 1.2.0
 */
public class DataSystemCommand implements CommandExecutor {
    private final GiftBond plugin;
    
    public DataSystemCommand(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender == null || !(sender instanceof Player)) {
            if (sender != null) {
                sender.sendMessage("§cSolo jugadores pueden usar este comando");
            }
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "status" -> showSystemStatus(player);
            case "test" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUso: /datasystem test <transaction|sync>");
                    return true;
                }
                testSystem(player, args[1]);
            }
            case "health" -> showHealthReport(player);
            default -> showHelp(player);
        }
        
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6═══════════════════════════════════════");
        player.sendMessage("§eGiftBond Data System Commands");
        player.sendMessage("§6═══════════════════════════════════════");
        player.sendMessage("§a/datasystem status §7- Mostrar estado del sistema");
        player.sendMessage("§a/datasystem test transaction §7- Testear transacciones");
        player.sendMessage("§a/datasystem test sync §7- Testear sincronización");
        player.sendMessage("§a/datasystem health §7- Reporte de salud del sistema");
        player.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void showSystemStatus(Player player) {
        player.sendMessage("§6═══════════════════════════════════════");
        player.sendMessage("§eESTADO DEL SISTEMA DE DATOS");
        player.sendMessage("§6═══════════════════════════════════════");
        
        // Estado de transacciones
        TransactionManager.TransactionHealth txHealth = plugin.getTransactionManager().checkTransactionHealth();
        player.sendMessage("§aTransacciones:");
        player.sendMessage("  §7Activas: §e" + txHealth.getActiveTransactions());
        player.sendMessage("  §7Conectado: §e" + (txHealth.isDatabaseConnected() ? "✓" : "✗"));
        player.sendMessage("  §7Salud: §e" + (txHealth.isHealthy() ? "BUENA" : "PROBLEMAS"));
        
        // Estado de sincronización
        SynchronizationManager.SynchronizationStatus syncStatus = plugin.getSynchronizationManager().getStatus();
        player.sendMessage("§aSincronización:");
        player.sendMessage("  §7Locks totales: §e" + syncStatus.getTotalResourceLocks());
        player.sendMessage("  §7Locks activos: §e" + syncStatus.getActiveLocks());
        player.sendMessage("  §7Monitores: §e" + syncStatus.getMonitors());
        player.sendMessage("  §7Salud: §e" + (syncStatus.isHealthy() ? "BUENA" : "PROBLEMAS"));
        
        player.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void testSystem(Player player, String testType) {
        switch (testType.toLowerCase()) {
            case "transaction" -> testTransactionSystem(player);
            case "sync" -> testSynchronizationSystem(player);
            default -> player.sendMessage("§cTipo de test inválido. Usa: transaction o sync");
        }
    }
    
    private void testTransactionSystem(Player player) {
        player.sendMessage("§6═══════════════════════════════════════");
        player.sendMessage("§eTESTEANDO SISTEMA DE TRANSACCIONES");
        player.sendMessage("§6═══════════════════════════════════════");
        
        try {
            // Test básico de transacción
            String result = plugin.getTransactionManager().executeInTransaction(conn -> {
                player.sendMessage("§a✓ Transacción iniciada correctamente");
                return "Test completado";
            }, "test_command_transaction");
            
            player.sendMessage("§a✓ " + result);
            player.sendMessage("§a✓ Sistema de transacciones funcionando correctamente");
            
        } catch (SQLException e) {
            player.sendMessage("§c✗ Error en test de transacciones: " + e.getMessage());
        }
        
        player.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void testSynchronizationSystem(Player player) {
        player.sendMessage("§6═══════════════════════════════════════");
        player.sendMessage("§eTESTEANDO SISTEMA DE SINCRONIZACIÓN");
        player.sendMessage("§6═══════════════════════════════════════");
        
        try {
            // Test de sincronización
            String result = plugin.getSynchronizationManager().executeSynchronized("test_lock", () -> {
                player.sendMessage("§a✓ Acceso sincronizado concedido");
                return "Sincronización OK";
            });
            
            player.sendMessage("§a✓ " + result);
            player.sendMessage("§a✓ Sistema de sincronización funcionando correctamente");
            
        } catch (Exception e) {
            player.sendMessage("§c✗ Error en test de sincronización: " + e.getMessage());
        }
        
        player.sendMessage("§6═══════════════════════════════════════");
    }
    
    private void showHealthReport(Player player) {
        player.sendMessage("§6═══════════════════════════════════════");
        player.sendMessage("§eREPORTE DE SALUD DEL SISTEMA");
        player.sendMessage("§6═══════════════════════════════════════");
        
        // Verificar componentes críticos
        boolean databaseOK = false;
        boolean transactionOK = false;
        boolean syncOK = false;
        
        try {
            // Test de conexión a base de datos
            if (plugin.getDatabaseManager().getConnection() != null) {
                databaseOK = true;
                player.sendMessage("§a✓ Base de datos: CONECTADA");
            } else {
                player.sendMessage("§c✗ Base de datos: DESCONECTADA");
            }
        } catch (Exception e) {
            player.sendMessage("§c✗ Base de datos: ERROR - " + e.getMessage());
        }
        
        try {
            // Test de transacciones
            TransactionManager.TransactionHealth txHealth = plugin.getTransactionManager().checkTransactionHealth();
            if (txHealth.isHealthy()) {
                transactionOK = true;
                player.sendMessage("§a✓ Transacciones: SALUDABLE");
            } else {
                player.sendMessage("§c✗ Transacciones: PROBLEMAS DETECTADOS");
            }
        } catch (Exception e) {
            player.sendMessage("§c✗ Transacciones: ERROR - " + e.getMessage());
        }
        
        try {
            // Test de sincronización
            SynchronizationManager.SynchronizationStatus syncStatus = plugin.getSynchronizationManager().getStatus();
            if (syncStatus.isHealthy()) {
                syncOK = true;
                player.sendMessage("§a✓ Sincronización: SALUDABLE");
            } else {
                player.sendMessage("§c✗ Sincronización: PROBLEMAS DETECTADOS");
            }
        } catch (Exception e) {
            player.sendMessage("§c✗ Sincronización: ERROR - " + e.getMessage());
        }
        
        // Resumen general
        player.sendMessage("§6───────────────────────────────────────");
        if (databaseOK && transactionOK && syncOK) {
            player.sendMessage("§a✓ ESTADO GENERAL: TODOS LOS SISTEMAS OPERATIVOS");
            player.sendMessage("§a  El sistema de guardado de datos está funcionando correctamente");
        } else {
            player.sendMessage("§c✗ ESTADO GENERAL: PROBLEMAS DETECTADOS");
            player.sendMessage("§c  Se recomienda revisar los logs del servidor");
        }
        player.sendMessage("§6═══════════════════════════════════════");
    }
}