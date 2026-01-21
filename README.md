# GiftBond

Advanced friendship and gift system for Minecraft (Spigot/Paper) with modular configuration and PlaceholderAPI integration.

## 🌟 Features

*   **Modular Configuration System**: Split configuration into dedicated YAML files (`config.yml`, `messages.yml`, `gifts.yml`, `database.yml`) that auto-generate on first load.
*   **Configurable Prefix**: Customize the plugin's message prefix (e.g., `[GiftBond]`, `[San Valentín]`, etc.) from messages.yml.
*   **Dual Boost System**: Combine permanent permission-based boosts with temporary admin-granted boosts that multiply together for maximum flexibility.
    *   **Permission Boosts**: Permanent multipliers for VIP ranks, event groups, etc. (configured in config.yml)
    *   **Temporal Boosts**: Admin command to grant temporary boosts to specific players that stack with permission boosts
*   **Dual Point System**: Earn both partnership points (for rankings) and personal points (to spend). Configurable to reward both sender and receiver.
*   **Gift History System**: Track all sent and received gifts with a paginated GUI (`/amistad historial`).
*   **Daily Gift Limit**: Set a maximum number of gifts per day to prevent spam.
*   **Top 1 Broadcast**: Automatic server-wide announcement when a new couple reaches first place.
*   **Top 1 Custom Commands**: Execute custom commands (e.g., DiscordSRV notifications, rewards) when a couple reaches #1.
*   **Master Switch**: Toggle all plugin restrictions (cooldowns, playtime, etc.) globally from the config.
*   **Interactive Gift Menu**: `/regalo <player>` opens a GUI with configurable gift items.
*   **Inventory Payment**: Gifts can require specific items from the player's inventory as payment.
*   **Playtime Requirement**: Configurable minimum playtime (e.g., 12 hours) to send or receive gifts using PlaceholderAPI with enable/disable toggle.
*   **Dynamic UI**: Menu descriptions update automatically to show boosted rewards.
*   **Automated Backups**: Hourly snapshots and final shutdown backups of the SQLite/H2 database.
*   **Color Support**: Full support for `&` and `§` color codes in configuration and menus.
*   **PlaceholderAPI Integration**: Comprehensive placeholder support for displaying rankings and points.
*   **Auto/Manual Gift Modes**: Choose between automatic randomized gifts or manual configuration.
*   **Version Compatibility**: Automatic Minecraft version detection with manual override option (1.20.4 to 1.21.11).
*   **Multi-Database Support**: SQLite and H2 database providers with automatic backup system.
*   **🔧 Debug Mode**: Advanced debugging system with `/giftbond debug` command to enable/disable console logging.
*   **🔄 Auto-Gift Session Management**: Seamless gift sending in auto mode with proper recipient tracking.
*   **🛡️ Conditional Logging**: All debug messages respect the `debug.enabled` configuration flag.

## 📜 Commands

### Player Commands
*   **/amistad**: View your total friendship points, personal balance, and top friends.
*   **/amistad historial**: View your complete gift history with pagination (sent and received gifts).
*   **/regalo <jugador>**: Open the gift selection menu for a specific player.
*   **/topregalos**: Display the top 10 friendship pairs on the server.

### Admin Commands
*   **/giftbond reload**: Reload the plugin configuration and gift definitions.
*   **/giftbond savedata**: Force a manual backup of the database.
*   **/giftbond points <jugador> <view|add|remove|set> [cantidad]**: Manage any player's personal point balance.
*   **/giftbond boost <jugador> <multiplicador> [minutos]**: Grant a temporary boost to a specific player (default: 60 minutes).
*   **/giftbond random**: Generate new random gifts (in auto mode).
*   **/giftbond debug <on|off>**: Enable or disable debug mode for detailed console logging.

## 🔑 Permissions

*   `giftbond.use`: Access to basic player commands (Default: true).
*   `giftbond.admin`: Master admin permission.
*   `giftbond.savedata`: Permission to use `/giftbond savedata` (Default: op).
*   `giftbond.admin.reload`: Permission to use `/giftbond reload` (Default: op).
*   `giftbond.admin.points`: Permission to use `/giftbond points` (Default: op).
*   `giftbond.admin.boost`: Permission to use `/giftbond boost` (Default: op).
*   `giftbond.admin`: Permission to use `/giftbond debug` (Default: op).

### Boost Permissions (Permanent)
Configure permanent boost permissions in `config.yml`. Examples:
*   `giftbond.boost.vip`: VIP boost (1.5x multiplier)
*   `giftbond.boost.premium`: Premium boost (2.0x multiplier)
*   `giftbond.boost.ultra`: Ultra boost (3.0x multiplier)

You can create unlimited custom boost tiers with any permission name and multiplier.

## 🚀 Debug System

GiftBond includes an advanced debugging system that can be toggled on/off:

### 🔧 Debug Command
```bash
/giftbond debug <on|off>

# Examples:
/giftbond debug on     # Enable debug mode
/giftbond debug off    # Disable debug mode
/giftbond debug        # Show current debug status
```

### 📊 What Debug Mode Shows
When enabled, debug mode displays detailed information about:
*   **Gift Processing**: Item validation, point calculations, inventory operations
*   **Player Verification**: Playtime requirements, permission checks, boost calculations
*   **Auto-Gift Generation**: Random gift creation, material filtering, point assignment
*   **Session Management**: Gift session tracking, recipient validation
*   **Database Operations**: Save operations, query performance

### 🛡️ Production Safety
*   **Default OFF**: Debug mode is disabled by default in production
*   **Configuration Controlled**: All debug output respects `debug.enabled` in config.yml
*   **Zero Spam**: No debug messages appear in console when disabled
*   **Immediate Effect**: Changes take effect immediately without restart

### 🎯 Example Output (Debug ON)
```
[GiftBond] [DEBUG] Processing gift selection for Notch -> Steve
[GiftBond] [DEBUG] Checking hours requirement for Notch:
[GiftBond] [DEBUG]   Master enabled: true
[GiftBond] [DEBUG]   Hours requirement enabled: true
[GiftBond] [DEBUG]   Min hours required: 1
[GiftBond] [BOOST] Base points: 25, Multiplier: 2.0
[GiftBond] [BOOST] Final points calculated: 50
[GiftBond] [AUTO GIFT] Creating gift: random_diamond, Material: DIAMOND, Amount: 1, Points: 75
```

## 🎲 Auto/Manual Gift Modes

GiftBond supports two operational modes for gift configuration:

### Auto Mode (Recommended)

Automatic gift generation with version compatibility:

```yaml
# gifts.yml
mode: "auto"

auto_mode:
  enabled: true
  # Automatic server version detection
  detect_version: true
  # Manual override (leave empty "" for auto-detection)
  force_version: ""
  
  # Item filtering by category
  allowed_categories:
    food: true
    blocks: true
    tools: true
    weapons: true
    # ... other categories
  
  # Automatic rotation settings
  rotation:
    enabled: true
    interval: 60  # minutes
    active_gifts: 9
    broadcast_on_change: true
```

**Features:**
*   🔄 Automatic item randomization based on server version
*   🎯 Version-aware item filtering (1.20.4 to 1.21.11)
*   ⏰ Configurable rotation intervals
*   📢 Broadcast notifications on gift changes
*   ⚙️ Manual version override capability
*   🔧 Session management for proper recipient tracking

### Manual Mode

Traditional fixed gift configuration:

```yaml
mode: "manual"

manual_mode:
  enabled: true
  gifts:
    diamante:
      name: "Diamante"
      points: 50
      material: "DIAMOND"
      amount: 1
      description: "Un regalo brillante y valioso"
    # ... other gifts
```

## 🛠️ Configuration

The `config.yml` allows you to customize:
*   **Prefix**: Custom plugin prefix for all messages (e.g., `[GiftBond]`, `[San Valentín]`, etc.).
*   **Boosts**: Define unlimited permission-based boost tiers with custom multipliers and permission nodes.
*   **Gifts**: Name, points, required items, and description.
*   **Requirements**: Minimum hours played with enable/disable toggle and the PlaceholderAPI placeholder to use.
*   **Settings**: Master enabled toggle, dual personal points reward, boost for receivers, cooldowns, self-gifting, daily gift limit, top 1 broadcast, and max friends display.
*   **Messages**: Full translation support for all plugin alerts and notifications using the `{prefix}` placeholder.
*   **Debug**: Enable/disable debug mode for development and troubleshooting.

### Debug Configuration
```yaml
debug:
  enabled: false  # Set to true for development/troubleshooting
```

### Example Boost Configuration
```yaml
boosts:
  vip:
    multiplier: 1.5
    permission: "giftbond.boost.vip"
  premium:
    multiplier: 2.0
    permission: "giftbond.boost.premium"
  ultra:
    multiplier: 3.0
    permission: "giftbond.boost.ultra"
  evento_sanvalentin:
    multiplier: 2.5
    permission: "giftbond.boost.evento"
```

### Top 1 Custom Commands
You can execute custom commands when a couple reaches #1. Perfect for DiscordSRV integration or giving rewards:

```yaml
settings:
  broadcast_top1: true
  top1_commands:
    - "discordsrv broadcast ✨ **{player1}** y **{player2}** son ahora la pareja Nº1 con **{points} puntos!** ✨"
    - "give {player1} diamond 5"
    - "give {player2} diamond 5"
    - "lp user {player1} permission set couples.top1 true"
```

**Available Placeholders:**
- `{player1}` - First player's name
- `{player2}` - Second player's name
- `{points}` - Total friendship points

**Features:**
- Commands execute from console (no permission checks)
- Multiple commands supported (execute in order)
- Works with any plugin (DiscordSRV, LuckPerms, EssentialsX, etc.)
- Automatic placeholder replacement

## 📋 Requirements

*   **Spigot/Paper**: 1.20.4 - 1.21.11 (version-aware item compatibility)
*   **PlaceholderAPI**: Required for playtime verification and placeholders.
*   **Statistic Expansion**: Needed for `%statistic_hours_played%` (Install with `/papi ecloud download Statistic`).

### Optional Dependencies

*   **Vault**: For economy integration (optional)
*   **DiscordSRV**: For Discord notifications (works with top1_commands)

## 📁 Configuration Files

On first load, GiftBond automatically generates these modular configuration files:

*   **`config.yml`** - Main plugin settings and boost configuration
*   **`messages.yml`** - All user-facing messages and translations
*   **`gifts.yml`** - Gift system configuration (auto/manual modes)
*   **`database.yml`** - Database and backup settings

All files include comprehensive default configurations and comments for easy customization.