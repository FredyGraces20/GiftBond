package com.fredygraces.giftbond.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.GiftManager;
import com.fredygraces.giftbond.menus.GiftMenu;
import com.fredygraces.giftbond.utils.GiftSessionManager;

public class RegaloCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final GiftMenu giftMenu;

    public RegaloCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.giftMenu = plugin.getGiftMenu();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (sender != null) {
                sender.sendMessage("§cSolo los jugadores pueden usar este comando.");
            }
            return true;
        }

        Player player = (Player) sender;
        
        // Verificar permisos básicos
        if (!player.hasPermission("giftbond.use")) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        // Si no hay argumentos, mostrar ayuda
        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        // Manejar comando de reclamo
        if (action.equals("reclamar") || action.equals("claim") || action.equals("redeem")) {
            return handleMailboxCommand(player, args);
        }

        // Manejar envío de regalo (funcionalidad original)
        return handleGiftSending(player, args);
    }

    private void showHelp(Player player) {
        player.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "Comandos disponibles:");
        player.sendMessage(ChatColor.YELLOW + "/regalo <jugador>" + ChatColor.GRAY + " - Enviar un regalo");
        player.sendMessage(ChatColor.YELLOW + "/regalo reclamar" + ChatColor.GRAY + " - Ver regalos pendientes");
        player.sendMessage(ChatColor.YELLOW + "/regalo reclamar <nick>" + ChatColor.GRAY + " - Reclamar de alguien específico");
        player.sendMessage(ChatColor.YELLOW + "/regalo reclamar all" + ChatColor.GRAY + " - Reclamar todos los regalos");
    }

    private boolean handleMailboxCommand(Player player, String[] args) {
        // Obtener el comando mailbox ya registrado
        Command mailboxCmd = plugin.getCommand("mailbox");
        if (mailboxCmd == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "❌ Comando mailbox no encontrado");
            return true;
        }
        
        // Reindexar argumentos para el comando mailbox (eliminar "reclamar")
        String[] mailboxArgs = new String[args.length - 1];
        System.arraycopy(args, 1, mailboxArgs, 0, args.length - 1);
        
        // Ejecutar el comando mailbox
        return mailboxCmd.execute(player, "mailbox", mailboxArgs);
    }

    private boolean handleGiftSending(Player player, String[] args) {
        GiftSessionManager sessionManager = GiftSessionManager.getInstance();
        String targetPlayerName = args[0];
        Player targetPlayer = plugin.getServer().getPlayer(targetPlayerName);
        
        if (targetPlayer == null) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "Jugador no encontrado: " + targetPlayerName);
            return true;
        }

        // Si las configuraciones están desactivadas, saltar todas las restricciones
        if (!plugin.getConfigManager().getMainConfig().getBoolean("settings.enabled", true)) {
            giftMenu.openGiftMenu(player, targetPlayer);
            return true;
        }

        // Verificar si se permite enviarse regalos a uno mismo
        if (player.equals(targetPlayer) && !plugin.getConfigManager().getMainConfig().getBoolean("settings.allow_self_gifts", false)) {
            String msg = plugin.getMessage("messages.no_self_gift", "{prefix}&cNo puedes enviarte regalos a ti mismo.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return true;
        }

        // Verificar requisito de horas jugadas para el emisor
        GiftManager giftManager = plugin.getGiftManager();
        int minHours = plugin.getConfigManager().getMainConfig().getInt("settings.min_hours_played", 0);
        
        if (!giftManager.hasMinimumPlaytime(player)) {
            String msg = plugin.getMessage("messages.min_hours_sender", 
                "{prefix}&cDebes tener al menos {min} horas jugadas para enviar regalos. (Tienes {current} horas)");
            int currentHours = giftManager.getPlayerHours(player);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                msg.replace("{min}", String.valueOf(minHours)).replace("{current}", String.valueOf(currentHours))));
            return true;
        }

        // Verificar requisito de horas jugadas para el receptor
        if (!giftManager.hasMinimumPlaytime(targetPlayer)) {
            String msg = plugin.getMessage("messages.min_hours_receiver", 
                "{prefix}&cEl jugador {player} debe tener al menos {min} horas jugadas para recibir regalos.");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                msg.replace("{min}", String.valueOf(minHours)).replace("{player}", targetPlayer.getName())));
            return true;
        }
        
        // Registrar la sesión antes de abrir el menú
        sessionManager.startGiftSession(player, targetPlayer.getName());
        
        // Abrir el menú de regalos
        giftMenu.openGiftMenu(player, targetPlayer);
        
        return true;
    }
}