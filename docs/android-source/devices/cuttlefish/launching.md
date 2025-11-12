# Cuttlefish: Launching and Usage

**Source:** [Official Cuttlefish Documentation](https://source.android.com/docs/setup/create/cuttlefish) & Internal Project Findings
**Scrape Date:** 2025-10-23

---

## Overview

Once the Cuttlefish host environment is correctly installed, you can use its tools to launch any compatible AOSP build. For StoneOS, this means launching the images produced by our `aosp_cf_x86_64_phone-eng` build target.

## The Correct Build Target

To build a Cuttlefish-compatible OS image, the `lunch` target must be specifically for Cuttlefish. The generic AOSP targets do not include the necessary kernel modules and device configurations.

<!-- StoneOS Note: This was the outcome of Ticket #22. The correct target is non-negotiable. -->
**Correct Target:**
```bash
source build/envsetup.sh
lunch aosp_cf_x86_64_phone-eng
m
```

This will produce all necessary images and the Cuttlefish host package in the output directory: `~/aosp/out/target/product/vsoc_x86_64/`.

## Launching StoneOS

The `launch_cvd` command is the primary tool for starting a Cuttlefish device. It is designed to be run from the directory containing the AOSP build artifacts.

**Step-by-Step Launch:**

1.  **Navigate to the Build Output Directory:**
    The `launch_cvd` command automatically discovers the images (`system.img`, `boot.img`, etc.) in the current directory.
    ```bash
    cd ~/aosp/out/target/product/vsoc_x86_64/
    ```

2.  **Launch Cuttlefish:**
    Run the launch command. It's recommended to run it in the background (`&`) so you can continue to use your terminal.
    ```bash
    launch_cvd &
    ```
    The device will begin booting in the background. You can monitor its progress by viewing the logs at `~/cuttlefish/logs/launch.log`.

## Accessing the Device

There are two primary ways to interact with a running Cuttlefish device.

### 1. Web UI (Primary Method)

Cuttlefish provides a WebRTC-based streaming view of the device's screen in a web browser. This is the primary method for graphical interaction in our VNC environment.

-   **URL:** `https://0.0.0.0:8443`
-   **Usage:** Open a web browser *inside the VNC session* and navigate to this URL. You will see the device's screen and can interact with it using your mouse and keyboard.

### 2. ADB

The Cuttlefish device automatically connects to ADB, just like a physical device. You can use all standard `adb` commands.

-   **Check Connection:**
    ```bash
    adb devices
    ```
    You should see a device listed, typically `127.0.0.1:6520`.

-   **Get a Shell:**
    ```bash
    adb shell
    ```

-   **Monitor Logs:**
    ```bash
    adb logcat
    ```
    <!-- StoneOS Note: We use this to monitor our custom log tags: `adb logcat -s StoneOS:* SystemUI:* StoneManager:*` -->

## Stopping the Device

To stop the Cuttlefish virtual device and free up resources, use the `stop_cvd` command.

```bash
stop_cvd
```
This will shut down the virtual machine and all related processes.
