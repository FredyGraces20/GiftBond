package com.fredygraces.giftbond;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.fredygraces.giftbond.commands.AmistadCommand;
import com.fredygraces.giftbond.commands.GiftBondCommand;
import com.fredygraces.giftbond.commands.RegaloCommand;
import com.fredygraces.giftbond.commands.TopRegalosCommand;
import com.fredygraces.giftbond.events.GiftMenuListener;
import com.fredygraces.giftbond.events.HistoryMenuListener;
import com.fredygraces.giftbond.managers.ConfigManager;
import com.fredygraces.giftbond.managers.DatabaseManager;
import com.fredygraces.giftbond.managers.EconomyManager;
import com.fredygraces.giftbond.managers.FriendshipManager;
import com.fredygraces.giftbond.managers.GiftManager;
import com.fredygraces.giftbond.menus.GiftMenu;
import com.fredygraces.giftbond.menus.HistoryMenu;
import com.fredygraces.giftbond.storage.StorageManager;
import com.fredygraces.giftbond.utils.ItemFilter;
import com.fredygraces.giftbond.utils.RandomGiftGenerator;
import com.fredygraces.giftbond.utils.VersionDetector;

public final class GiftBond extends JavaPlugin {
    private static GiftBond instance;
    private ConfigManager configManager;  // Nuevo gestor de configuraciones
    private DatabaseManager databaseManager;
    private StorageManager storageManager;
    private FriendshipManager friendshipManager;
    private EconomyManager economyManager;
    private GiftManager giftManager;
    private GiftMenu giftMenu;
    private HistoryMenu historyMenu;
    
    // Sistema de regalos aleatorios
    private VersionDetector versionDetector;
    private ItemFilter itemFilter;
    private RandomGiftGenerator randomGiftGenerator;
    private BukkitRunnable rotationTask;

    @Override
    public void onEnable() {
        instance = this;
        
        // Inicializar ConfigManager PRIMERO
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        // Garantizar que todos los archivos de configuración existan
        configManager.ensureAllConfigsExist();
        
        // ASCII Art de inicio
        getLogger().info("");
        getLogger().info("Autor: Fredy_Graces                    _  _          .--.");
        getLogger().info("                                      ( \\/ )       .-    \\");
        getLogger().info("  _  _                   _  _          \\  /       /_      \\");
        getLogger().info(" ( \\/ )                 ( \\/ )          \\/       (o        )");
        getLogger().info("  \\  /                   \\  /                  _/          |");
        getLogger().info("   \\/                     \\/                  (c       .-. |");
        getLogger().info("             ___                  ;;          /      .'   \\");
        getLogger().info("          .''   ``.               ;;         O)     |      \\");
        getLogger().info("        _/ .-. .-. \\_     () ()  / _          `.__  \\       \\");
        getLogger().info("       (o|( O   O )|o)   ()(O)() |/ )           /    \\       \\");
        getLogger().info("        .'         `.      ()\\  _|_            /      \\       \\");
        getLogger().info("       /    (c c)    \\        \\(_  \\          /        \\       \\");
        getLogger().info("       |             |        (__)  `.______ ( ._/      \\       )");
        getLogger().info("       \\     (o)     /        (___)`._ ____.'           )     /");
        getLogger().info("        `.         .'         (__)  ______ /            /     /");
        getLogger().info("          `-.___.-'            /|\\         |           /     /");
        getLogger().info("          ___)(___            /  \\         \\          /     /");
        getLogger().info("       .-'        `-.                       `.      .'     /");
        getLogger().info("      / .-.      .-. \\                        `-  /.'     /");
        getLogger().info("     / /  ( .  . )  \\ \\          _  _          / \\)| | | |");
        getLogger().info("    / /    \\    /    \\ \\        ( \\/ )         /     \\_\\_\\_)");
        getLogger().info("    \\ \\     )  (     / /         \\  /        (    /");
        getLogger().info("     \\ \\   ( __ )   / /           \\/           \\   \\ \\  \\");
        getLogger().info("    /   )  //  \\\\  (   \\                        \\   \\ \\  \\");
        getLogger().info("(\\ / / /\\) \\\\  // (/\\ \\ \\ /)                     )   \\ \\  \\");
        getLogger().info(" -'-'-'  .'  )(  `.  `-`-`-                     .'   |.'   |");
        getLogger().info("       .'_ .'  `. _`.                      _.--'     (     (");
        getLogger().info("     oOO(_)      (_)OOo                   (__.--._____)_____)");
        getLogger().info("");
        
        // Copiar config.yml por defecto si no existe
        saveDefaultConfig();
        
        // Inicializar DatabaseManager (legacy - para compatibilidad)
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();
        
        // Inicializar StorageManager (nuevo sistema multi-database)
        storageManager = new StorageManager(this);
        if (!storageManager.initialize()) {
            getLogger().severe("ERROR CRÍTICO: No se pudo inicializar el sistema de almacenamiento!");
            getLogger().severe("Deshabilitando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Inicializar FriendshipManager
        friendshipManager = new FriendshipManager(this);
        
        // Inicializar EconomyManager
        economyManager = new EconomyManager(this);
        
        // Inicializar GiftManager
        giftManager = new GiftManager(this);
        
        // Inicializar sistema de regalos aleatorios (si está en modo auto)
        initializeRandomGiftSystem();
        
        // Inicializar GiftMenu
        giftMenu = new GiftMenu(this);
        
        // Inicializar HistoryMenu
        historyMenu = new HistoryMenu(this);
        
        // Registrar comandos
        getCommand("amistad").setExecutor(new AmistadCommand(this));
        getCommand("regalo").setExecutor(new RegaloCommand(this));
        getCommand("topregalos").setExecutor(new TopRegalosCommand(this));
        
        GiftBondCommand giftBondCommand = new GiftBondCommand(this);
        getCommand("giftbond").setExecutor(giftBondCommand);
        getCommand("giftbond").setTabCompleter(giftBondCommand);
        
        // Registrar eventos
        getServer().getPluginManager().registerEvents(new GiftMenuListener(this), this);
        getServer().getPluginManager().registerEvents(new HistoryMenuListener(this), this);
        
        // Registrar Placeholders de PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new com.fredygraces.giftbond.placeholders.GiftBondPlaceholders(this).register();
            getLogger().info("✓ Placeholders de GiftBond registrados");
        } else {
            getLogger().warning("⚠ PlaceholderAPI no encontrado - los placeholders no estarán disponibles");
        }
        
        getLogger().info("GiftBond enabled successfully!");
    }
    
    /**
     * Inicializa el sistema de regalos aleatorios
     */
    private void initializeRandomGiftSystem() {
        String mode = configManager.getGiftsConfig().getString("mode", "manual").toLowerCase();
        
        if (!mode.equals("auto")) {
            getLogger().info("Modo MANUAL - Sistema de regalos aleatorios desactivado");
            return;
        }
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  INICIALIZANDO SISTEMA ALEATORIO");
        getLogger().info("═══════════════════════════════════════");
        
        // 1. Detectar versión del servidor
        versionDetector = new VersionDetector(this);
        
        // 2. Inicializar filtro de items
        itemFilter = new ItemFilter(this, versionDetector);
        
        // 3. Inicializar generador de regalos aleatorios
        randomGiftGenerator = new RandomGiftGenerator(this, itemFilter);
        randomGiftGenerator.initialize();
        
        // 4. Conectar generador con GiftManager
        giftManager.setRandomGiftGenerator(randomGiftGenerator);
        
        // 5. Iniciar task de rotación automática
        if (configManager.getGiftsConfig().getBoolean("auto_mode.rotation.enabled", true)) {
            startRotationTask();
        }
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  SISTEMA ALEATORIO LISTO");
        getLogger().info("═══════════════════════════════════════");
    }
    
    /**
     * Inicia la tarea de rotación automática de regalos
     */
    private void startRotationTask() {
        int intervalMinutes = configManager.getGiftsConfig().getInt("auto_mode.rotation.interval", 60);
        
        rotationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (randomGiftGenerator != null && randomGiftGenerator.shouldRotate()) {
                    getLogger().info("Rotando regalos automáticamente...");
                    randomGiftGenerator.generateNewGifts();
                }
            }
        };
        
        // Ejecutar cada minuto (20 ticks * 60 = 1200 ticks)
        // Verificamos cada minuto si es tiempo de rotar
        rotationTask.runTaskTimer(this, 1200L, 1200L);
        
        getLogger().info("Task de rotación iniciada (intervalo: " + intervalMinutes + " minutos)");
    }

    @Override
    public void onDisable() {
        getLogger().info("Iniciando proceso de apagado de GiftBond...");
        
        // Cancelar tarea de rotación
        if (rotationTask != null) {
            getLogger().info("Cancelando tarea de rotación...");
            rotationTask.cancel();
        }
        
        // Cerrar StorageManager (gestiona todos los almacenamientos)
        if (storageManager != null) {
            getLogger().info("Cerrando sistema de almacenamiento...");
            storageManager.close();
        }
        
        // Cerrar DatabaseManager legacy
        if (databaseManager != null) {
            getLogger().info("Cerrando conexión legacy a la base de datos...");
            databaseManager.close();
        }
        
        getLogger().info("GiftBond disabled!");
    }

    public static GiftBond getInstance() {
        return instance;
    }
    
    /**
     * Obtiene el DatabaseManager legacy (compatibilidad hacia atrás)
     * @return DatabaseManager instance
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    /**
     * Obtiene el StorageManager (nuevo sistema multi-database)
     * @return StorageManager instance
     */
    public StorageManager getStorageManager() {
        return storageManager;
    }
    
    public FriendshipManager getFriendshipManager() {
        return friendshipManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public GiftManager getGiftManager() {
        return giftManager;
    }
    
    public GiftMenu getGiftMenu() {
        return giftMenu;
    }
    
    public HistoryMenu getHistoryMenu() {
        return historyMenu;
    }
    
    public String getMessage(String path, String defaultMsg) {
        return configManager.getMessage(path, defaultMsg);
    }
    
    /**
     * Obtiene el prefix del plugin con colores traducidos
     * @return Prefix formateado
     */
    public String getPrefix() {
        return configManager.getPrefix();
    }
    
    /**
     * Obtiene el ConfigManager
     * @return ConfigManager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
}