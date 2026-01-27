package com.minekarta.playerauction.notification;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.config.ConfigManager;
import com.minekarta.playerauction.util.MessageParser;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Manages broadcast notifications for auction events
 * Broadcasts to all online players or specific world based on configuration
 */
public class BroadcastManager {

    private final PlayerAuction plugin;
    private final ConfigManager configManager;

    private final boolean broadcastEnabled;
    private final boolean onListing;
    private final boolean onPurchase;
    private final BroadcastRange range;

    public BroadcastManager(PlayerAuction plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        // Load configuration
        this.broadcastEnabled = configManager.getConfig().getBoolean("auction.broadcast.enabled", true);
        this.onListing = configManager.getConfig().getBoolean("auction.broadcast.on-listing", true);
        this.onPurchase = configManager.getConfig().getBoolean("auction.broadcast.on-purchase", true);

        String rangeStr = configManager.getConfig().getString("auction.broadcast.range", "GLOBAL");
        this.range = BroadcastRange.valueOf(rangeStr.toUpperCase());
    }

    /**
     * Broadcast when a player lists an item
     *
     * @param playerName The player who listed the item
     * @param itemName The item name
     * @param quantity Item quantity
     * @param price Formatted price string
     * @param playerWorld The world where the player is located
     */
    public void broadcastListing(String playerName, String itemName, int quantity, String price, World playerWorld) {
        if (!broadcastEnabled || !onListing) {
            return;
        }

        // Get message from config
        String rawMessage = configManager.getMessages().getString("broadcast.item-listed",
            "<#F5A623>🛒</#F5A623> <#ECF0F1>{player}</#ECF0F1> <#7F8C8D>listed</#7F8C8D> <#FFFFFF>{item}</#FFFFFF> <#7F8C8D>×{quantity} for</#7F8C8D> <#2ECC71>{price}</#2ECC71>");

        // Replace placeholders
        String message = rawMessage
            .replace("{player}", playerName)
            .replace("{item}", itemName)
            .replace("{quantity}", String.valueOf(quantity))
            .replace("{price}", price);

        // Parse and broadcast
        broadcast(message, playerWorld);
    }

    /**
     * Broadcast when someone purchases an item
     *
     * @param buyerName The player who bought the item
     * @param sellerName The player who sold the item
     * @param itemName The item name
     * @param quantity Item quantity
     * @param price Formatted price string
     * @param buyerWorld The world where the buyer is located
     */
    public void broadcastPurchase(String buyerName, String sellerName, String itemName, int quantity, String price, World buyerWorld) {
        if (!broadcastEnabled || !onPurchase) {
            return;
        }

        // Get message from config
        String rawMessage = configManager.getMessages().getString("broadcast.item-purchased",
            "<#2ECC71>✔</#2ECC71> <#ECF0F1>{buyer}</#ECF0F1> <#7F8C8D>bought</#7F8C8D> <#FFFFFF>{item}</#FFFFFF> <#7F8C8D>×{quantity} from</#7F8C8D> <#ECF0F1>{seller}</#ECF0F1> <#7F8C8D>for</#7F8C8D> <#F5A623>{price}</#F5A623>");

        // Replace placeholders
        String message = rawMessage
            .replace("{buyer}", buyerName)
            .replace("{seller}", sellerName)
            .replace("{item}", itemName)
            .replace("{quantity}", String.valueOf(quantity))
            .replace("{price}", price);

        // Parse and broadcast
        broadcast(message, buyerWorld);
    }

    /**
     * Internal method to broadcast a message based on configured range
     *
     * @param message The message to broadcast (with MiniMessage formatting)
     * @param referenceWorld The world to use for WORLD range
     */
    private void broadcast(String message, World referenceWorld) {
        // Parse message to Component (supports MiniMessage, hex colors, clickable text)
        Component component = MessageParser.parse(message);

        // Broadcast based on range
        switch (range) {
            case GLOBAL:
                // Broadcast to all online players
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(component);
                }
                break;

            case WORLD:
                // Broadcast only to players in the same world
                if (referenceWorld != null) {
                    for (Player player : referenceWorld.getPlayers()) {
                        player.sendMessage(component);
                    }
                }
                break;

            case NONE:
            default:
                // No broadcast
                break;
        }
    }

    /**
     * Enum for broadcast range options
     */
    public enum BroadcastRange {
        /**
         * Broadcast to all online players across all worlds
         */
        GLOBAL,

        /**
         * Broadcast only to players in the same world
         */
        WORLD,

        /**
         * No broadcast (disabled)
         */
        NONE
    }

    /**
     * Check if broadcast is enabled
     */
    public boolean isEnabled() {
        return broadcastEnabled;
    }

    /**
     * Check if listing broadcasts are enabled
     */
    public boolean isListingBroadcastEnabled() {
        return broadcastEnabled && onListing;
    }

    /**
     * Check if purchase broadcasts are enabled
     */
    public boolean isPurchaseBroadcastEnabled() {
        return broadcastEnabled && onPurchase;
    }

    /**
     * Get the current broadcast range
     */
    public BroadcastRange getRange() {
        return range;
    }
}
