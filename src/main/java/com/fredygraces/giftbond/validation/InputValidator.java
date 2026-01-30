package com.fredygraces.giftbond.validation;

import java.util.UUID;
import java.util.regex.Pattern;

import org.bukkit.entity.Player;

/**
 * Sistema de validación de entradas para GiftBond
 * Proporciona sanitización y validación de datos de usuario
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class InputValidator {
    
    // Patrones de validación
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]{3,16}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    private static final Pattern COMMAND_ARGUMENT_PATTERN = Pattern.compile("^[\\w\\-_ .]{1,50}$");
    
    // Límites de longitud
    private static final int MAX_PLAYER_NAME_LENGTH = 16;
    private static final int MIN_PLAYER_NAME_LENGTH = 3;
    private static final int MAX_GIFT_NAME_LENGTH = 50;
    private static final int MAX_MESSAGE_LENGTH = 200;
    private static final int MAX_COMMAND_ARGS = 10;
    
    /**
     * Valida un nombre de jugador
     * 
     * @param playerName El nombre a validar
     * @return ValidationResult con el resultado de la validación
     */
    public static ValidationResult validatePlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return ValidationResult.failure("El nombre del jugador no puede estar vacío");
        }
        
        if (playerName.length() > MAX_PLAYER_NAME_LENGTH) {
            return ValidationResult.failure(
                String.format("El nombre del jugador es demasiado largo (máximo %d caracteres)", 
                    MAX_PLAYER_NAME_LENGTH));
        }
        
        if (playerName.length() < MIN_PLAYER_NAME_LENGTH) {
            return ValidationResult.failure(
                String.format("El nombre del jugador es demasiado corto (mínimo %d caracteres)", 
                    MIN_PLAYER_NAME_LENGTH));
        }
        
        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            return ValidationResult.failure(
                "El nombre del jugador contiene caracteres inválidos. Solo se permiten letras, números, guiones bajos y puntos");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Valida un UUID
     * 
     * @param uuidString El UUID como string
     * @return ValidationResult con el resultado de la validación
     */
    public static ValidationResult validateUUID(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return ValidationResult.failure("El UUID no puede estar vacío");
        }
        
        if (!UUID_PATTERN.matcher(uuidString).matches()) {
            return ValidationResult.failure("Formato de UUID inválido");
        }
        
        try {
            UUID.fromString(uuidString);
            return ValidationResult.success();
        } catch (IllegalArgumentException e) {
            return ValidationResult.failure("UUID inválido: " + e.getMessage());
        }
    }
    
    /**
     * Valida un nombre de regalo
     * 
     * @param giftName El nombre del regalo
     * @return ValidationResult con el resultado de la validación
     */
    public static ValidationResult validateGiftName(String giftName) {
        if (giftName == null || giftName.isEmpty()) {
            return ValidationResult.failure("El nombre del regalo no puede estar vacío");
        }
        
        if (giftName.length() > MAX_GIFT_NAME_LENGTH) {
            return ValidationResult.failure(
                String.format("El nombre del regalo es demasiado largo (máximo %d caracteres)", 
                    MAX_GIFT_NAME_LENGTH));
        }
        
        // Verificar caracteres peligrosos
        if (giftName.contains(";") || giftName.contains("--") || giftName.contains("/*")) {
            return ValidationResult.failure("El nombre del regalo contiene caracteres peligrosos");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Valida un número entero positivo
     * 
     * @param value El valor a validar
     * @param fieldName Nombre del campo para mensajes de error
     * @param minValue Valor mínimo permitido
     * @param maxValue Valor máximo permitido
     * @return ValidationResult con el resultado de la validación
     */
    public static ValidationResult validatePositiveInteger(String value, String fieldName, 
                                                         int minValue, int maxValue) {
        if (value == null || value.isEmpty()) {
            return ValidationResult.failure(String.format("El campo %s no puede estar vacío", fieldName));
        }
        
        try {
            int number = Integer.parseInt(value);
            
            if (number < minValue) {
                return ValidationResult.failure(
                    String.format("El valor de %s debe ser al menos %d", fieldName, minValue));
            }
            
            if (number > maxValue) {
                return ValidationResult.failure(
                    String.format("El valor de %s no puede exceder %d", fieldName, maxValue));
            }
            
            return ValidationResult.success();
            
        } catch (NumberFormatException e) {
            return ValidationResult.failure(String.format("El campo %s debe ser un número válido", fieldName));
        }
    }
    
    /**
     * Valida argumentos de comando
     * 
     * @param args Array de argumentos
     * @return ValidationResult con el resultado de la validación
     */
    public static ValidationResult validateCommandArguments(String[] args) {
        if (args == null) {
            return ValidationResult.failure("Los argumentos del comando no pueden ser nulos");
        }
        
        if (args.length > MAX_COMMAND_ARGS) {
            return ValidationResult.failure(
                String.format("Demasiados argumentos (máximo %d)", MAX_COMMAND_ARGS));
        }
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg == null) {
                return ValidationResult.failure(String.format("El argumento %d no puede ser nulo", i + 1));
            }
            
            if (arg.length() > MAX_MESSAGE_LENGTH) {
                return ValidationResult.failure(
                    String.format("El argumento %d es demasiado largo (máximo %d caracteres)", 
                        i + 1, MAX_MESSAGE_LENGTH));
            }
            
            if (!COMMAND_ARGUMENT_PATTERN.matcher(arg).matches()) {
                return ValidationResult.failure(
                    String.format("El argumento %d contiene caracteres inválidos", i + 1));
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Sanitiza un string para prevenir inyecciones
     * 
     * @param input El string a sanitizar
     * @return El string sanitizado
     */
    public static String sanitizeInput(String input) {
        if (input == null) {
            return "";
        }
        
        // Remover caracteres peligrosos comunes
        return input
            .replace("'", "")
            .replace("\"", "")
            .replace("\\", "")
            .replace(";", "")
            .trim();
    }
    
    /**
     * Verifica si un jugador existe en el servidor
     * 
     * @param playerName Nombre del jugador
     * @return ValidationResult con el resultado
     */
    public static ValidationResult validatePlayerExists(String playerName) {
        ValidationResult nameValidation = validatePlayerName(playerName);
        if (!nameValidation.isValid()) {
            return nameValidation;
        }
        
        Player player = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return ValidationResult.failure(
                String.format("El jugador '%s' no está conectado al servidor", playerName));
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Clase para resultados de validación
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}