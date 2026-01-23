package com.fredygraces.giftbond.commands;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.logging.GiftBondLogger;
import com.fredygraces.giftbond.metrics.MetricsManager;
import com.fredygraces.giftbond.permissions.PermissionManager;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para mostrar métricas y estadísticas del plugin
 * /giftbond metrics [report]
 */
public class MetricsCommand implements CommandExecutor {
    
    private final GiftBond plugin;
    private final MetricsManager metricsManager;
    
    public MetricsCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.metricsManager = new MetricsManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar permisos administrativos
        if (!PermissionManager.isAdmin(sender)) {
            sender.sendMessage(plugin.getPrefix() + 
                ChatColor.translateAlternateColorCodes('&', 
                    PermissionManager.getPermissionDeniedMessage(PermissionManager.ADMIN_ALL)));
            GiftBondLogger.warn(String.format("Player '%s' attempted to use /giftbond metrics without permission", 
                sender instanceof Player ? ((Player) sender).getName() : "CONSOLE"));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "report" -> showMetricsReport(sender);
            case "reset" -> resetMetrics(sender);
            default -> {
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Subcomando desconocido: " + subcommand);
                showHelp(sender);
            }
        }
        
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "Comandos de Métricas:");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond metrics report" + ChatColor.GRAY + " - Mostrar reporte completo");
        sender.sendMessage(ChatColor.YELLOW + "/giftbond metrics reset" + ChatColor.GRAY + " - Reiniciar contadores");
    }
    
    private void showMetricsReport(CommandSender sender) {
        try {
            var report = metricsManager.generateReport();
            
            sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "=== Reporte de Métricas ===");
            sender.sendMessage(ChatColor.GREEN + "Regalos Enviados: " + ChatColor.WHITE + report.totalGiftsSent);
            sender.sendMessage(ChatColor.GREEN + "Regalos Reclamados: " + ChatColor.WHITE + report.totalGiftsRedeemed);
            sender.sendMessage(ChatColor.GREEN + "Puntos de Amistad: " + ChatColor.WHITE + report.totalPointsEarned);
            sender.sendMessage(ChatColor.GREEN + "Comandos Ejecutados: " + ChatColor.WHITE + report.totalCommandsExecuted);
            sender.sendMessage(ChatColor.GREEN + "Tiempo Promedio: " + ChatColor.WHITE + 
                String.format("%.2f ms", report.averageResponseTimeMs));
            
            if (report.peakUsageHour >= 0) {
                sender.sendMessage(ChatColor.GREEN + "Hora Pico: " + ChatColor.WHITE + 
                    String.format("%02d:00", report.peakUsageHour));
            }
            
            sender.sendMessage(ChatColor.GREEN + "Comando Más Usado: " + ChatColor.WHITE + report.mostUsedCommand);
            sender.sendMessage(ChatColor.GREEN + "Jugadores Activos: " + ChatColor.WHITE + 
                report.playerActivity.activePlayers);
            
            // Mostrar información básica de jugadores activos
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "Estadísticas de Jugadores:");
            sender.sendMessage(ChatColor.YELLOW + "• Jugadores activos: " + ChatColor.WHITE + 
                report.playerActivity.activePlayers);
            sender.sendMessage(ChatColor.YELLOW + "• Top enviadores: " + ChatColor.WHITE + 
                "(información disponible en próxima actualización)");
            
            GiftBondLogger.info("Metrics report generated for " + 
                (sender instanceof Player ? ((Player) sender).getName() : "CONSOLE"));
                
        } catch (Exception e) {
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Error generando reporte: " + e.getMessage());
            GiftBondLogger.error("Error generating metrics report", e);
        }
    }
    
    private void resetMetrics(CommandSender sender) {
        // Nota: En una implementación real, aquí se reiniciarían los contadores
        sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + 
            "⚠ Los contadores se reinician automáticamente cada 24 horas.");
        sender.sendMessage(ChatColor.GRAY + "Para efectos de demostración, los datos se mantienen.");
        
        GiftBondLogger.info("Metrics reset attempted by " + 
            (sender instanceof Player ? ((Player) sender).getName() : "CONSOLE"));
    }
}