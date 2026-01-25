package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for displaying player's mailbox (unclaimed items and money from auctions)
 */
public class MailboxGui extends PaginatedGui {

    private final PlayerAuction kah;

    public MailboxGui(PlayerAuction plugin, Player player, int page) {
        super(plugin, player, page, 28); // 28 items per page
        this.kah = plugin;
        setAsync(true);
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.mailbox-title", null);
    }

    @Override
    protected void build() {
        // Add control bar first
        addControlBar();

        // Add back button
        addBackButton();

        // TODO: Load mailbox items from storage
        // For now, show empty mailbox message

        List<String> emptyLore = new ArrayList<>();
        emptyLore.add("");
        emptyLore.add("&#BDC3C7Your mailbox is empty");
        emptyLore.add("");
        emptyLore.add("&#7F8C8DSold items and expired");
        emptyLore.add("&#7F8C8Dauctions will appear here");

        inventory.setItem(22, new GuiItemBuilder(Material.HOPPER)
            .setName("&#7F8C8D○ ᴇᴍᴘᴛʏ ᴍᴀɪʟʙᴏx")
            .setLore(emptyLore)
            .build());

        // Open inventory after build
        plugin.getServer().getScheduler().runTask(plugin, this::openInventory);
    }

    private void addBackButton() {
        String backName = kah.getConfigManager().getMessage("gui.control-items.back", null);
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add("&#BDC3C7Return to auction house");

        inventory.setItem(45, new GuiItemBuilder(Material.ARROW)
            .setName(backName)
            .setLore(backLore)
            .build());
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handleControlBarClick(event)) return;

        int slot = event.getSlot();

        // Back button
        if (slot == 45) {
            new MainAuctionGui(kah, player, 1,
                com.minekarta.playerauction.gui.model.SortOrder.NEWEST, null).open();
            return;
        }

        // TODO: Handle claiming items from mailbox
    }

    @Override
    protected void openPage(int newPage) {
        new MailboxGui(kah, player, newPage).open();
    }
}
