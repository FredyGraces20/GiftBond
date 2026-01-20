package com.fredygraces.giftbond.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.menus.HistoryMenu;

public class HistoryMenuListener implements Listener {
    private final HistoryMenu historyMenu;

    public HistoryMenuListener(GiftBond plugin) {
        this.historyMenu = plugin.getHistoryMenu();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        if (!title.startsWith("§d📜 Historial de Regalos")) {
            return;
        }
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }
        
        // Extraer el número de página del título
        String[] parts = title.split("Pág ");
        if (parts.length < 2) return;
        
        int currentPage;
        try {
            currentPage = Integer.parseInt(parts[1].trim()) - 1;
        } catch (NumberFormatException e) {
            return;
        }
        
        // Manejar navegación
        String itemName = clickedItem.getItemMeta().getDisplayName();
        
        if (itemName.contains("◄ Página Anterior")) {
            historyMenu.openHistoryMenu(player, currentPage - 1);
        } else if (itemName.contains("► Página Siguiente")) {
            historyMenu.openHistoryMenu(player, currentPage + 1);
        }
    }
}
