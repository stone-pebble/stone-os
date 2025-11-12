# AOSP Lunch Targets

**Source:** [Building Android](https://source.android.com/docs/setup/build/building)
**Scrape Date:** 2025-10-23

---

## Overview

The `lunch` command is used to configure the build target for an AOSP compilation. It tells the build system which product to build, for which Android release, and with which set of tools and permissions.

## Format

The official format for a `lunch` target is a string composed of three parts, separated by hyphens:

`lunch <product_name>-<release_config>-<build_variant>`

<!-- StoneOS Note: Our primary build target is `aosp_cf_x86_64_phone-ap2a-eng`. -->

### 1. `product_name`

This defines the specific product configuration, including the device type and hardware architecture.
-   `aosp_x86_64`: A generic AOSP build for an x86_64 architecture.
-   `aosp_cf_x86_64_phone`: A specific AOSP build for the **Cuttlefish** x86_64 phone emulator. The `_cf` is critical and enables the build of all necessary Cuttlefish host tools.

### 2. `release_config`

This specifies the Android release version.
-   `ap2a`: This is the release code for **Android 14 QPR2**.

### 3. `build_variant`

This defines the type of build, controlling which modules are included and the level of debugging and security.

-   **`eng` (Engineering):**
    -   **Purpose:** For active development by engineers working on the platform.
    -   **Features:** Installs the maximum number of debugging tools. `adb` is enabled by default, and the device has root access. Security is minimal.
    -   <!-- StoneOS Note: This is our standard variant for all development builds. -->

-   **`userdebug`:**
    -   **Purpose:** For testing a build in a more production-like environment.
    -   **Features:** Similar to a `user` build but with `adb` enabled by default and root access. It allows for more in-depth debugging of issues found in near-release builds.

-   **`user`:**
    -   **Purpose:** For production, release-ready builds.
    -   **Features:** Minimal debugging tools. `adb` is disabled by default, and there is no root access. This is the most secure configuration and is what is shipped to end-users.

## How to List Available Targets

To see a menu of all available lunch combinations for your specific AOSP checkout, run the `lunch` command without any arguments:

```bash
cd ~/aosp
source build/envsetup.sh
lunch
```
This will print a numbered list of common targets, allowing you to select one.
