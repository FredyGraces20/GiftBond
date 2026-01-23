package com.fredygraces.giftbond.permissions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.bukkit.entity.Player;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para el sistema de permisos de GiftBond
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class PermissionManagerTest {
    
    @Test
    @DisplayName("Verificar permisos base del plugin")
    public void testBasePermissions() {
        assertEquals("giftbond", PermissionManager.BASE_PERMISSION);
        assertEquals("giftbond.send", PermissionManager.COMMAND_SEND);
        assertEquals("giftbond.redeem", PermissionManager.COMMAND_REDEEM);
        assertEquals("giftbond.admin.*", PermissionManager.ADMIN_ALL);
    }
    
    @Test
    @DisplayName("Consola debe tener todos los permisos")
    public void testConsoleHasAllPermissions() {
        // Crear un mock de CommandSender que no sea Player
        org.bukkit.command.CommandSender consoleSender = mock(org.bukkit.command.CommandSender.class);
        
        assertTrue(PermissionManager.hasPermission(consoleSender, "any.permission"));
        assertTrue(PermissionManager.canSendGifts(consoleSender));
        assertTrue(PermissionManager.canRedeemGifts(consoleSender));
        assertTrue(PermissionManager.isAdmin(consoleSender));
    }
    
    @Test
    @DisplayName("Validar permisos de jugador normal")
    public void testNormalPlayerPermissions() {
        Player player = mock(Player.class);
        
        // Jugador sin permisos especiales
        when(player.hasPermission(anyString())).thenReturn(false);
        
        assertFalse(PermissionManager.canSendGifts(player));
        assertFalse(PermissionManager.canRedeemGifts(player));
        assertFalse(PermissionManager.isAdmin(player));
        assertFalse(PermissionManager.canBypassLimits(player));
    }
    
    @Test
    @DisplayName("Validar permisos de jugador con permisos básicos")
    public void testPlayerWithBasicPermissions() {
        Player player = mock(Player.class);
        
        // Jugador con permiso para enviar regalos
        when(player.hasPermission(PermissionManager.COMMAND_SEND)).thenReturn(true);
        when(player.hasPermission(PermissionManager.ADMIN_ALL)).thenReturn(false);
        when(player.hasPermission(PermissionManager.COMMAND_REDEEM)).thenReturn(false);
        when(player.hasPermission(PermissionManager.COMMAND_AMISTAD)).thenReturn(false);
        when(player.hasPermission(PermissionManager.BYPASS_LIMITS)).thenReturn(false);
        when(player.hasPermission(PermissionManager.BYPASS_COOLDOWN)).thenReturn(false);
        when(player.hasPermission(PermissionManager.PREMIUM_FEATURES)).thenReturn(false);
        
        assertTrue(PermissionManager.canSendGifts(player));
        assertFalse(PermissionManager.canRedeemGifts(player));
        assertFalse(PermissionManager.isAdmin(player));
    }
    
    @Test
    @DisplayName("Validar permisos de administrador")
    public void testAdminPermissions() {
        Player player = mock(Player.class);
        
        // Jugador con permisos administrativos
        when(player.hasPermission(PermissionManager.ADMIN_ALL)).thenReturn(true);
        
        assertTrue(PermissionManager.canSendGifts(player));
        assertTrue(PermissionManager.canRedeemGifts(player));
        assertTrue(PermissionManager.isAdmin(player));
        assertTrue(PermissionManager.canBypassLimits(player));
        assertTrue(PermissionManager.canBypassCooldown(player));
    }
    
    @Test
    @DisplayName("Validar mensaje de permiso denegado")
    public void testPermissionDeniedMessage() {
        String permission = "giftbond.test";
        String expectedMessage = "§cNo tienes permiso para usar este comando. Se requiere: §e" + permission;
        
        assertEquals(expectedMessage, PermissionManager.getPermissionDeniedMessage(permission));
    }
    
    @Test
    @DisplayName("Validar permisos específicos")
    public void testSpecificPermissions() {
        Player player = mock(Player.class);
        
        // Testear permiso de bypass limits
        when(player.hasPermission(PermissionManager.BYPASS_LIMITS)).thenReturn(true);
        assertTrue(PermissionManager.canBypassLimits(player));
        
        // Testear permiso premium
        when(player.hasPermission(PermissionManager.PREMIUM_FEATURES)).thenReturn(true);
        assertTrue(PermissionManager.hasPremiumAccess(player));
        
        // Testear permisos de amistad
        when(player.hasPermission(PermissionManager.COMMAND_AMISTAD)).thenReturn(true);
        assertTrue(PermissionManager.canUseFriendshipCommands(player));
    }
}