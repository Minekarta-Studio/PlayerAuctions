package com.minekarta.playerauction.gui.model;

public enum AuctionCategory {
    ALL, WEAPONS, ARMOR, BLOCKS, MISC;

    /**
     * Checks if this category matches the given item type
     * @param itemType The item type to check
     * @return true if this category matches the item type, false otherwise
     */
    public boolean matches(String itemType) {
        if (this == ALL) {
            return true;
        }

        switch (this) {
            case WEAPONS:
                return itemType.contains("SWORD") || itemType.contains("AXE") || itemType.contains("PICKAXE") ||
                       itemType.contains("SHOVEL") || itemType.contains("HOE") || itemType.contains("TRIDENT") ||
                       itemType.contains("BOW") || itemType.contains("CROSSBOW") || itemType.contains("ARROW");
            case ARMOR:
                return itemType.contains("HELMET") || itemType.contains("CHESTPLATE") ||
                       itemType.contains("LEGGINGS") || itemType.contains("BOOTS");
            case BLOCKS:
                return itemType.contains("_ORE") || itemType.contains("_BLOCK") || itemType.contains("_LOG") ||
                       itemType.contains("_PLANKS") || itemType.contains("_SLAB") || itemType.contains("_STAIRS") ||
                       itemType.contains("_FENCE") || itemType.contains("_DOOR") || itemType.contains("_TRAPDOOR") ||
                       itemType.contains("_PRESSURE_PLATE") || itemType.contains("_BUTTON") || itemType.contains("STONE") ||
                       itemType.contains("GRASS") || itemType.contains("DIRT") || itemType.contains("COBBLESTONE") ||
                       itemType.contains("GRAVEL") || itemType.contains("SAND") || itemType.contains("GLASS") ||
                       itemType.contains("WOOL") || itemType.contains("CARPET");
            case MISC:
                // For MISC category, return true if the item doesn't match other specific categories
                return !(itemType.contains("SWORD") || itemType.contains("AXE") || itemType.contains("PICKAXE") ||
                         itemType.contains("SHOVEL") || itemType.contains("HOE") || itemType.contains("TRIDENT") ||
                         itemType.contains("BOW") || itemType.contains("CROSSBOW") || itemType.contains("ARROW") ||
                         itemType.contains("HELMET") || itemType.contains("CHESTPLATE") ||
                         itemType.contains("LEGGINGS") || itemType.contains("BOOTS") ||
                         itemType.contains("_ORE") || itemType.contains("_BLOCK") || itemType.contains("_LOG") ||
                         itemType.contains("_PLANKS") || itemType.contains("_SLAB") || itemType.contains("_STAIRS") ||
                         itemType.contains("_FENCE") || itemType.contains("_DOOR") || itemType.contains("_TRAPDOOR") ||
                         itemType.contains("_PRESSURE_PLATE") || itemType.contains("_BUTTON") || itemType.contains("STONE") ||
                         itemType.contains("GRASS") || itemType.contains("DIRT") || itemType.contains("COBBLESTONE") ||
                         itemType.contains("GRAVEL") || itemType.contains("SAND") || itemType.contains("GLASS") ||
                         itemType.contains("WOOL") || itemType.contains("CARPET"));
            default:
                return false; // Should not reach here
        }
    }

    /**
     * Determines the category for a given item type
     * @param itemType The item type to categorize
     * @return The appropriate category for the item type
     */
    public static AuctionCategory getCategoryForItemType(String itemType) {
        if (itemType.contains("SWORD") || itemType.contains("AXE") || itemType.contains("PICKAXE") ||
            itemType.contains("SHOVEL") || itemType.contains("HOE") || itemType.contains("TRIDENT") ||
            itemType.contains("BOW") || itemType.contains("CROSSBOW") || itemType.contains("ARROW")) {
            return WEAPONS;
        } else if (itemType.contains("HELMET") || itemType.contains("CHESTPLATE") ||
                   itemType.contains("LEGGINGS") || itemType.contains("BOOTS")) {
            return ARMOR;
        } else if (itemType.contains("_ORE") || itemType.contains("_BLOCK") || itemType.contains("_LOG") ||
                   itemType.contains("_PLANKS") || itemType.contains("_SLAB") || itemType.contains("_STAIRS") ||
                   itemType.contains("_FENCE") || itemType.contains("_DOOR") || itemType.contains("_TRAPDOOR") ||
                   itemType.contains("_PRESSURE_PLATE") || itemType.contains("_BUTTON") || itemType.contains("STONE") ||
                   itemType.contains("GRASS") || itemType.contains("DIRT") || itemType.contains("COBBLESTONE") ||
                   itemType.contains("GRAVEL") || itemType.contains("SAND") || itemType.contains("GLASS") ||
                   itemType.contains("WOOL") || itemType.contains("CARPET")) {
            return BLOCKS;
        } else {
            return MISC;
        }
    }
}

