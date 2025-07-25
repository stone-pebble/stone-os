# StoneOS AOSP Patch System

## Overview

StoneOS uses a layered patch system inspired by the Yocto Project to maintain AOSP customizations. This approach ensures long-term maintainability, easy security updates, and clean separation of concerns.

## Why Patches Instead of Forking?

1. **Maintainability**: Easy to rebase on new AOSP versions
2. **Transparency**: Clear view of all modifications
3. **Modularity**: Patches organized by functionality
4. **Collaboration**: Easier to upstream beneficial changes
5. **Security**: Quick application of upstream security fixes

## Patch Organization Structure

```
patches/
├── 0001-device/              # Hardware-specific patches
│   ├── 0001-pixel8a-support.patch
│   └── 0002-custom-drivers.patch
├── 0002-framework/           # Core framework modifications
│   ├── 0001-remove-launcher.patch
│   ├── 0002-webview-shell.patch
│   └── 0003-mcp-service.patch
├── 0003-ui/                  # UI layer changes
│   ├── 0001-disable-systemui.patch
│   └── 0002-react-integration.patch
├── 0004-services/            # System service modifications
│   ├── 0001-activity-manager.patch
│   └── 0002-window-manager.patch
├── 0005-security/            # Security enhancements
│   ├── 0001-permission-model.patch
│   └── 0002-encryption.patch
└── 0006-performance/         # Performance optimizations
    ├── 0001-memory-tuning.patch
    └── 0002-battery-optimization.patch
```

## Key AOSP Modules to Patch

### 1. frameworks/base

The heart of Android - requires the most extensive modifications:

```patch
# Example: Disable default launcher
--- a/frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
+++ b/frameworks/base/services/core/java/com/android/server/am/ActivityManagerService.java
@@ -1234,6 +1234,11 @@ public class ActivityManagerService {
     private void startHomeActivity() {
+        // StoneOS: Replace default home with our WebView shell
+        Intent intent = new Intent();
+        intent.setComponent(new ComponentName("com.stoneos.ui", "com.stoneos.ui.MainActivity"));
+        intent.addCategory(Intent.CATEGORY_HOME);
+        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
-        startHomeActivityLocked(mCurrentUserId, "systemReady");
+        mContext.startActivity(intent);
     }
```

### 2. packages/SystemUI

Complete replacement of the status bar and navigation:

```patch
# Disable SystemUI components
--- a/packages/SystemUI/AndroidManifest.xml
+++ b/packages/SystemUI/AndroidManifest.xml
@@ -23,7 +23,8 @@
     <application
         android:name=".SystemUIApplication"
-        android:enabled="true"
+        android:enabled="false"
+        android:persistent="false"
```

### 3. system/core

Init system modifications for our services:

```patch
# Add MCP service to init
--- a/system/core/rootdir/init.rc
+++ b/system/core/rootdir/init.rc
@@ -823,6 +823,13 @@ on boot
     start surfaceflinger
     start bootanim
 
+# StoneOS Master Control Program
+service mcp /system/bin/mcp_service
+    class core
+    user system
+    group system
+    capabilities SYS_NICE
+
```

### 4. build/make

Product definition for StoneOS:

```makefile
# StoneOS product configuration
PRODUCT_NAME := stoneos
PRODUCT_DEVICE := generic_arm64
PRODUCT_BRAND := StoneOS
PRODUCT_MODEL := StoneOS Device
PRODUCT_MANUFACTURER := Pebble

# Remove default apps
PRODUCT_PACKAGES := \
    $(filter-out Launcher3 SystemUI Settings, $(PRODUCT_PACKAGES))

# Add StoneOS components
PRODUCT_PACKAGES += \
    StoneUI \
    MasterControlProgram \
    StoneAgentService \
    ReactNativeRuntime
```

## Patch Development Workflow

### 1. Setting Up Development Environment

```bash
# Clone AOSP
mkdir ~/stoneos && cd ~/stoneos
repo init -u https://android.googlesource.com/platform/manifest -b android-14.0.0_r1
repo sync -j8

# Clone StoneOS patches
git clone https://github.com/pebble/stoneos-patches patches
```

### 2. Creating a New Patch

```bash
# Make changes in AOSP tree
cd frameworks/base
# ... make your changes ...

# Generate patch
git diff > ~/stoneos-patches/0002-framework/0004-new-feature.patch

# Add patch header
cat > patch_header << EOF
From: Your Name <email@example.com>
Date: $(date)
Subject: [PATCH] Brief description of the change

Detailed explanation of what this patch does and why it's needed.

---
EOF

cat patch_header ~/stoneos-patches/0002-framework/0004-new-feature.patch > temp
mv temp ~/stoneos-patches/0002-framework/0004-new-feature.patch
```

### 3. Applying Patches

```bash
#!/bin/bash
# apply-patches.sh

PATCH_DIR="patches"
AOSP_ROOT="."

# Apply patches in order
for category in $(ls $PATCH_DIR | sort); do
    echo "Applying $category patches..."
    for patch in $(ls $PATCH_DIR/$category/*.patch | sort); do
        echo "  Applying $(basename $patch)..."
        patch -p1 < $patch || exit 1
    done
done

echo "All patches applied successfully!"
```

### 4. Validating Patches

```bash
#!/bin/bash
# validate-patches.sh

# Check if patches apply cleanly
for patch in $(find patches -name "*.patch"); do
    if ! patch --dry-run -p1 < $patch > /dev/null 2>&1; then
        echo "ERROR: $patch does not apply cleanly"
        exit 1
    fi
done

echo "All patches validated successfully!"
```

## Critical Patches for StoneOS

### 1. WebView Shell Integration (0002-framework/0002-webview-shell.patch)

Replaces the entire Android launcher with our WebView-based shell:

- Modifies ActivityManagerService to launch our shell
- Grants system-level permissions to the WebView
- Implements native bridge for JavaScript access
- Locks WebView to local content only

### 2. MCP Service Integration (0002-framework/0003-mcp-service.patch)

Adds the Master Control Program as a system service:

- Registers MCP with ServiceManager
- Implements binder interface for IPC
- Adds permission checks for app access
- Integrates with PackageManager for app queries

### 3. Remove Default UI (0003-ui/0001-disable-systemui.patch)

Completely removes the standard Android UI:

- Disables SystemUI service
- Removes status bar and navigation bar
- Prevents notification shade
- Disables recent apps screen

### 4. Security Enhancements (0005-security/0001-permission-model.patch)

Implements StoneOS-specific security model:

- New permission groups for AI agents
- Restricted app installation
- Enhanced privacy controls
- Audit logging for all MCP access

## Patch Maintenance

### Rebasing on New AOSP Versions

1. **Preparation**
   ```bash
   # Create new branch for rebase
   repo start rebase-android-15 --all
   ```

2. **Apply Patches with Conflicts**
   ```bash
   # Try applying each patch
   for patch in patches/**/*.patch; do
       if ! git apply --check $patch; then
           echo "Conflict in $patch"
           # Manual resolution needed
       fi
   done
   ```

3. **Update Patches**
   ```bash
   # After resolving conflicts
   git diff > updated_patch.patch
   ```

### Patch Review Process

1. **Code Review**: All patches must be reviewed by 2+ team members
2. **Testing**: Each patch must include test cases
3. **Documentation**: Update this guide with new patches
4. **Compatibility**: Test on multiple AOSP versions

## Best Practices

1. **Keep Patches Small**: One logical change per patch
2. **Document Thoroughly**: Include rationale in patch description
3. **Test Incrementally**: Verify each patch independently
4. **Version Control**: Track patch history in git
5. **Upstream When Possible**: Contribute improvements back to AOSP

## Troubleshooting

### Common Issues

1. **Patch Conflicts**
   - Usually due to AOSP updates
   - Check surrounding context
   - May need to recreate patch

2. **Build Failures**
   - Verify all dependencies
   - Check patch order
   - Review build logs

3. **Runtime Errors**
   - Check SELinux policies
   - Verify permissions
   - Review logcat output

### Debug Commands

```bash
# Check patch status
quilt series

# Show applied patches
quilt applied

# Revert last patch
quilt pop

# Force apply patch
patch -p1 --force < patch_file
```

## Future Improvements

1. **Automated Patch Management**
   - CI/CD for patch validation
   - Automated conflict resolution
   - Patch dependency tracking

2. **Upstream Collaboration**
   - Identify generally useful changes
   - Prepare patches for AOSP submission
   - Engage with Android community

3. **Tool Development**
   - Visual patch editor
   - Conflict resolution assistant
   - Patch impact analyzer 