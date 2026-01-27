package com.minekarta.playerauction.mailbox;

import com.minekarta.playerauction.mailbox.model.MailboxItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for player mailbox items
 */
public interface MailboxStorage {

    /**
     * Initialize the storage system
     */
    void init();

    /**
     * Add an item to a player's mailbox
     *
     * @param item The mailbox item to add
     * @return CompletableFuture that completes when item is saved
     */
    CompletableFuture<Void> addItem(MailboxItem item);

    /**
     * Get all unclaimed items for a player (with pagination)
     *
     * @param playerId The player's UUID
     * @param page Page number (1-based)
     * @param limit Items per page
     * @return CompletableFuture with list of mailbox items
     */
    CompletableFuture<List<MailboxItem>> getUnclaimedItems(UUID playerId, int page, int limit);

    /**
     * Get total count of unclaimed items for a player
     *
     * @param playerId The player's UUID
     * @return CompletableFuture with count
     */
    CompletableFuture<Integer> getUnclaimedCount(UUID playerId);

    /**
     * Mark an item as claimed
     *
     * @param itemId The mailbox item ID
     * @return CompletableFuture that completes with true if successful
     */
    CompletableFuture<Boolean> claimItem(UUID itemId);

    /**
     * Get a specific mailbox item by ID
     *
     * @param itemId The mailbox item ID
     * @return CompletableFuture with Optional of the item
     */
    CompletableFuture<Optional<MailboxItem>> getItem(UUID itemId);

    /**
     * Delete expired items from all mailboxes
     * This should be called periodically by a cleanup task
     *
     * @return CompletableFuture with count of deleted items
     */
    CompletableFuture<Integer> deleteExpiredItems();

    /**
     * Delete all claimed items older than specified days
     *
     * @param days Age in days
     * @return CompletableFuture with count of deleted items
     */
    CompletableFuture<Integer> deleteOldClaimedItems(int days);
}
