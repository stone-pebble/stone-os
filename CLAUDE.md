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

#### 2. Root Setup for Pixel 8a
```bash
# Unlock bootloader (WIPES DEVICE!)
adb reboot bootloader
fastboot flashing unlock

# Use Magisk app on device for Direct Install
# Open Magisk app → Install → Direct Install → Reboot

# Verify root access
adb shell su -c 'whoami'  # Should output: root
```

#### 3. Stone Launcher Development
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

#### 4. Stone Agent (LiveKit Integration)
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

#### 5. MCP Server Development
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

#### 6. LiveKit Server on Device
```bash
# Push LiveKit server to device
adb push livekit-server-arm64 /data/local/tmp/
adb shell chmod +x /data/local/tmp/livekit-server

# Start LiveKit server on device
adb shell "/data/local/tmp/livekit-server --dev --port 7880"

# Port forward for local testing
adb forward tcp:7880 tcp:7880
```

#### 7. Testing & Debugging
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
- `SYSTEMUI_STATUS.md`: Current SystemUI modification progress
- `NEXT_STEPS.md`: Immediate action items for development
- `systemui-mod.sh`: Script to modify SystemUI
- `install-systemui.sh`: Script to install modified SystemUI