package com.fredygraces.giftbond.managers;

import com.fredygraces.giftbond.GiftBond;

public class EconomyManager {
    private final GiftBond plugin;

    public EconomyManager(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    // Método que utiliza el plugin para acceder a funcionalidades
    public GiftBond getPlugin() {
        return this.plugin;
    }
    
    // Método para verificar si el jugador tiene suficiente dinero
    public boolean hasEnoughMoney(org.bukkit.entity.Player player, double amount) {
        // Usar PlaceholderAPI para obtener el balance del jugador
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            String balanceStr = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%vault_eco_balance%");
            
            try {
                // Limpiar el string del balance (remover símbolos de moneda, espacios, etc.)
                String cleanBalance = balanceStr.replaceAll("[^0-9.,]", "").replace(",", "");
                double playerBalance = Double.parseDouble(cleanBalance);
                
                return playerBalance >= amount;
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("No se pudo parsear el balance de " + player.getName() + ": '" + balanceStr + "'");
                // Si no podemos verificar el balance, denegar por seguridad
                return false;
            }
        } else {
            plugin.getLogger().warning("PlaceholderAPI no encontrado - no se puede verificar balance");
            // Si no hay PlaceholderAPI, permitir por compatibilidad (como antes)
            return true;
        }
    }
    
    // Método para cobrar al jugador
    public void chargePlayer(org.bukkit.entity.Player player, double amount) {
        if (amount <= 0) return;
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), 
            "eco take " + player.getName() + " " + amount);
    }
}