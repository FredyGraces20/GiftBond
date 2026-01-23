# Changelog

## v1.1.0 (2026-01-23)

### 🎉 Major Features Added
- **Advanced Mailbox System**: Physical gifts are now stored until recipients claim them
- **SQLite Database Integration**: Full migration from H2 to SQLite for mailbox storage
- **Gift Sharing**: Configurable percentage of gift cost shared with recipients
- **Inventory Space Validation**: Prevents claiming gifts when inventory is full
- **Detailed Error Messages**: Clear instructions when claiming fails due to space

### 🔧 Improvements
- **Enhanced Debug System**: More granular debug logging with `/giftbond debug` command
- **Better Error Handling**: Improved messages for inventory space issues
- **Performance Optimization**: Optimized database queries and resource management
- **Code Quality**: Applied Java 8+ best practices (lambda expressions, multicatch, try-with-resources)

### 🐛 Bug Fixes
- Fixed automatic gift system not processing items correctly
- Resolved debug messages appearing even when disabled
- Fixed mailbox command registration issues
- Corrected database connection lifecycle management

### ⚙️ Configuration Updates
- Added `mailbox` section in `config.yml` with new settings:
  - `enabled`: Toggle mailbox system
  - `shared_percentage`: Percentage of gift cost shared with recipient
  - `auto_claim_free_gifts`: Automatically claim free gifts
  - `storage_duration_days`: Duration to keep gifts (0 = permanent)
  - `max_pending_gifts`: Maximum gifts per player
  - `cleanup_interval_minutes`: Cleanup frequency

### 📦 Dependencies
- Updated to SQLite JDBC 3.42.0.0
- Maintained H2 2.2.224 for backward compatibility

---

## v1.0.0 (Initial Release)
- Basic friendship and gift system
- Points and ranking system
- Manual and automatic gift modes
- Boost system (permanent and temporary)
- Playtime requirements
- Database backup system