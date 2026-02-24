package com.minekarta.playerauction.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive message parser with support for MiniMessage, Hex colors, RGB,
 * and Legacy color codes.
 *
 * Supported formats:
 * - MiniMessage: <gradient:red:blue>Text</gradient>,
 * <color:#FF0000>Text</color>
 * - Hex Colors: &#FF0000Text
 * - RGB Colors: &rgb(255,0,0)Text
 * - Legacy: &a, &b, &c, etc.
 *
 * The parser auto-detects the format and applies appropriate parsing logic.
 */
public class MessageParser {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.legacySection();

    // Pattern untuk detect hex color (#RRGGBB atau &#RRGGBB)
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern RGB_PATTERN = Pattern
            .compile("&rgb\\(\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\s*,\\s*([0-9]{1,3})\\s*\\)");

    // Pattern untuk detect MiniMessage tags
    private static final Pattern MINI_MESSAGE_TAG_PATTERN = Pattern.compile("<[^>]+>");

    /**
     * Parse message dengan auto-detect format type.
     * Priority: MiniMessage > Hex/RGB > Legacy
     *
     * @param message Raw message string
     * @return Parsed Component
     */
    public static Component parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        Component parsed;
        // Detect format type and parse accordingly
        if (isMiniMessage(message)) {
            parsed = parseMiniMessage(message);
        } else if (hasHexOrRgb(message)) {
            parsed = parseHexAndRgb(message);
        } else {
            parsed = parseLegacy(message);
        }

        // Remove default italic formatting (especially for item names and lore)
        return Component.empty().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)
                .append(parsed);
    }

    /**
     * Parse message dan convert ke legacy format string (untuk backward
     * compatibility).
     *
     * @param message Raw message string
     * @return Legacy formatted string dengan § codes
     */
    public static String parseToLegacy(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Component component = parse(message);
        return SECTION_SERIALIZER.serialize(component);
    }

    /**
     * Parse MiniMessage format.
     * Contoh: <gradient:red:blue>Text</gradient>, <color:#ff0000>Text</color>
     *
     * @param message MiniMessage formatted string
     * @return Parsed Component
     */
    public static Component parseMiniMessage(String message) {
        try {
            // First, convert any legacy codes to MiniMessage format
            String processed = convertLegacyToMiniMessage(message);
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            // Fallback ke legacy jika parsing gagal
            return parseLegacy(message);
        }
    }

    /**
     * Parse Hex dan RGB color codes dan convert ke MiniMessage.
     * Contoh: &#FF0000Text, &rgb(255,0,0)Text
     *
     * @param message Message with hex or RGB codes
     * @return Parsed Component
     */
    public static Component parseHexAndRgb(String message) {
        try {
            // Convert RGB to Hex first
            String processed = convertRgbToHex(message);

            // Convert Hex to MiniMessage format
            processed = convertHexToMiniMessage(processed);

            // Convert legacy codes that might still exist
            processed = convertLegacyToMiniMessage(processed);

            // Parse dengan MiniMessage
            return MINI_MESSAGE.deserialize(processed);
        } catch (Exception e) {
            return parseLegacy(message);
        }
    }

    /**
     * Parse legacy color codes (&a, &b, etc).
     *
     * @param message Legacy formatted message
     * @return Parsed Component
     */
    public static Component parseLegacy(String message) {
        String translated = ChatColor.translateAlternateColorCodes('&', message);
        return LEGACY_SERIALIZER.deserialize(translated);
    }

    /**
     * Convert RGB format ke Hex format.
     * &rgb(255,0,0) -> &#FF0000
     *
     * @param message Message with RGB codes
     * @return Message with hex codes
     */
    private static String convertRgbToHex(String message) {
        Matcher matcher = RGB_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            try {
                int r = Integer.parseInt(matcher.group(1).trim());
                int g = Integer.parseInt(matcher.group(2).trim());
                int b = Integer.parseInt(matcher.group(3).trim());

                // Validate RGB values (0-255)
                if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                    String hex = String.format("%02X%02X%02X", r, g, b);
                    matcher.appendReplacement(buffer, "&#" + hex);
                } else {
                    // Keep original if values are out of range
                    matcher.appendReplacement(buffer, matcher.group(0));
                }
            } catch (NumberFormatException e) {
                // Keep original if parsing fails
                matcher.appendReplacement(buffer, matcher.group(0));
            }
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Convert Hex format ke MiniMessage format.
     * &#FF0000 -> <color:#FF0000>
     *
     * @param message Message with hex codes
     * @return Message with MiniMessage color tags
     */
    private static String convertHexToMiniMessage(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, "<color:#" + hex + ">");
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    /**
     * Convert legacy color codes ke MiniMessage format (hanya untuk codes yang
     * tersisa).
     *
     * @param message Message with legacy codes
     * @return Message with MiniMessage tags
     */
    private static String convertLegacyToMiniMessage(String message) {
        // Convert common legacy codes to MiniMessage
        String result = message;

        // Only convert if not already in MiniMessage format
        if (!isMiniMessage(result)) {
            // Color codes
            result = result.replace("&0", "<black>");
            result = result.replace("&1", "<dark_blue>");
            result = result.replace("&2", "<dark_green>");
            result = result.replace("&3", "<dark_aqua>");
            result = result.replace("&4", "<dark_red>");
            result = result.replace("&5", "<dark_purple>");
            result = result.replace("&6", "<gold>");
            result = result.replace("&7", "<gray>");
            result = result.replace("&8", "<dark_gray>");
            result = result.replace("&9", "<blue>");
            result = result.replace("&a", "<green>");
            result = result.replace("&b", "<aqua>");
            result = result.replace("&c", "<red>");
            result = result.replace("&d", "<light_purple>");
            result = result.replace("&e", "<yellow>");
            result = result.replace("&f", "<white>");

            // Format codes
            result = result.replace("&k", "<obfuscated>");
            result = result.replace("&l", "<bold>");
            result = result.replace("&m", "<strikethrough>");
            result = result.replace("&n", "<underlined>");
            result = result.replace("&o", "<italic>");
            result = result.replace("&r", "<reset>");
        }

        return result;
    }

    /**
     * Detect apakah message menggunakan MiniMessage format.
     *
     * @param message Message to check
     * @return true jika menggunakan MiniMessage
     */
    private static boolean isMiniMessage(String message) {
        if (!message.contains("<") || !message.contains(">")) {
            return false;
        }

        // Check for common MiniMessage tags (including simple colors and hex codes)
        return message.contains("</") ||
                message.contains("<gradient") ||
                message.contains("<rainbow") ||
                message.contains("<color:") ||
                message.matches(".*<#[0-9a-fA-F]{6}>.*") ||
                message.matches(".*<[a-z_]+>.*"); // Captures simple tags like <red>, <bold>, <gold>
    }

    /**
     * Detect apakah message menggunakan Hex atau RGB.
     *
     * @param message Message to check
     * @return true jika menggunakan hex atau RGB
     */
    private static boolean hasHexOrRgb(String message) {
        return HEX_PATTERN.matcher(message).find() ||
                RGB_PATTERN.matcher(message).find();
    }

    /**
     * Convert Component kembali ke plain text (untuk logging).
     *
     * @param component Component to convert
     * @return Plain text string
     */
    public static String toPlainText(Component component) {
        return SECTION_SERIALIZER.serialize(component);
    }

    /**
     * Strip all color codes and formatting dari message.
     *
     * @param message Message to strip
     * @return Plain text without colors
     */
    public static String stripColors(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Component component = parse(message);
        return component.toString();
    }
}
