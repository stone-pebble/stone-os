# Stone Launcher - Architecture & Implementation Guide

**Version**: 2.0
**Last Updated**: November 12, 2025

---

## What We're Building

Stone Launcher is a standalone Android application that provides a minimalist, AI-augmented phone experience where every function can be controlled with equal ease through touch OR conversational AI.

**Core Principle**: "Choice First, Not Voice First"
- Users can start tasks with touch and finish with voice
- Or start with voice and finish with touch
- The system adapts to user preference moment-by-moment

---

## Project Requirements

This document outlines the complete requirements for Stone Launcher. See `/STONEOS_SPECS.md` for detailed feature specifications.

### Architectural Hypothesis

This document proposes one approach to implementing Stone Launcher as a standalone Android application using standard Android APIs, permissions, and the "Head & Headless" pattern.

**Important**: This is a hypothesis for achieving the requirements. If developers or agents discover alternative approaches that better satisfy the requirements in STONEOS_SPECS.md, those approaches should be explored and documented.

### System-Level Requirements (Hypothesis)

Some requirements *may* require system-level modifications or AOSP customization to achieve fully:
- **System-wide grayscale** (all apps, not just launcher)
- **True window split** (app at 2/3, chat at 1/3 simultaneously)
- **Notification interception** (beyond NotificationListenerService)
- **Deep app control** (answering calls, etc.)

**Current Approach**: Attempt implementation via:
1. Standard Android permissions
2. AccessibilityService (where appropriate)
3. NotificationListenerService
4. Documented permission grants via ADB (for testing)

**If Standard APIs Are Insufficient**: Document the limitations and explore:
- Xposed/LSPosed modules
- Magisk modules
- AOSP modifications (return to custom ROM approach)
- Hybrid approach (launcher + system mods)

---

## Core Architecture: "Head & Headless" Pattern

### Fundamental Principle

Every feature must be accessible through TWO equal interfaces:

1. **The "Head"** - Touch UI (Native Kotlin)
2. **The "Headless"** - Intent API (for AI agents)

Both interfaces call the **same underlying business logic**.

```
Touch UI (Native Kotlin)       AI Agent (Voice/Text)
         ↓                              ↓
   Activity/Fragment          Intent API (Broadcast)
         ↓                              ↓
         └──────────→ Controller ←──────┘
                    (Core Logic)
                         ↓
                  Android APIs
```

**Example**: WiFi Control
- Touch: User taps WiFi toggle → Activity calls WifiController.setEnabled()
- Voice: Agent sends SET_WIFI intent → BroadcastReceiver calls WifiController.setEnabled()
- **Both paths** execute WifiController.setEnabled() - single source of truth

---

## The 12 Stone Apps - Requirements

Each app has specific functional requirements detailed in STONEOS_SPECS.md. Summary:

| App | Function | Key Requirements |
|-----|----------|------------------|
| **TICK** | Time management | Alarms, timers, stopwatch, world clock - all controllable via touch/voice |
| **CONNECT** | Communications | Unified contacts, calls, SMS, email from all platforms |
| **ASK** | Knowledge/search | Perplexity-style search, limited browsing, AI-curated content |
| **GO** | Navigation | Google Maps integration, voice-controlled navigation |
| **LISTEN** | Music | Spotify control, playlist management, voice playback |
| **LOOK** | Digital library | Project Gutenberg books, reading interface |
| **PLAN** | Calendar/goals | Event management, goal tracking with progress visualization |
| **THINK** | Notes | Voice-to-text notes, auto-organization, tags |
| **REFLECT** | Life journal | Automatic activity logging, daily summaries |
| **TASK** | App/MCP management | Launch permitted apps, MCP tool discovery, app allowlist management |
| **SET** | Settings | WiFi, Bluetooth, brightness, volume, 2FA app access |
| **FUND** | Payments | Android Pay, banking app access (no AI involvement) |

---

## Required Capabilities

### UI/UX Requirements

**From STONEOS_SPECS.md**:

1. **Stone Icon Always Visible**
   - Bottom of screen in all contexts
   - Swipe up reveals chat interface (1/3 screen)
   - Current app shrinks to 2/3 screen OR chat overlays (implementation choice)

2. **Grayscale Aesthetic**
   - All Stone UI in grayscale
   - Third-party apps ideally grayscale (may require system-level modification)
   - Exceptions: Camera viewfinder, photos, Perplexity image results

3. **Minimal Home Screen**
   - 3x4 grid of app names (text-only)
   - No icons, no widgets, no clutter
   - Swipe right for camera

4. **Voice Mode Behavior**
   - NO transcription shown while user speaks
   - Stone icon glows during AI processing
   - Pure voice conversation without visual distraction

5. **Unlock Screen**
   - AI-generated narrative summary (not notification list)
   - Markdown rendering
   - Contextual, human-like language

### Functional Requirements by Category

#### System Control
- [ ] WiFi enable/disable
- [ ] WiFi settings access
- [ ] Bluetooth enable/disable
- [ ] Airplane mode toggle
- [ ] Brightness control (0-255)
- [ ] Volume control (music, ring, alarm, notification, system, voice call)
- [ ] Battery status
- [ ] Device information

#### Application Management
- [ ] List all installed apps
- [ ] Launch any app by name or package
- [ ] Search/filter apps
- [ ] App allowlist management (Wisephone-style curation)
- [ ] Block installation of banned apps (social media, shopping, addictive games)
- [ ] Access to "permitted apps" - utility-focused apps outside 12 Stone Apps
- [ ] App request and approval workflow

#### Communications
- [ ] Read contacts
- [ ] Search contacts
- [ ] Make phone calls
- [ ] Send SMS/MMS
- [ ] Read SMS/MMS
- [ ] Email integration (Gmail, Outlook)
- [ ] Cross-platform messaging (Slack, Teams - future)
- [ ] Contact-centric conversation view (all comms with person in one thread)

#### Time Management
- [ ] Set alarms with custom repeat patterns
- [ ] Start countdown timers
- [ ] Stopwatch with lap times
- [ ] World clock (multiple timezones)
- [ ] Natural language time parsing ("wake me at 7 on weekdays")

#### Location & Navigation
- [ ] Get current GPS location
- [ ] Search nearby places
- [ ] Get directions (driving, walking, transit, bicycling)
- [ ] Start navigation
- [ ] Real-time traffic updates

#### Media Control
- [ ] Spotify authentication
- [ ] Play music by song/artist/playlist
- [ ] Playback control (play, pause, next, previous)
- [ ] Create/edit playlists
- [ ] Music search

#### Calendar & Goals
- [ ] Read calendar events
- [ ] Create calendar events
- [ ] Edit/delete events
- [ ] Goal tracking with progress %
- [ ] Milestone visualization

#### Notes & Journaling
- [ ] Create notes from voice/text
- [ ] Auto-categorization
- [ ] Tag extraction
- [ ] Search notes
- [ ] Activity logging (if user enables)
- [ ] Daily activity summaries

#### Notifications
- [ ] Read all notifications (NotificationListenerService)
- [ ] Dismiss notifications
- [ ] Execute notification actions
- [ ] AI-generated summary for unlock screen
- [ ] Intelligent filtering (spam vs. important)

---

## Permission Strategy

### Permission Tiers

**Tier 1: Normal Permissions** (automatic)
```xml
ACCESS_WIFI_STATE, BLUETOOTH, INTERNET, VIBRATE, RECEIVE_BOOT_COMPLETED
```

**Tier 2: Dangerous Permissions** (runtime request)
```xml
ACCESS_FINE_LOCATION
CALL_PHONE
SEND_SMS, READ_SMS, RECEIVE_SMS
READ_CONTACTS, WRITE_CONTACTS
READ_CALL_LOG
READ_CALENDAR, WRITE_CALENDAR
CAMERA
```

**Implementation**:
- Clear explanations for each permission
- Show use cases before requesting
- Graceful degradation if denied

**Tier 3: Special Permissions** (manual grant in settings)
```xml
WRITE_SETTINGS (brightness control)
NOTIFICATION_LISTENER (notification aggregation)
BIND_ACCESSIBILITY_SERVICE (enhanced control - optional)
```

**Implementation**:
- Detect if not granted
- Show instructions with "Open Settings" button
- Direct user to exact settings page

**Tier 4: ADB-Granted Permissions** (for advanced features)
```
WRITE_SECURE_SETTINGS (mobile data, NFC, location toggles)
```

**Implementation**:
- Document exact adb command
- Show in settings UI for power users
- Clearly mark as optional/advanced

### Unknown: What Permissions Are Actually Needed?

**This is part of the exploration**. Developers should:
1. Attempt each feature with standard permissions
2. Document what works and what doesn't
3. Propose alternatives for blocked features
4. Escalate to system-level mods only if truly necessary

---

## Intent API Specification

Complete API documented in `/docs/TOOLS.md`.

### Intent Structure

All Stone Launcher Intents follow this pattern:

```
Action: com.stone.launcher.action.{FEATURE}
Category: com.stone.launcher.category.API
Extras: Feature-specific parameters
```

### Response Pattern

```
Action: com.stone.launcher.result.{FEATURE}
Extras:
  - success: boolean
  - (if success) result data
  - (if failure) error_message, error_code
```

### Example: WiFi Control

**Request**:
```bash
adb shell am broadcast \
  -a com.stone.launcher.action.SET_WIFI \
  --ez enabled true
```

**Response** (broadcast):
```
Action: com.stone.launcher.result.SET_WIFI
Extras:
  success: true
  wifi_enabled: true
```

**See TOOLS.md** for all 50+ Intent actions.

---

## AI Agent Architecture

### Agent Server (agents.js)

The AI agents run as a **separate server process** (agents.js), NOT embedded in the Android app.

**Server Setup**:
- LiveKit Agents SDK (TypeScript/Node.js server)
- Runs on a cloud server or local machine
- Launcher connects to it via LiveKit client
- Server sends tool calls → Launcher executes via Intent API → Returns results

**Launcher's Role**:
- Native Kotlin Android app
- LiveKit Android SDK client (connects to agents.js server)
- Receives tool calls from agents
- Executes via Intent API
- Returns results to agents

**TODO: Research LiveKit Android SDK integration patterns** for:
- Best practices for connecting Kotlin app to LiveKit room
- Data channel usage for tool calling (sending Intent calls, receiving results)
- Audio track management for voice input/output

### Agent Hierarchy

**Main Agent** (Runs in agents.js server):
- Always listening when Stone chat is active
- Has context of entire conversation
- Can delegate to sub-agents
- Uses dynamic tool loading

**Sub-Agents** (Also run in agents.js server):
- TICK Agent (time management tools)
- CONNECT Agent (communication tools)
- GO Agent (navigation tools)
- LISTEN Agent (Spotify tools)
- etc. (one for each of 12 apps)

### Dynamic Tool Loading Pattern

**How It Works**:

1. User asks main agent: *"Play some jazz music"*

2. Main agent calls tool: `activate_listen_agent()`
   - Returns: Full tool index for LISTEN agent
   - Tools now available: `play_music()`, `control_playback()`, `create_playlist()`, etc.

3. Main agent uses LISTEN tools to fulfill request

4. Main agent calls tool: `deactivate_listen_agent()`
   - Removes LISTEN tools from context
   - Frees up context window

### Memory Architecture

**Each sub-agent has**:
- Own memory (mem0 instance)
- App-specific context
- Access to main agent's conversation history

**Example**:
- LISTEN agent remembers user's music preferences
- PLAN agent remembers common meeting times
- CONNECT agent remembers frequent contacts

**Main agent has**:
- Overarching conversation context
- Ability to delegate to any sub-agent
- Cross-app context (e.g., "call the person I'm meeting tomorrow")

### Tool Calling Flow

```
User: "Play my workout playlist" (spoken to Stone Launcher)
  ↓
Launcher: Sends audio to agents.js server (LiveKit)
  ↓
Main Agent (in agents.js): Recognizes this needs LISTEN agent
  ↓
Main Agent: Calls activate_listen_agent()
  ↓
System: Exposes listen_* tools to agent
  ↓
Main Agent: Calls play_music(query="workout playlist")
  ↓
play_music tool: Sends Intent API call to launcher (via LiveKit data channel)
  ↓
Launcher: Executes com.stone.launcher.action.PLAY_MUSIC
  ↓
Launcher: Returns success to agents.js
  ↓
Main Agent: Calls deactivate_listen_agent()
  ↓
Main Agent: Responds to user "Playing your workout playlist"
  ↓
Launcher: Plays TTS response
```

### Agent Implementation

**Technology Stack**:
- **Server (agents.js)**: LiveKit Agents SDK (TypeScript/Node.js)
- **LLM**: OpenAI GPT-4 (or Claude)
- **STT**: Deepgram or OpenAI Whisper
- **TTS**: ElevenLabs or OpenAI TTS
- **Memory**: mem0 for context storage
- **Client (Launcher)**: LiveKit Android SDK

**See AI_AGENT_INTEGRATION.md** for complete implementation details.

---

## Technical Stack (Proposed)

### Launcher (Native Android App)
- **Language**: Kotlin
- **UI Framework**: Native Android Views (Activities, Fragments, XML layouts)
- **Design**: Clone of stone-web-app-proto (grayscale, minimalist)
- **Pattern**: Controller → Activity/Fragment + Intent API
- **Permissions**: AndroidX Permission handling
- **Storage**: SQLite (Room) + SharedPreferences
- **LiveKit**: LiveKit Android SDK (client to connect to agents.js)

**TODO: Research LiveKit Android SDK client implementation** for:
- Gradle dependency configuration
- Connection lifecycle management in Kotlin
- Audio track setup for bidirectional voice communication

### Agent Server (agents.js)
- **Language**: TypeScript (Node.js)
- **Framework**: LiveKit Agents SDK
- **LLM**: OpenAI GPT-4 / Anthropic Claude
- **STT**: Deepgram / OpenAI Whisper
- **TTS**: ElevenLabs / OpenAI TTS
- **Memory**: mem0
- **Intent Bridge**: LiveKit data channel to Launcher
- **Deployment**: Cloud server or local machine

---

## Development Approach

### Phase-Based Development (No Fixed Timeline)

Work proceeds in capability phases:

**Phase 1: Foundation**
- Intent API infrastructure (BroadcastReceiver)
- Basic WiFi/Bluetooth control (proof of concept)
- Permission management system
- App launcher (TASK app)

**Phase 2: Core Communication**
- CONNECT app (contacts, calls, SMS)
- Basic agent integration (single feature)
- Permission flows working end-to-end

**Phase 3: Essential Apps**
- TICK (time management)
- SET (settings)
- PLAN (calendar)
- Agent sub-agent delegation working

**Phase 4: Advanced Apps**
- GO (navigation)
- LISTEN (Spotify)
- THINK (notes)
- ASK (search)

**Phase 5: Polish & Advanced Features**
- REFLECT (journaling)
- FUND (wallet)
- LOOK (library)
- Notification aggregation
- Voice mode refinement

### Testing Strategy

**For Each Feature**:
- [ ] Touch UI works
- [ ] Intent API works
- [ ] Both paths use same controller
- [ ] Permissions handled correctly
- [ ] Error states graceful
- [ ] Documented in TOOLS.md

**Integration Testing**:
- [ ] Agent can control feature via Intent
- [ ] LiveKit data channel working
- [ ] Intent results returned correctly
- [ ] Sub-agent delegation functioning
- [ ] Memory persisting correctly

---

## Open Questions & Research Areas

### What We Need to Discover

1. **Grayscale Implementation**
   - Can AccessibilityService apply system-wide color filters?
   - Does this work for third-party apps?
   - Performance impact?
   - Alternative: Xposed module?

2. **Window Management**
   - Can launcher truly keep app at 2/3 + chat at 1/3?
   - Or must chat overlay app?
   - Multi-window API feasible?
   - SystemUI modification required?

3. **Call Control**
   - Can we answer/hang up calls without Accessibility hacks?
   - Telecom framework APIs sufficient?
   - What about Bluetooth headset integration?

4. **Notification Interception**
   - Is NotificationListenerService enough?
   - Can we programmatically dismiss/act on notifications?
   - Any restrictions on what actions we can perform?

5. **Third-Party App Control**
   - Can we embed Spotify and control it?
   - Permissions for app-to-app communication?
   - Alternative: OAuth + API instead of embedding?

**Approach**: Build incrementally, document limitations, propose solutions.

---

## Success Criteria

### MVP (Minimum Viable Product)
- [ ] All 12 Stone apps have basic touch UI
- [ ] Intent API working for all features
- [ ] Main agent can delegate to sub-agents
- [ ] Voice mode working (STT → LLM → Intent → TTS)
- [ ] Permission system working correctly

### Feature Complete
- [ ] All requirements from STONEOS_SPECS.md implemented
- [ ] Both touch and voice work for every feature
- [ ] Grayscale aesthetic achieved (to maximum extent possible)
- [ ] Stone icon always visible
- [ ] Notification aggregation working

### Production Ready
- [ ] All known Android versions supported (10+)
- [ ] Battery impact < 5% per day
- [ ] Crash-free sessions > 99%
- [ ] Voice latency < 500ms
- [ ] Comprehensive error handling
- [ ] User documentation complete

---

## Alternative Approaches to Consider

This architecture proposes a standalone launcher. Alternatives include:

1. **Launcher + SystemUI Modifications**
   - Launcher for apps and UI
   - SystemUI mods for grayscale, window mgmt
   - Magisk module for modifications
   - Hybrid approach

2. **Xposed/LSPosed Framework**
   - Launcher as base
   - Xposed module for system-level features
   - Hooks into Android framework
   - May not work on all devices

3. **Full AOSP Build**
   - Complete system control
   - All features achievable
   - Slower iteration
   - Device-specific

4. **Launcher + Accessibility Service**
   - Launcher for UI
   - Accessibility Service for control
   - More capabilities than standard permissions
   - May feel "hacky"

**Current Approach**: Start with #1 or #4, document limitations, escalate to other approaches as needed.

---

## Next Steps

1. **Review Requirements** - Ensure STONEOS_SPECS.md is comprehensive
2. **Validate Approach** - Confirm this architecture is worth exploring
3. **Build Foundation** - Implement TICKET_001 (Intent API)
4. **Proof of Concept** - Get one feature working end-to-end (WiFi control)
5. **Iterate** - Document what works, what doesn't, adapt approach

---

## References

- **STONEOS_SPECS.md** - Complete feature requirements
- **LAUNCHER_REQUIREMENTS.md** - Developer implementation patterns
- **TOOLS.md** - Intent API specification (50+ actions)
- **AI_AGENT_INTEGRATION.md** - Agent architecture and LiveKit integration
- **/tickets/** - Implementation tickets for each feature

---

**This architecture is a hypothesis. Developers should explore, experiment, and document findings. If this approach doesn't satisfy requirements, propose alternatives.**
