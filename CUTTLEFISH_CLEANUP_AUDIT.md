# Cuttlefish Cleanup Audit Report

**Date**: 2025-10-31
**System**: Ubuntu 24.04 on GCP n2-standard-32
**Working Directory**: /home/samuellarson/stone-os
**AOSP Directory**: /home/samuellarson/aosp

---

## Executive Summary

This audit identified **ALL Cuttlefish-related artifacts** across the system from multiple failed installation attempts. The system contains approximately **3GB** of Cuttlefish files, with the majority (3GB) being Bazel build artifacts from a source compilation attempt.

**Total items found**: 100+ files/directories across 10+ system locations
**Estimated space to reclaim**: ~3GB
**Cleanup script location**: `/home/samuellarson/stone-os/cleanup_cuttlefish.sh`

---

## Complete Inventory by Category

### 1. Home Directory Files (~3.15GB total)

#### Downloaded Archives (20K)
```
/home/samuellarson/cvd-host_package.tar.gz         (8K)
/home/samuellarson/cvd-host_package.tar.gz.1       (4K)
/home/samuellarson/cvd-host_package.tar.gz.2       (4K)
/home/samuellarson/cvd-host_package.tar.gz.3       (4K)
```
**Note**: Multiple download attempts, all appear corrupted or incomplete (should be ~700MB)

#### Symlinks (0 bytes, but broken links)
```
/home/samuellarson/cuttlefish_assembly -> /home/samuellarson/cuttlefish/assembly [BROKEN]
/home/samuellarson/cuttlefish_runtime -> /home/samuellarson/cuttlefish/instances/cvd-1 [BROKEN]
/home/samuellarson/cuttlefish_runtime.1 -> /home/samuellarson/cuttlefish/instances/cvd-1 [BROKEN]
```
**Note**: Target directory `/home/samuellarson/cuttlefish` does not exist

#### Configuration Directory (4K)
```
/home/samuellarson/.cuttlefish/                    (empty directory)
```

#### Test/Build Directory (124K)
```
/home/samuellarson/stoneos_cf/
├── etc/
│   └── cvd_config/
│       └── cvd_config_phone.json
```

#### Bazel Build Cache (~3GB) **← LARGEST CLEANUP**
```
/home/samuellarson/.cache/bazel/_bazel_samuellarson/53423c03277c14cca6a6b750e4d4a0d7/
└── execroot/_main/bazel-out/k8-fastbuild/bin/cuttlefish/
    ├── package/cuttlefish-common/bin/
    │   ├── cvd
    │   ├── cvd_internal_start
    │   ├── cvd_internal_stop
    │   ├── cvd_internal_status
    │   ├── cvd_internal_env
    │   ├── cvd_internal_display
    │   ├── cvd_internal_host_bugreport
    │   ├── run_cvd
    │   ├── assemble_cvd
    │   ├── restart_cvd
    │   ├── powerbtn_cvd
    │   ├── powerwash_cvd
    │   ├── record_cvd
    │   ├── snapshot_util_cvd
    │   ├── cvdalloc
    │   ├── cvd_send_sms
    │   ├── cvd_send_id_disclosure
    │   ├── cvd_update_location
    │   ├── cvd_update_security_algorithm
    │   ├── cvd_import_locations
    │   └── [various .runfiles, .repo_mapping files]
    ├── package/cuttlefish-common/etc/
    │   └── cvd_custom_action_config
    ├── host/libs/config/_objs/cuttlefish_config/
    ├── host/libs/command_util/_objs/run_cvd_proto/
    ├── host/libs/wayland/_objs/cuttlefish_wayland_server/
    ├── host/frontend/webrtc/_objs/
    ├── host/commands/snapshot_util_cvd/
    └── [many more build artifacts]
```
**Note**: This is from a Bazel-based source build attempt (9+ directories)

---

### 2. Temporary Files (/tmp) (136K total)

#### Runtime Directories
```
/tmp/cf_avd_1001/cvd-1/                           (16K)
/tmp/cvd/1001/1761852357345463/home/              (48K)
└── .cuttlefish_config.json
```

#### Log Files
```
/tmp/launch_cvd.log                               (59 bytes)
/tmp/launch_cvd_verbose.log                       (976 bytes)
/tmp/launch_cvd_help.txt                          (0 bytes)
/tmp/assemble_cvd_strace.log                      (53K)
/tmp/fetch_cvd.log                                (4.1K)
/tmp/fetch_cvd_attempt.log                        (4.1K)
/tmp/fetch_cvd_final.log                          (641 bytes)
/tmp/fetch_cvd_ticket47.log                       (48 bytes)
```

---

### 3. System Configuration (/etc) (40K total)

#### Cuttlefish Common Configuration (20K)
```
/etc/cuttlefish-common/
└── operator/
    └── cert/
        ├── cert.pem                              (1789 bytes, owner: _cutf-operator)
        └── key.pem                               (3268 bytes, owner: _cutf-operator)
```

#### Cuttlefish Orchestration Configuration (20K)
```
/etc/cuttlefish-orchestration/
└── ssl/
    └── cert/
        ├── cert.pem                              (1789 bytes, owner: root)
        └── key.pem                               (3272 bytes, owner: root)
```

#### APT Configuration
```
/etc/apt/trusted.gpg.d/cuttlefish.gpg
```
**Note**: No sources.list entry found (likely removed or never created)

#### Nginx Configuration
```
/etc/nginx/sites-enabled/cuttlefish-orchestration.conf  [SYMLINK BROKEN]
```

---

### 4. System Data (/var) (256K total)

#### Library Data (8K)
```
/var/lib/cuttlefish-common/
└── userartifacts/                                (empty directory, owner: httpcvd)
```

#### Crash Reports (248K)
```
/var/crash/_usr_lib_cuttlefish-common_bin_assemble_cvd.1001.crash     (124K)
/var/crash/_usr_lib_cuttlefish-common_bin_cvd_internal_start.1001.crash (124K)
```
**Note**: These indicate failed launch attempts on Oct 24 and Oct 30

#### APT Cache
```
/var/lib/apt/lists/us-apt.pkg.dev_projects_android-cuttlefish-artifacts_dists_android-cuttlefish_main_binary-amd64_Packages
/var/lib/apt/lists/us-apt.pkg.dev_projects_android-cuttlefish-artifacts_dists_android-cuttlefish_InRelease
```

#### DPKG Info
```
/var/lib/dpkg/info/cuttlefish-frontend-build-deps.list
/var/lib/dpkg/info/cuttlefish-frontend-build-deps.md5sums
```

---

### 5. Runtime Files (/run) (Minimal, will be recreated on reboot)

```
/run/cuttlefish/operator_control                  (socket, owner: _cutf-operator)
/run/cuttlefish-dnsmasq-cvd-wbr.pid
/run/cuttlefish-dnsmasq-cvd-wbr.leases
/run/cuttlefish-dnsmasq-cvd-ebr.pid
/run/cuttlefish-dnsmasq-cvd-ebr.leases
```

---

### 6. Installed Debian Packages

```
cuttlefish-frontend-build-deps                    v1.24.0 (installed)
```

**Package details**:
- Status: [installed,local]
- Documentation only: /usr/share/doc/cuttlefish-frontend-build-deps/
  - README.Debian
  - changelog.gz
  - copyright
- No actual binaries or libraries (build-dependencies meta-package)

---

### 7. System Users & Groups (KEPT INTENTIONALLY)

#### Users
```
httpcvd:x:116:124::/var/empty:/usr/sbin/nologin
_cutf-operator:x:121:1004::/var/empty:/usr/sbin/nologin
```

#### Groups
```
cvdnetwork:x:1004:samuellarson,httpcvd
```

**Note**: User `samuellarson` is a member of `cvdnetwork` group
**Decision**: These are left intact as they consume negligible space and removal could cause system issues

---

### 8. Network Interfaces

**Status**: No active Cuttlefish network interfaces found
- No `cvd-wbr` or `cvd-ebr` bridges
- No `tap*` devices associated with Cuttlefish
- dnsmasq PID files exist but processes not running

---

### 9. Running Processes

**Status**: No active Cuttlefish processes
- No `cuttlefish` processes
- No `cvd` processes
- No `run_cvd`, `assemble_cvd`, or other CVD tools running

---

### 10. AOSP Source Tree

**Status**: Clean - No Cuttlefish artifacts found
```
/home/samuellarson/aosp/
```
**Finding**: No Cuttlefish-related files or directories in AOSP source tree
**Note**: This is expected and correct - AOSP will build its own Cuttlefish tools

---

## Cleanup Strategy

### Automated Removal (via script)

The cleanup script `/home/samuellarson/stone-os/cleanup_cuttlefish.sh` will:

1. ✓ Kill any running Cuttlefish processes
2. ✓ Remove all home directory files and symlinks (~3.15GB)
3. ✓ Remove Bazel cache (largest single cleanup at 3GB)
4. ✓ Remove all temp files and logs (136K)
5. ✓ Remove system configuration directories (40K)
6. ✓ Remove /var data and crash reports (256K)
7. ✓ Remove /run runtime files
8. ✓ Purge Debian package `cuttlefish-frontend-build-deps`
9. ✓ Clean APT cache and trusted keys

### Items NOT Removed (intentional)

- System users: `httpcvd`, `_cutf-operator`
- System group: `cvdnetwork`
- User group membership: `samuellarson` in `cvdnetwork`

**Rationale**: These system accounts consume negligible space (<1K) and their removal:
- Could cause permission errors if any residual files remain
- Could interfere with future AOSP Cuttlefish build
- Are harmless to leave in place

---

## Space Reclamation Summary

| Category | Size | Details |
|----------|------|---------|
| **Bazel Cache** | **3.0GB** | Primary cleanup target |
| Home archives | 20K | 4 corrupted downloads |
| Home directories | 128K | stoneos_cf, .cuttlefish |
| Temp files | 136K | Logs and runtime dirs |
| System config | 40K | /etc certs and configs |
| System data | 256K | /var lib and crash reports |
| Debian packages | ~100K | Build-deps meta-package |
| **TOTAL** | **~3.0GB** | Majority from Bazel |

---

## Verification After Cleanup

To verify complete cleanup, run:

```bash
# Check for any remaining Cuttlefish files
find /home/samuellarson -name "*cuttlefish*" -o -name "*cvd*" 2>/dev/null
find /tmp -name "*cuttlefish*" -o -name "*cvd*" 2>/dev/null
find /etc -name "*cuttlefish*" -o -name "*cvd*" 2>/dev/null
find /var -name "*cuttlefish*" -o -name "*cvd*" 2>/dev/null

# Check for remaining packages
dpkg -l | grep -i cuttlefish

# Check for remaining processes
ps aux | grep -E "cuttlefish|cvd" | grep -v grep
```

Expected result: No output (clean system)

---

## Timeline of Failed Attempts

Based on file timestamps and logs:

1. **Oct 23 23:08**: First `cvd-host_package.tar.gz` download (8K, corrupted)
2. **Oct 24 00:27**: Orchestration setup, system users created, assemble_cvd crash
3. **Oct 30 18:43**: Second attempt, more orchestration config
4. **Oct 30 19:35**: Launch attempt failed (launch_cvd.log)
5. **Oct 30 20:21**: assemble_cvd strace debugging
6. **Oct 30 21:45**: Third download attempt (cvd-host_package.tar.gz.1)
7. **Oct 30 23:43**: Verbose launch attempt
8. **Oct 31 00:24**: Fourth download attempt (cvd-host_package.tar.gz.2)
9. **Oct 31 00:35**: fetch_cvd attempts (multiple logs)
10. **Oct 31 00:55**: Bazel source build attempt (created 3GB of artifacts)
11. **Oct 31 01:09**: Fifth download attempt (cvd-host_package.tar.gz.3)

**Conclusion**: 5+ download attempts, 2+ crash failures, 1 source build attempt, multiple launch failures over 8 days.

---

## Recommendations

### Before Building AOSP Cuttlefish

1. **Run the cleanup script**: `bash /home/samuellarson/stone-os/cleanup_cuttlefish.sh`
2. **Verify clean system**: Use verification commands above
3. **Check disk space**: Ensure at least 500GB free for AOSP build + Cuttlefish artifacts

### For Clean AOSP Build

The AOSP source tree at `/home/samuellarson/aosp` is already clean. After cleanup:

1. AOSP will build its own Cuttlefish tools from source
2. Build outputs will be in `~/aosp/out/host/linux-x86/bin/`
3. No pre-installed Cuttlefish packages should interfere
4. All system users/groups will remain functional

### Post-Cleanup State

After running the cleanup script:
- System will be in a clean slate for AOSP Cuttlefish build
- No package manager conflicts
- No leftover binaries or libraries
- System users preserved for proper permissions
- Ready for `m` build in AOSP tree

---

## Script Execution

To perform the cleanup:

```bash
cd /home/samuellarson/stone-os
bash cleanup_cuttlefish.sh
```

The script will:
- Ask for confirmation before proceeding
- Show progress for each category
- Require sudo password for system directories
- Display final summary with space reclaimed

**Estimated execution time**: < 1 minute (primarily limited by 3GB Bazel cache deletion)

---

**END OF AUDIT REPORT**
