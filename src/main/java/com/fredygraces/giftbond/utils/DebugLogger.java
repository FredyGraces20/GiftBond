package com.fredygraces.giftbond.utils;

import java.util.logging.Logger;

import com.fredygraces.giftbond.GiftBond;

/**
 * Utilidad para logging condicional basado en configuración
 * Solo imprime mensajes de debug si está activado en config.yml
 */
public class DebugLogger {
    private final GiftBond plugin;
    private static final Logger logger = Logger.getLogger(DebugLogger.class.getName());
    
    public DebugLogger(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Imprime mensaje de debug solo si está activado en la configuración
     * @param message Mensaje a imprimir
     */
    public void debug(String message) {
        if (isDebugEnabled()) {
            logger.info(() -> "[DEBUG] " + message);
        }
    }
    
    /**
     * Imprime mensaje de warning solo si está activado en la configuración
     * @param message Mensaje a imprimir
     */
    public void debugWarning(String message) {
        if (isDebugEnabled()) {
            logger.warning(() -> "[DEBUG] " + message);
        }
    }
    
    /**
     * Imprime mensaje de error solo si está activado en la configuración
     * @param message Mensaje a imprimir
     */
    public void debugSevere(String message) {
        if (isDebugEnabled()) {
            logger.severe(() -> "[DEBUG] " + message);
        }
    }
    
    /**
     * Imprime mensaje de fine (nivel más bajo) solo si está activado
     * @param message Mensaje a imprimir
     */
    public void debugFine(String message) {
        if (isDebugEnabled()) {
            logger.fine(() -> "[DEBUG] " + message);
        }
    }
    
    /**
     * Verifica si el modo debug está activado en la configuración
     * @return true si debug está activado
     */
    private boolean isDebugEnabled() {
        try {
            return plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
        } catch (Exception e) {
            // Si hay error accediendo a la configuración, asumir debug desactivado
            return false;
        }
    }
    
    /**
     * Imprime mensaje siempre (independientemente del modo debug)
     * @param message Mensaje a imprimir
     */
    public void info(String message) {
        logger.info(message);
    }
    
    /**
     * Imprime mensaje de warning siempre
     * @param message Mensaje a imprimir
     */
    public void warning(String message) {
        logger.warning(message);
    }
    
    /**
     * Imprime mensaje de error siempre
     * @param message Mensaje a imprimir
     */
    public void severe(String message) {
        logger.severe(message);
    }
}