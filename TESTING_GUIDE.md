# MiniMessage Testing Guide - PlayerAuctions

This guide will help you test the new MiniMessage support in PlayerAuctions.

---

## 🧪 Quick Test Checklist

### 1. Installation Test
- [ ] Place `PlayerAuctions-2.0.0-Modern.jar` in `plugins/` folder
- [ ] Start server
- [ ] Check console for "MessageManager initialized with Adventure API support"
- [ ] No errors during startup

### 2. Legacy Compatibility Test
- [ ] Existing messages still display correctly
- [ ] All legacy color codes (`&a`, `&c`, etc.) work
- [ ] Placeholders are replaced properly
- [ ] No visual changes to existing messages

### 3. MiniMessage Test
- [ ] Edit `messages.yml` with MiniMessage format
- [ ] Reload plugin (`/ah reload`)
- [ ] Messages display with new formatting
- [ ] Gradients render properly

### 4. Hex Color Test
- [ ] Add hex color message (`&#FF0000`)
- [ ] Reload and test
- [ ] Color displays correctly

### 5. RGB Color Test
- [ ] Add RGB color message (`&rgb(255,0,0)`)
- [ ] Reload and test
- [ ] Color displays correctly

### 6. GUI Test
- [ ] Open auction house (`/ah`)
- [ ] Item names and lore display correctly
- [ ] Control buttons show proper colors
- [ ] No formatting issues

---

## 📝 Test Messages

Add these to your `messages.yml` for testing:

```yaml
# Test section - add at the bottom of messages.yml
testing:
  # Legacy test (should still work)
  legacy-test: "&aGreen &cRed &eYellow &l&nBold Underline"
  
  # MiniMessage gradient test
  gradient-test: "<gradient:red:blue>This is a gradient!</gradient>"
  
  # MiniMessage color test
  color-test: "<red>Red</red> <green>Green</green> <gold>Gold</gold>"
  
  # Hex color test
  hex-test: "&#FF0000Red &#00FF00Green &#0000FFBlue"
  
  # RGB color test
  rgb-test: "&rgb(255,0,0)Red &rgb(0,255,0)Green &rgb(0,0,255)Blue"
  
  # Mixed format test
  mixed-test: "<gradient:gold:yellow>Gradient</gradient> &#FFD700Hex &aLegacy &rgb(255,0,255)RGB"
  
  # Complex MiniMessage test
  complex-test: "<gradient:green:aqua><bold>Bold Gradient</bold></gradient> <hover:show_text:'<green>This is a tooltip!'>Hover here</hover>"
  
  # Placeholder test
  placeholder-test: "<gold>Hello {player_name}!</gold> Your balance: <green>{balance}</green>"
```

---

## 🎮 In-Game Testing Commands

### Method 1: Use /ah command (if implemented)
```
/ah reload
```

### Method 2: Restart server
```
/stop
(Start server again)
```

### Method 3: Test via console
Use commands that display messages from `messages.yml`

---

## ✅ Expected Results

### Legacy Test
```
Expected: Green Red Yellow Bold Underline
Should look like traditional Minecraft colors
```

### Gradient Test
```
Expected: Smooth color transition from red to blue
"This is a gradient!" should blend colors
```

### Color Test
```
Expected: Three colored words
Red (red) Green (green) Gold (gold/yellow)
```

### Hex Test
```
Expected: Pure colors
Red Green Blue in exact hex values
```

### RGB Test
```
Expected: Same as hex test
Red Green Blue (RGB values converted to display)
```

### Mixed Test
```
Expected: All formats working together
Gradient, Hex, Legacy, and RGB all visible
```

### Complex Test
```
Expected: 
- "Bold Gradient" in bold with green-to-aqua gradient
- "Hover here" shows tooltip when mouse over
```

---

## 🔍 Troubleshooting

### Issue: Colors not showing

**Check:**
1. Minecraft client supports RGB colors (1.16+)
2. No typos in color codes
3. MiniMessage tags are properly closed
4. Adventure API loaded successfully

**Solution:**
- Check console for errors
- Verify messages.yml syntax
- Test with legacy codes first

### Issue: Gradients not working

**Check:**
1. Client version supports RGB (1.16+)
2. MiniMessage tags are correct
3. At least 2 colors specified

**Solution:**
```yaml
# ✅ Correct
message: "<gradient:red:blue>Text</gradient>"

# ❌ Wrong
message: "<gradient:red>Text</gradient>"  # Need 2+ colors
message: "<gradient:red:blue>Text"        # Missing closing tag
```

### Issue: Hex colors not showing

**Check:**
1. Format is `&#RRGGBB` (6 hex digits)
2. No spaces in hex code
3. Valid hex characters (0-9, A-F)

**Solution:**
```yaml
# ✅ Correct
message: "&#FF0000Text"

# ❌ Wrong
message: "#FF0000Text"      # Missing &
message: "&#FF00Text"       # Only 4 digits
message: "&#GG0000Text"     # Invalid hex (GG)
```

### Issue: Placeholders not replaced

**Check:**
1. Placeholder format: `{placeholder}` or `%placeholder%`
2. Placeholder name is correct
3. Context/data is provided

**Solution:**
```yaml
# ✅ Both work
message: "Hello {player_name}!"
message: "Hello %player_name%!"

# ❌ Wrong
message: "Hello $player_name$!"  # Wrong delimiters
```

---

## 🎯 Advanced Testing

### Test 1: Performance Test
```yaml
# Create 100 messages with different formats
# Reload plugin multiple times
# Check server performance
```

### Test 2: Edge Cases
```yaml
# Empty messages
test-empty: ""

# Very long messages
test-long: "<gradient:red:blue>Very long text...</gradient> (500+ chars)"

# Nested tags
test-nested: "<bold><italic><gradient:red:blue>Nested</gradient></italic></bold>"

# Special characters
test-special: "Test with <brackets> and {braces} and %percents%"
```

### Test 3: GUI Integration
```yaml
gui:
  test-items:
    name: "<gradient:gold:yellow>Test Item</gradient>"
    lore:
      - "<gray>Lore line 1</gray>"
      - "&#FF0000Hex color line"
      - "&rgb(0,255,0)RGB color line"
      - "&aLegacy color line"
```

---

## 📊 Test Results Template

Copy this to track your testing:

```
# PlayerAuctions MiniMessage Testing

Date: _______________
Server Version: _______________
Plugin Version: 2.0.0

## Installation
- [ ] Plugin loads without errors
- [ ] MessageManager initialized
- [ ] No startup exceptions

## Format Tests
- [ ] Legacy codes work
- [ ] MiniMessage gradient works
- [ ] MiniMessage colors work
- [ ] Hex colors work
- [ ] RGB colors work
- [ ] Mixed formats work

## Functionality Tests
- [ ] Messages display in chat
- [ ] GUI items show correctly
- [ ] Placeholders replaced
- [ ] Command outputs work
- [ ] Error messages formatted

## Performance
- [ ] No lag when displaying messages
- [ ] Reload time acceptable
- [ ] No memory issues

## Issues Found
_______________________________________________
_______________________________________________
_______________________________________________

## Additional Notes
_______________________________________________
_______________________________________________
_______________________________________________
```

---

## 🔗 Testing Resources

### Color Testing Tools
- **Color Picker**: https://www.google.com/search?q=color+picker
- **Hex to RGB**: https://www.rapidtables.com/convert/color/hex-to-rgb.html
- **Gradient Generator**: https://www.cssportal.com/css-gradient-generator/

### MiniMessage Testing
- **MiniMessage Web UI**: https://webui.advntr.dev/
  - Test messages before adding to config
  - Preview formatting
  - Validate syntax

### Minecraft Color Testing
- **Test Server**: Use a test server before production
- **Creative Mode**: Easier to test repeatedly
- **Console Commands**: Test from console first

---

## 💡 Testing Tips

1. **Start Simple**: Test legacy codes first, then move to advanced formats
2. **Use Test Messages**: Add temporary test section to messages.yml
3. **Check Console**: Always check console for errors
4. **Incremental Testing**: Test one format at a time
5. **Document Issues**: Note any problems for bug reports
6. **Version Check**: Ensure client supports RGB (1.16+)
7. **Backup First**: Keep backup of original messages.yml

---

## 🆘 Getting Help

If you encounter issues:

1. **Check Console Logs**: Look for error messages
2. **Review Documentation**: Read MINIMESSAGE_GUIDE.md
3. **Verify Syntax**: Use MiniMessage Web UI
4. **Test Isolation**: Test each format separately
5. **Contact Support**: Provide error logs and test results

---

## ✨ Example Test Session

```yaml
# 1. Add test message
testing:
  hello: "<gradient:gold:yellow>Hello {player_name}!</gradient>"

# 2. Reload plugin
/ah reload

# 3. Trigger message (e.g., via command)
# Expected: "Hello Steve!" in gold-to-yellow gradient

# 4. If works, proceed to next test
testing:
  complex: "<hover:show_text:'<green>Tooltip!'>Hover me</hover>"

# 5. Continue testing each format
```

---

## 🎉 Success Criteria

Your testing is successful if:

✅ All legacy codes still work  
✅ New MiniMessage formats display correctly  
✅ No console errors  
✅ No performance issues  
✅ GUI items formatted properly  
✅ Placeholders replaced correctly  
✅ Mixed formats work together  

---

**Happy Testing! 🧪**
