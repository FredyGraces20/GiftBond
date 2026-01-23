package com.fredygraces.giftbond.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * Modelo para representar un regalo pendiente en el mailbox
 */
public class MailboxGift {
    private int id;
    private final UUID receiverUUID;
    private final String receiverName;
    private final UUID senderUUID;
    private final String senderName;
    private final String giftId;
    private final String giftName;
    private final List<ItemStack> originalItems; // Items enviados originalmente
    private final List<ItemStack> sharedItems;   // Items que recibe el receptor (porcentaje)
    private final int basePoints;                // Puntos base del regalo (sin boost)
    private final int pointsAwarded;             // Puntos finales calculados (con boost del momento de envío)
    private final long timestamp;
    private boolean claimed;
    private Long claimTimestamp;
    
    public MailboxGift(UUID receiverUUID, String receiverName, UUID senderUUID, 
                      String senderName, String giftId, String giftName, 
                      List<ItemStack> originalItems, List<ItemStack> sharedItems, 
                      int basePoints, int pointsAwarded) {
        this.receiverUUID = receiverUUID;
        this.receiverName = receiverName;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.giftId = giftId;
        this.giftName = giftName;
        this.originalItems = originalItems != null ? new ArrayList<>(originalItems) : new ArrayList<>();
        this.sharedItems = sharedItems != null ? new ArrayList<>(sharedItems) : new ArrayList<>();
        this.basePoints = basePoints;
        this.pointsAwarded = pointsAwarded;
        this.timestamp = System.currentTimeMillis();
        this.claimed = false;
        this.claimTimestamp = null;
    }
    
    // Constructor para cargar desde base de datos
    public MailboxGift(int id, UUID receiverUUID, String receiverName, UUID senderUUID,
                      String senderName, String giftId, String giftName, List<ItemStack> originalItems,
                      List<ItemStack> sharedItems, int basePoints, int pointsAwarded, long timestamp, 
                      boolean claimed, Long claimTimestamp) {
        this.id = id;
        this.receiverUUID = receiverUUID;
        this.receiverName = receiverName;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.giftId = giftId;
        this.giftName = giftName;
        this.originalItems = originalItems != null ? new ArrayList<>(originalItems) : new ArrayList<>();
        this.sharedItems = sharedItems != null ? new ArrayList<>(sharedItems) : new ArrayList<>();
        this.basePoints = basePoints;
        this.pointsAwarded = pointsAwarded;
        this.timestamp = timestamp;
        this.claimed = claimed;
        this.claimTimestamp = claimTimestamp;
    }
    
    // Getters y setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public UUID getReceiverUUID() { return receiverUUID; }
    public String getReceiverName() { return receiverName; }
    
    public UUID getSenderUUID() { return senderUUID; }
    public String getSenderName() { return senderName; }
    
    public String getGiftId() { return giftId; }
    public String getGiftName() { return giftName; }
    
    public List<ItemStack> getOriginalItems() { return new ArrayList<>(originalItems); }
    public List<ItemStack> getSharedItems() { return new ArrayList<>(sharedItems); }
    public int getBasePoints() { return basePoints; }
    public int getPointsAwarded() { return pointsAwarded; }
    
    public long getTimestamp() { return timestamp; }
    public boolean isClaimed() { return claimed; }
    public Long getClaimTimestamp() { return claimTimestamp; }
    
    public void setClaimed(boolean claimed) { 
        this.claimed = claimed;
        if (claimed) {
            this.claimTimestamp = System.currentTimeMillis();
        }
    }
}