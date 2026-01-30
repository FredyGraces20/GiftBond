package com.fredygraces.giftbond.menus;

import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.GiftManager;
import com.fredygraces.giftbond.models.GiftItem;
import com.fredygraces.giftbond.utils.GiftSessionManager;
import com.fredygraces.giftbond.utils.RandomGiftGenerator;

public class GiftMenu {
    private final GiftBond plugin;
    private final GiftManager giftManager;

    public GiftMenu(GiftBond plugin) {
        this.plugin = plugin;
        this.giftManager = plugin.getGiftManager();
    }

    public void openGiftMenu(Player sender, Player receiver) {
        // Iniciar sesión de regalo para el destinatario
        GiftSessionManager.getInstance().startGiftSession(sender, receiver.getName());

        String title = getDefaultTitle(receiver.getName());
        
        // En modo auto, mostrar información de rotación en el título
        if (giftManager.isAutoMode()) {
            RandomGiftGenerator generator = giftManager.getRandomGiftGenerator();
            if (generator != null) {
                String timeLeft = generator.getTimeUntilNextRotationFormatted();
                title = getAutoTitle(timeLeft);
            }
        } else {
            // Modo manual
            title = getManualTitle(receiver.getName());
        }
        
        // En modo auto, el menú tiene 5 filas (45 slots)
        int size = giftManager.isAutoMode() ? 45 : 27;
        Inventory inventory = Bukkit.createInventory(null, size, title);
        
        if (giftManager.isAutoMode()) {
            setupAutoGiftMenu(inventory, sender);
        } else {
            setupManualGiftMenu(inventory, sender);
        }
        
        sender.openInventory(inventory);
    }

    /**
     * Configura el menú en modo automático con items, dinero y fillers
     */
    private void setupAutoGiftMenu(Inventory inventory, Player sender) {
        RandomGiftGenerator generator = giftManager.getRandomGiftGenerator();
        if (generator == null) return;

        // 1. Llenar filas de fillers (0-8 y 18-26)
        ItemStack filler = createRandomFiller();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, filler);
            inventory.setItem(i + 18, filler);
        }
        
        // 2. Llenar última fila con fillers y reloj (36-44)
        for (int i = 36; i < 45; i++) {
            if (i == 40) {
                inventory.setItem(i, createConfigurableRotationInfoItem());
            } else {
                inventory.setItem(i, filler);
            }
        }

        // 3. Colocar items de regalo (9-17)
        Collection<GiftItem> gifts = giftManager.getAllGifts();
        int itemIndex = 9;
        for (GiftItem gift : gifts) {
            if (itemIndex > 17) break;
            
            ItemStack item;
            if (giftManager.hasRequiredItems(sender, gift)) {
                item = createAvailableGiftItem(sender, getDisplayMaterial(gift), gift);
            } else {
                item = createUnavailableGiftItem(sender, gift);
            }
            inventory.setItem(itemIndex++, item);
        }

        // 4. Colocar botones de dinero (27-35) con verificación de saldo
        List<RandomGiftGenerator.RandomMoneyGift> moneyGifts = generator.getCurrentMoneyGifts();
        int moneyIndex = 27;
        for (int i = 0; i < moneyGifts.size() && moneyIndex <= 35; i++) {
            RandomGiftGenerator.RandomMoneyGift moneyGift = moneyGifts.get(i);
            
            // Verificar si el botón está habilitado
            String buttonPath = "auto_mode.money_gifts.button_" + (i + 1);
            boolean enabled = plugin.getConfigManager().getGiftsConfig().getBoolean(buttonPath + ".enabled", true);
            
            if (!enabled || moneyGift.getAmount() <= 0) {
                // Botón deshabilitado - mostrar barrera
                inventory.setItem(moneyIndex++, createDisabledMoneyButton(i + 1));
                continue;
            }
            
            // Verificar si el jugador tiene suficiente dinero
            if (plugin.getEconomyManager().hasEnoughMoney(sender, moneyGift.getAmount())) {
                inventory.setItem(moneyIndex++, createMoneyGiftItem(sender, moneyGift, i + 1));
            } else {
                inventory.setItem(moneyIndex++, createInsufficientFundsButton(sender, moneyGift, i + 1));
            }
        }
    }

    /**
     * Configura el menú en modo manual (comportamiento original)
     */
    private void setupManualGiftMenu(Inventory inventory, Player sender) {
        // Crear items de regalos basados en la configuración
        ItemStack[] giftItems = createConfigurableGiftItems(sender);
        
        // Añadir items al inventario
        for (int i = 0; i < giftItems.length && i < 27; i++) {
            inventory.setItem(i, giftItems[i]);
        }
        
        // Agregar item de información si está habilitado
        if (isRotationInfoEnabled()) {
            ItemStack infoItem = createConfigurableRotationInfoItem();
            int slot = getRotationInfoSlot();
            if (slot >= 0 && slot < 27) {
                inventory.setItem(slot, infoItem);
            } else {
                inventory.setItem(26, infoItem);
            }
        }
    }

    /**
     * Crea un item que representa un regalo de dinero
     */
    private ItemStack createMoneyGiftItem(Player sender, RandomGiftGenerator.RandomMoneyGift moneyGift, int buttonNumber) {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        
        double multiplier = plugin.getFriendshipManager().getActiveMultiplier(sender.getUniqueId().toString());
        int basePoints = moneyGift.getPoints();
        int finalPoints = (int) (basePoints * multiplier);
        
        // Calcular porcentaje compartido para el lore
        int sharedMoneyPercentage = plugin.getConfigManager().getMainConfig().getInt("mailbox.shared_money_percentage", 50);
        double costAmount = moneyGift.getAmount();
        double receiverAmount = costAmount * (sharedMoneyPercentage / 100.0);
        
        if (meta != null) {
            // Obtener nombre configurado del botón
            String displayName = plugin.getConfigManager().getGiftsConfig()
                .getString("auto_mode.money_gifts.button_" + buttonNumber + ".name", "§6§l🎁 Regalo de Dinero");
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            List<String> lore = new java.util.ArrayList<>();
            lore.add("§e§lInformación del envío");
            lore.add("§7------------------------");
            lore.add("§fCosto: §a$" + String.format("%,d", (int)costAmount));
            
            if (multiplier > 1.0) {
                lore.add("§fRecibe: §a$" + String.format("%,d", (int)receiverAmount));
                lore.add("§fPuntos: §a" + basePoints);
                lore.add("§7------------------------");
                lore.add("§fBoost: §b" + String.format("%.1f", multiplier) + "x");
                lore.add("§fPuntos: §a" + finalPoints);
            } else {
                lore.add("§fRecibe: §a$" + String.format("%,d", (int)receiverAmount));
                lore.add("§fPuntos: §a" + basePoints);
                lore.add("§7------------------------");
            }
            
            lore.add("");
            lore.add("§eHaz clic para enviar este");
            lore.add("§eregalo de dinero.");
            lore.add("§7━━━━━━━━━━━━━━━━━━━━");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Crea un botón de dinero deshabilitado (barrera)
     */
    private ItemStack createDisabledMoneyButton(int buttonNumber) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = plugin.getConfigManager().getGiftsConfig()
                .getString("auto_mode.money_gifts.button_" + buttonNumber + ".name", "§c§lBotón Deshabilitado");
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            
            List<String> lore = new java.util.ArrayList<>();
            lore.add("§cEste botón está deshabilitado");
            lore.add("§7Contacta a un administrador");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Crea un botón que indica fondos insuficientes
     */
    private ItemStack createInsufficientFundsButton(Player sender, RandomGiftGenerator.RandomMoneyGift moneyGift, int buttonNumber) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = plugin.getConfigManager().getGiftsConfig()
                .getString("auto_mode.money_gifts.button_" + buttonNumber + ".name", "§c§lFondos Insuficientes");
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName));
            
            List<String> lore = new java.util.ArrayList<>();
            lore.add("§cNo tienes suficiente dinero");
            lore.add("§7Costo: §f$" + String.format("%,d", moneyGift.getAmount()));
            lore.add("§7Tu saldo: §f$" + getFormattedIntegerBalance(sender));
            lore.add("");
            lore.add("§7Consigue más dinero para");
            lore.add("§7desbloquear este regalo");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Obtiene el balance formateado del jugador (números enteros)
     */
    private String getFormattedIntegerBalance(Player player) {
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            String balanceStr = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%vault_eco_balance%");
            try {
                String cleanBalance = balanceStr.replaceAll("[^0-9.,]", "").replace(",", "");
                double balance = Double.parseDouble(cleanBalance);
                return String.format("%,d", (int)balance);
            } catch (NumberFormatException e) {
                return "0";
            }
        }
        return "0";
    }

    /**
     * Crea un panel de cristal de color aleatorio para relleno
     */
    private ItemStack createRandomFiller() {
        Material[] colors = {
            Material.WHITE_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.MAGENTA_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE,
            Material.PURPLE_STAINED_GLASS_PANE, Material.BLUE_STAINED_GLASS_PANE,
            Material.BROWN_STAINED_GLASS_PANE, Material.GREEN_STAINED_GLASS_PANE,
            Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE
        };
        
        Material randomColor = colors[new java.util.Random().nextInt(colors.length)];
        ItemStack filler = new ItemStack(randomColor);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            filler.setItemMeta(meta);
        }
        return filler;
    }
    
    /**
     * Crea el item de información de rotación (solo en modo auto)
     */
    private ItemStack createDefaultRotationInfoItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l⏰ Rotación de Regalos");
        }
        
        List<String> lore = new java.util.ArrayList<>();
        lore.add("§7Los regalos cambian automáticamente");
        lore.add("§7cada cierto tiempo.");
        lore.add("");
        
        RandomGiftGenerator generator = giftManager.getRandomGiftGenerator();
        if (generator != null) {
            lore.add("§ePróximo cambio: §f" + generator.getTimeUntilNextRotationFormatted());
        }
        
        lore.add("");
        lore.add("§7¡Aprovecha los regalos actuales!");
        
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Método que utiliza el plugin para acceder a configuraciones o funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
    
    // Métodos de configuración para personalización del menú
    
    private String getDefaultTitle(String receiverName) {
        return "Enviar regalo a " + receiverName;
    }
    
    private String getManualTitle(String receiverName) {
        String configTitle = plugin.getConfigManager().getGiftsConfig()
            .getString("menu_customization.manual_title", "&d🎁 Enviar regalo a {receiver}");
        if (configTitle == null) {
            configTitle = "&d🎁 Enviar regalo a {receiver}";
        }
        return ChatColor.translateAlternateColorCodes('&', 
            configTitle.replace("{receiver}", receiverName));
    }
    
    private String getAutoTitle(String timeLeft) {
        String configTitle = plugin.getConfigManager().getGiftsConfig()
            .getString("menu_customization.auto_title", "&d🎁 Regalos - Rotan en {time_left}");
        if (configTitle == null) {
            configTitle = "&d🎁 Regalos - Rotan en {time_left}";
        }
        return ChatColor.translateAlternateColorCodes('&', 
            configTitle.replace("{time_left}", timeLeft));
    }
    
    private boolean isRotationInfoEnabled() {
        return plugin.getConfigManager().getGiftsConfig()
            .getBoolean("menu_customization.rotation_info_item.enabled", true);
    }
    
    private int getRotationInfoSlot() {
        return plugin.getConfigManager().getGiftsConfig()
            .getInt("menu_customization.rotation_info_item.slot", 26);
    }
    
    private ItemStack createConfigurableRotationInfoItem() {
        org.bukkit.configuration.ConfigurationSection config = 
            plugin.getConfigManager().getGiftsConfig()
            .getConfigurationSection("menu_customization.rotation_info_item");
        
        if (config == null) {
            // Fallback to default implementation
            return createDefaultRotationInfoItem();
        }
        
        String materialStr = config.getString("material", "CLOCK");
        String name = config.getString("name", "&e&l⏰ Rotación de Regalos");
        java.util.List<String> loreLines = config.getStringList("lore");
        
        Material material = null;
        if (materialStr != null) {
            material = Material.getMaterial(materialStr.toUpperCase());
        }
        if (material == null) {
            material = Material.CLOCK; // Fallback
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = name != null ? name : "&e&l⏰ Rotación de Regalos";
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            
            java.util.List<String> lore = new java.util.ArrayList<>();
            RandomGiftGenerator generator = giftManager.getRandomGiftGenerator();
            String timeLeft = generator != null ? generator.getTimeUntilNextRotationFormatted() : "--:--";
            
            for (String line : loreLines) {
                String processedLine = line.replace("{time_left}", timeLeft);
                lore.add(ChatColor.translateAlternateColorCodes('&', processedLine));
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    private ItemStack[] createConfigurableGiftItems(Player sender) {
        if (sender == null || giftManager == null) {
            return new ItemStack[0]; // Safe fallback
        }
        
        Collection<GiftItem> gifts = giftManager.getAllGifts();
        if (gifts == null) {
            return new ItemStack[0];
        }
        
        ItemStack[] items = new ItemStack[gifts.size()];
        
        int index = 0;
        for (GiftItem gift : gifts) {
            if (gift == null) continue; // Skip null gifts
            
            Material displayMaterial = getDisplayMaterial(gift);
            ItemStack item;
            
            // Verificar si el jugador tiene los items requeridos
            if (giftManager.hasRequiredItems(sender, gift)) {
                item = createAvailableGiftItem(sender, displayMaterial, gift);
            } else {
                item = createUnavailableGiftItem(sender, gift);
            }
            
            if (item != null && index < items.length) {
                items[index++] = item;
            }
        }
        
        return items;
    }
    
    private Material getDisplayMaterial(GiftItem gift) {
        if (gift == null) {
            return Material.STONE; // Safe fallback
        }
        
        // En modo auto, usar el material real del regalo
        if (giftManager.isAutoMode()) {
            if (gift.getRequiredItems() != null && !gift.getRequiredItems().isEmpty()) {
                return gift.getRequiredItems().get(0).getMaterial();
            }
        }
        
        // Modo manual: mapear tipos de regalo a materiales visuales
        String giftId = gift.getId();
        if (giftId == null) {
            return Material.STONE;
        }
        
        return switch (giftId) {
            case "friendship_points" -> Material.PAPER;
            case "heart" -> Material.RED_DYE;
            case "flowers" -> Material.POPPY;
            case "food" -> Material.COOKED_BEEF;
            case "bow" -> Material.BOW;
            case "diamond" -> Material.DIAMOND;
            case "book" -> Material.BOOK;
            case "cake" -> Material.CAKE;
            case "chest" -> Material.CHEST;
            default -> Material.STONE;
        };
    }
    
    private ItemStack createAvailableGiftItem(Player sender, Material material, GiftItem gift) {
        if (gift == null || material == null) {
            return new ItemStack(Material.BARRIER); // Safe fallback
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String giftName = gift.getName();
            if (giftName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', giftName));
            }
        }
        
        double multiplier = plugin.getFriendshipManager().getActiveMultiplier(sender.getUniqueId().toString());
        int basePoints = gift.getPoints();
        int finalPoints = (int) (basePoints * multiplier);

        // Crear lore con información detallada de envío
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§e§lInformación del envío");
        lore.add("§7------------------------");
        
        // Mostrar costo en dinero si existe
        if (gift.getMoneyRequired() > 0) {
            lore.add("§fCosto: §a$" + String.format("%,.2f", gift.getMoneyRequired()));
        }

        // Mostrar items requeridos si existen
        if (!gift.getRequiredItems().isEmpty()) {
            for (com.fredygraces.giftbond.models.GiftItem.ItemRequirement req : gift.getRequiredItems()) {
                String reqName = req.getMaterial().name().replace("_", " ").toLowerCase();
                lore.add("§fItem: §a" + req.getAmount() + "x " + reqName);
            }
        }
        
        // Mostrar cantidad que recibe la otra persona para items
        if (!gift.getRequiredItems().isEmpty()) {
            // Calcular cantidad que recibe basado en configuración
            int sharedItemPercentage = plugin.getConfigManager().getMainConfig().getInt("mailbox.shared_items_percentage", 50);
            for (com.fredygraces.giftbond.models.GiftItem.ItemRequirement req : gift.getRequiredItems()) {
                int receiverAmount = (int) Math.floor(req.getAmount() * (sharedItemPercentage / 100.0));
                if (receiverAmount > 0) {
                    String reqName = req.getMaterial().name().replace("_", " ").toLowerCase();
                    lore.add("§fRecibe: §a" + receiverAmount + "x " + reqName);
                }
            }
        }
        
        lore.add("§fPuntos: §a" + basePoints);
        lore.add("§7------------------------");
        
        if (multiplier > 1.0) {
            lore.add("§fBoost: §b" + String.format("%.1f", multiplier) + "x");
            lore.add("§fPuntos: §a" + finalPoints);
        }
        
        lore.add("");
        
        // Añadir descripción original (QUITADO - Limpieza de lore solicitada)
        /*
        if (!gift.getId().startsWith("random_")) {
            for (String line : gift.getDescription()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            if (!gift.getDescription().isEmpty()) {
                lore.add("");
            }
        }
        */

        lore.add("§a✓ Disponible");
        lore.add("§7Clic para enviar este regalo");
        lore.add("§7━━━━━━━━━━━━━━━━━━━━");
        
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createUnavailableGiftItem(Player sender, GiftItem gift) {
        if (gift == null) {
            return new ItemStack(Material.BARRIER); // Safe fallback
        }
            
        ItemStack item = new ItemStack(Material.BARRIER); // Usar barrera para indicar no disponible
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String giftName = gift.getName();
            String displayName = giftName != null ? 
                "&c" + giftName + " &7(Bloqueado)" : 
                "&cGift &7(Bloqueado)";
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        }
            
        double multiplier = plugin.getFriendshipManager().getActiveMultiplier(sender.getUniqueId().toString());
        int basePoints = gift.getPoints();
        int finalPoints = (int) (basePoints * multiplier);
    
        // Crear lore con información detallada
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("§e§lInformación del envío");
        lore.add("§7------------------------");
        
        // Mostrar costo en dinero si existe
        if (gift.getMoneyRequired() > 0) {
            lore.add("§fCosto: §a$" + String.format("%,.2f", gift.getMoneyRequired()));
        }

        // Mostrar items requeridos si existen
        if (!gift.getRequiredItems().isEmpty()) {
            for (com.fredygraces.giftbond.models.GiftItem.ItemRequirement req : gift.getRequiredItems()) {
                String reqName = req.getMaterial().name().replace("_", " ").toLowerCase();
                lore.add("§fItem: §a" + req.getAmount() + "x " + reqName);
            }
        }
        
        // Mostrar cantidad que recibe la otra persona para items
        if (!gift.getRequiredItems().isEmpty()) {
            // Calcular cantidad que recibe basado en configuración
            int sharedItemPercentage = plugin.getConfigManager().getMainConfig().getInt("mailbox.shared_items_percentage", 50);
            for (com.fredygraces.giftbond.models.GiftItem.ItemRequirement req : gift.getRequiredItems()) {
                int receiverAmount = (int) Math.floor(req.getAmount() * (sharedItemPercentage / 100.0));
                if (receiverAmount > 0) {
                    String reqName = req.getMaterial().name().replace("_", " ").toLowerCase();
                    lore.add("§fRecibe: §a" + receiverAmount + "x " + reqName);
                }
            }
        }
                
        lore.add("§fPuntos: §a" + basePoints);
        lore.add("§7------------------------");
        
        if (multiplier > 1.0) {
            lore.add("§fBoost: §b" + String.format("%.1f", multiplier) + "x");
            lore.add("§fPuntos: §a" + finalPoints);
        }
        
        lore.add("");
    
        // Añadir descripción original (QUITADO - Limpieza de lore solicitada)
        /*
        if (!gift.getId().startsWith("random_")) {
            for (String line : gift.getDescription()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            if (!gift.getDescription().isEmpty()) {
                lore.add("");
            }
        }
        */
    
        lore.add("§c✗ No tienes los requisitos necesarios");
        lore.add("§7Consigue los items o el dinero para desbloquear");
        lore.add("§7━━━━━━━━━━━━━━━━━━━━");
            
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}