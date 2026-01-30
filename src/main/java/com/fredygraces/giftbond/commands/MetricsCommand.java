package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.logging.GiftBondLogger;
import com.fredygraces.giftbond.metrics.MetricsManager;
import com.fredygraces.giftbond.permissions.PermissionManager;

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
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.unknown_subcommand", "{prefix}&cSubcomando desconocido: {subcommand}")
                        .replace("{subcommand}", subcommand)));
                showHelp(sender);
            }
        }
        
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.help_header", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_help_title", "&eComandos de Métricas:")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_help_report", "&e/giftbond metrics report &7- Mostrar reporte completo")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_help_reset", "&e/giftbond metrics reset &7- Reiniciar contadores")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.help_footer", "&d&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }
    
    private void showMetricsReport(CommandSender sender) {
        try {
            var report = metricsManager.generateReport();
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_report_header", "{prefix}&6=== Reporte de Métricas ===")));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_gifts_sent", "&aRegalos Enviados: &f{count}")
                    .replace("{count}", String.valueOf(report.totalGiftsSent))));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_gifts_redeemed", "&aRegalos Reclamados: &f{count}")
                    .replace("{count}", String.valueOf(report.totalGiftsRedeemed))));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_points_earned", "&aPuntos de Amistad: &f{count}")
                    .replace("{count}", String.valueOf(report.totalPointsEarned))));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_commands_executed", "&aComandos Ejecutados: &f{count}")
                    .replace("{count}", String.valueOf(report.totalCommandsExecuted))));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_average_time", "&aTiempo Promedio: &f{time} ms")
                    .replace("{time}", String.format("%.2f", report.averageResponseTimeMs))));
            
            if (report.peakUsageHour >= 0) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_peak_hour", "&aHora Pico: &f{hour}")
                        .replace("{hour}", String.format("%02d:00", report.peakUsageHour))));
            }
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_most_used_command", "&aComando Más Usado: &f{command}")
                    .replace("{command}", report.mostUsedCommand)));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_active_players", "&aJugadores Activos: &f{count}")
                    .replace("{count}", String.valueOf(report.playerActivity.activePlayers))));
            
            // Mostrar información básica de jugadores activos
            sender.sendMessage("");
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_player_stats_title", "&6Estadísticas de Jugadores:")));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_active_players_count", "&e• Jugadores activos: &f{count}")
                    .replace("{count}", String.valueOf(report.playerActivity.activePlayers))));
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_top_senders_title", "&e• Top enviadores: &7(próximamente)")));
            
            GiftBondLogger.info("Metrics report generated for " + 
                (sender instanceof Player ? ((Player) sender).getName() : "CONSOLE"));
                
        } catch (Exception e) {
            String errorMsg = plugin.getMessage("commands.metrics_error", "{prefix}&cError generando reporte: {error}")
                    .replace("{error}", e.getMessage());
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', errorMsg));
            GiftBondLogger.error("Error generating metrics report", e);
        }
    }
    
    private void resetMetrics(CommandSender sender) {
        // Nota: En una implementación real, aquí se reiniciarían los contadores
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_reset_warning", "{prefix}&e⚠ Los contadores se reinician automáticamente cada 24 horas.")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.metrics_reset_demo", "&7Para efectos de demostración, los datos se mantienen.")));
        
        GiftBondLogger.info("Metrics reset attempted by " + 
            (sender instanceof Player ? ((Player) sender).getName() : "CONSOLE"));
    }
}