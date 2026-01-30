package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.fredygraces.giftbond.GiftBond;

public class ReloadCommand implements CommandExecutor {
    private final GiftBond plugin;

    public ReloadCommand(GiftBond plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("giftbond.admin.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission", "{prefix}&cNo tienes permiso para usar este comando.")));
            return true;
        }

        plugin.getConfigManager().reloadConfigs();
        plugin.getGiftManager().reload();
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("success.config_reloaded", "{prefix}&aConfiguración recargada.")));
        return true;
    }
}
