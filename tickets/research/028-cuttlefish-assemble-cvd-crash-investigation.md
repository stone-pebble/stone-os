# Research Ticket #28: Investigate Cuttlefish assemble_cvd Crash on GCP n2-standard-32

**Status**: Active
**Assigned to**: Gemini (Research Agent)
**Priority**: Critical (blocks first boot - Ticket #26)
**Created**: 2025-10-23

---

## RESEARCH QUESTION

**Primary question**: Why is `assemble_cvd --helpxml` crashing with SIGABRT on our GCP n2-standard-32 instance, and what's the correct way to resolve it?

**Context from Ticket #26**:

The Emulator Agent successfully completed Phase 1 (GCP migration) but is blocked on Phase 2 (Cuttlefish launch):

**What's working**:
- ✅ KVM enabled and verified (`kvm-ok` passes)
- ✅ n2-standard-32 instance with nested virtualization
- ✅ Cuttlefish tools built with AOSP at `~/aosp/out/host/linux-x86/bin/`
- ✅ User in correct groups (kvm, cvdnetwork, render)
- ✅ AOSP build exists at `~/aosp/out/target/product/vsoc_x86_64/`
- ✅ `cvd` command launches server successfully

**What's broken**:
- ❌ `assemble_cvd --helpxml` crashes: `Aborted (core dumped)` (Signal 6 - SIGABRT)
- ❌ `launch_cvd` depends on `assemble_cvd --helpxml` and fails
- ❌ `cvd start` fails with "Unique ID allocation failed"

**Error**:
```bash
$ assemble_cvd --helpxml
Aborted (core dumped)
```

This appears to be either:
1. A bug in the AOSP-built `assemble_cvd` binary
2. Missing dependency/library on the system
3. Incompatibility with the GCP environment
4. Build artifact corruption

---

## RESEARCH OBJECTIVES

### 1. Identify Root Cause

Investigate:
- Is this a known issue with AOSP-built Cuttlefish tools?
- Does `assemble_cvd --helpxml` require specific system libraries?
- Are there version mismatches between Cuttlefish components?
- Is there a GCP-specific configuration issue?

### 2. Find Working Solution

Research the official recommended approach:
- Should we rebuild Cuttlefish tools? If so, which build target?
- Should we use pre-built Cuttlefish binaries from Android CI instead?
- Is there an official Cuttlefish setup script for GCP?
- What's the difference between AOSP-built vs pre-built Cuttlefish packages?

### 3. Document Correct Workflow

Determine the authoritative, Google-recommended way to set up Cuttlefish for custom AOSP builds on GCP.

---

## SPECIFIC QUESTIONS TO ANSWER

1. **Pre-built vs AOSP-built Cuttlefish**:
   - What's the official recommendation for custom AOSP builds?
   - Where to download pre-built Cuttlefish packages?
   - What's in `cuttlefish-common` vs `cuttlefish-user` packages?

2. **Build Target for Cuttlefish Tools**:
   - If rebuilding, what's the correct build command?
   - Is there a specific `m cuttlefish_host_tools` or similar target?
   - Does `lunch aosp_cf_x86_64_phone-ap2a-eng` automatically build host tools?

3. **System Dependencies**:
   - What libraries does `assemble_cvd` require?
   - Are there known Debian/Ubuntu package dependencies?
   - Could this be an `ldd` missing library issue?

4. **GCP-Specific Setup**:
   - Is there official GCP Cuttlefish documentation?
   - Are there known issues with n2-standard-32 instances?
   - Does Cuttlefish require specific kernel modules beyond KVM?

5. **Debugging the Crash**:
   - Should we run `assemble_cvd` under `gdb` or `strace`?
   - Are there Cuttlefish debug logs somewhere?
   - Could this be a known bug in Android 14 QPR2 Cuttlefish build?

---

## RESEARCH SOURCES

### Priority Sources (Check These First)

1. **Official Cuttlefish Documentation**:
   - https://source.android.com/docs/setup/create/cuttlefish
   - Look for "Known Issues" sections
   - GCP-specific instructions

2. **Android CI Pre-built Packages**:
   - https://ci.android.com/
   - Search for: `aosp-main` or `aosp_cf_x86_64_phone` builds
   - Look for: `cuttlefish-common`, `cuttlefish-user` .deb packages

3. **AOSP Issue Tracker**:
   - Search for: "assemble_cvd crash", "assemble_cvd SIGABRT"
   - Filter by: Cuttlefish component

4. **Google Groups / Mailing Lists**:
   - android-building Google Group
   - Cuttlefish-specific discussions

5. **GitHub Issues**:
   - https://github.com/google/android-cuttlefish (if exists)
   - Search for similar crash reports

### Secondary Sources

- Stack Overflow (Cuttlefish + assemble_cvd tags)
- Reddit r/androiddev (search: Cuttlefish GCP)
- Developer blogs about AOSP + Cuttlefish

---

## DESIRED OUTPUT

### 1. Root Cause Analysis

Document:
- What causes `assemble_cvd --helpxml` to crash
- Why AOSP-built tools might not work
- System dependency requirements

### 2. Recommended Solution

Provide **step-by-step instructions** for one of these approaches:

**Option A: Use Pre-built Cuttlefish** (if this is recommended)
```bash
# Download and install pre-built packages
wget [URL to cuttlefish-common.deb]
sudo dpkg -i cuttlefish-common_*.deb cuttlefish-user_*.deb
# ... exact steps
```

**Option B: Rebuild Cuttlefish Tools** (if AOSP-built should work)
```bash
# Correct build commands
cd ~/aosp
m [correct target for cuttlefish tools]
# ... exact steps
```

**Option C: Debug and Fix** (if it's a fixable configuration issue)
```bash
# Steps to identify and fix the issue
ldd ~/aosp/out/host/linux-x86/bin/assemble_cvd
# ... diagnostic steps
```

### 3. Updated Ticket #26 Specification

Based on your findings, provide revised instructions for Phase 2 of Ticket #26 that will actually work.

---

## SUCCESS CRITERIA

- [ ] Root cause of `assemble_cvd --helpxml` crash identified
- [ ] Recommended solution documented with exact commands
- [ ] Solution tested against official Google documentation
- [ ] Any known issues or caveats documented
- [ ] Revised Ticket #26 Phase 2 instructions provided
- [ ] Reference to authoritative source (Google docs, issue tracker, etc.)

---

## URGENCY

**Critical** - This blocks:
- First boot of StoneOS (Ticket #26)
- All future OS testing and development
- Validation of our entire fork-based workflow

**Timeline**: As soon as possible - this is the top blocker for the project.

---

## RESEARCH FINDINGS

**Gemini (Research Agent)**: Fill this section when research is complete.

### Root Cause
The `assemble_cvd` crash (SIGABRT) is caused by an improperly configured host environment. Our previous approach of using Cuttlefish tools built directly from the AOSP source (`m debs-package` or as part of `m`) is incorrect.

The official AOSP documentation makes it clear that the host machine must be prepared by installing official, pre-built Debian packages. These packages do more than just provide binaries; they set up critical system-level components, including:
-   `udev` rules for device management.
-   The `cvdnetwork` user group for virtual networking.
-   Required kernel modules.
-   Default configuration files that `assemble_cvd` depends on.

The AOSP-built tools are crashing because they are being run in an environment that is missing these essential host configurations.

### Recommended Solution
The definitive, Google-recommended solution is to **ignore the Cuttlefish tools built from our AOSP tree** and instead **install the official, pre-built host packages** from the Android Continuous Integration server.

Once the host environment is correctly set up with these official packages, we will use those stable tools to launch our own custom-built StoneOS images (`system.img`, etc.) that were successfully created in Ticket #22.

This approach separates the stable **host setup** from the experimental **target OS**, which is the intended workflow.

### Why This Approach
This is the explicit "Get Started" procedure from the official Cuttlefish documentation on `source.android.com`. It guarantees a known-good, stable host environment, eliminating the risk of bugs or misconfigurations from our AOSP-built tools.

### Alternative Approaches Considered
-   **Debugging the AOSP-built tools:** Rejected. This is a high-effort, low-reward path. We would be trying to manually replicate the complex setup that the official Debian packages provide automatically.
-   **Rebuilding the OS:** Rejected. The OS build from Ticket #22 was successful. The problem is not with our OS images; it is with the host environment.

### System Requirements Identified
-   **KVM:** Must be enabled and accessible (Completed in Ticket #26, Phase 1).
-   **User Groups:** The user running Cuttlefish must be a member of the `kvm`, `cvdnetwork`, and `render` groups.
-   **Official Host Packages:** The `cuttlefish-common` and `cuttlefish-user` Debian packages must be installed.

### Revised Ticket #26 Instructions
The Emulator Agent should abandon the previous approach and execute the following steps precisely.

---

#### **Revised Phase 2: Install Official Cuttlefish Host Environment**

1.  **Download the Cuttlefish Host Package Installer Script:**
    ```bash
    cd ~
    wget https://ci.android.com/builds/latest/branches/aosp-main/targets/aosp_cf_x86_64_phone-userdebug/latest/cuttlefish-host-resources.sh
    chmod +x cuttlefish-host-resources.sh
    ```

2.  **Run the Script to Download Packages:**
    This script will download the latest stable `cuttlefish-common` and `cuttlefish-user` Debian packages into your current directory.
    ```bash
    ./cuttlefish-host-resources.sh
    ```

3.  **Install the Debian Packages:**
    This command will install the packages and configure the system.
    ```bash
    sudo dpkg -i cuttlefish-common_*_amd64.deb cuttlefish-user_*_amd64.deb
    ```
    *If you encounter dependency errors, run `sudo apt-get install -f` to fix them.*

4.  **Add User to Required Groups:**
    ```bash
    sudo usmod -aG kvm,cvdnetwork,render $USER
    ```

5.  **Restart the VNC Session:**
    For the group changes to take effect, you **must** restart the VNC server.
    ```bash
    vncserver -kill :1
    vncserver
    ```
    *After this, reconnect your VNC viewer.*

#### **Revised Phase 3: Launch StoneOS with Official Tools**

1.  **Navigate to the AOSP Build Output Directory:**
    The `launch_cvd` command should be run from the directory containing our built OS images.
    ```bash
    cd ~/aosp/out/target/product/vsoc_x86_64
    ```

2.  **Launch Cuttlefish:**
    The newly installed `launch_cvd` will automatically find and use the `system.img` and other files in this directory.
    ```bash
    launch_cvd
    ```

3.  **Verify in Browser:**
    Open the browser inside your VNC session and navigate to `https://0.0.0.0:8443` to see the device boot.

---
### References
-   [Official Cuttlefish "Get Started" Guide](https://source.android.com/docs/setup/create/cuttlefish)
-   [Android CI Server](https://ci.android.com/)

### Known Issues to Watch For
-   The `usermod` command requires a logout/login to apply group changes. Restarting the VNC server is the correct way to achieve this in our environment.
-   Ensure you are in the correct build output directory (`~/aosp/out/target/product/vsoc_x86_64/`) before running `launch_cvd`.

---

## NOTES FOR RESEARCH AGENT

- Focus on **official Google documentation** first
- Check Android CI for pre-built packages (this might be the standard approach)
- The AOSP source tree is at: `~/aosp/`
- Build target was: `lunch aosp_cf_x86_64_phone-ap2a-eng`
- Instance specs: GCP n2-standard-32, Debian-based, KVM enabled
- This is blocking the entire project - prioritize speed and confidence in solution
