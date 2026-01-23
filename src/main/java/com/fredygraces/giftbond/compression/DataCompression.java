package com.fredygraces.giftbond.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utilidad de compresión para optimizar almacenamiento de datos
 */
public class DataCompression {
    
    /**
     * Comprimir string usando GZIP
     */
    public static String compress(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return data;
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data.getBytes("UTF-8"));
        }
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
    
    /**
     * Descomprimir string usando GZIP
     */
    public static String decompress(String compressedData) throws IOException {
        if (compressedData == null || compressedData.isEmpty()) {
            return compressedData;
        }
        
        byte[] decoded = Base64.getDecoder().decode(compressedData);
        ByteArrayInputStream bis = new ByteArrayInputStream(decoded);
        
        try (GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toString("UTF-8");
        }
    }
    
    /**
     * Comprimir datos binarios
     */
    public static byte[] compressBytes(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return data;
        }
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }
    
    /**
     * Descomprimir datos binarios
     */
    public static byte[] decompressBytes(byte[] compressedData) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return compressedData;
        }
        
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        try (GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
    
    /**
     * Calcular ratio de compresión
     */
    public static double getCompressionRatio(String original, String compressed) {
        if (original == null || compressed == null) return 0.0;
        return (double) compressed.length() / original.length();
    }
    
    /**
     * Verificar si vale la pena comprimir (ahorro significativo)
     */
    public static boolean shouldCompress(String data) {
        if (data == null || data.length() < 100) {
            return false; // No comprimir datos pequeños
        }
        
        try {
            String compressed = compress(data);
            double ratio = getCompressionRatio(data, compressed);
            return ratio < 0.8; // Comprimir si reduce al menos 20%
        } catch (IOException e) {
            return false;
        }
    }
}