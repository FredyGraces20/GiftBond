package com.fredygraces.giftbond.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.FriendshipManager;
import com.fredygraces.giftbond.managers.GiftManager;
import com.fredygraces.giftbond.models.GiftItem;

public class GiftMenuListener implements Listener {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;
    private final GiftManager giftManager;

    public GiftMenuListener(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
        this.giftManager = plugin.getGiftManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().startsWith("Enviar regalo a ")) {
            event.setCancelled(true);
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType().isAir()) {
                return;
            }
            
            // Obtener el nombre del jugador receptor del título del inventario
            String title = event.getView().getTitle();
            String receiverName = title.substring("Enviar regalo a ".length());
            Player receiver = plugin.getServer().getPlayer(receiverName);
            
            if (receiver == null) {
                player.sendMessage(ChatColor.RED + "El jugador ya no está en línea.");
                player.closeInventory();
                return;
            }

            // Verificar cooldown
            if (giftManager.isOnCooldown(player)) {
                int remaining = giftManager.getRemainingCooldown(player);
                String msg = plugin.getMessage("messages.cooldown", "{prefix}&cDebes esperar {seconds} segundos antes de enviar otro regalo.");
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("{seconds}", String.valueOf(remaining))));
                player.closeInventory();
                return;
            }

            // Verificar límite diario
            int dailyLimit = plugin.getConfigManager().getMainConfig().getInt("settings.daily_gift_limit", 0);
            if (dailyLimit > 0) {
                int todayCount = plugin.getDatabaseManager().getDailyGiftCount(player.getUniqueId().toString());
                if (todayCount >= dailyLimit) {
                    String msg = plugin.getMessage("messages.daily_limit", 
                        "{prefix}&cHas alcanzado el límite diario de {limit} regalos. Vuelve mañana!");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg.replace("{limit}", String.valueOf(dailyLimit))));
                    player.closeInventory();
                    return;
                }
            }

            // Verificar requisito de horas jugadas nuevamente por seguridad
            if (plugin.getConfigManager().getMainConfig().getBoolean("settings.enabled", true)) {
                if (!giftManager.hasMinimumPlaytime(player)) {
                    int minHours = plugin.getConfigManager().getMainConfig().getInt("settings.min_hours_played", 0);
                    String msg = plugin.getMessage("messages.min_hours_sender", 
                        "{prefix}&cDebes tener al menos {min} horas jugadas para enviar regalos. (Tienes {current} horas)");
                    int currentHours = giftManager.getPlayerHours(player);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        msg.replace("{min}", String.valueOf(minHours)).replace("{current}", String.valueOf(currentHours))));
                    player.closeInventory();
                    return;
                }

                if (!giftManager.hasMinimumPlaytime(receiver)) {
                    int minHours = plugin.getConfigManager().getMainConfig().getInt("settings.min_hours_played", 0);
                    String msg = plugin.getMessage("messages.min_hours_receiver", 
                        "{prefix}&cEl jugador {player} debe tener al menos {min} horas jugadas para recibir regalos.");
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        msg.replace("{min}", String.valueOf(minHours)).replace("{player}", receiver.getName())));
                    player.closeInventory();
                    return;
                }
            }
            
            // Procesar el regalo según el ítem clicado
            processGiftSelection(player, receiver, clickedItem);
        }
    }
    
    private void processGiftSelection(Player sender, Player receiver, ItemStack item) {
        // Buscar el regalo correspondiente por nombre
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        
        // Remover indicadores de disponibilidad
        if (itemName.endsWith(" (Bloqueado)")) {
            itemName = itemName.substring(0, itemName.length() - 12);
        }
        
        GiftItem selectedGift = findGiftByName(itemName);
        
        if (selectedGift == null) {
            sender.sendMessage(ChatColor.RED + "Regalo no encontrado.");
            return;
        }
        
        // Verificar si el regalo está disponible (no es barrera)
        if (item.getType() == org.bukkit.Material.BARRIER) {
            sender.sendMessage(ChatColor.RED + "No tienes los items necesarios para este regalo.");
            sender.closeInventory();
            return;
        }
        
        // Verificar nuevamente si tiene los items requeridos
        if (!giftManager.hasRequiredItems(sender, selectedGift)) {
            sender.sendMessage(ChatColor.RED + "Ya no tienes los items necesarios para este regalo.");
            sender.closeInventory();
            return;
        }
        
        // Eliminar los items requeridos del inventario
        giftManager.removeRequiredItems(sender, selectedGift);
        
        // Agregar puntos de amistad
        String senderUUID = sender.getUniqueId().toString();
        String receiverUUID = receiver.getUniqueId().toString();
        int points = selectedGift.getPoints();
        
        int finalPoints = friendshipManager.addFriendshipPoints(senderUUID, receiverUUID, points);
        
        // Guardar en historial
        String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', selectedGift.getName()));
        plugin.getDatabaseManager().saveGiftHistory(senderUUID, receiverUUID, giftName, finalPoints);
        
        // Incrementar contador diario
        plugin.getDatabaseManager().incrementDailyGiftCount(senderUUID);
        
        // Establecer cooldown
        giftManager.setCooldown(sender);
        
        // Enviar mensajes de confirmación usando la configuración
        String giftSentMessage = plugin.getMessage("messages.gift_sent", 
            "{prefix}&eHas enviado un regalo de &f{gift} &e({points} puntos) a &f{receiver}");
        giftSentMessage = giftSentMessage.replace("{gift}", ChatColor.stripColor(selectedGift.getName()))
                                         .replace("{points}", String.valueOf(finalPoints))
                                         .replace("{receiver}", receiver.getName());
        
        String giftReceivedMessage = plugin.getMessage("messages.gift_received",
            "{prefix}&eHas recibido un regalo de &f{gift} &e({points} puntos) de &f{sender}");
        giftReceivedMessage = giftReceivedMessage.replace("{gift}", ChatColor.stripColor(selectedGift.getName()))
                                                .replace("{points}", String.valueOf(finalPoints))
                                                .replace("{sender}", sender.getName());
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', giftSentMessage));
        receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', giftReceivedMessage));
        
        // Cerrar inventario
        sender.closeInventory();
    }
    
    private GiftItem findGiftByName(String name) {
        for (GiftItem gift : giftManager.getAllGifts()) {
            String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', gift.getName()));
            if (giftName.equals(name)) {
                return gift;
            }
        }
        return null;
    }
}