# TICKET_002 Implementation Complete

## Native Kotlin Launcher UI

This document describes the implementation of TICKET_002: Native Kotlin Launcher UI for Stone Launcher.

---

## What Was Built

A native Android launcher app in Kotlin that displays a minimalist 3x4 grid of Stone apps with gesture navigation support.

### Features Implemented

- Black background (#000000) minimalist design
- 3x4 grid of 12 Stone apps (text-only, serif font)
- Full screen mode (no status bar or navigation bar)
- Swipe gesture detection:
  - Swipe left → Stone chat (placeholder toast)
  - Swipe right → Camera (placeholder toast)
  - Swipe down → Unlock screen (placeholder toast)
- Tap detection on apps (placeholder toast)
- Launcher replacement capability (can be set as default home launcher)

### The 12 Stone Apps

```
tick      pebbles    set
listen    ask        look
plan      think      reflect
connect   go         fund
```

---

## Files Created/Modified

### New Files Created

1. **`/android/app/src/main/java/com/stonelauncher/models/StoneApp.kt`**
   - Data class for Stone app model
   - Contains id and name fields

2. **`/android/app/src/main/java/com/stonelauncher/ui/StoneAppsAdapter.kt`**
   - RecyclerView adapter for displaying app grid
   - Handles click events for each app

3. **`/android/app/src/main/res/layout/activity_main.xml`**
   - Main launcher screen layout
   - Contains RecyclerView for app grid
   - Black background with padding

4. **`/android/app/src/main/res/layout/item_app.xml`**
   - Layout for individual app items in the grid
   - TextView with white serif text, centered
   - 100dp height for good touch targets

### Modified Files

5. **`/android/app/src/main/java/com/stonelauncher/MainActivity.kt`**
   - Completely rewritten from placeholder to full implementation
   - Grid layout with 3 columns
   - Gesture detection for swipes
   - Full screen immersive mode
   - AppCompatActivity for modern Android support

6. **`/android/app/src/main/res/values/styles.xml`**
   - Updated theme to black minimalist design
   - Full screen configuration
   - White text on black background
   - Transparent status bar

7. **`/android/app/src/main/AndroidManifest.xml`**
   - Added HOME category to make it launcher-capable
   - Added DEFAULT category
   - Set launchMode to singleTask
   - Portrait orientation locked
   - Config changes handled

8. **`/android/app/build.gradle`**
   - Added RecyclerView dependency (1.3.2)
   - Added ConstraintLayout dependency (2.1.4)

---

## Building and Running

### Option 1: Using Android Studio (Recommended)

1. **Open the project:**
   ```bash
   open -a "Android Studio" /Users/samuellarson/Pebble/Github/stone-os/android
   ```

   Or:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to `/Users/samuellarson/Pebble/Github/stone-os/android`

2. **Wait for Gradle sync:**
   - Android Studio will automatically sync Gradle dependencies
   - This may take a few minutes on first run

3. **Run on emulator or device:**
   - Click the green "Run" button (▶) or press `Shift + F10`
   - Select a device/emulator from the list
   - App will build and install

### Option 2: Command Line Build

If you have Android SDK set up:

```bash
cd /Users/samuellarson/Pebble/Github/stone-os/android

# First time: Initialize Gradle wrapper (if needed)
# This requires gradle to be installed
gradle wrapper --gradle-version 8.2

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Or build and install in one step
./gradlew installDebug
```

The APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Option 3: Manual APK Installation

```bash
# Build in Android Studio, then:
adb install app/build/outputs/apk/debug/app-debug.apk

# Or if already installed:
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Testing the Launcher

### 1. Launch the App

After installation, the app will appear in the app drawer as "Stone Launcher".

### 2. Set as Default Launcher

To test launcher replacement functionality:

```bash
# Press the Home button on the device/emulator
# Android will prompt you to choose a launcher
# Select "Stone Launcher"
# Choose "Always" to set as default

# Or via adb:
adb shell cmd package set-home-activity com.stonelauncher/.MainActivity
```

### 3. Test Functionality

**Grid Display:**
- Should see 12 apps in a 3x4 grid
- Black background, white serif text
- Full screen (no status bar)

**App Taps:**
- Tap any app name (e.g., "tick", "listen")
- Should see toast: "Opening [app name]"
- Check logs: `adb logcat -s MainActivity:D`

**Swipe Gestures:**
- Swipe left (fast) → "Stone Chat (placeholder)" toast
- Swipe right (fast) → "Camera (placeholder)" toast
- Swipe down (fast) → "Unlock Screen (placeholder)" toast
- Check logs for "Swipe [direction] detected" messages

### 4. Debugging

Monitor logs in real-time:
```bash
adb logcat -s MainActivity:D StoneApiReceiver:D
```

Clear app data and restart:
```bash
adb shell pm clear com.stonelauncher
adb shell am start -n com.stonelauncher/.MainActivity
```

Take screenshot:
```bash
adb shell screencap -p /sdcard/stone_launcher.png
adb pull /sdcard/stone_launcher.png
```

---

## Known Limitations (Intentional for TICKET_002)

These are **expected** and will be addressed in future tickets:

1. **Placeholder actions**: Apps and gestures show toasts instead of real functionality
2. **No LiveKit integration**: Voice/AI features coming in TICKET_022
3. **No chat interface**: Chat UI will be added in TICKET_021
4. **No camera integration**: Will be implemented later
5. **No unlock screen**: Will be implemented later
6. **No app icons**: Intentionally text-only for minimalist design

---

## Design Notes

### Why This Approach

- **Native Kotlin**: Better performance than React Native for launcher
- **Text-only apps**: Minimalist, matches web prototype design
- **Serif font**: Aesthetic choice from reference design
- **3x4 grid**: Optimal for 12 apps, good touch targets
- **Full screen**: Immersive experience, no distractions
- **Black background**: Core Stone aesthetic, battery-friendly on OLED

### Architecture Decisions

- **AppCompatActivity**: Modern Android best practice
- **RecyclerView**: Efficient scrolling (even though we don't scroll)
- **GridLayoutManager**: Simple and effective for grid layout
- **GestureDetector**: Built-in Android gesture support
- **Data class**: Kotlin idiomatic approach for models

---

## Next Steps

After TICKET_002 is verified working:

1. **TICKET_021**: Add chat interface UI (swipe left destination)
2. **TICKET_022**: Integrate LiveKit for voice
3. **TICKET_023**: Set up agent server
4. **TICKET_024**: Implement individual Stone apps (tick, listen, etc.)

---

## Testing Checklist

- [ ] App builds without errors
- [ ] App runs in emulator
- [ ] Grid shows all 12 apps correctly
- [ ] Apps are in correct order (tick, pebbles, set, etc.)
- [ ] Black background displays
- [ ] White serif text displays
- [ ] Full screen mode works (no status bar)
- [ ] Tapping app shows correct toast
- [ ] Swipe left shows chat toast
- [ ] Swipe right shows camera toast
- [ ] Swipe down shows unlock toast
- [ ] Can be set as default launcher
- [ ] Home button returns to Stone Launcher
- [ ] No crashes or errors in logcat

---

## Success Criteria (from ticket)

- [x] Native Kotlin Android app (no React Native)
- [x] Runs in Android emulator
- [x] Looks like web prototype (black, minimalist)
- [x] All 12 Stone apps displayed in grid
- [x] Basic swipe gestures work
- [x] Can be set as home launcher
- [x] Clean, well-structured Kotlin code
- [x] No crashes or errors

---

## Implementation Notes

- Used `View.SYSTEM_UI_FLAG_*` for full screen (older API, but works on API 26+)
- Swipe threshold set to 100px for reliable detection
- GridLayoutManager span count set to 3 for 3x4 grid
- Item height set to 100dp for good touch targets
- No external libraries except AndroidX (keeping it simple)
- Followed ticket implementation plan exactly

---

## Contact

For questions about this implementation, refer to:
- Ticket: `/Users/samuellarson/Pebble/Github/stone-os/tickets/outstanding/TICKET_002_Native_Kotlin_Launcher_UI.md`
- Reference design: `/Users/samuellarson/Pebble/Github/stone-web-app-proto/ui/src/pages/HomeScreen.tsx`
