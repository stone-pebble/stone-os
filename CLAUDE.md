# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StoneOS is a minimalist, AI-augmented Android experience that seamlessly integrates third-party apps with intelligent agents. It's not voice-first, but **choice-first**: Users can interact via touch OR conversational AI, switching seamlessly between both.

**CRITICAL: We are using OPTION C - SystemUI Modification**
- NOT just making an app - we are CHANGING THE ANDROID OS
- Modifying `/system_ext/priv-app/SystemUI/SystemUI.apk` directly
- This gives us 90% of vision without full AOSP build
- Device: Pixel 8a (bootloader UNLOCKED, ready for OS modification)

**What Option C Achieves:**
- True 2/3 app + 1/3 chat split (via SystemUI window management)
- System-level grayscale (not overlay)
- Native gesture control (swipe-up from Stone icon)
- Actual notification aggregation (not hacky listener)

**AOSP Status**: Downloaded to `/Volumes/StoneOS_SSD/aosp/` (backup only, NOT primary approach)

## Architecture

### Core Components Stack (OPTION C)
```
Modified SystemUI.apk (Controls window management + system panels)
↓
Stone Launcher (React Native, replaces home screen)
↓
MCP Bridge Services (Control apps via accessibility + root)
↓ 
LiveKit Agent (Local voice processing)
↓
Stock Android + Root (No AOSP build needed)
```

### Key Implementation Steps
1. **Extract SystemUI.apk** from device
2. **Decompile and modify** for Stone panel + grayscale
3. **Replace SystemUI** using root access
4. **Install Stone launcher** as system app
5. **Deploy MCP bridges** for app control

### SystemUI Modifications Required
- **StonePanel.java**: New system panel for chat (1/3 screen)
- **WindowManager**: Force apps to 2/3 height when chat active
- **NotificationManager**: Aggregate into Stone summaries
- **ColorMatrixFilter**: System-wide grayscale
- **GestureDetector**: Swipe-up from Stone icon

## Development Environment Requirements

### Hardware Requirements
- **RAM**: 32GB minimum (AOSP builds are memory-intensive)
- **Storage**: 400GB+ free space (multiple AOSP variants, build artifacts)
- **Target Device**: Google Pixel 8a (primary development device)

### Software Stack
- **AOSP**: Android 14+ build environment
- **Languages**: Kotlin (MCP service), JavaScript/React (UI), Python (agents)
- **Build System**: AOSP build system + patch management tooling
- **Voice Processing**: LiveKit for real-time audio
- **AI Integration**: Anthropic's Model Context Protocol

## Key Technical Concepts

### Master Control Program (MCP)
Native Android service that provides unified API layer for:
- App state management and control
- System service integration
- Permission and security enforcement
- Agent communication interface

### Patch System
Maintainable AOSP customization approach:
- Core modifications as discrete patches
- Automated patch application and validation
- Enables clean AOSP security updates
- Version control for system modifications

### Agent Architecture
AI agents handle user intents through:
- Voice command processing via LiveKit
- Context-aware task execution
- Multi-agent orchestration for complex tasks
- Integration with third-party services (Spotify, Maps, etc.)

## Repository Structure

### Current Implementation Status
The project has transitioned from documentation to active development. Core components are being built following the Option C approach (SystemUI modification).

### Active Development Files
```
/
├── SystemUI_original.apk      # Extracted from Pixel 8a
├── SystemUI_decompiled/        # Decompiled SystemUI for modification
├── systemui-mod.sh            # Script to modify SystemUI
├── install-systemui.sh        # Script to install modified SystemUI
├── stone-launcher/            # React Native launcher (main UI)
│   ├── src/screens/          # App screens (Home, Listen, etc.)
│   ├── src/components/       # StoneIcon, StoneChat components
│   └── package.json          # Dependencies including LiveKit
├── stone-agent/              # Unified AI agent with LiveKit
│   └── package.json         # TypeScript agents configuration
├── mcp-servers/             # MCP implementations for each app
│   ├── spotify-mcp/
│   ├── maps-mcp/
│   ├── telephony-mcp/
│   ├── calendar-mcp/
│   └── notion-mcp/
├── STONEOS_SPECS.md        # Complete feature specifications
├── SYSTEMUI_STATUS.md      # Current SystemUI modification status
├── NEXT_STEPS.md          # Immediate action items
└── README.md              # Project overview
```

### Key Architectural Concepts

#### Dual MCP Systems
StoneOS implements two different "MCP" systems:
1. **Master Control Program**: Native Android service for unified API access
2. **Model Context Protocol**: Anthropic's standard for AI tool integration

#### Component Integration Flow
```
Voice Input → LiveKit Agent → MCP Tools → Master Control Program → Native APIs → Response
```

#### Expected Implementation Structure (Future)
```
~/stoneos-workspace/
├── aosp/              # AOSP source tree with patches
├── patches/           # StoneOS AOSP modifications  
├── ui/                # React Native WebView shell
├── mcp/               # Master Control Program (Kotlin)
├── agents/            # AI agents (Python + LiveKit)
└── tools/             # Build and development tools
```

## Development Components

### Foundation Layer
- AOSP build environment setup and configuration
- Patch management system development
- Basic WebView shell implementation
- Initial LiveKit agent integration

### Core Systems
- Master Control Program development in Kotlin
- Agent ecosystem deployment and testing
- MCP server implementation and API design
- Security framework and permission system

### Integration & Polish
- Third-party app integration (Spotify, Google services)
- Performance optimization and profiling
- UI refinement and user experience testing
- Beta preparation and testing infrastructure

## Security Architecture

### Multi-Layer Security Model
1. **AOSP Security**: Android's built-in security features
2. **MCP Isolation**: Sandboxed service with restricted permissions  
3. **Agent Sandboxing**: Isolated execution environments
4. **API Security**: Authenticated and rate-limited service access
5. **Privacy Controls**: User consent and data minimization

### Key Security Considerations
- All agent communication goes through MCP service
- App integrations use secure, limited-scope APIs
- Voice data processed locally when possible
- Comprehensive audit logging and monitoring

## Integration Strategy

### Supported App Integrations
- **Spotify**: Playback control, playlist management, music discovery
- **Google Maps**: Navigation, location services, traffic updates
- **Calendar**: Event management, scheduling, reminders
- **Contacts**: Communication initiation, contact management

### MCP Protocol Implementation
- Language-agnostic tool integration standard
- JSON-RPC based communication protocol
- Secure authentication and authorization
- Extensible for third-party developers

## Development Workflow

### Key Build System Understanding (Sept 8-9, 2024 Sessions)

#### Critical Discovery: Why Stone Classes Weren't Being Included
1. **Build System Caching**: Soong caches file lists at parse time, not build time
2. **Glob Pattern Timing**: `"src/**/*.java"` evaluated BEFORE we copy files
3. **SystemUI Structure**: Uses SystemUI-core library, not direct app sources
4. **Root Cause**: AOSP's build system evaluates glob patterns like `"src/**/*.java"` at **parse time**, not build time - files must exist BEFORE the build system starts

#### Failed Approaches and Why

1. **Device Overlays (DEVICE_PACKAGE_OVERLAYS)**
   - **What we tried**: Created device tree with overlay structure
   - **Why it failed**: Device overlays ONLY work for resources (XML, images), NOT Java/Kotlin code
   - **Key learning**: This is by design in AOSP - overlays are for customizing resources, not adding new code

2. **LineageOS Glob Pattern Approach**
   - **What we tried**: Dropped files in src/ expecting glob pattern to pick them up
   - **Why it failed**: Glob patterns are evaluated when build system initializes, not during compilation
   - **Evidence**: Build succeeded but Stone classes weren't in the APK

3. **Direct Android.bp Patching with sed**
   - **What we tried**: Used sed to add Stone files to srcs array
   - **Why it failed**: Pattern didn't match actual Android.bp formatting, files still not included

4. **Android.mk Modification**
   - **What we tried**: Created Android.mk with LOCAL_SRC_FILES
   - **Why it failed**: SystemUI uses Android.bp (Soong), not Android.mk (Make)

#### THE SOLUTION THAT WORKS: Fork AOSP Like LineageOS

**Date**: Sept 9, 2024  
**Approach**: Fork frameworks/base and add Stone files BEFORE build system initialization

**What we did:**
1. Forked https://github.com/aosp-mirror/platform_frameworks_base to stone-pebble/stoneos-frameworks
2. Added Stone files directly to the fork at `packages/SystemUI/src/com/android/systemui/stone/`
3. Created local manifest to replace Google's frameworks/base with our fork:
```xml
<manifest>
  <remove-project name="platform/frameworks/base" />
  <project name="stone-pebble/stoneos-frameworks" 
           path="frameworks/base" 
           remote="github" 
           revision="android-14.0.0_r61" />
</manifest>
```

**Why this works:**
- **Files exist BEFORE build system starts** - This is critical!
- **Glob patterns find the files** because they're already in the source tree
- **This is how LineageOS does it** - They fork repos and add their customizations
- **No patching needed** - Files are part of the source from the beginning

**Key Learning:**
The Android build system is designed for forks, not runtime modifications. Google expects device manufacturers to fork AOSP repos and add their customizations directly.

### Common Build Commands

#### 1. SystemUI Modification (OPTION C - PRIMARY APPROACH)
```bash
# Extract SystemUI from device
adb pull /system_ext/priv-app/SystemUI/SystemUI.apk SystemUI_original.apk

# Decompile SystemUI
apktool d SystemUI_original.apk -o SystemUI_decompiled

# Modify SystemUI (automated by script)
./systemui-mod.sh

# Install modified SystemUI (DANGEROUS - have recovery ready)
./install-systemui.sh

# Check if modification is working
adb logcat | grep StoneOS
```

#### 2. AOSP Build (CRITICAL - PROVEN WORKING CONFIGURATION WITH FORK)
```bash
# Download AOSP
mkdir ~/aosp && cd ~/aosp
repo init -u https://android.googlesource.com/platform/manifest -b android-14.0.0_r61 --depth=1

# Add local manifest to use forked frameworks/base
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

# Sync with our fork included
repo sync -c -j4 --no-tags --no-clone-bundle --current-branch

# Build with CORRECT config
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng  # For Android 14 - NOT trunk_staging!
m SystemUI -j8

# Output location
# out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk
```

**Why these configs work:**
- **trunk_staging**: Google internal only, not in public AOSP
- **ap2a**: Android 14 QPR2 (Quarterly Platform Release)
- **ap3a**: Android 15 QPR1
- **aosp_current**: Latest development release

#### 3. GCP Instance Management
```bash
# Create AOSP build instance (auto-shutdown after 8 hours)
./gcp_aosp_instance.sh create
./gcp_aosp_instance.sh status  # Check if running
./gcp_aosp_instance.sh ssh     # SSH into instance
./gcp_aosp_instance.sh stop    # Stop (keeps disk, ~$0.08/hr)
./gcp_aosp_instance.sh delete  # Delete completely (no charges)
```

#### 4. Root Setup for Pixel 8a
```bash
# Unlock bootloader (WIPES DEVICE!)
adb reboot bootloader
fastboot flashing unlock

# Use Magisk app on device for Direct Install
# Open Magisk app → Install → Direct Install → Reboot

# Verify root access
adb shell su -c 'whoami'  # Should output: root
```

#### 5. Stone Launcher Development
```bash
# Build and install launcher
cd stone-launcher
npm install
npm run build:android
npm run install:device

# Set as default launcher
adb shell cmd package set-home-activity com.stonelauncher/.MainActivity

# Debug launcher
adb logcat | grep -E "StoneOS|ReactNative"

# Hot reload during development
npm start  # In one terminal
npm run android  # In another terminal
```

#### 6. Stone Agent (LiveKit Integration)
```bash
# Build TypeScript agents
cd stone-agent
npm install
npm run build

# Start individual agents
npm run start:stone   # Main Stone agent
npm run start:go      # Maps agent
npm run start:listen  # Spotify agent

# Start all agents
npm run start:all

# Test locally
npm test
```

#### 7. MCP Server Development
```bash
# Start individual MCP servers
cd mcp-servers/spotify-mcp && npm start
cd mcp-servers/maps-mcp && npm start
cd mcp-servers/telephony-mcp && npm start
cd mcp-servers/calendar-mcp && npm start
cd mcp-servers/notion-mcp && npm start

# Test MCP integration
curl -X POST http://localhost:3000/execute \
  -H "Content-Type: application/json" \
  -d '{"tool": "play_song", "params": {"query": "test"}}'
```

#### 8. LiveKit Server on Device
```bash
# Push LiveKit server to device
adb push livekit-server-arm64 /data/local/tmp/
adb shell chmod +x /data/local/tmp/livekit-server

# Start LiveKit server on device
adb shell "/data/local/tmp/livekit-server --dev --port 7880"

# Port forward for local testing
adb forward tcp:7880 tcp:7880
```

#### 9. Testing & Debugging
```bash
# Monitor all StoneOS components
adb logcat -s StoneOS:* Stone:* MCP:* LiveKit:*

# Clear app data and restart
adb shell pm clear com.stonelauncher
adb shell am start com.stonelauncher/.MainActivity

# Take screenshot for debugging
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png

# Check running services
adb shell dumpsys activity services | grep -i stone
```

### Build Process
AOSP builds require significant resources and time:
- Full builds can take 4-8 hours depending on hardware
- Incremental builds significantly faster after initial setup
- Multiple build variants for different testing scenarios

### Testing Strategy
- **Unit Tests**: Individual component testing
- **Integration Tests**: Cross-component interaction testing  
- **System Tests**: Full OS functionality validation
- **Device Tests**: Real hardware testing on Pixel 8a
- **Performance Tests**: Voice latency, battery life, memory usage

### Quality Assurance
- Always test SystemUI modifications with recovery method ready
- Manual testing on Pixel 8a hardware
- Monitor logs with `adb logcat | grep StoneOS`
- Verify root access before system modifications

## Technical Reference

### Build Optimization for Limited RAM (16-32GB)
```bash
# Limit parallel jobs for repo sync (MUST use -j4 to avoid HTTP 429)
repo sync -c -j4 --no-tags --no-clone-bundle --current-branch

# Build with moderate parallelism
m SystemUI -j8

# For very limited RAM systems
export ANDROID_JACK_VM_ARGS="-Xmx4g -Dfile.encoding=UTF-8"
export USE_CCACHE=1
```

### Essential Debug Commands
```bash
# SystemUI logs
adb logcat -s SystemUI:* | grep StoneOS

# Window manager state
adb shell dumpsys window

# Graphics performance (target < 5% overhead)
adb shell dumpsys gfxinfo com.android.systemui

# Test in emulator
emulator -avd Pixel_8a_API_35 -writable-system
adb root && adb remount
adb push SystemUI.apk /system/system_ext/priv-app/SystemUI/
adb reboot
```

### SystemUI Key Locations (for modifications)
- Stone Panel: `SystemUI/src/com/android/systemui/stone/`
- Quick Settings: `SystemUI/src/com/android/systemui/qs/`
- Navigation Bar: `SystemUI/src/com/android/systemui/navigationbar/`
- Status Bar: `SystemUI/src/com/android/systemui/statusbar/`

### Grayscale Color Matrix (ITU-R BT.709)
For system-wide grayscale in SurfaceFlinger:
```cpp
float[] mat = {
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Red
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Green  
    0.2126f, 0.7152f, 0.0722f, 0, 0,  // Blue
    0, 0, 0, 1, 0                      // Alpha
}
```
Location: `frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp`

## Technical Constraints

### AOSP Limitations
- Build system complexity and resource requirements
- Hardware compatibility limited to supported devices
- Security update dependency on AOSP release schedule
- Limited customization without system-level changes

### Performance Targets
- Boot time: < 30 seconds
- Voice response latency: < 500ms  
- Battery life: > 12 hours typical use
- System crash rate: < 0.1%

## Common Pitfalls & Solutions

### What Failed & Why (Learn from our mistakes)
1. **ARM VM on Mac** - Can't use x86 AOSP prebuilts
2. **x86 VM on Mac** - Too slow (emulated), network issues  
3. **Docker on Mac** - x86 emulation very slow on Apple Silicon
4. **Wrong lunch target** - `trunk_staging` is Google internal only
5. **Device Overlays for Java** - Only work for resources, not code
6. **Dropping files after repo sync** - Glob patterns already evaluated
7. **Patching Android.bp** - Too fragile, doesn't reliably work
8. **Android.mk for SystemUI** - SystemUI uses Soong (Android.bp) not Make

### Critical Build Requirements
- **MUST use `-j4` for repo sync** - Higher causes HTTP 429 rate limiting
- **Use `$HOME` not `/home`** - Permission issues otherwise
- **Touch `/tmp/ready` immediately** - Prevents startup timeout
- **Label format** - Hyphens only, no periods/underscores
- **Total time**: ~35 minutes
- **Total cost**: ~$0.15 with SPOT instances

## Future Considerations

### Scalability
- Multi-device synchronization capabilities
- Cloud agent processing for resource-intensive tasks
- Third-party agent development platform
- Enterprise and developer API ecosystem

### Platform Evolution  
- Support for additional hardware platforms
- Integration with IoT and smart home devices
- Advanced AI capabilities and model updates
- Developer SDK for MCP agent creation

## Stone Classes Compilation Issue & Solution

### The Problem (Sept 9, 2024)
Despite successfully forking frameworks/base and having Stone files present in the source tree, the Stone classes are not being compiled into SystemUI.apk.

**Evidence:**
- Files ARE present: `/home/samuellarson/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/`
- Build succeeds without errors
- APK is generated (43MB)
- But Stone classes are NOT in the final APK

### Root Cause
SystemUI is not a single module but a collection of libraries. Our Stone files need proper module integration in Android.bp. Simply having files in the source tree isn't enough - they must be explicitly included in a module.

### THE SOLUTION: Modify Android.bp in Fork

**Option 1: Add Stone as Separate Module (RECOMMENDED)**
```java
// In packages/SystemUI/Android.bp, add:
android_library {
    name: "SystemUI-stone",
    srcs: [
        "src/com/android/systemui/stone/*.java",
    ],
    static_libs: [
        "SystemUI-core",
    ],
}

// Then include it in SystemUI app:
android_app {
    name: "SystemUI",
    static_libs: [
        "SystemUI-stone",  // Add this line
        "SystemUI-core",
        // ... other libs
    ],
}
```

**Option 2: Explicitly List Files**
```java
android_library {
    name: "SystemUI-core",
    srcs: [
        "src/**/*.kt",
        "src/**/*.java",
        "src/**/I*.aidl",
        // Add these lines:
        "src/com/android/systemui/stone/StonePanel.java",
        "src/com/android/systemui/stone/StoneIcon.java",
        ":ReleaseJavaFiles",
    ],
}
```

**Option 3: Use a Filegroup**
```java
filegroup {
    name: "StoneFiles",
    srcs: [
        "src/com/android/systemui/stone/*.java",
    ],
}

android_library {
    name: "SystemUI-core",
    srcs: [
        "src/**/*.kt",
        "src/**/*.java",
        ":StoneFiles",  // Add this
    ],
}
```

## Failed Approaches (Learn from Our Mistakes)

### 1. Device Overlays (Sept 7-8, 2024)
**What we tried:** Use DEVICE_PACKAGE_OVERLAYS to add Stone files
```bash
DEVICE_PACKAGE_OVERLAYS += device/stone/stoneos/overlay
```
**Why it failed:** Device overlays ONLY work for resources (XML, images), NOT Java/Kotlin code. This is by design in AOSP.

### 2. LineageOS Glob Pattern Approach (Sept 8, 2024)
**What we tried:** Drop files in src/ expecting glob pattern to pick them up
**Why it failed:** Glob patterns are evaluated at parse time, not build time. Files added after repo sync aren't included.

### 3. Direct Android.mk Modification
**What we tried:** Create Android.mk with LOCAL_SRC_FILES
**Why it failed:** SystemUI uses Android.bp (Soong), not Android.mk (Make)

### 4. Custom Lunch Target
**What we tried:** Define custom lunch target without full device tree
**Why it failed:** Custom lunch targets require complete device configuration

## Implementation Plan

### Phase 1: Fix Android.bp in Fork (CURRENT)
1. Clone fork locally
2. Modify Android.bp to include Stone module
3. Push changes to GitHub
4. Run clean build with updated fork

### Phase 2: Core SystemUI Modifications
- Implement swipe-up gesture from Stone icon
- Create chat interface layout (1/3 screen)
- Implement window management for 2/3 app view

### Phase 3: Grayscale System
- Add ColorMatrixFilter to SurfaceFlinger
- Apply system-wide grayscale (except camera/photos)

### Phase 4: Testing with Cuttlefish
```bash
# On GCP build instance after build completes
sudo apt install -y git devscripts equivs config-package-dev debhelper-compat golang curl

# Build Cuttlefish packages
git clone https://github.com/google/android-cuttlefish
cd android-cuttlefish
debuild -i -us -uc -b
sudo dpkg -i ../cuttlefish-*.deb

# Launch with our build
launch_cvd --daemon \
  --system_image_dir=$HOME/aosp/out/target/product/generic_x86_64 \
  --kernel_path=$HOME/aosp/out/target/product/generic_x86_64/kernel

# Access via browser at https://localhost:8443
```

## Current Build Status (Sept 6, 2024 Night Session)

### Major Accomplishments Today
1. **Fixed all build issues** and successfully built SystemUI.apk multiple times
2. **Identified Stone integration problem**: Classes weren't being compiled into APK
3. **FIXED the root cause**: Updated build script to patch Android.bp to include Stone classes
4. **Set up rooted emulator**: AOSP image (not Google Play) that supports `adb root`
5. **Optimized build performance**: Upgraded to 32-core instance, reduced time to ~25 min
6. **Cleaned up repository**: Removed redundant files, consolidated docs

### Build Script Improvements
- ✅ Fixed file upload paths (stone/* instead of stone/stone/*)
- ✅ Added Android.bp patching to include Stone classes in compilation
- ✅ Added verification steps throughout build
- ✅ Upgraded to n2-standard-32 (32 cores) for 40% faster builds
- ✅ Cost remains low: ~$0.64 per build

### Testing Environment Ready
- Android Studio installed and configured
- Pixel 8a AVD created with Android 14 (API 34)
- Using AOSP system image (rootable) instead of Google Play image
- Emulator starts with: `-writable-system -selinux permissive`

### Tomorrow's Build Plan
```bash
# 1. Run the updated build script
./build_stoneos.sh

# 2. This will now:
- Copy Stone files correctly
- Patch Android.bp to include them
- Build with 32 cores (faster)
- Verify Stone classes are in APK

# 3. Test on rooted emulator
adb root
adb push StoneOS_SystemUI.apk /system/system_ext/priv-app/SystemUI/
adb shell pkill -f com.android.systemui
adb logcat | grep -E "StoneOS|StonePanel"
```

### Key Learning: Third-Party Apps on Custom ROM
- **Google Play Services**: Custom ROMs ship without it (legal reasons)
- **Solution 1**: Flash OpenGApps after ROM install (most common)
- **Solution 2**: Use MicroG (open source Google Services replacement)
- **Solution 3**: Aurora Store (anonymous Play Store access)
- For StoneOS: Will likely use minimal GApps or MicroG approach

### Testing Commands
```bash
# Extract and check APK contents
unzip -l StoneOS_SystemUI.apk | grep -i stone
# Check dex for Stone classes  
dexdump classes.dex | grep Stone
# Install and monitor
adb install -r StoneOS_SystemUI.apk
adb logcat | grep StoneOS
```

### Emulator Setup & Testing

#### Getting a Rooted Emulator
```bash
# Option 1: Download non-PlayStore image (rootable)
# In Android Studio: Tools → SDK Manager → SDK Tools
# Download: "Android 14 (API 34) Google APIs" (NOT PlayStore version)
# Create AVD with this image - it supports 'adb root'

# Option 2: Use our AOSP build output
# From GCP build, download system.img, vendor.img, etc.
# Create custom AVD using these images

# Option 3: Start existing AVD with root-friendly flags
~/Library/Android/sdk/emulator/emulator -avd Pixel_8a_StoneOS \
  -writable-system \
  -selinux permissive \
  -no-snapshot-load
```

#### Installing SystemUI on Rooted Emulator
```bash
# 1. Check if rootable
adb root  # Should say "restarting adbd as root"

# 2. Remount system as writable
adb remount

# 3. Install our SystemUI
adb push StoneOS_SystemUI.apk /system/system_ext/priv-app/SystemUI/SystemUI.apk
adb shell chmod 644 /system/system_ext/priv-app/SystemUI/SystemUI.apk

# 4. Restart SystemUI
adb shell pkill -f com.android.systemui

# 5. Monitor logs
adb logcat | grep -E "StoneOS|StonePanel|StoneIcon"
```

## Key Development Principles

### Core Implementation Approach
- **OPTION C is the way**: Modify SystemUI directly, not AOSP build
- **Real device testing**: Pixel 8a with unlocked bootloader
- **Root required**: Use Magisk for system-level access
- **Grayscale everything**: System-wide grayscale (except camera/images)
- **Real apps, not web views**: Embed actual Android apps

### Development Philosophy
- **Don't take shortcuts**: Follow the specs exactly as written
- **Test on real hardware**: Not emulators
- **SystemUI modification is dangerous**: Always have recovery ready
- **Stone icon always visible**: Bottom of screen in all apps
- **Choice-first, not voice-first**: User chooses interaction method

### Next Session Priorities
1. **Get root working**: Magisk Direct Install on device
2. **Test SystemUI replacement**: Run systemui-mod.sh and install-systemui.sh
3. **Install Stone launcher**: Replace default home screen
4. **Verify chat overlay**: Swipe-up from Stone icon

### Important Files to Reference
- `STONEOS_SPECS.md`: The complete feature specifications (source of truth)
- `THIRD_PARTY_APPS_SOLUTION.md`: How to get apps working on StoneOS
- `android_bp_fix.md`: Proper way to modify Android.bp for Stone classes
- `build_stoneos.sh`: Main build script (now with fixed Android.bp patching)