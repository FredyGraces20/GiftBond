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
        // Por ahora permitimos siempre, pero la lógica de cobro usará eco take
        // que fallará si no tiene dinero si el plugin de economía es estricto.
        return true; 
    }
    
    // Método para cobrar al jugador
    public void chargePlayer(org.bukkit.entity.Player player, double amount) {
        if (amount <= 0) return;
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), 
            "eco take " + player.getName() + " " + amount);
    }
}