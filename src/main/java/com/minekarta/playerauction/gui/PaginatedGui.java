package com.minekarta.playerauction.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.util.PlaceholderContext;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PaginatedGui extends Gui {

    protected int page;
    protected final int itemsPerPage;
    protected boolean hasNextPage = false;

    public PaginatedGui(PlayerAuction plugin, Player player, int page, int itemsPerPage) {
        super(plugin, player);
        this.page = page;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public int getSize() {
        return 54; // Standard 6-row inventory for better layout with borders
    }

    protected void addControlBar() {
        // Get border configuration
        boolean borderEnabled = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getBoolean("gui.border.enabled", true);
        Material borderMaterial = Material.getMaterial(((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getString("gui.border.material", "BLACK_STAINED_GLASS_PANE"));
        String borderName = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getString("gui.border.name", " ");
        java.util.List<String> borderLore = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getStringList("gui.border.lore");

        ItemStack borderFiller = new GuiItemBuilder(borderMaterial != null ? borderMaterial : Material.BLACK_STAINED_GLASS_PANE).setName(borderName).setLore(borderLore).build();
        ItemStack accentFiller = new GuiItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).setName(" ").build();

        // Add border if enabled
        if (borderEnabled) {
            // Top and bottom borders
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, borderFiller); // Top row
                inventory.setItem(i + 45, borderFiller); // Bottom row
            }
            // Side borders
            for (int i = 1; i < 5; i++) {
                inventory.setItem(i * 9, borderFiller); // Left side
                inventory.setItem(i * 9 + 8, borderFiller); // Right side
            }
        } else {
            // Create decorative border for bottom control area only
            for (int i = 36; i <= 44; i++) {
                inventory.setItem(i, borderFiller);
            }
        }

        // Corner accents for visual appeal
        inventory.setItem(borderEnabled ? 0 : 36, borderFiller); // Bottom-left corner
        inventory.setItem(borderEnabled ? 8 : 44, borderFiller); // Bottom-right corner

        // Add side accents for visual framing
        for (int i = 45; i <= 53; i++) {
            if (i % 2 == 1) {
                inventory.setItem(i, accentFiller);
            }
        }

        // Clean layout with essential controls only
        // Previous Page Button (Slot 46) - Left side
        if (page > 1) {
            PlaceholderContext prevPageContext = new PlaceholderContext()
                .addPlaceholder("current_page", page - 1);
            String prevPageName = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessage("gui.control-items.previous-page", prevPageContext);
            String prevPageLore = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessage("gui.control-items.previous-page-lore", prevPageContext)
                .replace("[", "").replace("]", "").replace(",", "\n"); // Convert list format to newlines

            inventory.setItem(46, new GuiItemBuilder(Material.ARROW)
                .setName(prevPageName)
                .setLore(prevPageLore.split("\n"))
                .build());
        }

        // Sort Button (Slot 47) - Left side
        PlaceholderContext sortContext = new PlaceholderContext()
            .addPlaceholder("sort_order", getCurrentSortOrder());
        String sortName = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessage("gui.control-items.sort", sortContext);
        String sortLore = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessage("gui.control-items.sort-lore", sortContext)
            .replace("[", "").replace("]", "").replace(",", "\n"); // Convert list format to newlines

        inventory.setItem(47, new GuiItemBuilder(Material.COMPARATOR)
            .setName(sortName)
            .setLore(sortLore.split("\n"))
            .build());

        // Player Info Item (Slot 49) - Center position
        createPlayerInfoItem().thenAccept(item -> {
            inventory.setItem(49, item);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                inventory.getViewers().forEach(viewer -> {
                    if (viewer instanceof Player playerViewer) {
                        playerViewer.updateInventory();
                    }
                });
            });
        });

        // Next Page Button (Slot 52) - Right side
        if (hasNextPage) {
            PlaceholderContext nextPageContext = new PlaceholderContext()
                .addPlaceholder("current_page", page + 1);
            String nextPageName = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessage("gui.control-items.next-page", nextPageContext);
            String nextPageLore = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessage("gui.control-items.next-page-lore", nextPageContext)
                .replace("[", "").replace("]", "").replace(",", "\n"); // Convert list format to newlines

            inventory.setItem(52, new GuiItemBuilder(Material.ARROW)
                .setName(nextPageName)
                .setLore(nextPageLore.split("\n"))
                .build());
        }
    }

    protected boolean handleControlBarClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // Navigation controls
        if (slot == 46 && page > 1) {
            // Previous page
            openPage(page - 1);
            return true;
        } else if (slot == 52 && hasNextPage) {
            // Next page
            openPage(page + 1);
            return true;
        } else if (slot == 47) {
            // Sort button - to be implemented by subclasses
            return false;
        }

        // Default handling for other slots
        return false;
    }

    protected String getCurrentSortOrder() {
        return "Newest"; // Default implementation, can be overridden by subclasses
    }

    protected abstract void openPage(int newPage);

    /**
     * Updates the player info item with the correct total pages
     */
    protected void updatePlayerInfoItem(int totalPages) {
        // Create placeholder context for player info with correct total pages
        com.minekarta.playerauction.util.PlaceholderContext context = new com.minekarta.playerauction.util.PlaceholderContext()
            .addPlaceholder("player_name", player.getName())
            .addPlaceholder("balance", "0") // We don't have the balance here, but it's not used in the name anyway)
            .addPlaceholder("page", page)
            .addPlaceholder("total_pages", totalPages);

        // Get the player info lines from config using placeholders
        java.util.List<String> rawLines = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getMessages().getStringList("gui.control-items.player-info");
        java.util.List<String> processedLines = new java.util.ArrayList<>();

        for (String line : rawLines) {
            String processedLine = context.applyTo(line);
            processedLines.add(processedLine);
        }

        // Extract the name and lore from the processed lines
        String itemName = processedLines.size() > 0 ? processedLines.get(0) : "&e" + player.getName();
        String balanceLine = processedLines.size() > 1 ? processedLines.get(1) : "&7Balance: &e0";
        String pageLine = processedLines.size() > 2 ? processedLines.get(2) : "&7Page: &e" + page + "/" + totalPages;

        ItemStack playerInfoItem = new GuiItemBuilder(org.bukkit.Material.PLAYER_HEAD)
            .setSkullOwner(player.getName())
            .setName(itemName)
            .setLore(balanceLine, pageLine)
            .build();

        inventory.setItem(49, playerInfoItem);
    }
}
