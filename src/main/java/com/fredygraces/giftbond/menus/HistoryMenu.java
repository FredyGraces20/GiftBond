package com.fredygraces.giftbond.menus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.DatabaseManager;

public class HistoryMenu {
    private final GiftBond plugin;
    private static final int ITEMS_PER_PAGE = 45; // 5 filas de 9 items

    public HistoryMenu(GiftBond plugin) {
        this.plugin = plugin;
    }

    public void openHistoryMenu(Player player, int page) {
        String playerUUID = player.getUniqueId().toString();
        
        // Obtener historial paginado
        int offset = page * ITEMS_PER_PAGE;
        List<DatabaseManager.GiftHistoryEntry> history = plugin.getDatabaseManager()
            .getGiftHistory(playerUUID, ITEMS_PER_PAGE, offset);
        
        int totalEntries = plugin.getDatabaseManager().getGiftHistoryCount(playerUUID);
        int totalPages = (int) Math.ceil((double) totalEntries / ITEMS_PER_PAGE);
        
        // Crear inventario
        Inventory inv = Bukkit.createInventory(null, 54, "§d📜 Historial de Regalos - Pág " + (page + 1));
        
        // Llenar con items del historial
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (int i = 0; i < history.size() && i < ITEMS_PER_PAGE; i++) {
            DatabaseManager.GiftHistoryEntry entry = history.get(i);
            boolean isSent = entry.getSenderUUID().equals(playerUUID);
            
            ItemStack item;
            if (isSent) {
                item = new ItemStack(Material.PAPER);
            } else {
                item = new ItemStack(Material.ENCHANTED_BOOK);
            }
            
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(isSent ? "§d✉ Regalo Enviado" : "§b📬 Regalo Recibido");
            
            List<String> lore = new ArrayList<>();
            lore.add("§7━━━━━━━━━━━━━━━━━━━━");
            lore.add("§f📅 Fecha: §e" + sdf.format(new Date(entry.getTimestamp())));
            lore.add("§f💝 Regalo: §a" + entry.getGiftName());
            
            if (isSent) {
                String receiverName = getPlayerName(entry.getReceiverUUID());
                lore.add("§f➡ Enviado a: §b" + receiverName);
            } else {
                String senderName = getPlayerName(entry.getSenderUUID());
                lore.add("§f⬅ Recibido de: §b" + senderName);
            }
            
            lore.add("§f⭐ Puntos: §6" + entry.getPoints() + " pts");
            lore.add("§7━━━━━━━━━━━━━━━━━━━━");
            
            meta.setLore(lore);
            item.setItemMeta(meta);
            
            inv.setItem(i, item);
        }
        
        // Llenar espacios vacíos con vidrio gris
        for (int i = history.size(); i < ITEMS_PER_PAGE; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
            inv.setItem(i, filler);
        }
        
        // Fila de navegación (slots 45-53)
        // Botón anterior
        if (page > 0) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta meta = prevButton.getItemMeta();
            meta.setDisplayName("§e◄ Página Anterior");
            List<String> lore = new ArrayList<>();
            lore.add("§7Haz clic para ver");
            lore.add("§7la página anterior");
            meta.setLore(lore);
            prevButton.setItemMeta(meta);
            inv.setItem(45, prevButton);
        }
        
        // Estadísticas (centro)
        ItemStack statsItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) statsItem.getItemMeta();
        skullMeta.setOwningPlayer(player);
        skullMeta.setDisplayName("§6📊 Mis Estadísticas");
        
        List<String> statsLore = new ArrayList<>();
        statsLore.add("§7━━━━━━━━━━━━━━━━━━━━");
        statsLore.add("§f📦 Total Regalos: §e" + totalEntries);
        statsLore.add("§f📄 Páginas: §e" + (page + 1) + " / " + Math.max(1, totalPages));
        statsLore.add("§7━━━━━━━━━━━━━━━━━━━━");
        skullMeta.setLore(statsLore);
        statsItem.setItemMeta(skullMeta);
        inv.setItem(49, statsItem);
        
        // Botón siguiente
        if (page < totalPages - 1) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta meta = nextButton.getItemMeta();
            meta.setDisplayName("§e► Página Siguiente");
            List<String> lore = new ArrayList<>();
            lore.add("§7Haz clic para ver");
            lore.add("§7la siguiente página");
            meta.setLore(lore);
            nextButton.setItemMeta(meta);
            inv.setItem(53, nextButton);
        }
        
        player.openInventory(inv);
    }
    
    private String getPlayerName(String uuid) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid));
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : "Desconocido";
    }
}
