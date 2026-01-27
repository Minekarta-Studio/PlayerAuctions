package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.util.MessageParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder class untuk membuat ItemStack dengan support MiniMessage, Hex, RGB, dan Legacy colors.
 */
public class GuiItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public GuiItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public GuiItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    /**
     * Set display name dengan full format support (MiniMessage, Hex, RGB, Legacy).
     *
     * @param name Display name
     * @return Builder instance
     */
    public GuiItemBuilder setName(String name) {
        if (itemMeta != null) {
            // Use MessageParser untuk comprehensive format support
            String parsed = MessageParser.parseToLegacy(name);
            itemMeta.setDisplayName(parsed);
        }
        return this;
    }

    /**
     * Set lore dengan full format support.
     *
     * @param lore Lore lines
     * @return Builder instance
     */
    public GuiItemBuilder setLore(String... lore) {
        if (itemMeta != null) {
            List<String> parsedLore = List.of(lore).stream()
                    .map(MessageParser::parseToLegacy)
                    .collect(Collectors.toList());
            itemMeta.setLore(parsedLore);
        }
        return this;
    }

    /**
     * Set lore dari list dengan full format support.
     *
     * @param lore Lore list
     * @return Builder instance
     */
    public GuiItemBuilder setLore(List<String> lore) {
        if (itemMeta != null) {
            List<String> parsedLore = lore.stream()
                    .map(MessageParser::parseToLegacy)
                    .collect(Collectors.toList());
            itemMeta.setLore(parsedLore);
        }
        return this;
    }

    /**
     * Set lore using Components (supports MiniMessage gradients in item tooltips).
     * This is the CORRECT way to use MiniMessage in GUI items - preserves gradients!
     *
     * @param lore Component list
     * @return Builder instance
     */
    public GuiItemBuilder setLoreComponents(List<Component> lore) {
        if (itemMeta != null) {
            itemMeta.lore(lore);  // Paper's Component lore method - preserves MiniMessage!
        }
        return this;
    }

    /**
     * Set lore using MiniMessage strings (auto-converts to Components).
     * Use this when you have MiniMessage-formatted strings and want to preserve gradients.
     *
     * @param miniMessageLore MiniMessage formatted lore lines
     * @return Builder instance
     */
    public GuiItemBuilder setLoreMiniMessage(List<String> miniMessageLore) {
        if (itemMeta != null) {
            List<Component> components = miniMessageLore.stream()
                    .map(MessageParser::parse)  // Parse to Component, preserves gradients
                    .collect(Collectors.toList());
            itemMeta.lore(components);  // Set as Component lore
        }
        return this;
    }

    /**
     * Set amount of items in stack.
     *
     * @param amount Item amount
     * @return Builder instance
     */
    public GuiItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    /**
     * Add single line ke lore dengan full format support.
     *
     * @param line Lore line to add
     * @return Builder instance
     */
    public GuiItemBuilder addLore(String line) {
        if (itemMeta != null) {
            List<String> lore = itemMeta.getLore();
            if (lore == null) {
                lore = new java.util.ArrayList<>();
            }
            String parsed = MessageParser.parseToLegacy(line);
            lore.add(parsed);
            itemMeta.setLore(lore);
        }
        return this;
    }

    /**
     * Set skull owner untuk player heads.
     *
     * @param ownerName Player name
     * @return Builder instance
     */
    public GuiItemBuilder setSkullOwner(String ownerName) {
        if (itemStack.getType() == Material.PLAYER_HEAD && itemMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) itemMeta;
            skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(ownerName));
        }
        return this;
    }

    /**
     * Build final ItemStack.
     *
     * @return Built ItemStack
     */
    public ItemStack build() {
        if (itemMeta != null) {
            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }
}

