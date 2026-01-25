package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class Gui implements InventoryHolder, Listener {

    protected final JavaPlugin plugin;
    protected final Player player;
    protected Inventory inventory;
    private boolean isAsync = false;

    public Gui(JavaPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract String getTitle();
    protected abstract int getSize();
    protected abstract void build();
    protected abstract void onClick(InventoryClickEvent event);

    /**
     * Mark this GUI as async. When true, the GUI will NOT auto-open after build().
     * The subclass must call openInventory() manually after async operations complete.
     */
    protected void setAsync(boolean async) {
        this.isAsync = async;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, getSize(), getTitle());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.build();

        // Only auto-open if not async - async GUIs will call openInventory() themselves
        if (!isAsync) {
            player.openInventory(inventory);
        }
    }

    /**
     * Opens the inventory to the player.
     * For async GUIs, call this after all async operations complete.
     * Ensures execution on main thread.
     */
    protected void openInventory() {
        if (player.isOnline()) {
            // Ensure we're on main thread
            if (Bukkit.isPrimaryThread()) {
                player.openInventory(inventory);
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        player.openInventory(inventory);
                    }
                });
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != this) {
            return;
        }

        onClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }

    protected CompletableFuture<ItemStack> createPlayerInfoItem() {
        if (!(plugin instanceof PlayerAuction kah)) {
            // Fallback for safety, though it should always be a PlayerAuction instance
            return CompletableFuture.completedFuture(
                new GuiItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(player.getName())
                    .setName("&e" + player.getName())
                    .build()
            );
        }

        return kah.getEconomyRouter().getService().getBalance(player.getUniqueId()).thenApply(balance -> {
            String formattedBalance = kah.getEconomyRouter().getService().format(balance);

            // Create placeholder context for player info
            com.minekarta.playerauction.util.PlaceholderContext context = new com.minekarta.playerauction.util.PlaceholderContext()
                .addPlaceholder("player_name", player.getName())
                .addPlaceholder("balance", formattedBalance);

            // Add page info if this is a paginated GUI
            if (this instanceof PaginatedGui paginatedGui) {
                context.addPlaceholder("page", paginatedGui.page);

                // For now, we'll set total pages to "?" and it will be updated later in the specific GUIs
                context.addPlaceholder("total_pages", "?");
            }

            // Get the player info lines from config using placeholders
            java.util.List<String> rawLines = kah.getConfigManager().getMessages().getStringList("gui.control-items.player-info");
            java.util.List<String> processedLines = new java.util.ArrayList<>();

            for (String line : rawLines) {
                String processedLine = context.applyTo(line);
                processedLines.add(processedLine);
            }

            // Extract the name and lore from the processed lines
            String itemName = processedLines.size() > 0 ? processedLines.get(0) : "&e" + player.getName();
            String balanceLine = processedLines.size() > 1 ? processedLines.get(1) : "&7Balance: &e" + formattedBalance;
            String pageLine = processedLines.size() > 2 ? processedLines.get(2) : "&7Page: &e" + (this instanceof PaginatedGui ? ((PaginatedGui) this).page : "1");

            GuiItemBuilder builder = new GuiItemBuilder(Material.PLAYER_HEAD)
                .setSkullOwner(player.getName())
                .setName(itemName)
                .setLore(balanceLine, pageLine);

            return builder.build();
        });
    }
}
