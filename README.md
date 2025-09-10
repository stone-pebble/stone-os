# StoneOS - Central Development Repository

> **The complete StoneOS development environment in one organized repository**

StoneOS is a minimalist, AI-augmented Android ROM that transforms Android into a choice-first experience where traditional apps and intelligent agents coexist seamlessly.

## 🏗️ Repository Structure

This repository contains **everything** needed for StoneOS development:

```
stone-os/                                    # 👈 THIS IS YOUR CENTRAL REPO
├── 🎯 Core Components/
│   ├── stone/                              # Stone SystemUI components (source of truth)
│   │   ├── StonePanel.java                 # 1/3 screen chat interface
│   │   └── StoneIcon.java                  # Always-visible Stone icon
│   ├── stone-agent/                        # LiveKit-based AI agents
│   └── mcp-servers/                        # Model Context Protocol servers
│
├── 🔧 Development Environment/
│   ├── development/
│   │   ├── fork-workspace/                 # GitHub fork management
│   │   │   └── stoneos-frameworks/        # AOSP fork with Stone components
│   │   ├── aosp-source -> ~/aosp          # Symlink to full AOSP (150GB)
│   │   └── tools/                         # Development utilities
│   ├── scripts/                           # Build and deployment scripts
│   │   ├── build_stoneos.sh              # Main GCP build script
│   │   └── test_emulator.sh              # Emulator testing
│   └── builds/                           # Build outputs
│       └── latest/                       # Most recent SystemUI.apk
│
├── 📚 Documentation/
│   ├── docs/                             # All project documentation
│   ├── DEVELOPMENT_STRUCTURE.md          # Detailed workflow guide
│   └── README.md                         # This file
│
└── 🚀 Application Components/
    ├── stone-launcher/                   # React Native launcher (future)
    └── integration/                      # MCP configurations
```

## 🎯 Quick Start

### 1. Clone This Repository
```bash
git clone https://github.com/stone-pebble/stone-os.git
cd stone-os
```

### 2. Build StoneOS
```bash
# Everything runs from this central repo
./scripts/build_stoneos.sh
```

### 3. Test Your Build
```bash
./scripts/test_emulator.sh
```

## 🔄 Development Workflow

### Making Changes to Stone Components

1. **Edit files** in `stone/` directory (source of truth)
2. **Sync to fork**:
   ```bash
   # Copy changes to fork workspace
   cp stone/*.java development/fork-workspace/stoneos-frameworks/packages/SystemUI/src/com/android/systemui/stone/
   
   # Push to GitHub fork
   cd development/fork-workspace/stoneos-frameworks
   git add -A && git commit -m "Update Stone components"
   git push origin android-14.0.0_r61
   ```
3. **Rebuild**:
   ```bash
   # Back to central repo
   cd ~/stone-os
   ./scripts/build_stoneos.sh
   ```

### Repository Key Features

- **📍 Single Source of Truth**: All Stone development happens here
- **🔗 Integrated Workflow**: Fork management built into repository
- **📦 Complete Environment**: Everything needed in one place
- **🎯 Clear Organization**: Logical separation of concerns
- **🚀 Ready to Use**: Scripts and tools included

## 📋 Project Status

### ✅ Completed
- [x] Fork-based AOSP development workflow
- [x] Stone SystemUI components (StonePanel.java, StoneIcon.java)
- [x] Successful SystemUI.apk builds (42MB)
- [x] Centralized development environment
- [x] Build automation via GCP

### 🚧 In Progress
- [ ] Stone component functionality implementation
- [ ] LiveKit agent integration
- [ ] MCP server deployment
- [ ] Emulator/device testing

### 🎯 Next Milestones
- [ ] Working swipe-up gesture detection
- [ ] Chat panel sliding animation
- [ ] System-wide grayscale implementation
- [ ] Stone launcher deployment

## 🏛️ Architecture Overview

### Core Philosophy
**Choice-First, Not Voice-First**: Users can interact via touch OR conversational AI, switching seamlessly between both.

### Key Components
- **Stone SystemUI**: Custom Android system interface with 1/3 chat panel
- **AI Agents**: LiveKit-powered voice and text interaction
- **MCP Servers**: Protocol bridges to control apps (Spotify, Maps, etc.)
- **Stone Launcher**: Minimalist home screen replacement

### Technical Stack
- **Base**: Android 14 (AOSP)
- **Target Device**: Pixel 8a (akita) with unlocked bootloader
- **Build System**: Forked frameworks/base with Soong integration
- **AI Framework**: LiveKit for real-time voice/text processing
- **App Control**: MCP (Model Context Protocol) for third-party integration

## 🛠️ Development Commands

All commands run from the central `stone-os/` directory:

```bash
# Build commands
./scripts/build_stoneos.sh          # Full GCP build
./scripts/build_stoneos.sh --quick  # Build without testing
./scripts/build_stoneos.sh --cost   # Show cost estimates

# Testing commands
./scripts/test_emulator.sh           # Launch emulator with StoneOS
./scripts/test_emulator.sh --device # Deploy to connected device

# Development utilities
cd development/fork-workspace/stoneos-frameworks  # Access AOSP fork
ls builds/latest/                                # Check latest builds
```

## 📖 Documentation

- **[DEVELOPMENT_STRUCTURE.md](DEVELOPMENT_STRUCTURE.md)**: Complete technical workflow
- **[docs/STONEOS_SPECS.md](docs/STONEOS_SPECS.md)**: Full feature specifications  
- **[docs/CLAUDE.md](docs/CLAUDE.md)**: AI assistant development guide
- **[docs/](docs/)**: All project documentation

## 🤝 Contributing

1. Fork this repository
2. Create your feature branch from `master`
3. Make changes in the appropriate directories
4. Test your changes with `./scripts/build_stoneos.sh`
5. Submit a pull request

## 📜 License

MIT License - See [docs/](docs/) for full details

---

**This is your complete StoneOS development environment. Everything you need is right here.**