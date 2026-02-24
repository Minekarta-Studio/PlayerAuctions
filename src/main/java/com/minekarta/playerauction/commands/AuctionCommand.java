package com.minekarta.playerauction.commands;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.auction.AuctionService;
import com.minekarta.playerauction.config.ConfigManager;
import com.minekarta.playerauction.gui.HistoryGui;
import com.minekarta.playerauction.gui.MainAuctionGui;
import com.minekarta.playerauction.gui.MyListingsGui;
import com.minekarta.playerauction.gui.model.SortOrder;
import com.minekarta.playerauction.players.PlayerSettingsService;
import com.minekarta.playerauction.util.DurationParser;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.incendo.cloud.Command;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.bukkit.parser.OfflinePlayerParser;

public class AuctionCommand {

    private final PlayerAuction plugin;
    private final LegacyPaperCommandManager<CommandSender> commandManager;
    private final AuctionService auctionService;
    private final ConfigManager configManager;
    private final PlayerSettingsService playerSettingsService;

    public AuctionCommand(PlayerAuction plugin, LegacyPaperCommandManager<CommandSender> commandManager,
            AuctionService auctionService, ConfigManager configManager,
            PlayerSettingsService playerSettingsService) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.auctionService = auctionService;
        this.configManager = configManager;
        this.playerSettingsService = playerSettingsService;

        registerCommands();
    }

    private void registerCommands() {
        Command.Builder<CommandSender> base = commandManager.commandBuilder("ah", "auction", "auctionhouse");

        // Base command: /ah - opens MainAuctionGui
        commandManager.command(base
                .senderType(Player.class)
                .handler(ctx -> {
                    Player player = ctx.sender();
                    new MainAuctionGui(plugin, player, 1, SortOrder.NEWEST).open();
                }));

        // /ah help
        commandManager.command(base.literal("help")
                .senderType(Player.class)
                .permission("playerauctions.use")
                .handler(ctx -> handleHelp(ctx.sender())));

        // /ah listings
        commandManager.command(base.literal("listings")
                .senderType(Player.class)
                .permission("playerauctions.use")
                .handler(ctx -> {
                    Player player = ctx.sender();
                    new MyListingsGui(plugin, player, 1).open();
                }));

        // /ah myauctions
        commandManager.command(base.literal("myauctions")
                .senderType(Player.class)
                .permission("playerauctions.use")
                .handler(ctx -> {
                    Player player = ctx.sender();
                    new MyListingsGui(plugin, player, 1).open();
                }));

        // /ah reload
        commandManager.command(base.literal("reload")
                .permission("playerauctions.reload")
                .handler(ctx -> {
                    configManager.loadConfigs();
                    ctx.sender().sendMessage(configManager.getPrefixedMessage("info.reload-success"));
                }));

        // /ah sell <price> [buyNow] [duration]
        commandManager.command(base.literal("sell")
                .senderType(Player.class)
                .permission("playerauctions.sell")
                .required("price", DoubleParser.doubleParser())
                .optional("buyNow", DoubleParser.doubleParser())
                .optional("duration", StringParser.stringParser())
                .handler(ctx -> {
                    Player player = ctx.sender();
                    double price = ctx.get("price");
                    Double buyNow = ctx.optional("buyNow").map(o -> (Double) o).orElse(null);
                    String durationStr = ctx.optional("duration").map(o -> (String) o)
                            .orElse(configManager.getConfig().getString("auction.defaults.duration", "24h"));
                    handleSell(player, price, buyNow, durationStr);
                }));

        // /ah search <keyword>
        commandManager.command(base.literal("search")
                .senderType(Player.class)
                .permission("playerauctions.search")
                .required("keyword", StringParser.greedyStringParser())
                .handler(ctx -> {
                    Player player = ctx.sender();
                    // Assuming you have logic in MainAuctionGui to handle search,
                    // currently MainAuctionGui is opened for search the same way.
                    new MainAuctionGui(plugin, player, 1, SortOrder.NEWEST).open();
                }));

        // /ah notify <on|off>
        commandManager.command(base.literal("notify")
                .senderType(Player.class)
                .permission("playerauctions.notify")
                .required("state", StringParser.stringParser())
                .handler(ctx -> {
                    Player player = ctx.sender();
                    String state = ctx.get("state");
                    if (!state.equalsIgnoreCase("on") && !state.equalsIgnoreCase("off")) {
                        player.sendMessage(configManager.getPrefixedMessage("errors.usage-notify", "{usage}",
                                "/ah notify <on|off>"));
                        return;
                    }
                    boolean enabled = state.equalsIgnoreCase("on");
                    playerSettingsService.setNotificationsEnabled(player, enabled);

                    if (enabled) {
                        player.sendMessage(configManager.getPrefixedMessage("info.notifications-on",
                                "Auction notifications have been enabled."));
                    } else {
                        player.sendMessage(configManager.getPrefixedMessage("info.notifications-off",
                                "Auction notifications have been disabled."));
                    }
                }));

        // /ah history [player]
        commandManager.command(base.literal("history")
                .senderType(Player.class)
                .permission("playerauctions.history")
                .optional("target", OfflinePlayerParser.offlinePlayerParser())
                .handler(ctx -> {
                    Player player = ctx.sender();
                    OfflinePlayer target = ctx.optional("target").map(o -> (OfflinePlayer) o).orElse(player);

                    if (!target.getUniqueId().equals(player.getUniqueId())
                            && !player.hasPermission("playerauctions.history.others")) {
                        player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
                        return;
                    }

                    if (!target.hasPlayedBefore() && target.getName() != null) {
                        player.sendMessage(configManager.getPrefixedMessage("errors.player-not-found", "{player}",
                                target.getName()));
                        return;
                    }

                    new HistoryGui(plugin, player, target.getUniqueId(), 1).open();
                }));
    }

    private void handleSell(Player player, double price, Double buyNow, String durationStr) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-item-in-hand"));
            return;
        }

        if (price < configManager.getConfig().getDouble("auction.min-price", 1.0)) {
            player.sendMessage(configManager.getPrefixedMessage("errors.price-too-low", "{min}",
                    String.valueOf(configManager.getConfig().getDouble("auction.min-price", 1.0))));
            return;
        }

        long durationMillis = DurationParser.parse(durationStr).orElse(0L);
        if (durationMillis <= 0) {
            player.sendMessage(configManager.getPrefixedMessage("errors.duration-out-of-range", "{min}", "1s", "{max}",
                    "infinite"));
            return;
        }

        int maxAuctions = configManager.getConfig().getInt("auction.max-auctions-per-player", 5);
        auctionService.getPlayerActiveAuctionCount(player.getUniqueId()).thenAccept(count -> {
            if (count >= maxAuctions) {
                player.sendMessage(configManager.getPrefixedMessage("errors.listing-limit-reached", "{limit}",
                        String.valueOf(maxAuctions)));
                return;
            }

            ItemStack toSell = itemInHand.clone();
            player.getInventory().setItemInMainHand(null);

            auctionService.createListing(player, toSell, price, buyNow, null, durationMillis).thenAccept(success -> {
                if (success) {
                    String formattedDuration = com.minekarta.playerauction.util.TimeUtil.formatDuration(durationMillis);
                    String formattedPrice = plugin.getEconomyRouter().getService().format(price);

                    player.sendMessage(configManager.getPrefixedMessage("info.listed",
                            "{item}", toSell.getType().toString(),
                            "{price}", formattedPrice,
                            "{duration}", formattedDuration));

                    plugin.getBroadcastManager().broadcastListing(
                            player.getName(),
                            toSell.getType().toString(),
                            toSell.getAmount(),
                            formattedPrice,
                            player.getWorld());
                } else {
                    player.sendMessage(configManager.getPrefixedMessage("errors.generic-error"));
                    player.getInventory().addItem(toSell);
                }
            });
        });
    }

    private void handleHelp(Player player) {
        player.sendMessage("§6§lPlayerAuctions Help");
        player.sendMessage("§7─────────────────────────");
        player.sendMessage("§e/ah §7- Opens the auction house GUI");

        if (player.hasPermission("playerauctions.sell")) {
            player.sendMessage("§e/ah sell <price> [buy_now] [duration] §7- Sell item in hand");
            player.sendMessage("§7  §8Price: Listing price");
            player.sendMessage("§7  §8Buy Now: Instant purchase price (optional)");
            player.sendMessage("§7  §8Duration: 1h, 6h, 12h, 24h, 48h, 72h (default: 24h)");
        }

        if (player.hasPermission("playerauctions.search")) {
            player.sendMessage("§e/ah search <keyword> §7- Search for auction items");
        }

        if (player.hasPermission("playerauctions.use")) {
            player.sendMessage("§e/ah listings | myauctions §7- View your auction listings");
        }

        if (player.hasPermission("playerauctions.notify")) {
            player.sendMessage("§e/ah notify <on/off> §7- Toggle auction notifications");
        }

        if (player.hasPermission("playerauctions.history")) {
            player.sendMessage("§e/ah history [player] §7- View auction history");
        }

        if (player.hasPermission("playerauctions.reload")) {
            player.sendMessage("§e/ah reload §7- Reload plugin configuration");
        }

        player.sendMessage("§7─────────────────────────");
        player.sendMessage("§6Aliases: §f/ah, /auction, /auctionhouse");
    }
}
