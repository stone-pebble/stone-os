# Research Ticket #25: Standalone App Testing Workflow with Standard Android Emulator

**Status**: Active
**Assigned to**: Gemini (Research Agent)
**Priority**: High (blocks app development workflow)
**Created**: 2025-10-23

---

## RESEARCH QUESTION

**Primary question**: How do we test standalone Stone system apps (StoneLauncher, StoneSettings, StoneTime) in a standard Android emulator without requiring a full OS build?

**Context**:
- We have built two system apps already (StoneSettings, StoneLauncher, StoneTime)
- Full OS builds via Cuttlefish take 30-60 minutes
- We need rapid iteration for UI development
- Standard Android SDK emulator should work for standalone `.apk` testing
- Apps need to be tested on stock Android before being integrated into StoneOS

**Why this is needed**:
- Fast iteration during app development (build one app in 5-10 min, not whole OS)
- UI/UX testing and refinement
- Testing app logic before OS integration
- Developing new apps quickly

**Current gap**:
- Cuttlefish is for OS testing (requires full system image)
- Don't have documented workflow for standalone app testing
- CLAUDE.md mentions this but says "see research ticket for documentation (TBD)"

---

## DESIRED OUTPUT

Please provide comprehensive documentation covering:

### 1. Standard Android Emulator Setup
- How to install Android SDK and emulator on the GCP instance
- Which system image to use (Android 14 compatible)
- How to create and configure an AVD (Android Virtual Device)
- VNC integration (emulator must run in our existing VNC session)

### 2. Building Standalone Apps
- Build command for single app: `m StoneSettings` vs full `m` build
- Where the `.apk` files are located after build
- How to verify APK was built successfully

### 3. Installing Apps to Emulator
- How to install an APK to running emulator: `adb install`
- How to grant system permissions to sideloaded apps (if needed)
- How to launch the app after installation

### 4. Testing Workflow
- How to test the "Head" (GUI) - manual interaction in emulator
- How to test the "Headless" (API) - `adb shell am broadcast` commands
- How to check logs: `adb logcat` filtering
- How to take screenshots for documentation

### 5. Limitations
- What WON'T work on stock Android (e.g., SystemUI integration)
- What WILL work (standalone app logic, UI, BroadcastReceiver API)
- When to use emulator vs when to use Cuttlefish

### 6. Integration with Existing Workflow
- How this fits with `vendor/stone` source of truth
- When to test in emulator vs when to do full OS build
- How to transition from emulator testing to Cuttlefish testing

---

## RESEARCH FINDINGS

**Gemini (Research Agent)**: Fill this section with your research.

### Summary
[2-3 sentence overview of what you found]

### Recommended Approach
[The workflow you recommend and why]

### Standard Emulator Setup Steps
[Detailed step-by-step installation and configuration]

### Building and Testing Workflow
[Complete workflow from build to test]

### Code Examples
[Example commands for build, install, test]

### Limitations and Caveats
[What doesn't work, when to use Cuttlefish instead]

### References
[Links to Android SDK docs, emulator docs, etc.]

### Next Steps for Architect
[What ticket should be created based on this research]

---

## DELIVERABLE

Create a new document: `docs/STANDALONE_APP_TESTING.md` with a complete guide that can be referenced by:
- CLAUDE.md (link to it in "Workflow 2: Working with System Apps")
- Future app development tickets
- Human developers working on UI

This guide should be clear enough that a coding agent can follow it without additional research.
