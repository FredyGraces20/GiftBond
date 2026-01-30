package com.fredygraces.giftbond.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class GiftItem {
    private final String id;
    private final String name;
    private final int points;
    private final double moneyRequired;
    private final List<ItemRequirement> requiredItems;
    private final List<String> description;

    public GiftItem(String id, ConfigurationSection config) {
        this.id = id;
        this.name = config.getString("name", id);
        this.points = config.getInt("points", 0);
        this.moneyRequired = config.getDouble("money_required", 0.0);
        
        this.requiredItems = new ArrayList<>();
        List<Map<?, ?>> itemsList = config.getMapList("items_required");
        if (itemsList != null) {
            for (Map<?, ?> itemMap : itemsList) {
                try {
                    String materialName = (String) itemMap.get("material");
                    Object amountObj = itemMap.get("amount");
                    int amount = 1;
                    
                    if (amountObj instanceof Number) {
                        amount = ((Number) amountObj).intValue();
                    }
                    
                    if (materialName != null) {
                        Material material = Material.valueOf(materialName.toUpperCase());
                        requiredItems.add(new ItemRequirement(material, amount));
                    }
                } catch (Exception e) {
                    // Ignorar items mal configurados
                }
            }
        }
        
        this.description = config.getStringList("description");
    }
    
    /**
     * Constructor alternativo para crear regalos programáticamente (sin config)
     * Usado por el sistema de regalos aleatorios
     */
    public GiftItem(String id, String name, int points, Material material, int amount, List<String> description) {
        this(id, name, points, 0.0, material, amount, description);
    }

    public GiftItem(String id, String name, int points, double moneyRequired, Material material, int amount, List<String> description) {
        this.id = id;
        this.name = name;
        this.points = points;
        this.moneyRequired = moneyRequired;
        this.requiredItems = new ArrayList<>();
        if (material != null) {
            this.requiredItems.add(new ItemRequirement(material, amount));
        }
        this.description = description != null ? description : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPoints() {
        return points;
    }

    public double getMoneyRequired() {
        return moneyRequired;
    }

    public List<ItemRequirement> getRequiredItems() {
        return requiredItems;
    }

    public List<String> getDescription() {
        return description;
    }

    public static class ItemRequirement {
        private final Material material;
        private final int amount;

        public ItemRequirement(Material material, int amount) {
            this.material = material;
            this.amount = amount;
        }

        public Material getMaterial() {
            return material;
        }

        public int getAmount() {
            return amount;
        }
    }
}