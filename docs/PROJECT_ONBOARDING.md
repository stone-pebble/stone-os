# StoneOS Project: Exhaustive Technical History & Onboarding

**To:** New Collaborator
**From:** Samuel Larson
**Date:** November 7, 2025

**Objective:** This document provides a complete, granular technical history of the StoneOS project. It is not a summary, but a detailed log of the actions taken, the specific errors encountered, the hypotheses tested, and the analysis that led to the current project state. Its purpose is to provide the full, unassailable context required for technical contribution and to prevent the repetition of failed development paths.

---

## 1. Project Definition

*   **Name:** StoneOS
*   **Base:** AOSP `android-14.0.0_r61`
*   **Objective:** A custom version of Android 14 where core functions are controllable via a standard touch UI and a conversational AI agent.
*   **Custom Components:**
    *   **SystemUI:** `StoneIcon` and `StonePanel` integrated into `frameworks/base`.
    *   **System Apps:** `StoneLauncher`, `StoneSettings`, `StoneTime`.
*   **Immediate Goal:** Achieve a successful boot of StoneOS on a physical **Google Pixel 8a (codename: "akita")**.

---

## 2. Environment Specification

*   **Build Server:** Google Cloud Platform (GCP) `n2-standard-32` VM.
*   **Host OS:** **Ubuntu 24.04 LTS**.
    *   **Analysis:** This modern host OS has been identified as a primary source of incompatibility with the Android 14-era AOSP toolchain.
*   **AOSP Source Location:** `~/aosp/` on the build server.
*   **Custom Code Location:** `~/stone-os/vendor/stone/`.

---

## 3. Investigation Log: The Exhaustive Search for a Viable Emulator

The initial project strategy was to use an emulator for testing. This strategy was pursued for weeks and ultimately abandoned after every conceivable path was exhausted. The following is a detailed log of this investigation.

### 3.1. Attempt #1: The Standard Android SDK Emulator

*   **Action:** An `x86_64` build of StoneOS was compiled. The `emulator` binary from the Android SDK was used to launch it.
*   **Observed Error:** The emulator process launched, but the Android OS boot sequence did not complete. The process was characterized by extreme slowness.
*   **Root Cause Analysis:** The GCP VM does not expose nested virtualization. This prevents the use of KVM for hardware acceleration. The emulator falls back to QEMU's TCG (Tiny Code Generator) for full software emulation of the CPU, which is not performant enough to boot a modern Android version in a reasonable timeframe.
*   **Conclusion:** This path is not viable on the current cloud infrastructure.

### 3.2. Attempt #2: The Cuttlefish Virtual Device Saga

Cuttlefish is the official AOSP emulator. This was the primary focus of the virtualization effort. The core conflict was a deep incompatibility between the Cuttlefish toolchain and the Ubuntu 24.04 host environment.

#### 3.2.1. Path A: Using AOSP-Built Host Tools

The first logical path was to use the Cuttlefish tools that were built directly from our AOSP source tree. This ensures perfect version alignment between the tools and the OS.

*   **Initial Failure: The `f2fs-tools` Build-Time Catastrophe.**
    *   **Action:** Run `m` on the `aosp_cf_x86_64_phone` target.
    *   **Observed Error:** The build itself would fail when trying to build a required component (`openwrt_rootfs`). The log showed `Error: open /dev/loop0 failed errno:13` followed by `Error: Not available on mounted device!`.
    *   **Analysis:** The version of `make_f2fs` built from the AOSP source attempts to access loopback devices. The AOSP build sandbox (`sbox`) blocks this access, causing the tool to fail.
    *   **Failed Workaround #1: Symlinking.** I tried replacing the buggy AOSP `make_f2fs` with the working system version (`/usr/sbin/make_f2fs`). This failed because the `sbox` sandbox is hermetic and does not respect `PATH` overrides or changes to the `out/` directory after the build starts.
    *   **Failed Workaround #2: Disabling Sandbox.** I hypothesized an escape hatch (`DISABLE_SBOX_SANDBOX=true`). Research proved this variable does not exist.
    *   **Failed Workaround #3: `cc_prebuilt_binary` Override.** I attempted to use Soong's `overrides` feature to replace the module. This also failed to influence the sandboxed build environment.
    *   **Failed Workaround #4: Incompatible System Tools.** I discovered that even if I could replace `make_f2fs`, the sister tool `sload_f2fs` from the system packages had a different command-line interface (`-f` flag) than the AOSP version, making it incompatible with the build scripts.
    *   **Final Resolution:** After exhausting all workarounds, I discovered this was a known bug. The solution was to patch the AOSP source code for `external/f2fs-tools` by cherry-picking two upstream commits from `aosp/main`: `14197d5f` and `efcff4b`. **This successfully fixed the build-time error.**

*   **Second Failure: The `crosvm` Run-Time Crash.**
    *   **Action:** With a complete, patched set of AOSP-built tools, I attempted to launch Cuttlefish.
    *   **Observed Error:** The launch would begin but then crash when the virtual machine monitor (`crosvm`) tried to initialize.
    *   **Log Snippet:** `[ERROR crosvm] exiting with error 1: the architecture failed to build the vm` followed by `failed to create a PCI root hub: failed to create proxy device: Failed to configure tube: failed to receive packet: Connection reset by peer (os error 104)`.
    *   **Analysis:** This is a low-level IPC failure within `crosvm`. It indicates a fundamental incompatibility between the `crosvm` binary (built from Android 14 source) and the KVM interface provided by the modern Linux kernel on Ubuntu 24.04.

#### 3.2.2. Path B: Using Pre-Built Host Tools

Since my self-built tools were failing at runtime, the next logical path was to use official, pre-built binaries from Google.

*   **Initial Failure: Version Mismatch (Too New).**
    *   **Action:** Installed the latest stable Cuttlefish Debian packages (e.g., `cuttlefish-common` v1.29.0).
    *   **Observed Error:** `Could not read config file ... fetcher_config.json`.
    *   **Analysis:** The latest Cuttlefish tools are too new. They expect modern build artifacts that are not generated by the older Android 14 build process.

*   **Second Failure: `fetch_cvd` Incompatibility.**
    *   **Action:** Used the `fetch_cvd` tool to attempt to download a "period-correct" build for our branch.
    *   **Observed Error:** `404 Not Found` for branch `aosp-android14-qpr2`.
    *   **Analysis:** The public Git branch names used in `repo` do not map directly to the internal build target names used by Google's Build API and CI server. The `fetch_cvd` tool from our AOSP branch was also too old and did not support the modern `--branch` flag syntax.

*   **Third Failure: The Hybrid Environment Paradox.**
    *   **Action:** I discovered that the AOSP tools had a hardcoded dependency on a script (`/usr/lib/cuttlefish-common/bin/capability_query.py`) that is only provided by the Debian packages. I installed the foundational Debian package (`cuttlefish-common`) and then attempted to use my AOSP-built binaries.
    *   **Result:** This resolved the `capability_query.py` error but immediately failed with the **exact same `crosvm` PCI hub crash**.

#### 3.2.3. Cuttlefish: Final Conclusion

This exhaustive process led to one conclusion: for the specified environment (GCP `n2-standard-32` + Ubuntu 24.04 + AOSP `android-14.0.0_r61`), Cuttlefish is not a viable path. The host kernel is believed to be incompatible with the Android 14-era `crosvm` binary, regardless of whether that binary is built from source or downloaded pre-built.

---

## 4. Pivot to Physical Hardware & The "Soft Brick" Incident

Following the virtualization failures, the strategy shifted to flashing a physical Pixel 8a. The first attempt resulted in an unresponsive device (a "soft brick") and provided critical information about the correct build and flash procedures.

### 4.1. The Incident

*   **Action:** An `ARM64` build was created for the Pixel 8a (`aosp_akita` target). An attempt was made to flash this build to a secondary device.
*   **Result:** The device became unresponsive after a failed `fastboot` command. It is currently in a recovery process (extended charging) and is expected to be recoverable.
*   **Analysis:** The failure was a direct result of two procedural errors.

*   **Error #1: Incomplete OS Package.**
    *   **Action:** The OS was built with the `m` command.
    *   **Finding:** For modern Android devices with "dynamic partitions," `m` is insufficient. It builds the components (`system.img`, `vendor.img`, etc.), but does not package them into the required `super.img` file.
    *   **Correction:** The `m dist` command (or a similar packaging command) must be run after the main build to generate `super.img`.

*   **Error #2: Incorrect Flashing Procedure.**
    *   **Action:** Without a `super.img`, an attempt was made to flash partitions individually from the standard bootloader (e.g., `fastboot flash system system.img`).
    *   **Finding:** The bootloader on modern Pixel devices cannot write to individual dynamic partitions. The command `fastboot flash system system.img` will fail with `FAILED (remote: 'partition (system) not found')`.
    *   **Correction:** The correct command is `fastboot flash super super.img`.

---

## 5. Current Strategy & Action Plan

This is the active project plan.

1.  **Recover Secondary Device:** The unresponsive LineageOS device is undergoing recovery. The plan is to restore it by flashing the official Google Factory Image once it becomes responsive.

2.  **Create a Complete, Flashable Build:** An `ARM64` build for the Pixel 8a (`aosp_akita` target) is in progress on the cloud server. After the `m` command completes, the next step is to run `m dist` to generate all flashable images, including `super.img`.

3.  **Wait for Primary Device OEM Unlock:** The primary Pixel 8a is in a **7-14 day OEM unlock waiting period**. This is a non-bypassable security feature that is being monitored daily.

4.  **Flash Primary Device:** Once the OEM unlock is available and a complete build package with `super.img` is generated, the flash will be performed using the `fastboot flash super super.img` procedure.

---

## 6. Areas for Technical Exploration

The following are areas for potential contribution.

*   **Virtualization:** Investigate Cuttlefish viability in a different host environment (e.g., a local Linux machine with an older LTS kernel like Ubuntu 22.04).
*   **Build & Packaging:** Determine the most efficient build command to generate `super.img` (`m dist`, `m otatools`, etc.). Investigate the creation of a recovery-flashable `update.zip`.
*   **Flashing Automation:** Develop a script for the flashing process that includes pre-flight checks (device unlock status, image architecture), flashes partitions in the correct order, and includes error handling.