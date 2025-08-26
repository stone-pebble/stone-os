# Instructions for Next Agent Working on StoneOS

## CRITICAL: What We Learned

### ✅ CORRECT Build Approach
```bash
lunch aosp_x86_64-ap3a-eng      # Android 15
lunch aosp_x86_64-ap2a-eng      # Android 14
```

### ❌ WRONG (Will Fail)
```bash
lunch aosp_x86_64-trunk_staging-eng  # Google internal only!
lunch sdk_phone_x86_64-userdebug     # Deprecated
```

## Current State

### Device
- Pixel 8a (akita) - Bootloader unlocked, rooted with Magisk
- Ready for custom ROM

### Code Status
- **Stone Panel**: Created at `/stone-os/SystemUI/stone/StonePanel.java`
- **Stone Icon**: Created at `/stone-os/SystemUI/stone/StoneIcon.java`
- Both files implement specs correctly (1/3 panel, swipe gestures)

### What Failed & Why
1. **ARM VM on Mac** - Can't use x86 AOSP prebuilts
2. **x86 VM on Mac** - Too slow (emulated), network issues
3. **Docker on Mac** - x86 emulation very slow on Apple Silicon
4. **GCP Build** - Used wrong lunch target (`trunk_staging`)

## Next Steps - DO THIS

### 1. Create New GCP Instance
```bash
# Use the script we already have
./gcp_aosp_instance.sh create
```

### 2. Download AOSP Source
```bash
mkdir ~/aosp && cd ~/aosp
repo init -u https://android.googlesource.com/platform/manifest \
  -b android-14.0.0_r61 --depth=1
repo sync -c -j8 --no-tags --no-clone-bundle
```

### 3. Add Stone Modifications
```bash
# Copy Stone files
mkdir -p ~/aosp/frameworks/base/packages/SystemUI/src/com/android/systemui/stone/
# Copy StonePanel.java and StoneIcon.java from this repo
```

### 4. Build with CORRECT Config
```bash
cd ~/aosp
source build/envsetup.sh
lunch aosp_x86_64-ap2a-eng  # THIS IS THE KEY FIX!
m SystemUI -j8
```

### 5. Test
SystemUI.apk will be at:
`out/target/product/generic_x86_64/system/system_ext/priv-app/SystemUI/SystemUI.apk`

## GCP Instance Management

### Create & Start
```bash
./gcp_aosp_instance.sh create  # Auto-shutdown after 8 hours
./gcp_aosp_instance.sh status  # Check if running
```

### Stop When Done
```bash
./gcp_aosp_instance.sh stop    # Stop (keeps disk)
./gcp_aosp_instance.sh delete  # Delete completely (no charges)
```

### Cost
- Running: ~$0.77/hour
- Stopped: ~$0.08/hour (disk only)
- Deleted: $0

## Testing Without Full Build

If build still fails, test UI concept first:
```bash
# Create simple Android app with Stone panel behavior
# This validates the UX before wrestling with AOSP
```

## Resources

- **AOSP Build Fix**: See `AOSP_BUILD_FIX.md`
- **GCP Script**: `gcp_aosp_instance.sh`
- **Stone Code**: `SystemUI/stone/` directory

## Time Estimate

- GCP setup: 5 minutes
- AOSP download: 30-60 minutes
- Build SystemUI: 20-30 minutes
- Total: ~2 hours

## Remember

1. **ALWAYS** use `ap2a` or `ap3a`, never `trunk_staging`
2. **ALWAYS** stop/delete GCP instance when done
3. **TEST** in emulator before device