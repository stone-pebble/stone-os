# Third-Party App Integration for StoneOS

## The Problem
StoneOS requires third-party apps (Spotify, Maps, etc.) but:
- AOSP builds don't include Google Play Services
- Can't distribute GApps due to licensing
- Need apps for the full StoneOS experience

## Solutions (In Order of Preference)

### 1. MicroG (Recommended for StoneOS)
**What**: Open-source reimplementation of Google Play Services
**Why Best**: Lightweight, privacy-focused, perfect for minimal OS

**Implementation**:
```bash
# During ROM build, include MicroG
# Add to device tree:
PRODUCT_PACKAGES += \
    GmsCore \
    GsfProxy \
    FakeStore \
    com.google.android.maps.jar

# Or flash after install:
# 1. Flash StoneOS ROM
# 2. Flash MicroG package
# 3. Enable signature spoofing
```

**Pros**:
- Minimal footprint (~20MB vs 200MB+ for GApps)
- Privacy-preserving
- Supports most apps that need Play Services
- Can use Aurora Store for app downloads

**Cons**:
- Some apps may not work perfectly
- Requires signature spoofing patch

### 2. OpenGApps Pico (For Maximum Compatibility)
**What**: Minimal Google Apps package
**When**: If users need 100% app compatibility

**Installation** (after StoneOS install):
```bash
# 1. Boot into recovery (TWRP)
# 2. Flash OpenGApps Pico for ARM64 Android 14
# 3. Wipe cache/dalvik
# 4. Reboot
```

**Package Options**:
- `pico`: Bare minimum (Play Store + Services)
- `nano`: Pico + some Google apps
- `micro`: More Google integration

### 3. Aurora Store (Standalone Solution)
**What**: Anonymous Play Store client
**Use**: Downloads APKs without Google account

```bash
# Install Aurora Store APK
adb install AuroraStore.apk

# Aurora provides anonymous tokens
# Can download any free app from Play Store
```

### 4. Direct APK Installation
**For StoneOS Testing/Development**:

```bash
# Create preinstalled apps directory
mkdir -p vendor/stoneos/apps/

# Add APKs to build
PRODUCT_PACKAGES += \
    Spotify \
    GoogleMaps \
    WhatsApp

# Or install manually
adb install spotify.apk
adb install maps.apk
```

## Recommended Approach for StoneOS

### Phase 1: Development/Testing
1. Use Aurora Store for APK downloads
2. Manually install required apps
3. Test functionality without Play Services

### Phase 2: Alpha/Beta
1. Integrate MicroG into build
2. Add signature spoofing support
3. Pre-bundle essential apps (with proper licensing)

### Phase 3: Production
1. Offer choice during setup:
   - MicroG (default, privacy-focused)
   - OpenGApps (optional, full compatibility)
2. Partner with app developers for official integration

## Technical Implementation

### Adding MicroG to Build

1. **Add to manifest** (`device/stoneos/manifest.xml`):
```xml
<project name="microg/android_packages_apps_GmsCore" 
         path="packages/apps/GmsCore" 
         remote="github" 
         revision="master" />
```

2. **Enable signature spoofing** (frameworks/base):
```diff
+ <!-- MicroG support -->
+ <uses-permission android:name="android.permission.FAKE_PACKAGE_SIGNATURE"/>
```

3. **Add to device makefile**:
```makefile
# MicroG
PRODUCT_PACKAGES += \
    GmsCore \
    GsfProxy \
    FakeStore

# Signature spoofing
PRODUCT_PACKAGES += \
    framework-signature-spoofing
```

### Testing Without Google Services

For development, test core StoneOS features without any Google Services:

```bash
# 1. Build vanilla AOSP
# 2. Install StoneOS SystemUI
# 3. Test with sideloaded APKs
# 4. Use web versions where possible
```

## App Compatibility Matrix

| App | Works w/o Services | MicroG | GApps |
|-----|------------------|---------|--------|
| Spotify | Partial | ✓ | ✓ |
| Google Maps | No | ✓ | ✓ |
| WhatsApp | ✓ | ✓ | ✓ |
| Gmail | No | Partial | ✓ |
| YouTube | Web only | ✓ | ✓ |
| Uber | No | ✓ | ✓ |

## Emulator Testing

For emulator testing without Play Services:

```bash
# 1. Use AOSP system image (not Google APIs)
emulator -avd Pixel_8a_AOSP -writable-system

# 2. Install apps directly
adb install spotify.apk
adb install aurora-store.apk

# 3. For apps needing services, use MicroG
adb install microg-services.apk
```

## Legal Considerations

- **Cannot** bundle Google Apps without license
- **Can** include MicroG (open source)
- **Can** provide instructions for users to add GApps
- **Should** partner with app developers for official support

## Next Steps

1. Test current build without any services
2. Add MicroG to next build iteration
3. Create app compatibility test suite
4. Document user installation process