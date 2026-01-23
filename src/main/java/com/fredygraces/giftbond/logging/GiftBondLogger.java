package com.fredygraces.giftbond.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * Sistema avanzado de logging para GiftBond
 * Proporciona registro estructurado y categorizado de eventos
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class GiftBondLogger {
    
    private static final Logger LOGGER = Bukkit.getLogger();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Niveles de log personalizados
    public enum LogLevel {
        DEBUG("DEBUG", ChatColor.GRAY),
        INFO("INFO", ChatColor.GREEN),
        WARN("WARN", ChatColor.YELLOW),
        ERROR("ERROR", ChatColor.RED),
        SECURITY("SECURITY", ChatColor.DARK_RED);
        
        private final String prefix;
        private final ChatColor color;
        
        LogLevel(String prefix, ChatColor color) {
            this.prefix = prefix;
            this.color = color;
        }
        
        public String getPrefix() {
            return prefix;
        }
        
        public ChatColor getColor() {
            return color;
        }
    }
    
    /**
     * Loggea un mensaje con nivel DEBUG
     * 
     * @param message El mensaje a loggear
     */
    public static void debug(String message) {
        log(LogLevel.DEBUG, message, null);
    }
    
    /**
     * Loggea un mensaje con nivel DEBUG y excepción
     * 
     * @param message El mensaje a loggear
     * @param throwable La excepción asociada
     */
    public static void debug(String message, Throwable throwable) {
        log(LogLevel.DEBUG, message, throwable);
    }
    
    /**
     * Loggea un mensaje con nivel INFO
     * 
     * @param message El mensaje a loggear
     */
    public static void info(String message) {
        log(LogLevel.INFO, message, null);
    }
    
    /**
     * Loggea un mensaje con nivel WARN
     * 
     * @param message El mensaje a loggear
     */
    public static void warn(String message) {
        log(LogLevel.WARN, message, null);
    }
    
    /**
     * Loggea un mensaje con nivel WARN y excepción
     * 
     * @param message El mensaje a loggear
     * @param throwable La excepción asociada
     */
    public static void warn(String message, Throwable throwable) {
        log(LogLevel.WARN, message, throwable);
    }
    
    /**
     * Loggea un mensaje con nivel ERROR
     * 
     * @param message El mensaje a loggear
     */
    public static void error(String message) {
        log(LogLevel.ERROR, message, null);
    }
    
    /**
     * Loggea un mensaje con nivel ERROR y excepción
     * 
     * @param message El mensaje a loggear
     * @param throwable La excepción asociada
     */
    public static void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    /**
     * Loggea un mensaje de seguridad
     * 
     * @param message El mensaje a loggear
     */
    public static void security(String message) {
        log(LogLevel.SECURITY, message, null);
    }
    
    /**
     * Loggea un mensaje de seguridad con excepción
     * 
     * @param message El mensaje a loggear
     * @param throwable La excepción asociada
     */
    public static void security(String message, Throwable throwable) {
        log(LogLevel.SECURITY, message, throwable);
    }
    
    /**
     * Método principal de logging
     * 
     * @param level El nivel del log
     * @param message El mensaje
     * @param throwable La excepción (opcional)
     */
    private static void log(LogLevel level, String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String formattedMessage = String.format("[%s] [%s] %s", 
            timestamp, level.getPrefix(), message);
        
        // Loggear al logger de Bukkit
        switch (level) {
            case DEBUG, INFO -> LOGGER.info(formattedMessage);
            case WARN -> LOGGER.warning(formattedMessage);
            case ERROR, SECURITY -> LOGGER.severe(formattedMessage);
        }
        
        // Si hay excepción, loggear el stack trace
        if (throwable != null) {
            LOGGER.log(Level.SEVERE, "Exception details:", throwable);
        }
    }
    
    /**
     * Loggea eventos de comandos
     * 
     * @param playerName Nombre del jugador
     * @param command Comando ejecutado
     * @param success Si el comando fue exitoso
     */
    public static void logCommand(String playerName, String command, boolean success) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Player '%s' executed command '%s' - Status: %s", 
            playerName, command, status);
        info(message);
    }
    
    /**
     * Loggea eventos de regalos
     * 
     * @param sender Nombre del remitente
     * @param receiver Nombre del receptor
     * @param giftName Nombre del regalo
     * @param points Puntos otorgados
     */
    public static void logGiftEvent(String sender, String receiver, String giftName, int points) {
        String message = String.format("Gift sent from '%s' to '%s' - Gift: %s - Points: %d", 
            sender, receiver, giftName, points);
        info(message);
    }
    
    /**
     * Loggea eventos de seguridad
     * 
     * @param eventType Tipo de evento de seguridad
     * @param details Detalles del evento
     */
    public static void logSecurityEvent(String eventType, String details) {
        String message = String.format("SECURITY EVENT: %s - Details: %s", eventType, details);
        security(message);
    }
}