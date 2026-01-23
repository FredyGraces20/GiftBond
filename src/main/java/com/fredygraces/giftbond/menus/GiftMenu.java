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
import com.fredygraces.giftbond.utils.RandomGiftGenerator;

public class GiftMenu {
    private final GiftBond plugin;
    private final GiftManager giftManager;

    public GiftMenu(GiftBond plugin) {
        this.plugin = plugin;
        this.giftManager = plugin.getGiftManager();
    }

    public void openGiftMenu(Player sender, Player receiver) {
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
        
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        // Crear items de regalos basados en la configuración
        ItemStack[] giftItems = createConfigurableGiftItems(sender);
        
        // Añadir items al inventario
        for (int i = 0; i < giftItems.length && i < 27; i++) {
            inventory.setItem(i, giftItems[i]);
        }
        
        // En modo auto, agregar item de información en el último slot
        if (giftManager.isAutoMode() && isRotationInfoEnabled()) {
            ItemStack infoItem = createConfigurableRotationInfoItem();
            int slot = getRotationInfoSlot();
            if (slot >= 0 && slot < 27) {
                inventory.setItem(slot, infoItem);
            } else {
                inventory.setItem(26, infoItem); // Fallback al último slot
            }
        }
        
        sender.openInventory(inventory);
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
        boolean hasBoost = multiplier > 1.0;

        // Crear lore con descripción y costo
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (String line : gift.getDescription()) {
            String translated = ChatColor.translateAlternateColorCodes('&', line);
            if (hasBoost && translated.contains("Premio:")) {
                int finalPoints = (int) (gift.getPoints() * multiplier);
                translated += ChatColor.GREEN + " (" + finalPoints + ")";
            }
            lore.add(translated);
        }

        if (hasBoost) {
            lore.add(ChatColor.GREEN + "Boost Activo x" + multiplier);
        }

        lore.add("");
        lore.add("§a✓ Disponible");
        lore.add("§7Clic para enviar este regalo");
        
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
        boolean hasBoost = multiplier > 1.0;

        // Crear lore con descripción y requisitos faltantes
        java.util.List<String> lore = new java.util.ArrayList<>();
        for (String line : gift.getDescription()) {
            String translated = ChatColor.translateAlternateColorCodes('&', line);
            if (hasBoost && translated.contains("Premio:")) {
                int finalPoints = (int) (gift.getPoints() * multiplier);
                translated += ChatColor.GREEN + " (" + finalPoints + ")";
            }
            lore.add(translated);
        }

        if (hasBoost) {
            lore.add(ChatColor.GREEN + "Boost Activo x" + multiplier);
        }

        lore.add("");
        lore.add("§c✗ No tienes los items necesarios");
        lore.add("§7Consigue los items requeridos para desbloquear");
        
        if (meta != null) {
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}