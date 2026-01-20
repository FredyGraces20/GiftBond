# GiftBond

Advanced friendship and gift system for Minecraft (Spigot/Paper).

## 🌟 Features

*   **Configurable Prefix**: Customize the plugin's message prefix (e.g., `[GiftBond]`, `[San Valentín]`, etc.) from config.yml.
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
*   **Playtime Requirement**: Configurable minimum playtime (e.g., 12 hours) to send or receive gifts using PlaceholderAPI.
*   **Dynamic UI**: Menu descriptions update automatically to show boosted rewards.
*   **Automated Backups**: Hourly snapshots and final shutdown backups of the SQLite database.
*   **Color Support**: Full support for `&` and `§` color codes in configuration and menus.

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

## 🔑 Permissions

*   `giftbond.use`: Access to basic player commands (Default: true).
*   `giftbond.admin`: Master admin permission.
*   `giftbond.savedata`: Permission to use `/giftbond savedata` (Default: op).
*   `giftbond.admin.reload`: Permission to use `/giftbond reload` (Default: op).
*   `giftbond.admin.points`: Permission to use `/giftbond points` (Default: op).
*   `giftbond.admin.boost`: Permission to use `/giftbond boost` (Default: op).

### Boost Permissions (Permanent)
Configure permanent boost permissions in `config.yml`. Examples:
*   `giftbond.boost.vip`: VIP boost (1.5x multiplier)
*   `giftbond.boost.premium`: Premium boost (2.0x multiplier)
*   `giftbond.boost.ultra`: Ultra boost (3.0x multiplier)

You can create unlimited custom boost tiers with any permission name and multiplier.

## 🚀 Boost System Explained

The plugin features a **dual boost system** that combines two types of multipliers:

### 1️⃣ Permission-Based Boosts (Permanent)
Configured in `config.yml`:
```yaml
boosts:
  vip:
    multiplier: 1.5
    permission: "giftbond.boost.vip"
  premium:
    multiplier: 2.0
    permission: "giftbond.boost.premium"
  evento:
    multiplier: 2.5
    permission: "giftbond.boost.evento"
```

**How to grant:**
```bash
# Using LuckPerms
/lp user PlayerName permission set giftbond.boost.vip true
/lp group VIP permission set giftbond.boost.vip true

# For server-wide events
/lp group default permission set giftbond.boost.evento true
```

**Characteristics:**
- ✅ Permanent (until permission is removed)
- ✅ Persists through restarts
- ✅ If player has multiple boost permissions, uses the highest one
- ✅ Managed through your permission plugin

### 2️⃣ Temporal Boosts (Admin Command)
Granted via command:
```bash
/giftbond boost <player> <multiplier> [minutes]

# Examples:
/giftbond boost Notch 2.0 30      # 2.0x for 30 minutes
/giftbond boost Steve 1.5         # 1.5x for 60 minutes (default)
```

**Characteristics:**
- ⏱️ Time-limited (auto-expires)
- ✅ Persists through restarts (saved in database)
- ✅ Works for online and offline players
- ✅ Player receives notification if online

### 🔢 How Boosts Combine (Multiplication)

Both boost types **multiply together**:

**Formula:** `Permission Boost × Temporal Boost = Final Multiplier`

**Examples:**

| Scenario | Permission | Temporal | Result |
|----------|------------|----------|--------|
| Only permission | x2.0 (Premium) | x1.0 | **x2.0** |
| Only temporal | x1.0 | x1.5 | **x1.5** |
| Both combined | x2.0 (Premium) | x1.5 | **x3.0** ✨ |
| Maximum power | x3.0 (Ultra) | x2.0 | **x6.0** 🚀 |

### ⏰ Expiration Behavior

**When temporal boost expires:**
- Player keeps their permission boost (if they have one)
- Example: `x2.0 (permission) × x1.0 (expired) = x2.0`

**When permission is removed:**
- Player keeps their temporal boost (if still active)
- Example: `x1.0 × x1.5 (temporal) = x1.5`

**When both end:**
- Returns to normal: `x1.0 × x1.0 = x1.0`

**Key Point:** Changes apply **immediately** on the next gift sent (no restart needed).

## 🛠️ Configuration

The `config.yml` allows you to customize:
*   **Prefix**: Custom plugin prefix for all messages (e.g., `[GiftBond]`, `[San Valentín]`, etc.).
*   **Boosts**: Define unlimited permission-based boost tiers with custom multipliers and permission nodes.
*   **Gifts**: Name, points, required items, and description.
*   **Requirements**: Minimum hours played and the PlaceholderAPI placeholder to use.
*   **Settings**: Master enabled toggle, dual personal points reward, boost for receivers, cooldowns, self-gifting, daily gift limit, top 1 broadcast, and max friends display.
*   **Messages**: Full translation support for all plugin alerts and notifications using the `{prefix}` placeholder.

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

*   **Spigot/Paper**: 1.21+
*   **PlaceholderAPI**: Required for playtime verification.
*   **Statistic Expansion**: Needed for `%statistic_hours_played%` (Install with `/papi ecloud download Statistic`).
