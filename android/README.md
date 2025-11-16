# Stone Launcher - Android Application

This directory contains the native Android application for Stone Launcher.

## Project Structure

```
android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/stonelauncher/
│   │       │   ├── api/              # Intent API (Headless interface)
│   │       │   │   ├── StoneApiReceiver.kt
│   │       │   │   ├── IntentResult.kt
│   │       │   │   └── README.md
│   │       │   ├── controllers/      # Business logic (shared by UI & API)
│   │       │   ├── modules/          # React Native bridge modules
│   │       │   ├── ui/               # Native UI components
│   │       │   └── MainActivity.kt   # Main launcher activity
│   │       ├── res/                  # Android resources
│   │       └── AndroidManifest.xml   # App configuration
│   ├── build.gradle                  # App-level build config
│   └── proguard-rules.pro           # ProGuard rules
├── build.gradle                      # Project-level build config
├── settings.gradle                   # Project settings
└── gradle.properties                 # Gradle properties

```

## Current Status

**TICKET_001: Intent API Foundation** ✅ COMPLETE

The Intent API infrastructure is implemented and ready for use:

- ✅ BroadcastReceiver (`StoneApiReceiver`) created
- ✅ Result wrapper (`IntentResult`) implemented
- ✅ AndroidManifest.xml configured
- ✅ Test script created
- ✅ Documentation complete

### What Works

- Receiving broadcast Intents with custom actions
- Routing to handler methods based on action
- Sending result broadcasts
- Error handling for unknown actions
- Logging for debugging

### What's Next

Implement actual feature controllers:

- **TICKET_002**: WiFi Controller (reference implementation)
- **TICKET_003**: TASK App - App Launcher
- **TICKET_004**: SET App - Settings Control
- **TICKET_005**: Permission Management System

## Building the Project

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 34 (Android 14)
- JDK 17
- Gradle 8.2 (managed by wrapper)

### Build Commands

```bash
# Build debug APK
cd android
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test
```

### Using Android Studio

1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to `/Users/samuellarson/Pebble/Github/stone-os/android`
4. Wait for Gradle sync to complete
5. Run using the green play button or `Shift + F10`

## Testing the Intent API

### Manual Testing via adb

```bash
# Send test Intent
adb shell am broadcast -a com.stone.launcher.action.GET_WIFI_STATE

# Monitor logs
adb logcat -s StoneApiReceiver:*

# Run test script
../test_intent_api.sh
```

### Expected Output

```
D/StoneApiReceiver: Received intent: com.stone.launcher.action.GET_WIFI_STATE
D/StoneApiReceiver: handleGetWifiState called (placeholder - will be implemented in TICKET_002)
D/StoneApiReceiver: Sent result for com.stone.launcher.action.GET_WIFI_STATE: success=true
```

## Architecture

Stone Launcher follows the "Head & Headless" pattern:

### The "Head" - Touch UI

- React Native components (future implementation)
- Native modules bridge to Controllers
- User interacts via touch

### The "Headless" - Intent API

- BroadcastReceiver handles Intents
- Routes to Controllers
- AI agents interact via Intents

### Controllers - Shared Business Logic

- All actual functionality lives here
- Both UI and API call the same methods
- Uses `Result<T>` for error handling
- No UI dependencies

## Adding New Features

See `/android/app/src/main/java/com/stonelauncher/api/README.md` for detailed instructions on adding Intent handlers.

Quick summary:

1. Create Controller in `controllers/`
2. Add handler method to `StoneApiReceiver`
3. Register action in `AndroidManifest.xml`
4. Document in `/docs/TOOLS.md`
5. Add test case to `test_intent_api.sh`

## Permissions

Permissions are declared in `AndroidManifest.xml` and handled based on type:

- **Normal**: Automatically granted
- **Dangerous**: Runtime request via UI
- **Special**: Manual grant in settings
- **ADB-granted**: Advanced features (documented separately)

See `/docs/LAUNCHER_REQUIREMENTS.md` for permission management patterns.

## Debugging

### Enable verbose logging

```bash
adb logcat -s StoneApiReceiver:V MainActivity:V
```

### Common issues

**Intent not received**
- Check `AndroidManifest.xml` registration
- Verify receiver is exported (`android:exported="true"`)
- Ensure action string matches exactly

**Result not sent**
- Check logcat for "Sent result" message
- Verify result Intent action matches pattern
- Ensure category is included

**Controller not found**
- Verify Controller class exists in `controllers/` package
- Check imports in handler method
- Ensure ProGuard rules don't strip Controller

## Resources

- [Android BroadcastReceiver Guide](https://developer.android.com/guide/components/broadcasts)
- `/docs/LAUNCHER_ARCHITECTURE.md` - Overall architecture
- `/docs/LAUNCHER_REQUIREMENTS.md` - Implementation patterns
- `/docs/TOOLS.md` - Intent API specification
- `/tickets/` - Implementation tickets

## License

Part of StoneOS project.
