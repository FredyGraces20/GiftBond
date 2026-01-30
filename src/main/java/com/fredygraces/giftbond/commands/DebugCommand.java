package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.fredygraces.giftbond.GiftBond;

public class DebugCommand implements CommandExecutor {
    private final GiftBond plugin;
    
    public DebugCommand(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("giftbond.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission", "{prefix}&cNo tienes permiso para usar este comando.")));
            return true;
        }
        
        if (args.length == 0) {
            // Mostrar estado actual
            boolean debugEnabled = plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("info.debug_status", "{prefix}&eEstado de debug: &f{status}")
                    .replace("{status}", debugEnabled ? "§aACTIVADO" : "§cDESACTIVADO")));
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_debug", "&cUso: /giftbond debug <on|off>")));
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "on", "true", "enable" -> {
                plugin.getConfigManager().getMainConfig().set("debug.enabled", true);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("success.debug_enabled", "{prefix}&a✅ Debug ACTIVADO - Mensajes de debug en consola")));
                plugin.getLogger().info(() -> "=== DEBUG MODE ACTIVATED BY " + sender.getName() + " ===");
            }
                
            case "off", "false", "disable" -> {
                plugin.getConfigManager().getMainConfig().set("debug.enabled", false);
                plugin.saveConfig();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("success.debug_disabled", "{prefix}&a❌ Debug DESACTIVADO")));
                plugin.getLogger().info(() -> "=== DEBUG MODE DEACTIVATED BY " + sender.getName() + " ===");
            }
                
            default -> sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.usage_debug", "&cUso: /giftbond debug <on|off>")));
        }
        
        return true;
    }
}