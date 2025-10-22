# StoneSettings Agent Tool Definitions

This document defines the programmatic API for controlling StoneSettings through broadcast intents. AI agents can use these tools to control device settings without user interaction.

## Overview

StoneSettings exposes its functionality through Android BroadcastIntents. Each tool corresponds to a broadcast action that triggers the `SettingsControlReceiver`.

**Base Package**: `com.stoneos.settings`

---

## set_wifi_state

**Description**: Enables or disables the device's Wi-Fi radio.

**Parameters**:
- `enabled` (boolean, required): Set to `true` to enable Wi-Fi, `false` to disable it.

**Broadcast Action**: `com.stoneos.settings.SET_WIFI_STATE`

**Example Usage**:
```bash
# Enable Wi-Fi
adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled true

# Disable Wi-Fi
adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled false
```

**Permissions Required**:
- `android.permission.CHANGE_WIFI_STATE`

**Success Indicators**:
- Log message: "Wi-Fi enabled" or "Wi-Fi disabled"
- Toast notification displayed to user

---

## set_bluetooth_state

**Description**: Enables or disables the device's Bluetooth adapter.

**Parameters**:
- `enabled` (boolean, required): Set to `true` to enable Bluetooth, `false` to disable it.

**Broadcast Action**: `com.stoneos.settings.SET_BLUETOOTH_STATE`

**Example Usage**:
```bash
# Enable Bluetooth
adb shell am broadcast -a com.stoneos.settings.SET_BLUETOOTH_STATE --ez enabled true

# Disable Bluetooth
adb shell am broadcast -a com.stoneos.settings.SET_BLUETOOTH_STATE --ez enabled false
```

**Permissions Required**:
- `android.permission.BLUETOOTH_ADMIN`
- `android.permission.BLUETOOTH_CONNECT` (Android 12+)

**Success Indicators**:
- Log message: "Bluetooth enabled" or "Bluetooth disabled"
- Toast notification displayed to user

**Note**: Bluetooth state changes may take a few seconds to take effect.

---

## set_screen_brightness

**Description**: Adjusts the screen brightness level and disables adaptive brightness.

**Parameters**:
- `level` (integer, required): The brightness level, from 0 (dimmest) to 255 (brightest).

**Broadcast Action**: `com.stoneos.settings.SET_BRIGHTNESS`

**Example Usage**:
```bash
# Set brightness to minimum (0%)
adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 0

# Set brightness to 50%
adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 128

# Set brightness to maximum (100%)
adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 255
```

**Permissions Required**:
- `android.permission.WRITE_SETTINGS`

**Success Indicators**:
- Log message: "Brightness set to: {level}"
- Toast notification with percentage (e.g., "Brightness set to 50%")

**Behavior**:
- Automatically switches to manual brightness mode (disables adaptive brightness)
- Invalid levels (< 0 or > 255) are rejected with an error log

---

## Error Handling

All tools log errors to Android logcat with the tag `StoneSettingsAPI`:

```bash
# Monitor API logs
adb logcat -s StoneSettingsAPI:*
```

**Common error messages**:
- `"missing 'enabled' extra"` - Boolean parameter not provided
- `"missing 'level' extra"` - Integer parameter not provided
- `"invalid level X (must be 0-255)"` - Brightness value out of range
- `"WifiManager not available"` - Wi-Fi hardware not accessible
- `"BluetoothAdapter not available"` - Bluetooth hardware not accessible

---

## Integration with AI Agents

### LiveKit Agent Example

```typescript
// Example: Implementing set_wifi_state in a LiveKit agent
async function setWifiState(enabled: boolean): Promise<void> {
  const command = `am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled ${enabled}`;

  // Execute via adb or Android shell
  await executeShellCommand(command);

  // Wait for state change
  await new Promise(resolve => setTimeout(resolve, 2000));
}

// Usage
await setWifiState(true);  // Turn on Wi-Fi
```

### MCP Server Example

```javascript
// Example: Exposing as MCP tool
const tools = [
  {
    name: "set_wifi_state",
    description: "Enables or disables the device's Wi-Fi",
    parameters: {
      type: "object",
      properties: {
        enabled: { type: "boolean", description: "true to enable, false to disable" }
      },
      required: ["enabled"]
    },
    execute: async ({ enabled }) => {
      const result = await execAsync(
        `adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled ${enabled}`
      );
      return { success: true, message: `Wi-Fi ${enabled ? 'enabled' : 'disabled'}` };
    }
  }
];
```

---

## Testing

### Manual Testing via ADB

1. **Test Wi-Fi control**:
   ```bash
   adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled true
   adb shell dumpsys wifi | grep "Wi-Fi is"
   ```

2. **Test Bluetooth control**:
   ```bash
   adb shell am broadcast -a com.stoneos.settings.SET_BLUETOOTH_STATE --ez enabled true
   adb shell dumpsys bluetooth_manager | grep "enabled"
   ```

3. **Test Brightness control**:
   ```bash
   adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 128
   adb shell settings get system screen_brightness
   ```

### Automated Testing Script

```bash
#!/bin/bash
# test_settings_api.sh

echo "Testing StoneSettings API..."

# Test Wi-Fi
echo "1. Testing Wi-Fi on"
adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled true
sleep 2

echo "2. Testing Wi-Fi off"
adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled false
sleep 2

# Test Brightness
echo "3. Testing brightness at 50%"
adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 128
sleep 1

echo "4. Testing brightness at 100%"
adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 255
sleep 1

# Test Bluetooth
echo "5. Testing Bluetooth on"
adb shell am broadcast -a com.stoneos.settings.SET_BLUETOOTH_STATE --ez enabled true
sleep 2

echo "6. Testing Bluetooth off"
adb shell am broadcast -a com.stoneos.settings.SET_BLUETOOTH_STATE --ez enabled false

echo "Done! Check logcat for results:"
echo "adb logcat -s StoneSettingsAPI:*"
```

---

## Future Enhancements

Additional tools that could be implemented:

- `set_volume` - Control system volume levels
- `set_airplane_mode` - Enable/disable airplane mode
- `set_auto_rotate` - Control screen rotation
- `set_do_not_disturb` - Control Do Not Disturb mode
- `set_mobile_data` - Enable/disable mobile data
- `get_battery_level` - Query current battery percentage
- `get_network_info` - Query current network connection details

---

## Architecture Notes

**Why BroadcastReceiver?**

Using BroadcastIntents as the API mechanism provides:
1. **Simplicity**: No need for AIDL or complex IPC
2. **Security**: Android's permission system controls access
3. **Debuggability**: Easy to test via `adb` command line
4. **Language-agnostic**: Any process can send broadcasts
5. **Asynchronous**: Non-blocking for both sender and receiver

**System App Requirement**:

StoneSettings must be a privileged system app because:
- Controlling Wi-Fi/Bluetooth requires system permissions
- Writing to `Settings.System` requires `WRITE_SETTINGS` or system UID
- BroadcastReceiver must be exported but protected by permissions

---

## License

Part of StoneOS - a minimalist, AI-augmented Android ROM.
