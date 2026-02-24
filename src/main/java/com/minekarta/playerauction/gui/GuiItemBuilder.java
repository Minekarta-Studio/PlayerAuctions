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
 * Builder class untuk membuat ItemStack dengan support MiniMessage, Hex, RGB,
 * dan Legacy colors.
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
     * Set display name dengan Component (supports MiniMessage natively).
     *
     * @param component Display name component
     * @return Builder instance
     */
    public GuiItemBuilder setName(Component component) {
        if (itemMeta != null) {
            itemMeta.displayName(component);
        }
        return this;
    }

    /**
     * Set display name dengan full format support (MiniMessage, Hex, RGB, Legacy).
     * Parses the string to a Component.
     *
     * @param name Display name
     * @return Builder instance
     */
    public GuiItemBuilder setName(String name) {
        if (itemMeta != null) {
            itemMeta.displayName(MessageParser.parse(name));
        }
        return this;
    }

    /**
     * Set lore dengan full format support (Parses strings to Components).
     *
     * @param lore Lore lines
     * @return Builder instance
     */
    public GuiItemBuilder setLore(String... lore) {
        if (itemMeta != null) {
            List<Component> parsedLore = List.of(lore).stream()
                    .map(MessageParser::parse)
                    .collect(Collectors.toList());
            itemMeta.lore(parsedLore);
        }
        return this;
    }

    /**
     * Set lore dari list dengan full format support (Parses strings to Components).
     *
     * @param lore Lore list
     * @return Builder instance
     */
    public GuiItemBuilder setLore(List<String> lore) {
        if (itemMeta != null) {
            List<Component> parsedLore = lore.stream()
                    .map(MessageParser::parse)
                    .collect(Collectors.toList());
            itemMeta.lore(parsedLore);
        }
        return this;
    }

    /**
     * Set lore using Components (supports MiniMessage gradients in item tooltips).
     * This is the CORRECT way to use MiniMessage in GUI items - preserves
     * gradients!
     *
     * @param lore Component list
     * @return Builder instance
     */
    public GuiItemBuilder setLoreComponents(List<Component> lore) {
        if (itemMeta != null) {
            itemMeta.lore(lore);
        }
        return this;
    }

    /**
     * Set lore using MiniMessage strings (auto-converts to Components).
     * Use this when you have MiniMessage-formatted strings and want to preserve
     * gradients.
     *
     * @param miniMessageLore MiniMessage formatted lore lines
     * @return Builder instance
     */
    public GuiItemBuilder setLoreMiniMessage(List<String> miniMessageLore) {
        if (itemMeta != null) {
            List<Component> components = miniMessageLore.stream()
                    .map(MessageParser::parse) // Parse to Component, preserves gradients
                    .collect(Collectors.toList());
            itemMeta.lore(components); // Set as Component lore
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
     * Add single line ke lore dengan format support (Parses to Component).
     *
     * @param line Lore line to add
     * @return Builder instance
     */
    public GuiItemBuilder addLore(String line) {
        if (itemMeta != null) {
            List<Component> lore = itemMeta.hasLore() ? itemMeta.lore() : new java.util.ArrayList<>();
            if (lore == null) {
                lore = new java.util.ArrayList<>();
            } else {
                lore = new java.util.ArrayList<>(lore); // make mutable
            }
            lore.add(MessageParser.parse(line));
            itemMeta.lore(lore);
        }
        return this;
    }

    /**
     * Add single line ke lore as a Component to preserve formats
     *
     * @param line Lore line to add
     * @return Builder instance
     */
    public GuiItemBuilder addLore(Component line) {
        if (itemMeta != null) {
            List<Component> lore = itemMeta.hasLore() ? itemMeta.lore() : new java.util.ArrayList<>();
            if (lore == null) {
                lore = new java.util.ArrayList<>();
            } else {
                lore = new java.util.ArrayList<>(lore); // make mutable
            }
            lore.add(line);
            itemMeta.lore(lore);
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
