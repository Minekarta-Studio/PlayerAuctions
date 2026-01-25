package com.minekarta.playerauction.config;

import com.minekarta.playerauction.util.MessageParser;
import com.minekarta.playerauction.util.PlaceholderContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        config = loadConfig("config.yml");
        messages = loadConfig("messages.yml");
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String getMessage(String path, String... replacements) {
        String message = messages.getString(path, "&cMissing message: " + path);

        // Apply manual replacements first (BEFORE parsing to avoid format conflicts)
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = replacements[i];
                String value = replacements[i + 1];
                if (placeholder != null && value != null) {
                    message = message.replace(placeholder, value);
                    // Also handle the opposite format for compatibility
                    // If placeholder is {item}, also replace %item%
                    // If placeholder is %item%, also replace {item}
                    if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
                        String percentFormat = "%" + placeholder.substring(1, placeholder.length() - 1) + "%";
                        message = message.replace(percentFormat, value);
                    } else if (placeholder.startsWith("%") && placeholder.endsWith("%")) {
                        String braceFormat = "{" + placeholder.substring(1, placeholder.length() - 1) + "}";
                        message = message.replace(braceFormat, value);
                    }
                }
            }
        }

        // Use MessageParser for comprehensive format support (MiniMessage, Hex, RGB, Legacy)
        return MessageParser.parseToLegacy(message);
    }

    /**
     * Gets a message with both manual replacements and dynamic placeholder context.
     *
     * @param path The message path in messages.yml
     * @param context The placeholder context containing dynamic replacements
     * @param replacements Manual replacements in the format {placeholder, value, placeholder, value, ...}
     * @return The processed message with all placeholders replaced
     */
    public String getMessage(String path, PlaceholderContext context, String... replacements) {
        String message = messages.getString(path, "&cMissing message: " + path);

        // Apply manual replacements first
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = replacements[i];
                String value = replacements[i + 1];
                if (placeholder != null && value != null) {
                    message = message.replace(placeholder, value);
                    // Also handle the opposite format for compatibility
                    // If placeholder is {item}, also replace %item%
                    // If placeholder is %item%, also replace {item}
                    if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
                        String percentFormat = "%" + placeholder.substring(1, placeholder.length() - 1) + "%";
                        message = message.replace(percentFormat, value);
                    } else if (placeholder.startsWith("%") && placeholder.endsWith("%")) {
                        String braceFormat = "{" + placeholder.substring(1, placeholder.length() - 1) + "}";
                        message = message.replace(braceFormat, value);
                    }
                }
            }
        }

        // Apply dynamic context replacements
        if (context != null) {
            message = context.applyTo(message);
        }

        // Use MessageParser for comprehensive format support
        return MessageParser.parseToLegacy(message);
    }

    /**
     * Process a raw message string (not a path) with placeholder context.
     * This is used when the message content is already retrieved from config.
     *
     * Use this when you have the actual message content from a list or direct string,
     * not when you have a path like "gui.item-lore".
     *
     * @param messageContent The actual message content (not a path)
     * @param context The placeholder context containing dynamic replacements
     * @return The processed message with all placeholders replaced
     */
    public String processMessage(String messageContent, PlaceholderContext context) {
        if (messageContent == null || messageContent.isEmpty()) {
            return "";
        }

        String message = messageContent;

        // Apply dynamic context replacements
        if (context != null) {
            message = context.applyTo(message);
        }

        // Use MessageParser for comprehensive format support
        return MessageParser.parseToLegacy(message);
    }

    /**
     * Process a raw message string and return as Component (preserves MiniMessage formatting).
     * Use this for GUI lore and other displays that support Component.
     *
     * @param messageContent The actual message content (not a path)
     * @param context The placeholder context containing dynamic replacements
     * @return The processed message as Component with MiniMessage formatting preserved
     */
    public Component processMessageAsComponent(String messageContent, PlaceholderContext context) {
        if (messageContent == null || messageContent.isEmpty()) {
            return Component.empty();
        }

        String message = messageContent;

        // Apply dynamic context replacements
        if (context != null) {
            message = context.applyTo(message);
        }

        // Parse to Component (preserves MiniMessage gradients and formatting)
        return MessageParser.parse(message);
    }

    /**
     * Send a message to a player using Adventure Component API (preserves MiniMessage formatting).
     * This is the CORRECT way to send MiniMessage-formatted messages to players.
     *
     * @param player The player to send the message to
     * @param path The message path in messages.yml
     * @param context The placeholder context
     */
    public void sendMessage(org.bukkit.entity.Player player, String path, PlaceholderContext context) {
        String rawMessage = messages.getString(path, "<red>Missing message: " + path + "</red>");

        // Apply placeholders
        if (context != null) {
            rawMessage = context.applyTo(rawMessage);
        }

        // Parse to Component (preserves MiniMessage formatting)
        Component component = MessageParser.parse(rawMessage);

        // Send using Paper's Adventure API
        player.sendMessage(component);
    }

    /**
     * Send a prefixed message to a player using Adventure Component API.
     *
     * @param player The player to send the message to
     * @param path The message path in messages.yml
     * @param context The placeholder context
     */
    public void sendPrefixedMessage(org.bukkit.entity.Player player, String path, PlaceholderContext context) {
        String prefix = messages.getString("prefix", "<gradient:gold:yellow><bold>[PlayerAuctions]</bold></gradient> <gray>»</gray> ");
        String rawMessage = messages.getString(path, "<red>Missing message: " + path + "</red>");

        // Apply placeholders
        if (context != null) {
            rawMessage = context.applyTo(rawMessage);
        }

        // Parse both to Components
        Component prefixComponent = MessageParser.parse(prefix);
        Component messageComponent = MessageParser.parse(rawMessage);

        // Combine and send
        Component combined = prefixComponent.append(messageComponent);
        player.sendMessage(combined);
    }

    public String getPrefixedMessage(String path, String... replacements) {
        String prefix = messages.getString("prefix", "&7[&6KAH&7] ");
        String message = getMessage(path, replacements);

        // Process prefix with MessageParser
        String processedPrefix = MessageParser.parseToLegacy(prefix);

        return processedPrefix + message;
    }

    /**
     * Gets a prefixed message with both manual replacements and dynamic placeholder context.
     *
     * @param path The message path in messages.yml
     * @param context The placeholder context containing dynamic replacements
     * @param replacements Manual replacements in the format {placeholder, value, placeholder, value, ...}
     * @return The processed message with prefix and all placeholders replaced
     */
    public String getPrefixedMessage(String path, PlaceholderContext context, String... replacements) {
        String prefix = messages.getString("prefix", "&7[&6KAH&7] ");

        // Process prefix with MessageParser
        String processedPrefix = MessageParser.parseToLegacy(prefix);

        String message = getMessage(path, context, replacements);
        return processedPrefix + message;
    }

    /**
     * Process color codes in a string using MessageParser (supports all formats).
     *
     * @param text Text to process
     * @return Processed text with legacy color codes
     */
    public String processColors(String text) {
        return MessageParser.parseToLegacy(text);
    }

    /**
     * Get Component directly for Adventure API usage.
     *
     * @param path Message path
     * @param context Placeholder context
     * @return Parsed Component
     */
    public Component getComponent(String path, PlaceholderContext context) {
        String message = messages.getString(path, "&cMissing message: " + path);

        if (context != null) {
            message = context.applyTo(message);
        }

        return MessageParser.parse(message);
    }

    /**
     * Get Component without placeholders.
     *
     * @param path Message path
     * @return Parsed Component
     */
    public Component getComponent(String path) {
        return getComponent(path, null);
    }
}
