# MiniMessage Support Guide - PlayerAuctions

PlayerAuctions now supports **MiniMessage**, the modern text formatting system for Minecraft plugins! This guide will help you understand and use all available message formats.

## 📖 Table of Contents

1. [Overview](#overview)
2. [Supported Formats](#supported-formats)
3. [MiniMessage Examples](#minimessage-examples)
4. [Hex Color Examples](#hex-color-examples)
5. [RGB Color Examples](#rgb-color-examples)
6. [Legacy Color Codes](#legacy-color-codes)
7. [Mixing Formats](#mixing-formats)
8. [Placeholders](#placeholders)
9. [Best Practices](#best-practices)
10. [Migration Guide](#migration-guide)

---

## Overview

PlayerAuctions now includes a **comprehensive message parser** that automatically detects and processes multiple message formats:

- **MiniMessage** (Recommended) - Modern, powerful, and feature-rich
- **Hex Colors** (`&#RRGGBB`) - 16.7 million colors
- **RGB Colors** (`&rgb(R,G,B)`) - Same colors, different syntax
- **Legacy Codes** (`&a`, `&c`, etc.) - Backward compatible

The plugin **automatically detects** which format you're using, so you can mix and match formats in the same `messages.yml` file!

---

## Supported Formats

### 1. MiniMessage (Recommended)

MiniMessage is the most powerful and modern format. It supports:

- ✅ Colors (named and hex)
- ✅ Gradients
- ✅ Rainbow text
- ✅ Formatting (bold, italic, etc.)
- ✅ Hover tooltips
- ✅ Click actions
- ✅ And much more!

**Official Documentation**: https://docs.advntr.dev/minimessage/format.html

### 2. Hex Colors

Simple 6-digit hex color codes with `&#` prefix.

**Format**: `&#RRGGBB`

### 3. RGB Colors

RGB values (0-255) for each color channel.

**Format**: `&rgb(R,G,B)`

### 4. Legacy Color Codes

Traditional Minecraft color codes with `&` prefix.

**Format**: `&a`, `&c`, `&l`, etc.

---

## MiniMessage Examples

### Basic Colors

```yaml
# Named colors
message: "<red>This is red text</red>"
message: "<green>This is green text</green>"
message: "<gold>This is gold text</gold>"

# Hex colors
message: "<color:#FF0000>Custom red color</color>"
message: "<color:#00FF00>Custom green color</color>"
message: "<#FFD700>Short form hex</color>"
```

### Gradients

```yaml
# Two-color gradient
prefix: "<gradient:gold:yellow>[PlayerAuctions]</gradient>"

# Multi-color gradient
message: "<gradient:red:blue:green>Rainbow text!</gradient>"

# Gradient with hex colors
message: "<gradient:#FF0000:#00FF00>Custom gradient</gradient>"
```

### Rainbow Text

```yaml
# Animated rainbow effect
message: "<rainbow>This text cycles through colors!</rainbow>"

# Rainbow with custom phase
message: "<rainbow:!>Shifted rainbow</rainbow>"
message: "<rainbow:2>Faster rainbow</rainbow>"
```

### Formatting

```yaml
# Bold
message: "<bold>Bold text</bold>"

# Italic
message: "<italic>Italic text</italic>"

# Underlined
message: "<underlined>Underlined text</underlined>"

# Strikethrough
message: "<strikethrough>Strikethrough text</strikethrough>"

# Obfuscated (magic)
message: "<obfuscated>Magic text</obfuscated>"

# Combined formatting
message: "<bold><gradient:gold:yellow>Bold Gradient!</gradient></bold>"
```

### Hover Effects

```yaml
# Hover to show text
message: "<hover:show_text:'This is a tooltip!'>Hover over me</hover>"

# Multi-line tooltips
message: "<hover:show_text:'Line 1\nLine 2\nLine 3'>Hover here</hover>"

# Styled tooltips
message: "<hover:show_text:'<green>Click for info!'>Info button</hover>"
```

### Click Actions

```yaml
# Run command
message: "<click:run_command:'/ah'>Click to open auction house</click>"

# Suggest command
message: "<click:suggest_command:'/ah sell '>Click to sell</click>"

# Open URL
message: "<click:open_url:'https://example.com'>Visit website</click>"

# Copy to clipboard
message: "<click:copy_to_clipboard:'copied text'>Click to copy</click>"
```

### Complex Examples

```yaml
# Gradient + Bold + Hover + Click
auction:
  purchase-success: "<gradient:green:aqua><bold>Purchase Successful!</bold></gradient> <hover:show_text:'<gray>Item: <white>{item}</white><newline><gray>Price: <gold>{price}</gold>'><click:run_command:'/ah'>Click to buy more!</click></hover>"

# Rainbow + Italic
info:
  listed: "<rainbow><italic>Your item is now listed in the auction house!</italic></rainbow>"

# Multiple effects
prefix: "<gradient:gold:yellow><bold>[PlayerAuctions]</bold></gradient> <gray>|</gray> "
```

---

## Hex Color Examples

Hex colors give you access to **16.7 million colors**!

```yaml
# Basic hex colors
message: "&#FF0000Red text"
message: "&#00FF00Green text"
message: "&#0000FFBlue text"

# Multiple hex colors in one line
message: "&#FFD700Gold &#C0C0C0Silver &#CD7F32Bronze"

# Hex with legacy formatting
message: "&#FF0000&lBold Red Text"

# Common hex colors
pure-white: "&#FFFFFFWhite"
pure-black: "&#000000Black"
orange: "&#FF8C00Orange"
purple: "&#9932CCPurple"
pink: "&#FF69B4Pink"
sky-blue: "&#87CEEBSky Blue"
```

### Popular Hex Color Palettes

```yaml
# Material Design Colors
red-500: "&#F44336"
pink-500: "&#E91E63"
purple-500: "&#9C27B0"
blue-500: "&#2196F3"
green-500: "&#4CAF50"
yellow-500: "&#FFEB3B"
orange-500: "&#FF9800"

# Minecraft-inspired
minecraft-gold: "&#FFD700"
minecraft-aqua: "&#55FFFF"
minecraft-red: "&#FF5555"
minecraft-green: "&#55FF55"
minecraft-blue: "&#5555FF"
```

---

## RGB Color Examples

RGB colors are another way to specify custom colors using Red, Green, Blue values (0-255).

```yaml
# Basic RGB colors
message: "&rgb(255,0,0)Red text"
message: "&rgb(0,255,0)Green text"
message: "&rgb(0,0,255)Blue text"

# RGB with spaces (also supported)
message: "&rgb(255, 215, 0)Gold text"

# Common RGB colors
white: "&rgb(255,255,255)White"
black: "&rgb(0,0,0)Black"
orange: "&rgb(255,140,0)Orange"
purple: "&rgb(153,50,204)Purple"
pink: "&rgb(255,105,180)Pink"
```

---

## Legacy Color Codes

Traditional Minecraft color codes are fully supported for **backward compatibility**.

### Color Codes

| Code | Color | Example |
|------|-------|---------|
| `&0` | Black | `&0Black` |
| `&1` | Dark Blue | `&1Dark Blue` |
| `&2` | Dark Green | `&2Dark Green` |
| `&3` | Dark Aqua | `&3Dark Aqua` |
| `&4` | Dark Red | `&4Dark Red` |
| `&5` | Dark Purple | `&5Dark Purple` |
| `&6` | Gold | `&6Gold` |
| `&7` | Gray | `&7Gray` |
| `&8` | Dark Gray | `&8Dark Gray` |
| `&9` | Blue | `&9Blue` |
| `&a` | Green | `&aGreen` |
| `&b` | Aqua | `&bAqua` |
| `&c` | Red | `&cRed` |
| `&d` | Light Purple | `&dLight Purple` |
| `&e` | Yellow | `&eYellow` |
| `&f` | White | `&fWhite` |

### Format Codes

| Code | Format | Example |
|------|--------|---------|
| `&k` | Obfuscated | `&kMagic` |
| `&l` | Bold | `&lBold` |
| `&m` | Strikethrough | `&mStrike` |
| `&n` | Underline | `&nUnderline` |
| `&o` | Italic | `&oItalic` |
| `&r` | Reset | `&rReset all` |

```yaml
# Examples
message: "&a&lBold Green Text"
message: "&c&oItalic Red Text"
message: "&e&nUnderlined Yellow"
message: "&b&m&oStrikethrough Italic Aqua"
```

---

## Mixing Formats

You can **mix different formats** in the same file! The plugin auto-detects each format.

```yaml
# MiniMessage
prefix: "<gradient:gold:yellow>[PlayerAuctions]</gradient>"

# Hex colors
auction:
  sold: "&#00FF00Your item sold for &#FFD700{price}&#00FF00!"

# RGB colors
errors:
  no-permission: "&rgb(255,0,0)You don't have permission!"

# Legacy codes (backward compatible)
info:
  listed: "&aYour item is listed!"

# Mixed in one message
complex-message: "<gradient:red:blue>Gradient</gradient> &#FFD700Hex &aLegacy &rgb(255,0,255)RGB"
```

---

## Placeholders

All message formats support **placeholders**. Use either `{placeholder}` or `%placeholder%` syntax.

```yaml
# Both formats work
message: "Hello {player_name}!"
message: "Hello %player_name%!"

# With colors
message: "<gold>Welcome, <yellow>{player_name}</yellow>!</gold>"
message: "&#FFD700Welcome, &#FFFF00{player_name}&#FFD700!"

# Multiple placeholders
auction:
  sold: "<green>Your <white>{item}</white> sold for <gold>{price}</gold>!</green>"
  
# Common placeholders
{player_name}    # Player's name
{balance}        # Player's balance
{price}          # Item price
{item}           # Item name
{seller}         # Seller name
{buyer}          # Buyer name
{amount}         # Transaction amount
{time_left}      # Time remaining
{page}           # Current page
{total_pages}    # Total pages
```

---

## Best Practices

### 1. Choose the Right Format

- **MiniMessage**: Best for modern, feature-rich messages with gradients, hover, click actions
- **Hex/RGB**: Best for precise color control without advanced features
- **Legacy**: Best for simple messages and backward compatibility

### 2. Readability

```yaml
# ✅ Good - Readable and clear
message: "<green>Success! <yellow>Item sold.</yellow></green>"

# ❌ Avoid - Too complex, hard to read
message: "<gradient:red:blue:green:yellow:orange><rainbow><bold><italic>Text</italic></bold></rainbow></gradient>"
```

### 3. Performance

- Cache frequently-used messages
- Avoid nested gradients (can be expensive)
- Use simple formats for high-frequency messages

### 4. Consistency

```yaml
# Choose a style and stick with it
prefix: "<gradient:gold:yellow>[PlayerAuctions]</gradient>"
success-messages: "<green>...</green>"
error-messages: "<red>...</red>"
info-messages: "<aqua>...</aqua>"
```

### 5. Testing

Always test your messages in-game! What looks good in YAML might look different in Minecraft.

---

## Migration Guide

### Migrating from Legacy to MiniMessage

#### Before (Legacy)
```yaml
prefix: "&7[&6PlayerAuctions&7] "
auction:
  sold: "&aYour item &e{item}&a sold for &e{price}&a!"
errors:
  no-permission: "&cYou don't have permission!"
```

#### After (MiniMessage)
```yaml
prefix: "<gradient:gold:yellow>[PlayerAuctions]</gradient> "
auction:
  sold: "<green>Your item <yellow>{item}</yellow> sold for <gold>{price}</gold>!</green>"
errors:
  no-permission: "<red>You don't have permission!</red>"
```

### Gradual Migration

You don't have to convert everything at once! Mix formats:

```yaml
# Keep legacy messages
prefix: "&7[&6PlayerAuctions&7] "  # Legacy - still works

# Add MiniMessage for new messages
auction:
  sold: "<gradient:green:aqua>Item sold!</gradient>"  # MiniMessage - fancy!

# Use hex for specific colors
special-message: "&#FF69B4Pink text with hex"  # Hex - precise color
```

### Conversion Script

For bulk conversion, use this pattern:

| Legacy | MiniMessage Equivalent |
|--------|----------------------|
| `&a` | `<green>` |
| `&c` | `<red>` |
| `&e` | `<yellow>` |
| `&6` | `<gold>` |
| `&l` | `<bold>` |
| `&o` | `<italic>` |
| `&n` | `<underlined>` |
| `&m` | `<strikethrough>` |
| `&r` | `<reset>` |

---

## Advanced Examples

### Complete Message Set Example

```yaml
# Prefix with gradient
prefix: "<gradient:gold:yellow><bold>[PlayerAuctions]</bold></gradient> <gray>»</gray> "

# Success messages with hover
auction:
  purchase-success: "<gradient:green:aqua><bold>Purchase Successful!</bold></gradient> <hover:show_text:'<gray>Item: <white>{item}</white><newline><gray>Seller: <yellow>{seller}</yellow><newline><gray>Price: <gold>{price}</gold>'><green>View details</green></hover>"
  
  sold: "<green>✔</green> <gradient:gold:yellow>Sale Complete!</gradient> <gray>Your <white>{item}</white> sold for <gold>{price}</gold>!</gray>"
  
  listed: "<green>✓</green> <white>{item}</white> <gray>listed for</gray> <gold>{price}</gold> <gray>•</gray> <yellow>{duration}</yellow>"

# Error messages with formatting
errors:
  no-permission: "<red><bold>✘ Permission Denied</bold></red> <gray>Contact staff for assistance.</gray>"
  
  insufficient-funds: "<red>Insufficient Funds!</red> <gray>Need:</gray> <gold>{required}</gold> <gray>Have:</gray> <gold>{current}</gold>"
  
  auction-not-found: "<red>Auction not found!</red> <gray>It may have expired or been removed.</gray>"

# Info messages with click actions
info:
  help: "<click:run_command:'/ah help'><hover:show_text:'<green>Click for help!'><aqua>Need help? Click here!</aqua></hover></click>"
  
  search-prompt: "<aqua>Type your search query in chat...</aqua> <gray>or</gray> <click:suggest_command:'/ah search '><yellow>Click to search</yellow></click>"

# GUI item lore with multiple effects
gui:
  item-lore:
    - "<gray>Seller:</gray> <yellow>{seller}</yellow>"
    - ""
    - "<gold>Price:</gold> <green>{price}</green>"
    - "<gray>Time left:</gray> <aqua>{time_left}</aqua>"
    - ""
    - "<gradient:green:aqua>» Click to purchase «</gradient>"
```

---

## Troubleshooting

### Message not showing colors?

1. **Check syntax**: MiniMessage tags must be closed properly
   ```yaml
   # ❌ Wrong
   message: "<red>Text"
   
   # ✅ Correct
   message: "<red>Text</red>"
   ```

2. **Hex format**: Must use `&#` prefix
   ```yaml
   # ❌ Wrong
   message: "#FF0000Text"
   
   # ✅ Correct
   message: "&#FF0000Text"
   ```

3. **RGB format**: Must use `&rgb()` format
   ```yaml
   # ❌ Wrong
   message: "rgb(255,0,0)Text"
   
   # ✅ Correct
   message: "&rgb(255,0,0)Text"
   ```

### Placeholders not replaced?

Use the correct format: `{placeholder}` or `%placeholder%`

```yaml
# ✅ Both work
message: "Hello {player_name}!"
message: "Hello %player_name%!"
```

### Tags showing literally in game?

This usually means MiniMessage parsing failed. Check for:
- Malformed tags
- Unclosed tags
- Invalid tag names

The plugin will automatically fall back to legacy parsing if MiniMessage fails.

---

## Resources

- **MiniMessage Documentation**: https://docs.advntr.dev/minimessage/
- **Adventure Documentation**: https://docs.advntr.dev/
- **Color Picker**: https://www.google.com/search?q=color+picker
- **Hex to RGB Converter**: https://www.rapidtables.com/convert/color/hex-to-rgb.html

---

## Support

For issues or questions about MiniMessage support:
1. Check this guide
2. Test your messages in-game
3. Check console for errors
4. Contact plugin support

---

**Happy formatting! 🎨**
