# Stone Launcher - Developer Requirements & Implementation Guide

**For**: Developers and AI agents implementing Stone Launcher features
**Prerequisites**: Read LAUNCHER_ARCHITECTURE.md first
**Last Updated**: November 13, 2025

---

## Quick Start for Developers

### Repository Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── java/com/stonelauncher/
│   │   │   ├── api/          # Intent API (Headless interface)
│   │   │   ├── controllers/  # Business logic
│   │   │   ├── ui/           # Native UI (Activities/Fragments)
│   │   │   └── MainActivity.kt
│   │   ├── res/              # Resources, layouts
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

### Development Environment Setup

```bash
# 1. Open Android Studio
# File → Open → /Users/samuellarson/Pebble/Github/stone-os/android

# 2. Wait for Gradle sync

# 3. Build and run
./gradlew assembleDebug
./gradlew installDebug

# 4. Or use Android Studio Run button (Shift + F10)

# 5. Monitor logs
adb logcat -s StoneApiReceiver:* MainActivity:*
```

### Testing the Intent API

```bash
# Example: Toggle Wi-Fi via Intent
adb shell am broadcast \
  -a com.stone.launcher.action.SET_WIFI \
  --ez enabled true
```

---

## Core Development Principles

### 1. The "Head & Headless" Pattern

**Every feature must be implemented in TWO layers:**

```
Touch UI (Native Kotlin Activity/Fragment)
         ↓
Core Logic (Kotlin Controllers) ← Intent API (BroadcastReceiver)
```

Both the UI and the Intent API call the same Controller methods - single source of truth.

**Example Implementation**:

```kotlin
// android/app/src/main/java/com/stonelauncher/controllers/WifiController.kt
package com.stonelauncher.controllers

import android.content.Context
import android.net.wifi.WifiManager

class WifiController(private val context: Context) {
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // CORE LOGIC - Single source of truth
    fun setWifiEnabled(enabled: Boolean): Result<Boolean> {
        return try {
            if (wifiManager.setWifiEnabled(enabled)) {
                Result.success(enabled)
            } else {
                Result.failure(Exception("Failed to set WiFi state"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }
}
```

```kotlin
// android/app/src/main/java/com/stonelauncher/ui/SettingsActivity.kt
package com.stonelauncher.ui

import android.os.Bundle
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stonelauncher.controllers.WifiController

class SettingsActivity : AppCompatActivity() {

    private lateinit var wifiController: WifiController
    private lateinit var wifiSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        wifiController = WifiController(this)
        wifiSwitch = findViewById(R.id.wifi_switch)

        // Initialize switch state
        wifiSwitch.isChecked = wifiController.isWifiEnabled()

        // TOUCH UI - Calls same controller
        wifiSwitch.setOnCheckedChangeListener { _, isChecked ->
            wifiController.setWifiEnabled(isChecked).fold(
                onSuccess = { /* Update UI if needed */ },
                onFailure = { error ->
                    Toast.makeText(this, "WiFi error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
```

```kotlin
// android/app/src/main/java/com/stonelauncher/api/StoneApiReceiver.kt
package com.stonelauncher.api

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StoneApiReceiver : BroadcastReceiver() {

    // INTENT API (The "Headless")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.stone.launcher.action.SET_WIFI" -> {
                val enabled = intent.getBooleanExtra("enabled", false)
                val wifiController = WifiController(context)
                val result = wifiController.setWifiEnabled(enabled)

                // Send result back
                val resultIntent = Intent("com.stone.launcher.result.SET_WIFI")
                result.fold(
                    onSuccess = {
                        resultIntent.putExtra("success", true)
                        resultIntent.putExtra("wifi_enabled", it)
                    },
                    onFailure = {
                        resultIntent.putExtra("success", false)
                        resultIntent.putExtra("error_message", it.message)
                    }
                )
                context.sendBroadcast(resultIntent)
            }
        }
    }
}
```

```xml
<!-- res/layout/activity_settings.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="#000000">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Wi-Fi"
        android:textSize="18sp"
        android:textColor="#FFFFFF" />

    <Switch
        android:id="@+id/wifi_switch"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp" />

</LinearLayout>
```

**Key Principles**:
- ✅ WifiController has no UI dependencies
- ✅ SettingsActivity and StoneApiReceiver both use WifiController
- ✅ Touch UI calls controller methods directly
- ✅ AI agents call Intent API
- ✅ Both paths execute the same logic

---

### 2. UI-First Development Approach

**IMPORTANT**: Clone the web prototype design first, then add AI/chat capabilities.

**Development Order**:
1. Build native Kotlin UI that matches web prototype design
2. Integrate LiveKit for chat interface
3. Add AI agent capabilities
4. Implement tool calling API

**Why UI First**:
- Visual feedback confirms features work correctly
- Users need touch interface even with AI
- Easier to test and debug
- Matches actual user experience (choice-first, not voice-first)

**UI Design Principles** (from web prototype):
- Grayscale aesthetic (black background, white text)
- Minimal, text-focused interface
- No icons, no clutter
- 3x4 grid for app names on home screen
- Stone icon always visible at bottom
- Swipe up reveals chat (1/3 screen)

---

### 3. LiveKit Integration for Chat

**IMPORTANT**: LiveKit is CLIENT-SIDE ONLY in the launcher. The agents run in a separate server (agents.js).

**TODO: Research LiveKit Android SDK integration patterns** for accurate implementation details.

**Chat Interface Architecture** (conceptual - needs LiveKit SDK research):

```kotlin
// LiveKit Android SDK integration (CLIENT SIDE)
// Connects to agents.js server running separately
// TODO: Verify exact API patterns with LiveKit Android SDK docs
class ChatFragment : Fragment() {

    private lateinit var liveKitRoom: Room
    private lateinit var audioTrack: LocalAudioTrack

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize LiveKit CLIENT
        // TODO: Research exact initialization pattern
        liveKitRoom = LiveKit.create(
            appContext = requireContext().applicationContext
        )

        // Connect to agents.js server (running separately)
        connectToAgentServer()
    }

    private fun connectToAgentServer() {
        // URL points to your agents.js server (cloud or local)
        val url = "wss://stone-agent.example.com"
        val token = "..." // Auth token

        // TODO: Research LiveKit Room connection API
        liveKitRoom.connect(url, token)

        // Set up audio track for voice
        // TODO: Research audio track setup patterns
        audioTrack = liveKitRoom.localParticipant.createAudioTrack()
        liveKitRoom.localParticipant.publishAudioTrack(audioTrack)

        // Listen for tool calls from agents.js
        setupToolCallListener()
    }

    private fun setupToolCallListener() {
        // TODO: Research data channel API for receiving tool calls
        // Receive tool calls from agents.js via data channel
        // Execute them via Intent API
        // Return results to agents.js
    }
}
```

**Client/Server Architecture**:
- **Launcher (this app)**: LiveKit Android SDK client
- **agents.js**: LiveKit Agents SDK server (separate process)
- **Communication**: Audio, video, and data channels via LiveKit

**Chat appears when**:
- User swipes up from Stone icon
- User taps Stone icon
- Agent proactively sends message (notification)

**Chat Layout** (1/3 screen overlay):

```xml
<!-- res/layout/fragment_chat.xml -->
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="#000000"
    android:padding="16dp">

    <!-- Chat messages recycler view -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/chat_messages"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

    <!-- Voice indicator (when listening) -->
    <View
        android:id="@+id/voice_indicator"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:background="@drawable/stone_icon"
        android:alpha="0.5" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

### 4. Permission Management Pattern

**Every permission-gated feature follows this pattern:**

```kotlin
// android/app/src/main/java/com/stonelauncher/utils/PermissionHelper.kt
package com.stonelauncher.utils

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    const val PERMISSION_REQUEST_CODE = 100

    fun hasPermission(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(
        activity: Activity,
        permission: String,
        requestCode: Int = PERMISSION_REQUEST_CODE
    ) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    fun shouldShowRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}
```

**Usage in Activity**:

```kotlin
class ContactsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check and request permission
        if (!PermissionHelper.hasPermission(this, Manifest.permission.READ_CONTACTS)) {
            PermissionHelper.requestPermission(this, Manifest.permission.READ_CONTACTS)
        } else {
            loadContacts()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionHelper.PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts()
            } else {
                showPermissionDeniedDialog()
            }
        }
    }
}
```

---

### 5. Error Handling Pattern

**All async operations must use Kotlin Result<T>:**

```kotlin
// Controllers return Result<T>
fun launchApp(packageName: String): Result<Unit> {
    return try {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return Result.failure(Exception("App not found: $packageName"))

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// UI handles both success and failure
val result = appLauncherController.launchApp("com.spotify.music")
result.fold(
    onSuccess = {
        Log.d(TAG, "App launched successfully")
    },
    onFailure = { error ->
        Toast.makeText(this, "Failed: ${error.message}", Toast.LENGTH_SHORT).show()
    }
)
```

---

## Component Implementation Checklist

When implementing a new feature, ensure:

### For Controllers (Business Logic)

- [ ] Create Controller class in `android/app/src/main/java/com/stonelauncher/controllers/`
  - [ ] Core business logic only (no Android UI dependencies)
  - [ ] Returns `Result<T>` for error handling
  - [ ] Well-documented public methods
  - [ ] Takes Context in constructor if needed

### For Native UI (Activities/Fragments)

- [ ] Create Activity/Fragment in `android/app/src/main/java/com/stonelauncher/ui/`
  - [ ] Calls Controller methods
  - [ ] Handles permission requests
  - [ ] Handles loading states
  - [ ] Handles error states
  - [ ] Follows design system (grayscale, minimal)

- [ ] Create XML layout in `res/layout/`
  - [ ] Black background (#000000)
  - [ ] White text (#FFFFFF)
  - [ ] Minimal design, no unnecessary elements
  - [ ] Follows web prototype design

- [ ] Register Activity in `AndroidManifest.xml`

### For Intent API (Headless)

- [ ] Add Intent handler to `StoneApiReceiver.kt`:
  ```kotlin
  when (intent.action) {
      "com.stone.launcher.action.YOUR_ACTION" -> {
          // Extract parameters
          // Call controller
          // Send result
      }
  }
  ```

- [ ] Register Intent action in `AndroidManifest.xml`:
  ```xml
  <receiver android:name=".api.StoneApiReceiver" android:exported="true">
      <intent-filter>
          <action android:name="com.stone.launcher.action.YOUR_ACTION" />
      </intent-filter>
  </receiver>
  ```

- [ ] Document in `/docs/TOOLS.md`:
  - [ ] Action string
  - [ ] Required extras with types
  - [ ] Optional extras
  - [ ] Required permissions
  - [ ] Success response format
  - [ ] Error response format
  - [ ] Example adb command

---

## Development Roadmap

See `/docs/DEVELOPMENT_ROADMAP.md` for the complete development sequence.

**Quick Summary**:

1. **Phase 1: Native UI Foundation** - Build home screen, app grid, grayscale design
2. **Phase 2: Chat Interface** - Integrate LiveKit, create chat overlay
3. **Phase 3: AI Agent Integration** - Connect to agents.js backend, implement tool calling
4. **Phase 4: Feature Implementation** - Build the 12 Stone Apps

---

## Testing Requirements

### Unit Tests (Kotlin)

```kotlin
// android/app/src/test/java/com/stonelauncher/controllers/WifiControllerTest.kt
class WifiControllerTest {

    @Test
    fun `setWifiEnabled returns success when WiFi is enabled`() {
        val context = mockContext()
        val controller = WifiController(context)

        val result = controller.setWifiEnabled(true)

        assertTrue(result.isSuccess)
    }
}
```

### Integration Tests (Intent API)

```bash
# Test WiFi Intent
adb shell am broadcast -a com.stone.launcher.action.SET_WIFI --ez enabled true

# Monitor result
adb logcat | grep "StoneApiReceiver"
```

### UI Tests (Espresso)

```kotlin
@Test
fun wifiToggle_changesState() {
    onView(withId(R.id.wifi_switch)).perform(click())
    // Verify WiFi state changed
}
```

---

## Code Style Guidelines

### Kotlin

```kotlin
// File naming: PascalCase (WifiController.kt)
// Class naming: PascalCase (WifiController)
// Function naming: camelCase (setWifiEnabled)
// Constants: UPPER_SNAKE_CASE (MAX_RETRY_COUNT)

// Use Result<T> for operations that can fail
fun riskyOperation(): Result<Data> {
    return try {
        val data = performOperation()
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Use data classes for DTOs
data class Contact(
    val id: String,
    val name: String,
    val phoneNumbers: List<String>
)

// Document public APIs
/**
 * Enables or disables WiFi.
 *
 * @param enabled true to enable WiFi, false to disable
 * @return Result containing the new WiFi state or an error
 */
fun setWifiEnabled(enabled: Boolean): Result<Boolean>
```

### XML Layouts

```xml
<!-- Use descriptive IDs -->
<TextView
    android:id="@+id/app_name_text"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Wi-Fi"
    android:textColor="#FFFFFF"
    android:textSize="18sp" />

<!-- Black background always -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">
```

---

## Common Issues & Solutions

### Issue: "Module not found" when importing Controller

**Solution**: Ensure package structure matches:
```
com.stonelauncher.controllers.WifiController
```

### Issue: Intent not received by BroadcastReceiver

**Solution**: Check `AndroidManifest.xml` registration:
```xml
<receiver android:name=".api.StoneApiReceiver" android:exported="true">
    <intent-filter>
        <action android:name="com.stone.launcher.action.YOUR_ACTION" />
    </intent-filter>
</receiver>
```

### Issue: Permission denied errors

**Solution**:
1. Declare in `AndroidManifest.xml`
2. Request at runtime for dangerous permissions
3. Use `PermissionHelper` utility

### Issue: App crashes when calling system APIs

**Solution**: Wrap in try-catch and return `Result<T>`:
```kotlin
return try {
    // risky operation
    Result.success(data)
} catch (e: Exception) {
    Result.failure(e)
}
```

---

## Next Steps

1. **Read DEVELOPMENT_ROADMAP.md** - Understand development phases
2. **Study web prototype** - Understand UI design to clone
3. **Build home screen** - Start with basic launcher UI
4. **Integrate LiveKit** - Add chat interface
5. **Connect to agents** - Implement tool calling API

---

**For Questions**: Refer to LAUNCHER_ARCHITECTURE.md for high-level design decisions, or create a new ticket with your question.
