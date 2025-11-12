# StoneOS - AI-Augmented Android ROM

> **A minimalist, choice-first mobile operating system where humans and AI agents coexist seamlessly**

StoneOS is a custom Android 14 ROM built on AOSP that reimagines mobile interaction. Users choose moment-by-moment whether to use touch interfaces or conversational AI to accomplish tasks—both paths have equal capability.

---

## 🎯 Core Philosophy

**Choice First, Not Voice First**

- Traditional Android apps run normally (in grayscale aesthetic)
- AI assistant available but never intrusive
- User decides: touch UI or voice/chat, task by task
- Full functional parity between interaction methods

---

## 🏗️ Repository Structure

```
stone-os/                              # Central development repository
├── vendor/stone/                      # Source of truth for Stone code
│   ├── packages/SystemUI/src/.../stone/
│   │   ├── StoneManager.java         # SystemUI lifecycle manager
│   │   ├── StoneIcon.java            # Always-visible 🗿 icon
│   │   └── StonePanel.java           # Sliding chat interface
│   └── packages/apps/
│       ├── StoneLauncher/            # 3x4 grid home screen
│       ├── StoneSettings/            # Settings with AI API
│       └── StoneTime/                # Alarms & timers
│
├── tickets/                           # Development workflow
│   ├── CLAUDE.md                     # Guide for coding agents
│   ├── [NNN-ticket-name.md]          # Active tickets
│   ├── archive/                      # Completed tickets
│   └── research/                     # Research assignments
│
├── scripts/
│   ├── sync_vendor.sh                # Sync code to AOSP tree
│   └── build_stoneos.sh              # GCP build automation
│
├── stone-agent/                       # LiveKit AI agents
├── mcp-servers/                       # Model Context Protocol servers
│
├── CLAUDE.md                          # Implementation guide (for AI agents)
├── GEMINI.md                          # Architecture guide (for research/architect)
└── README.md                          # This file (for humans)
```

**AOSP source tree** (separate, ~150GB): `/home/samuellarson/aosp/`

---

## 🚀 Quick Start

### For Human Developers

1. **Clone the repository**
   ```bash
   git clone https://github.com/stone-pebble/stone-os.git
   cd stone-os
   ```

2. **Understand the architecture**
   - Read this README for project overview
   - Read `GEMINI.md` for architectural decisions (the "why")
   - Read `CLAUDE.md` for implementation patterns (the "how")

3. **Explore active work**
   ```bash
   ls tickets/           # See what's being worked on
   cat tickets/CLAUDE.md # Understand the workflow
   ```

### For AI Coding Agents

1. **Read your guide**: `/home/samuellarson/stone-os/CLAUDE.md`
2. **Check for assigned tickets**: `/home/samuellarson/stone-os/tickets/`
3. **Follow ticket workflow**: See `tickets/CLAUDE.md`

---

## 🎨 What is StoneOS?

### User Experience

**Normal state**: Full-screen Android app (in grayscale)
- Stone icon (🗿) always visible at bottom
- Apps work exactly like stock Android

**Chat active** (swipe up from Stone icon):
```
┌─────────────────────────┐
│   Embedded App (2/3)    │  ← App continues running
├─────────────────────────┤
│   Chat Interface (1/3)  │  ← AI assistant
└─────────────────────────┘
```

**Example interaction**:
- **Touch**: Open Spotify, search for song, tap play
- **Voice**: "Play Radiohead" → Same result
- **User chooses** which method, when

### Key Components

1. **Stone SystemUI** (Custom Android system interface)
   - `StoneManager`: Lifecycle integration
   - `StoneIcon`: Always-visible entry point
   - `StonePanel`: Sliding chat/voice interface

2. **System Apps** (Minimalist replacements)
   - `StoneLauncher`: 3x4 word grid home screen
   - `StoneSettings`: Settings with BroadcastReceiver API
   - `StoneTime`: Alarms, timers, stopwatch

3. **AI Infrastructure**
   - LiveKit agents for voice/text processing
   - MCP servers for third-party app control

### The "Head & Headless" Pattern

**Every StoneOS app has two interfaces**:

- **Head** (GUI): Traditional Android UI for human touch
- **Headless** (API): BroadcastReceiver for AI agent control

Both call the same Android system services → Perfect functional parity

**Example** (Wi-Fi control):
```java
// Human path: Touch switch in Settings
SettingsActivity → WifiManager.setWifiEnabled(true)

// AI path: Voice command "Turn on Wi-Fi"
BroadcastIntent → SettingsControlReceiver → WifiManager.setWifiEnabled(true)
                                             ↑ Same system call
```

---

## 🏗️ Development Process

### Ticket-Based Workflow

All work is organized through tickets in `/home/samuellarson/stone-os/tickets/`:

**Ticket types**:
- **Implementation tickets**: Specific features to build (OS Builder, App Builder, Emulator agents)
- **Research tickets**: Technical investigations (Gemini research agent)

**Ticket lifecycle**:
1. Architect creates ticket with SPECIFICATION
2. Coding agent implements, fills IMPLEMENTATION REPORT
3. If blocked, agent fills COMPLICATIONS & REVISIONS
4. When complete, ticket moves to `archive/`

**Example active tickets**:
- `023-fix-stonemanager-rebuild-systemui.md`
- `026-migrate-gcp-launch-cuttlefish.md`

See `tickets/CLAUDE.md` for detailed workflow.

### Architecture & Technical Decisions

All major technical decisions are documented in `GEMINI.md`:

**The Four Core Pillars**:
1. Forked `frameworks/base` approach (not device overlays)
2. Cuttlefish emulation strategy (not standard SDK emulator)
3. "Head & Headless" application architecture
4. `vendor/stone` as source of truth

**Architectural Decision Records (ADRs)**:
- ADR-001: Fork frameworks/base
- ADR-002: Cuttlefish for OS testing
- ADR-003: BroadcastReceiver for agent API

---

## 🔧 Build System

### Target Device
Google Pixel 8a (akita) with unlocked bootloader

### Build Environment
- **Platform**: Google Cloud Platform (GCP)
- **Instance type**: n2-standard-32 (32 vCPUs, 128GB RAM)
- **Pricing**: SPOT instances (~$0.23/hour)
- **Build time**: 25-35 minutes full build, 10-15 min incremental

### Build Process

**Our Workflow** (Fork-Based AOSP Development):

StoneOS modifies AOSP directly through a forked `frameworks/base` repository. This is **not** a device overlay approach - we maintain actual source code changes in the AOSP tree.

**Development cycle**:
```bash
# 1. Edit code in vendor/stone (source of truth)
vim vendor/stone/packages/SystemUI/src/com/android/systemui/stone/StoneManager.java

# 2. Sync to AOSP tree
./scripts/sync_vendor.sh

# 3. Build with standard AOSP tools
cd ~/aosp
source build/envsetup.sh
lunch aosp_cf_x86_64_phone-ap2a-eng
m SystemUI  # Or m for full OS build
```

**Key points**:
- We use Google's official `repo` tool for source management
- We use Google's official Soong build system (Android.bp files)
- Our custom code syncs into the AOSP tree before building
- We follow standard AOSP development patterns (not custom build scripts)

### Testing

**Cuttlefish emulator** (for OS testing):
```bash
cd ~/aosp
launch_cvd
# Access web UI at https://0.0.0.0:8443 in VNC session
```

**Standard emulator** (for standalone app testing):
- See research ticket for documentation (TBD)

---

## 📊 Project Status

### ✅ Completed (Archived Tickets)

- **Infrastructure**
  - Ticket #12: VNC environment on GCP
  - Ticket #15: StoneLauncher (3x4 grid home)
  - Ticket #16: StoneSettings (with Headless API)
  - Ticket #20: StoneTime (alarms & timers)
  - Ticket #21: Grayscale filter implementation
  - Ticket #22: Cuttlefish build target

- **Architecture**
  - Fork-based AOSP workflow established
  - "Head & Headless" pattern validated
  - Build automation via GCP

### 🚧 In Progress (Active Tickets)

- **Ticket #23**: Fix StoneManager CoreStartable implementation, rebuild SystemUI
- **Ticket #24**: Documentation reorganization (this update!)
- **Ticket #26**: Migrate GCP instance, launch Cuttlefish for first boot

### 🎯 Next Milestones

**Immediate**:
- [ ] First successful boot of StoneOS in Cuttlefish
- [ ] Verify StoneLauncher appears as home screen
- [ ] Verify StoneIcon visibility and swipe-up gesture
- [ ] Verify StonePanel animation

**Short-term**:
- [ ] Implement StoneManager start() logic
- [ ] Wire StoneIcon swipe to StonePanel toggle
- [ ] Connect StonePanel WebView to LiveKit agent
- [ ] Test full voice interaction flow

**Medium-term**:
- [ ] Deploy MCP servers for app control
- [ ] Integrate Spotify, Maps, Perplexity
- [ ] System-wide grayscale rendering
- [ ] Physical device testing (Pixel 8a)

---

## 📚 Documentation Guide

### For Different Audiences

| Audience | Document | Purpose |
|----------|----------|---------|
| **Human developers** | README.md (this file) | Project overview, getting started |
| **AI coding agents** | CLAUDE.md | Implementation patterns, build commands |
| **Architect/Research** | GEMINI.md | Architecture decisions, deep technical details |
| **All agents** | tickets/CLAUDE.md | Ticket workflow and process |

### Key Concepts to Understand

1. **Fork-based development**: We maintain a fork of AOSP's `frameworks/base` (not overlays)
2. **vendor/stone as source of truth**: All custom code lives here, synced to AOSP before builds
3. **Cuttlefish for testing**: Not the standard SDK emulator (requires KVM/nested virt)
4. **"Head & Headless" apps**: Every app has GUI and API with functional parity

---

## 🤝 Contributing

### If you're human:

1. Understand the architecture (read GEMINI.md)
2. Check active tickets to see what's being worked on
3. Propose new features via issues
4. Submit PRs following the "Head & Headless" pattern

### If you're an AI agent:

1. Read CLAUDE.md for implementation guide
2. Wait for ticket assignment from user
3. Follow ticket workflow (see tickets/CLAUDE.md)
4. Report complications if blocked

---

## 🔗 Key Links

- **Fork repository**: https://github.com/stone-pebble/stoneos-frameworks
- **Product specs**: `/home/samuellarson/stone-os/docs/STONEOS_SPECS.md`
- **Active tickets**: `/home/samuellarson/stone-os/tickets/`
- **Build outputs**: `/home/samuellarson/aosp/out/target/product/vsoc_x86_64/`

---

## 📜 License

MIT License - See repository for details

---

**StoneOS**: Where traditional apps and intelligent agents coexist. The user always chooses.
