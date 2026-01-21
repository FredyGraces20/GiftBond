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
            sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }
        
        if (args.length == 0) {
            // Mostrar estado actual
            boolean debugEnabled = plugin.getConfigManager().getMainConfig().getBoolean("debug.enabled", false);
            sender.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "Estado de debug: " + 
                             (debugEnabled ? ChatColor.GREEN + "ACTIVADO" : ChatColor.RED + "DESACTIVADO"));
            sender.sendMessage(plugin.getPrefix() + ChatColor.YELLOW + "Usa: /giftbond debug <on|off>");
            return true;
        }
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "on":
            case "true":
            case "enable":
                plugin.getConfigManager().getMainConfig().set("debug.enabled", true);
                plugin.saveConfig();
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "✅ Debug ACTIVADO - Mensajes de debug en consola");
                plugin.getLogger().info("=== DEBUG MODE ACTIVATED BY " + sender.getName() + " ===");
                break;
                
            case "off":
            case "false":
            case "disable":
                plugin.getConfigManager().getMainConfig().set("debug.enabled", false);
                plugin.saveConfig();
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "❌ Debug DESACTIVADO");
                plugin.getLogger().info("=== DEBUG MODE DEACTIVATED BY " + sender.getName() + " ===");
                break;
                
            default:
                sender.sendMessage(plugin.getPrefix() + ChatColor.RED + "Uso: /giftbond debug <on|off>");
                break;
        }
        
        return true;
    }
}