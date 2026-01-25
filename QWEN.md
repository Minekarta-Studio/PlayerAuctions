# PlayerAuctions Plugin - Development Context

## Project Overview

PlayerAuctions is a modern and feature-rich auction house plugin for PaperMC servers. It provides a robust, intuitive, and scalable platform for players to buy and sell items, fully integrated with Vault-based economies and PlaceholderAPI. The plugin supports Minecraft versions 1.16-1.21 with version-specific builds for optimal performance.

### Key Technologies
- **Platform**: PaperMC (Minecraft server software)
- **Language**: Java 21
- **Build System**: Maven
- **Database**: SQLite (with potential for MySQL)
- **GUI Framework**: InventoryFramework
- **Economy Integration**: Vault API
- **Placeholder Support**: PlaceholderAPI

### Architecture
The plugin follows a modular architecture with distinct packages for different concerns:
- `auction`: Core auction business logic and services
- `commands`: Command handling and tab completion
- `config`: Configuration management
- `economy`: Economy integration layer
- `gui`: Graphical user interface components
- `notification`: Player notification systems
- `players`: Player-specific settings and data
- `storage`: Data persistence layer
- `tasks`: Scheduled tasks and background operations
- `transaction`: Transaction logging and history
- `util`: Utility classes and helpers

## Building and Running

### Prerequisites
- Java 21 or higher
- Maven 3.6.0 or higher
- Git (for cloning dependencies)

### Build Commands
The project supports multiple build profiles for different Minecraft versions:

```bash
# Build modern versions (1.19-1.21) - default
mvn clean package

# Build legacy versions (1.16-1.18)
mvn clean package -Plegacy

# Build specific versions
mvn clean package -P1.20  # For 1.20.x
mvn clean package -P1.19  # For 1.19.x

# Build all versions using provided scripts
./build-all.sh    # Linux/MacOS
build-all.bat     # Windows
```

### Generated Artifacts
After building, JAR files are generated in the `target/` directory:
- `PlayerAuctions-2.0.0-Modern.jar` (~16MB) - Minecraft 1.19-1.21
- `PlayerAuctions-1.9.9-Legacy.jar` (~15MB) - Minecraft 1.16-1.18
- Version-specific builds like `PlayerAuctions-2.0.0-1.20.jar`

### Installation
1. Choose the correct version for your Minecraft server
2. Place the appropriate `PlayerAuctions-*.jar` file in your server's `plugins` folder
3. Restart or reload your server
4. Configure settings in `plugins/PlayerAuctions/config.yml`
5. Set up permissions as needed

## Development Conventions

### Code Structure
- All main classes are in the `com.minekarta.playerauction` package
- Each major feature has its own sub-package
- Service classes follow the singleton pattern or are instantiated in the main plugin class
- Async operations use a dedicated thread pool managed by the main plugin class

### Dependency Management
- Dependencies are shaded into the final JAR using Maven Shade Plugin
- Package relocation is used to avoid conflicts with other plugins
- Provided scope is used for server APIs (Paper, Vault, PlaceholderAPI)
- Compile scope is used for embedded libraries (SQLite, InventoryFramework)

### Configuration
- Configuration is managed through `config.yml` with defaults provided in resources
- The plugin supports multiple economy providers with fallback mechanisms
- GUI customization options are available through configuration
- Database settings are abstracted (currently SQLite by default)

### Testing
- Unit tests use JUnit Jupiter framework
- Debug commands are available when debug mode is enabled
- Manual testing through in-game commands is the primary testing method

## Key Features

### Auction Management
- Players can list items for auction with custom prices and durations
- Support for instant buy, bidding, and reserve prices
- Automatic expiration and cleanup of auctions
- Player-specific limits on active listings

### GUI System
- Modern inventory-based interface using InventoryFramework
- Configurable borders and layout
- Real-time updates when auctions are bought, sold, or expired
- Search and sorting capabilities
- Responsive design with intuitive controls

### Economy Integration
- Full Vault economy support
- Support for custom KartaEmeraldCurrency
- Tax system on auction sales
- Multiple economy provider fallback

### Notifications
- Multiple notification methods (chat, action bar, titles, sounds)
- Personal notification toggling
- Detailed transaction feedback

### Data Storage
- JSON files for persistent storage (replaces SQLite)
- Transaction logging system
- Player settings persistence
- Mailbox system for claimed items

## Commands and Permissions

### Main Commands
- `/ah` (aliases: `/auction`, `/auctionhouse`) - Main command to open GUI or manage listings
- Subcommands include: `sell`, `search`, `notify`, `history`, `listings`, `reload`

### Key Permissions
- `playerauctions.use` - Basic plugin usage (default: true)
- `playerauctions.sell` - Listing items for sale (default: true)
- `playerauctions.cancel` - Canceling own auctions (default: true)
- `playerauctions.reload` - Reloading configuration (default: op)
- `playerauctions.admin` - Administrative access (default: op)

## Troubleshooting

### Common Issues
- Economy not found: Ensure Vault and an economy plugin are installed
- GUI not opening: Check permissions and console for errors
- Border not showing: Verify `gui.border.enabled` in config.yml
- Performance issues: Consider using MySQL instead of SQLite for large servers

### Debug Commands
When debug mode is enabled in config.yml, the `/kahdebug` command becomes available for testing services, economy integration, and GUI functionality.

## Dependencies

### Runtime Dependencies
- Paper API (provided by server)
- Vault API (economy integration)
- PlaceholderAPI (optional)
- KartaEmeraldCurrency (optional)

### Embedded Dependencies
- SQLite JDBC driver
- SLF4J NOP logger
- SnakeYAML
- InventoryFramework
- HikariCP (for potential MySQL support)
- Google Protocol Buffers

## Project Structure
```
src/main/java/com/minekarta/playerauction/
├── auction/          # Auction business logic
├── commands/         # Command executors and tab completers
├── common/           # Shared utilities
├── config/           # Configuration management
├── economy/          # Economy integration
├── gui/              # GUI components
├── notification/     # Notification system
├── players/          # Player-specific settings
├── storage/          # Data persistence
├── tasks/            # Scheduled tasks
├── transaction/      # Transaction logging
├── util/             # General utilities
└── PlayerAuction.java # Main plugin class

src/main/resources/
├── config.yml        # Default configuration
├── messages.yml      # Localized messages
└── plugin.yml        # Plugin metadata
```