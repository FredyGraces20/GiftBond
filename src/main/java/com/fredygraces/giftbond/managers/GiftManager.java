package com.fredygraces.giftbond.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.models.GiftItem;
import com.fredygraces.giftbond.utils.RandomGiftGenerator;

import me.clip.placeholderapi.PlaceholderAPI;

public class GiftManager {
    private final GiftBond plugin;
    private final Map<String, GiftItem> giftItems;
    private final Map<UUID, Long> cooldowns;
    private RandomGiftGenerator randomGiftGenerator;  // Sistema de regalos aleatorios
    private boolean autoMode;  // true = auto, false = manual

    public GiftManager(GiftBond plugin) {
        this.plugin = plugin;
        this.giftItems = new HashMap<>();
        this.cooldowns = new HashMap<>();
        this.autoMode = false;
        loadGiftsFromConfig();
    }
    
    /**
     * Establece el generador de regalos aleatorios (llamado desde GiftBond)
     */
    public void setRandomGiftGenerator(RandomGiftGenerator generator) {
        this.randomGiftGenerator = generator;
    }

    public void reload() {
        giftItems.clear();
        cooldowns.clear();
        loadGiftsFromConfig();
        
        // Si está en modo auto, recargar también el generador aleatorio
        if (autoMode && randomGiftGenerator != null) {
            randomGiftGenerator.reload();
        }
    }

    private void loadGiftsFromConfig() {
        // Detectar modo de operación
        String mode = plugin.getConfigManager().getGiftsConfig().getString("mode", "manual").toLowerCase();
        this.autoMode = mode.equals("auto");
        
        if (autoMode) {
            plugin.getLogger().info("⚙️  Modo AUTO activado - Regalos aleatorios");
            // Los regalos se generarán automáticamente por RandomGiftGenerator
            // No cargamos nada del config
        } else {
            plugin.getLogger().info("⚙️  Modo MANUAL activado - Regalos del config.yml");
            // Cargar regalos desde gifts.yml (modo tradicional)
            ConfigurationSection manualSection = plugin.getConfigManager().getGiftsConfig().getConfigurationSection("manual_mode.gifts");
            if (manualSection != null) {
                for (String giftId : manualSection.getKeys(false)) {
                    ConfigurationSection giftConfig = manualSection.getConfigurationSection(giftId);
                    if (giftConfig != null) {
                        GiftItem giftItem = new GiftItem(giftId, giftConfig);
                        giftItems.put(giftId, giftItem);
                    }
                }
                plugin.getLogger().info("  Regalos cargados: " + giftItems.size());
            } else {
                plugin.getLogger().warning("  No se encontró configuración de regalos manuales!");
            }
        }
    }
    
    /**
     * Obtiene todos los regalos disponibles
     * En modo AUTO: retorna los regalos aleatorios actuales
     * En modo MANUAL: retorna los regalos del config
     */
    public Collection<GiftItem> getAllGifts() {
        if (autoMode && randomGiftGenerator != null) {
            return randomGiftGenerator.getCurrentGiftsAsGiftItems();
        }
        return giftItems.values();
    }
    
    /**
     * Obtiene todos los regalos como lista (para GUI)
     */
    public List<GiftItem> getAllGiftsAsList() {
        return new ArrayList<>(getAllGifts());
    }

    public GiftItem getGiftById(String id) {
        if (autoMode && randomGiftGenerator != null) {
            // En modo auto, buscar en los regalos generados
            for (GiftItem gift : randomGiftGenerator.getCurrentGiftsAsGiftItems()) {
                if (gift.getId().equals(id)) {
                    return gift;
                }
            }
            return null;
        }
        return giftItems.get(id);
    }
    
    /**
     * Verifica si es modo automático
     */
    public boolean isAutoMode() {
        return autoMode;
    }
    
    /**
     * Obtiene el generador de regalos aleatorios
     */
    public RandomGiftGenerator getRandomGiftGenerator() {
        return randomGiftGenerator;
    }

    public boolean hasRequiredItems(Player player, GiftItem giftItem) {
        for (GiftItem.ItemRequirement requirement : giftItem.getRequiredItems()) {
            if (!hasEnoughItems(player, requirement.getMaterial(), requirement.getAmount())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEnoughItems(Player player, org.bukkit.Material material, int requiredAmount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count >= requiredAmount;
    }

    public void removeRequiredItems(Player player, GiftItem giftItem) {
        for (GiftItem.ItemRequirement requirement : giftItem.getRequiredItems()) {
            removeItems(player, requirement.getMaterial(), requirement.getAmount());
        }
    }

    public boolean isOnCooldown(Player player) {
        if (!plugin.getConfigManager().getMainConfig().getBoolean("settings.enabled", true)) return false;
        if (!cooldowns.containsKey(player.getUniqueId())) return false;
        long lastTime = cooldowns.get(player.getUniqueId());
        int cooldownSeconds = plugin.getConfigManager().getMainConfig().getInt("settings.gift_cooldown", 30);
        return (System.currentTimeMillis() - lastTime) < (cooldownSeconds * 1000L);
    }

    public int getRemainingCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) return 0;
        long lastTime = cooldowns.get(player.getUniqueId());
        int cooldownSeconds = plugin.getConfigManager().getMainConfig().getInt("settings.gift_cooldown", 30);
        long remainingMillis = (lastTime + (cooldownSeconds * 1000L)) - System.currentTimeMillis();
        return (int) Math.ceil(remainingMillis / 1000.0);
    }

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public boolean hasMinimumPlaytime(Player player) {
        // Debug logging
        boolean masterEnabled = plugin.getConfigManager().getMainConfig().getBoolean("settings.enabled", true);
        boolean hoursRequirementEnabled = plugin.getConfigManager().getMainConfig().getBoolean("settings.enable_min_hours_requirement", true);
        int minHours = plugin.getConfigManager().getMainConfig().getInt("settings.min_hours_played", 0);
        
        plugin.getLogger().info("[DEBUG] Checking hours requirement for " + player.getName() + ":");
        plugin.getLogger().info("[DEBUG]   Master enabled: " + masterEnabled);
        plugin.getLogger().info("[DEBUG]   Hours requirement enabled: " + hoursRequirementEnabled);
        plugin.getLogger().info("[DEBUG]   Min hours required: " + minHours);
        
        // Si las configuraciones principales están desactivadas, permitir todo
        if (!masterEnabled) {
            plugin.getLogger().info("[DEBUG] Master settings disabled - allowing gift");
            return true;
        }
        
        // Si el requisito de horas está desactivado, permitir
        if (!hoursRequirementEnabled) {
            plugin.getLogger().info("[DEBUG] Hours requirement disabled - allowing gift");
            return true;
        }
        
        if (minHours <= 0) {
            plugin.getLogger().info("[DEBUG] Min hours is " + minHours + " - allowing gift");
            return true;
        }

        // Verificar que PlaceholderAPI esté instalado
        boolean placeholderAPIEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        plugin.getLogger().info("[DEBUG] PlaceholderAPI enabled: " + placeholderAPIEnabled);
        
        if (!placeholderAPIEnabled) {
            plugin.getLogger().warning("PlaceholderAPI no encontrado - el requisito de horas será ignorado");
            return true; // Si no hay PlaceholderAPI, permitir por compatibilidad
        }

        String placeholder = plugin.getConfigManager().getMainConfig().getString("settings.hours_played_placeholder", "%statistic_hours_played%");
        String value = PlaceholderAPI.setPlaceholders(player, placeholder);
        
        plugin.getLogger().info("[DEBUG] Using placeholder: " + placeholder);
        plugin.getLogger().info("[DEBUG] Placeholder returned: '" + value + "'");
        
        try {
            int hours = Integer.parseInt(value);
            boolean meetsRequirement = hours >= minHours;
            
            // Log para debugging (solo en modo verbose)
            if (!meetsRequirement) {
                plugin.getLogger().fine("Jugador " + player.getName() + " no cumple requisito: " + hours + " < " + minHours + " horas");
            }
            
            return meetsRequirement;
        } catch (NumberFormatException e) {
            // Si el placeholder no devuelve un número válido
            plugin.getLogger().warning("Placeholder " + placeholder + " devolvió valor no numérico: '" + value + "' para jugador " + player.getName());
            plugin.getLogger().warning("Verifica que la expansión Statistic esté instalada: /papi ecloud download Statistic");
            return false; // Ser estricto: si no podemos verificar, denegar
        }
    }

    public int getPlayerHours(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return 0;
        }

        String placeholder = plugin.getConfigManager().getMainConfig().getString("settings.hours_played_placeholder", "%statistic_hours_played%");
        String value = PlaceholderAPI.setPlaceholders(player, placeholder);
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // Log para debugging
            plugin.getLogger().fine("No se pudo parsear horas para " + player.getName() + ": '" + value + "'");
            return 0;
        }
    }

    private void removeItems(Player player, org.bukkit.Material material, int amountToRemove) {
        int remaining = amountToRemove;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.updateInventory();
    }
}