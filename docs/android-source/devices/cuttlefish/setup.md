# Cuttlefish: Setup and Installation

**Source:** [Official Cuttlefish Documentation](https://source.android.com/docs/setup/create/cuttlefish) & [Android CI Server](https://ci.android.com/)
**Scrape Date:** 2025-10-23

---

## Overview

Cuttlefish (CVD) is the official virtual device for AOSP development and testing. Unlike the standard Android SDK emulator, it is designed to run custom-built Android images with high fidelity in a cloud or virtualized environment.

<!-- StoneOS Note: This is our primary and mandatory tool for testing all OS-level modifications. The standard emulator is not a viable option for our workflow. -->

## The Correct Architectural Approach

The key to a stable Cuttlefish setup is to separate the **host environment** from the **target OS images**.

1.  **Host Environment:** The host machine (our GCP VM) must be prepared using official, pre-built Debian packages from Google's CI server. These packages install the Cuttlefish binaries and configure all necessary system dependencies (kernel modules, user groups, network settings).
2.  **Target OS Images:** We use the host environment to run our own custom-built OS images (`system.img`, etc.) that are produced by our AOSP build.

Attempting to use Cuttlefish tools built directly from the AOSP source (`m debs-package`) is **not** the recommended approach and can lead to instability and crashes, as it does not perform the required host system configuration.

## Step-by-Step Installation Guide

### Phase 1: Host Machine Prerequisites

1.  **Enable KVM:** Cuttlefish requires KVM for hardware acceleration. On a GCP VM, this means the instance must be a compatible machine type (e.g., N1, N2 series) and have **Nested Virtualization** enabled.
    <!-- StoneOS Note: This was the objective of Ticket #26. -->

    **Verification Command:**
    ```bash
    kvm-ok
    ```
    The expected output is `INFO: /dev/kvm exists and is usable`. If this fails, Cuttlefish cannot be installed.

### Phase 2: Install Official Host Packages

1.  **Download the Installer Script:**
    This script, provided by Google, automatically fetches the latest stable Debian packages for the Cuttlefish host tools.
    ```bash
    cd ~
    wget https://ci.android.com/builds/latest/branches/aosp-main/targets/aosp_cf_x86_64_phone-userdebug/latest/cuttlefish-host-resources.sh
    chmod +x cuttlefish-host-resources.sh
    ./cuttlefish-host-resources.sh
    ```

2.  **Install the Packages:**
    Use `dpkg` to install the downloaded packages.
    ```bash
    sudo dpkg -i cuttlefish-common_*_amd64.deb cuttlefish-user_*_amd64.deb
    ```
    If you encounter any dependency errors, run the following command to automatically fix them:
    ```bash
    sudo apt-get install -f
    ```

3.  **Add User to Required Groups:**
    The user account that will launch Cuttlefish must be a member of three specific groups to access the necessary hardware and network resources.
    ```bash
    sudo usermod -aG kvm,cvdnetwork,render $USER
    ```

4.  **Apply Group Changes:**
    For the new group memberships to take effect, you must log out and log back in. In our VNC environment, the correct way to do this is to restart the VNC server.
    ```bash
    # Kill the current VNC session
    vncserver -kill :1

    # Start a new one
    vncserver
    ```
    You will need to reconnect your VNC viewer after this step.

The host environment is now correctly configured to run Cuttlefish.
