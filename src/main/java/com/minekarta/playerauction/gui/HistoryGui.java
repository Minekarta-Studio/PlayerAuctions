package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.transaction.model.Transaction;
import com.minekarta.playerauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HistoryGui extends PaginatedGui {

    private final PlayerAuction kah;
    private final UUID targetPlayerId;
    private List<Transaction> transactions;

    public HistoryGui(PlayerAuction plugin, Player player, UUID targetPlayerId, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
        this.targetPlayerId = targetPlayerId;
        // Mark as async GUI - we'll call openInventory() after build completes
        setAsync(true);
    }

    @Override
    protected String getTitle() {
        return com.minekarta.playerauction.util.MessageParser.toPlainText(getTitleComponent());
    }

    protected net.kyori.adventure.text.Component getTitleComponent() {
        return kah.getConfigManager().getMessage("gui.history-title");
    }

    @Override
    protected void build() {
        // Fetch transactions and build page
        kah.getTransactionLogger().getHistory(targetPlayerId, page, itemsPerPage + 1)
                .thenAccept(fetchedTransactions -> {
                    this.hasNextPage = fetchedTransactions.size() > itemsPerPage;
                    this.transactions = hasNextPage ? fetchedTransactions.subList(0, itemsPerPage)
                            : fetchedTransactions;

                    // Run on main thread to populate items
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        // Populate transaction items
                        for (int i = 0; i < transactions.size(); i++) {
                            Transaction transaction = transactions.get(i);
                            ItemStack displayItem = createHistoryItem(transaction);
                            inventory.setItem(i, displayItem);
                        }

                        // Show empty message if no transactions
                        if (transactions.isEmpty()) {
                            ItemStack emptyItem = new GuiItemBuilder(Material.WRITABLE_BOOK)
                                    .setName("<gold>No Transaction History")
                                    .setLore(
                                            "<gray>You don't have any auction transactions yet.",
                                            "<dark_gray>Start buying or selling items to see your history!",
                                            "",
                                            "<green>Browse the auction house to get started.")
                                    .build();
                            inventory.setItem(22, emptyItem);
                        }

                        // Add control bar and custom controls
                        addControlBar();
                        addCustomControls();

                        // ✅ FIX: Open inventory AFTER build is complete
                        openInventory();
                    });
                }).exceptionally(ex -> {
                    kah.getLogger().severe("Error building HistoryGui: " + ex.getMessage());
                    ex.printStackTrace();
                    plugin.getServer().getScheduler().runTask(plugin, this::openInventory);
                    return null;
                });
    }

    private void addCustomControls() {
        // Add back button
        net.kyori.adventure.text.Component backName = kah.getConfigManager().getMessage("gui.control-items.back");
        List<String> backLore = new ArrayList<>();
        backLore.add("<gray>Return to the main auction house");
        inventory.setItem(46,
                new GuiItemBuilder(Material.SPECTRAL_ARROW)
                        .setName(com.minekarta.playerauction.util.MessageParser.parse("<green>").append(backName))
                        .setLore(backLore).build());
    }

    private ItemStack createHistoryItem(Transaction transaction) {
        ItemStack item = transaction.itemSnapshot().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();

        // Header information
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("<white>" + item.getType().toString().replace("_", " ").toLowerCase());
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");

        // Status with color coding and icon
        String statusText;
        String statusIcon;
        String statusColor;
        switch (transaction.status()) {
            case "SOLD":
                statusIcon = "✓";
                statusColor = "<green>";
                statusText = "SOLD";
                break;
            case "EXPIRED":
                statusIcon = "⏰";
                statusColor = "<yellow>";
                statusText = "EXPIRED";
                break;
            case "CANCELLED":
                statusIcon = "✗";
                statusColor = "<red>";
                statusText = "CANCELLED";
                break;
            default:
                statusIcon = "•";
                statusColor = "<gray>";
                statusText = transaction.status();
                break;
        }
        lore.add("<gray>➤ <gold>Status: " + statusColor + statusIcon + " " + statusText);

        // Date and time
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        lore.add("<gray>➤ <gold>Date: <yellow>" + dateFormat.format(new java.util.Date(transaction.timestamp())));

        // Determine user role in transaction
        boolean isSeller = player.getUniqueId().equals(transaction.sellerUuid());
        boolean isBuyer = transaction.buyerUuid() != null && player.getUniqueId().equals(transaction.buyerUuid());

        // Transaction details based on status
        if (transaction.status().equals("SOLD")) {
            if (isSeller) {
                // User was the seller
                String buyerName = transaction.buyerUuid() != null
                        ? kah.getPlayerNameCache().getName(transaction.buyerUuid()).join()
                        : "Unknown";
                lore.add("<gray>➤ <gold>Sold to: <yellow>" + buyerName);
                lore.add("<gray>➤ <gold>Earned: <green>+"
                        + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            } else if (isBuyer) {
                // User was the buyer
                String sellerName = transaction.sellerUuid() != null
                        ? kah.getPlayerNameCache().getName(transaction.sellerUuid()).join()
                        : "Unknown";
                lore.add("<gray>➤ <gold>Bought from: <yellow>" + sellerName);
                lore.add("<gray>➤ <gold>Paid: <red>-"
                        + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            } else {
                // User is just viewing history
                lore.add("<gray>➤ <gold>Price: <yellow>"
                        + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            }
        } else if (transaction.status().equals("EXPIRED")) {
            lore.add("<gray>➤ <gold>Result: <yellow>Item expired without sale");
            if (isSeller) {
                lore.add("<gray>➤ <gold>Outcome: <green>Item returned to inventory");
            }
        } else if (transaction.status().equals("CANCELLED")) {
            lore.add("<gray>➤ <gold>Result: <yellow>Auction was cancelled");
            if (isSeller) {
                lore.add("<gray>➤ <gold>Outcome: <green>Item returned to inventory");
            }
        }

        // Additional item details
        if (item.getAmount() > 1) {
            lore.add("<gray>➤ <gold>Quantity: <yellow>" + item.getAmount());
        }

        lore.add("");
        lore.add("<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("<dark_gray>Transaction ID: <gray>" + transaction.transactionId().toString().substring(0, 8) + "...");

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handleControlBarClick(event))
            return;

        int slot = event.getSlot();

        // Handle custom control clicks
        if (slot == 46) { // Back button
            new MainAuctionGui(kah, player, 1, com.minekarta.playerauction.gui.model.SortOrder.NEWEST).open();
            return;
        }

        // Handle clicking on transaction items - show more details
        if (slot >= 0 && slot < itemsPerPage && transactions != null && slot < transactions.size()) {
            Transaction clickedTransaction = transactions.get(slot);

            // Create detailed info message
            List<String> details = new ArrayList<>();
            details.add("<gold>=== Transaction Details ===");
            details.add("<gray>Transaction ID: <yellow>" + clickedTransaction.transactionId().toString());
            details.add("<gray>Status: " + clickedTransaction.status());
            details.add("<gray>Date: <yellow>" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new java.util.Date(clickedTransaction.timestamp())));
            details.add("<gray>Item: <yellow>" + clickedTransaction.itemSnapshot().toItemStack().getType().toString());

            if (clickedTransaction.finalPrice() != null) {
                details.add(
                        "<gray>Amount: <yellow>"
                                + kah.getEconomyRouter().getService().format(clickedTransaction.finalPrice()));
            }

            details.add("<gray>" + clickedTransaction.details());

            // Send details to player
            for (String line : details) {
                player.sendMessage(com.minekarta.playerauction.util.MessageParser.parse(line));
            }
            return;
        }

        // Handle other control buttons
        if (slot == 48) { // My Listings button
            new MyListingsGui(kah, player, 1).open();
        } else if (slot == 50) { // History button - refresh current page
            new HistoryGui(kah, player, targetPlayerId, page).open();
        } else if (slot == 51) { // Create Auction button
            net.kyori.adventure.text.Component createUnavailableMsg = kah.getConfigManager().getPrefixedMessage(
                    "info.create-auction-unavailable",
                    "Create auction feature is currently unavailable.");
            player.sendMessage(createUnavailableMsg);
        }
    }

    @Override
    protected void openPage(int newPage) {
        new HistoryGui(kah, player, targetPlayerId, newPage).open();
    }
}
