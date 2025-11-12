# StoneOS - Feature & User Experience Specifications

## Executive Summary

StoneOS is a minimalist, AI-augmented Android experience that seamlessly integrates third-party apps with intelligent agents. It's not voice-first, but rather a simplified mobile OS where every app can be controlled equally well through traditional touch interfaces or conversational AI assistance.

## Core Philosophy

**Minimalist Design with Maximum Capability**
- Clean, grayscale aesthetic with no visual clutter
- Real Android apps embedded when needed
- AI assistant available but not intrusive
- User chooses interaction method moment-by-moment

---

## Architectural Decisions & Current Status
**NOTE:** This section documents the foundational technical decisions that have been made based on extensive testing and research. It serves as a guide to the current, validated development path.

### AOSP & Environment
*   **AOSP Base:** `android-14.0.0_r61`
*   **Build Environment:**
    *   **Host Server:** Google Cloud Platform (GCP) `n-standard-32` VM.
    *   **Host OS:** Ubuntu 24.04 LTS.
    *   **Analysis:** The modern host OS has been a source of toolchain incompatibility with the older AOSP branch, particularly for virtualization tools.
*   **Target Hardware:** Google Pixel 8a (codename: "akita").
*   **Target Architecture:** `ARM64`.

### Deployment Strategy: Physical Device Flashing
*   **Conclusion:** Virtualization is **not a viable test environment** for this project's specific configuration (AOSP version + Host OS).
*   **Analysis Summary:**
    *   **Standard Android Emulator:** Failed due to lack of KVM (hardware acceleration) support on the GCP VM, making it too slow to boot.
    *   **Cuttlefish Emulator:** Failed due to a fundamental incompatibility between the Android 14-era `crosvm` (the virtual machine monitor) and the modern Ubuntu 24.04 host kernel. This was proven by testing both AOSP-built tools and official Google pre-built binaries, both of which crashed with the same low-level error (`failed to create a PCI root hub`). All testing and verification must be performed on physical hardware.

### Build & Deployment Procedure (Validated)
*   **Build Command:** `m` followed by `m dist`.
    *   **Analysis:** The `m dist` command is a required post-build step. It packages all dynamic partitions (`system`, `vendor`, `product`, etc.) into a single, flashable `super.img`. Failure to generate this image was the root cause of a previous flashing failure.
*   **Flashing Command:** `fastboot flash super super.img`.
    *   **Analysis:** Flashing the `super.img` is the only supported method for updating dynamic partitions on the target hardware. Attempting to flash `system.img` individually from the bootloader will fail.

---

## User Interface Architecture
**Status:** Implemented

### Layout Structure
```
Normal State:
┌─────────────────────────┐
│                         │
│                         │
│   Full Screen App       │  ← Actual Android app (grayscale)
│                         │
│                         │
└─────────────────[🗿]────┘  ← Stone icon at bottom

Chat Active (after swipe up):
┌─────────────────────────┐
│                         │
│   Embedded App (2/3)    │  ← App continues running
│                         │
├─────────────────────────┤
│   Chat Interface (1/3)  │  ← Slides up from bottom
└─────────────────────────┘
```

### Chat Interface Behavior

**Access Method**: 
- **Stone Icon** (🗿) always visible at bottom of screen
- **Swipe up** from Stone icon to reveal chat (takes bottom 1/3)
- **Swipe down** to hide chat and return to full-screen app

**Interaction Modes**:
1. **Initial state**: Opens in text mode when swiped up
2. **Text mode**: 
   - User types messages
   - Sees AI text responses
   - Can read full conversation history
3. **Voice mode** (tap microphone icon to activate):
   - NO transcription shown while speaking (prevents distraction)
   - Stone icon glows when AI is responding
   - Pure voice conversation without text
   - User focuses on natural speech, not watching text

### Camera Access

**Not an app, but a swipe gesture**: Swipe right from home screen
**Implementation**: Stock Android camera
**Storage**: Google Photos integration
**Modifications**: None - uses existing Android camera system as-is
**Grayscale**: No - camera viewfinder and photos remain in color

## App Specifications

### 1. LISTEN - Music Control
**Status:** Planned
**Third-Party App**: Spotify (Android app from Play Store)
**Display**: Grayscale Spotify app in top 2/3
**Why Not Web**: Need actual app for audio streaming services quality
**Agent Integration**: Spotify MCP for control
**Capabilities**:
- Play songs, build playlists, search music
- Control playback via chat
- Use existing Spotify developer MCP
**Open Question**: Authentication method for Spotify app integration?

### 2. GO - Navigation
**Status:** Planned
**Implementation**: Google Maps API (not third-party app)
**Display**: Custom-built map interface using Google Maps JavaScript API streaming
**Agent Integration**: Custom MCP for map manipulation
**Capabilities**:
- Navigate to destinations
- Search places
- Get directions
- Agent can manipulate map elements directly

### 3. ASK - Knowledge & Limited Browsing
**Status:** Planned
**Third-Party App**: Perplexity (Android app)
**Display**: Grayscale Perplexity app with images remaining in color
**Browsing Philosophy**: 
- Internet access throttled through LLMs to steer toward non-harmful content
- No open web browsing or Google search
- Can follow links from Perplexity to specific pages only
**Capabilities**:
- Perplexity-style search with citations
- Image results shown in color (exception to grayscale)
- Click links to view specific pages
- Temporary browser viewer for linked content only

### 4. TASK - MCP Discovery & Permitted Apps
**Status:** Planned

#### MCP Discovery Section
**Purpose**: StoneOS version of app store for agent capabilities
**Display**: MCP library/discovery window
**Functionality**:
- Browse available MCPs
- Install/authenticate MCPs
- Manage agent capabilities
- "Agentic browser for the agentic internet"

#### Permitted Apps Section
**Purpose**: Access to essential third-party apps
**Display**: Flat list showing only app names (no icons)
**Behavior**:
- These apps run full-screen when opened
- Stone icon still accessible at bottom
- Stone can be summoned but won't know app contents
- Stone only knows which app is being used
- Notifications from these apps feed into unlock screen summaries
**App Discovery**:
- Users find these apps on regular Android app store
- Future: Approval/curation process (not needed now)

### 5. SET - System Settings & Identity
**Status:** Implemented (StoneSettings app); Agent integration is Planned

#### Settings Control
**Purpose**: Phone settings control
**Display**: System settings interface
**Agent Integration**: Expose settings for manipulation via tool calls
**Capabilities**:
- Adjust all phone settings
- Voice-controlled configuration
- System-level access with root

#### Identity Section
**Purpose**: Two-factor authentication management
**Functionality**:
- Users can install any 2FA app from Android App Store
- Apps like Google Authenticator, Authy, Microsoft Authenticator
- Provides secure login capability for all services
- 2FA apps remain accessible even in minimalist interface

### 6. TICK - Time Management
**Status:** Implemented (StoneTime app); Agent integration is Planned
**Purpose**: Clock, timer, stopwatch, alarms
**Display**: Custom time interface (already built in UI)
**Agent Integration**: MCP for time manipulation
**Voice Commands Examples**:
- "Set a timer for three hours"
- "Set an alarm for 6:00 AM"
- "Start my stopwatch"
- "What time is it in Tokyo?"

### 7. LOOK - Digital Library
**Status:** Planned (Stretch Goal)
**Purpose**: Public domain book reader
**Content Source**: Project Gutenberg database
**Implementation**: AI agent crawls Project Gutenberg, builds searchable database
**Agent Integration**: 
- Search and recommend books
- Find specific books
- Navigate to pages
- Track reading progress
- Make recommendations based on reading history
**Note**: Stretch goal but highly desired feature

### 8. PLAN - Calendar
**Status:** Planned
**Third-Party App**: Google Calendar (initially)
**Future Options**: Notion, Outlook, others
**Display**: Grayscale Google Calendar app
**Agent Integration**: Calendar MCP for event management
**Capabilities**:
- Create, edit, delete events
- Check availability
- Set reminders
- Schedule meetings

### 9. THINK - Notes
**Status:** Planned
**Third-Party App**: Notion
**Display**: Grayscale Notion app
**Agent Integration**: Notion MCP (will continue to evolve)
**Capabilities**:
- Create, update, edit notes via conversation
- Search through notes
- Organize information
- Agent can make notes based on conversations

### 10. CONNECT - Unified Communications Hub
**Status:** Planned
**Purpose**: All communications in one interface
**Complexity**: Most challenging app to implement

#### Core Functionality:
**Native Phone Features**:
- Calling and texting (RCS preferred over SMS on newer hardware/Android 15)
- AI call screening:
  - Unrecognized contacts → AI answers first
  - Spam/marketing calls → auto-block number
  - Real callers → "I'm the helpful AI Stone Assistant. I will hand you off to the user if they can pick up"
- Text message management with AI assistance

**Third-Party Integration**:
- Initial apps: Slack, Teams, Gmail, Outlook, Yahoo Mail
- Future: MCP-only without downloading apps
- Unified UI sorted by person (not by platform)
- Source of truth: Contacts database

**Contact-Centric View**:
- All communications with a person in one thread
- Example: Mom → texts + emails together
- Example: Coworker → Slack + email + phone number
- Encourages building out complete contact information

**Implementation Approach**:
- V0: Download select communication apps
- Custom UI overlays for calls/texts
- Future: Pure MCP integration without apps

### 11. FUND - Payments & Banking
**Status:** Planned
**Primary Function**: Android native Wallet/Payments
**Display**: Grayscale Android Wallet
**AI Interaction**: None - no AI involvement with payments for security
**Payment Process**:
- Click Fund → opens wallet
- Select card or use default
- Make payment via tap or selection
- Double-tap activation for quick pay

**Banking Apps** (accessible by scrolling past wallet):
- Payment apps: PayPal, Venmo, Cash App, Zelle
- Major banks: Wells Fargo, Wealthfront, Fidelity, Chase, Bank of America
- All displayed in grayscale for consistency
- No AI access to financial data

### 12. REFLECT - AI Life Journal
**Status:** Planned
**Purpose**: Automatic daily reflection and journaling
**Unique Feature**: AI has complete logs of everything user does on phone
**Data Sources for Reflection**:
- Every text message sent/received
- All communications across platforms
- Apps used and duration
- Calls made and received
- Notes taken
- Calendar events attended
- Music listened to
- Places visited

**Configuration Options**:
- User sets "end of day" time in SET app
- Choose trigger time for reflection
- **Privacy controls in SET**:
  - Turn off Reflect functionality completely
  - Delete specific days/logs
  - Choose what to include/exclude from logging

**AI-Generated Journal Content**:
- Comprehensive daily log
- Analysis of interactions
- Patterns and insights
- Emotional tone of communications
- Productivity metrics

**User Interaction**:
- Review AI-generated journal entry
- Discuss specific interactions with agent
- Add personal thoughts and annotations
- Ask agent about patterns over time
- Contextual analysis of activities

**Data Management**:
- Users can delete any logs at any time
- Option to disable all logging in SET
- Clear data retention policies

## Unlock Screen & Notification System

### The Unlock Experience
**Not a notification list, but a personalized message**

Instead of traditional notifications, users see a conversational summary written as if by a helpful human. The Stone writes markdown that naturally incorporates what matters while ignoring what doesn't.

**Example Unlock Screen**:
```markdown
you've been working on the stone prototype for 6 hours. 
good progress on the interface design.

---

• design system documentation completed
• new ai vision feature proposed by gus and william
• scheduled demo with investors tomorrow at 11

---

evelyn's heading to the coffee shop near you.
take a walk.

---

*63° and falling - sunset at 7:07*
```

### Philosophy
- **Human-like communication**: Not sorted by apps or groups
- **Contextual awareness**: Knows what's important to you
- **Intelligent filtering**: Ignores spam and irrelevant notifications
- **Natural language**: Reads like a friend's note, not a system alert

### Technical Implementation
- **NotificationListenerService**: Captures all system notifications
- **AI Processing**: Converts raw notifications into human narrative
- **Smart Filtering**: Decides what to include/exclude
- **Markdown Rendering**: Clean, readable presentation

### Notification Sources
- All system notifications
- Permitted apps from TASK
- System events (battery, updates, etc.)
- Communications (calls, texts, emails)
- Calendar reminders

### User Control
- Swipe up from unlock screen → Home
- Swipe right → Camera
- Swipe left → Stone assistant
- Tap anywhere → Stone assistant

## System Integration Requirements

### Root Access Enables:
1. **App Embedding**: Display real Android apps in 2/3 screen
2. **System Control**: Direct hardware/settings access
3. **Full Telephony**: Complete phone/SMS/RCS control
4. **Notification Listener**: System-level notification access
5. **Accessibility Override**: Control any app programmatically
6. **Deep App Integration**: Access app internals for MCP control

### Visual Design Principles
1. **Grayscale Everything**: All apps and UI in grayscale
2. **Exceptions**: 
   - Camera viewfinder and photos (full color)
   - Perplexity search result images (full color)
3. **Minimalist Home**: No app grid or icons
4. **Clean Typography**: Simple, readable fonts
5. **Subtle Animations**: Smooth but not distracting
6. **No Theme Expansion**: Grayscale is the only theme

## User Experience Flow

### Interaction Model
Users can choose between:
1. **Traditional Touch**: Tap and use apps normally
2. **AI Chat**: Conversational control via Stone agent
3. **Mixed Mode**: Switch between both seamlessly

### Agent Integration Philosophy
- Agent sees and controls all embedded apps
- Complete logs of user activity for reflection
- Contextual awareness across all apps
- Seamless handoff between manual and AI control
- User always maintains control over interaction method

## Technical Implementation Notes

### Priority Order
1. **Core Communications**: Connect app (phone/text) - vitally important
2. **Essential Daily**: Listen (Spotify), Go (Maps), Tick (time)
3. **Productivity**: Plan (Calendar), Think (Notes), Task (MCP store)
4. **Information**: Ask (Perplexity), Set (Settings + Identity)
5. **Financial**: Fund (Payments + Banking)
6. **Advanced**: Look (Books - stretch goal), Reflect (Journal)

### Critical Success Factors
1. **Seamless app embedding** with root access
2. **Reliable MCP bridges** for each service
3. **Smooth voice/text mode switching** without transcription distraction
4. **Fast agent response times** (< 500ms)
5. **Consistent grayscale aesthetic** with minimal exceptions
6. **Robust notification aggregation** system

## Open Questions & Considerations

1. **Spotify Authentication**: How to authenticate agent into Spotify app?
2. **RCS Availability**: Will RCS be available on target hardware with Android 15?
3. **MCP Discovery**: How to build and maintain MCP marketplace?
4. **Banking Security**: Compliance requirements for financial apps?

## Future Notes

- **Agent Behavior Specification**: Need separate documentation for how Stone agent personality and interaction patterns work
- **MCP Registry**: Build out discovery and approval process for third-party MCPs
- **Privacy Framework**: Detailed policies for data retention and user control

## Success Metrics

### User Experience
- Time to complete tasks vs traditional Android
- Frequency of AI vs manual interaction choice
- User satisfaction with minimalist design
- Reduction in app-switching time
- Notification management effectiveness

### Technical
- Agent response latency < 500ms
- App embedding stability
- MCP integration reliability
- Voice recognition accuracy without transcription distraction
- Notification aggregation accuracy

## Summary

StoneOS transforms Android into a minimalist, AI-augmented experience where traditional apps and intelligent agents coexist seamlessly. Users maintain full control over their interaction method while benefiting from deep AI integration across all functions. The system prioritizes simplicity, privacy, and user agency while delivering powerful capabilities through conversational AI.

The key innovation is not replacing touch with voice, but rather creating a unified experience where both interaction methods are first-class citizens, working together to amplify human capability without adding complexity. Every design decision—from hiding voice transcription to grayscaling apps—serves the goal of reducing cognitive load while maintaining full functionality.

The notification aggregation system and permitted apps list ensure users can still access everything they need while maintaining the minimalist aesthetic and AI-enhanced experience that defines StoneOS.