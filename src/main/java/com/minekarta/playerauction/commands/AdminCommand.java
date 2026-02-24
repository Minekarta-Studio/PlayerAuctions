package com.minekarta.playerauction.commands;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.parser.standard.StringParser;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminCommand {

    private final PlayerAuction plugin;
    private final LegacyPaperCommandManager<CommandSender> commandManager;
    private final ConfigManager configManager;

    private final Set<UUID> debugPlayers = new HashSet<>();

    public AdminCommand(PlayerAuction plugin, LegacyPaperCommandManager<CommandSender> commandManager,
            ConfigManager configManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.configManager = configManager;

        registerCommands();
    }

    private void registerCommands() {
        Command.Builder<CommandSender> base = commandManager.commandBuilder("ahadmin")
                .permission("playerauctions.admin");

        // Base command: /ahadmin - Just show help or a message for now
        commandManager.command(base
                .senderType(Player.class)
                .handler(ctx -> {
                    ctx.sender().sendMessage("§6§lPlayerAuctions Admin Commands");
                    ctx.sender().sendMessage("§7─────────────────────────");
                    ctx.sender().sendMessage("§e/ahadmin debug <on/off> §7- Toggle debug mode");
                    ctx.sender().sendMessage("§7─────────────────────────");
                }));

        // /ahadmin debug <on|off>
        commandManager.command(base.literal("debug")
                .senderType(Player.class)
                .required("state", StringParser.stringParser())
                .handler(ctx -> {
                    Player player = ctx.sender();
                    String state = ctx.get("state");

                    if (!state.equalsIgnoreCase("on") && !state.equalsIgnoreCase("off")) {
                        player.sendMessage("Usage: /ahadmin debug <on|off>");
                        return;
                    }

                    boolean enable = state.equalsIgnoreCase("on");
                    if (enable) {
                        addPlayerToDebugMode(player.getUniqueId());
                        player.sendMessage(configManager.getPrefixedMessage("admin.debug-enabled"));
                    } else {
                        removePlayerFromDebugMode(player.getUniqueId());
                        player.sendMessage(configManager.getPrefixedMessage("admin.debug-disabled"));
                    }
                }));
    }

    public boolean isInDebugMode(Player player) {
        return debugPlayers.contains(player.getUniqueId());
    }

    public boolean isInDebugMode(UUID playerId) {
        return debugPlayers.contains(playerId);
    }

    public void addPlayerToDebugMode(UUID playerId) {
        debugPlayers.add(playerId);
    }

    public void removePlayerFromDebugMode(UUID playerId) {
        debugPlayers.remove(playerId);
    }
}
