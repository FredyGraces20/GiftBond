package com.fredygraces.giftbond.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

/**
 * Gestor de sesiones de regalos para almacenar información temporal
 * sobre quién está enviando regalos a quién
 */
public class GiftSessionManager {
    private static GiftSessionManager instance;
    private final Map<UUID, String> playerSessions; // UUID del jugador -> nombre del destinatario
    
    private GiftSessionManager() {
        this.playerSessions = new HashMap<>();
    }
    
    public static GiftSessionManager getInstance() {
        if (instance == null) {
            instance = new GiftSessionManager();
        }
        return instance;
    }
    
    /**
     * Inicia una sesión de regalo para un jugador
     * @param sender Jugador que envía el regalo
     * @param receiverNombre Nombre del destinatario
     */
    public void startGiftSession(Player sender, String receiverNombre) {
        playerSessions.put(sender.getUniqueId(), receiverNombre);
    }
    
    /**
     * Obtiene el nombre del destinatario para una sesión
     * @param sender Jugador que envía el regalo
     * @return Nombre del destinatario o null si no hay sesión
     */
    public String getReceiverName(Player sender) {
        return playerSessions.get(sender.getUniqueId());
    }
    
    /**
     * Finaliza una sesión de regalo
     * @param sender Jugador que envía el regalo
     */
    public void endGiftSession(Player sender) {
        playerSessions.remove(sender.getUniqueId());
    }
    
    /**
     * Verifica si un jugador tiene una sesión activa
     * @param sender Jugador a verificar
     * @return true si tiene sesión activa
     */
    public boolean hasActiveSession(Player sender) {
        return playerSessions.containsKey(sender.getUniqueId());
    }
}