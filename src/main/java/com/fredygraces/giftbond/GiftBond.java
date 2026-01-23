package com.fredygraces.giftbond;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.fredygraces.giftbond.commands.AmistadCommand;
import com.fredygraces.giftbond.commands.GiftBondCommand;
import com.fredygraces.giftbond.commands.MailboxCommand;
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
import com.fredygraces.giftbond.security.LicenseChecker;
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
        
        // Verificar licencia y seguridad
        if (!LicenseChecker.verifyLicense(getDescription())) {
            getLogger().severe("❌ VERIFICACIÓN DE LICENCIA FALLIDA");
            getLogger().severe("❌ Este plugin está protegido por derechos de autor");
            getLogger().severe("❌ Uso no autorizado detectado - deshabilitando plugin");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        LicenseChecker.logSecurityInfo();
        
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
        // Inicializar StorageManager primero
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
        PluginCommand amistadCmd = getCommand("amistad");
        if (amistadCmd != null) {
            amistadCmd.setExecutor(new AmistadCommand(this));
        }
        
        PluginCommand regaloCmd = getCommand("regalo");
        if (regaloCmd != null) {
            regaloCmd.setExecutor(new RegaloCommand(this));
        }
        
        PluginCommand topregalosCmd = getCommand("topregalos");
        if (topregalosCmd != null) {
            topregalosCmd.setExecutor(new TopRegalosCommand(this));
        }
        
        // Registrar comando mailbox
        MailboxCommand mailboxCommand = new MailboxCommand(this);
        PluginCommand mailboxCmd = getCommand("mailbox");
        if (mailboxCmd != null) {
            mailboxCmd.setExecutor(mailboxCommand);
        }
        
        GiftBondCommand giftBondCommand = new GiftBondCommand(this);
        PluginCommand giftBondCmd = getCommand("giftbond");
        if (giftBondCmd != null) {
            giftBondCmd.setExecutor(giftBondCommand);
            giftBondCmd.setTabCompleter(giftBondCommand);
        }
        
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
        if (configManager != null && configManager.getGiftsConfig() != null) {
            String mode = configManager.getGiftsConfig().getString("mode", "manual");
            if (mode != null) {
                mode = mode.toLowerCase();
                
                if (!mode.equals("auto")) {
                    getLogger().info("Modo MANUAL - Sistema de regalos aleatorios desactivado");
                    return;
                }
            }
        } else {
            getLogger().warning("ConfigManager no disponible - sistema aleatorio desactivado");
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
        if (configManager != null && configManager.getGiftsConfig() != null) {
            boolean rotationEnabled = configManager.getGiftsConfig().getBoolean("auto_mode.rotation.enabled", true);
            if (rotationEnabled) {
                startRotationTask();
            }
        }
        
        getLogger().info("═══════════════════════════════════════");
        getLogger().info("  SISTEMA ALEATORIO LISTO");
        getLogger().info("═══════════════════════════════════════");
    }
    
    /**
     * Inicia la tarea de rotación automática de regalos
     */
    private void startRotationTask() {
        final int intervalMinutes;
        if (configManager != null && configManager.getGiftsConfig() != null) {
            intervalMinutes = configManager.getGiftsConfig().getInt("auto_mode.rotation.interval", 60);
        } else {
            intervalMinutes = 60; // Valor por defecto
        }
        
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
        
        getLogger().info(() -> "Task de rotación iniciada (intervalo: " + intervalMinutes + " minutos)");
    }
    
    @Override
    public void onDisable() {
        // Cancelar task de rotación
        if (rotationTask != null) {
            rotationTask.cancel();
        }
        
        // Cerrar StorageManager (gestiona todos los almacenamientos)
        if (storageManager != null) {
            storageManager.close();
        }
        
        getLogger().info("GiftBond disabled successfully!");
    }
    
    /**
     * Obtiene la instancia singleton del plugin
     * @return Instancia de GiftBond
     */
    public static GiftBond getInstance() {
        return instance;
    }
    
    /**
     * Obtiene el DatabaseManager (legacy)
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
    
    /**
     * Obtiene el FriendshipManager
     * @return FriendshipManager instance
     */
    public FriendshipManager getFriendshipManager() {
        return friendshipManager;
    }
    
    /**
     * Obtiene el EconomyManager
     * @return EconomyManager instance
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    /**
     * Obtiene el GiftManager
     * @return GiftManager instance
     */
    public GiftManager getGiftManager() {
        return giftManager;
    }
    
    /**
     * Obtiene el GiftMenu
     * @return GiftMenu instance
     */
    public GiftMenu getGiftMenu() {
        return giftMenu;
    }
    
    /**
     * Obtiene el HistoryMenu
     * @return HistoryMenu instance
     */
    public HistoryMenu getHistoryMenu() {
        return historyMenu;
    }
    
    /**
     * Obtiene un mensaje del config con valor por defecto
     * @param path Ruta del mensaje en messages.yml
     * @param defaultMsg Mensaje por defecto si no se encuentra
     * @return Mensaje formateado
     */
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