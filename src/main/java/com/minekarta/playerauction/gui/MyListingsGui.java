package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.auction.model.AuctionStatus;
import com.minekarta.playerauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MyListingsGui extends PaginatedGui {

    private final PlayerAuction kah;
    private List<Auction> auctions;

    // ✅ FIX: Available slots for auction items (same as MainAuctionGui)
    // GUI Layout: 28 slots available (slots 10-16, 19-25, 28-34, 37-43)
    private static final int[] AUCTION_SLOTS = {
        10, 11, 12, 13, 14, 15, 16, // Row 1
        19, 20, 21, 22, 23, 24, 25, // Row 2
        28, 29, 30, 31, 32, 33, 34, // Row 3
        37, 38, 39, 40, 41, 42, 43  // Row 4
    };

    private static final int ITEMS_PER_PAGE = AUCTION_SLOTS.length; // 28 items per page

    public MyListingsGui(PlayerAuction plugin, Player player, int page) {
        super(plugin, player, page, ITEMS_PER_PAGE);
        this.kah = plugin;
        // Mark as async GUI - we'll call openInventory() after build completes
        setAsync(true);
    }

    /**
     * Convert item index to actual GUI slot.
     */
    private int getSlotForItemIndex(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= AUCTION_SLOTS.length) {
            return -1;
        }
        return AUCTION_SLOTS[itemIndex];
    }

    /**
     * Convert GUI slot to item index.
     */
    private int getItemIndexForSlot(int slot) {
        for (int i = 0; i < AUCTION_SLOTS.length; i++) {
            if (AUCTION_SLOTS[i] == slot) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessages().getString("gui.my-listings-title", "&1My Listings");
    }

    @Override
    protected void build() {
        // Fetch player's auctions and build page content
        kah.getAuctionService().getPlayerAuctions(player.getUniqueId(), page, itemsPerPage + 1)
            .thenAccept(fetchedAuctions -> {
                // Determine pagination
                this.hasNextPage = fetchedAuctions.size() > itemsPerPage;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, itemsPerPage) : fetchedAuctions;

                // Populate auction items on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    for (int i = 0; i < auctions.size(); i++) {
                        Auction auction = auctions.get(i);
                        ItemStack displayItem = createAuctionItem(auction);
                        int guiSlot = getSlotForItemIndex(i);
                        if (guiSlot != -1) {
                            inventory.setItem(guiSlot, displayItem);
                        }
                    }

                    // Show empty message if no auctions
                    if (auctions.isEmpty()) {
                        ItemStack emptyItem = new GuiItemBuilder(Material.BARRIER)
                            .setName("§cNo Active Listings")
                            .setLore(
                                "§7You don't have any active auction listings.",
                                "§8Create a listing to start selling items!",
                                "",
                                "§aClick 'Create Auction' to get started."
                            )
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
                kah.getLogger().severe("Error building MyListingsGui: " + ex.getMessage());
                ex.printStackTrace();
                plugin.getServer().getScheduler().runTask(plugin, this::openInventory);
                return null;
            });
    }

    private void addCustomControls() {
        // Add back button
        String backName = kah.getConfigManager().getMessage("gui.control-items.back");
        List<String> backLore = new ArrayList<>();
        backLore.add("&7Return to the main auction house");
        backLore.add("&8Click to go back");
        inventory.setItem(46, new GuiItemBuilder(Material.SPECTRAL_ARROW).setName("&a" + backName).setLore(backLore).build());
    }

    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Create placeholder context for auction-specific information
        com.minekarta.playerauction.util.PlaceholderContext context = new com.minekarta.playerauction.util.PlaceholderContext()
            .addPlaceholder("item_name", item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? item.getItemMeta().getDisplayName()
                : item.getType().toString().replace("_", " ").toLowerCase())
            .addPlaceholder("starting_price", kah.getEconomyRouter().getService().format(auction.price()))
            .addPlaceholder("current_bid", kah.getEconomyRouter().getService().format(auction.price())) // Using price as current bid for now
            .addPlaceholder("buy_now_price", auction.buyNowPrice() != null ? kah.getEconomyRouter().getService().format(auction.buyNowPrice()) : "N/A")
            .addPlaceholder("reserve_price", auction.reservePrice() != null ? kah.getEconomyRouter().getService().format(auction.reservePrice()) : "N/A");

        // Time left for active auctions
        long timeLeft = auction.endAt() - System.currentTimeMillis();
        String timeStr = TimeUtil.formatDuration(timeLeft);
        String timeColor;
        if (timeLeft > 24 * 60 * 60 * 1000) { // More than 1 day
            timeColor = "&a";
        } else if (timeLeft > 60 * 60 * 1000) { // More than 1 hour
            timeColor = "&e";
        } else { // Less than 1 hour
            timeColor = "&c";
        }
        context.addPlaceholder("time_left", timeStr);

        // Status information with modern hex colors
        String statusText;
        String statusColor;
        switch (auction.status()) {
            case ACTIVE:
                statusColor = "&#2ECC71"; // Emerald
                statusText = "ACTIVE";
                break;
            case FINISHED:
                statusColor = "&#F5A623"; // Amber Gold
                statusText = "SOLD";
                break;
            case CANCELLED:
                statusColor = "&#7F8C8D"; // Slate Gray
                statusText = "CANCELLED";
                break;
            case EXPIRED:
                statusColor = "&#E74C3C"; // Coral Red
                statusText = "EXPIRED";
                break;
            default:
                statusColor = "&#BDC3C7"; // Silver
                statusText = auction.status().name();
                break;
        }
        context.addPlaceholder("status", statusText);
        context.addPlaceholder("status_color", statusColor);

        // Additional item details
        context.addPlaceholder("duration", TimeUtil.formatDuration(auction.endAt() - auction.createdAt()));
        context.addPlaceholder("bidder_count", "0"); // Assuming no bidders for simplicity

        // Create lore using the message system with placeholders
        List<String> lore = new ArrayList<>();

        // Add header information
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("&f" + item.getType().toString().replace("_", " ").toLowerCase());
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");

        // Get the item lore template from config and apply placeholders
        java.util.List<String> rawLore = kah.getConfigManager().getMessages().getStringList("gui.my-listings-lore");
        for (String loreLine : rawLore) {
            // Apply the context to each lore line
            String processedLine = context.applyTo(loreLine);
            // Apply time color specifically to the time line
            if (processedLine.contains("{time_left}")) {
                processedLine = processedLine.replace("{time_left}", timeColor + timeStr);
            }
            lore.add(processedLine);
        }

        // Additional item details
        if (item.getAmount() > 1) {
            lore.add("&7➤ &6Quantity: &e" + item.getAmount());
        }

        lore.add("");
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Action buttons based on status
        if (auction.status() == AuctionStatus.ACTIVE) {
            lore.add("&c&l▶ CLICK TO CANCEL LISTING");
            lore.add("&7Remove this item from the auction house");
            lore.add("&8Item will be returned to your inventory");
        } else {
            lore.add("&7&l▶ LISTING " + statusText);
            switch (auction.status()) {
                case FINISHED:
                    lore.add("&7Item was sold to another player");
                    break;
                case CANCELLED:
                    lore.add("&7You cancelled this listing");
                    break;
                case EXPIRED:
                    lore.add("&7Listing expired without sale");
                    break;
                default:
                    lore.add("&7This listing cannot be modified");
                    break;
            }
        }

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handleControlBarClick(event)) return;

        int slot = event.getSlot();

        // Handle custom control clicks
        if (slot == 46) { // Back button
            new MainAuctionGui(kah, player, 1, com.minekarta.playerauction.gui.model.SortOrder.NEWEST, null).open();
            return;
        }

        // ✅ FIX: Convert GUI slot to item index
        int itemIndex = getItemIndexForSlot(slot);

        // Handle clicking on an auction item
        if (itemIndex != -1 && auctions != null && itemIndex < auctions.size()) {
            Auction clickedAuction = auctions.get(itemIndex);

            if (clickedAuction.status() == AuctionStatus.ACTIVE) {
                // Show confirmation before cancelling
                player.closeInventory();
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.confirm-cancel",
                    "%item%", clickedAuction.item().toItemStack().getType().toString(),
                    "%price%", kah.getEconomyRouter().getService().format(clickedAuction.price())));

                // Cancel the auction
                kah.getAuctionService().cancelAuction(player, clickedAuction.id()).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(kah.getConfigManager().getPrefixedMessage("auction.cancel-success",
                            "%item%", clickedAuction.item().toItemStack().getType().toString()));
                        // Refresh the GUI after a short delay
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            new MyListingsGui(kah, player, page).open();
                        }, 20L); // 1 second delay
                    } else {
                        player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.cancel-failed",
                            "Could not cancel listing. Please try again."));
                    }
                });
            } else {
                // Show message for non-active auctions
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.auction-not-active",
                    "Only active listings can be cancelled."));
            }
            return;
        }

        // Handle other control buttons
        if (slot == 48) { // My Listings button - refresh current page
            new MyListingsGui(kah, player, page).open();
        } else if (slot == 50) { // History button
            new HistoryGui(kah, player, player.getUniqueId(), 1).open();
        } else if (slot == 51) { // Create Auction button
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.create-auction-unavailable",
                "Create auction feature is currently unavailable."));
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MyListingsGui(kah, player, newPage).open();
    }
}
