package com.fredygraces.giftbond.permissions;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Sistema avanzado de gestión de permisos para GiftBond
 * Proporciona control granular de acceso a funcionalidades
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class PermissionManager {
    
    // Permisos base del plugin
    public static final String BASE_PERMISSION = "giftbond";
    
    // Permisos para comandos principales
    public static final String COMMAND_SEND = BASE_PERMISSION + ".send";
    public static final String COMMAND_REDEEM = BASE_PERMISSION + ".redeem";
    public static final String COMMAND_AMISTAD = BASE_PERMISSION + ".amistad";
    public static final String COMMAND_BOOST = BASE_PERMISSION + ".boost";
    public static final String COMMAND_HISTORY = BASE_PERMISSION + ".history";
    public static final String COMMAND_TOP = BASE_PERMISSION + ".top";
    public static final String COMMAND_MAILBOX = BASE_PERMISSION + ".mailbox";
    
    // Permisos administrativos
    public static final String ADMIN_RELOAD = BASE_PERMISSION + ".admin.reload";
    public static final String ADMIN_SAVE = BASE_PERMISSION + ".admin.save";
    public static final String ADMIN_DEBUG = BASE_PERMISSION + ".admin.debug";
    public static final String ADMIN_ALL = BASE_PERMISSION + ".admin.*";
    
    // Permisos especiales
    public static final String BYPASS_LIMITS = BASE_PERMISSION + ".bypass.limits";
    public static final String BYPASS_COOLDOWN = BASE_PERMISSION + ".bypass.cooldown";
    public static final String PREMIUM_FEATURES = BASE_PERMISSION + ".premium";
    
    /**
     * Verifica si un jugador tiene un permiso específico
     * 
     * @param sender El remitente del comando
     * @param permission El permiso a verificar
     * @return true si tiene el permiso, false en caso contrario
     */
    public static boolean hasPermission(CommandSender sender, String permission) {
        if (!(sender instanceof Player)) {
            // Consola tiene todos los permisos
            return true;
        }
        
        Player player = (Player) sender;
        return player.hasPermission(permission) || player.hasPermission(ADMIN_ALL);
    }
    
    /**
     * Verifica si un jugador tiene permiso para enviar regalos
     * 
     * @param sender El remitente del comando
     * @return true si puede enviar regalos
     */
    public static boolean canSendGifts(CommandSender sender) {
        return hasPermission(sender, COMMAND_SEND);
    }
    
    /**
     * Verifica si un jugador puede reclamar regalos
     * 
     * @param sender El remitente del comando
     * @return true si puede reclamar regalos
     */
    public static boolean canRedeemGifts(CommandSender sender) {
        return hasPermission(sender, COMMAND_REDEEM);
    }
    
    /**
     * Verifica si un jugador puede usar comandos de amistad
     * 
     * @param sender El remitente del comando
     * @return true si puede usar comandos de amistad
     */
    public static boolean canUseFriendshipCommands(CommandSender sender) {
        return hasPermission(sender, COMMAND_AMISTAD);
    }
    
    /**
     * Verifica si un jugador tiene permisos administrativos
     * 
     * @param sender El remitente del comando
     * @return true si es administrador
     */
    public static boolean isAdmin(CommandSender sender) {
        return hasPermission(sender, ADMIN_ALL) || 
               hasPermission(sender, ADMIN_RELOAD) ||
               hasPermission(sender, ADMIN_SAVE) ||
               hasPermission(sender, ADMIN_DEBUG);
    }
    
    /**
     * Verifica si un jugador puede bypassear límites
     * 
     * @param sender El remitente del comando
     * @return true si puede bypassear límites
     */
    public static boolean canBypassLimits(CommandSender sender) {
        return hasPermission(sender, BYPASS_LIMITS);
    }
    
    /**
     * Verifica si un jugador puede bypassear cooldowns
     * 
     * @param sender El remitente del comando
     * @return true si puede bypassear cooldowns
     */
    public static boolean canBypassCooldown(CommandSender sender) {
        return hasPermission(sender, BYPASS_COOLDOWN);
    }
    
    /**
     * Verifica si un jugador tiene acceso a características premium
     * 
     * @param sender El remitente del comando
     * @return true si tiene acceso premium
     */
    public static boolean hasPremiumAccess(CommandSender sender) {
        return hasPermission(sender, PREMIUM_FEATURES);
    }
    
    /**
     * Obtiene el mensaje de falta de permisos
     * 
     * @param permission El permiso requerido
     * @return Mensaje de error formateado
     */
    public static String getPermissionDeniedMessage(String permission) {
        return "§cNo tienes permiso para usar este comando. Se requiere: §e" + permission;
    }
}