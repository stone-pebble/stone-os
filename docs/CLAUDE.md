# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StoneOS is a minimalist, AI-augmented Android experience that transforms Android into a simplified mobile OS where traditional apps and intelligent agents coexist. It's not voice-first, but **choice-first**: Users can interact via touch OR conversational AI, switching seamlessly between both.

**Current Implementation Strategy: Forked AOSP Build**
- Building full AOSP from source with forked frameworks/base
- Using fork: https://github.com/stone-pebble/stoneos-frameworks
- Stone components integrated directly into SystemUI source
- Target device: Pixel 8a (akita) with unlocked bootloader
- Build configuration: `aosp_x86_64-ap2a-eng` for emulator testing

## Architecture

### Core AOSP Modifications
```
Forked frameworks/base (Contains all Stone SystemUI components)
├── packages/SystemUI/src/com/android/systemui/stone/
│   ├── StonePanel.java     # 1/3 screen chat panel
│   └── StoneIcon.java      # Always-visible Stone icon
├── PhoneWindowManager       # Window resizing for 2/3 app view
└── SurfaceFlinger          # System-wide grayscale rendering
```

### Component Stack
```
Modified AOSP SystemUI → Stone Launcher → MCP Servers → LiveKit Agents
```

### Key SystemUI Components
- **StonePanel.java**: Sliding chat interface (1/3 screen)
- **StoneIcon.java**: Always-visible Stone icon at bottom
- **Window Management**: Resizes apps to 2/3 when chat active
- **Grayscale Rendering**: System-wide color matrix filter
- **Gesture Detection**: Swipe-up from Stone icon to reveal chat

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

### Repository Structure
```
/
├── build_stoneos.sh        # Main build script for GCP AOSP builds
├── test_emulator.sh        # Local emulator testing script
├── stone/                  # Stone SystemUI components (in fork)
│   ├── StonePanel.java    # Chat panel implementation
│   └── StoneIcon.java     # Stone icon implementation
├── stone-agent/           # LiveKit-based AI agents
│   └── package.json       # TypeScript agent configuration
├── mcp-servers/          # Model Context Protocol servers
│   ├── spotify-mcp/
│   ├── maps-mcp/
│   ├── telephony-mcp/
│   ├── calendar-mcp/
│   └── notion-mcp/
└── STONEOS_SPECS.md      # Complete feature specifications
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

## Build System Understanding

### Critical Build System Knowledge
- **Soong Build System**: AOSP uses Soong (Android.bp), not Make (Android.mk)
- **Parse-Time Evaluation**: Glob patterns evaluated BEFORE build starts
- **Fork Required**: Must fork frameworks/base to include custom SystemUI code
- **Local Manifest**: Uses `.repo/local_manifests/` to override Google repos

### Why Forking Works
The Android build system expects customizations through forked repos:
1. Fork `platform/frameworks/base` to add Stone components
2. Use local manifest to replace Google's repo with fork
3. Stone files exist in source tree before build system starts
4. Soong glob patterns find Stone files naturally

## Common Build Commands

### AOSP Build (Primary Approach)
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

### GCP Build Management
```bash
# Full build cycle with emulator testing
./build_stoneos.sh

# Quick build without testing
./build_stoneos.sh --quick

# Test existing build in emulator
./build_stoneos.sh --test

# Deploy to connected device
./build_stoneos.sh --deploy

# Show cost estimates
./build_stoneos.sh --cost
```

### Device Setup (Pixel 8a)
```bash
# Unlock bootloader (WIPES DEVICE!)
adb reboot bootloader
fastboot flashing unlock

# Use Magisk app on device for Direct Install
# Open Magisk app → Install → Direct Install → Reboot

# Verify root access
adb shell su -c 'whoami'  # Should output: root
```

### Stone Agent (LiveKit Integration)
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

### MCP Server Development
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

### Testing & Debugging
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

### Build Performance
- **GCP Instance**: n2-standard-32 (32 vCPUs, 128GB RAM)
- **Build Time**: ~25-35 minutes total
  - AOSP download: ~15 minutes (rate-limited to -j4)
  - SystemUI build: ~10-15 minutes with 32 cores
- **Cost**: ~$0.17-$0.23 per build with SPOT pricing

## Technical Reference

### Critical Build Requirements
```bash
# MUST use -j4 for repo sync (higher causes HTTP 429)
repo sync -c -j4 --no-tags --no-clone-bundle --current-branch

# Build with appropriate parallelism
m SystemUI -j16  # On 32-core GCP instance
m SystemUI -j8   # On local machine

# Enable ccache for faster rebuilds
export USE_CCACHE=1
export CCACHE_DIR=/tmp/ccache
ccache -M 20G
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

### Key File Locations
- **Stone Components**: `frameworks/base/packages/SystemUI/src/com/android/systemui/stone/`
- **Window Manager**: `frameworks/base/services/core/java/com/android/server/policy/PhoneWindowManager.java`
- **SurfaceFlinger**: `frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp`
- **Build Output**: `out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk`

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

## Performance Targets
- **Voice response latency**: < 500ms
- **Chat panel animation**: 300ms
- **System grayscale overhead**: < 5%
- **Build time**: ~25-35 minutes on GCP
- **APK size**: ~40-50MB

## Common Issues & Solutions

### Build Issues
- **HTTP 429 on repo sync**: Use `-j4` (not higher)
- **trunk_staging lunch target**: Use `aosp_x86_64-ap2a-eng` instead
- **Stone classes not in APK**: Ensure fork is properly synced
- **GCP instance timeout**: Touch `/tmp/ready` in startup script

### What Doesn't Work
- **Device overlays for Java code**: Only work for resources
- **Runtime Android.bp patching**: Too fragile
- **Android.mk for SystemUI**: SystemUI uses Soong
- **Building on macOS**: Use GCP Linux instance instead

## Next Steps

### Phase 1: Android.bp Integration
Modify Android.bp in forked frameworks/base to properly include Stone classes in SystemUI build.

### Phase 2: Core Features
- Implement swipe-up gesture detection
- Create chat panel sliding animation
- Add window management for 2/3 app view

### Phase 3: System Integration
- Implement system-wide grayscale filter
- Add notification aggregation system
- Integrate with LiveKit agents

## Important References
- **STONEOS_SPECS.md**: Complete feature specifications
- **build_stoneos.sh**: Main GCP build script
- **Fork Repository**: https://github.com/stone-pebble/stoneos-frameworks