# StoneOS - Custom Android ROM with AI Integration

StoneOS is a **custom Android ROM** that transforms Android into a minimalist, AI-augmented experience where traditional apps and intelligent agents coexist seamlessly.

## 🚨 Important: This is an AOSP Build Project

**This is NOT an app or launcher - it's a full custom Android operating system** that requires building the Android Open Source Project (AOSP) from source.

## Vision

Not voice-first, but **choice-first**: Users can interact via touch OR conversational AI, switching seamlessly between both. Every app is grayscale, the interface is minimal, and Stone - your AI assistant - can control everything when asked.

## Architecture

```
Normal State:
┌─────────────────────────┐
│                         │
│   Full Screen App       │  ← Actual Android app (grayscale)
│                         │
└─────────────────[🗿]────┘  ← Stone icon at bottom

Chat Active (swipe up):
┌─────────────────────────┐
│   Embedded App (2/3)    │  ← App continues running
├─────────────────────────┤
│   Chat Interface (1/3)  │  ← Slides up from bottom
└─────────────────────────┘
```

## Current Status

### ✅ What's Done
- **Device Ready**: Pixel 8a (akita) - Bootloader unlocked, rooted with Magisk 27.0
- **AOSP Downloaded**: Android 15 source ready at `/Volumes/StoneOS_SSD/aosp/`
- **Stone Components Created**: StonePanel.java and StoneIcon.java implemented
- **Build Fix Documented**: Correct lunch targets identified (ap2a/ap3a)

### 🚧 Next Steps
1. Set up GCP instance for AOSP build
2. Build SystemUI with correct lunch target
3. Test in Android emulator
4. Flash to Pixel 8a

## Build Instructions

### Prerequisites
- 400GB+ free disk space
- 32GB+ RAM (64GB recommended)
- Linux or GCP instance (macOS cannot build AOSP)

### Quick Start (Google Cloud Platform)

```bash
# 1. Create GCP instance (auto-shutdown after 8 hours)
./gcp_aosp_instance.sh create

# 2. SSH into instance
gcloud compute ssh aosp-build --zone=us-central1-a

# 3. Download AOSP
mkdir ~/aosp && cd ~/aosp
repo init -u https://android.googlesource.com/platform/manifest -b android-14.0.0_r61
repo sync -c -j4

# 4. Build with CORRECT config
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng  # NOT trunk_staging!
m SystemUI -j8

# 5. Stop instance when done (saves money)
./gcp_aosp_instance.sh stop
```

## Core AOSP Modifications

### 1. SystemUI Stone Panel
- **Location**: `frameworks/base/packages/SystemUI/src/com/android/systemui/stone/`
- **Files**: StonePanel.java, StoneIcon.java
- **Function**: 1/3 screen chat panel that slides up from bottom

### 2. Window Manager
- **File**: `PhoneWindowManager.java`
- **Function**: Resizes apps to 2/3 screen when Stone panel is active

### 3. SurfaceFlinger
- **File**: `SurfaceFlinger.cpp`
- **Function**: System-wide grayscale rendering

### 4. Gesture System
- **File**: `SystemGesturesPointerEventListener.java`
- **Function**: Detects swipe-up from Stone icon

## Failed Approaches (Don't Try These)

❌ **Magisk Modules** - SystemUI crashes without platform keys
❌ **APK Modifications** - Requires AOSP signing keys
❌ **Overlay Services** - Can't achieve window split
❌ **Xposed/LSPosed** - Incompatible with Android 15
❌ **trunk_staging** - Google internal only, not in public AOSP

## Features

### Core Apps
- **Listen** - Spotify control via MCP
- **Go** - Google Maps navigation
- **Ask** - Perplexity search (limited browsing)
- **Connect** - Unified communications hub
- **Plan** - Calendar management
- **Think** - Notion notes
- **Set** - Settings + 2FA apps
- **Tick** - Clock, timers, alarms
- **Fund** - Native wallet + banking
- **Reflect** - AI-powered daily journal
- **Task** - MCP discovery + permitted apps
- **Look** - Digital library (stretch goal)

### Unique Features
- **Swipe-up Chat**: Stone available anywhere via swipe
- **No Voice Transcription**: In voice mode, no distracting text
- **Unified Notifications**: AI-written summaries, not notification lists
- **App Embedding**: Real Android apps at 2/3 size, not web views
- **System Grayscale**: Everything except camera and search images

## Documentation

- **[STONEOS_SPECS.md](STONEOS_SPECS.md)** - Complete feature specifications
- **[AOSP_BUILD_FIX.md](AOSP_BUILD_FIX.md)** - How to fix common build errors
- **[NEXT_AGENT_INSTRUCTIONS.md](NEXT_AGENT_INSTRUCTIONS.md)** - Instructions for next session
- **[CLAUDE.md](CLAUDE.md)** - AI agent instructions

## Cost Management

GCP instances cost ~$0.77/hour when running. **Always stop or delete when done:**

```bash
./gcp_aosp_instance.sh stop    # Keeps disk (~$0.08/hour)
./gcp_aosp_instance.sh delete  # Removes everything ($0)
```

## License

MIT

---

**Remember**: This is a full AOSP build project. There are no shortcuts. The only way to achieve the Stone panel, window management, and system grayscale is through building Android from source.