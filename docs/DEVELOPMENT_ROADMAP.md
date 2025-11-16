# Stone Launcher - Development Roadmap

**Version**: 1.0
**Last Updated**: November 13, 2025

---

## Overview

This roadmap outlines the development sequence for Stone Launcher, a native Kotlin Android application with AI agent integration. The approach is **UI-first**, building and testing features visually before adding AI capabilities.

**Core Principle**: Clone the web prototype design, then layer in LiveKit chat and AI agent tool calling.

---

## Phase 1: Native Kotlin UI Foundation

**Goal**: Build the home screen and basic launcher functionality matching the web prototype design.

**Priority**: CRITICAL - Everything depends on this foundation.

### 1.1 Project Setup
- [x] Create Android project structure
- [x] Configure Gradle with Kotlin
- [x] Set up grayscale theme
- [x] Configure target SDK 34 (Android 14)
- [x] Set up version control

### 1.2 Home Screen (Clone Web Prototype)
- [ ] Create MainActivity as home screen
- [ ] Implement 3x4 app grid (text-only, no icons)
- [ ] Apply grayscale design (#000000 background, #FFFFFF text)
- [ ] Add Stone icon at bottom of screen
- [ ] Implement swipe-right for camera
- [ ] Test on real device (Pixel 8a)

**Deliverables**:
- Home screen matches web prototype design
- App grid displays installed apps
- Navigation gestures work smoothly

**Reference**: `/stone-web-app-proto/` for design

---

## Phase 2: Intent API Foundation

**Goal**: Create the "headless" control layer for AI agents.

**Priority**: CRITICAL - Enables all AI features.

### 2.1 Intent API Infrastructure
- [x] Create BroadcastReceiver (StoneApiReceiver)
- [x] Implement Intent routing
- [x] Create result broadcast system
- [x] Add error handling
- [x] Create test scripts

**Status**: ✅ COMPLETED (TICKET_001)

### 2.2 Reference Implementation (WiFi Controller)
- [ ] Create WifiController in `/controllers/`
- [ ] Implement HEAD interface (SettingsActivity)
- [ ] Implement HEADLESS interface (Intent handler)
- [ ] Verify both paths use same controller
- [ ] Test via touch UI and adb commands

**Deliverables**:
- WiFi control works via touch
- WiFi control works via Intent API
- Pattern documented for future features

**Reference**: `/docs/LAUNCHER_REQUIREMENTS.md` for "Head & Headless" pattern

---

## Phase 3: Core Stone Apps (Touch UI)

**Goal**: Build the 12 Stone apps with touch UI only (no AI yet).

**Priority**: HIGH - Core user experience.

### 3.1 Essential Apps (Touch UI Only)
Build these apps with native Kotlin views:

1. **TASK** - App Launcher
   - Display all installed apps
   - Launch apps on tap
   - Search/filter functionality

2. **SET** - Settings
   - WiFi toggle
   - Bluetooth toggle
   - Brightness slider
   - Volume controls

3. **TICK** - Time Management
   - Set alarms
   - Create timers
   - Stopwatch
   - World clock

4. **CONNECT** - Communications
   - Contacts list
   - Dialer
   - SMS interface
   - Email (read-only)

### 3.2 Secondary Apps (Touch UI Only)

5. **PLAN** - Calendar & Goals
   - Calendar view
   - Create/edit events
   - Goal tracking

6. **GO** - Navigation
   - Google Maps integration
   - Location search
   - Start navigation

7. **LISTEN** - Music
   - Spotify authentication
   - Play/pause/skip controls
   - Playlist management

8. **THINK** - Notes
   - Create/edit notes
   - Tag system
   - Search notes

### 3.3 Advanced Apps (Touch UI Only)

9. **ASK** - Knowledge/Search
   - Web search interface
   - Perplexity integration
   - Limited browsing

10. **REFLECT** - Activity Journal
    - Activity timeline
    - Daily summaries
    - Notification aggregation

11. **FUND** - Wallet
    - Android Pay access
    - Banking app shortcuts

12. **LOOK** - Digital Library
    - Project Gutenberg integration
    - Book reader

**Deliverables**:
- All 12 apps functional via touch
- Controllers created for business logic
- Permissions handled correctly
- Design matches web prototype

**Timeline**: 4-6 weeks (each app ~2-3 days)

---

## Phase 4: LiveKit Android SDK Integration

**Goal**: Add chat interface that connects to agents.js server.

**Priority**: HIGH - Enables AI interaction.

**TODO: Research LiveKit Android SDK integration patterns** before starting this phase:
- Gradle dependency configuration and version compatibility
- Room connection lifecycle in Android/Kotlin
- Audio track management for bidirectional voice
- Data channel usage for tool calling
- Best practices for error handling and reconnection

### 4.1 LiveKit Client Setup
- [ ] Add LiveKit Android SDK dependency
- [ ] Create ChatFragment
- [ ] Implement connection to agents.js server
- [ ] Set up audio tracks for voice
- [ ] Create chat UI (1/3 screen overlay)

### 4.2 Chat UI Implementation
- [ ] Design chat message layout (grayscale)
- [ ] Implement swipe-up gesture to show chat
- [ ] Add Stone icon glow animation
- [ ] Create voice indicator
- [ ] Test audio streaming to server

**Deliverables**:
- Chat UI overlay working
- Audio streaming to agents.js
- Receiving TTS responses from agents.js
- Gesture control for showing/hiding chat

**Reference**: LiveKit Android SDK documentation

---

## Phase 5: Agent Server (agents.js) Setup

**Goal**: Create the TypeScript server that runs AI agents.

**Priority**: HIGH - Required for tool calling.

### 5.1 LiveKit Agents SDK Server
- [ ] Initialize Node.js project
- [ ] Install LiveKit Agents SDK
- [ ] Configure OpenAI/Claude integration
- [ ] Set up Deepgram STT
- [ ] Set up ElevenLabs TTS
- [ ] Create main agent logic

### 5.2 Tool Calling Bridge
- [ ] Implement data channel listener
- [ ] Create Intent API bridge
- [ ] Map tool calls to Intent actions
- [ ] Return Intent results to agents
- [ ] Error handling and retries

**Deliverables**:
- agents.js server running
- Voice conversation working end-to-end
- Tool calls sent to launcher
- Results returned to agents

**Reference**: `/docs/AI_AGENT_INTEGRATION.md`

---

## Phase 6: Tool Calling API Implementation

**Goal**: Connect agent tool calls to Intent API handlers.

**Priority**: HIGH - Enables AI control.

### 6.1 Intent API Handlers for All Apps
For each Stone app, create Intent handlers:
- [ ] TASK app handlers (launch_app, list_apps)
- [ ] SET app handlers (set_wifi, set_bluetooth, etc.)
- [ ] TICK app handlers (set_alarm, start_timer, etc.)
- [ ] CONNECT app handlers (make_call, send_sms, etc.)
- [ ] PLAN app handlers (create_event, etc.)
- [ ] GO app handlers (start_navigation, etc.)
- [ ] LISTEN app handlers (play_music, etc.)
- [ ] THINK app handlers (create_note, etc.)
- [ ] ASK app handlers (web_search, etc.)
- [ ] REFLECT app handlers (get_activity_summary, etc.)
- [ ] FUND app handlers (open_wallet, etc.)
- [ ] LOOK app handlers (open_book, etc.)

### 6.2 Tool Index Creation
- [ ] Document all tools in agents.js
- [ ] Implement dynamic tool loading
- [ ] Create sub-agents for each app
- [ ] Test tool activation/deactivation

**Deliverables**:
- All 50+ Intent actions documented
- Each action has corresponding tool in agents.js
- Sub-agents can be activated/deactivated
- Voice commands control all launcher features

**Reference**: `/docs/TOOLS.md` for complete Intent API

---

## Phase 7: Advanced Features & Polish

**Goal**: Add advanced UX features and optimize performance.

**Priority**: MEDIUM - Nice to have.

### 7.1 Notification Aggregation
- [ ] Implement NotificationListenerService
- [ ] Create AI summary generation
- [ ] Design unlock screen narrative
- [ ] Markdown rendering for summaries

### 7.2 Voice Mode Refinement
- [ ] Hide transcription during user speech
- [ ] Implement Stone icon glow animation
- [ ] Optimize audio latency
- [ ] Add voice activity detection

### 7.3 Grayscale System-Wide
- [ ] Investigate AccessibilityService approach
- [ ] Test system-wide color filter
- [ ] Add exceptions (camera, photos)
- [ ] Measure performance impact

### 7.4 Window Management
- [ ] Implement 2/3 app + 1/3 chat split
- [ ] Test multi-window API
- [ ] Handle edge cases (full-screen apps)

**Deliverables**:
- Notification summaries working
- Voice mode polished
- Grayscale applied system-wide (if possible)
- Window split working

---

## Phase 8: Testing & Optimization

**Goal**: Ensure production-quality reliability.

**Priority**: MEDIUM - Before public release.

### 8.1 Automated Testing
- [ ] Unit tests for all controllers
- [ ] Integration tests for Intent API
- [ ] UI tests with Espresso
- [ ] Voice interaction tests

### 8.2 Performance Optimization
- [ ] Profile battery usage
- [ ] Optimize memory footprint
- [ ] Reduce voice latency
- [ ] Test on multiple devices

### 8.3 Error Handling & Edge Cases
- [ ] Handle permission denials gracefully
- [ ] Test offline behavior
- [ ] Test with no Internet connection
- [ ] Handle agent server downtime

**Deliverables**:
- Test coverage > 80%
- Battery impact < 5% per day
- Voice latency < 500ms
- Crash-free sessions > 99%

---

## Success Metrics by Phase

### Phase 1 Success (UI Foundation)
- [ ] Home screen displays correctly
- [ ] App grid shows installed apps
- [ ] Grayscale aesthetic applied
- [ ] Stone icon visible at bottom

### Phase 2 Success (Intent API)
- [ ] Can send Intent via adb
- [ ] Receives result broadcasts
- [ ] WiFi controller works both ways
- [ ] Pattern documented

### Phase 3 Success (Stone Apps)
- [ ] All 12 apps functional via touch
- [ ] Permissions handled correctly
- [ ] Controllers created for each app
- [ ] Design matches web prototype

### Phase 4 Success (LiveKit Client)
- [ ] Chat UI overlay working
- [ ] Audio streaming to agents.js
- [ ] Receiving TTS from agents.js
- [ ] Gesture control working

### Phase 5 Success (Agent Server)
- [ ] agents.js server running
- [ ] Voice conversation working
- [ ] Tool calls sent to launcher
- [ ] Results returned to agents

### Phase 6 Success (Tool Calling)
- [ ] All Intent handlers implemented
- [ ] Voice commands control all features
- [ ] Sub-agents working
- [ ] Dynamic tool loading working

### Phase 7 Success (Advanced)
- [ ] Notification summaries working
- [ ] Voice mode polished
- [ ] Grayscale system-wide (if possible)
- [ ] Window split working

### Phase 8 Success (Production Ready)
- [ ] All tests passing
- [ ] Performance targets met
- [ ] Error handling comprehensive
- [ ] Ready for beta testing

---

## Development Principles

### UI-First Approach
Build features with touch UI first, then add AI control. This ensures:
- Visual feedback during development
- Users can fall back to touch
- Easier debugging and testing
- Matches "choice-first" philosophy

### Single Source of Truth
Every feature has ONE controller that's called by:
- Touch UI (Activities/Fragments)
- Intent API (BroadcastReceiver)

### Test as You Build
- Test touch UI immediately
- Test Intent API with adb
- Test AI integration incrementally
- Don't build everything before testing

### Clone, Then Enhance
- Start by cloning web prototype design
- Match grayscale aesthetic exactly
- Then add LiveKit and AI features
- Don't reinvent the visual design

---

## Timeline Estimates

**Note**: These are rough estimates. Actual timelines depend on developer experience and blockers.

- **Phase 1** (UI Foundation): 1 week
- **Phase 2** (Intent API): 1 week (✅ COMPLETED)
- **Phase 3** (Stone Apps): 4-6 weeks
- **Phase 4** (LiveKit Client): 1 week
- **Phase 5** (Agent Server): 2 weeks
- **Phase 6** (Tool Calling): 2-3 weeks
- **Phase 7** (Advanced): 2-3 weeks
- **Phase 8** (Testing): 2 weeks

**Total**: ~15-20 weeks (3.5-5 months)

---

## Critical Path

These items MUST be completed in order:

1. Phase 1: UI Foundation → **Blocks all UI work**
2. Phase 2: Intent API → **Blocks all AI work** (✅ COMPLETED)
3. Phase 3: Stone Apps → **Blocks tool calling**
4. Phase 4: LiveKit Client → **Blocks agent integration**
5. Phase 5: Agent Server → **Blocks voice control**
6. Phase 6: Tool Calling → **Blocks full AI experience**

Phases 7-8 can happen in parallel with later stages of Phase 6.

---

## Next Steps (Immediate)

### For Developers Starting Now:

1. **Read Documentation**:
   - `/docs/LAUNCHER_ARCHITECTURE.md`
   - `/docs/LAUNCHER_REQUIREMENTS.md`
   - `/STONEOS_SPECS.md`

2. **Set Up Environment**:
   - Install Android Studio
   - Clone repository
   - Build project: `./gradlew assembleDebug`

3. **Start Phase 1.2**:
   - Create MainActivity home screen
   - Implement app grid
   - Apply grayscale theme
   - Test on device

4. **Reference Web Prototype**:
   - Study `/stone-web-app-proto/` design
   - Match visual design exactly
   - Use same layout patterns

---

## Questions or Issues?

- **Architecture Questions**: See `/docs/LAUNCHER_ARCHITECTURE.md`
- **Implementation Patterns**: See `/docs/LAUNCHER_REQUIREMENTS.md`
- **Feature Specifications**: See `/STONEOS_SPECS.md`
- **Intent API Reference**: See `/docs/TOOLS.md`
- **Create Ticket**: If you discover new work needed

---

## Success Criteria (Final)

Stone Launcher is complete when:

- [ ] All 12 Stone apps work via touch
- [ ] All 12 Stone apps work via voice
- [ ] agents.js server deployed and stable
- [ ] Voice latency < 500ms
- [ ] Battery impact < 5% per day
- [ ] Grayscale aesthetic fully implemented
- [ ] Stone icon always visible
- [ ] Chat overlay working smoothly
- [ ] Test coverage > 80%
- [ ] Ready for user beta testing

---

**Remember**: UI first, then AI. Clone the web prototype, then enhance with intelligence.
