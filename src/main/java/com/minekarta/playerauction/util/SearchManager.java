package com.minekarta.playerauction.util;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.gui.MainAuctionGui;
import com.minekarta.playerauction.gui.model.SortOrder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages search sessions for players in the auction house.
 * Handles chat input when players are in search mode.
 *
 * This class intercepts chat messages from players who have clicked the search button,
 * validates their input, and opens the auction GUI with search results.
 */
public class SearchManager implements Listener {

    private final PlayerAuction plugin;
    private final Map<UUID, SearchSession> activeSessions;

    public SearchManager(PlayerAuction plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();

        // Register this as an event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("SearchManager initialized successfully");
    }

    /**
     * Starts a search session for a player.
     *
     * @param player The player starting the search
     * @param currentSort The current sort order to maintain
     */
    public void startSearchSession(Player player, SortOrder currentSort) {
        UUID playerId = player.getUniqueId();

        // Create new search session
        SearchSession session = new SearchSession(player, currentSort);
        activeSessions.put(playerId, session);

        // Send instruction message using Component API
        plugin.getConfigManager().sendPrefixedMessage(player, "info.enter-search-query", null);

        plugin.getLogger().info("Search session started for player: " + player.getName());
    }

    /**
     * Checks if a player has an active search session.
     *
     * @param player The player to check
     * @return true if player is in search mode
     */
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    /**
     * Cancels a player's search session.
     *
     * @param player The player whose session to cancel
     */
    public void cancelSession(Player player) {
        UUID playerId = player.getUniqueId();
        SearchSession removed = activeSessions.remove(playerId);

        if (removed != null) {
            plugin.getConfigManager().sendPrefixedMessage(player, "info.search-cancelled", null);
            plugin.getLogger().info("Search session cancelled for player: " + player.getName());
        }
    }

    /**
     * Handles chat events to capture search queries.
     * This is called asynchronously, so GUI operations must be scheduled on the main thread.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check if player has active search session
        SearchSession session = activeSessions.get(playerId);
        if (session == null) {
            return; // Not in search mode
        }

        // Cancel the chat event to prevent message from being broadcast
        event.setCancelled(true);

        String message = event.getMessage().trim();

        // Check for cancel commands
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("exit") ||
            message.equalsIgnoreCase("quit") || message.equalsIgnoreCase("stop")) {

            // Remove session
            activeSessions.remove(playerId);

            // Reopen main GUI without search (on main thread)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                new MainAuctionGui(plugin, player, 1, session.getSortOrder(), null).open();
                plugin.getConfigManager().sendPrefixedMessage(player, "info.search-cancelled", null);
            });

            plugin.getLogger().info("Player " + player.getName() + " cancelled search");
            return;
        }

        // Validate search query - empty check
        if (message.isEmpty()) {
            plugin.getConfigManager().sendPrefixedMessage(player, "errors.empty-search-query", null);
            return;
        }

        // Validate search query - minimum length
        if (message.length() < 2) {
            plugin.getConfigManager().sendPrefixedMessage(player, "errors.search-query-too-short", null);
            return;
        }

        // Validate search query - maximum length
        if (message.length() > 50) {
            plugin.getConfigManager().sendPrefixedMessage(player, "errors.search-query-too-long", null);
            return;
        }

        // Valid search query - remove session and open search results
        final String searchQuery = message;
        activeSessions.remove(playerId);

        // Open search results GUI on main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            new MainAuctionGui(plugin, player, 1, session.getSortOrder(), searchQuery).open();

            // Send search started message with query using Component API
            PlaceholderContext context = new PlaceholderContext()
                .addPlaceholder("query", searchQuery);
            plugin.getConfigManager().sendMessage(player, "info.search-started", context);
        });

        plugin.getLogger().info("Player " + player.getName() + " searched for: " + searchQuery);
    }

    /**
     * Cleanup when player quits to prevent memory leaks.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        SearchSession removed = activeSessions.remove(playerId);

        if (removed != null) {
            plugin.getLogger().info("Cleaned up search session for disconnected player: " + event.getPlayer().getName());
        }
    }

    /**
     * Gets the number of active search sessions.
     * Useful for monitoring and debugging.
     *
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Clears all active search sessions.
     * Used during plugin disable to cleanup properly.
     */
    public void clearAllSessions() {
        int count = activeSessions.size();
        activeSessions.clear();

        if (count > 0) {
            plugin.getLogger().info("Cleared " + count + " active search session(s)");
        }
    }

    /**
     * Represents a player's search session.
     * Stores the player, their current sort order, and session start time.
     */
    private static class SearchSession {
        private final Player player;
        private final SortOrder sortOrder;
        private final long startTime;

        public SearchSession(Player player, SortOrder sortOrder) {
            this.player = player;
            this.sortOrder = sortOrder;
            this.startTime = System.currentTimeMillis();
        }

        public Player getPlayer() {
            return player;
        }

        public SortOrder getSortOrder() {
            return sortOrder;
        }

        public long getStartTime() {
            return startTime;
        }

        /**
         * Checks if this session has timed out (5 minutes).
         * Sessions that timeout should be automatically removed.
         *
         * @return true if session is older than 5 minutes
         */
        public boolean isTimedOut() {
            return (System.currentTimeMillis() - startTime) > 300000; // 5 minutes
        }
    }
}
