package com.fredygraces.giftbond.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;

import com.fredygraces.giftbond.GiftBond;

/**
 * Filtro de items para el sistema de regalos aleatorios
 * Filtra items de creativo, spawners, spawn eggs, etc.
 */
public class ItemFilter {
    
    private final GiftBond plugin;
    private final VersionDetector versionDetector;
    private final Set<Material> blacklist;
    
    public ItemFilter(GiftBond plugin, VersionDetector version) {
        this.plugin = plugin;
        this.versionDetector = version;
        this.blacklist = buildBlacklist();
    }
    
    /**
     * Construye la blacklist de items no permitidos
     */
    private Set<Material> buildBlacklist() {
        Set<Material> tempBlacklist = new HashSet<>();
        
        // Cargar blacklist del config
        List<String> configBlacklist = plugin.getConfigManager().getGiftsConfig().getStringList("auto_mode.exclude_items");
        
        for (String itemName : configBlacklist) {
            // Wildcard para spawn eggs
            if (itemName.equals("*_SPAWN_EGG")) {
                for (Material mat : Material.values()) {
                    if (mat.name().endsWith("_SPAWN_EGG")) {
                        tempBlacklist.add(mat);
                    }
                }
                continue;
            }
            
            // Item específico
            try {
                Material mat = Material.valueOf(itemName.toUpperCase());
                tempBlacklist.add(mat);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Item desconocido en blacklist: " + itemName);
            }
        }
        
        // plugin.getLogger().info("Items en blacklist: " + tempBlacklist.size());
        return tempBlacklist;
    }
    
    /**
     * Obtiene la lista de items válidos para regalos
     */
    public List<Material> getValidItems() {
        List<Material> validItems = new ArrayList<>();
        
        for (Material mat : Material.values()) {
            // Verificar que sea un item (no bloque técnico)
            if (!mat.isItem()) continue;
            
            // Verificar compatibilidad de versión
            if (!isMaterialCompatibleWithVersion(mat)) continue;
            
            // Verificar blacklist
            if (blacklist.contains(mat)) continue;
            
            // Verificar categoría permitida
            ItemCategory category = getCategory(mat);
            if (!isCategoryAllowed(category)) continue;
            
            validItems.add(mat);
        }
        
        // plugin.getLogger().info("Items válidos para regalos: " + validItems.size() + " de " + Material.values().length + " totales");
        return validItems;
    }
    
    /**
     * Determina la categoría de un item
     */
    public ItemCategory getCategory(Material mat) {
        String name = mat.name();
        
        // Comida
        if (mat.isEdible()) return ItemCategory.FOOD;
        
        // Herramientas
        if (name.contains("_PICKAXE") || name.contains("_AXE") || 
            name.contains("_SHOVEL") || name.contains("_HOE")) {
            return ItemCategory.TOOLS;
        }
        
        // Armas
        if (name.contains("_SWORD") || name.contains("BOW") || 
            name.equals("TRIDENT") || name.equals("CROSSBOW")) {
            return ItemCategory.WEAPONS;
        }
        
        // Armadura
        if (name.contains("_HELMET") || name.contains("_CHESTPLATE") ||
            name.contains("_LEGGINGS") || name.contains("_BOOTS")) {
            return ItemCategory.ARMOR;
        }
        
        // Recursos (lingotes, gemas, minerales)
        if (name.contains("_INGOT") || name.contains("_ORE") ||
            name.equals("DIAMOND") || name.equals("EMERALD") ||
            name.equals("COAL") || name.equals("REDSTONE") ||
            name.equals("LAPIS_LAZULI") || name.equals("QUARTZ") ||
            name.contains("_NUGGET") || name.contains("RAW_")) {
            return ItemCategory.RESOURCES;
        }
        
        // Pociones
        if (name.contains("POTION") || name.equals("GLASS_BOTTLE") ||
            name.equals("BREWING_STAND") || name.contains("_EYE") ||
            name.equals("BLAZE_POWDER") || name.equals("MAGMA_CREAM")) {
            return ItemCategory.POTIONS;
        }
        
        // Plantas
        if (name.contains("SAPLING") || name.contains("FLOWER") ||
            name.contains("SEEDS") || name.equals("WHEAT") ||
            name.contains("_LEAVES") || name.equals("FERN") ||
            name.equals("GRASS") || name.contains("VINE")) {
            return ItemCategory.PLANTS;
        }
        
        // Bloques
        if (mat.isBlock()) return ItemCategory.BLOCKS;
        
        return ItemCategory.MISC;
    }
    
    /**
     * Verifica si un material es compatible con la versión del servidor
     * 
     * @param mat Material a verificar
     * @return true si es compatible con la versión actual
     */
    private boolean isMaterialCompatibleWithVersion(Material mat) {
        String materialName = mat.name();
        
        // Para versiones inferiores a 1.20.4, excluir materiales introducidos después
        if (!versionDetector.isAtLeast1_20_4()) {
            // Materiales introducidos en 1.20.4 o posteriores
            if (materialName.startsWith("GOAT_HORN") ||
                materialName.startsWith("WIND_CHARGE") ||
                materialName.startsWith("BUCKET_OF_")) {
                return false;
            }
        }
        
        // Para versiones inferiores a 1.21, excluir materiales introducidos después
        if (!versionDetector.isAtLeast1_21()) {
            // Materiales introducidos en 1.21 o posteriores
            if (materialName.startsWith("VAULT") ||
                materialName.startsWith("OMINOUS_BOTTLE") ||
                materialName.startsWith("OMINOUS_CATALYST") ||
                materialName.startsWith("BREEZE_ROD") ||
                materialName.startsWith("WIND_CHARGE")) {
                return false;
            }
        }
        
        // Verificar que no exceda el límite superior (1.21.11)
        if (!versionDetector.isAtMost1_21_11()) {
            // Si la versión es superior a 1.21.11, podríamos tener que manejar materiales futuros
            // Por ahora, simplemente aceptamos todos los materiales conocidos
        }
        
        return true;
    }
    
    /**
     * Verifica si una categoría está permitida
     */
    private boolean isCategoryAllowed(ItemCategory category) {
        return plugin.getConfigManager().getGiftsConfig().getBoolean(
            "auto_mode.allowed_categories." + category.name().toLowerCase(), 
            true
        );
    }
    
    /**
     * Obtiene el nombre legible de un material
     */
    public String getFriendlyName(Material mat) {
        // Convertir COOKED_BEEF a "Cooked Beef"
        String name = mat.name().toLowerCase().replace("_", " ");
        // Capitalizar cada palabra
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
    
    /**
     * Categorías de items
     */
    public enum ItemCategory {
        FOOD,      // Comida
        BLOCKS,    // Bloques
        RESOURCES, // Recursos (lingotes, minerales)
        TOOLS,     // Herramientas
        WEAPONS,   // Armas
        ARMOR,     // Armaduras
        POTIONS,   // Pociones
        PLANTS,    // Plantas
        MISC       // Otros
    }
}
