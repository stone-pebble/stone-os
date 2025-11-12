# CLAUDE.md - Implementation Guide for Coding Agents

**Last Updated**: 2025-10-23
**For**: OS Builder Agent, App Builder Agent, Emulator Agent

---

## How to Use This Guide

This guide is for AI coding agents executing implementation tickets for StoneOS.

**Your workflow**:
1. User assigns you a ticket from `/home/samuellarson/stone-os/tickets/`
2. Read the ticket's SPECIFICATION section carefully
3. Consult this CLAUDE.md for StoneOS-specific implementation patterns
4. Execute the work using your available tools
5. Fill in the ticket's IMPLEMENTATION REPORT section
6. If blocked, fill in COMPLICATIONS & REVISIONS and stop

**Important**: Read `/home/samuellarson/stone-os/tickets/CLAUDE.md` for detailed ticket workflow instructions.

---

## What is StoneOS?

StoneOS is a minimalist, AI-augmented Android ROM built on Android 14 (AOSP android-14.0.0_r61).

**Core concept**: "Choice-First, Not Voice-First"
- Users can interact via **touch** OR **conversational AI**
- Traditional Android apps run normally (in grayscale)
- AI agents can control apps through MCP servers
- User chooses interaction method moment-by-moment

**Target device**: Google Pixel 8a (akita) with unlocked bootloader
**Build environment**: GCP n2-standard-32 instances
**Testing environment**: Cuttlefish virtual device

### Key Components

1. **Stone SystemUI**: Custom Android system interface
   - `StoneManager`: Lifecycle manager (CoreStartable)
   - `StoneIcon`: Always-visible 🗿 icon at bottom of screen
   - `StonePanel`: Sliding chat interface (1/3 of screen)

2. **System Apps**: Minimalist replacements for AOSP defaults
   - `StoneLauncher`: 3x4 grid home screen (replaces Launcher3)
   - `StoneSettings`: Settings app with BroadcastReceiver API
   - `StoneTime`: Alarms, timers, stopwatch

3. **AI Infrastructure**:
   - LiveKit agents for voice/text processing
   - MCP servers for app control (Spotify, Maps, etc.)

---

## Repository Structure

```
/home/samuellarson/stone-os/          # THIS IS YOUR WORKING DIRECTORY
├── vendor/stone/                      # Source of truth for Stone code
│   ├── packages/SystemUI/src/com/android/systemui/stone/
│   │   ├── StoneManager.java         # SystemUI lifecycle manager
│   │   ├── StoneIcon.java           # Bottom icon with swipe detection
│   │   └── StonePanel.java          # Sliding chat interface
│   └── packages/apps/
│       ├── StoneLauncher/           # Home screen app
│       ├── StoneSettings/           # Settings app
│       └── StoneTime/               # Time management app
├── tickets/                          # Your work assignments
│   ├── CLAUDE.md                    # Ticket workflow guide
│   ├── [NNN-active-ticket.md]       # Active tickets
│   └── archive/                     # Completed tickets
├── scripts/
│   ├── sync_vendor.sh              # Sync vendor/stone → AOSP
│   └── build_stoneos.sh            # GCP build automation
└── docs/
    └── STONEOS_SPECS.md            # Product specifications

/home/samuellarson/aosp/              # AOSP source tree (150GB)
├── frameworks/base/packages/SystemUI/  # Where SystemUI builds from
├── vendor/stone/                       # Synced from stone-os/vendor/
└── out/target/product/vsoc_x86_64/    # Build outputs
```

---

## Development Workflows

### Workflow 1: Working with Stone SystemUI Components

**Files**: `StoneManager.java`, `StoneIcon.java`, `StonePanel.java`

**Source of truth**: `/home/samuellarson/stone-os/vendor/stone/packages/SystemUI/src/com/android/systemui/stone/`

**Steps**:

1. **Edit** source files in `stone-os/vendor/stone/...`

2. **Sync to AOSP** frameworks/base:
   ```bash
   cd ~/stone-os
   ./scripts/sync_vendor.sh

   # Then manually copy to frameworks/base
   cp ~/aosp/vendor/stone/packages/SystemUI/src/com/android/systemui/stone/*.java \
      ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
   ```

3. **Build SystemUI**:
   ```bash
   cd ~/aosp
   source build/envsetup.sh
   lunch aosp_cf_x86_64_phone-ap2a-eng
   m SystemUI
   ```

4. **Verify classes in APK**:
   ```bash
   cd /tmp
   unzip -q ~/aosp/out/target/product/vsoc_x86_64/system_ext/priv-app/SystemUI/SystemUI.apk 'classes*.dex'
   strings classes*.dex | grep -i "StoneManager\|StoneIcon\|StonePanel"
   ```

**Build output**: `~/aosp/out/target/product/vsoc_x86_64/system_ext/priv-app/SystemUI/SystemUI.apk` (~42MB)

---

### Workflow 2: Working with System Apps

**Apps**: `StoneLauncher`, `StoneSettings`, `StoneTime`

**Source of truth**: `/home/samuellarson/stone-os/vendor/stone/packages/apps/[AppName]/`

**Steps**:

1. **Edit** app files in `stone-os/vendor/stone/packages/apps/[AppName]/`

2. **Sync to AOSP**:
   ```bash
   cd ~/stone-os
   ./scripts/sync_vendor.sh
   ```

3. **Build single app** (fast, ~5-10 min):
   ```bash
   cd ~/aosp
   source build/envsetup.sh
   lunch aosp_cf_x86_64_phone-ap2a-eng
   m [AppName]  # e.g., m StoneSettings
   ```

4. **Build full system** (slow, ~30-60 min):
   ```bash
   cd ~/aosp
   source build/envsetup.sh
   lunch aosp_cf_x86_64_phone-ap2a-eng
   m  # Full build
   ```

**Build outputs**: `~/aosp/out/target/product/vsoc_x86_64/system_ext/priv-app/[AppName]/[AppName].apk`

---

### Workflow 3: Testing in Cuttlefish Emulator

**Prerequisites**: Instance must have KVM enabled (n2-standard-32 machine type)

**Steps**:

1. **Ensure full system image is built**:
   ```bash
   ls ~/aosp/out/target/product/vsoc_x86_64/system.img  # Should exist (650MB+)
   ls ~/aosp/out/host/linux-x86/bin/launch_cvd          # Should exist
   ```

2. **Launch Cuttlefish**:
   ```bash
   cd ~/aosp
   source build/envsetup.sh
   lunch aosp_cf_x86_64_phone-ap2a-eng
   launch_cvd
   ```

3. **Access via web UI** (from within VNC session):
   - Open browser in VNC
   - Navigate to `https://0.0.0.0:8443`

4. **Verify Stone components**:
   - StoneLauncher should be the home screen (3x4 grid)
   - StoneIcon should be visible at bottom
   - Swipe up from StoneIcon should reveal StonePanel

**Stopping Cuttlefish**:
```bash
stop_cvd
```

---

## Core Architectural Patterns

### Pattern 1: The "Head & Headless" Application Architecture

**Principle**: Every StoneOS app must be controllable by both humans (GUI) and AI agents (API).

**Implementation**:

1. **The "Head" (GUI Layer)**: Standard Android Activity
   - XML layouts, touch interactions
   - Calls Android system services directly
   - Example: `SettingsActivity.java` with SeekBar for brightness

2. **The "Headless" (API Layer)**: BroadcastReceiver
   - Listens for Intent actions
   - Calls the SAME system services as GUI
   - Example: `SettingsControlReceiver.java` receives `SET_BRIGHTNESS` intent

**Code example**:

```java
// In AndroidManifest.xml
<receiver
    android:name=".SettingsControlReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="com.stoneos.settings.SET_BRIGHTNESS" />
    </intent-filter>
</receiver>

// In SettingsControlReceiver.java
public class SettingsControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.stoneos.settings.SET_BRIGHTNESS".equals(intent.getAction())) {
            int level = intent.getIntExtra("level", 128);
            Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, level);
        }
    }
}
```

**Testing the API**:
```bash
adb shell am broadcast \
  -a com.stoneos.settings.SET_BRIGHTNESS \
  --ei level 200
```

**Documentation**: Each app with a Headless API must have a `TOOLS.md` file documenting all Intent actions.

---

### Pattern 2: The Forked frameworks/base Approach

**Why**: Android's Soong build system evaluates source trees at parse-time. Device overlays only work for resources (XML), not Java/Kotlin code.

**How it works**:

1. We maintain a fork of AOSP's `frameworks/base` at: https://github.com/stone-pebble/stoneos-frameworks
2. Fork contains our custom Stone components in the source tree
3. AOSP build pulls from our fork via `.repo/local_manifests/stoneos.xml`

**What this means for you**:
- Stone Java files must exist in `frameworks/base/packages/SystemUI/src/com/android/systemui/stone/` BEFORE build starts
- You cannot patch files at runtime
- Always sync from `vendor/stone/` → `frameworks/base/` before building

**Critical**: Do NOT create separate static libraries for Stone components. They need access to SystemUI framework classes (CoreStartable, etc.), which creates circular dependencies.

---

### Pattern 3: Dagger Dependency Injection in SystemUI

**Core concept**: SystemUI uses Dagger for dependency injection. Custom components must follow specific patterns.

**Required pattern for Stone components**:

```java
import com.android.systemui.CoreStartable;
import com.android.systemui.dagger.SysUISingleton;
import javax.inject.Inject;

@SysUISingleton
public class StoneManager implements CoreStartable {  // IMPLEMENTS, not extends!

    private final Context mContext;

    @Inject  // Constructor injection
    public StoneManager(Context context) {
        mContext = context;  // Store context, no super() call
        // ... initialization
    }

    @Override
    public void start() {
        // Called by SystemUI on boot
    }

    @Override
    public void dump(PrintWriter pw, String[] args) {
        // Required by Dumpable interface
    }
}
```

**Registration** (in `SystemUICoreStartableModule.kt`):

```kotlin
@Binds
@IntoMap
@ClassKey(StoneManager::class)
abstract fun bindStoneManager(sysui: StoneManager): CoreStartable
```

**Common mistake**: Using `extends CoreStartable` instead of `implements CoreStartable`. CoreStartable is an **interface**, not a class.

---

## Build System Reference

### Build Targets

**For Cuttlefish (our standard)**:
```bash
lunch aosp_cf_x86_64_phone-ap2a-eng
```

**What this means**:
- `aosp_cf_x86_64_phone`: Cuttlefish virtual device, x86_64 architecture
- `ap2a`: Android 14 QPR2 release
- `eng`: Engineering build (includes debug tools)

**NEVER use**: `aosp_cf_x86_64_phone-eng` (missing release specifier) - this will fail

### Build Commands

**Build everything**:
```bash
m
```

**Build specific module**:
```bash
m SystemUI
m StoneSettings
m StoneLauncher
```

**Clean build** (when Android.bp changes or files won't compile):
```bash
m clean
m SystemUI
```

**Incremental builds**: If only Java files changed, just run `m [module]` without clean.

### Critical Build Notes

**repo sync MUST use -j4**:
```bash
repo sync -c -j4  # NOT -j8 or higher!
```
Why: Google's git servers rate-limit. Higher concurrency causes HTTP 429 errors.

**Glob patterns are evaluated at parse-time**:
- `"src/**/*.java"` in Android.bp includes ALL .java files in src/ tree
- Stone components are automatically included
- No need to explicitly list `stone/*.java`

---

## File Locations Reference

### Source Files (Edit Here)

| Component | Source of Truth |
|-----------|----------------|
| StoneManager, StoneIcon, StonePanel | `/home/samuellarson/stone-os/vendor/stone/packages/SystemUI/src/com/android/systemui/stone/` |
| StoneLauncher | `/home/samuellarson/stone-os/vendor/stone/packages/apps/StoneLauncher/` |
| StoneSettings | `/home/samuellarson/stone-os/vendor/stone/packages/apps/StoneSettings/` |
| StoneTime | `/home/samuellarson/stone-os/vendor/stone/packages/apps/StoneTime/` |

### Build Files (AOSP Tree)

| Component | Build Location |
|-----------|---------------|
| SystemUI source | `~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/` |
| System apps source | `~/aosp/vendor/stone/packages/apps/[AppName]/` |
| SystemUI Android.bp | `~/aosp/frameworks/base/packages/SystemUI/Android.bp` |
| Dagger bindings | `~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/dagger/SystemUICoreStartableModule.kt` |

### Build Outputs

| Component | Output Location |
|-----------|----------------|
| SystemUI.apk | `~/aosp/out/target/product/vsoc_x86_64/system_ext/priv-app/SystemUI/SystemUI.apk` |
| App APKs | `~/aosp/out/target/product/vsoc_x86_64/system_ext/priv-app/[AppName]/[AppName].apk` |
| system.img | `~/aosp/out/target/product/vsoc_x86_64/system.img` |
| super.img | `~/aosp/out/target/product/vsoc_x86_64/super.img` |
| Cuttlefish tools | `~/aosp/out/host/linux-x86/bin/launch_cvd` |

---

## Common Issues & Solutions

### Issue: "Invalid lunch combo" error

**Symptom**:
```
Invalid lunch combo: aosp_cf_x86_64_phone-eng
Valid combos must be of the form <product>-<release>-<variant>
```

**Solution**: Use the full format with release specifier:
```bash
lunch aosp_cf_x86_64_phone-ap2a-eng
```

---

### Issue: Stone classes not in SystemUI.apk

**Symptom**: Build succeeds but DEX verification shows no Stone classes.

**Causes**:
1. Files not in `frameworks/base` before build
2. Build cache is stale

**Solution**:
```bash
# Verify files exist
ls ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/

# Clean and rebuild
cd ~/aosp
m clean
m SystemUI
```

---

### Issue: CoreStartable compilation error

**Symptom**: `cannot find symbol: class CoreStartable` or `unexpected interface type`

**Cause**: Using `extends CoreStartable` instead of `implements CoreStartable`

**Solution**: CoreStartable is an interface. Always use `implements`:
```java
public class StoneManager implements CoreStartable {
    private final Context mContext;

    @Inject
    public StoneManager(Context context) {
        mContext = context;  // No super() call
    }
}
```

---

### Issue: KVM not available for Cuttlefish

**Symptom**: `launch_cvd` fails with KVM error

**Cause**: GCP instance doesn't support nested virtualization

**Solution**: Instance must be `n2-standard-32` machine type. See Ticket #26 for migration steps.

---

## Tool Integration

### TOOLS.md Files

Each app with a Headless API has a `TOOLS.md` file documenting all available Intent actions.

**Example** (`StoneSettings/TOOLS.md`):

```markdown
## set_brightness

Sets screen brightness level.

**Intent Action**: `com.stoneos.settings.SET_BRIGHTNESS`

**Parameters**:
- `level` (int, required): Brightness level 0-255

**Example**:
```bash
adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 128
```
```

**As a coding agent**: When implementing a Headless API, always create or update the corresponding `TOOLS.md` file.

---

## Important Reminders

1. **Always sync from vendor/stone before building**
   ```bash
   cd ~/stone-os && ./scripts/sync_vendor.sh
   ```

2. **SystemUI requires copying to frameworks/base**
   ```bash
   cp ~/aosp/vendor/stone/packages/SystemUI/src/com/android/systemui/stone/*.java \
      ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
   ```

3. **Verify DEX contents after build**
   ```bash
   cd /tmp
   unzip -q [path-to-apk] 'classes*.dex'
   strings classes*.dex | grep -i "Stone"
   ```

4. **Use correct lunch target**
   ```bash
   lunch aosp_cf_x86_64_phone-ap2a-eng  # Note the -ap2a-
   ```

5. **Fill in ticket reports completely**
   - Include command outputs
   - Show verification results
   - List build artifact locations and sizes

---

## Getting Help

**If you're blocked**:
1. Fill in the COMPLICATIONS & REVISIONS section of your ticket
2. Include full error messages and logs
3. STOP and report to user
4. Architect will research and revise the ticket

**If the specification is unclear**:
- Ask the user for clarification BEFORE starting work
- Don't guess or improvise

**If you need architectural context**:
- Consult GEMINI.md (but don't use it for implementation details)
- GEMINI.md explains WHY we do things a certain way
- CLAUDE.md (this file) explains HOW to do them

---

**Remember**: You are an implementation specialist. Your job is to execute ticket specifications precisely, document your work clearly, and report complications honestly. The Architect handles strategy and research.
