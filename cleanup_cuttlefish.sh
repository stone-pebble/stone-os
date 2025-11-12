#!/bin/bash
# Comprehensive Cuttlefish Cleanup Script
# Generated: 2025-10-31
# This script removes ALL Cuttlefish-related artifacts from the system

set -e  # Exit on error

echo "======================================"
echo "CUTTLEFISH COMPREHENSIVE CLEANUP"
echo "======================================"
echo ""
echo "This will remove ALL Cuttlefish artifacts from the system."
echo "Estimated total space to reclaim: ~30GB"
echo ""
read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Aborted."
    exit 1
fi

echo ""
echo "===== 1. STOPPING ANY RUNNING PROCESSES ====="
# Kill any running Cuttlefish processes
pkill -f cuttlefish || true
pkill -f cvd || true
echo "✓ Killed any running Cuttlefish/CVD processes"

echo ""
echo "===== 2. REMOVING HOME DIRECTORY FILES ====="

# Remove downloaded host packages
echo "Removing cvd-host_package archives..."
rm -f /home/samuellarson/cvd-host_package.tar.gz*
echo "✓ Removed cvd-host_package.tar.gz files (20K)"

# Remove symlinks
echo "Removing Cuttlefish symlinks..."
rm -f /home/samuellarson/cuttlefish_assembly
rm -f /home/samuellarson/cuttlefish_runtime
rm -f /home/samuellarson/cuttlefish_runtime.1
echo "✓ Removed Cuttlefish symlinks"

# Remove .cuttlefish directory
echo "Removing .cuttlefish config directory..."
rm -rf /home/samuellarson/.cuttlefish
echo "✓ Removed .cuttlefish directory (4K)"

# Remove stoneos_cf directory
echo "Removing stoneos_cf directory..."
rm -rf /home/samuellarson/stoneos_cf
echo "✓ Removed stoneos_cf directory (124K)"

echo ""
echo "===== 3. REMOVING BAZEL CACHE (LARGEST CLEANUP) ====="
echo "Removing Cuttlefish Bazel cache..."
rm -rf /home/samuellarson/.cache/bazel/_bazel_samuellarson/53423c03277c14cca6a6b750e4d4a0d7/execroot/_main/bazel-out/k8-fastbuild/bin/cuttlefish
echo "✓ Removed Bazel Cuttlefish build artifacts (~3GB)"

echo ""
echo "===== 4. REMOVING TEMP DIRECTORIES ====="
echo "Removing /tmp Cuttlefish files..."
rm -rf /tmp/cf_avd_1001
rm -rf /tmp/cvd
rm -f /tmp/launch_cvd.log
rm -f /tmp/launch_cvd_verbose.log
rm -f /tmp/launch_cvd_help.txt
rm -f /tmp/assemble_cvd_strace.log
rm -f /tmp/fetch_cvd.log
rm -f /tmp/fetch_cvd_attempt.log
rm -f /tmp/fetch_cvd_final.log
rm -f /tmp/fetch_cvd_ticket47.log
echo "✓ Removed /tmp Cuttlefish files (136K)"

echo ""
echo "===== 5. REMOVING SYSTEM DIRECTORIES (REQUIRES SUDO) ====="

# Remove /etc directories
echo "Removing /etc/cuttlefish-common..."
sudo rm -rf /etc/cuttlefish-common
echo "✓ Removed /etc/cuttlefish-common (20K)"

echo "Removing /etc/cuttlefish-orchestration..."
sudo rm -rf /etc/cuttlefish-orchestration
echo "✓ Removed /etc/cuttlefish-orchestration (20K)"

# Remove nginx config if exists
if [ -L /etc/nginx/sites-enabled/cuttlefish-orchestration.conf ]; then
    echo "Removing nginx Cuttlefish config..."
    sudo rm -f /etc/nginx/sites-enabled/cuttlefish-orchestration.conf
    echo "✓ Removed nginx config"
fi

# Remove APT GPG key
echo "Removing APT GPG key..."
sudo rm -f /etc/apt/trusted.gpg.d/cuttlefish.gpg
echo "✓ Removed /etc/apt/trusted.gpg.d/cuttlefish.gpg"

echo ""
echo "===== 6. REMOVING /var DIRECTORIES ====="

echo "Removing /var/lib/cuttlefish-common..."
sudo rm -rf /var/lib/cuttlefish-common
echo "✓ Removed /var/lib/cuttlefish-common (8K)"

echo "Removing crash files..."
sudo rm -f /var/crash/_usr_lib_cuttlefish-common_bin_assemble_cvd.1001.crash
sudo rm -f /var/crash/_usr_lib_cuttlefish-common_bin_cvd_internal_start.1001.crash
echo "✓ Removed crash files (248K)"

echo "Removing APT cache..."
sudo rm -f /var/lib/apt/lists/us-apt.pkg.dev_projects_android-cuttlefish-artifacts_dists_android-cuttlefish_main_binary-amd64_Packages
sudo rm -f /var/lib/apt/lists/us-apt.pkg.dev_projects_android-cuttlefish-artifacts_dists_android-cuttlefish_InRelease
echo "✓ Removed APT cache files"

echo ""
echo "===== 7. REMOVING /run RUNTIME FILES ====="
echo "Removing /run/cuttlefish..."
sudo rm -rf /run/cuttlefish
sudo rm -f /run/cuttlefish-dnsmasq-cvd-wbr.pid
sudo rm -f /run/cuttlefish-dnsmasq-cvd-wbr.leases
sudo rm -f /run/cuttlefish-dnsmasq-cvd-ebr.pid
sudo rm -f /run/cuttlefish-dnsmasq-cvd-ebr.leases
echo "✓ Removed /run Cuttlefish runtime files"

echo ""
echo "===== 8. PURGING DEBIAN PACKAGES ====="
echo "Purging cuttlefish-frontend-build-deps..."
sudo apt-get purge -y cuttlefish-frontend-build-deps 2>/dev/null || true
sudo apt-get autoremove -y 2>/dev/null || true
echo "✓ Purged Debian packages"

echo ""
echo "===== 9. REMOVING SYSTEM USERS AND GROUPS ====="
# Note: Keeping system users and groups for safety
# They don't consume meaningful space and removal could cause issues
echo "NOTE: System users (httpcvd, _cutf-operator) and group (cvdnetwork) are left intact."
echo "      User 'samuellarson' will remain in cvdnetwork group."
echo "      These consume negligible space and their removal could cause system issues."

# To remove group membership (optional, commented out):
# sudo deluser samuellarson cvdnetwork

echo ""
echo "======================================"
echo "CLEANUP COMPLETE!"
echo "======================================"
echo ""
echo "Summary of removed items:"
echo "  ✓ Downloaded archives: cvd-host_package.tar.gz* (20K)"
echo "  ✓ Home directory: symlinks, .cuttlefish, stoneos_cf (128K)"
echo "  ✓ Bazel cache: Cuttlefish build artifacts (~3GB)"
echo "  ✓ Temp files: /tmp/*cvd*, logs (136K)"
echo "  ✓ System config: /etc/cuttlefish-* (40K)"
echo "  ✓ System data: /var/lib/cuttlefish-common (8K)"
echo "  ✓ Crash reports: /var/crash/*cuttlefish* (248K)"
echo "  ✓ Runtime files: /run/cuttlefish* (minimal)"
echo "  ✓ Debian packages: cuttlefish-frontend-build-deps"
echo ""
echo "TOTAL RECLAIMED: Approximately 3GB (primarily from Bazel cache)"
echo ""
echo "Remaining items (intentionally kept):"
echo "  - System users: httpcvd, _cutf-operator"
echo "  - System group: cvdnetwork"
echo "  - User group membership: samuellarson in cvdnetwork"
echo ""
echo "System is now clean and ready for AOSP-based Cuttlefish build."
