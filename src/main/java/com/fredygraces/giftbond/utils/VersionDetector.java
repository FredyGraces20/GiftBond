package com.fredygraces.giftbond.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

import com.fredygraces.giftbond.GiftBond;

/**
 * Detector de versión del servidor Minecraft
 * Detecta automáticamente la versión de Minecraft que está corriendo el servidor
 */
public class VersionDetector {
    
    private final String serverVersion;
    private final int majorVersion;
    private final int minorVersion;
    private final int patchVersion;
    
    public VersionDetector(GiftBond plugin) {
        
        // Primero, verificar si hay una versión manual específica en la configuración
        // Esto permite al usuario elegir una versión específica independientemente de la versión del servidor
        String forcedVersion = plugin.getConfigManager().getGiftsConfig().getString("force_selected_version", "");
                
        if (forcedVersion != null && !forcedVersion.isEmpty()) {
            // Usar la versión forzada por el usuario
            this.serverVersion = forcedVersion;
            String[] parts = forcedVersion.split("\\.");
            this.majorVersion = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
            this.minorVersion = parts.length > 1 ? Integer.parseInt(parts[1]) : 21;
            this.patchVersion = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                    
            plugin.getLogger().info("Versión forzada por configuración: " + serverVersion);
        } else {
            // Detectar versión del servidor normalmente
            // Formato típico: "git-Paper-123 (MC: 1.21.4)" o "CraftBukkit version git-Spigot-xyz (MC: 1.20.1)"
            String fullVersion = Bukkit.getVersion();
                    
            // Intentar extraer versión con regex
            Pattern pattern = Pattern.compile("MC:\\s*(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
            Matcher matcher = pattern.matcher(fullVersion);
                    
            if (matcher.find()) {
                this.majorVersion = Integer.parseInt(matcher.group(1));
                this.minorVersion = Integer.parseInt(matcher.group(2));
                this.patchVersion = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
                this.serverVersion = majorVersion + "." + minorVersion + (patchVersion > 0 ? "." + patchVersion : "");
            } else {
                // Fallback: intentar con Bukkit.getBukkitVersion()
                String bukkitVersion = Bukkit.getBukkitVersion();
                Pattern fallbackPattern = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");
                Matcher fallbackMatcher = fallbackPattern.matcher(bukkitVersion);
                        
                if (fallbackMatcher.find()) {
                    this.majorVersion = Integer.parseInt(fallbackMatcher.group(1));
                    this.minorVersion = Integer.parseInt(fallbackMatcher.group(2));
                    this.patchVersion = fallbackMatcher.group(3) != null ? Integer.parseInt(fallbackMatcher.group(3)) : 0;
                    this.serverVersion = majorVersion + "." + minorVersion + (patchVersion > 0 ? "." + patchVersion : "");
                } else {
                    // Último fallback: usar versión configurada manualmente
                    String manualVersion = plugin.getConfigManager().getGiftsConfig().getString("auto_mode.force_version", "1.21");
                    this.serverVersion = manualVersion != null ? manualVersion : "1.21";
                    String[] parts = this.serverVersion.split("\\.");
                    this.majorVersion = parts.length > 0 ? Integer.parseInt(parts[0]) : 1;
                    this.minorVersion = parts.length > 1 ? Integer.parseInt(parts[1]) : 21;
                    this.patchVersion = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                            
                    plugin.getLogger().warning("No se pudo detectar la versión automáticamente. Usando versión manual: " + serverVersion);
                }
            }
        }
        
        plugin.getLogger().info("═══════════════════════════════════════");
        plugin.getLogger().info("  Versión de Minecraft detectada: " + serverVersion);
        plugin.getLogger().info("  Versión completa del servidor:");
        String fullVersion = Bukkit.getVersion();
        plugin.getLogger().info("  " + fullVersion);
        plugin.getLogger().info("═══════════════════════════════════════");
    }
    
    /**
     * Obtiene la versión del servidor en formato string (ej: "1.21.4")
     */
    public String getVersion() {
        return serverVersion;
    }
    
    /**
     * Obtiene la versión mayor (ej: 1)
     */
    public int getMajorVersion() {
        return majorVersion;
    }
    
    /**
     * Obtiene la versión menor (ej: 21)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Obtiene la versión de parche (ej: 4)
     */
    public int getPatchVersion() {
        return patchVersion;
    }
    
    /**
     * Verifica si la versión actual es igual o superior a la especificada
     * 
     * @param major Versión mayor
     * @param minor Versión menor
     * @return true si es igual o superior
     */
    public boolean isVersionOrHigher(int major, int minor) {
        if (this.majorVersion > major) return true;
        if (this.minorVersion >= minor) return true;
        return false;
    }
    
    /**
     * Verifica si la versión actual es igual o superior a la especificada (incluyendo patch)
     * 
     * @param major Versión mayor
     * @param minor Versión menor
     * @param patch Versión de parche
     * @return true si es igual o superior
     */
    public boolean isVersionOrHigher(int major, int minor, int patch) {
        if (this.majorVersion > major) return true;
        if (this.minorVersion > minor) return true;
        if (this.minorVersion == minor && this.patchVersion >= patch) return true;
        return false;
    }
    
    /**
     * Verifica si el servidor está en el rango de versiones soportadas (1.20.4 - 1.21.11)
     * 
     * @return true si la versión está en el rango soportado
     */
    public boolean isSupportedVersion() {
        // Verificar si es 1.20.x >= 1.20.4
        if (majorVersion == 1 && minorVersion == 20) {
            return patchVersion >= 4; // 1.20.4 o superior
        }
        // Verificar si es 1.21.x <= 1.21.11
        else if (majorVersion == 1 && minorVersion == 21) {
            return patchVersion <= 11; // 1.21.11 o inferior
        }
        // Soportar futuras versiones 1.22.x, etc. hasta 1.21.11
        else if (majorVersion == 1 && minorVersion > 21) {
            return minorVersion <= 21 && patchVersion <= 11; // hasta 1.21.11
        }
        return false;
    }
    
    /**
     * Verifica si la versión actual es 1.20.4 o superior
     * 
     * @return true si es 1.20.4 o superior
     */
    public boolean isAtLeast1_20_4() {
        if (majorVersion > 1) return true;
        if (majorVersion == 1 && minorVersion > 20) return true;
        if (minorVersion == 20 && patchVersion >= 4) return true;
        return false;
    }
    
    /**
     * Verifica si la versión actual es 1.21 o superior
     * 
     * @return true si es 1.21 o superior
     */
    public boolean isAtLeast1_21() {
        if (majorVersion > 1) return true;
        if (minorVersion >= 21) return true;
        return false;
    }
    
    /**
     * Verifica si la versión actual es 1.21.11 o inferior
     * 
     * @return true si es 1.21.11 o inferior
     */
    public boolean isAtMost1_21_11() {
        if (majorVersion < 1) return true;
        if (minorVersion < 21) return true;
        if (minorVersion == 21 && patchVersion <= 11) return true;
        return false;
    }
    
    @Override
    public String toString() {
        return "Minecraft " + serverVersion;
    }
}
