package com.fredygraces.giftbond.autocomplete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.fredygraces.giftbond.GiftBond;

/**
 * Sistema avanzado de autocompletado para comandos de GiftBond
 * 
 * @author GiftBond Team
 * @version 1.1.0
 */
public class SmartTabCompleter implements TabCompleter {
    
    public SmartTabCompleter(GiftBond plugin) {
        // Constructor mantenido para compatibilidad futura
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("giftbond")) {
            return Collections.emptyList();
        }
        
        return handleGiftBondCompletion(sender, args);
    }
    
    /**
     * Manejar autocompletado para comando /giftbond
     */
    private List<String> handleGiftBondCompletion(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return getMainCommands(sender);
        }
        
        String mainCommand = args[0].toLowerCase();
        
        switch (mainCommand) {
            case "friends" -> {
                return handleFriendsCompletion(sender, args);
            }
            case "top" -> {
                return Collections.emptyList(); // No necesita autocompletado
            }
            case "send" -> {
                return handleSendCompletion(sender, args);
            }
            case "redeem" -> {
                return handleRedeemCompletion(sender, args);
            }
            case "points" -> {
                return handlePointsCompletion(sender, args);
            }
            case "boost" -> {
                return handleBoostCompletion(sender, args);
            }
            case "reload", "savedata", "debug" -> {
                return Collections.emptyList(); // Comandos sin parámetros adicionales
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }
    
    /**
     * Obtener comandos principales según permisos
     */
    private List<String> getMainCommands(CommandSender sender) {
        List<String> commands = new ArrayList<>();
        
        // Comandos básicos (todos los usuarios)
        if (sender.hasPermission("giftbond.friends")) {
            commands.add("friends");
        }
        if (sender.hasPermission("giftbond.top")) {
            commands.add("top");
        }
        if (sender.hasPermission("giftbond.send")) {
            commands.add("send");
        }
        if (sender.hasPermission("giftbond.redeem")) {
            commands.add("redeem");
        }
        
        // Comandos de administrador
        if (sender.hasPermission("giftbond.admin.reload")) {
            commands.add("reload");
        }
        if (sender.hasPermission("giftbond.savedata")) {
            commands.add("savedata");
        }
        if (sender.hasPermission("giftbond.admin.points")) {
            commands.add("points");
        }
        if (sender.hasPermission("giftbond.admin.boost")) {
            commands.add("boost");
        }
        if (sender.hasPermission("giftbond.admin.debug")) {
            commands.add("debug");
        }
        
        return filterByInput(commands, "");
    }
    
    /**
     * Autocompletado para /giftbond friends
     */
    private List<String> handleFriendsCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Completar nombre de jugador
            return getOnlinePlayersStartingWith(args[1]);
        }
        return Collections.emptyList();
    }
    
    /**
     * Autocompletado para /giftbond send
     */
    private List<String> handleSendCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Completar nombre de jugador receptor
            return getOnlinePlayersStartingWith(args[1]);
        }
        return Collections.emptyList();
    }
    
    /**
     * Autocompletado para /giftbond redeem
     */
    private List<String> handleRedeemCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> options = new ArrayList<>();
            options.add("all");
            
            // Agregar nombres de remitentes con regalos pendientes
            if (sender instanceof Player) {
                // Aquí se conectaría con el mailbox para obtener remitentes
                // Por ahora simulamos algunos nombres comunes
                options.addAll(Arrays.asList("Steve", "Alex", "Notch"));
            }
            
            return filterByInput(options, args[1]);
        }
        return Collections.emptyList();
    }
    
    /**
     * Autocompletado para /giftbond points
     */
    private List<String> handlePointsCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Completar nombre de jugador
            return getOnlinePlayersStartingWith(args[1]);
        }
        
        if (args.length == 3) {
            // Completar acciones según permisos
            List<String> actions = new ArrayList<>();
            
            // Todos pueden usar "view" si tienen el permiso específico
            if (sender.hasPermission("giftbond.points.view")) {
                actions.add("view");
            }
            
            // Solo admins pueden usar estas acciones
            if (sender.hasPermission("giftbond.admin.points")) {
                actions.addAll(Arrays.asList("add", "remove", "set"));
            }
            
            return filterByInput(actions, args[2]);
        }
        
        if (args.length == 4 && args.length > 2) {
            String action = args[2].toLowerCase();
            if (action.equals("add") || action.equals("remove") || action.equals("set")) {
                // Sugerir valores comunes
                return Arrays.asList("1", "10", "100", "1000");
            }
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Autocompletado para /giftbond boost
     */
    private List<String> handleBoostCompletion(CommandSender sender, String[] args) {
        if (args.length == 2) {
            // Completar nombre de jugador
            return getOnlinePlayersStartingWith(args[1]);
        }
        
        if (args.length == 3) {
            // Completar tipos de boost
            List<String> boostTypes = Arrays.asList("vip", "premium", "ultra", "temporal");
            return filterByInput(boostTypes, args[2]);
        }
        
        if (args.length == 4) {
            // Completar duración (para boosts temporales)
            return Arrays.asList("1h", "24h", "7d", "30d");
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Obtener jugadores online que empiezan con el texto dado
     */
    private List<String> getOnlinePlayersStartingWith(String input) {
        final String searchTerm = (input == null) ? "" : input;
        
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(searchTerm.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Filtrar lista por texto de entrada
     */
    private List<String> filterByInput(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(options);
        }
        
        return options.stream()
            .filter(option -> option.toLowerCase().startsWith(input.toLowerCase()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Obtener sugerencias contextuales basadas en historial
     */
    public List<String> getContextualSuggestions(CommandSender sender, String commandContext) {
        // Implementar sistema de aprendizaje de patrones de uso
        // Por ejemplo: si un jugador usa frecuentemente ciertos comandos
        // con ciertos jugadores, sugerir esos patrones
        
        List<String> suggestions = new ArrayList<>();
        
        // Ejemplo básico de sugerencias contextuales
        if (commandContext.contains("send")) {
            suggestions.add("send Steve");
            suggestions.add("send Alex");
        } else if (commandContext.contains("points")) {
            suggestions.add("points Steve view");
        } else if (commandContext.contains("redeem")) {
            suggestions.add("redeem all");
        }
        
        return suggestions;
    }
    
    /**
     * Validar sintaxis de comando mientras se escribe
     */
    public boolean validateSyntax(String[] args) {
        if (args.length == 0) return false;
        
        String mainCommand = args[0].toLowerCase();
        
        return switch (mainCommand) {
            case "friends" -> args.length <= 2; // /giftbond friends [jugador]
            case "top" -> args.length == 1; // /giftbond top
            case "send" -> args.length == 2; // /giftbond send <jugador>
            case "redeem" -> args.length <= 2; // /giftbond redeem [all|jugador]
            case "points" -> args.length >= 3 && args.length <= 4; // /giftbond points <jugador> <accion> [valor]
            case "boost" -> args.length >= 3 && args.length <= 4; // /giftbond boost <jugador> <tipo> [duración]
            case "reload", "savedata", "debug" -> args.length == 1; // Comandos sin parámetros
            default -> false;
        };
    }
}