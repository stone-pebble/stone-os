# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StoneOS is an AI-first mobile operating system that transforms Android by replacing the traditional application layer with a voice-driven interface powered by AI agents and the Model Context Protocol (MCP).

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

This codebase contains comprehensive documentation for StoneOS but **no implementation code yet** - the project is in architecture and planning phase.

### Documentation Organization
```
/
├── architecture/          # System design and technical specs
│   ├── README.md         # Architecture overview
│   └── mcp-architecture.md # Dual MCP system design
├── agents/               # AI agent specs and MCP integration
├── development/          # Build setup and workflow
├── integration/          # Third-party app integration guides  
├── patches/              # AOSP patch documentation
├── security/             # Security architecture
├── ui/                   # React Native shell specs
├── EXECUTIVE_SUMMARY.md  # Project vision and strategy
├── FEATURES.md          # Core feature specifications
├── ROADMAP.md           # Development timeline
└── README.md            # Project overview
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

#### SystemUI Modification (OPTION C - PRIMARY APPROACH)
```bash
# Extract SystemUI from device
adb pull /system_ext/priv-app/SystemUI/SystemUI.apk

# Decompile SystemUI
apktool d SystemUI.apk -o SystemUI

# After modifications, rebuild
apktool b SystemUI -o SystemUI_modified.apk

# Sign with platform key (required!)
java -jar signapk.jar platform.x509.pem platform.pk8 SystemUI_modified.apk SystemUI_signed.apk

# Replace SystemUI (DANGEROUS - have recovery ready)
adb push SystemUI_signed.apk /sdcard/
adb shell "su -c 'mount -o rw,remount /system_ext'"
adb shell "su -c 'cp /sdcard/SystemUI_signed.apk /system_ext/priv-app/SystemUI/SystemUI.apk'"
adb shell "su -c 'chmod 644 /system_ext/priv-app/SystemUI/SystemUI.apk'"
adb reboot
```

#### Root Setup for Pixel 8
```bash
# Unlock bootloader (WIPES DEVICE!)
adb reboot bootloader
fastboot flashing unlock

# Flash Magisk patched boot image
fastboot flash init_boot magisk_patched.img
fastboot reboot
```

#### Future: AOSP Build Setup (Deferred)
```bash
# When ready for full AOSP approach:
source build/envsetup.sh
lunch stoneos_pixel8a-userdebug
make -j$(nproc) dist
```

#### Component-Specific Builds
```bash
# Build Master Control Program
cd ~/stoneos-workspace/mcp
./gradlew build
cp build/outputs/*.jar ../aosp/vendor/pebble/mcp/

# Build React Native UI
cd ~/stoneos-workspace/ui
npm install
npm run build:android
npx react-native bundle --platform android --dev false --entry-file index.js \
    --bundle-output ../aosp/vendor/pebble/ui/bundle.js \
    --assets-dest ../aosp/vendor/pebble/ui/assets

# Build AI agents
cd ~/stoneos-workspace/agents
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python test_agent.py
```

#### Device Commands
```bash
# Flash StoneOS to device
fastboot flashall -w

# Quick development cycle commands
adb root && adb remount
adb push build/android/* /system/app/StoneUI/
adb shell am force-stop com.stoneos.ui
adb shell am start com.stoneos.ui/.MainActivity

# Debug and monitoring
adb logcat -s StoneOS:* MCP:* StoneUI:* Agent:*
adb shell dumpsys mcp
adb bugreport stoneos-bugreport.zip
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
- **Device Tests**: Real hardware testing on Pixel devices
- **Performance Tests**: Voice latency, battery life, memory usage

### Quality Assurance
- Automated testing pipeline for all changes
- Manual testing on target hardware
- Performance benchmarking and regression testing
- Security audit and penetration testing

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