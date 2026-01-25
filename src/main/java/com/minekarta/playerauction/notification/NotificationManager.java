package com.minekarta.playerauction.notification;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.config.ConfigManager;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

public class NotificationManager {

    private final PlayerAuction plugin;
    private final ConfigManager configManager;
    private final com.minekarta.playerauction.players.PlayerSettingsService playerSettingsService;
    private final boolean placeholderApiEnabled;

    public NotificationManager(PlayerAuction plugin, ConfigManager configManager, com.minekarta.playerauction.players.PlayerSettingsService playerSettingsService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerSettingsService = playerSettingsService;
        this.placeholderApiEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    public void sendNotification(Player player, String messageKey, Map<String, String> placeholders) {
        if (player == null || !player.isOnline() || !playerSettingsService.getNotificationsEnabled(player)) {
            return;
        }

        List<String> methods = configManager.getConfig().getStringList("auction.notification-methods");

        // Create placeholder context and add the provided placeholders
        com.minekarta.playerauction.util.PlaceholderContext context = new com.minekarta.playerauction.util.PlaceholderContext();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            // Handle both %placeholder% and {placeholder} formats
            String key = entry.getKey();
            // Remove percent signs or curly braces to get the base key
            String cleanKey = key.replace("%", "").replace("{", "").replace("}", "");
            context.addPlaceholder(cleanKey, entry.getValue());
        }

        String message = configManager.getMessage(messageKey, context);

        // Apply PlaceholderAPI placeholders
        if (placeholderApiEnabled) {
            message = PlaceholderAPI.setPlaceholders(player, message);
        }

        for (String method : methods) {
            switch (method.toLowerCase()) {
                case "chat":
                    player.sendMessage(message);
                    break;
                case "actionbar":
                    player.sendActionBar(message);
                    break;
                case "title":
                    // Titles can be split into title and subtitle with a newline
                    String[] parts = message.split("\n", 2);
                    String title = parts[0];
                    String subtitle = parts.length > 1 ? parts[1] : "";
                    player.sendTitle(title, subtitle, 10, 70, 20); // Default fade-in, stay, fade-out times
                    break;
                case "sound":
                    // Make the sound configurable
                    String soundName = configManager.getConfig().getString("auction.notification-sound", "BLOCK_NOTE_BLOCK_PLING");
                    float volume = (float) configManager.getConfig().getDouble("auction.notification-sound-volume", 1.0);
                    float pitch = (float) configManager.getConfig().getDouble("auction.notification-sound-pitch", 1.0);
                    player.playSound(player.getLocation(), soundName, volume, pitch);
                    break;
            }
        }
    }
}
