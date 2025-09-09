# StoneOS Build Session Findings - Sept 8, 2024

## Critical Discovery: Why Stone Classes Weren't Being Included

### The Core Problem
The AOSP build system (Soong) evaluates glob patterns like `"src/**/*.java"` at **parse time**, not build time. This means:
- Files must exist BEFORE the build system starts
- Adding files after `repo sync` doesn't work with glob patterns
- Build system caches file lists aggressively

### Failed Approaches and Why

1. **Device Overlays (DEVICE_PACKAGE_OVERLAYS)**
   - **What we tried**: Created device tree with overlay structure
   - **Why it failed**: Device overlays ONLY work for resources (XML, images), NOT Java/Kotlin code
   - **Key learning**: This is by design in AOSP - overlays are for customizing resources, not adding new code

2. **LineageOS Glob Pattern Approach**
   - **What we tried**: Dropped files in src/ expecting glob pattern to pick them up
   - **Why it failed**: Glob patterns are evaluated when build system initializes, not during compilation
   - **Evidence**: Build succeeded but Stone classes weren't in the APK

3. **Direct Android.mk Modification**
   - **What we tried**: Created Android.mk with LOCAL_SRC_FILES
   - **Why it failed**: SystemUI uses Android.bp (Soong), not Android.mk (Make)

## The Solution: Direct Android.bp Patching

### What Should Work
Explicitly patch Android.bp to add Stone files to the srcs array:
```bash
sed -i '/\"src\/\*\*\/\*.java\",/a\        \"src/com/android/systemui/stone/StonePanel.java\",\n        \"src/com/android/systemui/stone/StoneIcon.java\",' Android.bp
```

### Current Status - Sept 9, 2024
### MAJOR BREAKTHROUGH: Forked AOSP Approach
- **Problem Solved**: Stone classes weren't being included because glob patterns are evaluated at parse time
- **Solution**: Forked AOSP frameworks/base repository (like LineageOS does)
- **Fork Created**: https://github.com/stone-pebble/stoneos-frameworks
- **Stone Files Added**: Committed directly to fork at `packages/SystemUI/src/com/android/systemui/stone/`
- **Build Script Updated**: Now uses local manifest to replace Google's frameworks/base with our fork

### UPDATE - Sept 9, 2024 (Evening)
### Fork Approach Works But Needs Android.bp Modification
- **Discovery**: Files ARE in source tree from fork but NOT being compiled
- **Root Cause**: SystemUI uses multiple modules; our files need proper module integration
- **Solution Required**: Must modify Android.bp in fork to either:
  1. Create separate SystemUI-stone module (recommended)
  2. Explicitly list Stone files in srcs array
  3. Use a filegroup for Stone files
- **Key Learning**: Fork approach is correct but requires proper AOSP build system integration

## Build Performance Findings

### Optimal Configuration
- **Instance**: n2-standard-32 (32 vCPUs)
- **AOSP Download**: MUST use `-j4` (higher causes HTTP 429 rate limiting)
- **Build**: Can use `-j16` or `-j32` successfully
- **Total Time**: ~35-40 minutes
- **Cost**: ~$0.15-0.25 per build with SPOT instances

### Build Phases
1. **AOSP Download**: ~15-20 minutes (limited by Google's servers)
2. **SystemUI Build**: ~15-20 minutes with 32 cores
3. **Verification**: Can take several minutes for dexdump on 42MB APK

## Key Learnings

### What Doesn't Work
- Device overlays for Java code - only for resources
- Dropping files and hoping glob picks them up
- Android.mk for SystemUI (uses Android.bp)
- Custom lunch targets without full device tree

### What We Know Works
- GCP SPOT instances for cost-effective builds
- AOSP branch `android-14.0.0_r61` with `aosp_x86_64-ap2a-eng`
- Build completes successfully, produces 42MB SystemUI.apk
- Clean build cache before building: `rm -rf out/soong/.intermediates/frameworks/base/packages/SystemUI/`

### What Needs Investigation
- Why sed patching of Android.bp isn't working as expected
- Exact format of Android.bp srcs array in AOSP
- Alternative methods to explicitly add files to SystemUI-core

## Next Steps

1. **Download current build** - APK is ready even if verification is stuck
2. **Manually check for Stone classes** using dexdump locally
3. **Debug sed pattern** - might need to adjust for actual Android.bp format
4. **Consider Python script** instead of sed for more robust Android.bp patching

## Commands for Testing

```bash
# Extract and check APK locally
unzip StoneOS_SystemUI.apk
dexdump classes*.dex | grep -i stone

# Alternative: check with aapt
aapt dump badging StoneOS_SystemUI.apk | grep stone

# Check if classes were compiled (on build instance)
find out/ -name "*Stone*.class"
```

## Important Notes

- The `--quick` flag just skips emulator testing, doesn't affect the build
- Build logs show the build completed successfully in 17:40
- APK size is consistent at ~42MB
- Verification step (dexdump) can hang on large APKs