package com.minekarta.playerauction.mailbox.model;

import com.minekarta.playerauction.common.SerializedItem;

import java.util.UUID;

/**
 * Represents an item in a player's mailbox.
 * Items can be from:
 * - Expired auctions (items returned)
 * - Cancelled auctions (items returned)
 * - Sold auctions (money received)
 */
public record MailboxItem(
    UUID id,                    // Unique ID for this mailbox item
    UUID playerId,              // Owner of the mailbox item
    MailboxItemType type,       // Type of mailbox item (ITEM or MONEY)
    SerializedItem item,        // The item (null if type is MONEY)
    double amount,              // Money amount (0 if type is ITEM)
    String reason,              // Reason for this mailbox item (e.g., "Auction expired", "Auction sold")
    UUID relatedAuctionId,      // Related auction ID
    long createdAt,             // Timestamp when item was added to mailbox
    long expiresAt,             // Timestamp when item will be deleted if not claimed
    boolean claimed             // Whether item has been claimed
) {

    /**
     * Create a mailbox item for returned items (expired/cancelled auctions)
     */
    public static MailboxItem forReturnedItem(UUID playerId, SerializedItem item, String reason, UUID auctionId, long retentionDays) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (retentionDays * 24 * 60 * 60 * 1000L);

        return new MailboxItem(
            UUID.randomUUID(),
            playerId,
            MailboxItemType.ITEM,
            item,
            0.0,
            reason,
            auctionId,
            now,
            expiresAt,
            false
        );
    }

    /**
     * Create a mailbox item for money (sold auctions)
     */
    public static MailboxItem forMoney(UUID playerId, double amount, String reason, UUID auctionId, long retentionDays) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (retentionDays * 24 * 60 * 60 * 1000L);

        return new MailboxItem(
            UUID.randomUUID(),
            playerId,
            MailboxItemType.MONEY,
            null,
            amount,
            reason,
            auctionId,
            now,
            expiresAt,
            false
        );
    }

    /**
     * Mark this mailbox item as claimed
     */
    public MailboxItem markAsClaimed() {
        return new MailboxItem(
            id,
            playerId,
            type,
            item,
            amount,
            reason,
            relatedAuctionId,
            createdAt,
            expiresAt,
            true
        );
    }

    /**
     * Check if this mailbox item is expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * Get time remaining until expiration in milliseconds
     */
    public long getTimeRemaining() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}
