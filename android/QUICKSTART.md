# Stone Launcher - Quick Start Guide

## TICKET_002: Native Kotlin Launcher UI

---

## TL;DR - Get Running in 3 Steps

```bash
# 1. Open in Android Studio
open -a "Android Studio" /Users/samuellarson/Pebble/Github/stone-os/android

# 2. Wait for Gradle sync (happens automatically)

# 3. Click Run ▶ button (or press Shift + F10)
```

**That's it!** The app will build and launch in the emulator.

---

## What You'll See

### On Launch
- Black screen
- 12 white app names in serif font
- 3x4 grid layout
- Full screen (no status bar)

### The Grid
```
tick      pebbles    set
listen    ask        look
plan      think      reflect
connect   go         fund
```

---

## Testing Interactions

### Tap any app
**Result**: Toast message "Opening [app name]"

### Swipe left (fast)
**Result**: Toast "Stone Chat (placeholder)"

### Swipe right (fast)
**Result**: Toast "Camera (placeholder)"

### Swipe down (fast)
**Result**: Toast "Unlock Screen (placeholder)"

---

## Set as Default Launcher

1. Press Home button in emulator
2. Select "Stone Launcher"
3. Choose "Always"

**Or via command:**
```bash
adb shell cmd package set-home-activity com.stonelauncher/.MainActivity
```

---

## Debugging

### Watch logs
```bash
adb logcat -s MainActivity:D
```

### Clear app data
```bash
adb shell pm clear com.stonelauncher
```

### Restart app
```bash
adb shell am start -n com.stonelauncher/.MainActivity
```

### Take screenshot
```bash
adb shell screencap -p /sdcard/stone.png
adb pull /sdcard/stone.png
```

---

## Files You'll Care About

### Main Implementation
- `app/src/main/java/com/stonelauncher/MainActivity.kt` - Entry point, gesture handling
- `app/src/main/java/com/stonelauncher/ui/StoneAppsAdapter.kt` - Grid adapter
- `app/src/main/java/com/stonelauncher/models/StoneApp.kt` - App data model

### Layouts
- `app/src/main/res/layout/activity_main.xml` - Main screen
- `app/src/main/res/layout/item_app.xml` - Grid item
- `app/src/main/res/values/styles.xml` - Black theme

### Configuration
- `app/src/main/AndroidManifest.xml` - Launcher setup
- `app/build.gradle` - Dependencies

---

## Common Issues

### "Gradle sync failed"
**Solution**: Make sure Android SDK is installed. Android Studio will prompt you.

### "No device found"
**Solution**: Create an emulator in Android Studio → Device Manager

### Gestures don't work
**Solution**: Swipe faster. Threshold is 100px with velocity > 100.

### Can't set as default launcher
**Solution**: Check AndroidManifest.xml has `category.HOME` (it does).

---

## What's Next

After TICKET_002 is verified:

1. **TICKET_021**: Chat interface UI
2. **TICKET_022**: LiveKit integration
3. **TICKET_023**: Agent server setup

---

## Need More Info?

- **Full implementation details**: `TICKET_002_IMPLEMENTATION.md`
- **Status and testing**: `/tickets/outstanding/TICKET_002_STATUS.md`
- **Original ticket**: `/tickets/outstanding/TICKET_002_Native_Kotlin_Launcher_UI.md`

---

## Build from Command Line (Optional)

If you have Android SDK configured:

```bash
cd /Users/samuellarson/Pebble/Github/stone-os/android

# Build
./gradlew assembleDebug

# Install
./gradlew installDebug

# Or use convenience script
./build-and-test.sh
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

---

**Happy testing!** 🚀
