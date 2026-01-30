package com.fredygraces.giftbond.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.logging.GiftBondLogger;
import com.fredygraces.giftbond.models.MailboxGift;
import com.fredygraces.giftbond.permissions.PermissionManager;
import com.fredygraces.giftbond.storage.MailboxDAO;
import com.fredygraces.giftbond.utils.DebugLogger;

/**
 * Comando para gestionar el mailbox de regalos
 * /gb redeem [nick|all]
 */
public class MailboxCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final MailboxDAO mailboxDAO;
    private final DebugLogger debugLogger;

    public MailboxCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.mailboxDAO = plugin.getMailboxDAO();
        this.debugLogger = new DebugLogger(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessage("errors.no_permission_player_only", "{prefix}&cSolo los jugadores pueden usar este comando.")));
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Verificar permisos usando el sistema profesional
        if (!PermissionManager.canRedeemGifts(player)) {
            player.sendMessage(plugin.getPrefix() + 
                ChatColor.translateAlternateColorCodes('&', 
                    PermissionManager.getPermissionDeniedMessage(PermissionManager.COMMAND_REDEEM)));
            GiftBondLogger.warn(String.format("Player '%s' attempted to use /gb redeem without permission", 
                player.getName()));
            return true;
        }

        // Sin argumentos - mostrar ayuda o resumen
        if (args.length == 0 || args[0].equalsIgnoreCase("status") || args[0].equalsIgnoreCase("info")) {
            showPendingSenders(player, playerUUID);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            showHelp(player);
            return true;
        }

        String action = args[0].toLowerCase();

        // Reclamar items
        if (action.equals("items")) {
            claimGiftsFiltered(player, playerUUID, "items");
            return true;
        }

        // Reclamar dinero
        if (action.equals("money") || action.equals("dinero")) {
            claimGiftsFiltered(player, playerUUID, "money");
            return true;
        }

        // Reclamar todos los regalos
        if (action.equals("all") || action.equals("todos")) {
            claimGiftsFiltered(player, playerUUID, "all");
            return true;
        }

        // Reclamar regalos de un remitente específico
        claimGiftsFromSender(player, playerUUID, action);
        return true;
    }

    /**
     * Mostrar ayuda del comando mailbox
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_header", "&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_title", "&6&l📬 SISTEMA DE MAILBOX")));
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_status", "&e/gb redeem &7- Ver resumen de regalos")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_all", "&e/gb redeem all &7- Reclamar todos los regalos")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_sender", "&e/gb redeem <jugador> &7- Reclamar de alguien específico")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_items", "&e/gb redeem items &7- Reclamar solo items")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_money", "&e/gb redeem money &7- Reclamar solo dinero")));
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("commands.mailbox_help_footer", "&6&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")));
    }

    /**
     * Reclamar regalos filtrados (items, money, all)
     */
    private void claimGiftsFiltered(Player player, UUID playerUUID, String filter) {
        debugLogger.debug("=== CLAIM FILTERED ATTEMPT (" + filter + ") ===");
        debugLogger.debug("Player: " + player.getName());
        
        List<MailboxGift> allGifts = mailboxDAO.getAllPendingGifts(playerUUID);
        List<MailboxGift> toProcess = new ArrayList<>();
        
        for (MailboxGift gift : allGifts) {
            if (filter.equals("all")) {
                toProcess.add(gift);
            } else if (filter.equals("items") && !gift.getSharedItems().isEmpty()) {
                toProcess.add(gift);
            } else if (filter.equals("money") && gift.getMoney() > 0) {
                toProcess.add(gift);
            }
        }

        if (toProcess.isEmpty()) {
            String typeKey = filter.equals("items") ? "items" : (filter.equals("money") ? "dinero" : "regalos");
            String msg = plugin.getMessage("mailbox.no_gifts_type", "{prefix}&c❌ No tienes {type} pendientes para reclamar.")
                    .replace("{type}", typeKey);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return;
        }

        // Verificar espacio solo si hay items
        List<ItemStack> allItems = getAllItems(toProcess);
        if (!allItems.isEmpty() && !hasInventorySpace(player, allItems)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.no_space_inventory", "{prefix}&c❌ ¡Espacio insuficiente en el inventario!")));
            return;
        }

        // Entregar regalos
        int totalGifts = 0;
        int totalPoints = 0;
        double totalMoney = 0;
        
        for (MailboxGift gift : toProcess) {
            // Entregar items
            for (ItemStack item : gift.getSharedItems()) {
                player.getInventory().addItem(item);
            }
            
            // Entregar dinero
            if (gift.getMoney() > 0) {
                totalMoney += gift.getMoney();
                debugLogger.debug("[CLAIM-MONEY] Giving $" + gift.getMoney() + " to " + player.getName());
                String giveCmd = "eco give " + player.getName() + " " + gift.getMoney();
                debugLogger.debug("[CLAIM-MONEY] Executing command: " + giveCmd);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);
            }
            
            // Otorgar puntos correspondientes
            int points = gift.getPointsAwarded();
            plugin.getFriendshipManager().addFriendshipPoints(gift.getSenderUUID().toString(), 
                                                              playerUUID.toString(), 
                                                              gift.getBasePoints());
            totalPoints += points;
            
            // Acumular estadísticas
            totalGifts++;
            
            // Marcar como reclamado y eliminar
            mailboxDAO.markAsClaimed(gift.getId());
            mailboxDAO.deleteGift(gift.getId());
        }

        String claimMsg;
        if (totalMoney > 0) {
            claimMsg = plugin.getMessage("mailbox.claim_success_money", "{prefix}&a✅ Has reclamado &f{count} &aregalo(s) incluyendo &f${amount}!")
                    .replace("{count}", String.valueOf(totalGifts))
                    .replace("{amount}", String.format("%,.2f", totalMoney));
        } else {
            claimMsg = plugin.getMessage("mailbox.gift_claimed", "{prefix}&a✅ ¡Has reclamado {count} regalo(s) de {sender}!")
                    .replace("{count}", String.valueOf(totalGifts))
                    .replace("{sender}", "varios");
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', claimMsg));
        
        String pointsMsg = plugin.getMessage("mailbox.claim_success_points", "&7Recibiste &f{points} &7puntos de amistad.")
                .replace("{points}", String.valueOf(totalPoints));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', pointsMsg));

        // Notificar al remitente
        notifySenders(player, toProcess);
    }
    private void showPendingSenders(Player player, UUID playerUUID) {
        List<MailboxGift> allGifts = mailboxDAO.getAllPendingGifts(playerUUID);
        List<MailboxDAO.GiftSummary> summaries = mailboxDAO.getPendingGiftSummaries(playerUUID);

        if (allGifts.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.no_pending_gifts", "{prefix}&a📭 No tienes regalos pendientes.")));
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.status_header", "{prefix}&6📬 Resumen de tu Mailbox:")));
        
        double totalMoney = 0;
        List<ItemStack> allItems = new ArrayList<>();
        
        for (MailboxGift gift : allGifts) {
            totalMoney += gift.getMoney();
            allItems.addAll(gift.getSharedItems());
        }
        
        int requiredSlots = calculateRequiredSlots(allItems);
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.status_money", "&7 » &fDinero acumulado: &a${amount}")
                .replace("{amount}", String.format("%,.2f", totalMoney))));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.status_slots", "&7 » &fEspacio necesario: &b{slots} slots")
                .replace("{slots}", String.valueOf(requiredSlots))));
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.status_players_title", "&6🎁 Regalos por jugador:")));
        
        for (MailboxDAO.GiftSummary summary : summaries) {
            String senderName = summary.getSenderName();
            int count = summary.getCount();
            long timeAgo = (System.currentTimeMillis() - summary.getLastGiftTimestamp()) / 1000;
            
            String timeText = getTimeAgoText(timeAgo);
            
            String entry = plugin.getMessage("mailbox.status_player_entry", "&a  • {player} &7({count} regalo{plural}) &8· &7{time}")
                    .replace("{player}", senderName)
                    .replace("{count}", String.valueOf(count))
                    .replace("{plural}", count > 1 ? "s" : "")
                    .replace("{time}", timeText);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', entry));
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.status_usage_sender", "&eUsa &f/gb redeem <nick> &epara recoger de alguien específico")));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getMessage("mailbox.status_usage_all", "&eUsa &f/gb redeem all &epara recoger todo")));
    }

    /**
     * Reclamar regalos de un remitente específico
     */
    private void claimGiftsFromSender(Player player, UUID playerUUID, String senderName) {
        debugLogger.debug("=== CLAIM FROM SENDER ATTEMPT ===");
        debugLogger.debug("Player: " + player.getName());
        debugLogger.debug("Sender: " + senderName);
        debugLogger.debug("UUID: " + playerUUID);
        
        List<MailboxGift> gifts = mailboxDAO.getPendingGiftsFromSender(playerUUID, senderName);
        debugLogger.debug("Found " + gifts.size() + " gifts from " + senderName);

        if (gifts.isEmpty()) {
            String msg = plugin.getMessage("mailbox.gift_not_found", "{prefix}&c❌ No tienes regalos pendientes de {sender}")
                    .replace("{sender}", senderName);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            return;
        }

        // Verificar espacio en inventario
        List<ItemStack> allItems = getAllItems(gifts);
        if (!hasInventorySpace(player, allItems)) {
            int requiredSlots = calculateRequiredSlots(allItems);
            int availableSlots = getEmptySlots(player);
            
            String message = plugin.getMessage("mailbox.mailbox_no_space_detailed", """
                {prefix}&c❌ No tienes suficiente espacio en el inventario!
                &7Necesitas &f{required_slots} &7espacios libres.
                &7Tienes &f{available_slots} &7espacios disponibles.
                
                &eVacía algunos slots y usa:
                &f/gb redeem {command}""");
            
            message = message.replace("{required_slots}", String.valueOf(requiredSlots))
                           .replace("{available_slots}", String.valueOf(availableSlots))
                           .replace("{command}", senderName);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        // Entregar items y dinero
        int totalGifts = 0;
        int totalPoints = 0;
        double totalMoney = 0;
        
        for (MailboxGift gift : gifts) {
            // Entregar items
            for (ItemStack item : gift.getSharedItems()) {
                player.getInventory().addItem(item);
            }
            
            // Entregar dinero
            if (gift.getMoney() > 0) {
                totalMoney += gift.getMoney();
                String giveCmd = "eco give " + player.getName() + " " + gift.getMoney();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), giveCmd);
            }
            
            // Otorgar puntos correspondientes
            plugin.getFriendshipManager().addFriendshipPoints(gift.getSenderUUID().toString(), 
                                                              playerUUID.toString(), 
                                                              gift.getBasePoints());
            
            // Acumular estadísticas
            totalGifts++;
            totalPoints += gift.getPointsAwarded();
            
            // Marcar como reclamado y eliminar
            mailboxDAO.markAsClaimed(gift.getId());
            mailboxDAO.deleteGift(gift.getId());
            
            debugLogger.debug("Regalo reclamado de " + gift.getSenderName() + " a " + player.getName());
        }

        // Mensaje de éxito
        String claimMsg;
        if (totalMoney > 0) {
            claimMsg = plugin.getMessage("mailbox.claim_success_money", "{prefix}&a✅ Has reclamado &f{count} &aregalo(s) incluyendo &f${amount}!")
                    .replace("{count}", String.valueOf(totalGifts))
                    .replace("{amount}", String.format("%,.2f", totalMoney));
        } else {
            claimMsg = plugin.getMessage("mailbox.gift_claimed", "{prefix}&a✅ ¡Has reclamado {count} regalo(s) de {sender}!")
                    .replace("{count}", String.valueOf(totalGifts))
                    .replace("{sender}", senderName);
        }
        
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', claimMsg));
        
        String pointsMsg = plugin.getMessage("mailbox.claim_success_points", "&7Recibiste &f{points} &7puntos de amistad.")
                .replace("{points}", String.valueOf(totalPoints));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', pointsMsg));

        // Notificar al remitente si está en línea
        notifySenders(player, gifts);
    }

    // Métodos utilitarios
    private List<ItemStack> getAllItems(List<MailboxGift> gifts) {
        List<ItemStack> allItems = new java.util.ArrayList<>();
        for (MailboxGift gift : gifts) {
            allItems.addAll(gift.getSharedItems());
        }
        return allItems;
    }

    private boolean hasInventorySpace(Player player, List<ItemStack> items) {
        int emptySlots = getEmptySlots(player);
        int requiredSlots = calculateRequiredSlots(items);
        
        debugLogger.debug("Inventory space check for " + player.getName());
        debugLogger.debug("  Empty slots: " + emptySlots);
        debugLogger.debug("  Required slots: " + requiredSlots);
        debugLogger.debug("  Result: " + (emptySlots >= requiredSlots));
        
        return emptySlots >= requiredSlots;
    }

    private int getEmptySlots(Player player) {
        int emptySlots = 0;
        ItemStack[] contents = player.getInventory().getContents();
        
        debugLogger.debug("Checking inventory for " + player.getName() + " (" + contents.length + " slots total)");
        
        // Solo contar slots 0-35 (inventario principal)
        int inventorySize = Math.min(contents.length, 36);
        
        for (int i = 0; i < inventorySize; i++) {
            ItemStack slot = contents[i];
            if (slot == null || slot.getType() == org.bukkit.Material.AIR) {
                emptySlots++;
                debugLogger.debug("  Slot " + i + ": EMPTY");
            } else {
                debugLogger.debug("  Slot " + i + ": " + slot.getType() + " x" + slot.getAmount());
            }
        }
        
        // No contar armadura (36-39) ni escudo/off-hand (40)
        debugLogger.debug("Ignored armor slots (36-39) and off-hand slot (40)");
        debugLogger.debug("Total empty inventory slots: " + emptySlots);
        return emptySlots;
    }

    private int calculateRequiredSlots(List<ItemStack> items) {
        int slots = 0;
        
        debugLogger.debug("Calculating required slots for " + items.size() + " items:");
        
        for (ItemStack item : items) {
            int maxStackSize = item.getMaxStackSize();
            int amount = item.getAmount();
            int itemSlots = (int) Math.ceil((double) amount / maxStackSize);
            
            debugLogger.debug("  " + item.getType() + " x" + amount + 
                            " (max stack: " + maxStackSize + ") = " + itemSlots + " slots");
            
            slots += itemSlots;
        }
        
        debugLogger.debug("Total required slots: " + slots);
        return slots;
    }

    private String getTimeAgoText(long seconds) {
        if (seconds < 60) return "ahora";
        if (seconds < 3600) return (seconds / 60) + "m";
        if (seconds < 86400) return (seconds / 3600) + "h";
        return (seconds / 86400) + "d";
    }

    private void notifySenders(Player claimer, List<MailboxGift> gifts) {
        for (MailboxGift gift : gifts) {
            Player sender = Bukkit.getPlayer(gift.getSenderName());
            if (sender != null && sender.isOnline()) {
                String msg = plugin.getMessage("mailbox.gift_claimed_notification", "{prefix}&a¡{claimer} ha reclamado tus regalos!")
                        .replace("{claimer}", claimer.getName());
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            }
        }
    }
}