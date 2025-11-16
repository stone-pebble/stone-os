# TICKET_007: Tool Calling Integration - Testing Guide

## Overview

This guide provides testing procedures for all device control tools implemented in TICKET_007.

## Architecture Summary

```
Agent (agents.js)
  ↓ (LiveKit data channel)
  ↓ Topic: "device_command"
  ↓ Message: {"tool": "openApp", "params": {"appName": "spotify"}}
  ↓
ChatViewModel.handleDeviceCommand()
  ↓
ToolExecutor.executeTool()
  ↓
[AppController | SettingsController | TelephonyController | NavigationController]
  ↓
Android APIs
  ↓
ToolResultMessage
  ↓ Topic: "device_command_result"
  ↓ Message: {"success": true, "result": {...}}
  ↓
Agent receives result
```

## Testing via LiveKit Data Channel

### Prerequisites

1. **Device Setup**:
   - Stone Launcher installed and running
   - Permissions granted (see Permission Testing section)
   - LiveKit connection active (can test in local mode)

2. **Agent Server Setup** (optional for manual testing):
   - `stone-agent-server` running
   - Connected to same LiveKit room as Android app

### Test Message Format

All tool commands use this JSON format on the `device_command` topic:

```json
{
  "tool": "toolName",
  "params": {
    "param1": "value1",
    "param2": "value2"
  }
}
```

Results are returned on the `device_command_result` topic:

```json
{
  "success": true,
  "result": {
    "key": "value"
  }
}
```

Or on error:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Error description"
  }
}
```

---

## Tool Testing Matrix

### 1. openApp - Launch Applications

**Purpose**: Open any installed app by name or package

**Test Cases**:

#### TC1: Open Stone App
```json
{
  "tool": "openApp",
  "params": {
    "appName": "listen"
  }
}
```
**Expected**: Opens LISTEN app (or shows "Opening listen" toast if not yet implemented)

#### TC2: Open Third-Party App (Spotify)
```json
{
  "tool": "openApp",
  "params": {
    "appName": "spotify"
  }
}
```
**Expected**: Opens Spotify if installed, or redirects to Play Store

#### TC3: Open by Package Name
```json
{
  "tool": "openApp",
  "params": {
    "appName": "com.google.android.apps.maps"
  }
}
```
**Expected**: Opens Google Maps

#### TC4: Unknown App (Error Case)
```json
{
  "tool": "openApp",
  "params": {
    "appName": "nonexistent"
  }
}
```
**Expected**: Error response with "Unknown app" message

**Success Criteria**:
- ✅ Opens installed apps correctly
- ✅ Redirects to Play Store for non-installed apps
- ✅ Returns appropriate error for unknown apps
- ✅ Works for both Stone apps and third-party apps

---

### 2. setWifi - WiFi Control

**Purpose**: Enable or disable WiFi

**Permission Required**: None (normal permission)

**Test Cases**:

#### TC1: Enable WiFi
```json
{
  "tool": "setWifi",
  "params": {
    "enabled": true
  }
}
```
**Expected**: WiFi turns on

#### TC2: Disable WiFi
```json
{
  "tool": "setWifi",
  "params": {
    "enabled": false
  }
}
```
**Expected**: WiFi turns off

**Note**: On Android 10+, this may require user interaction. The tool will guide user to settings if needed.

**Success Criteria**:
- ✅ WiFi state changes when permission allows
- ✅ Graceful error message if permission denied
- ✅ Result includes actual WiFi state

---

### 3. setBrightness - Screen Brightness Control

**Purpose**: Adjust screen brightness

**Permission Required**: WRITE_SETTINGS (special - manual grant)

**Test Cases**:

#### TC1: Set Medium Brightness
```json
{
  "tool": "setBrightness",
  "params": {
    "brightness": 128
  }
}
```
**Expected**: Brightness adjusts to 50% (128/255)

#### TC2: Set Maximum Brightness
```json
{
  "tool": "setBrightness",
  "params": {
    "brightness": 255
  }
}
```
**Expected**: Brightness at 100%

#### TC3: Set Minimum Brightness
```json
{
  "tool": "setBrightness",
  "params": {
    "brightness": 0
  }
}
```
**Expected**: Brightness at minimum

#### TC4: Without Permission (Error Case)
**Expected**: Error response indicating WRITE_SETTINGS permission needed

**Success Criteria**:
- ✅ Brightness changes correctly (0-255 range)
- ✅ Values are clamped to valid range
- ✅ Clear error if permission not granted
- ✅ Result includes final brightness value

---

### 4. setVolume - Volume Control

**Purpose**: Adjust volume for different audio streams

**Permission Required**: MODIFY_AUDIO_SETTINGS (normal - auto-granted)

**Test Cases**:

#### TC1: Set Media Volume to 50%
```json
{
  "tool": "setVolume",
  "params": {
    "streamType": "media",
    "level": 50
  }
}
```
**Expected**: Media volume at 50%

#### TC2: Set Ring Volume to 100%
```json
{
  "tool": "setVolume",
  "params": {
    "streamType": "ring",
    "level": 100
  }
}
```
**Expected**: Ringer volume at max

#### TC3: Set Alarm Volume
```json
{
  "tool": "setVolume",
  "params": {
    "streamType": "alarm",
    "level": 75
  }
}
```
**Expected**: Alarm volume at 75%

#### TC4: Invalid Stream Type (Error Case)
```json
{
  "tool": "setVolume",
  "params": {
    "streamType": "invalid",
    "level": 50
  }
}
```
**Expected**: Error response "Unknown stream type"

**Supported Stream Types**:
- `media` - Music, videos, games
- `ring` - Phone ringer
- `alarm` - Alarms
- `notification` - Notification sounds

**Success Criteria**:
- ✅ Volume changes for correct stream
- ✅ Level percentage (0-100) converts to system scale correctly
- ✅ Result includes actual volume level and max volume
- ✅ Error for invalid stream type

---

### 5. makeCall - Phone Calls

**Purpose**: Initiate a phone call

**Permission Required**: CALL_PHONE (dangerous - runtime request)

**Test Cases**:

#### TC1: Call Valid Number
```json
{
  "tool": "makeCall",
  "params": {
    "phoneNumber": "+1-555-123-4567"
  }
}
```
**Expected**: Phone call initiated

#### TC2: Call with Formatting
```json
{
  "tool": "makeCall",
  "params": {
    "phoneNumber": "(555) 123-4567"
  }
}
```
**Expected**: Number cleaned, call initiated

#### TC3: Invalid Number (Error Case)
```json
{
  "tool": "makeCall",
  "params": {
    "phoneNumber": "123"
  }
}
```
**Expected**: Error "Invalid phone number"

#### TC4: Without Permission (Error Case)
**Expected**: Error "CALL_PHONE permission not granted"

**Success Criteria**:
- ✅ Call initiated with valid number
- ✅ Phone number formatting cleaned automatically
- ✅ Clear error for invalid numbers
- ✅ Permission check before attempting call
- ✅ Result includes cleaned phone number

**Safety Note**: Test with your own phone number or a test number. This tool initiates real phone calls!

---

### 6. sendMessage - SMS Messaging

**Purpose**: Send text messages

**Permission Required**: SEND_SMS (dangerous - runtime request)

**Test Cases**:

#### TC1: Send Short Message
```json
{
  "tool": "sendMessage",
  "params": {
    "phoneNumber": "+1-555-123-4567",
    "message": "Test message from Stone"
  }
}
```
**Expected**: SMS sent

#### TC2: Send Long Message (>160 chars)
```json
{
  "tool": "sendMessage",
  "params": {
    "phoneNumber": "+1-555-123-4567",
    "message": "This is a very long message that exceeds the standard 160 character limit for a single SMS. It should be automatically split into multiple parts by the SmsManager."
  }
}
```
**Expected**: Multi-part SMS sent

#### TC3: Empty Message (Error Case)
```json
{
  "tool": "sendMessage",
  "params": {
    "phoneNumber": "+1-555-123-4567",
    "message": ""
  }
}
```
**Expected**: Error "Message cannot be empty"

#### TC4: Message Too Long (Error Case)
```json
{
  "tool": "sendMessage",
  "params": {
    "phoneNumber": "+1-555-123-4567",
    "message": "[1700 character message]"
  }
}
```
**Expected**: Error "Message too long (max 1600 characters)"

**Success Criteria**:
- ✅ Short messages send successfully
- ✅ Long messages split into parts automatically
- ✅ Validation catches empty/too-long messages
- ✅ Permission check before sending
- ✅ Result includes message length

**Safety Note**: Test with your own phone number! This tool sends real SMS messages that may incur carrier charges.

---

### 7. navigate - Google Maps Navigation

**Purpose**: Start turn-by-turn navigation

**Permission Required**: None (uses implicit intent)

**Test Cases**:

#### TC1: Navigate to Address
```json
{
  "tool": "navigate",
  "params": {
    "destination": "1600 Amphitheatre Parkway, Mountain View, CA",
    "mode": "driving"
  }
}
```
**Expected**: Google Maps opens with navigation to address

#### TC2: Navigate Walking
```json
{
  "tool": "navigate",
  "params": {
    "destination": "Central Park, New York",
    "mode": "walking"
  }
}
```
**Expected**: Maps opens with walking directions

#### TC3: Navigate Bicycling
```json
{
  "tool": "navigate",
  "params": {
    "destination": "Golden Gate Bridge",
    "mode": "bicycling"
  }
}
```
**Expected**: Maps opens with bicycle route

#### TC4: Navigate Transit
```json
{
  "tool": "navigate",
  "params": {
    "destination": "Times Square, NYC",
    "mode": "transit"
  }
}
```
**Expected**: Maps opens with public transit options

#### TC5: Empty Destination (Error Case)
```json
{
  "tool": "navigate",
  "params": {
    "destination": "",
    "mode": "driving"
  }
}
```
**Expected**: Error "Destination cannot be empty"

**Supported Modes**:
- `driving` (default)
- `walking`
- `bicycling`
- `transit`

**Success Criteria**:
- ✅ Navigation starts for valid destinations
- ✅ Correct mode selected
- ✅ Works with addresses and place names
- ✅ Error if Google Maps not installed
- ✅ Result includes destination and mode

---

### 8. showLocation - Show Location on Map

**Purpose**: Display a location on the map (without starting navigation)

**Permission Required**: None

**Test Cases**:

#### TC1: Show Place by Name
```json
{
  "tool": "showLocation",
  "params": {
    "query": "Statue of Liberty"
  }
}
```
**Expected**: Maps shows Statue of Liberty location

#### TC2: Show by Coordinates
```json
{
  "tool": "showLocation",
  "params": {
    "latitude": 37.7749,
    "longitude": -122.4194,
    "label": "San Francisco"
  }
}
```
**Expected**: Maps shows coordinates with label

#### TC3: Show Coordinates Without Label
```json
{
  "tool": "showLocation",
  "params": {
    "latitude": 40.7128,
    "longitude": -74.0060
  }
}
```
**Expected**: Maps shows coordinates

#### TC4: Missing Parameters (Error Case)
```json
{
  "tool": "showLocation",
  "params": {}
}
```
**Expected**: Error "Missing parameters: need either 'query' or 'latitude'/'longitude'"

**Success Criteria**:
- ✅ Shows location by place name
- ✅ Shows location by coordinates
- ✅ Optional label works correctly
- ✅ Error if no valid parameters
- ✅ Result includes location details

---

## Not Yet Implemented (Placeholders)

These tools return "NOT_IMPLEMENTED" error:

### 9. setAlarm - Set Device Alarm
```json
{
  "tool": "setAlarm",
  "params": {
    "time": "07:00",
    "label": "Wake up"
  }
}
```
**Expected**: Error "Tool 'setAlarm' not yet implemented"
**Implementation**: Will be added in TICK app ticket

### 10. setTimer - Set Countdown Timer
```json
{
  "tool": "setTimer",
  "params": {
    "duration": 180
  }
}
```
**Expected**: Error "Tool 'setTimer' not yet implemented"
**Implementation**: Will be added in TICK app ticket

### 11. playMusic - Spotify Control
```json
{
  "tool": "playMusic",
  "params": {
    "query": "jazz playlist"
  }
}
```
**Expected**: Error "Tool 'playMusic' not yet implemented"
**Implementation**: Will be added in LISTEN app ticket (requires Spotify SDK integration)

---

## Permission Testing

### Runtime Permissions (Dangerous)

These require explicit user grant:

#### Test Permission Flow: CALL_PHONE
1. Open Stone Launcher
2. Send `makeCall` tool command
3. **First time**: Permission dialog appears
4. Grant permission
5. Call initiates

#### Test Permission Denial
1. Deny permission
2. Send tool command again
3. **Expected**: Error "CALL_PHONE permission not granted"

### Special Permissions

#### Test WRITE_SETTINGS (Brightness Control)
1. Send `setBrightness` command
2. **If not granted**: Error with instructions
3. Navigate to Settings → Apps → Stone Launcher → Permissions → Modify system settings
4. Grant permission
5. Send command again
6. **Expected**: Brightness changes

### Permission List by Tool

| Tool | Permission | Type | Auto-Granted |
|------|------------|------|--------------|
| openApp | None | - | ✅ |
| setWifi | CHANGE_WIFI_STATE | Normal | ✅ |
| setBrightness | WRITE_SETTINGS | Special | ❌ (manual) |
| setVolume | MODIFY_AUDIO_SETTINGS | Normal | ✅ |
| makeCall | CALL_PHONE | Dangerous | ❌ (runtime) |
| sendMessage | SEND_SMS | Dangerous | ❌ (runtime) |
| navigate | None | - | ✅ |
| showLocation | None | - | ✅ |

---

## Integration Testing

### End-to-End Flow Test

1. **Connect to LiveKit room**
2. **Send multiple commands in sequence**:
```javascript
// In agent server
await sendCommand({tool: "setVolume", params: {streamType: "media", level: 50}});
await sendCommand({tool: "openApp", params: {appName: "spotify"}});
await sendCommand({tool: "navigate", params: {destination: "coffee shop nearby", mode: "walking"}});
```

3. **Verify**:
   - Each tool executes successfully
   - Results returned correctly
   - Chat UI shows operation status
   - No errors in logcat

### Stress Testing

Send rapid succession of commands:
```javascript
for (let i = 0; i < 10; i++) {
  await sendCommand({tool: "setVolume", params: {streamType: "media", level: i * 10}});
}
```

**Expected**: All commands execute, no crashes, results returned in order

---

## Error Scenarios

### Common Error Codes

| Code | Meaning | Common Cause |
|------|---------|--------------|
| UNKNOWN_TOOL | Tool name not recognized | Typo in tool name |
| INVALID_PARAMETERS | Missing or invalid params | Missing required parameter |
| PERMISSION_DENIED | Permission not granted | User denied permission |
| NOT_IMPLEMENTED | Tool not yet complete | Placeholder tool |
| APP_NOT_INSTALLED | App not on device | Third-party app not installed |
| UNKNOWN_ERROR | Unexpected error | Exception in controller |

### Test Error Handling

#### Invalid Tool Name
```json
{"tool": "invalidTool", "params": {}}
```
**Expected**: `{"success": false, "error": {"code": "UNKNOWN_TOOL", ...}}`

#### Missing Parameters
```json
{"tool": "openApp", "params": {}}
```
**Expected**: `{"success": false, "error": {"code": "INVALID_PARAMETERS", ...}}`

---

## Debugging

### LogCat Filters

Monitor tool execution:
```bash
adb logcat | grep -E "ToolExecutor|ChatViewModel|AppController|SettingsController|TelephonyController|NavigationController"
```

### Enable Verbose Logging

In ChatViewModel:
```kotlin
// Already enabled - check logcat for:
// D/ChatViewModel: Data received on topic 'device_command': ...
// D/ToolExecutor: Executing tool: ...
// I/AppController: Successfully opened: ...
```

---

## Success Criteria Summary

TICKET_007 is complete when:

- ✅ All 8 core tools execute correctly
- ✅ Results returned via data channel
- ✅ ChatViewModel processes device_command topic
- ✅ Permissions handled appropriately
- ✅ Error cases handled gracefully
- ✅ AndroidManifest updated with package queries
- ✅ Documentation complete (this file)
- ✅ Integration tests pass

---

## Next Steps (Future Tickets)

- **TICKET_008**: Implement setAlarm, setTimer (TICK app)
- **TICKET_009**: Implement playMusic with Spotify SDK (LISTEN app)
- **TICKET_010**: Add permission management UI
- **TICKET_011**: Implement rate limiting and security layer
- **TICKET_012**: Add user consent dialogs for dangerous operations
