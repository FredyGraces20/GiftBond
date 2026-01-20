package com.fredygraces.giftbond.managers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.fredygraces.giftbond.GiftBond;

/**
 * Gestor de múltiples archivos de configuración
 * Maneja config.yml, messages.yml, gifts.yml, database.yml
 */
public class ConfigManager {
    
    private final GiftBond plugin;
    private FileConfiguration mainConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration giftsConfig;
    private FileConfiguration databaseConfig;
    
    private File messagesFile;
    private File giftsFile;
    private File databaseFile;
    
    public ConfigManager(GiftBond plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Carga todos los archivos de configuración
     */
    public void loadConfigs() {
        // Guardar config.yml por defecto
        plugin.saveDefaultConfig();
        mainConfig = plugin.getConfig();
        
        // Crear archivos de configuración adicionales
        createCustomConfig("messages.yml");
        createCustomConfig("gifts.yml");
        createCustomConfig("database.yml");
        
        // Cargar configuraciones
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        giftsFile = new File(plugin.getDataFolder(), "gifts.yml");
        giftsConfig = YamlConfiguration.loadConfiguration(giftsFile);
        
        databaseFile = new File(plugin.getDataFolder(), "database.yml");
        databaseConfig = YamlConfiguration.loadConfiguration(databaseFile);
        
        plugin.getLogger().info("✓ Archivos de configuración cargados:");
        plugin.getLogger().info("  - config.yml");
        plugin.getLogger().info("  - messages.yml");
        plugin.getLogger().info("  - gifts.yml");
        plugin.getLogger().info("  - database.yml");
    }
    
    /**
     * Crea un archivo de configuración personalizado
     */
    private void createCustomConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        plugin.getLogger().info("Intentando crear archivo: " + fileName);
        plugin.getLogger().info("Ruta del archivo: " + file.getAbsolutePath());
        plugin.getLogger().info("Directorio del plugin: " + plugin.getDataFolder().getAbsolutePath());
        
        if (!file.exists()) {
            try {
                // Crear directorio si no existe
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                    plugin.getLogger().info("Directorio del plugin creado");
                }
                
                // Copiar desde recursos
                plugin.getLogger().info("Buscando recurso: " + fileName);
                InputStream inputStream = plugin.getResource(fileName);
                
                if (inputStream != null) {
                    plugin.getLogger().info("Recurso encontrado, copiando...");
                    Files.copy(inputStream, file.toPath());
                    plugin.getLogger().info("✓ Creado " + fileName + " desde recursos");
                } else {
                    plugin.getLogger().warning("No se encontró " + fileName + " en recursos, creando estructura básica...");
                    createBasicConfigStructure(file, fileName);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error al crear " + fileName + ": " + e.getMessage());
                plugin.getLogger().severe("Stack trace: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                // Fallback: crear archivo con contenido básico
                try {
                    plugin.getLogger().info("Intentando crear estructura básica como fallback...");
                    createBasicConfigStructure(file, fileName);
                } catch (IOException ioException) {
                    plugin.getLogger().severe("Error al crear estructura básica para " + fileName + ": " + ioException.getMessage());
                }
            }
        } else {
            plugin.getLogger().info(fileName + " ya existe, omitiendo creación");
        }
    }
    
    /**
     * Crea una estructura básica de configuración para archivos que no se encuentran
     */
    private void createBasicConfigStructure(File file, String fileName) throws IOException {
        switch (fileName) {
            case "messages.yml":
                createBasicMessagesConfig(file);
                break;
            case "gifts.yml":
                createBasicGiftsConfig(file);
                break;
            case "database.yml":
                createBasicDatabaseConfig(file);
                break;
            default:
                // Crear archivo vacío
                Files.createFile(file.toPath());
                plugin.getLogger().info("✓ Creado archivo vacío: " + fileName);
        }
    }
    
    /**
     * Crea estructura básica para messages.yml
     */
    private void createBasicMessagesConfig(File file) throws IOException {
        String defaultContent = """
            # ═══════════════════════════════════════════════════════════════
            # 💬 MESSAGES.YML - Mensajes del Plugin GiftBond
            # ═══════════════════════════════════════════════════════════════
            # Todos los mensajes editables del plugin
            # Soporta códigos de color con '&' o '§'
            # Usa {prefix} para insertar el prefix del plugin
            # ═══════════════════════════════════════════════════════════════
            
            # Prefix del plugin (se usa en todos los mensajes con {prefix})
            prefix: "&d[GiftBond] &r"
            
            # ═══════════════════════════════════════════════════════════════
            # 🚫 MENSAJES DE ERROR
            # ═══════════════════════════════════════════════════════════════
            
            errors:
              no_permission: "{prefix}&cNo tienes permiso para usar este comando."
              player_only: "{prefix}&cSolo los jugadores pueden usar este comando."
              player_not_found: "{prefix}&cJugador no encontrado: {player}"
              player_offline: "{prefix}&cEl jugador ya no está en línea."
              no_self_gift: "{prefix}&cNo puedes enviarte regalos a ti mismo."
              cooldown: "{prefix}&cDebes esperar {seconds} segundos antes de enviar otro regalo."
              daily_limit: "{prefix}&cHas alcanzado el límite diario de {limit} regalos. ¡Vuelve mañana!"
              min_hours_sender: "{prefix}&cDebes tener al menos {min} horas jugadas para enviar regalos. (Tienes {current} horas)"
              min_hours_receiver: "{prefix}&cEl jugador {player} debe tener al menos {min} horas jugadas para recibir regalos."
              insufficient_items: "{prefix}&cNo tienes los items necesarios para enviar este regalo."
            
            # ═══════════════════════════════════════════════════════════════
            # ✅ MENSAJES DE ÉXITO
            # ═══════════════════════════════════════════════════════════════
            
            success:
              gift_sent: "{prefix}&eHas enviado un regalo de &f{gift} &e({points} puntos) a &f{receiver}"
              gift_received: "{prefix}&eHas recibido un regalo de &f{gift} &e({points} puntos) de &f{sender}"
              boost_granted: "{prefix}&a✨ Boost x{multiplier} otorgado a {player} por {duration}"
              boost_expired: "{prefix}&7Tu boost ha expirado."
              config_reloaded: "{prefix}&aConfiguración recargada correctamente."
              data_saved: "{prefix}&aDatos guardados correctamente."
            
            # ═══════════════════════════════════════════════════════════════
            # 📢 BROADCASTS Y ANUNCIOS
            # ═══════════════════════════════════════════════════════════════
            
            broadcasts:
              # Broadcast cuando una pareja alcanza el Top 1
              top1_achieved: "&d✨ ¡{player1} y {player2} son ahora la pareja Nº1 con {points} puntos! ✨"
              
              # Broadcast cuando los regalos aleatorios cambian (modo auto)
              gifts_rotated: "&e&l⚡ ¡Los regalos han cambiado! &7Usa &f/regalo &7para ver los nuevos."
            
            # ═══════════════════════════════════════════════════════════════
            # 🎁 MENSAJES DE REGALOS ALEATORIOS
            # ═══════════════════════════════════════════════════════════════
            
            random_gifts:
              # Lore de regalos aleatorios (modo auto)
              item_name: "&6&l✦ &e{item} &6&l✦"
              lore_separator: "&8&m━━━━━━━━━━━━━━━━━━━━"
              lore_title: "&7&o🎁 Regalo del Momento"
              lore_reward: "&a&l💰 Premio: &f{points} puntos"
              lore_cost: "&c&l📦 Costo: &f{amount}x &e{item}"
              lore_rotation: "&7⏰ Los regalos rotan cada hora"
              lore_footer: "&7&o¡Aprovecha esta oportunidad!"
            """;
        
        Files.writeString(file.toPath(), defaultContent);
        plugin.getLogger().info("✓ Creado messages.yml con contenido predeterminado");
    }
    
    /**
     * Crea estructura básica para gifts.yml
     */
    private void createBasicGiftsConfig(File file) throws IOException {
        String defaultContent = """
            # ═══════════════════════════════════════════════════════════════
            # 🎁 GIFTS.YML - Sistema de Regalos
            # ═══════════════════════════════════════════════════════════════
            # Configuración completa del sistema de regalos
            # ═══════════════════════════════════════════════════════════════
            
            # Modo de operación: "auto" o "manual"
            # - auto: El sistema detecta la versión y genera regalos aleatorios
            # - manual: Usa la lista de regalos definida manualmente abajo
            mode: "auto"
            
            # Versión específica para usar (cuando se desea sobreescribir la detección automática)
            # Deja vacío "" para usar detección automática, o pon la versión específica como "1.20.6" o "1.21.1"
            force_selected_version: ""
            
            # ═══════════════════════════════════════════════════════════════
            # 🎲 MODO AUTOMÁTICO - Regalos Aleatorios
            # ═══════════════════════════════════════════════════════════════
            
            auto_mode:
              enabled: true
              
              # Detección automática de versión del servidor
              detect_version: true
              # Versión manual (solo si detect_version = false)
              # Formato: "1.21" o "1.20.4"
              force_version: "1.21"
              
              # Blacklist de items (nunca aparecerán como regalos)
              exclude_items:
                - BARRIER
                - STRUCTURE_VOID
                - SPAWNER
                - "*_SPAWN_EGG"
                - AIR
                - BEDROCK
              
              # Categorías permitidas
              allowed_categories:
                food: true
                blocks: true
                resources: true
                tools: true
                weapons: true
                armor: true
                potions: true
                plants: true
                misc: true
              
              # Rotación de regalos
              rotation:
                enabled: true
                interval: 60
                active_gifts: 9
                broadcast_on_change: true
              
              # Rango de puntos y items
              points:
                min: 10
                max: 100
              items:
                min: 1
                max: 64
            
            # ═══════════════════════════════════════════════════════════════
            # 📝 MODO MANUAL - Lista Fija de Regalos
            # ═══════════════════════════════════════════════════════════════
            
            manual_mode:
              enabled: false
              gifts: {}
            """;
        
        Files.writeString(file.toPath(), defaultContent);
        plugin.getLogger().info("✓ Creado gifts.yml con contenido predeterminado");
    }
    
    /**
     * Crea estructura básica para database.yml
     */
    private void createBasicDatabaseConfig(File file) throws IOException {
        String defaultContent = """
            # ═══════════════════════════════════════════════════════════════
            # 💾 DATABASE.YML - Configuración de Base de Datos
            # ═══════════════════════════════════════════════════════════════
            # Configuración del sistema de almacenamiento
            # ═══════════════════════════════════════════════════════════════
            
            # Configuración de respaldo automático
            backup:
              # Intervalo de respaldo en minutos (0 = desactivado)
              interval_minutes: 60
              # Número máximo de archivos de respaldo a mantener
              max_backups: 10
              # Directorio de respaldos (relativo al directorio del plugin)
              directory: "backups"
            
            # Configuración de conexión a base de datos externa
            external:
              # Tipo de base de datos (sqlite, mysql, postgresql)
              type: "sqlite"
              # Configuración específica por tipo
              sqlite:
                # Ruta al archivo de base de datos
                path: "friendship.db"
              mysql:
                host: "localhost"
                port: 3306
                database: "giftbond"
                username: "root"
                password: "password"
                # Propiedades de conexión JDBC adicionales
                properties:
                  useSSL: false
                  serverTimezone: "UTC"
            """;
        
        Files.writeString(file.toPath(), defaultContent);
        plugin.getLogger().info("✓ Creado database.yml con contenido predeterminado");
    }
    
    /**
     * Recarga todos los archivos de configuración
     */
    public void reloadConfigs() {
        // Recargar config.yml principal
        plugin.reloadConfig();
        mainConfig = plugin.getConfig();
        
        // Recargar archivos secundarios
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        giftsConfig = YamlConfiguration.loadConfiguration(giftsFile);
        databaseConfig = YamlConfiguration.loadConfiguration(databaseFile);
        
        plugin.getLogger().info("✓ Configuración principal recargada");
        plugin.getLogger().info("✓ messages.yml recargado");
        plugin.getLogger().info("✓ gifts.yml recargado");
        plugin.getLogger().info("✓ database.yml recargado");
        plugin.getLogger().info("✓ Todas las configuraciones actualizadas");
    }
    
    /**
     * Obtiene la configuración de mensajes
     */
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
    
    /**
     * Obtiene la configuración de regalos
     */
    public FileConfiguration getGiftsConfig() {
        return giftsConfig;
    }
    
    /**
     * Obtiene la configuración de base de datos
     */
    public FileConfiguration getDatabaseConfig() {
        return databaseConfig;
    }
    
    /**
     * Obtiene la configuración principal (config.yml)
     */
    public FileConfiguration getMainConfig() {
        return mainConfig;
    }
    
    /**
     * Obtiene un mensaje del messages.yml con el prefix aplicado
     * @param path Ruta del mensaje (ej: "errors.no_permission")
     * @param defaultMsg Mensaje por defecto si no existe
     * @return Mensaje con prefix aplicado y colores traducidos
     */
    public String getMessage(String path, String defaultMsg) {
        String message = messagesConfig.getString(path, defaultMsg);
        String prefix = messagesConfig.getString("prefix", "&d[GiftBond] &r");
        
        if (message == null) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', defaultMsg);
        }
        
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message.replace("{prefix}", prefix));
    }
    
    /**
     * Obtiene un mensaje sin aplicar el prefix
     * @param path Ruta del mensaje
     * @param defaultMsg Mensaje por defecto
     * @return Mensaje con colores traducidos
     */
    public String getMessageRaw(String path, String defaultMsg) {
        String message = messagesConfig.getString(path, defaultMsg);
        if (message == null) {
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', defaultMsg);
        }
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * Obtiene el prefix del plugin
     * @return Prefix con colores traducidos
     */
    public String getPrefix() {
        String prefix = messagesConfig.getString("prefix", "&d[GiftBond] &r");
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix);
    }
    
    /**
     * Garantiza que todos los archivos de configuración existan
     * Crea cualquier archivo faltante con contenido básico
     */
    public void ensureAllConfigsExist() {
        plugin.getLogger().info("Verificando archivos de configuración...");
        
        // Verificar y crear cada archivo si no existe
        String[] configFiles = {"messages.yml", "gifts.yml", "database.yml"};
        
        for (String fileName : configFiles) {
            File file = new File(plugin.getDataFolder(), fileName);
            if (!file.exists()) {
                plugin.getLogger().info("Creando archivo faltante: " + fileName);
                createCustomConfig(fileName);
            }
        }
        
        plugin.getLogger().info("✓ Todos los archivos de configuración verificados");
    }
}
