package com.fredygraces.giftbond.commands;

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
import com.fredygraces.giftbond.models.MailboxGift;
import com.fredygraces.giftbond.storage.MailboxDAO;
import com.fredygraces.giftbond.utils.DebugLogger;

/**
 * Comando para gestionar el mailbox de regalos
 * /mailbox [nick|all]
 */
public class MailboxCommand implements CommandExecutor {
    private final GiftBond plugin;
    private final MailboxDAO mailboxDAO;
    private final DebugLogger debugLogger;

    public MailboxCommand(GiftBond plugin) {
        this.plugin = plugin;
        this.mailboxDAO = new MailboxDAO(plugin);
        this.debugLogger = new DebugLogger(plugin);
        
        // Inicializar tablas de mailbox
        this.mailboxDAO.initializeTables();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            String prefix = (plugin != null) ? plugin.getPrefix() : "[GiftBond] ";
            if (sender != null) {
                sender.sendMessage(prefix + ChatColor.RED + "Este comando solo puede ser usado por jugadores.");
            }
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        // Verificar permisos
        if (!player.hasPermission("giftbond.use")) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }

        // Sin argumentos - mostrar lista de remitentes
        if (args.length == 0) {
            showPendingSenders(player, playerUUID);
            return true;
        }

        String action = args[0].toLowerCase();

        // Reclamar todos los regalos
        if (action.equals("all") || action.equals("todos")) {
            claimAllGifts(player, playerUUID);
            return true;
        }

        // Reclamar regalos de un remitente específico
        claimGiftsFromSender(player, playerUUID, action);
        return true;
    }

    /**
     * Mostrar lista de remitentes con regalos pendientes
     */
    private void showPendingSenders(Player player, UUID playerUUID) {
        List<MailboxDAO.GiftSummary> summaries = mailboxDAO.getPendingGiftSummaries(playerUUID);

        if (summaries.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "📭 No tienes regalos pendientes.");
            return;
        }

        player.sendMessage(plugin.getPrefix() + ChatColor.GOLD + "📬 Tienes regalos de:");
        
        for (MailboxDAO.GiftSummary summary : summaries) {
            String senderName = summary.getSenderName();
            int count = summary.getCount();
            long timeAgo = (System.currentTimeMillis() - summary.getLastGiftTimestamp()) / 1000;
            
            String timeText = getTimeAgoText(timeAgo);
            
            player.sendMessage(ChatColor.GREEN + senderName + 
                             ChatColor.GRAY + " (" + count + " regalo" + (count > 1 ? "s" : "") + ")" +
                             ChatColor.DARK_GRAY + " · " + timeText);
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Usa " + ChatColor.WHITE + "/mailbox <nick>" + 
                         ChatColor.YELLOW + " para recoger regalos de alguien específico");
        player.sendMessage(ChatColor.YELLOW + "Usa " + ChatColor.WHITE + "/mailbox all" + 
                         ChatColor.YELLOW + " para recoger todos los regalos");
    }

    /**
     * Reclamar todos los regalos pendientes
     */
    private void claimAllGifts(Player player, UUID playerUUID) {
        debugLogger.debug("=== CLAIM ALL ATTEMPT ===");
        debugLogger.debug("Player: " + player.getName());
        debugLogger.debug("UUID: " + playerUUID);
        
        List<MailboxGift> allGifts = mailboxDAO.getAllPendingGifts(playerUUID);
        debugLogger.debug("Found " + allGifts.size() + " pending gifts");

        if (allGifts.isEmpty()) {
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "❌ No tienes regalos pendientes para reclamar.");
            return;
        }

        // Verificar espacio en inventario
        List<ItemStack> allItems = getAllItems(allGifts);
        if (!hasInventorySpace(player, allItems)) {
            int requiredSlots = calculateRequiredSlots(allItems);
            int availableSlots = getEmptySlots(player);
            
            String message = plugin.getMessage("mailbox.mailbox_no_space_detailed", """
                {prefix}&c❌ No tienes suficiente espacio en el inventario!
                &7Necesitas &f{required_slots} &7espacios libres.
                &7Tienes &f{available_slots} &7espacios disponibles.
                
                &eVacía algunos slots y usa:
                &f/mailbox all""");
            
            message = message.replace("{required_slots}", String.valueOf(requiredSlots))
                           .replace("{available_slots}", String.valueOf(availableSlots))
                           .replace("{command}", "all");
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        // Entregar todos los items
        int totalGifts = 0;
        int totalPoints = 0;
        
        for (MailboxGift gift : allGifts) {
            // Entregar items
            for (ItemStack item : gift.getSharedItems()) {
                player.getInventory().addItem(item);
            }
            
            // Acumular estadísticas
            totalGifts++;
            totalPoints += gift.getPointsAwarded();
            
            // Marcar como reclamado y eliminar
            mailboxDAO.markAsClaimed(gift.getId());
            mailboxDAO.deleteGift(gift.getId());
            
            debugLogger.debug("Regalo reclamado de " + gift.getSenderName() + " a " + player.getName());
        }

        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "✅ Has reclamado " + 
                         ChatColor.WHITE + totalGifts + ChatColor.GREEN + " regalo(s)!");
        player.sendMessage(ChatColor.GRAY + "Recibiste " + ChatColor.WHITE + totalPoints + 
                         ChatColor.GRAY + " puntos de amistad adicionales.");

        // Notificar al remitente si está en línea
        notifySenders(player, allGifts);
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
            player.sendMessage(plugin.getPrefix() + ChatColor.RED + "❌ No tienes regalos pendientes de " + senderName);
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
                &f/mailbox {command}""");
            
            message = message.replace("{required_slots}", String.valueOf(requiredSlots))
                           .replace("{available_slots}", String.valueOf(availableSlots))
                           .replace("{command}", senderName);
            
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
            return;
        }

        // Entregar items
        int totalGifts = 0;
        int totalPoints = 0;
        
        for (MailboxGift gift : gifts) {
            // Entregar items
            for (ItemStack item : gift.getSharedItems()) {
                player.getInventory().addItem(item);
            }
            
            // Acumular estadísticas
            totalGifts++;
            totalPoints += gift.getPointsAwarded();
            
            // Marcar como reclamado y eliminar
            mailboxDAO.markAsClaimed(gift.getId());
            mailboxDAO.deleteGift(gift.getId());
            
            debugLogger.debug("Regalo reclamado de " + gift.getSenderName() + " a " + player.getName());
        }

        player.sendMessage(plugin.getPrefix() + ChatColor.GREEN + "✅ Has reclamado " + 
                         ChatColor.WHITE + totalGifts + ChatColor.GREEN + " regalo(s) de " + 
                         ChatColor.WHITE + senderName + ChatColor.GREEN + "!");
        player.sendMessage(ChatColor.GRAY + "Recibiste " + ChatColor.WHITE + totalPoints + 
                         ChatColor.GRAY + " puntos de amistad adicionales.");

        // Notificar al remitente si está en línea
        if (Bukkit.getPlayer(senderName) != null) {
            Player senderPlayer = Bukkit.getPlayer(senderName);
            if (senderPlayer != null) {
                senderPlayer.sendMessage(plugin.getPrefix() + ChatColor.GREEN + player.getName() + 
                                 ChatColor.WHITE + " ha reclamado tus regalos!");
            }
        }
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
                sender.sendMessage(plugin.getPrefix() + ChatColor.GREEN + claimer.getName() + 
                                 ChatColor.WHITE + " ha reclamado tus regalos!");
            }
        }
    }
}