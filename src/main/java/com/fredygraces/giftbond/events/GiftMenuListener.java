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
import com.fredygraces.giftbond.utils.DebugLogger;
import com.fredygraces.giftbond.utils.GiftSessionManager;

public class GiftMenuListener implements Listener {
    private final GiftBond plugin;
    private final FriendshipManager friendshipManager;
    private final GiftManager giftManager;

    private final GiftSessionManager sessionManager;
    private final DebugLogger debugLogger;
    
    public GiftMenuListener(GiftBond plugin) {
        this.plugin = plugin;
        this.friendshipManager = plugin.getFriendshipManager();
        this.giftManager = plugin.getGiftManager();
        this.sessionManager = GiftSessionManager.getInstance();
        this.debugLogger = new DebugLogger(plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean isGiftMenu = title.startsWith("Enviar regalo a ") || title.startsWith("Regalos - Rotan en");
        
        if (isGiftMenu) {
            event.setCancelled(true);
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            
            if (clickedItem == null || clickedItem.getType().isAir()) {
                return;
            }
            
            // Obtener el nombre del jugador receptor
            Player receiver = null;
            if (title.startsWith("Enviar regalo a ")) {
                String receiverName = title.substring("Enviar regalo a ".length());
                receiver = plugin.getServer().getPlayer(receiverName);
            } else if (title.startsWith("Regalos - Rotan en")) {
                // Para modo auto, usar el sistema de sesiones
                String receiverName = sessionManager.getReceiverName(player);
                if (receiverName != null) {
                    receiver = plugin.getServer().getPlayer(receiverName);
                    // Limpiar la sesión después de usarla
                    sessionManager.endGiftSession(player);
                }
            }
            
            // Si aún no tenemos receptor, mostrar error
            if (receiver == null) {
                player.sendMessage(ChatColor.RED + "❌ Error: No se pudo determinar el destinatario del regalo.");
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
        debugLogger.debug("Processing gift selection for " + sender.getName() + " -> " + receiver.getName());
        debugLogger.debug("Clicked item: " + (item.hasItemMeta() ? item.getItemMeta().getDisplayName() : item.getType().name()));
        
        // Buscar el regalo correspondiente por nombre
        String itemName = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        debugLogger.debug("Item name (stripped): '" + itemName + "'");
        
        // Remover indicadores de disponibilidad
        if (itemName.endsWith(" (Bloqueado)")) {
            itemName = itemName.substring(0, itemName.length() - 12);
            debugLogger.debug("Removed 'Bloqueado' suffix, new name: '" + itemName + "'");
        }
        
        GiftItem selectedGift = findGiftByName(itemName);
        
        if (selectedGift == null) {
            sender.sendMessage(ChatColor.RED + "❌ Error: Regalo no encontrado. Por favor, contacta a un administrador.");
            debugLogger.severe("Gift not found: '" + itemName + "' - This shouldn't happen!");
            sender.closeInventory();
            return;
        }
        
        debugLogger.debug("Found gift: " + selectedGift.getId() + " (" + selectedGift.getName() + ")");
        
        // Verificar si el regalo está disponible (no es barrera)
        if (item.getType() == org.bukkit.Material.BARRIER) {
            sender.sendMessage(ChatColor.RED + "🚫 No tienes los items necesarios para este regalo.");
            sender.closeInventory();
            return;
        }
        
        // Verificar nuevamente si tiene los items requeridos
        if (!giftManager.hasRequiredItems(sender, selectedGift)) {
            sender.sendMessage(ChatColor.RED + "⚠️ Ya no tienes los items necesarios para este regalo.");
            sender.closeInventory();
            return;
        }
        
        debugLogger.debug("Player has required items, proceeding with gift...");
        
        // Eliminar los items requeridos del inventario
        debugLogger.debug("Removing required items for gift: " + selectedGift.getId());
        for (GiftItem.ItemRequirement req : selectedGift.getRequiredItems()) {
            debugLogger.debug("  Requiring: " + req.getAmount() + "x " + req.getMaterial());
        }
        giftManager.removeRequiredItems(sender, selectedGift);
        
        // Agregar puntos de amistad
        String senderUUID = sender.getUniqueId().toString();
        String receiverUUID = receiver.getUniqueId().toString();
        int points = selectedGift.getPoints();
        
        debugLogger.debug("Adding friendship points: " + points + " (base points)");
        int finalPoints = friendshipManager.addFriendshipPoints(senderUUID, receiverUUID, points);
        debugLogger.debug("Final points awarded: " + finalPoints + " (after boost)");
        
        // Guardar en historial
        String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', selectedGift.getName()));
        plugin.getDatabaseManager().saveGiftHistory(senderUUID, receiverUUID, giftName, finalPoints);
        
        // Incrementar contador diario
        plugin.getDatabaseManager().incrementDailyGiftCount(senderUUID);
        
        // Establecer cooldown
        giftManager.setCooldown(sender);
        
        // Enviar mensajes de confirmación usando la configuración
        String giftSentMessage = plugin.getMessage("messages.gift_sent", 
            "{prefix}&a✅ Has enviado un regalo de &f{gift} &a({points} puntos) a &f{receiver}");
        giftSentMessage = giftSentMessage.replace("{gift}", ChatColor.stripColor(selectedGift.getName()))
                                         .replace("{points}", String.valueOf(finalPoints))
                                         .replace("{receiver}", receiver.getName());
        
        String giftReceivedMessage = plugin.getMessage("messages.gift_received",
            "{prefix}&a🎉 Has recibido un regalo de &f{gift} &a({points} puntos) de &f{sender}");
        giftReceivedMessage = giftReceivedMessage.replace("{gift}", ChatColor.stripColor(selectedGift.getName()))
                                                .replace("{points}", String.valueOf(finalPoints))
                                                .replace("{sender}", sender.getName());
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', giftSentMessage));
        receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', giftReceivedMessage));
        
        debugLogger.info("[SUCCESS] Gift sent successfully from " + sender.getName() + " to " + receiver.getName());
        
        // Cerrar inventario
        sender.closeInventory();
    }
    
    private GiftItem findGiftByName(String name) {
        // Primero intentar búsqueda exacta
        for (GiftItem gift : giftManager.getAllGifts()) {
            String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', gift.getName()));
            if (giftName.equals(name)) {
                return gift;
            }
        }
        
        // Si no encuentra por nombre exacto, intentar búsqueda por material (modo auto)
        if (giftManager.isAutoMode()) {
            try {
                // Convertir el nombre mostrado al material correspondiente
                String materialName = name.toUpperCase().replace(" ", "_");
                org.bukkit.Material material = org.bukkit.Material.valueOf(materialName);
                
                // Buscar un regalo que tenga este material como requerimiento
                for (GiftItem gift : giftManager.getAllGifts()) {
                    for (GiftItem.ItemRequirement req : gift.getRequiredItems()) {
                        if (req.getMaterial() == material) {
                            return gift;
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // Material no encontrado, continuar
                debugLogger.debugWarning("No se pudo encontrar material para: " + name);
            }
        }
        
        debugLogger.debugWarning("Regalo no encontrado: " + name);
        debugLogger.debugWarning("Regalos disponibles: " + giftManager.getAllGifts().size());
        for (GiftItem gift : giftManager.getAllGifts()) {
            debugLogger.debugWarning("  - " + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', gift.getName())));
        }
        
        return null;
    }
}