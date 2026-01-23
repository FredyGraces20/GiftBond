package com.fredygraces.giftbond.metrics;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.logging.GiftBondLogger;

/**
 * Sistema avanzado de métricas y estadísticas para GiftBond
 * Rastrea uso, performance y estadísticas del plugin
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class MetricsManager {
    
    private final GiftBond plugin;
    
    // Contadores de uso
    private final AtomicInteger totalGiftsSent = new AtomicInteger(0);
    private final AtomicInteger totalGiftsRedeemed = new AtomicInteger(0);
    private final AtomicInteger totalFriendshipPointsEarned = new AtomicInteger(0);
    private final AtomicInteger totalCommandsExecuted = new AtomicInteger(0);
    
    // Tiempos de respuesta
    private final AtomicLong totalResponseTime = new AtomicLong(0);
    private final AtomicInteger responseTimeSamples = new AtomicInteger(0);
    
    // Uso por comando
    private final ConcurrentHashMap<String, AtomicInteger> commandUsage = new ConcurrentHashMap<>();
    
    // Horas pico de uso
    private final ConcurrentHashMap<Integer, AtomicInteger> hourlyUsage = new ConcurrentHashMap<>();
    
    // Jugadores activos
    private final ConcurrentHashMap<String, PlayerStats> playerStats = new ConcurrentHashMap<>();
    
    public MetricsManager(GiftBond plugin) {
        this.plugin = plugin;
        startHourlyTracker();
    }
    
    /**
     * Registra el envío de un regalo
     */
    public void recordGiftSent(String sender, String receiver, int points) {
        totalGiftsSent.incrementAndGet();
        totalFriendshipPointsEarned.addAndGet(points);
        
        // Actualizar estadísticas del jugador
        getPlayerStats(sender).giftsSent.incrementAndGet();
        getPlayerStats(receiver).giftsReceived.incrementAndGet();
        getPlayerStats(receiver).pointsReceived.addAndGet(points);
        
        GiftBondLogger.debug(String.format("Gift sent: %s -> %s (%d points)", sender, receiver, points));
    }
    
    /**
     * Registra el reclamo de un regalo
     */
    public void recordGiftRedeemed(String redeemer, int points, int itemCount) {
        totalGiftsRedeemed.incrementAndGet();
        
        getPlayerStats(redeemer).giftsRedeemed.incrementAndGet();
        getPlayerStats(redeemer).itemsReceived.addAndGet(itemCount);
        
        GiftBondLogger.debug(String.format("Gift redeemed by %s: %d points, %d items", 
            redeemer, points, itemCount));
    }
    
    /**
     * Registra ejecución de comando
     */
    public void recordCommandExecution(String command, long responseTimeMs) {
        totalCommandsExecuted.incrementAndGet();
        commandUsage.computeIfAbsent(command, k -> new AtomicInteger(0)).incrementAndGet();
        
        // Registrar tiempo de respuesta
        totalResponseTime.addAndGet(responseTimeMs);
        responseTimeSamples.incrementAndGet();
        
        GiftBondLogger.debug(String.format("Command executed: %s in %d ms", command, responseTimeMs));
    }
    
    /**
     * Registra uso por hora
     */
    private void recordHourlyUsage() {
        int currentHour = LocalDateTime.now().getHour();
        hourlyUsage.computeIfAbsent(currentHour, k -> new AtomicInteger(0)).incrementAndGet();
    }
    
    /**
     * Inicia el tracker horario
     */
    private void startHourlyTracker() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::recordHourlyUsage, 0L, 72000L); // Cada hora (72000 ticks)
    }
    
    /**
     * Obtiene estadísticas de un jugador
     */
    private PlayerStats getPlayerStats(String playerName) {
        return playerStats.computeIfAbsent(playerName, k -> new PlayerStats(playerName));
    }
    
    /**
     * Genera reporte de métricas
     */
    public MetricsReport generateReport() {
        return new MetricsReport(
            totalGiftsSent.get(),
            totalGiftsRedeemed.get(),
            totalFriendshipPointsEarned.get(),
            totalCommandsExecuted.get(),
            getAverageResponseTime(),
            getPeakUsageHour(),
            getMostUsedCommand(),
            getPlayerActivityReport()
        );
    }
    
    /**
     * Calcula tiempo promedio de respuesta
     */
    private double getAverageResponseTime() {
        int samples = responseTimeSamples.get();
        return samples > 0 ? (double) totalResponseTime.get() / samples : 0.0;
    }
    
    /**
     * Obtiene la hora pico de uso
     */
    private int getPeakUsageHour() {
        return hourlyUsage.entrySet().stream()
            .max((e1, e2) -> Integer.compare(e1.getValue().get(), e2.getValue().get()))
            .map(entry -> entry.getKey())
            .orElse(-1);
    }
    
    /**
     * Obtiene el comando más usado
     */
    private String getMostUsedCommand() {
        return commandUsage.entrySet().stream()
            .max((e1, e2) -> Integer.compare(e1.getValue().get(), e2.getValue().get()))
            .map(entry -> entry.getKey())
            .orElse("None");
    }
    
    /**
     * Genera reporte de actividad de jugadores
     */
    private PlayerActivityReport getPlayerActivityReport() {
        int activePlayers = (int) playerStats.values().stream()
            .filter(stats -> stats.giftsSent.get() > 0 || stats.giftsReceived.get() > 0)
            .count();
            
        int topSenders = Math.min(5, playerStats.size());
        var topSenderNames = playerStats.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().giftsSent.get(), e1.getValue().giftsSent.get()))
            .limit(topSenders)
            .map(Map.Entry::getKey)
            .toList();
            
        return new PlayerActivityReport(activePlayers, topSenderNames);
    }
    
    /**
     * Clase para estadísticas de jugador
     */
    private static class PlayerStats {
        final AtomicInteger giftsSent = new AtomicInteger(0);
        final AtomicInteger giftsReceived = new AtomicInteger(0);
        final AtomicInteger giftsRedeemed = new AtomicInteger(0);
        final AtomicInteger pointsReceived = new AtomicInteger(0);
        final AtomicInteger itemsReceived = new AtomicInteger(0);
        
        PlayerStats(String playerName) {
            // playerName se usa para identificación interna
        }
    }
    
    /**
     * Reporte de métricas completo
     */
    public static class MetricsReport {
        public final int totalGiftsSent;
        public final int totalGiftsRedeemed;
        public final int totalPointsEarned;
        public final int totalCommandsExecuted;
        public final double averageResponseTimeMs;
        public final int peakUsageHour;
        public final String mostUsedCommand;
        public final PlayerActivityReport playerActivity;
        
        MetricsReport(int totalGiftsSent, int totalGiftsRedeemed, int totalPointsEarned,
                     int totalCommandsExecuted, double averageResponseTimeMs,
                     int peakUsageHour, String mostUsedCommand,
                     PlayerActivityReport playerActivity) {
            this.totalGiftsSent = totalGiftsSent;
            this.totalGiftsRedeemed = totalGiftsRedeemed;
            this.totalPointsEarned = totalPointsEarned;
            this.totalCommandsExecuted = totalCommandsExecuted;
            this.averageResponseTimeMs = averageResponseTimeMs;
            this.peakUsageHour = peakUsageHour;
            this.mostUsedCommand = mostUsedCommand;
            this.playerActivity = playerActivity;
        }
        
        @Override
        public String toString() {
            return String.format("""
                === GiftBond Metrics Report ===
                Total Gifts Sent: %d
                Total Gifts Redeemed: %d
                Total Points Earned: %d
                Total Commands Executed: %d
                Average Response Time: %.2f ms
                Peak Usage Hour: %d:00
                Most Used Command: %s
                Active Players: %d
                """, totalGiftsSent, totalGiftsRedeemed, totalPointsEarned,
                totalCommandsExecuted, averageResponseTimeMs, peakUsageHour,
                mostUsedCommand, playerActivity.activePlayers);
        }
    }
    
    /**
     * Reporte de actividad de jugadores
     */
    public static class PlayerActivityReport {
        public final int activePlayers;
        public final java.util.List<String> topSenderNames;
        
        PlayerActivityReport(int activePlayers, java.util.List<String> topSenderNames) {
            this.activePlayers = activePlayers;
            this.topSenderNames = topSenderNames;
        }
    }
}