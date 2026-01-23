package com.fredygraces.giftbond.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Simple string encryption utility for basic protection
 * Not military grade, but prevents casual inspection
 */
public class StringProtector {
    
    private static final String KEY = "GiftBond2026"; // Simple key
    
    /**
     * Encrypt a string using XOR + Base64
     * @param input String to encrypt
     * @return Encrypted string
     */
    public static String encrypt(String input) {
        if (input == null || input.isEmpty()) return input;
        
        byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = new byte[inputBytes.length];
        
        for (int i = 0; i < inputBytes.length; i++) {
            encrypted[i] = (byte) (inputBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    /**
     * Decrypt a string
     * @param encrypted Encrypted string
     * @return Decrypted string
     */
    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) return encrypted;
        
        try {
            byte[] keyBytes = KEY.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = new byte[encryptedBytes.length];
            
            for (int i = 0; i < encryptedBytes.length; i++) {
                decrypted[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
            }
            
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encrypted; // Return original if decryption fails
        }
    }
    
    /**
     * Protect sensitive strings in code
     * @param className Class name
     * @param methodName Method name  
     * @param sensitiveData Data to protect
     * @return Protected version
     */
    public static String protectSensitiveData(String className, String methodName, String sensitiveData) {
        String combined = className + "::" + methodName + "::" + sensitiveData;
        return encrypt(combined);
    }
}