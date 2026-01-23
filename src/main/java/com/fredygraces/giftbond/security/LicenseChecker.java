package com.fredygraces.giftbond.security;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * Security utility class for license verification
 * This class helps protect the plugin from unauthorized use
 */
public class LicenseChecker {
    
    private static final String PLUGIN_AUTHOR = "Fredy_Graces";
    private static final String PLUGIN_NAME = "GiftBond";
    private static final String PLUGIN_VERSION = "1.1.0";
    
    /**
     * Verify plugin integrity and licensing
     * @param pluginDescription The plugin description file
     * @return true if verification passes, false otherwise
     */
    public static boolean verifyLicense(PluginDescriptionFile pluginDescription) {
        // Check author
        if (!PLUGIN_AUTHOR.equals(pluginDescription.getAuthors().get(0))) {
            Bukkit.getLogger().severe("[SECURITY] Invalid plugin author detected!");
            return false;
        }
        
        // Check plugin name
        if (!PLUGIN_NAME.equals(pluginDescription.getName())) {
            Bukkit.getLogger().severe("[SECURITY] Invalid plugin name detected!");
            return false;
        }
        
        // Check version (optional - can be commented out for flexibility)
        // if (!PLUGIN_VERSION.equals(pluginDescription.getVersion())) {
        //     Bukkit.getLogger().warning("[SECURITY] Version mismatch detected!");
        // }
        
        return true;
    }
    
    /**
     * Get security hash for plugin verification
     * @return Security hash string
     */
    public static String getSecurityHash() {
        String combined = PLUGIN_AUTHOR + PLUGIN_NAME + PLUGIN_VERSION;
        return Integer.toHexString(combined.hashCode());
    }
    
    /**
     * Log security information
     */
    public static void logSecurityInfo() {
        Bukkit.getLogger().info("[SECURITY] Plugin verification passed");
        Bukkit.getLogger().info("[SECURITY] Author: " + PLUGIN_AUTHOR);
        Bukkit.getLogger().info("[SECURITY] Protected by GPL v3 License");
    }
}