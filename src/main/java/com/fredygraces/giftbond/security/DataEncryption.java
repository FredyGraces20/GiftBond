package com.fredygraces.giftbond.security;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Sistema de encriptación para proteger datos sensibles
 */
public class DataEncryption {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String DEFAULT_KEY = "GiftBondSecureKey2026"; // En producción usar clave configurable
    
    private final SecretKey secretKey;
    
    public DataEncryption() {
        this.secretKey = generateKey();
    }
    
    public DataEncryption(String customKey) {
        this.secretKey = new SecretKeySpec(customKey.getBytes(), ALGORITHM);
    }
    
    /**
     * Generar clave secreta
     */
    private SecretKey generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(256); // AES-256
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidParameterException e) {
            // Fallback a clave predeterminada
            return new SecretKeySpec(DEFAULT_KEY.getBytes(), ALGORITHM);
        }
    }
    
    /**
     * Encriptar string
     */
    public String encrypt(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
            System.err.println("Encryption failed: " + e.getMessage());
            return data; // Return original if encryption fails
        }
    }
    
    /**
     * Desencriptar string
     */
    public String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.isEmpty()) {
            return encryptedData;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | IllegalArgumentException | UnsupportedEncodingException e) {
            System.err.println("Decryption failed: " + e.getMessage());
            return encryptedData; // Return encrypted if decryption fails
        }
    }
    
    /**
     * Encriptar datos binarios
     */
    public byte[] encryptBytes(byte[] data) {
        if (data == null || data.length == 0) {
            return data;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Byte encryption failed: " + e.getMessage());
            return data;
        }
    }
    
    /**
     * Desencriptar datos binarios
     */
    public byte[] decryptBytes(byte[] encryptedData) {
        if (encryptedData == null || encryptedData.length == 0) {
            return encryptedData;
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Byte decryption failed: " + e.getMessage());
            return encryptedData;
        }
    }
    
    /**
     * Verificar si un string está encriptado
     */
    public boolean isEncrypted(String data) {
        if (data == null || data.length() < 24) { // AES mínimo
            return false;
        }
        
        try {
            Base64.getDecoder().decode(data);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Generar salt aleatorio para mayor seguridad
     */
    public static String generateSalt(int length) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[length];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Combinar datos con salt
     */
    public static String combineWithSalt(String data, String salt) {
        return salt + ":" + data;
    }
    
    /**
     * Extraer datos de string con salt
     */
    public static String[] extractFromSalted(String saltedData) {
        if (saltedData == null || !saltedData.contains(":")) {
            return new String[]{"", saltedData};
        }
        
        int colonIndex = saltedData.indexOf(":");
        String salt = saltedData.substring(0, colonIndex);
        String data = saltedData.substring(colonIndex + 1);
        return new String[]{salt, data};
    }
}