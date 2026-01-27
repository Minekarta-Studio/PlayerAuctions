package com.minekarta.playerauction.util;

import java.util.HashMap;
import java.util.Map;

/**
 * A context for managing placeholder replacements.
 * This class holds key-value pairs of placeholders and their replacement values.
 */
public class PlaceholderContext {
    private final Map<String, String> placeholders = new HashMap<>();

    /**
     * Adds a placeholder and its replacement value.
     *
     * @param placeholder The placeholder key (without curly braces)
     * @param value The value to replace the placeholder with
     * @return This context for method chaining
     */
    public PlaceholderContext addPlaceholder(String placeholder, Object value) {
        placeholders.put("{" + placeholder + "}", value != null ? value.toString() : "");
        return this;
    }

    /**
     * Adds multiple placeholders at once.
     *
     * @param placeholdersMap A map of placeholder keys (without curly braces) to their values
     * @return This context for method chaining
     */
    public PlaceholderContext addPlaceholders(Map<String, Object> placeholdersMap) {
        for (Map.Entry<String, Object> entry : placeholdersMap.entrySet()) {
            addPlaceholder(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Gets the replacement value for a placeholder.
     *
     * @param placeholder The placeholder key (without curly braces)
     * @return The replacement value, or null if not found
     */
    public String getPlaceholderValue(String placeholder) {
        return placeholders.get("{" + placeholder + "}");
    }

    /**
     * Checks if a placeholder exists in this context.
     *
     * @param placeholder The placeholder key (without curly braces)
     * @return True if the placeholder exists, false otherwise
     */
    public boolean hasPlaceholder(String placeholder) {
        return placeholders.containsKey("{" + placeholder + "}");
    }

    /**
     * Gets all placeholders in this context.
     *
     * @return A copy of the placeholders map
     */
    public Map<String, String> getAllPlaceholders() {
        return new HashMap<>(placeholders);
    }

    /**
     * Applies all placeholders in this context to the given text.
     *
     * @param text The text to replace placeholders in
     * @return The text with all placeholders replaced
     */
    public String applyTo(String text) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
