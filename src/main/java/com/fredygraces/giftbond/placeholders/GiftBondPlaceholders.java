package com.fredygraces.giftbond.placeholders;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.managers.DatabaseManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * PlaceholderAPI expansion for GiftBond plugin
 * Provides placeholders for top couples and personal points
 */
public class GiftBondPlaceholders extends PlaceholderExpansion {
    
    private final GiftBond plugin;
    
    public GiftBondPlaceholders(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "giftbond";
    }
    
    @Override
    public String getAuthor() {
        return "Fredy_Graces";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, String params) {
        // Handle top couple placeholders
        if (params.startsWith("couple_top_")) {
            return getCoupleTopPlaceholder(params);
        }
        
        // Handle points placeholders
        if (params.startsWith("points_top_")) {
            return getPointsTopPlaceholder(params);
        }
        
        // Handle personal points placeholder
        if (params.equals("personal_points")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getFriendshipManager().getPersonalPoints(player.getUniqueId().toString()));
        }
        
        return null; // Unknown placeholder
    }
    
    /**
     * Handles couple top placeholders (%giftbond_couple_top_1%, %giftbond_couple_top_2%, etc.)
     */
    private String getCoupleTopPlaceholder(String params) {
        try {
            // Extract the number from the placeholder (e.g., "couple_top_1" -> 1)
            String numberStr = params.replace("couple_top_", "");
            int position = Integer.parseInt(numberStr);
            
            // Validate position (1-5)
            if (position < 1 || position > 5) {
                return "Posición inválida";
            }
            
            // Get top couples from database
            List<DatabaseManager.FriendshipPair> topPairs = plugin.getFriendshipManager().getTopFriendshipPairs(5);
            
            // Check if we have enough couples
            if (topPairs.size() < position) {
                return "Sin datos";
            }
            
            // Get the specific couple
            DatabaseManager.FriendshipPair pair = topPairs.get(position - 1);
            
            // Get player names
            String player1Name = getPlayerName(pair.getPlayer1UUID());
            String player2Name = getPlayerName(pair.getPlayer2UUID());
            
            // Return formatted couple names
            return player1Name + " & " + player2Name;
            
        } catch (NumberFormatException e) {
            return "Formato inválido";
        }
    }
    
    /**
     * Handles points top placeholders (%giftbond_points_top_1%, %giftbond_points_top_2%, etc.)
     */
    private String getPointsTopPlaceholder(String params) {
        try {
            // Extract the number from the placeholder (e.g., "points_top_1" -> 1)
            String numberStr = params.replace("points_top_", "");
            int position = Integer.parseInt(numberStr);
            
            // Validate position (1-5)
            if (position < 1 || position > 5) {
                return "0";
            }
            
            // Get top couples from database
            List<DatabaseManager.FriendshipPair> topPairs = plugin.getFriendshipManager().getTopFriendshipPairs(5);
            
            // Check if we have enough couples
            if (topPairs.size() < position) {
                return "0";
            }
            
            // Get the specific couple's points
            DatabaseManager.FriendshipPair pair = topPairs.get(position - 1);
            return String.valueOf(pair.getPoints());
            
        } catch (NumberFormatException e) {
            return "0";
        }
    }
    
    /**
     * Gets player name from UUID, returns "Desconocido" if not found
     */
    private String getPlayerName(String uuidString) {
        try {
            UUID uuid = UUID.fromString(uuidString);
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            return player.getName() != null ? player.getName() : "Desconocido";
        } catch (Exception e) {
            return "Desconocido";
        }
    }
}