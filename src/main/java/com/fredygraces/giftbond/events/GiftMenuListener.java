package com.fredygraces.giftbond.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
        debugLogger.debug("Clicked item: " + getItemDisplayName(item));
        
        // Buscar el regalo correspondiente por nombre
        String itemName = ChatColor.stripColor(getItemDisplayName(item));
        debugLogger.debug("Item name (stripped): '" + itemName + "'");
        
        // Remover indicadores de disponibilidad
        if (itemName != null && itemName.endsWith(" (Bloqueado)")) {
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
        
        // Verificar si el sistema de mailbox está habilitado
        boolean mailboxEnabled = plugin.getConfigManager().getMainConfig().getBoolean("mailbox.enabled", true);
        
        if (mailboxEnabled) {
            // Verificar si el regalo debe ir al mailbox
            int minCost = plugin.getConfigManager().getMainConfig().getInt("mailbox.min_cost_for_mailbox", 100);
            int giftCost = calculateGiftCost(selectedGift);
            
            debugLogger.debug("Gift cost: " + giftCost + ", Min cost for mailbox: " + minCost);
            
            // Condiciones para usar mailbox:
            // 1. minCost >= 0 y giftCost >= minCost
            // 2. minCost = 0 (todos los regalos van al mailbox)
            // 3. minCost = -1 y giftCost > 0 (solo regalos costosos van al mailbox)
            boolean useMailbox = (minCost >= 0 && giftCost >= minCost) || 
                               (minCost == 0) || 
                               (minCost == -1 && giftCost > 0);
            
            // Regalos gratis se entregan automáticamente si está configurado así
            boolean autoClaimFree = plugin.getConfigManager().getMainConfig().getBoolean("mailbox.auto_claim_free_gifts", true);
            if (giftCost == 0 && autoClaimFree) {
                useMailbox = false;
                debugLogger.debug("Free gift with auto-claim enabled, delivering directly");
            }
            
            if (useMailbox) {
                processGiftToMailbox(sender, receiver, selectedGift);
                return;
            }
        }
        
        // Proceso tradicional (entrega directa)
        processDirectGift(sender, receiver, selectedGift);
    }
    
    /**
     * Procesa un regalo que va al mailbox
     */
    private void processGiftToMailbox(Player sender, Player receiver, GiftItem gift) {
        debugLogger.debug("Processing gift to mailbox: " + gift.getName());
        
        // Calcular puntos compartidos
        int sharedPercentage = plugin.getConfigManager().getMainConfig().getInt("mailbox.shared_percentage", 25);
        int points = gift.getPoints();
        int sharedPoints = (int) Math.round(points * (sharedPercentage / 100.0));
        int senderPoints = points;
        
        debugLogger.debug("Points calculation - Base: " + points + ", Shared: " + sharedPoints + 
                         " (" + sharedPercentage + "%), Sender gets: " + senderPoints);
        
        // Crear items originales (basados en los items requeridos)
        List<ItemStack> originalItems = createItemsFromRequirements(gift.getRequiredItems());
        
        // Crear items compartidos (porcentaje de los originales)
        List<ItemStack> sharedItems = createSharedItems(originalItems, sharedPercentage);
        
        // Eliminar los items requeridos del inventario del emisor
        giftManager.removeRequiredItems(sender, gift);
        
        // Guardar en mailbox
        String senderUUID = sender.getUniqueId().toString();
        String receiverUUID = receiver.getUniqueId().toString();
        String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', gift.getName()));
        
        boolean saved = plugin.getDatabaseManager().savePendingGift(
            senderUUID, 
            sender.getName(),
            receiverUUID, 
            receiver.getName(), 
            gift.getId(), // Guardamos el ID del regalo
            giftName, 
            originalItems,
            sharedItems,
            points,        // basePoints
            senderPoints   // pointsAwarded (con boost del momento de envío)
        );
        
        if (saved) {
            // Agregar puntos de amistad al emisor
            int finalPoints = friendshipManager.addFriendshipPoints(senderUUID, receiverUUID, senderPoints);
            
            // Guardar en historial
            plugin.getDatabaseManager().saveGiftHistory(senderUUID, receiverUUID, giftName, finalPoints);
            
            // Incrementar contador diario
            plugin.getDatabaseManager().incrementDailyGiftCount(senderUUID);
            
            // Establecer cooldown
            giftManager.setCooldown(sender);
            
            // Mensajes de confirmación
            String giftSentMessage = plugin.getMessage("messages.gift_sent_mailbox", 
                "{prefix}&a✅ Has enviado un regalo de &f{gift} &a({points} puntos) a &f{receiver}&a. Se guardará en su mailbox hasta que lo reclame.");
            giftSentMessage = giftSentMessage.replace("{gift}", ChatColor.stripColor(gift.getName()))
                                           .replace("{points}", String.valueOf(finalPoints))
                                           .replace("{receiver}", receiver.getName());
            
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', giftSentMessage));
            
            // Notificar al receptor si está en línea
            if (receiver.isOnline()) {
                String notification = plugin.getMessage("messages.pending_gift_notification",
                    "{prefix}&6📬 ¡Tienes un nuevo regalo de &f{sender}&6! Usa &f/mailbox&6 para reclamarlo.");
                notification = notification.replace("{sender}", sender.getName());
                receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', notification));
            }
            
            debugLogger.info("[MAILBOX] Gift saved successfully from " + sender.getName() + " to " + receiver.getName());
        } else {
            sender.sendMessage(ChatColor.RED + "❌ Error al guardar el regalo. Contacta a un administrador.");
            debugLogger.severe("Failed to save gift to mailbox for " + sender.getName() + " -> " + receiver.getName());
        }
        
        sender.closeInventory();
    }
    
    /**
     * Procesa un regalo con entrega directa (proceso tradicional)
     */
    private void processDirectGift(Player sender, Player receiver, GiftItem gift) {
        debugLogger.debug("Processing direct gift: " + gift.getName());
        
        // Eliminar los items requeridos del inventario
        debugLogger.debug("Removing required items for gift: " + gift.getId());
        for (GiftItem.ItemRequirement req : gift.getRequiredItems()) {
            debugLogger.debug("  Requiring: " + req.getAmount() + "x " + req.getMaterial());
        }
        giftManager.removeRequiredItems(sender, gift);
        
        // Agregar puntos de amistad
        String senderUUID = sender.getUniqueId().toString();
        String receiverUUID = receiver.getUniqueId().toString();
        int points = gift.getPoints();
        
        debugLogger.debug("Adding friendship points: " + points + " (base points)");
        int finalPoints = friendshipManager.addFriendshipPoints(senderUUID, receiverUUID, points);
        debugLogger.debug("Final points awarded: " + finalPoints + " (after boost)");
        
        // Guardar en historial
        String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', gift.getName()));
        plugin.getDatabaseManager().saveGiftHistory(senderUUID, receiverUUID, giftName, finalPoints);
        
        // Incrementar contador diario
        plugin.getDatabaseManager().incrementDailyGiftCount(senderUUID);
        
        // Establecer cooldown
        giftManager.setCooldown(sender);
        
        // Entregar puntos de recompensa directamente (simulando los items)
        // En un sistema real, aquí se crearían los items basados en el regalo
        // Por ahora, solo damos los puntos como recompensa
        
        // Agregar puntos personales al receptor si está configurado
        boolean dualPoints = plugin.getConfigManager().getMainConfig().getBoolean("settings.dual_personal_points", true);
        boolean boostDual = plugin.getConfigManager().getMainConfig().getBoolean("settings.boost_dual_personal_points", true);
        
        if (dualPoints) {
            // Aplicar boost si está habilitado
            int receiverPoints = points;
            if (boostDual) {
                // Aquí iría la lógica de boost si fuera necesario
                // Por simplicidad, usamos los puntos directamente
            }
            plugin.getDatabaseManager().addPersonalPoints(receiverUUID, receiverPoints);
        }
        
        // Enviar mensajes de confirmación usando la configuración
        String giftSentMessage = plugin.getMessage("messages.gift_sent", 
            "{prefix}&a✅ Has enviado un regalo de &f{gift} &a({points} puntos) a &f{receiver}");
        giftSentMessage = giftSentMessage.replace("{gift}", ChatColor.stripColor(gift.getName()))
                                         .replace("{points}", String.valueOf(finalPoints))
                                         .replace("{receiver}", receiver.getName());
        
        String giftReceivedMessage = plugin.getMessage("messages.gift_received",
            "{prefix}&a🎉 Has recibido un regalo de &f{gift} &a({points} puntos) de &f{sender}");
        giftReceivedMessage = giftReceivedMessage.replace("{gift}", ChatColor.stripColor(gift.getName()))
                                                .replace("{points}", String.valueOf(finalPoints))
                                                .replace("{sender}", sender.getName());
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', giftSentMessage));
        receiver.sendMessage(ChatColor.translateAlternateColorCodes('&', giftReceivedMessage));
        
        debugLogger.info("[DIRECT] Gift sent successfully from " + sender.getName() + " to " + receiver.getName());
        
        sender.closeInventory();
    }
    
    /**
     * Crea items ItemStack a partir de los requerimientos del regalo
     */
    private List<ItemStack> createItemsFromRequirements(List<GiftItem.ItemRequirement> requirements) {
        List<ItemStack> items = new ArrayList<>();
        for (GiftItem.ItemRequirement req : requirements) {
            ItemStack item = new ItemStack(req.getMaterial(), req.getAmount());
            items.add(item);
        }
        return items;
    }
    
    /**
     * Crea items compartidos aplicando el porcentaje
     */
    private List<ItemStack> createSharedItems(List<ItemStack> originalItems, int percentage) {
        List<ItemStack> sharedItems = new ArrayList<>();
        for (ItemStack item : originalItems) {
            int sharedAmount = (int) Math.max(1, Math.round(item.getAmount() * (percentage / 100.0)));
            if (sharedAmount > 0) {
                ItemStack sharedItem = item.clone();
                sharedItem.setAmount(sharedAmount);
                sharedItems.add(sharedItem);
            }
        }
        return sharedItems;
    }
    
    /**
     * Calcula el costo aproximado de un regalo basado en sus items requeridos
     */
    private int calculateGiftCost(GiftItem gift) {
        int totalCost = 0;
        for (GiftItem.ItemRequirement req : gift.getRequiredItems()) {
            // Asignar valores aproximados a los items comunes
            switch (req.getMaterial()) {
                case DIAMOND -> totalCost += req.getAmount() * 100;
                case GOLD_INGOT -> totalCost += req.getAmount() * 50;
                case IRON_INGOT -> totalCost += req.getAmount() * 25;
                case EMERALD -> totalCost += req.getAmount() * 75;
                case LAPIS_LAZULI -> totalCost += req.getAmount() * 10;
                case REDSTONE -> totalCost += req.getAmount() * 5;
                case COAL -> totalCost += req.getAmount() * 1;
                default -> {
                    // Para otros items, asignar un valor básico
                    totalCost += req.getAmount() * 10;
                }
            }
        }
        return totalCost;
    }
    
    /**
     * Obtiene el nombre mostrado de un ítem de forma segura
     * @param item El ItemStack del cual obtener el nombre
     * @return El nombre mostrado o el nombre del tipo si no tiene metadata
     */
    private String getItemDisplayName(ItemStack item) {
        if (item == null) {
            return "NULL_ITEM";
        }
        
        if (!item.hasItemMeta()) {
            return item.getType().name();
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return item.getType().name();
        }
        
        return meta.getDisplayName();
    }
    
    private GiftItem findGiftByName(String name) {
        if (name == null) {
            return null;
        }
        
        // Primero intentar búsqueda exacta
        for (GiftItem gift : giftManager.getAllGifts()) {
            String giftName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', gift.getName()));
            if (giftName != null && giftName.equals(name)) {
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