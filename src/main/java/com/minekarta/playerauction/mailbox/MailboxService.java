package com.minekarta.playerauction.mailbox;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.common.SerializedItem;
import com.minekarta.playerauction.economy.EconomyRouter;
import com.minekarta.playerauction.mailbox.model.MailboxItem;
import com.minekarta.playerauction.mailbox.model.MailboxItemType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service class for managing mailbox operations
 */
public class MailboxService {

    private final PlayerAuction plugin;
    private final MailboxStorage storage;
    private final EconomyRouter economyRouter;
    private final long retentionDays;

    public MailboxService(PlayerAuction plugin, MailboxStorage storage, EconomyRouter economyRouter) {
        this.plugin = plugin;
        this.storage = storage;
        this.economyRouter = economyRouter;
        this.retentionDays = plugin.getConfigManager().getConfig().getLong("mailbox.retention-days", 30);
    }

    /**
     * Add an item to player's mailbox (for expired/cancelled auctions)
     */
    public CompletableFuture<Void> addReturnedItem(UUID playerId, SerializedItem item, String reason, UUID auctionId) {
        MailboxItem mailboxItem = MailboxItem.forReturnedItem(playerId, item, reason, auctionId, retentionDays);
        return storage.addItem(mailboxItem).thenRun(() -> {
            plugin.getLogger().info("Added returned item to mailbox for player " + playerId + ": " + reason);
        });
    }

    /**
     * Add money to player's mailbox (for sold auctions)
     */
    public CompletableFuture<Void> addMoney(UUID playerId, double amount, String reason, UUID auctionId) {
        MailboxItem mailboxItem = MailboxItem.forMoney(playerId, amount, reason, auctionId, retentionDays);
        return storage.addItem(mailboxItem).thenRun(() -> {
            plugin.getLogger().info("Added money to mailbox for player " + playerId + ": " + amount + " (" + reason + ")");
        });
    }

    /**
     * Get unclaimed mailbox items for a player
     */
    public CompletableFuture<List<MailboxItem>> getUnclaimedItems(UUID playerId, int page, int limit) {
        return storage.getUnclaimedItems(playerId, page, limit);
    }

    /**
     * Get count of unclaimed items
     */
    public CompletableFuture<Integer> getUnclaimedCount(UUID playerId) {
        return storage.getUnclaimedCount(playerId);
    }

    /**
     * Claim an item from mailbox
     * This will either give the player the item or deposit money to their account
     */
    public CompletableFuture<Boolean> claimItem(Player player, UUID itemId) {
        return storage.getItem(itemId).thenCompose(optItem -> {
            if (optItem.isEmpty()) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.item-not-found"));
                return CompletableFuture.completedFuture(false);
            }

            MailboxItem item = optItem.get();

            // Check if item belongs to player
            if (!item.playerId().equals(player.getUniqueId())) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.not-your-item"));
                return CompletableFuture.completedFuture(false);
            }

            // Check if already claimed
            if (item.claimed()) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.already-claimed"));
                return CompletableFuture.completedFuture(false);
            }

            // Check if expired
            if (item.isExpired()) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.item-expired"));
                return CompletableFuture.completedFuture(false);
            }

            // Handle based on type
            if (item.type() == MailboxItemType.ITEM) {
                return claimPhysicalItem(player, item);
            } else {
                return claimMoney(player, item);
            }
        });
    }

    /**
     * Claim a physical item
     */
    private CompletableFuture<Boolean> claimPhysicalItem(Player player, MailboxItem item) {
        ItemStack itemStack = item.item().toItemStack();

        // Try to add to inventory
        if (player.getInventory().firstEmpty() == -1) {
            // Inventory full
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.inventory-full"));
            return CompletableFuture.completedFuture(false);
        }

        // Mark as claimed first (to prevent duplicate claims)
        return storage.claimItem(item.id()).thenApply(success -> {
            if (success) {
                // Give item to player on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.getInventory().addItem(itemStack);
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.item-claimed",
                        "%item%", itemStack.getType().toString(),
                        "%quantity%", String.valueOf(itemStack.getAmount())));
                });
                return true;
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.claim-failed"));
                return false;
            }
        });
    }

    /**
     * Claim money
     */
    private CompletableFuture<Boolean> claimMoney(Player player, MailboxItem item) {
        // Mark as claimed first
        return storage.claimItem(item.id()).thenCompose(success -> {
            if (!success) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.claim-failed"));
                return CompletableFuture.completedFuture(false);
            }

            // Deposit money
            return economyRouter.getService().deposit(player.getUniqueId(), item.amount(), "Claimed from mailbox")
                .thenApply(v -> {
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.money-claimed",
                        "%amount%", economyRouter.getService().format(item.amount())));
                    return true;
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to deposit money to player " + player.getName() + ": " + ex.getMessage());
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.deposit-failed"));
                    return false;
                });
        });
    }

    /**
     * Claim all items in mailbox
     */
    public CompletableFuture<Integer> claimAll(Player player) {
        return storage.getUnclaimedItems(player.getUniqueId(), 1, 100).thenCompose(items -> {
            if (items.isEmpty()) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.no-items"));
                return CompletableFuture.completedFuture(0);
            }

            // Claim each item
            CompletableFuture<Integer> future = CompletableFuture.completedFuture(0);

            for (MailboxItem item : items) {
                future = future.thenCompose(count ->
                    claimItem(player, item.id()).thenApply(success -> success ? count + 1 : count)
                );
            }

            return future.thenApply(count -> {
                if (count > 0) {
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("mailbox.claimed-all",
                        "%count%", String.valueOf(count)));
                }
                return count;
            });
        });
    }

    /**
     * Cleanup expired items (should be called periodically)
     */
    public CompletableFuture<Integer> cleanupExpiredItems() {
        return storage.deleteExpiredItems();
    }

    /**
     * Cleanup old claimed items (should be called periodically)
     */
    public CompletableFuture<Integer> cleanupOldClaimedItems(int days) {
        return storage.deleteOldClaimedItems(days);
    }

    /**
     * Get the mailbox storage
     */
    public MailboxStorage getStorage() {
        return storage;
    }
}
