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
        // Create placeholder context for GUI-specific placeholders
        PlaceholderContext context = new PlaceholderContext()
            .addPlaceholder("sort_order", sortOrder.getDisplayName());

        // Update sort button with current sort order
        String sortName = kah.getConfigManager().getMessage("gui.control-items.sort", context);
        List<String> sortLore = new ArrayList<>();
        sortLore.add("§7Current: §e" + sortOrder.getDisplayName());
        sortLore.add("§7Click to cycle through options");
        sortLore.add("§8Available:");
        for (com.minekarta.playerauction.gui.model.SortOrder order : com.minekarta.playerauction.gui.model.SortOrder.values()) {
            sortLore.add("§8  • " + order.getDisplayName());
        }
        inventory.setItem(46, new GuiItemBuilder(Material.COMPARATOR).setName("§a" + sortName).setLore(sortLore).build());

        // Update search button with current search status
        String searchName = kah.getConfigManager().getMessage("gui.control-items.search", context);
        List<String> searchLore = new ArrayList<>();
        String currentQuery = searchQuery != null ? searchQuery : "";
        PlaceholderContext searchContext = new PlaceholderContext()
            .addPlaceholder("query", currentQuery);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchLore.add("§7Currently searching for:");
            searchLore.add("§e\"" + searchQuery + "\"");
            searchLore.add("§7Click to modify search");
        } else {
            searchLore.add("§7Click to search for items");
            searchLore.add("§8Type keywords to find");
        }
        inventory.setItem(47, new GuiItemBuilder(Material.ENDER_EYE).setName("§a" + searchName).setLore(searchLore).build());
    }

    private ItemStack createAuctionItem(Auction auction, double playerBalance) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Get clean item display name
        String itemDisplayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
            ? item.getItemMeta().getDisplayName()
            : formatItemName(item.getType().toString());

        // Create placeholder context for auction-specific information
        PlaceholderContext context = new PlaceholderContext()
            .addPlaceholder("seller", kah.getPlayerNameCache().getName(auction.seller()).join())
            .addPlaceholder("item_name", itemDisplayName)
            .addPlaceholder("starting_price", kah.getEconomyRouter().getService().format(auction.price()))
            .addPlaceholder("current_bid", kah.getEconomyRouter().getService().format(auction.price()))
            .addPlaceholder("buy_now_price", auction.buyNowPrice() != null ?
                kah.getEconomyRouter().getService().format(auction.buyNowPrice()) : "N/A")
            .addPlaceholder("reserve_price", auction.reservePrice() != null ?
                kah.getEconomyRouter().getService().format(auction.reservePrice()) : "N/A");

        // Time left
        long timeLeft = auction.endAt() - System.currentTimeMillis();
        String timeStr = TimeUtil.formatDuration(timeLeft);
        context.addPlaceholder("time_left", timeStr);
        context.addPlaceholder("duration", TimeUtil.formatDuration(auction.endAt() - auction.createdAt()));

        // Status information
        context.addPlaceholder("status", getStatusText(auction.status()));
        context.addPlaceholder("status_color", getStatusColor(auction.status()));

        // Price with affordability info
        double price = auction.price();
        String formattedPrice = kah.getEconomyRouter().getService().format(price);
        context.addPlaceholder("price", formattedPrice);
        context.addPlaceholder("affordable_text", playerBalance >= price ?
            "You can afford this item" : "Need more money to purchase");

        // ✅ FIX: Build lore using String with hex colors (not Component - item lore doesn't support gradients)
        List<String> lore = new ArrayList<>();

        // Get lore template from messages.yml
        List<String> rawLore = kah.getConfigManager().getMessages().getStringList("gui.item-lore");
        for (String loreLine : rawLore) {
            // Process message with placeholders and hex color support
            String processed = kah.getConfigManager().processMessage(loreLine, context);
            lore.add(processed);
        }

        // Add action buttons
        lore.add("");
        if (playerBalance >= price) {
            String actionMsg = kah.getConfigManager().getMessage("gui.item-action.can-purchase", context);
            // Handle multi-line action buttons (split by \n)
            for (String line : actionMsg.split("\n")) {
                lore.add(line);
            }
        } else {
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

        // Handle custom control clicks (these are the ones not handled by parent class)
        if (slot == 46) { // Sort button
            SortOrder nextSortOrder = sortOrder.next();
            new MainAuctionGui(kah, player, 1, nextSortOrder, searchQuery).open();
        } else if (slot == 47) { // Search button
            // ✅ FIX: Use SearchManager to start search session
            player.closeInventory();
            kah.getSearchManager().startSearchSession(player, sortOrder);
        } else if (slot == 51) { // Search button (alternative slot if needed)
            // ✅ FIX: Same as slot 47
            player.closeInventory();
            kah.getSearchManager().startSearchSession(player, sortOrder);
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
