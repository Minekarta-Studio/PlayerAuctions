package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.gui.model.AuctionCategory;
import com.minekarta.playerauction.gui.model.SortOrder;
import com.minekarta.playerauction.util.PlaceholderContext;
import com.minekarta.playerauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainAuctionGui extends PaginatedGui {

    private final PlayerAuction kah;
    private List<Auction> auctions;
    private final SortOrder sortOrder;
    private final String searchQuery;

    // ✅ FIX: Available slots for auction items (avoiding borders and controls)
    // GUI Layout (54 slots = 6 rows × 9 columns):
    // Row 0 (slots 0-8): Top border
    // Row 1 (slots 9-17): Left border | ITEMS 10-16 | Right border
    // Row 2 (slots 18-26): Left border | ITEMS 19-25 | Right border
    // Row 3 (slots 27-35): Left border | ITEMS 28-34 | Right border
    // Row 4 (slots 36-44): Left border | ITEMS 37-43 | Right border
    // Row 5 (slots 45-53): Bottom border + controls
    // Total: 28 slots available for auction items (7 per row × 4 rows)
    private static final int[] AUCTION_SLOTS = {
        10, 11, 12, 13, 14, 15, 16, // Row 1
        19, 20, 21, 22, 23, 24, 25, // Row 2
        28, 29, 30, 31, 32, 33, 34, // Row 3
        37, 38, 39, 40, 41, 42, 43  // Row 4
    };

    private static final int ITEMS_PER_PAGE = AUCTION_SLOTS.length; // 28 items per page

    public MainAuctionGui(PlayerAuction plugin, Player player, int page, SortOrder sortOrder, String searchQuery) {
        super(plugin, player, page, ITEMS_PER_PAGE);
        this.kah = plugin;
        this.sortOrder = sortOrder;
        this.searchQuery = searchQuery;
        // Mark as async GUI - we'll call openInventory() after build completes
        setAsync(true);
    }

    /**
     * Convert item index (0-27) to actual GUI slot (10-43, skipping borders).
     *
     * @param itemIndex Item index in the auction list (0-27)
     * @return GUI slot number, or -1 if invalid index
     */
    private int getSlotForItemIndex(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= AUCTION_SLOTS.length) {
            return -1;
        }
        return AUCTION_SLOTS[itemIndex];
    }

    /**
     * Convert GUI slot to item index in the auction list.
     *
     * @param slot GUI slot number
     * @return Item index (0-27) or -1 if not an auction slot
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
        return kah.getConfigManager().getConfig().getString("gui.title-main", "&6PlayerAuctions").replace("&", "§");
    }

    @Override
    protected void build() {
        // Get auctions for the current page and player balance
        CompletableFuture<List<Auction>> pageAuctionsFuture = kah.getAuctionService().getActiveAuctions(page, itemsPerPage, AuctionCategory.ALL, sortOrder, searchQuery);
        CompletableFuture<Double> balanceFuture = kah.getEconomyRouter().getService().getBalance(player.getUniqueId());
        CompletableFuture<Integer> totalCountFuture = kah.getAuctionService().getTotalActiveAuctionCount();

        // Combine all futures
        pageAuctionsFuture.thenCombine(balanceFuture, (fetchedAuctions, balance) -> {
            // Determine pagination for current page
            this.hasNextPage = fetchedAuctions.size() > itemsPerPage;
            this.auctions = hasNextPage ? fetchedAuctions.subList(0, itemsPerPage) : fetchedAuctions;

            // Populate auction items on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                for (int i = 0; i < this.auctions.size(); i++) {
                    Auction auction = this.auctions.get(i);
                    ItemStack displayItem = createAuctionItem(auction, balance);
                    inventory.setItem(getSlotForItemIndex(i), displayItem);
                }
            });

            return balance;
        }).thenCombine(totalCountFuture, (balance, totalCount) -> {
            int totalPages = (int) Math.ceil((double) totalCount / itemsPerPage);
            if (totalPages == 0) totalPages = 1;
            return new Object[]{totalPages, balance};
        }).thenAccept(data -> {
            int totalPages = (Integer) data[0];

            // Build static parts and open inventory on main thread
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                try {
                    addControlBar();
                    addCustomControls();
                    updatePlayerInfoItem(totalPages);

                    // ✅ FIX: Open inventory AFTER build is complete
                    openInventory();
                } catch (Exception e) {
                    kah.getLogger().severe("Error building MainAuctionGui: " + e.getMessage());
                    e.printStackTrace();
                    // Still try to open inventory
                    openInventory();
                }
            });
        }).exceptionally(ex -> {
            kah.getLogger().severe("Error in MainAuctionGui build: " + ex.getMessage());
            ex.printStackTrace();
            // Open inventory even on error
            plugin.getServer().getScheduler().runTask(plugin, this::openInventory);
            return null;
        });
    }

    @Override
    protected void updatePlayerInfoItem(int totalPages) {
        // Call the parent method to update the player info item
        super.updatePlayerInfoItem(totalPages);
    }

    private void addCustomControls() {
        // ═══════════════════════════════════════════════════════════════════════
        // GUI Control Bar Layout (Bottom Row - Slots 45-53):
        // [45] Border | [46] Prev | [47] Sort | [48] Search | [49] Profile
        // [50] MyListings | [51] Mailbox | [52] Next | [53] Border
        // ═══════════════════════════════════════════════════════════════════════

        // Sort Button (Slot 47)
        PlaceholderContext sortContext = new PlaceholderContext()
            .addPlaceholder("sort_order", sortOrder.getDisplayName());
        String sortName = kah.getConfigManager().getMessage("gui.control-items.sort", sortContext);
        List<String> sortLore = new ArrayList<>();
        sortLore.add("");
        sortLore.add("&#BDC3C7Current: &#ECF0F1" + sortOrder.getDisplayName());
        sortLore.add("");
        for (com.minekarta.playerauction.gui.model.SortOrder order : com.minekarta.playerauction.gui.model.SortOrder.values()) {
            if (order == sortOrder) {
                sortLore.add("&#2ECC71► " + order.getDisplayName());
            } else {
                sortLore.add("&#7F8C8D  " + order.getDisplayName());
            }
        }
        sortLore.add("");
        sortLore.add("&#F5A623Click to change");
        inventory.setItem(47, new GuiItemBuilder(Material.COMPARATOR).setName(sortName).setLore(sortLore).build());

        // Search Button (Slot 48)
        PlaceholderContext searchContext = new PlaceholderContext()
            .addPlaceholder("query", searchQuery != null ? searchQuery : "None");
        String searchName = kah.getConfigManager().getMessage("gui.control-items.search", searchContext);
        List<String> searchLore = new ArrayList<>();
        searchLore.add("");
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchLore.add("&#BDC3C7Searching for:");
            searchLore.add("&#F1C40F\"" + searchQuery + "\"");
            searchLore.add("");
            searchLore.add("&#E67E22Click to modify");
            searchLore.add("&#7F8C8DRight-click to clear");
        } else {
            searchLore.add("&#BDC3C7Find specific items");
            searchLore.add("");
            searchLore.add("&#3498DBClick to search");
        }
        inventory.setItem(48, new GuiItemBuilder(Material.COMPASS).setName(searchName).setLore(searchLore).build());

        // My Listings Button (Slot 50)
        String myListingsName = kah.getConfigManager().getMessage("gui.control-items.my-listings", null);
        kah.getAuctionService().getPlayerActiveAuctionCount(player.getUniqueId()).thenAccept(count -> {
            int maxListings = kah.getConfigManager().getConfig().getInt("auction.max-auctions-per-player", 5);
            List<String> myListingsLore = new ArrayList<>();
            myListingsLore.add("");
            myListingsLore.add("&#BDC3C7View your active auctions");
            myListingsLore.add("");
            myListingsLore.add("&#7F8C8DActive: &#ECF0F1" + count + "&#7F8C8D/&#ECF0F1" + maxListings);
            myListingsLore.add("");
            myListingsLore.add("&#E67E22Click to view");
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                inventory.setItem(50, new GuiItemBuilder(Material.BOOK).setName(myListingsName).setLore(myListingsLore).build());
            });
        });

        // Mailbox Button (Slot 51)
        String mailboxName = kah.getConfigManager().getMessage("gui.control-items.mailbox", null);
        List<String> mailboxLore = new ArrayList<>();
        mailboxLore.add("");
        mailboxLore.add("&#BDC3C7Check pending items & money");
        mailboxLore.add("");
        mailboxLore.add("&#2ECC71Click to open");
        inventory.setItem(51, new GuiItemBuilder(Material.ENDER_CHEST).setName(mailboxName).setLore(mailboxLore).build());
    }

    private ItemStack createAuctionItem(Auction auction, double playerBalance) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Get clean item display name
        String itemDisplayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
            ? item.getItemMeta().getDisplayName()
            : formatItemName(item.getType().toString());

        // Get seller name
        String sellerName = kah.getPlayerNameCache().getName(auction.seller()).join();

        // Check if this is player's own auction
        boolean isOwnAuction = player.getUniqueId().equals(auction.seller());

        // Economy service for formatting
        var economyService = kah.getEconomyRouter().getService();

        // Create placeholder context for auction-specific information
        PlaceholderContext context = new PlaceholderContext()
            .addPlaceholder("seller", sellerName)
            .addPlaceholder("item_name", itemDisplayName)
            .addPlaceholder("quantity", String.valueOf(item.getAmount()));

        // ═══════════════════════════════════════════════
        // PRICING INFORMATION
        // ═══════════════════════════════════════════════

        // Starting price (initial bid)
        context.addPlaceholder("starting_price", economyService.format(auction.price()));

        // Current bid (for now same as starting price, can be enhanced with bid tracking)
        // TODO: When bid system is fully implemented, this should show actual current highest bid
        context.addPlaceholder("current_bid", economyService.format(auction.price()));

        // Buy Now price with visual indicator
        if (auction.buyNowPrice() != null) {
            context.addPlaceholder("buy_now_price", economyService.format(auction.buyNowPrice()));
            context.addPlaceholder("buy_now_display", "&#2ECC71" + economyService.format(auction.buyNowPrice()));
        } else {
            context.addPlaceholder("buy_now_price", "N/A");
            context.addPlaceholder("buy_now_display", "&#7F8C8D—");
        }

        // Reserve price with visual indicator
        if (auction.reservePrice() != null) {
            context.addPlaceholder("reserve_price", economyService.format(auction.reservePrice()));
            context.addPlaceholder("reserve_display", "&#E67E22" + economyService.format(auction.reservePrice()));
        } else {
            context.addPlaceholder("reserve_price", "N/A");
            context.addPlaceholder("reserve_display", "&#7F8C8D—");
        }

        // ═══════════════════════════════════════════════
        // BID STATISTICS
        // ═══════════════════════════════════════════════

        // TODO: These can be enhanced when bid tracking is fully implemented
        context.addPlaceholder("bid_count", "0"); // Placeholder for bid count
        context.addPlaceholder("highest_bidder", "—"); // Placeholder for highest bidder name

        // ═══════════════════════════════════════════════
        // TIME INFORMATION
        // ═══════════════════════════════════════════════

        // Time left with color coding
        long timeLeft = auction.endAt() - System.currentTimeMillis();
        String timeStr = TimeUtil.formatDuration(timeLeft);
        String timeColor;
        if (timeLeft <= 0) {
            timeColor = "&#E74C3C"; // Coral Red - expired
        } else if (timeLeft < 60 * 60 * 1000) { // Less than 1 hour
            timeColor = "&#E74C3C"; // Coral Red - urgent
        } else if (timeLeft < 24 * 60 * 60 * 1000) { // Less than 24 hours
            timeColor = "&#E67E22"; // Carrot - warning
        } else {
            timeColor = "&#2ECC71"; // Emerald - plenty of time
        }
        context.addPlaceholder("time_left", timeStr);
        context.addPlaceholder("time_color", timeColor);

        // Total duration
        long totalDuration = auction.endAt() - auction.createdAt();
        context.addPlaceholder("duration", TimeUtil.formatDuration(totalDuration));

        // Listed date (when the auction was created)
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, HH:mm");
        String listedDate = dateFormat.format(new java.util.Date(auction.createdAt()));
        context.addPlaceholder("listed_date", listedDate);

        // ═══════════════════════════════════════════════
        // STATUS INFORMATION
        // ═══════════════════════════════════════════════

        context.addPlaceholder("status", getStatusText(auction.status()));
        context.addPlaceholder("status_color", getStatusColor(auction.status()));

        // ═══════════════════════════════════════════════
        // AFFORDABILITY & ACTION CONTEXT
        // ═══════════════════════════════════════════════

        double effectivePrice = auction.buyNowPrice() != null ? auction.buyNowPrice() : auction.price();
        context.addPlaceholder("price", economyService.format(effectivePrice));

        boolean canAfford = playerBalance >= effectivePrice;
        context.addPlaceholder("affordable_text", canAfford ?
            "You can afford this item" : "Need more money to purchase");

        double neededAmount = effectivePrice - playerBalance;
        context.addPlaceholder("needed_amount", neededAmount > 0 ?
            economyService.format(neededAmount) : economyService.format(0));

        // ═══════════════════════════════════════════════
        // BUILD LORE
        // ═══════════════════════════════════════════════

        List<String> lore = new ArrayList<>();

        // Get lore template from messages.yml
        List<String> rawLore = kah.getConfigManager().getMessages().getStringList("gui.item-lore");
        for (String loreLine : rawLore) {
            // Process message with placeholders and hex color support
            String processed = kah.getConfigManager().processMessage(loreLine, context);
            lore.add(processed);
        }

        // Add action buttons based on context
        lore.add("");

        if (isOwnAuction) {
            // Player's own auction - show manage option
            String actionMsg = kah.getConfigManager().getMessage("gui.item-action.own-auction", context);
            for (String line : actionMsg.split("\n")) {
                lore.add(line);
            }
        } else if (canAfford) {
            // Can afford - show purchase options
            String actionMsg = kah.getConfigManager().getMessage("gui.item-action.can-purchase", context);
            for (String line : actionMsg.split("\n")) {
                lore.add(line);
            }
        } else {
            // Cannot afford - show insufficient funds
            String actionMsg = kah.getConfigManager().getMessage("gui.item-action.insufficient-funds", context);
            for (String line : actionMsg.split("\n")) {
                lore.add(line);
            }
        }

        return builder.setLore(lore).build();
    }

    /**
     * Format item type name to be more readable.
     * Example: DIAMOND_SWORD -> Diamond Sword
     */
    private String formatItemName(String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return "Unknown Item";
        }

        // Split by underscore and capitalize each word
        String[] parts = typeName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                formatted.append(" ");
            }
            // Capitalize first letter
            if (parts[i].length() > 0) {
                formatted.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    formatted.append(parts[i].substring(1));
                }
            }
        }

        return formatted.toString();
    }

    private String getStatusText(com.minekarta.playerauction.auction.model.AuctionStatus status) {
        switch (status) {
            case ACTIVE:
                return "ACTIVE";
            case FINISHED:
                return "SOLD";
            case CANCELLED:
                return "CANCELLED";
            case EXPIRED:
                return "EXPIRED";
            default:
                return status.name();
        }
    }

    private String getStatusColor(com.minekarta.playerauction.auction.model.AuctionStatus status) {
        // Modern hex colors (not legacy Minecraft colors)
        switch (status) {
            case ACTIVE:
                return "&#2ECC71"; // Emerald for active
            case FINISHED:
                return "&#F5A623"; // Amber Gold for sold
            case CANCELLED:
                return "&#7F8C8D"; // Slate Gray for cancelled
            case EXPIRED:
                return "&#E74C3C"; // Coral Red for expired
            default:
                return "&#ECF0F1"; // Cloud White for unknown
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        // Handle pagination, close, and player info clicks from the parent
        if (handleControlBarClick(event)) return;

        int slot = event.getSlot();

        // Handle clicking on an auction item
        int itemIndex = getItemIndexForSlot(slot);
        if (itemIndex != -1 && auctions != null && itemIndex < auctions.size()) {
            Auction clickedAuction = auctions.get(itemIndex);
            // Direct purchase with confirmation
            double price = clickedAuction.price();
            kah.getEconomyRouter().getService().getBalance(player.getUniqueId()).thenAccept(balance -> {
                if (balance >= price) {
                    // Sufficient funds, proceed with purchase
                    kah.getAuctionService().buyItem(player, clickedAuction.id()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(kah.getConfigManager().getPrefixedMessage("auction.purchase-success",
                                "%item%", clickedAuction.item().toItemStack().getType().toString(),
                                "%price%", kah.getEconomyRouter().getService().format(clickedAuction.price())));
                            // Refresh the GUI
                            new MainAuctionGui(kah, player, page, sortOrder, searchQuery).open();
                        }
                    });
                } else {
                    // Insufficient funds
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.insufficient-funds",
                        "%needed%", kah.getEconomyRouter().getService().format(price - balance),
                        "%balance%", kah.getEconomyRouter().getService().format(balance)));
                }
            });
            return;
        }

        // Handle custom control clicks matching the slot layout:
        // [47] Sort | [48] Search | [49] Profile | [50] MyListings | [51] Mailbox

        if (slot == 47) { // Sort button
            SortOrder nextSortOrder = sortOrder.next();
            new MainAuctionGui(kah, player, 1, nextSortOrder, searchQuery).open();
        } else if (slot == 48) { // Search button
            if (event.isRightClick() && searchQuery != null && !searchQuery.isEmpty()) {
                // Right-click to clear search
                new MainAuctionGui(kah, player, 1, sortOrder, null).open();
            } else {
                // Left-click to start search session
                player.closeInventory();
                kah.getSearchManager().startSearchSession(player, sortOrder);
            }
        } else if (slot == 50) { // My Listings button
            new MyListingsGui(kah, player, 1).open();
        } else if (slot == 51) { // Mailbox button
            new MailboxGui(kah, player, 1).open();
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MainAuctionGui(kah, player, newPage, sortOrder, searchQuery).open();
    }

    @Override
    protected String getCurrentSortOrder() {
        return sortOrder.getDisplayName();
    }
}
