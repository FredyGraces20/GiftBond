# GiftBond v1.4.0 - Advanced Gift and Friendship System

Advanced friendship and gift system for Minecraft (Spigot/Paper) with modular configuration, PlaceholderAPI integration, mailbox system, dynamic points calculation, and plugin protection.

**License:** GNU General Public License v3.0  
**Author:** Fredy_Graces  
**Version:** 1.4.0

## 🛡️ Security and Protection

GiftBond v1.1.0 includes advanced security measures to protect the plugin:

### 🔒 Anti-Decompilation Protection
* **License Verification**: Runtime license verification system
* **String Protection**: Basic encryption for sensitive strings
* **Obfuscation**: ProGuard configuration to make reverse engineering difficult
* **Integrity Validation**: Author and plugin name verification

### ⚖️ License
* **GPL v3.0**: Source code available under open-source license
* **Copyright**: Protected by Fredy_Graces copyright
* **Free Distribution**: Allows modification and redistribution under the same license

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
*   **📫 Advanced Mailbox System**: Store physical gifts until recipients claim them with SQLite database integration.
    *   **Gift Storage**: Automatically saves expensive gifts until claimed
    *   **Recipient Sharing**: Configurable percentage of gift cost shared with recipient
    *   **Expiration System**: Automatic cleanup of old gifts (configurable duration)
    *   **Inventory Protection**: Prevents duplicate claims and ensures proper space checking
    *   **Multiple Claim Options**: Individual sender claims or bulk "all" claims
*   **🔄 Dynamic Points System**: Points are calculated at claim time based on sender's current boost status
    *   **Real-time Boost Calculation**: Points reflect sender's ACTIVE boost when gift is claimed
    *   **Backward Compatible**: Existing gifts maintain original point values
    *   **Configurable**: Toggle dynamic points system on/off in config.yml
    *   **Fair Distribution**: Recipients benefit from sender's current boost level

## 📜 Commands

### Player Commands
*   **/friends**: View your total friendship points, personal balance, and top friends.
*   **/friends history**: View your complete gift history with pagination (sent and received gifts).
*   **/gift <player>**: Open the gift selection menu for a specific player.
*   **/gift claim [nick|all]**: Claim pending gifts from specific senders or all at once.
*   **/mailbox [nick|all]**: Alternative command to claim pending gifts.
*   **/leaderboard**: Display the top 10 friendship pairs on the server.

### Admin Commands
*   **/giftbond reload**: Reload the plugin configuration and gift definitions.
*   **/giftbond savedata**: Force a manual backup of the database.
*   **/giftbond points <player> <view|add|remove|set> [amount]**: Manage any player's personal point balance.
*   **/giftbond boost <player> <multiplier> [minutes]**: Grant a temporary boost to a specific player (default: 60 minutes).
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

## 📫 Mailbox System

GiftBond includes an advanced mailbox system that stores physical gifts until recipients claim them:

### 🎁 How It Works
When players send expensive gifts, they're automatically stored in the recipient's mailbox instead of being delivered immediately. Recipients can claim gifts at their convenience.

### 📋 Commands
```bash
/regalo reclamar           # View list of senders with pending gifts
/regalo reclamar <nick>    # Claim gifts from a specific sender
/regalo reclamar all       # Claim all pending gifts at once
/mailbox                   # Alternative command (same functionality)
```

### ⚙️ Configuration
```yaml
# config.yml
mailbox:
  enabled: true
  shared_percentage: 25    # Percentage of gift cost shared with recipient
  auto_claim_free_gifts: true
  storage_duration_days: 7 # 0 = permanent storage
  max_pending_gifts: 50
  cleanup_interval_minutes: 60
```

### 🛡️ Features
*   **Anti-Dupe Protection**: Items are removed from sender's inventory only when claimed
*   **Space Validation**: Checks recipient's inventory space before claiming
*   **Notification System**: Sends notifications to both sender and recipient
*   **Automatic Cleanup**: Removes expired gifts based on configured duration
*   **Flexible Claiming**: Individual or bulk gift claiming options

## 🔄 Dynamic Points System

GiftBond v1.1.0 introduces a revolutionary dynamic points system that calculates friendship points at **claim time** based on the sender's **current boost status**.

### 🎯 How It Works

**Traditional System (v1.0.0):**
```
⏰ Send Time: Sender has x2.0 boost
🎁 Sends gift worth 100 points
📊 Points awarded: 200 points (calculated at send time)
📥 Recipient claims: Receives items only
```

**Dynamic System (v1.1.0):**
```
⏰ Send Time: Sender has x2.0 boost
🎁 Sends gift worth 100 points
📊 Points stored: Base 100 + Boosted 200
📥 Claim Time: System checks sender's CURRENT boost
📈 If sender still has x2.0 boost: Recipient gets 200 points
📉 If sender lost boost: Recipient gets 100 points
📦 Recipient receives: Items + Dynamically calculated points
```

### ⚙️ Configuration

```yaml
# config.yml
mailbox:
  dynamic_points:
    enabled: true  # Set to false for traditional behavior
```

### 🎁 Benefits

*   **公平性**: Recipients benefit from sender's current boost level
*   **激励保持**: Encourages players to maintain their boost status
*   **实时计算**: Points reflect actual boost conditions at claim time
*   **向后兼容**: Existing gifts work with original point values
*   **可配置**: Server owners can choose preferred behavior

### 📝 Example Scenarios

**Scenario 1: Boost Maintained**
```
Day 1: Player A (x2.0 boost) sends gift to Player B
Day 3: Player A still has x2.0 boost
Day 5: Player B claims gift
Result: Player B receives 200 points (100 base × 2.0 boost)
```

**Scenario 2: Boost Lost**
```
Day 1: Player A (x2.0 boost) sends gift to Player B  
Day 3: Player A loses boost (back to x1.0)
Day 5: Player B claims gift
Result: Player B receives 100 points (100 base × 1.0 boost)
```

### 🛠️ Technical Details

*   **数据存储**: Both base points and boosted points are stored
*   **实时查询**: System queries sender's active boost at claim time
*   **错误处理**: Graceful fallback to base points if boost calculation fails
*   **性能优化**: Efficient database queries with proper indexing

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
*   **Mailbox Operations**: Gift storage, claiming, and cleanup processes

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
[GiftBond] [MAILBOX] Saved gift from Notch to Steve (ID: 123)
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
*   **Mailbox**: Configure mailbox system behavior, sharing percentages, and cleanup settings.

### Debug Configuration
```yaml
debug:
  enabled: false  # Set to true for development/troubleshooting
```

### Mailbox Configuration
```yaml
mailbox:
  enabled: true
  shared_percentage: 25    # Percentage shared with recipient
  auto_claim_free_gifts: true
  storage_duration_days: 7
  max_pending_gifts: 50
  cleanup_interval_minutes: 60
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
*   **PlaceholderAPI**: Required for playtime verification and placeholders
*   **Statistic Expansion**: Needed for `%statistic_hours_played%` (Install with `/papi ecloud download Statistic`)

### Optional Dependencies

*   **Vault**: For economy integration (optional)
*   **DiscordSRV**: For Discord notifications (works with top1_commands)
*   **LuckPerms**: For permission management and permanent boosts

## 📁 Configuration Files

On first load, GiftBond automatically generates these modular configuration files:

*   **`config.yml`** - Main plugin settings and boost configuration
*   **`messages.yml`** - All user-facing messages and translations
*   **`gifts.yml`** - Gift system configuration (auto/manual modes)
*   **`database.yml`** - Database and backup settings

All files include comprehensive default configurations and comments for easy customization.

## 🔄 Version History

### v1.1.0 (Current)
*   ✅ **Dynamic Points System**: Points calculated at claim time based on sender's current boost status
*   ✅ **Plugin Protection**: License verification system and anti-decompilation protection
*   ✅ **Mailbox Improvements**: Precise inventory space validation (slots 0-35 only)
*   ✅ **Code Optimization**: Performance improvements and code cleanup
*   ✅ **Documentation Updates**: README and YAML files with new information

### v1.0.0
*   ✅ Base gift and friendship system
*   ✅ PlaceholderAPI integration
*   ✅ Mailbox system with SQLite
*   ✅ Dual boost system (permissions + temporary)
*   ✅ Auto/manual gift modes
*   ✅ Advanced debug system

## 📞 Support and Contributions

To report bugs, suggest improvements, or contribute to development:

*   **Issues**: [GitHub Repository](https://github.com/fredygraces/giftbond/issues)
*   **Discord**: Official support server
*   **Email**: fredy.graces@example.com

---

**© 2026 Fredy_Graces** - All rights reserved  
**GPL v3.0 License** - Source code freely available
