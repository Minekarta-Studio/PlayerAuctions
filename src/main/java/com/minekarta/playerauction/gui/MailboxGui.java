package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.mailbox.model.MailboxItem;
import com.minekarta.playerauction.mailbox.model.MailboxItemType;
import com.minekarta.playerauction.util.TimeUtil;
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
    private List<MailboxItem> mailboxItems;

    // Available slots for mailbox items (same layout as other GUIs)
    private static final int[] MAILBOX_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, // Row 1
            19, 20, 21, 22, 23, 24, 25, // Row 2
            28, 29, 30, 31, 32, 33, 34, // Row 3
            37, 38, 39, 40, 41, 42, 43 // Row 4
    };

    private static final int ITEMS_PER_PAGE = MAILBOX_SLOTS.length; // 28 items per page

    public MailboxGui(PlayerAuction plugin, Player player, int page) {
        super(plugin, player, page, ITEMS_PER_PAGE);
        this.kah = plugin;
        setAsync(true);
    }

    private int getSlotForItemIndex(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= MAILBOX_SLOTS.length) {
            return -1;
        }
        return MAILBOX_SLOTS[itemIndex];
    }

    private int getItemIndexForSlot(int slot) {
        for (int i = 0; i < MAILBOX_SLOTS.length; i++) {
            if (MAILBOX_SLOTS[i] == slot) {
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
        return kah.getConfigManager().getMessage("gui.mailbox-title");
    }

    @Override
    protected void build() {
        // Fetch mailbox items for player
        kah.getMailboxService().getUnclaimedItems(player.getUniqueId(), page, itemsPerPage + 1)
                .thenAccept(fetchedItems -> {
                    // Determine pagination
                    this.hasNextPage = fetchedItems.size() > itemsPerPage;
                    this.mailboxItems = hasNextPage ? fetchedItems.subList(0, itemsPerPage) : fetchedItems;

                    // Build GUI on main thread
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        // Add mailbox items
                        for (int i = 0; i < mailboxItems.size(); i++) {
                            MailboxItem item = mailboxItems.get(i);
                            ItemStack displayItem = createMailboxDisplayItem(item);
                            int guiSlot = getSlotForItemIndex(i);
                            if (guiSlot != -1) {
                                inventory.setItem(guiSlot, displayItem);
                            }
                        }

                        // Show empty message if no items
                        if (mailboxItems.isEmpty()) {
                            List<String> emptyLore = new ArrayList<>();
                            emptyLore.add("");
                            emptyLore.add("<#BDC3C7>Your mailbox is empty");
                            emptyLore.add("");
                            emptyLore.add("<#7F8C8D>Sold items and expired");
                            emptyLore.add("<#7F8C8D>auctions will appear here");

                            inventory.setItem(22, new GuiItemBuilder(Material.HOPPER)
                                    .setName("<#7F8C8D>○ ᴇᴍᴘᴛʏ ᴍᴀɪʟʙᴏx")
                                    .setLore(emptyLore)
                                    .build());
                        }

                        // Add control bar and buttons
                        addControlBar();
                        addCustomControls();

                        // Open inventory
                        openInventory();
                    });
                }).exceptionally(ex -> {
                    kah.getLogger().severe("Error building MailboxGui: " + ex.getMessage());
                    ex.printStackTrace();
                    plugin.getServer().getScheduler().runTask(plugin, this::openInventory);
                    return null;
                });
    }

    private void addCustomControls() {
        // Back button (slot 46)
        net.kyori.adventure.text.Component backName = kah.getConfigManager().getMessage("gui.control-items.back");
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add("<#BDC3C7>Return to auction house");

        inventory.setItem(46, new GuiItemBuilder(Material.ARROW)
                .setName(backName)
                .setLore(backLore)
                .build());

        // Claim All button (slot 50) - only if there are items
        if (mailboxItems != null && !mailboxItems.isEmpty()) {
            List<String> claimAllLore = new ArrayList<>();
            claimAllLore.add("");
            claimAllLore.add("<#BDC3C7>Claim all items at once");
            claimAllLore.add("");
            claimAllLore.add("<#7F8C8D>Items: <#ECF0F1>" + mailboxItems.size());
            claimAllLore.add("");
            claimAllLore.add("<#2ECC71>Click to claim all");

            inventory.setItem(50, new GuiItemBuilder(Material.CHEST)
                    .setName("<#2ECC71>◈ ᴄʟᴀɪᴍ ᴀʟʟ")
                    .setLore(claimAllLore)
                    .build());
        }
    }

    private ItemStack createMailboxDisplayItem(MailboxItem mailboxItem) {
        if (mailboxItem.type() == MailboxItemType.ITEM) {
            return createItemDisplay(mailboxItem);
        } else {
            return createMoneyDisplay(mailboxItem);
        }
    }

    private ItemStack createItemDisplay(MailboxItem mailboxItem) {
        ItemStack originalItem = mailboxItem.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(originalItem);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<#F5A623>▸ ɪᴛᴇᴍ ʀᴇᴛᴜʀɴᴇᴅ");
        lore.add("");
        lore.add("<#BDC3C7>Reason: <#ECF0F1>" + mailboxItem.reason());
        lore.add("<#BDC3C7>Quantity: <#ECF0F1>" + originalItem.getAmount());
        lore.add("");

        // Time remaining
        long timeRemaining = mailboxItem.getTimeRemaining();
        String timeStr = TimeUtil.formatDuration(timeRemaining);
        String timeColor = timeRemaining < 24 * 60 * 60 * 1000 ? "<#E74C3C>" : "<#2ECC71>";
        lore.add("<#BDC3C7>Expires in: " + timeColor + timeStr);
        lore.add("");
        lore.add("<#2ECC71>▶ ᴄʟɪᴄᴋ ᴛᴏ ᴄʟᴀɪᴍ");
        lore.add("<#7F8C8D>Item will be added to inventory");

        return builder.setLore(lore).build();
    }

    private ItemStack createMoneyDisplay(MailboxItem mailboxItem) {
        GuiItemBuilder builder = new GuiItemBuilder(Material.GOLD_INGOT);

        String formattedAmount = kah.getEconomyRouter().getService().format(mailboxItem.amount());

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("<#F5A623>▸ ᴍᴏɴᴇʏ ʀᴇᴄᴇɪᴠᴇᴅ");
        lore.add("");
        lore.add("<#BDC3C7>Reason: <#ECF0F1>" + mailboxItem.reason());
        lore.add("<#BDC3C7>Amount: <#2ECC71>" + formattedAmount);
        lore.add("");

        // Time remaining
        long timeRemaining = mailboxItem.getTimeRemaining();
        String timeStr = TimeUtil.formatDuration(timeRemaining);
        String timeColor = timeRemaining < 24 * 60 * 60 * 1000 ? "<#E74C3C>" : "<#2ECC71>";
        lore.add("<#BDC3C7>Expires in: " + timeColor + timeStr);
        lore.add("");
        lore.add("<#2ECC71>▶ ᴄʟɪᴄᴋ ᴛᴏ ᴄʟᴀɪᴍ");
        lore.add("<#7F8C8D>Money will be deposited to account");

        return builder
                .setName("<#F5A623>◈ " + formattedAmount)
                .setLore(lore)
                .build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handleControlBarClick(event))
            return;

        int slot = event.getSlot();

        // Back button
        if (slot == 46) {
            new MainAuctionGui(kah, player, 1, com.minekarta.playerauction.gui.model.SortOrder.NEWEST).open();
            return;
        }

        // Claim All button
        if (slot == 50) {
            player.closeInventory();
            kah.getMailboxService().claimAll(player).thenAccept(count -> {
                if (count > 0) {
                    // Reopen mailbox after short delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        new MailboxGui(kah, player, 1).open();
                    }, 20L);
                }
            });
            return;
        }

        // Handle clicking on mailbox item
        int itemIndex = getItemIndexForSlot(slot);
        if (itemIndex != -1 && mailboxItems != null && itemIndex < mailboxItems.size()) {
            MailboxItem clickedItem = mailboxItems.get(itemIndex);

            player.closeInventory();

            // Claim the item
            kah.getMailboxService().claimItem(player, clickedItem.id()).thenAccept(success -> {
                if (success) {
                    // Refresh mailbox GUI after short delay
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        new MailboxGui(kah, player, page).open();
                    }, 10L);
                }
            });
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MailboxGui(kah, player, newPage).open();
    }
}
