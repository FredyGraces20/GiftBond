package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;

public class SaveDataCommand implements CommandExecutor {
    private final GiftBond plugin;

    public SaveDataCommand(GiftBond plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Solo permitir a jugadores (no a la consola)
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission_player_only", "{prefix}&cSolo los jugadores pueden usar este comando.")));
            return true;
        }

        Player player = (Player) sender;
        
        // Verificar permiso
        if (!player.hasPermission("giftbond.savedata") && !player.isOp()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("errors.no_permission", "{prefix}&cNo tienes permiso para usar este comando.")));
            return true;
        }

        // Crear un backup manual
        plugin.getDatabaseManager().createManualBackup();
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("success.data_saved", "{prefix}&aDatos guardados correctamente.")));
        return true;
    }
}