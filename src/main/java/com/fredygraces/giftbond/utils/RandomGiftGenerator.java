package com.fredygraces.giftbond.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;

import com.fredygraces.giftbond.GiftBond;
import com.fredygraces.giftbond.models.GiftItem;

/**
 * Generador de regalos aleatorios
 * Genera regalos completamente aleatorios sin modificadores de rareza
 */
public class RandomGiftGenerator {
    
    private final GiftBond plugin;
    private final ItemFilter itemFilter;
    private final DebugLogger debugLogger;
    private final Random random;
    private List<Material> validItems;
    private final List<RandomGift> currentGifts;
    private final List<RandomMoneyGift> currentMoneyGifts;
    private long nextRotationTime;
    
    public RandomGiftGenerator(GiftBond plugin, ItemFilter itemFilter) {
        this.plugin = plugin;
        this.itemFilter = itemFilter;
        this.debugLogger = new DebugLogger(plugin);
        this.random = new Random();
        this.validItems = new ArrayList<>();
        this.currentGifts = new ArrayList<>();
        this.currentMoneyGifts = new ArrayList<>();
        this.nextRotationTime = 0;
    }
    
    /**
     * Inicializa el generador
     */
    public void initialize() {
        // Cargar items válidos
        this.validItems = itemFilter.getValidItems();
        
        if (validItems.isEmpty()) {
            plugin.getLogger().severe("¡No hay items válidos para generar regalos!");
            plugin.getLogger().severe("Revisa la configuración de categorías y blacklist");
            return;
        }
        
        // Generar regalos iniciales
        generateNewGifts();
        
        // plugin.getLogger().info(() -> "Generador de regalos inicializado con " + validItems.size() + " items disponibles");
    }
    
    /**
     * Genera nuevos regalos aleatorios
     */
    public void generateNewGifts() {
        int giftCount = plugin.getConfigManager().getGiftsConfig().getInt("auto_mode.rotation.active_gifts", 9);
        
        // Limpiar regalos actuales
        currentGifts.clear();
        currentMoneyGifts.clear();
        
        // Generar nuevos regalos de items
        for (int i = 0; i < giftCount; i++) {
            RandomGift gift = generateSingleGift();
            currentGifts.add(gift);
        }

        // Generar nuevos regalos de dinero configurables (9 botones)
        for (int button = 1; button <= 9; button++) {
            String buttonPath = "auto_mode.money_gifts.button_" + button;
            
            // Verificar si el botón está habilitado
            if (!plugin.getConfigManager().getGiftsConfig().getBoolean(buttonPath + ".enabled", true)) {
                currentMoneyGifts.add(new RandomMoneyGift(0, 0)); // Botón deshabilitado
                continue;
            }
            
            // Obtener rangos configurados
            double priceMin = plugin.getConfigManager().getGiftsConfig().getDouble(buttonPath + ".price.min", 1000);
            double priceMax = plugin.getConfigManager().getGiftsConfig().getDouble(buttonPath + ".price.max", 10000);
            int pointsMin = plugin.getConfigManager().getGiftsConfig().getInt(buttonPath + ".points.min", 100);
            int pointsMax = plugin.getConfigManager().getGiftsConfig().getInt(buttonPath + ".points.max", 500);
            
            // Generar valores aleatorios dentro de los rangos (números redondos)
            int amount = (int) Math.round(generateRandomDouble(priceMin, priceMax));
            int points = random.nextInt(pointsMax - pointsMin + 1) + pointsMin;
            
            currentMoneyGifts.add(new RandomMoneyGift(amount, points));
        }
        
        // Calcular tiempo de próxima rotación
        int intervalMinutes = plugin.getConfigManager().getGiftsConfig().getInt("auto_mode.rotation.interval", 60);
        nextRotationTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L);
        
/*
        plugin.getLogger().info("═══════════════════════════════════════");
        plugin.getLogger().info("  NUEVOS REGALOS GENERADOS");
        plugin.getLogger().info(() -> "  Total: " + currentGifts.size() + " regalos");
        plugin.getLogger().info(() -> "  Próxima rotación: " + intervalMinutes + " minutos");
        plugin.getLogger().info("═══════════════════════════════════════");
        
        // Log de regalos generados
        for (int i = 0; i < currentGifts.size(); i++) {
            RandomGift gift = currentGifts.get(i);
            plugin.getLogger().info(String.format("  Regalo %d: %dx %s = %d puntos", 
                i + 1, gift.getAmount(), gift.getMaterial().name(), gift.getPoints()));
        }
        plugin.getLogger().info("═══════════════════════════════════════");
*/
        
        // Broadcast si está habilitado
        if (plugin.getConfigManager().getGiftsConfig().getBoolean("auto_mode.rotation.broadcast_on_change", true)) {
            String message = plugin.getConfigManager().getMessage("broadcasts.gifts_rotated", "&e&l⚡ ¡Los regalos han cambiado! &7Usa &f/regalo &7para ver los nuevos.");
            plugin.getServer().broadcast(org.bukkit.ChatColor.translateAlternateColorCodes('&', message), "");
        }
    }
    
    /**
     * Genera un solo regalo aleatorio
     */
    private RandomGift generateSingleGift() {
        // Elegir material aleatorio
        Material material = validItems.get(random.nextInt(validItems.size()));
        
        // Cantidad aleatoria (completamente aleatoria, sin modificadores)
        int minAmount = plugin.getConfigManager().getGiftsConfig().getInt("auto_mode.items.min", 1);
        int maxAmount = plugin.getConfigManager().getGiftsConfig().getInt("auto_mode.items.max", 64);
        int amount = random.nextInt(maxAmount - minAmount + 1) + minAmount;
        
        // Ajustar cantidad si excede el stack size del item
        int maxStackSize = material.getMaxStackSize();
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }
        
        // Puntos aleatorios (completamente aleatorios, sin modificadores)
        int minPoints = plugin.getConfigManager().getGiftsConfig().getInt("auto_mode.points.min", 10);
        int maxPoints = plugin.getConfigManager().getGiftsConfig().getInt("auto_mode.points.max", 100);
        int points = random.nextInt(maxPoints - minPoints + 1) + minPoints;
        
        return new RandomGift(material, amount, points);
    }

    /**
     * Genera un double aleatorio entre min y max
     */
    private double generateRandomDouble(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
    
    /**
     * Obtiene los regalos de dinero actuales
     */
    public List<RandomMoneyGift> getCurrentMoneyGifts() {
        return new ArrayList<>(currentMoneyGifts);
    }
    
    /**
     * Obtiene los regalos actuales
     */
    public List<RandomGift> getCurrentGifts() {
        return new ArrayList<>(currentGifts);
    }
    
    /**
     * Convierte los regalos aleatorios a GiftItems
     */
    public List<GiftItem> getCurrentGiftsAsGiftItems() {
        List<GiftItem> giftItems = new ArrayList<>();
        
        debugLogger.debug("[AUTO GIFT] Converting " + currentGifts.size() + " random gifts to GiftItems");
        
        for (RandomGift randomGift : currentGifts) {
            String key = "random_" + randomGift.getMaterial().name().toLowerCase();
            String friendlyName = itemFilter.getFriendlyName(randomGift.getMaterial());
            
            debugLogger.debug("[AUTO GIFT] Creating gift: " + key + ", Material: " + randomGift.getMaterial() + ", Amount: " + randomGift.getAmount() + ", Points: " + randomGift.getPoints());
            
            GiftItem giftItem = new GiftItem(
                key,
                plugin.getConfigManager().getMessageRaw("random_gifts.item_name", "&6&l✦ &e{item} &6&l✦").replace("{item}", friendlyName),
                randomGift.getPoints(),
                randomGift.getMaterial(),
                randomGift.getAmount(),
                Arrays.asList(
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_separator", "&8&m━━━━━━━━━━━━━━━━━━━━"),
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_title", "&7&o🎁 Regalo del Momento"),
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_separator", "&8&m━━━━━━━━━━━━━━━━━━━━"),
                    "",
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_reward", "&a&l💰 Premio: &f{points} puntos").replace("{points}", String.valueOf(randomGift.getPoints())),
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_cost", "&c&l📦 Costo: &f{amount}x &e{item}").replace("{amount}", String.valueOf(randomGift.getAmount())).replace("{item}", friendlyName),
                    "",
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_rotation", "&7⏰ Los regalos rotan cada hora"),
                    plugin.getConfigManager().getMessageRaw("random_gifts.lore_footer", "&7&o¡Aprovecha esta oportunidad!")
                )
            );
            
            giftItems.add(giftItem);
        }
        
        return giftItems;
    }
    
    /**
     * Obtiene el tiempo restante hasta la próxima rotación en milisegundos
     */
    public long getTimeUntilNextRotation() {
        return Math.max(0, nextRotationTime - System.currentTimeMillis());
    }
    
    /**
     * Obtiene el tiempo restante formateado (ej: "45 minutos")
     */
    public String getTimeUntilNextRotationFormatted() {
        long timeLeft = getTimeUntilNextRotation();
        long minutes = timeLeft / (60 * 1000);
        long seconds = (timeLeft % (60 * 1000)) / 1000;
        
        if (minutes > 0) {
            return minutes + " minutos";
        } else {
            return seconds + " segundos";
        }
    }
    
    /**
     * Verifica si es tiempo de rotar
     */
    public boolean shouldRotate() {
        return System.currentTimeMillis() >= nextRotationTime;
    }
    
    /**
     * Recarga el generador (usado por /giftbond reload)
     */
    public void reload() {
        // plugin.getLogger().info("Recargando generador de regalos...");
        this.validItems = itemFilter.getValidItems();
        generateNewGifts();
    }
    
    /**
     * Clase interna para representar un regalo de dinero
     */
    public static class RandomMoneyGift {
        private final int amount;  // Cambiado a int para números redondos
        private final int points;

        public RandomMoneyGift(int amount, int points) {
            this.amount = amount;
            this.points = points;
        }

        public int getAmount() {  // Cambiado a int
            return amount;
        }

        public int getPoints() {
            return points;
        }
    }

    /**
     * Clase interna para representar un regalo aleatorio
     */
    public static class RandomGift {
        private final Material material;
        private final int amount;
        private final int points;
        
        public RandomGift(Material material, int amount, int points) {
            this.material = material;
            this.amount = amount;
            this.points = points;
        }
        
        public Material getMaterial() {
            return material;
        }
        
        public int getAmount() {
            return amount;
        }
        
        public int getPoints() {
            return points;
        }
    }
}
