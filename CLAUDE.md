# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StoneOS is a minimalist, AI-augmented Android ROM that transforms Android into a choice-first experience where traditional apps and intelligent agents coexist seamlessly. It's built on Android 14 (AOSP) with custom SystemUI components, LiveKit-based AI agents, and Model Context Protocol (MCP) servers for third-party app integration.

**Target Device**: Google Pixel 8a (akita) with unlocked bootloader

## Repository Architecture

This is a centralized development repository containing all StoneOS components:

```
stone-os/
├── vendor/stone/                   # Stone SystemUI Java components (source of truth)
│   └── packages/SystemUI/src/com/android/systemui/stone/
│       ├── StoneManager.java      # Central manager, implements CoreStartable
│       ├── StonePanel.java        # Sliding 1/3 screen chat interface with WebView
│       └── StoneIcon.java         # Always-visible Stone icon with gesture detection
├── development/
│   └── fork-workspace/
│       └── stoneos-frameworks/    # GitHub fork of AOSP frameworks/base
├── stone-agent/                   # TypeScript LiveKit agents
├── mcp-servers/                   # Model Context Protocol servers
│   ├── spotify-mcp/
│   ├── maps-mcp/
│   ├── telephony-mcp/
│   ├── calendar-mcp/
│   └── notion-mcp/
├── scripts/
│   ├── build_stoneos.sh          # Main GCP build automation
│   └── test_emulator.sh          # Local emulator testing
└── builds/latest/                # Build outputs (SystemUI.apk)
```

## Development Philosophy

### Fork-Based AOSP Development
StoneOS uses a **forked frameworks/base** approach rather than overlays or runtime patching:
- Fork repository: https://github.com/stone-pebble/stoneos-frameworks
- Branch: `android-14.0.0_r61` (Android 14 QPR2)
- Stone components live in: `packages/SystemUI/src/com/android/systemui/stone/`
- The fork is integrated via AOSP's `.repo/local_manifests/` mechanism

### Why This Approach Works
- **Soong build system**: Android uses Soong (Android.bp), not Make
- **Parse-time evaluation**: Glob patterns in Android.bp are evaluated before build starts
- **Source tree requirement**: Custom SystemUI code must exist in the source tree before build begins
- **No runtime patches**: Device overlays only work for resources, not Java/Kotlin code

## Key Architectural Components

### 1. Stone SystemUI Components

**StoneManager.java** (`stone/StoneManager.java`)
- Central manager implementing `CoreStartable` interface for SystemUI lifecycle integration
- Uses Dagger dependency injection (`@SysUISingleton`, `@Inject`)
- Manages WindowManager lifecycle for StoneIcon and StonePanel
- Registered in `SystemUICoreStartableModule.kt` via `@Binds @IntoMap @ClassKey`
- Called by SystemUI on boot via `start()` method

**StonePanel.java** (`vendor/stone/.../StonePanel.java`) ✓ **Implemented**
- Extends FrameLayout with WebView chat interface
- Loads `http://localhost:8080/chat` with JavaScript and DOM storage enabled
- ValueAnimator-based slide animation (300ms duration)
- Animates translationY from off-screen (panelHeight) to on-screen (0)
- Methods: `show()`, `hide()`, `toggle()`, `isExpanded()`
- Occupies 1/3 of screen height when visible
- Initial state: hidden below screen

**StoneIcon.java** (`vendor/stone/.../StoneIcon.java`) ✓ **Implemented**
- Extends View with custom onDraw rendering
- Gray rounded rectangle with two white circular eyes (moai-inspired)
- GestureDetector with SimpleOnGestureListener for swipe-up detection
- Detects upward fling gestures (velocityY < -1000 px/s)
- Visual feedback: LightingColorFilter darkens on press
- 64dp × 64dp touch target (onMeasure)
- OnSwipeUpListener callback interface

### 2. Integration Flow

```
User Input → StoneIcon (swipe up) → StonePanel (shows) → WebView Chat
                                                         ↓
                                                    LiveKit Agent
                                                         ↓
                                                    MCP Servers
                                                         ↓
                                                  Android APIs
```

### 3. Stone Agent Architecture (LiveKit)

The `stone-agent/` directory contains TypeScript agents using LiveKit:
- **stone-livekit.js**: Main Stone assistant agent
- **go.js**: Maps/navigation agent
- **listen.js**: Spotify music control agent
- **ask-livekit.js**: Perplexity knowledge agent
- **think-livekit.js**: Notion note-taking agent

Each agent communicates with corresponding MCP servers to control Android functionality.

### 4. MCP (Model Context Protocol) Servers

MCP servers act as bridges between AI agents and third-party services:
- **spotify-mcp**: Playback control, playlist management
- **maps-mcp**: Navigation, location services
- **telephony-mcp**: Phone calls, SMS/RCS
- **calendar-mcp**: Event management
- **notion-mcp**: Note creation and organization

## Build System

### GCP-Based AOSP Builds

StoneOS builds run on Google Cloud Platform for speed and resource optimization:

**Instance specifications:**
- Machine type: `n2-standard-32` (32 vCPUs, 128GB RAM)
- Pricing: SPOT instances (~$0.23/hour, 70% cheaper)
- Build time: 25-35 minutes total
  - AOSP sync: ~15 minutes (rate-limited to `-j4`)
  - SystemUI build: ~10-15 minutes

**CRITICAL**: Must use `-j4` for `repo sync` to avoid HTTP 429 errors from Google's rate limiting.

### Build Commands

**Primary build workflow:**
```bash
# Full build cycle with testing
./scripts/build_stoneos.sh

# Build only, skip testing
./scripts/build_stoneos.sh --quick

# Test existing build in emulator
./scripts/build_stoneos.sh --test

# Deploy to connected device
./scripts/build_stoneos.sh --deploy

# Show cost estimates
./scripts/build_stoneos.sh --cost
```

**Manual AOSP build process (on GCP or local):**
```bash
# Initial AOSP download
mkdir ~/aosp && cd ~/aosp
repo init -u https://android.googlesource.com/platform/manifest \
  -b android-14.0.0_r61 --depth=1

# Configure local manifest to use forked frameworks/base
mkdir -p .repo/local_manifests
cat > .repo/local_manifests/stoneos.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <remove-project name="platform/frameworks/base" />
  <project name="stone-pebble/stoneos-frameworks"
           path="frameworks/base"
           remote="github"
           revision="android-14.0.0_r61" />
  <remote name="github" fetch="https://github.com/" />
</manifest>
EOF

# Sync AOSP (MUST use -j4, not higher)
repo sync -c -j4 --no-tags --no-clone-bundle --current-branch

# Build SystemUI
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng  # Android 14 - NOT trunk_staging!
m SystemUI -j16  # Adjust -j based on CPU cores

# Output location
# out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk
```

**Important build configurations:**
- **aosp_x86_64-ap2a-eng**: Android 14 QPR2 (works)
- **trunk_staging**: Google internal only, NOT in public AOSP (doesn't work)

### Syncing Stone Components to Fork

When editing Stone components in `stone/`, sync them to the fork:

```bash
# Copy changes to fork workspace
cp stone/*.java development/fork-workspace/stoneos-frameworks/packages/SystemUI/src/com/android/systemui/stone/

# Commit and push to fork
cd development/fork-workspace/stoneos-frameworks
git add -A
git commit -m "Update Stone components"
git push origin android-14.0.0_r61

# Rebuild from main repo
cd ~/stone-os
./scripts/build_stoneos.sh
```

## Stone Agent Development

**Build and run agents:**
```bash
cd stone-agent

# Install dependencies
npm install

# Build TypeScript
npm run build

# Run individual agents
npm run start:stone    # Main Stone agent
npm run start:go       # Maps agent
npm run start:listen   # Spotify agent
npm run start:ask      # Knowledge agent
npm run start:think    # Notes agent

# Run all agents concurrently
npm run start:all

# Test locally
npm test
```

**Agent dependencies:**
- `@livekit/agents`: Core LiveKit agent framework
- `@livekit/rtc-node`: Real-time communication
- LiveKit plugins: Deepgram (speech), OpenAI (LLM), Silero (VAD)

## MCP Server Development

Each MCP server is a standalone Node.js service:

```bash
# Start individual servers
cd mcp-servers/spotify-mcp && npm start
cd mcp-servers/maps-mcp && npm start
cd mcp-servers/telephony-mcp && npm start
cd mcp-servers/calendar-mcp && npm start
cd mcp-servers/notion-mcp && npm start

# Test MCP endpoints
curl -X POST http://localhost:3000/execute \
  -H "Content-Type: application/json" \
  -d '{"tool": "play_song", "params": {"query": "test"}}'
```

## Application Architecture: The "Head & Headless" Pattern

All StoneOS system applications (e.g., StoneLauncher, StoneSettings, StoneTime) follow a core architectural pattern known as "Head & Headless." This design ensures that every feature is accessible to both a human user via a graphical interface and an AI agent via a programmatic API, fulfilling our "Choice First" philosophy.

- **The "Head" (GUI Layer):** This is the standard Android `Activity` that provides the visual user interface. It is built with native Android UI components (XML layouts) and is designed with a minimalist, black-and-white aesthetic. The user interacts with this layer directly.

- **The "Headless" Layer (API Layer):** This is a `BroadcastReceiver` implemented within the same application. It listens for a specific set of custom `Intent` actions. An AI agent can trigger functionality by sending a broadcast intent with the correct action and parameters. This layer is completely independent of the UI.

Both the "Head" and the "Headless" layers call the same underlying Android system services (`WifiManager`, `AlarmManager`, etc.) to perform their actions. This ensures functional parity between the user and the agent.

### API Documentation (`TOOLS.md`)

Each application that follows this pattern must include a `TOOLS.md` file in its root directory. This file serves as the formal API documentation for the "Headless" layer, defining the `Intent` actions, required parameters (extras), and their data types, formatted for consumption by an LLM.

## Standalone App Testing Workflow

While the full StoneOS operating system requires a long build time, individual applications (`.apk` files) can be developed and tested much more rapidly. For quick UI iteration, we use a lightweight, standalone testing workflow.

1.  **Environment:** A stock Android Virtual Device (AVD) is run inside our VNC-enabled GCP environment. This AVD runs a standard, pre-built Google system image, not our custom StoneOS build.
2.  **Build the App:** The developer builds only the specific application they are working on (e.g., `m StoneSettings`). This produces a standalone `.apk` file in minutes.
3.  **Install the App:** The developer uses `adb install` to side-load the newly compiled `.apk` onto the running stock AVD.
4.  **Test the UI:** The developer can then launch the app from the stock app drawer to test its UI, layout, and basic functionality.

**Limitations:** This workflow is for **UI and non-privileged logic testing only**. The app will not have its required system-level permissions on a stock OS, so any functionality that requires privileged access (like changing Wi-Fi state) will fail. Final, comprehensive testing must still be done on a full StoneOS build.

## Device Setup (Pixel 8a)

**Unlock bootloader (WIPES DEVICE):**
```bash
adb reboot bootloader
fastboot flashing unlock
```

**Root with Magisk:**
1. Install Magisk app on device
2. Open Magisk → Install → Direct Install
3. Reboot device
4. Verify: `adb shell su -c 'whoami'` should output `root`

**Deploy SystemUI to device:**
```bash
adb root && adb remount
adb push builds/latest/SystemUI.apk /system/system_ext/priv-app/SystemUI/
adb reboot
```

## Testing & Debugging

**Monitor StoneOS logs:**
```bash
# StoneOS-specific logs
adb logcat -s StoneOS:* Stone:* MCP:* LiveKit:*

# SystemUI logs only
adb logcat -s SystemUI:* | grep StoneOS

# Window manager state
adb shell dumpsys window

# Graphics performance
adb shell dumpsys gfxinfo com.android.systemui
```

**Testing workflow:**
```bash
# Clear app data
adb shell pm clear com.stonelauncher

# Restart app
adb shell am start com.stonelauncher/.MainActivity

# Take screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png

# Check running services
adb shell dumpsys activity services | grep -i stone
```

## Critical Technical Details

### Soong Build System (Android.bp)
- SystemUI uses **Soong** build system, not Make (Android.mk)
- Glob patterns like `src/**/*.java` are evaluated at parse time
- Custom Stone files must exist in source tree before build starts
- Cannot patch Android.bp at runtime - must be in fork

### Key File Locations
- **Stone components**: `frameworks/base/packages/SystemUI/src/com/android/systemui/stone/`
- **SystemUI Android.bp**: `frameworks/base/packages/SystemUI/Android.bp`
- **Build output**: `out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk`
- **Window manager**: `frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java`
- **SurfaceFlinger**: `frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp`

### Grayscale Implementation
For system-wide grayscale in SurfaceFlinger (future implementation):
```cpp
// ITU-R BT.709 luminance coefficients
float[] mat = {
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Red
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Green
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Blue
    0, 0, 0, 1, 0                      // Alpha
}
```
Location: `frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp`

## Performance Targets

- **Voice response latency**: < 500ms
- **Chat panel animation**: 300ms
- **System grayscale overhead**: < 5%
- **GCP build time**: 25-35 minutes
- **SystemUI APK size**: ~40-50MB

## Common Issues & Solutions

### Build Issues
- **HTTP 429 on repo sync**: Always use `-j4` (not `-j8` or higher)
- **trunk_staging lunch target not found**: Use `aosp_x86_64-ap2a-eng` instead
- **Stone classes not in APK**: Run `m clean` to clear build cache, then rebuild
- **GCP instance timeout**: Startup script must touch `/tmp/ready` file
- **Circular dependency with static libraries**: DON'T create separate `StoneUI-Lib` - use standard `"src/**/*.java"` glob pattern instead

### Verifying Stone Classes in APK
After building, verify that Stone classes were compiled:

```bash
# Extract DEX files and search for Stone classes
cd /tmp
unzip -q ~/aosp/out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk classes.dex classes2.dex classes3.dex
strings classes*.dex | grep -E "StoneManager|StoneIcon|StonePanel"
```

Expected output should include:
- `Lcom/android/systemui/stone/StoneManager;`
- `Lcom/android/systemui/stone/StoneIcon;`
- `Lcom/android/systemui/stone/StonePanel;`
- Log strings like "StoneOS: Initializing StoneManager"

### What Doesn't Work
- **Device overlays for Java code**: Only work for resources (XML), not code
- **Runtime Android.bp patching**: Too fragile, use fork instead
- **Separate static libraries for Stone components**: Creates circular dependencies (Stone needs SystemUI-core classes, SystemUI-core would need Stone library)
- **Android.mk for SystemUI**: SystemUI uses Soong (Android.bp)
- **Building on macOS**: Use GCP Linux instance for full AOSP builds

## Development Workflow Summary

1. **Edit Stone components** in `vendor/stone/packages/SystemUI/src/com/android/systemui/stone/`
2. **Copy to AOSP** in `~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/`
3. **Build SystemUI**: `cd ~/aosp && source build/envsetup.sh && lunch aosp_x86_64-ap2a-eng && m SystemUI -j16`
4. **Verify build**: Check classes in APK with DEX inspection
5. **Deploy to device**: `adb root && adb remount && adb push <apk> /system/system_ext/priv-app/SystemUI/ && adb reboot`
6. **Test and iterate** based on logs: `adb logcat -s StoneOS:* SystemUI:*`

## Current Implementation Status

### Phase 1: Core SystemUI ✓ (Complete)
- ✓ StoneManager implementing CoreStartable interface
- ✓ Dagger dependency injection registration
- ✓ WindowManager integration for StoneIcon and StonePanel
- ✓ Build system verification (glob pattern approach)
- ✓ Stone classes successfully compiled into SystemUI.apk

### Phase 2: UI Components ✓ (Complete)
- ✓ StoneIcon with GestureDetector for swipe-up detection
- ✓ StoneIcon custom drawing (gray stone with eyes)
- ✓ StonePanel with WebView loading http://localhost:8080/chat
- ✓ Panel slide animation with ValueAnimator (300ms)
- ✓ OnSwipeUpListener wired from StoneIcon to StonePanel.toggle()
- ✓ 64dp touch target on StoneIcon
- ✓ Visual press feedback with ColorFilter
- ✓ Build verified: all classes present in SystemUI.apk

### Phase 3: Device Testing & System Integration (Next)
- Deploy SystemUI.apk to device/emulator
- Test swipe-up gesture triggers panel
- Verify panel animation smoothness (300ms)
- Test WebView chat interface connectivity
- Add system-wide grayscale filter in SurfaceFlinger
- Implement window manager modifications for 2/3 app view
- Set up broadcast receiver for panel resize events
- Test with real Android apps (Spotify, Maps, etc.)

### Phase 4: Agent & MCP Integration (Future)
- Deploy LiveKit agents and connect to StonePanel
- Set up MCP servers for Spotify, Maps, Telephony
- Implement notification aggregation system
- Build unlock screen with AI-generated summaries

## Important Documentation

- **CLAUDE.md** (this file): Claude Code implementation guide
- **GEMINI.md**: Technical guidance for the Architect Agent (Gemini) - AOSP build system specifics, SystemUI architecture
- **README.md**: Repository overview and quick start
- **Fork repository**: https://github.com/stone-pebble/stoneos-frameworks
