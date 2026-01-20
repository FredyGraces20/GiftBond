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
        String title = "Enviar regalo a " + receiver.getName();
        
        // En modo auto, mostrar información de rotación en el título
        if (giftManager.isAutoMode()) {
            RandomGiftGenerator generator = giftManager.getRandomGiftGenerator();
            if (generator != null) {
                String timeLeft = generator.getTimeUntilNextRotationFormatted();
                title = "Regalos - Rotan en " + timeLeft;
            }
        }
        
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        // Crear items de regalos basados en la configuración
        ItemStack[] giftItems = createConfigurableGiftItems(sender, receiver);
        
        // Añadir items al inventario
        for (int i = 0; i < giftItems.length && i < 27; i++) {
            inventory.setItem(i, giftItems[i]);
        }
        
        // En modo auto, agregar item de información en el último slot
        if (giftManager.isAutoMode()) {
            ItemStack infoItem = createRotationInfoItem();
            inventory.setItem(26, infoItem);  // Último slot
        }
        
        sender.openInventory(inventory);
    }
    
    /**
     * Crea el item de información de rotación (solo en modo auto)
     */
    private ItemStack createRotationInfoItem() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l⏰ Rotación de Regalos");
        
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
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack[] createConfigurableGiftItems(Player sender, Player receiver) {
        Collection<GiftItem> gifts = giftManager.getAllGifts();
        ItemStack[] items = new ItemStack[gifts.size()];
        
        int index = 0;
        for (GiftItem gift : gifts) {
            Material displayMaterial = getDisplayMaterial(gift);
            ItemStack item;
            
            // Verificar si el jugador tiene los items requeridos
            if (giftManager.hasRequiredItems(sender, gift)) {
                item = createAvailableGiftItem(sender, displayMaterial, gift);
            } else {
                item = createUnavailableGiftItem(sender, displayMaterial, gift);
            }
            
            items[index++] = item;
        }
        
        return items;
    }
    
    private Material getDisplayMaterial(GiftItem gift) {
        // En modo auto, usar el material real del regalo
        if (giftManager.isAutoMode()) {
            if (!gift.getRequiredItems().isEmpty()) {
                return gift.getRequiredItems().get(0).getMaterial();
            }
        }
        
        // Modo manual: mapear tipos de regalo a materiales visuales
        switch (gift.getId()) {
            case "friendship_points": return Material.PAPER;
            case "heart": return Material.RED_DYE;
            case "flowers": return Material.POPPY;
            case "food": return Material.COOKED_BEEF;
            case "bow": return Material.BOW;
            case "diamond": return Material.DIAMOND;
            case "book": return Material.BOOK;
            case "cake": return Material.CAKE;
            case "chest": return Material.CHEST;
            default: return Material.STONE;
        }
    }
    
    private ItemStack createAvailableGiftItem(Player sender, Material material, GiftItem gift) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', gift.getName()));
        
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
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createUnavailableGiftItem(Player sender, Material material, GiftItem gift) {
        ItemStack item = new ItemStack(Material.BARRIER); // Usar barrera para indicar no disponible
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c" + gift.getName() + " &7(Bloqueado)"));
        
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
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    // Método que utiliza el plugin para acceder a configuraciones o funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
}