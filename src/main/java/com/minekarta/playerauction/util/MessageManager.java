package com.minekarta.playerauction.util;

import com.minekarta.playerauction.PlayerAuction;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Central message manager untuk PlayerAuctions dengan full MiniMessage support.
 * Handles semua message sending dengan Adventure API dan BukkitAudiences.
 */
public class MessageManager {

    private final PlayerAuction plugin;
    private BukkitAudiences audiences;
    private final Map<String, Component> messageCache = new HashMap<>();

    public MessageManager(PlayerAuction plugin) {
        this.plugin = plugin;
        initializeAudiences();
    }

    /**
     * Initialize BukkitAudiences untuk Adventure API.
     */
    private void initializeAudiences() {
        try {
            this.audiences = BukkitAudiences.create(plugin);
            plugin.getLogger().info("MessageManager initialized with Adventure API support");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to initialize BukkitAudiences: " + e.getMessage());
            plugin.getLogger().warning("Messages will use legacy format");
        }
    }

    /**
     * Reload message cache (dipanggil saat config reload).
     */
    public void reload() {
        messageCache.clear();
        plugin.getLogger().info("Message cache cleared");
    }

    /**
     * Get parsed Component dari config path dengan placeholder support.
     *
     * @param path Config path di messages.yml
     * @param placeholders Placeholder context untuk replacement
     * @return Parsed Component
     */
    public Component getComponent(String path, PlaceholderContext placeholders) {
        // Get raw message dari config
        String rawMessage = plugin.getConfigManager().getMessages().getString(path, "&cMissing message: " + path);

        // Apply placeholders jika ada
        if (placeholders != null) {
            rawMessage = placeholders.applyTo(rawMessage);
        }

        // Parse dengan MessageParser
        return MessageParser.parse(rawMessage);
    }

    /**
     * Get parsed Component tanpa placeholders.
     *
     * @param path Config path di messages.yml
     * @return Parsed Component
     */
    public Component getComponent(String path) {
        return getComponent(path, null);
    }

    /**
     * Get legacy formatted message string (untuk backward compatibility).
     *
     * @param path Config path di messages.yml
     * @param placeholders Placeholder context
     * @return Legacy formatted string
     */
    public String getLegacyMessage(String path, PlaceholderContext placeholders) {
        Component component = getComponent(path, placeholders);
        return MessageParser.toPlainText(component);
    }

    /**
     * Get legacy formatted message string tanpa placeholders.
     *
     * @param path Config path
     * @return Legacy formatted string
     */
    public String getLegacyMessage(String path) {
        return getLegacyMessage(path, null);
    }

    /**
     * Send message ke player dengan MiniMessage support.
     *
     * @param player Target player
     * @param path Config path
     * @param placeholders Placeholder context
     */
    public void sendMessage(Player player, String path, PlaceholderContext placeholders) {
        if (player == null) return;

        Component message = getComponent(path, placeholders);

        if (audiences != null) {
            audiences.player(player).sendMessage(message);
        } else {
            // Fallback ke legacy
            player.sendMessage(MessageParser.toPlainText(message));
        }
    }

    /**
     * Send message ke player tanpa placeholders.
     *
     * @param player Target player
     * @param path Config path
     */
    public void sendMessage(Player player, String path) {
        sendMessage(player, path, null);
    }

    /**
     * Send message dengan prefix ke player.
     *
     * @param player Target player
     * @param path Config path
     * @param placeholders Placeholder context
     */
    public void sendPrefixedMessage(Player player, String path, PlaceholderContext placeholders) {
        if (player == null) return;

        Component prefix = getComponent("prefix");
        Component message = getComponent(path, placeholders);
        Component combined = prefix.append(message);

        if (audiences != null) {
            audiences.player(player).sendMessage(combined);
        } else {
            player.sendMessage(MessageParser.toPlainText(combined));
        }
    }

    /**
     * Send prefixed message tanpa placeholders.
     *
     * @param player Target player
     * @param path Config path
     */
    public void sendPrefixedMessage(Player player, String path) {
        sendPrefixedMessage(player, path, null);
    }

    /**
     * Send message ke CommandSender (bisa player atau console).
     *
     * @param sender Command sender
     * @param path Config path
     * @param placeholders Placeholder context
     */
    public void sendMessage(CommandSender sender, String path, PlaceholderContext placeholders) {
        if (sender == null) return;

        Component message = getComponent(path, placeholders);

        if (audiences != null) {
            audiences.sender(sender).sendMessage(message);
        } else {
            sender.sendMessage(MessageParser.toPlainText(message));
        }
    }

    /**
     * Send message ke CommandSender tanpa placeholders.
     *
     * @param sender Command sender
     * @param path Config path
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, path, null);
    }

    /**
     * Send action bar message ke player.
     *
     * @param player Target player
     * @param path Config path
     * @param placeholders Placeholder context
     */
    public void sendActionBar(Player player, String path, PlaceholderContext placeholders) {
        if (player == null) return;

        Component message = getComponent(path, placeholders);

        if (audiences != null) {
            audiences.player(player).sendActionBar(message);
        } else {
            // Fallback: action bar tidak tersedia di legacy
            player.sendMessage(MessageParser.toPlainText(message));
        }
    }

    /**
     * Send action bar tanpa placeholders.
     *
     * @param player Target player
     * @param path Config path
     */
    public void sendActionBar(Player player, String path) {
        sendActionBar(player, path, null);
    }

    /**
     * Send title dan subtitle ke player.
     *
     * @param player Target player
     * @param titlePath Config path untuk title
     * @param subtitlePath Config path untuk subtitle
     * @param fadeIn Fade in time dalam ticks
     * @param stay Stay time dalam ticks
     * @param fadeOut Fade out time dalam ticks
     * @param placeholders Placeholder context
     */
    public void sendTitle(Player player, String titlePath, String subtitlePath,
                         int fadeIn, int stay, int fadeOut, PlaceholderContext placeholders) {
        if (player == null) return;

        Component titleComponent = getComponent(titlePath, placeholders);
        Component subtitleComponent = getComponent(subtitlePath, placeholders);

        if (audiences != null) {
            Title title = Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(
                    Duration.ofMillis(fadeIn * 50L),
                    Duration.ofMillis(stay * 50L),
                    Duration.ofMillis(fadeOut * 50L)
                )
            );

            audiences.player(player).showTitle(title);
        } else {
            // Fallback ke Bukkit title API
            player.sendTitle(
                MessageParser.toPlainText(titleComponent),
                MessageParser.toPlainText(subtitleComponent),
                fadeIn,
                stay,
                fadeOut
            );
        }
    }

    /**
     * Send title dengan default timing.
     *
     * @param player Target player
     * @param titlePath Title path
     * @param subtitlePath Subtitle path
     * @param placeholders Placeholders
     */
    public void sendTitle(Player player, String titlePath, String subtitlePath, PlaceholderContext placeholders) {
        sendTitle(player, titlePath, subtitlePath, 10, 70, 20, placeholders);
    }

    /**
     * Broadcast message ke semua online players.
     *
     * @param path Config path
     * @param placeholders Placeholder context
     */
    public void broadcast(String path, PlaceholderContext placeholders) {
        Component message = getComponent(path, placeholders);

        if (audiences != null) {
            audiences.players().sendMessage(message);
        } else {
            String legacyMessage = MessageParser.toPlainText(message);
            plugin.getServer().broadcastMessage(legacyMessage);
        }
    }

    /**
     * Broadcast message tanpa placeholders.
     *
     * @param path Config path
     */
    public void broadcast(String path) {
        broadcast(path, null);
    }

    /**
     * Parse raw message string dengan MessageParser.
     *
     * @param rawMessage Raw message
     * @return Parsed Component
     */
    public Component parseRaw(String rawMessage) {
        return MessageParser.parse(rawMessage);
    }

    /**
     * Parse raw message dan apply placeholders.
     *
     * @param rawMessage Raw message
     * @param placeholders Placeholder context
     * @return Parsed Component
     */
    public Component parseRaw(String rawMessage, PlaceholderContext placeholders) {
        if (placeholders != null) {
            rawMessage = placeholders.applyTo(rawMessage);
        }
        return MessageParser.parse(rawMessage);
    }

    /**
     * Cleanup audiences saat plugin disable.
     */
    public void shutdown() {
        if (audiences != null) {
            audiences.close();
            plugin.getLogger().info("MessageManager shut down successfully");
        }
        messageCache.clear();
    }

    /**
     * Check apakah Adventure API available.
     *
     * @return true jika Adventure API tersedia
     */
    public boolean isAdventureAvailable() {
        return audiences != null;
    }
}

