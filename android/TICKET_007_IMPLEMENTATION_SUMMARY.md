# TICKET_007: Tool Calling Integration - Implementation Summary

**Status**: ✅ COMPLETED
**Date**: 2025-11-13

---

## Overview

Implemented complete tool calling integration between AI agents (via LiveKit) and Android device control capabilities. This enables the "Headless" control layer of Stone's "Head & Headless" architecture, where AI agents can perform device operations on behalf of the user.

---

## Files Created

### 1. Data Models (`/tools/ToolMessages.kt`)
- `ToolCallMessage` - Tool call request from agent
- `ToolResultMessage` - Execution result sent back to agent
- `ToolError` - Error information structure
- `StatusUpdateMessage` - Progress updates for long operations

**Key Features**:
- Uses Kotlinx Serialization for JSON encoding/decoding
- Clean, type-safe message structures
- Matches agent server expectations

### 2. Controller Classes

#### `/controllers/AppController.kt`
**Responsibilities**:
- Launch apps by name or package identifier
- Resolve Stone app names to activities
- Resolve common third-party app names to packages
- Handle app-not-installed cases (redirect to Play Store)

**Tools Implemented**:
- `openApp(appName: String)` - Opens any app

**Features**:
- Smart app name resolution (Stone apps, common apps, package names)
- Package visibility awareness (Android 11+)
- Graceful degradation for missing apps

#### `/controllers/SettingsController.kt`
**Responsibilities**:
- Control WiFi state
- Adjust screen brightness
- Manage volume for different audio streams
- Handle special permissions (WRITE_SETTINGS)

**Tools Implemented**:
- `setWifiEnabled(enabled: Boolean)` - Toggle WiFi
- `getWifiState()` - Query WiFi state
- `setBrightness(brightness: Int)` - Adjust brightness (0-255)
- `setVolume(streamType: String, level: Int)` - Adjust volume (0-100%)
- `getVolume(streamType: String)` - Query volume

**Features**:
- Automatic permission checking
- Value normalization (brightness 0-255, volume 0-100%)
- Support for all audio streams (media, ring, alarm, notification)

#### `/controllers/TelephonyController.kt`
**Responsibilities**:
- Initiate phone calls
- Send SMS messages
- Handle telephony permissions

**Tools Implemented**:
- `makeCall(phoneNumber: String)` - Initiate phone call
- `sendSMS(phoneNumber: String, message: String)` - Send text message
- `openDialer(phoneNumber: String)` - Open dialer (no permission needed)

**Features**:
- Phone number validation and cleaning
- Automatic multi-part SMS for long messages
- Runtime permission checking (CALL_PHONE, SEND_SMS)
- Message length validation (max 1600 chars)

#### `/controllers/NavigationController.kt`
**Responsibilities**:
- Google Maps integration
- Navigation and location display
- No special permissions required

**Tools Implemented**:
- `navigateTo(destination: String, mode: String)` - Start navigation
- `showLocation(query: String)` - Show location by name
- `showCoordinates(lat: Double, lng: Double, label: String?)` - Show coordinates

**Features**:
- Multiple navigation modes (driving, walking, bicycling, transit)
- Works with addresses, place names, and coordinates
- Graceful error if Maps not installed

### 3. Main Executor (`/tools/ToolExecutor.kt`)

**Responsibilities**:
- Route tool calls to appropriate controllers
- Parameter validation and extraction
- Error handling and result formatting
- Tool-to-controller mapping

**Supported Tools**:
1. `openApp` - Launch applications
2. `setWifi` - WiFi control
3. `setBrightness` - Screen brightness
4. `setVolume` - Volume control
5. `makeCall` - Phone calls
6. `sendMessage` - SMS messaging
7. `navigate` - Maps navigation
8. `showLocation` - Show location on map
9. `setAlarm` - Placeholder (future TICK app)
10. `setTimer` - Placeholder (future TICK app)
11. `playMusic` - Placeholder (future LISTEN app)

**Features**:
- Centralized tool routing
- Consistent error handling
- JSON parameter extraction
- Result type conversion

### 4. Integration (`/ui/ChatViewModel.kt`)

**Modified Sections**:
- Added `ToolExecutor` initialization in `connect()`
- Added `handleDeviceCommand()` method to process "device_command" topic
- Added `sendToolResult()` method to return results via data channel
- Integrated status messages into chat UI

**Data Channel Topics**:
- **Incoming**: `device_command` - Tool calls from agent
- **Outgoing**: `device_command_result` - Execution results

**User Feedback**:
- Shows "executing: {tool}" when command received
- Shows "✓ {tool} completed" on success
- Shows "✗ {tool} failed: {error}" on failure

---

## Files Modified

### 1. `/app/build.gradle`
**Changes**:
- Added Kotlin Serialization plugin
- Added `kotlinx-serialization-json:1.6.0` dependency

**Purpose**: Enable JSON serialization for tool messages

### 2. `/AndroidManifest.xml`
**Changes**:
- Added `<queries>` section for package visibility (Android 11+)
- Declared visible packages: Spotify, Google Maps, Gmail, Chrome, Camera

**Purpose**: Allow Stone to query and launch third-party apps

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Agent Server (agents.js)                  │
│                                                               │
│  - Receives user voice/text input                            │
│  - Determines which tool to call                             │
│  - Sends tool call via LiveKit data channel                  │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      │ LiveKit Data Channel
                      │ Topic: "device_command"
                      │ {"tool": "openApp", "params": {...}}
                      │
                      ↓
┌─────────────────────────────────────────────────────────────┐
│                 Stone Launcher (Android App)                 │
│                                                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ChatViewModel.handleDeviceCommand()                   │  │
│  │  - Receives data from LiveKit                         │  │
│  │  - Parses ToolCallMessage                             │  │
│  │  - Shows "executing" status in chat                   │  │
│  └────────────────────┬──────────────────────────────────┘  │
│                       │                                       │
│                       ↓                                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ToolExecutor.executeTool()                            │  │
│  │  - Routes to appropriate controller                   │  │
│  │  - Validates parameters                               │  │
│  │  - Handles errors                                     │  │
│  └────────┬──────────────────────────────────────────────┘  │
│           │                                                   │
│           ├──→ AppController (open apps)                     │
│           ├──→ SettingsController (WiFi, brightness, volume) │
│           ├──→ TelephonyController (calls, SMS)              │
│           └──→ NavigationController (Maps)                   │
│                       │                                       │
│                       ↓                                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Android APIs                                          │  │
│  │  - PackageManager (launch apps)                       │  │
│  │  - WifiManager (WiFi control)                         │  │
│  │  - Settings.System (brightness)                       │  │
│  │  - AudioManager (volume)                              │  │
│  │  - SmsManager (SMS)                                   │  │
│  │  - Intent.ACTION_CALL (phone calls)                   │  │
│  │  - Intent.ACTION_VIEW (Maps)                          │  │
│  └───────────────────────────────────────────────────────┘  │
│                       │                                       │
│                       ↓                                       │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ChatViewModel.sendToolResult()                        │  │
│  │  - Converts Result to ToolResultMessage               │  │
│  │  - Serializes to JSON                                 │  │
│  │  - Sends via LiveKit data channel                     │  │
│  │  - Shows result in chat                               │  │
│  └────────────────────┬──────────────────────────────────┘  │
└─────────────────────┼─┼───────────────────────────────────────┘
                      │
                      │ LiveKit Data Channel
                      │ Topic: "device_command_result"
                      │ {"success": true, "result": {...}}
                      │
                      ↓
┌─────────────────────────────────────────────────────────────┐
│                    Agent Server (agents.js)                  │
│                                                               │
│  - Receives tool result                                      │
│  - Updates agent state                                       │
│  - Responds to user with result                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Permission Strategy

### Permission Types

#### Normal Permissions (Auto-Granted)
Already declared in AndroidManifest.xml:
- `ACCESS_WIFI_STATE` - Read WiFi state
- `CHANGE_WIFI_STATE` - Control WiFi
- `MODIFY_AUDIO_SETTINGS` - Control volume
- `BLUETOOTH` - Basic Bluetooth access

#### Dangerous Permissions (Runtime Request)
Already declared in AndroidManifest.xml:
- `CALL_PHONE` - Make phone calls (makeCall tool)
- `SEND_SMS` - Send text messages (sendMessage tool)
- `READ_SMS` - Read messages (future)
- `READ_CONTACTS` - Access contacts (future)
- `ACCESS_FINE_LOCATION` - GPS location (future Maps features)

#### Special Permissions (Manual Grant)
Already declared in AndroidManifest.xml:
- `WRITE_SETTINGS` - Modify system settings (setBrightness tool)

### Permission Handling Pattern

Controllers check permissions before execution:

```kotlin
// Example from TelephonyController
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
    != PackageManager.PERMISSION_GRANTED) {
    return Result.failure(
        SecurityException("CALL_PHONE permission not granted")
    )
}
```

**Error Response**:
```json
{
  "success": false,
  "error": {
    "code": "PERMISSION_DENIED",
    "message": "CALL_PHONE permission not granted"
  }
}
```

**Future Enhancement** (TICKET_010):
- Add permission request UI in Stone Launcher
- Show rationale before requesting dangerous permissions
- Guide user to Settings for special permissions
- Track permission grant/denial for analytics

---

## Testing Approach

### Unit Testing (Controllers)
Each controller can be tested independently:

```kotlin
val controller = AppController(context)
val result = controller.openApp("spotify")
assertTrue(result.isSuccess)
```

### Integration Testing (Tool Execution)
Test full tool execution flow:

```kotlin
val executor = ToolExecutor(context)
val toolCall = ToolCallMessage(
    tool = "openApp",
    params = mapOf("appName" to JsonPrimitive("spotify"))
)
val result = executor.executeTool(toolCall)
assertTrue(result.success)
```

### End-to-End Testing (LiveKit)
Test complete agent-to-device flow:

1. Agent sends tool call via data channel
2. ChatViewModel processes command
3. ToolExecutor executes
4. Result sent back to agent
5. Agent confirms receipt

See `TICKET_007_TESTING.md` for comprehensive test cases.

---

## Error Handling

### Error Code Types

| Code | Meaning | Example |
|------|---------|---------|
| UNKNOWN_TOOL | Tool name not recognized | "openAppp" (typo) |
| INVALID_PARAMETERS | Missing/wrong params | Missing "appName" |
| PERMISSION_DENIED | Permission not granted | CALL_PHONE denied |
| NOT_IMPLEMENTED | Placeholder tool | setAlarm, setTimer |
| APP_NOT_INSTALLED | App not on device | Spotify not installed |
| SecurityException | Permission error | WRITE_SETTINGS not granted |
| IllegalArgumentException | Invalid param value | Invalid phone number |
| ActivityNotFoundException | Intent target missing | Maps not installed |

### Error Handling Flow

```
Controller Operation
   ↓ (try-catch)
Result.failure(exception)
   ↓
ToolExecutor.getErrorCode()
   ↓
ToolResultMessage with ToolError
   ↓
Sent to agent
   ↓
Agent handles error gracefully
```

---

## Performance Considerations

### Asynchronous Execution
- All tools execute in coroutine context (viewModelScope)
- No blocking of UI thread
- Multiple tools can be queued

### Resource Management
- Controllers instantiated once per ChatViewModel
- No memory leaks (controllers tied to ViewModel lifecycle)
- Lightweight message passing via data channel

### Battery Impact
- Tools only execute on-demand (no background processing)
- No polling or continuous listeners
- Minimal impact on battery life

---

## Security Considerations

### Current Implementation
1. **Permission Checking**: All dangerous operations check permissions before execution
2. **Input Validation**: Phone numbers, app names, etc. validated before use
3. **Error Messages**: Don't expose sensitive information
4. **No Auto-Grant**: Permissions require explicit user grant

### Future Enhancements (TICKET_011)
1. **Rate Limiting**: Prevent abuse by limiting tool calls per minute
2. **User Consent**: Show confirmation dialog for sensitive operations
3. **Audit Logging**: Log all tool executions for forensics
4. **Blocked Operations**: Blacklist certain dangerous operations
5. **Trusted Agent Verification**: Verify agent identity before execution

---

## Known Limitations

### 1. WiFi Control (Android 10+)
**Issue**: `WifiManager.setWifiEnabled()` deprecated in API 29+
**Current Behavior**: Works on Android 9 and below, may require user interaction on 10+
**Future Fix**: Guide user to Settings to toggle WiFi

### 2. Bluetooth Control
**Not Implemented**: Bluetooth control requires different approach on modern Android
**Reason**: `BluetoothAdapter.enable()` deprecated, requires user interaction
**Future Ticket**: TICKET_013 - Add Bluetooth control with user prompt

### 3. Spotify Control
**Not Implemented**: playMusic tool is placeholder
**Reason**: Requires Spotify SDK integration and OAuth
**Future Ticket**: TICKET_014 - LISTEN app with Spotify SDK

### 4. Alarms and Timers
**Not Implemented**: setAlarm and setTimer are placeholders
**Reason**: Part of TICK app implementation
**Future Ticket**: TICKET_008 - TICK app (time management)

### 5. Permission UI
**Not Implemented**: No in-app permission request UI
**Current Behavior**: Errors returned if permission not granted
**Future Ticket**: TICKET_010 - Permission management UI

---

## Integration with Agent Server

### Agent-Side Implementation

**Location**: `stone-agent-server/src/tools/device-tools.ts` (needs to be created)

**Example Tool Definition**:
```typescript
export const deviceTools = [
  {
    name: 'open_app',
    description: 'Open an Android app by name',
    parameters: {
      type: 'object',
      properties: {
        appName: {
          type: 'string',
          description: 'App name (e.g., "spotify", "maps")'
        }
      },
      required: ['appName']
    },
    execute: async (params: any) => {
      // Send to Android via LiveKit data channel
      const message = {
        tool: 'openApp',
        params: params
      };

      await publishData(
        JSON.stringify(message),
        'device_command'
      );

      // Wait for result on 'device_command_result' topic
      return await waitForResult();
    }
  },
  // ... more tools
];
```

### Data Channel Communication

**Sending Command (Agent → Android)**:
```typescript
const message = {
  tool: 'setVolume',
  params: {
    streamType: 'media',
    level: 50
  }
};

await room.localParticipant.publishData(
  new TextEncoder().encode(JSON.stringify(message)),
  { topic: 'device_command', reliable: true }
);
```

**Receiving Result (Android → Agent)**:
```typescript
room.on('dataReceived', (data: Uint8Array, participant, topic: string) => {
  if (topic === 'device_command_result') {
    const result = JSON.parse(new TextDecoder().decode(data));

    if (result.success) {
      console.log('Tool succeeded:', result.result);
    } else {
      console.error('Tool failed:', result.error);
    }
  }
});
```

---

## Next Steps

### Immediate (Required for Full Integration)
1. **Agent Server Tools** (TICKET_006b):
   - Create `device-tools.ts` with all tool definitions
   - Implement LiveKit data channel publishing
   - Add result waiting logic
   - Test end-to-end flow

### Short-Term Enhancements
2. **Permission Management UI** (TICKET_010):
   - Add permission request dialogs
   - Show permission rationale
   - Guide to Settings for special permissions

3. **Security Layer** (TICKET_011):
   - Implement rate limiting
   - Add user consent dialogs for sensitive ops
   - Create audit logging system

### Long-Term Features
4. **TICK App** (TICKET_008):
   - Implement setAlarm tool
   - Implement setTimer tool
   - Add stopwatch and world clock

5. **LISTEN App** (TICKET_014):
   - Integrate Spotify SDK
   - Implement playMusic tool
   - Add playlist management

6. **Bluetooth Control** (TICKET_013):
   - Research modern Bluetooth API
   - Implement setBluetooth tool
   - Handle pairing and connections

---

## Success Metrics

### Completed ✅
- [x] All 8 core tools implemented and functional
- [x] Controllers follow "Head & Headless" pattern
- [x] ChatViewModel processes device_command topic
- [x] Results sent back via data channel
- [x] Error handling comprehensive
- [x] Permissions declared in manifest
- [x] Package visibility configured
- [x] Testing documentation complete
- [x] Implementation documented

### Pending (Future Tickets)
- [ ] Agent server tools implemented
- [ ] End-to-end testing with real agent
- [ ] Permission UI implemented
- [ ] Rate limiting added
- [ ] User consent dialogs added
- [ ] Audit logging system

---

## Conclusion

TICKET_007 has successfully implemented the core tool calling infrastructure for Stone Launcher. The Android side is fully prepared to receive and execute device control commands from AI agents via LiveKit. All 8 core tools are implemented with proper error handling, permission checking, and result reporting.

The implementation follows Stone's "Head & Headless" architecture pattern, where controllers are reusable by both UI (future) and Intent API layers. This ensures consistency and maintainability.

**Next Critical Step**: Implement agent-side tool definitions in `stone-agent-server` to enable bidirectional communication and complete the tool calling integration.

---

**Implementation By**: Claude (Anthropic AI)
**Ticket Reference**: TICKET_007
**Related Tickets**: TICKET_004 (LiveKit), TICKET_006 (Agent Server)
**Documentation**: TICKET_007_TESTING.md, TICKET_007_IMPLEMENTATION_SUMMARY.md
