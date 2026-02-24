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
            37, 38, 39, 40, 41, 42, 43 // Row 4
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
        return com.minekarta.playerauction.util.MessageParser.toPlainText(getTitleComponent());
    }

    protected net.kyori.adventure.text.Component getTitleComponent() {
        return kah.getConfigManager().getMessage("gui.my-listings-title");
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
                                    .setName("<#E74C3C>No Active Listings")
                                    .setLore(
                                            "<#BDC3C7>You don't have any active auction listings.",
                                            "<#7F8C8D>Create a listing to start selling items!",
                                            "",
                                            "<#2ECC71>Click 'Create Auction' to get started.")
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
        net.kyori.adventure.text.Component backName = kah.getConfigManager().getMessage("gui.control-items.back");
        List<String> backLore = new ArrayList<>();
        backLore.add("<#7F8C8D>Return to the main auction house");
        backLore.add("<#BDC3C7>Click to go back");
        inventory.setItem(46,
                new GuiItemBuilder(Material.SPECTRAL_ARROW)
                        .setName(com.minekarta.playerauction.util.MessageParser.parse("<#2ECC71>").append(backName))
                        .setLore(backLore).build());
    }

    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Get clean item display name
        net.kyori.adventure.text.Component itemDisplayNameComp = item.hasItemMeta()
                && item.getItemMeta().hasDisplayName()
                        ? item.getItemMeta().displayName()
                        : net.kyori.adventure.text.Component.text(formatItemName(item.getType().toString()));

        // Serialize component back to minimessage for placeholders so it persists
        // formats
        String itemDisplayNameStr = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .serialize(itemDisplayNameComp);

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            builder.setName(itemDisplayNameComp);
        } else {
            builder.setName(itemDisplayNameStr);
        }

        // Economy service
        var economyService = kah.getEconomyRouter().getService();

        // Create placeholder context with complete statistics
        com.minekarta.playerauction.util.PlaceholderContext context = new com.minekarta.playerauction.util.PlaceholderContext()
                .addPlaceholder("item_name", itemDisplayNameStr)
                .addPlaceholder("quantity", String.valueOf(item.getAmount()));

        // ═══════════════════════════════════════════════
        // PRICING INFORMATION
        // ═══════════════════════════════════════════════

        context.addPlaceholder("starting_price", economyService.format(auction.price()));
        context.addPlaceholder("current_bid", economyService.format(auction.price()));

        // Buy Now price with visual indicator
        if (auction.buyNowPrice() != null) {
            context.addPlaceholder("buy_now_price", economyService.format(auction.buyNowPrice()));
            context.addPlaceholder("buy_now_display", "<#2ECC71>" + economyService.format(auction.buyNowPrice()));
        } else {
            context.addPlaceholder("buy_now_price", "N/A");
            context.addPlaceholder("buy_now_display", "<#7F8C8D>—");
        }

        // Reserve price with visual indicator
        if (auction.reservePrice() != null) {
            context.addPlaceholder("reserve_price", economyService.format(auction.reservePrice()));
            context.addPlaceholder("reserve_display", "<#E67E22>" + economyService.format(auction.reservePrice()));
        } else {
            context.addPlaceholder("reserve_price", "N/A");
            context.addPlaceholder("reserve_display", "<#7F8C8D>—");
        }

        // ═══════════════════════════════════════════════
        // TIME INFORMATION
        // ═══════════════════════════════════════════════

        long timeLeft = auction.endAt() - System.currentTimeMillis();
        String timeStr = TimeUtil.formatDuration(timeLeft);
        String timeColor;
        if (timeLeft <= 0) {
            timeColor = "<#E74C3C>"; // Coral Red - expired
        } else if (timeLeft < 60 * 60 * 1000) { // Less than 1 hour
            timeColor = "<#E74C3C>"; // Coral Red - urgent
        } else if (timeLeft < 24 * 60 * 60 * 1000) { // Less than 24 hours
            timeColor = "<#E67E22>"; // Carrot - warning
        } else {
            timeColor = "<#2ECC71>"; // Emerald - plenty of time
        }
        context.addPlaceholder("time_left", timeStr);
        context.addPlaceholder("time_color", timeColor);

        // Duration and listed date
        context.addPlaceholder("duration", TimeUtil.formatDuration(auction.endAt() - auction.createdAt()));
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, HH:mm");
        context.addPlaceholder("listed_date", dateFormat.format(new java.util.Date(auction.createdAt())));

        // ═══════════════════════════════════════════════
        // BID STATISTICS
        // ═══════════════════════════════════════════════

        context.addPlaceholder("bid_count", "0"); // TODO: implement bid tracking
        context.addPlaceholder("highest_bidder", "—");

        // ═══════════════════════════════════════════════
        // STATUS INFORMATION
        // ═══════════════════════════════════════════════

        String statusText;
        String statusColor;
        switch (auction.status()) {
            case ACTIVE:
                statusColor = "<#2ECC71>"; // Emerald
                statusText = "ᴀᴄᴛɪᴠᴇ";
                break;
            case FINISHED:
                statusColor = "<#F5A623>"; // Amber Gold
                statusText = "sᴏʟᴅ";
                break;
            case CANCELLED:
                statusColor = "<#7F8C8D>"; // Slate Gray
                statusText = "ᴄᴀɴᴄᴇʟʟᴇᴅ";
                break;
            case EXPIRED:
                statusColor = "<#E74C3C>"; // Coral Red
                statusText = "ᴇxᴘɪʀᴇᴅ";
                break;
            default:
                statusColor = "<#BDC3C7>"; // Silver
                statusText = auction.status().name().toLowerCase();
                break;
        }
        context.addPlaceholder("status", statusText);
        context.addPlaceholder("status_color", statusColor);

        // ═══════════════════════════════════════════════
        // BUILD LORE
        // ═══════════════════════════════════════════════

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();

        // Get the item lore template from config
        java.util.List<String> rawLore = kah.getConfigManager().getMessages().getStringList("gui.my-listings-lore");
        for (String loreLine : rawLore) {
            net.kyori.adventure.text.Component processedLine = kah.getConfigManager()
                    .processMessageAsComponent(loreLine, context);
            lore.add(processedLine);
        }

        // Add action buttons based on status
        lore.add(net.kyori.adventure.text.Component.empty());
        if (auction.status() == AuctionStatus.ACTIVE) {
            lore.add(com.minekarta.playerauction.util.MessageParser.parse("<#E74C3C>▶ ᴄʟɪᴄᴋ ᴛᴏ ᴄᴀɴᴄᴇʟ"));
            lore.add(com.minekarta.playerauction.util.MessageParser.parse("<#7F8C8D>Item will be returned to mailbox"));
        } else {
            lore.add(com.minekarta.playerauction.util.MessageParser.parse("<#7F8C8D>○ " + statusText.toUpperCase()));
            switch (auction.status()) {
                case FINISHED:
                    lore.add(com.minekarta.playerauction.util.MessageParser.parse("<#7F8C8D>Sold to another player"));
                    break;
                case CANCELLED:
                    lore.add(com.minekarta.playerauction.util.MessageParser
                            .parse("<#7F8C8D>You cancelled this listing"));
                    break;
                case EXPIRED:
                    lore.add(com.minekarta.playerauction.util.MessageParser.parse("<#7F8C8D>Expired without sale"));
                    break;
                default:
                    lore.add(com.minekarta.playerauction.util.MessageParser.parse("<#7F8C8D>Cannot be modified"));
                    break;
            }
        }

        return builder.setLoreComponents(lore).build();
    }

    /**
     * Format item type name to be more readable.
     * Example: DIAMOND_SWORD -> Diamond Sword
     */
    private String formatItemName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "Unknown Item";
        }
        String[] parts = typeName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0)
                formatted.append(" ");
            if (parts[i].length() > 0) {
                formatted.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    formatted.append(parts[i].substring(1));
                }
            }
        }
        return formatted.toString();
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

        // ✅ FIX: Convert GUI slot to item index
        int itemIndex = getItemIndexForSlot(slot);

        // Handle clicking on an auction item
        if (itemIndex != -1 && auctions != null && itemIndex < auctions.size()) {
            Auction clickedAuction = auctions.get(itemIndex);

            if (clickedAuction.status() == AuctionStatus.ACTIVE) {
                // Show confirmation before cancelling
                player.closeInventory();
                net.kyori.adventure.text.Component confirmMessage = kah.getConfigManager().getPrefixedMessage(
                        "info.confirm-cancel",
                        "%item%", clickedAuction.item().toItemStack().getType().toString(),
                        "%price%", kah.getEconomyRouter().getService().format(clickedAuction.price()));
                player.sendMessage(confirmMessage);

                // Cancel the auction
                kah.getAuctionService().cancelAuction(player, clickedAuction.id()).thenAccept(success -> {
                    if (success) {
                        net.kyori.adventure.text.Component successMsg = kah.getConfigManager().getPrefixedMessage(
                                "auction.cancel-success",
                                "%item%", clickedAuction.item().toItemStack().getType().toString());
                        player.sendMessage(successMsg);
                        // Refresh the GUI after a short delay
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            new MyListingsGui(kah, player, page).open();
                        }, 20L); // 1 second delay
                    } else {
                        net.kyori.adventure.text.Component errMsg = kah.getConfigManager().getPrefixedMessage(
                                "errors.cancel-failed",
                                "Could not cancel listing. Please try again.");
                        player.sendMessage(errMsg);
                    }
                });
            } else {
                // Show message for non-active auctions
                net.kyori.adventure.text.Component notActiveMsg = kah.getConfigManager().getPrefixedMessage(
                        "errors.auction-not-active",
                        "Only active listings can be cancelled.");
                player.sendMessage(notActiveMsg);
            }
            return;
        }

        // Handle other control buttons
        if (slot == 48) { // My Listings button - refresh current page
            new MyListingsGui(kah, player, page).open();
        } else if (slot == 50) { // History button
            new HistoryGui(kah, player, player.getUniqueId(), 1).open();
        } else if (slot == 51) { // Create Auction button
            net.kyori.adventure.text.Component createUnavailableMsg = kah.getConfigManager().getPrefixedMessage(
                    "info.create-auction-unavailable",
                    "Create auction feature is currently unavailable.");
            player.sendMessage(createUnavailableMsg);
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MyListingsGui(kah, player, newPage).open();
    }
}
