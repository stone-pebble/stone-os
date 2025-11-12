# Ticket #26: Migrate GCP Instance and Launch StoneOS in Cuttlefish

**Status**: Active
**Assigned to**: Emulator Agent
**Priority**: Highest (blocks all testing)
**Created**: 2025-10-23

---

## SPECIFICATION

### Problem
The current GCP instance does not support nested virtualization (KVM), which is required to run Cuttlefish. The root cause is an incompatible machine type. We need to migrate to a supported machine type to enable KVM, then install Cuttlefish and launch our custom StoneOS build.

### Task Overview
This is a two-phase ticket:
1. **Phase 1**: Migrate GCP instance to enable KVM
2. **Phase 2**: Install Cuttlefish and launch StoneOS

---

## PHASE 1: Migrate GCP Instance to Enable KVM

### Objective
Change the machine type of the `stoneos-dev` instance to `n2-standard-32`.

### Warning
⚠️ This process will involve stopping the VM for a few minutes. All data on the persistent disk will be safe.

### Steps

1. **Stop the Instance**
   ```bash
   gcloud compute instances stop stoneos-dev --zone=us-central1-a
   ```

2. **Change the Machine Type**
   ```bash
   gcloud compute instances set-machine-type stoneos-dev \
     --zone=us-central1-a \
     --machine-type=n2-standard-32
   ```

3. **Start the Instance**
   ```bash
   gcloud compute instances start stoneos-dev --zone=us-central1-a
   ```

4. **Verify KVM**
   Once the VM is back online, SSH into the machine and run:
   ```bash
   kvm-ok
   ```

### Phase 1 Acceptance Criteria
- [ ] Instance successfully stopped
- [ ] Machine type changed to `n2-standard-32`
- [ ] Instance successfully restarted
- [ ] `kvm-ok` command outputs: `INFO: /dev/kvm exists and is usable`

**If `kvm-ok` fails, STOP and report in COMPLICATIONS section.**

---

## PHASE 2: Install Official Cuttlefish Host Environment

### Objective
Install official pre-built Cuttlefish host packages from Android CI. These packages set up the complete host environment (udev rules, cvdnetwork group, kernel modules, config files) required to run Cuttlefish.

**CRITICAL INSIGHT from Research Ticket #28**: DO NOT use the Cuttlefish tools built from AOSP (`~/aosp/out/host/linux-x86/bin/`). Those crash because they lack proper host environment setup. The official pre-built packages are the Google-recommended approach.

### Steps

1. **Download the Cuttlefish Host Package Installer Script**
   ```bash
   cd ~
   wget https://ci.android.com/builds/latest/branches/aosp-main/targets/aosp_cf_x86_64_phone-userdebug/latest/cuttlefish-host-resources.sh
   chmod +x cuttlefish-host-resources.sh
   ```

2. **Run the Script to Download Packages**
   This downloads the latest stable `cuttlefish-common` and `cuttlefish-user` Debian packages:
   ```bash
   ./cuttlefish-host-resources.sh
   ```

3. **Install the Debian Packages**
   This installs the packages and configures the system:
   ```bash
   sudo dpkg -i cuttlefish-common_*_amd64.deb cuttlefish-user_*_amd64.deb
   ```
   *If you encounter dependency errors, run: `sudo apt-get install -f`*

4. **Add User to Required Groups**
   ```bash
   sudo usermod -aG kvm,cvdnetwork,render $USER
   ```

5. **Restart VNC Server**
   For group changes to take effect, restart the VNC session:
   ```bash
   vncserver -kill :1
   vncserver
   ```
   *After this, reconnect your VNC viewer.*

---

## PHASE 3: Launch StoneOS with Official Tools

### Objective
Use the newly installed official `launch_cvd` tool to boot our custom StoneOS images.

### Steps

1. **Navigate to the AOSP Build Output Directory**
   The `launch_cvd` command should be run from the directory containing our built OS images:
   ```bash
   cd ~/aosp/out/target/product/vsoc_x86_64
   ```

2. **Launch Cuttlefish**
   The official `launch_cvd` will automatically find and use the `system.img` and other files in this directory:
   ```bash
   launch_cvd
   ```

3. **Verify in Browser**
   Open the browser inside your VNC session and navigate to:
   ```
   https://0.0.0.0:8443
   ```
   You should see the Cuttlefish web UI with StoneOS booting.

### Phase 2 Acceptance Criteria
- [ ] Cuttlefish host packages install successfully
- [ ] User added to `kvm`, `cvdnetwork`, and `render` groups
- [ ] VNC server restarted successfully
- [ ] `launch_cvd` command starts without errors
- [ ] Cuttlefish web UI accessible at `https://0.0.0.0:8443` from browser inside VNC session
- [ ] Virtual device boots into custom StoneLauncher (3x4 grid of words)
- [ ] StoneIcon is visible on screen
- [ ] StonePanel can be activated (swipe up from StoneIcon)

### Critical Notes

- **Web UI Access**: The Cuttlefish web UI must be accessed from **within the VNC session**, not from your local machine
- **Build Location**: Cuttlefish will automatically find the build artifacts in `~/aosp/out/target/product/vsoc_x86_64/`
- **Launch Time**: First boot may take 5-10 minutes. Be patient.
- **Lunch Command**: If `lunch aosp_cf_x86_64_phone-eng` fails, check the available lunch combos with just `lunch` and report the issue

---

## IMPLEMENTATION REPORT

**Emulator Agent**: Fill this section when task is complete.

### Phase 1 Results
[Report on instance migration and KVM verification]

### Phase 2 Results
[Report on Cuttlefish installation and StoneOS launch]

### Screenshots
[If possible, include screenshots of:]
- Cuttlefish web UI
- StoneLauncher home screen
- StoneIcon visibility
- StonePanel activation

### Launch Logs
[Paste relevant output from `launch_cvd` showing successful boot]

### Verification Commands
[Show output of verification commands proving all acceptance criteria met]

---

## COMPLICATIONS & REVISIONS

**Emulator Agent**: Document any unexpected issues here. If blocked, STOP and report.

### Issues Encountered
[Describe any problems, errors, or blockers]

### Error Messages
[Paste full error messages and stack traces]

### Questions for Architect
[What do you need clarification on?]

### Recommended Changes
[If you think the spec needs revision, suggest changes here]
