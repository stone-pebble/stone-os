# Stone Launcher - API Bridge & Tool Architecture

**Version**: 2.0
**Last Updated**: November 12, 2025

---

## Purpose

This document describes the architectural approach for exposing Stone Launcher functionality to AI agents via tool calls. It does NOT attempt to define every tool upfront - instead, it establishes the pattern and principles for how functionality should be exposed as it's developed.

---

## Core Principle: Everything Must Be Controllable

**Every piece of functionality implemented in Stone Launcher must be exposed via the API bridge for AI agent access.**

This is architectural, not optional:
- If a user can do it via touch UI, an agent must be able to do it via tool call
- The same underlying business logic serves both interfaces ("Head & Headless")
- No functionality is complete until both touch and agent control work

---

## The API Bridge Architecture

### Two Communication Layers

**Layer 1: React Native to Android Native (Touch UI)**
```
React Native UI → Native Module → Controller (Business Logic) → Android APIs
```

**Layer 2: AI Agent to Android Native (Voice/Agent Control)**
```
AI Agent Tool Call → Intent API Bridge → Controller (Business Logic) → Android APIs
```

Both layers call the **same Controller classes** - single source of truth for all business logic.

### How the Intent API Bridge Works

The Intent API Bridge receives tool calls from AI agents and translates them into Android Intent broadcasts that the Stone Launcher BroadcastReceiver handles.

**Flow**:
1. AI agent calls tool: `set_wifi({ enabled: true })`
2. LiveKit data channel sends tool execution request to device
3. Intent API Bridge on device broadcasts Android Intent:
   ```
   Action: com.stone.launcher.action.SET_WIFI
   Extras: { enabled: true }
   ```
4. StoneApiReceiver receives broadcast
5. Calls WifiController.setWifiEnabled(true) - same method the UI uses
6. Returns result via broadcast Intent
7. Intent API Bridge receives result
8. Returns success/failure to AI agent

### Intent Naming Convention

```
Action: com.stone.launcher.action.{FEATURE}
Result: com.stone.launcher.result.{FEATURE}
Category: com.stone.launcher.category.API
```

Examples:
- `com.stone.launcher.action.SET_WIFI`
- `com.stone.launcher.action.SEND_SMS`
- `com.stone.launcher.action.PLAY_MUSIC`
- `com.stone.launcher.action.CREATE_ALARM`

---

## Tool Indexing by Sub-Agent

Each of the 12 Stone Apps has its own sub-agent with its own tool index.

### Tool Index Structure

When a sub-agent is activated, it exposes its tool index to the main agent:

```typescript
{
  "tools": [
    {
      "name": "set_wifi",
      "description": "Enable or disable WiFi",
      "parameters": {
        "type": "object",
        "properties": {
          "enabled": {
            "type": "boolean",
            "description": "true to enable, false to disable"
          }
        },
        "required": ["enabled"]
      }
    },
    // ... more tools for this sub-agent
  ]
}
```

### Sub-Agent Tool Organization

**TICK Agent** (Time Management):
- Tools for alarms, timers, stopwatch, world clock
- Example: `create_alarm()`, `start_timer()`, `get_world_time()`

**CONNECT Agent** (Communications):
- Tools for contacts, calls, SMS, email
- Example: `send_sms()`, `make_call()`, `search_contacts()`

**GO Agent** (Navigation):
- Tools for maps, navigation, location
- Example: `get_directions()`, `start_navigation()`, `search_nearby()`

**LISTEN Agent** (Music):
- Tools for Spotify control
- Example: `play_music()`, `control_playback()`, `create_playlist()`

**PLAN Agent** (Calendar):
- Tools for calendar events, goals
- Example: `create_event()`, `get_events()`, `track_goal()`

**THINK Agent** (Notes):
- Tools for note-taking
- Example: `create_note()`, `search_notes()`, `tag_note()`

**ASK Agent** (Search/Knowledge):
- Tools for web search, Perplexity integration
- Example: `web_search()`, `ask_question()`

**SET Agent** (Settings):
- Tools for system settings
- Example: `set_wifi()`, `set_bluetooth()`, `adjust_brightness()`

**TASK Agent** (App Launcher):
- Tools for app management
- Example: `launch_app()`, `search_apps()`, `list_apps()`

**REFLECT Agent** (Activity Journal):
- Tools for activity logging, summaries
- Example: `get_daily_summary()`, `log_activity()`

**FUND Agent** (Payments):
- Tools for wallet access (NO AI involvement in actual payments)
- Example: `open_wallet()`, `open_banking_app()`

**LOOK Agent** (Digital Library):
- Tools for book reading
- Example: `search_books()`, `open_book()`, `bookmark_page()`

---

## Implementation Approach

### For Each Feature You Build:

1. **Implement the Controller** (business logic)
   ```kotlin
   class WifiController(context: Context) {
     fun setWifiEnabled(enabled: Boolean): Result<Boolean>
     fun isWifiEnabled(): Boolean
   }
   ```

2. **Expose via Native Module** (for React Native)
   ```kotlin
   class WifiModule(reactContext: ReactApplicationContext) {
     @ReactMethod
     fun setWifiEnabled(enabled: Boolean, promise: Promise) {
       wifiController.setWifiEnabled(enabled)
     }
   }
   ```

3. **Expose via Intent API** (for AI agents)
   ```kotlin
   // In StoneApiReceiver.kt
   private fun handleSetWifi(context: Context, intent: Intent): IntentResult {
     val enabled = intent.getBooleanExtra("enabled", false)
     val controller = WifiController(context)
     return controller.setWifiEnabled(enabled).toIntentResult()
   }
   ```

4. **Add to Tool Index** (for sub-agent)
   ```typescript
   // In SetTools.ts (for SET sub-agent)
   {
     name: 'set_wifi',
     description: 'Enable or disable WiFi',
     parameters: { /* ... */ },
     execute: async (params) => {
       await intentBridge.sendIntent(
         'com.stone.launcher.action.SET_WIFI',
         { enabled: params.enabled }
       );
     }
   }
   ```

5. **Document in AndroidManifest.xml**
   ```xml
   <receiver android:name=".api.StoneApiReceiver">
     <intent-filter>
       <action android:name="com.stone.launcher.action.SET_WIFI" />
       <category android:name="com.stone.launcher.category.API" />
     </intent-filter>
   </receiver>
   ```

### What We DON'T Know Yet

This document intentionally does NOT specify:
- Exact parameter schemas for every tool
- Complete list of all tools
- Specific error codes and handling
- Rate limiting or security measures
- Detailed permission requirements

**Why?** Because we'll discover the right design as we implement each feature. The architecture above provides the pattern - the specifics emerge during development.

---

## Testing the API Bridge

### Manual Testing (adb)

You can test any Intent action via command line:

```bash
# Example: Set WiFi
adb shell am broadcast \
  -a com.stone.launcher.action.SET_WIFI \
  --ez enabled true

# Example: Send SMS
adb shell am broadcast \
  -a com.stone.launcher.action.SEND_SMS \
  --es recipient "+1234567890" \
  --es message "Test message"

# Monitor response
adb logcat -s StoneApiReceiver:*
```

### Agent Testing

AI agents test tools through the Intent API Bridge in the LiveKit data channel.

---

## Design Principles

### 1. Discoverability
Tools should be self-describing with clear names and parameter schemas.

### 2. Consistency
All tools follow the same Intent pattern and response format.

### 3. Idempotency Where Possible
Calling the same tool with same parameters multiple times should produce the same result.

### 4. Clear Error Messages
When tools fail, they should return actionable error messages the agent can understand.

### 5. Permission Transparency
Tools should clearly indicate when they fail due to missing permissions.

---

## Next Steps

As each Stone App is developed:
1. Build the core functionality
2. Expose via both Native Module AND Intent API
3. Create tool definitions for the sub-agent
4. Test both touch and agent control
5. Document any discovered limitations or patterns

**The goal**: By the time we're feature-complete, we'll have a comprehensive tool index that emerged organically from actual implementation, not premature specification.

---

## References

- **LAUNCHER_ARCHITECTURE.md** - Overall system architecture
- **LAUNCHER_REQUIREMENTS.md** - "Head & Headless" pattern details
- **AI_AGENT_INTEGRATION.md** - Agent architecture and tool loading
- **/tickets/** - Implementation tickets for each feature
