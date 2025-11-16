# Ticket #007: Tool Calling and Device Control Integration

**Status**: Not Started
**Priority**: HIGH
**Dependencies**: TICKET_006 (Agent Server Setup)

---

## Objective

Connect the AI agents to device control capabilities through tool calling. This enables the AI to control Android device features like opening apps, controlling settings, navigation, and more.

---

## Background

With the agent server (TICKET_006) and Android LiveKit integration (TICKET_005) in place, we need to implement the bidirectional communication that allows:
- AI agents to call tools that control the Android device
- Android app to receive and execute these commands
- Status updates to flow back to the agent

This creates the "headless" control layer where AI can operate the device on behalf of the user.

---

## Requirements

### Android Side (Device Control)
- [ ] Receive tool commands via LiveKit data channels
- [ ] Parse and validate commands
- [ ] Execute device actions (open apps, control settings, etc.)
- [ ] Send status updates back to agent
- [ ] Handle permissions for device control

### Agent Side (Tool Definitions)
- [ ] Define tools for all 12 Stone apps
- [ ] Implement device control tools (WiFi, Bluetooth, etc.)
- [ ] Navigation and location tools
- [ ] Media control tools
- [ ] Status broadcasting during operations

### Integration
- [ ] Bidirectional data channel communication
- [ ] Command validation and error handling
- [ ] Operation tracking and status updates
- [ ] Permission handling and user consent

---

## Research Findings

### 1. Android Device Control APIs

#### A. App Control (Opening Apps)

**Primary Method: PackageManager + Intent**
```kotlin
// Launch app by package name
fun openApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)

    // Android 13+ compatibility
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        intent?.addCategory(Intent.CATEGORY_LAUNCHER)
    }

    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    if (intent != null) {
        context.startActivity(intent)
    } else {
        // App not installed - redirect to Play Store
        val storeIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(storeIntent)
    }
}
```

**CRITICAL: Package Visibility (Android 11+)**
```xml
<!-- Must declare in AndroidManifest.xml -->
<queries>
    <package android:name="com.spotify.music"/>
    <package android:name="com.google.android.apps.maps"/>
    <!-- Add all apps Stone can control -->
</queries>
```

**Opening Specific Activities:**
```kotlin
// Launch specific activity with ComponentName
val intent = Intent().apply {
    component = ComponentName("com.app.package", "com.app.package.MainActivity")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
}
context.startActivity(intent)
```

#### B. WiFi Control

**API: WifiManager**
```kotlin
import android.net.wifi.WifiManager
import android.content.Context

fun setWifiEnabled(context: Context, enabled: Boolean): Result<Boolean> {
    val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager

    return try {
        wifiManager.isWifiEnabled = enabled
        Result.success(wifiManager.isWifiEnabled)
    } catch (e: SecurityException) {
        Result.failure(e)
    }
}

fun getWifiState(context: Context): Boolean {
    val wifiManager = context.applicationContext
        .getSystemService(Context.WIFI_SERVICE) as WifiManager
    return wifiManager.isWifiEnabled
}
```

**Required Permissions:**
```xml
<!-- Normal permissions (auto-granted) -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

<!-- For Android 13+ WiFi scanning -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />
```

#### C. Bluetooth Control

**API: BluetoothAdapter**
```kotlin
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context

fun setBluetoothEnabled(context: Context, enabled: Boolean): Result<Boolean> {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val bluetoothAdapter = bluetoothManager.adapter

    return try {
        if (enabled) {
            // Request user to enable Bluetooth
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(enableIntent)
        } else {
            // Cannot programmatically disable - security restriction
            // Must guide user to settings
        }
        Result.success(bluetoothAdapter.isEnabled)
    } catch (e: SecurityException) {
        Result.failure(e)
    }
}
```

**Required Permissions (Android 12+):**
```xml
<!-- For scanning/discovering devices -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />

<!-- For connecting to paired devices -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- For making device discoverable -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- Legacy permissions (maxSdkVersion 30) -->
<uses-permission android:name="android.permission.BLUETOOTH"
                 android:maxSdkVersion="30" />
```

**IMPORTANT:** All Bluetooth permissions except legacy are **Dangerous** and require runtime request.

#### D. Screen Brightness Control

**API: Settings.System**
```kotlin
import android.provider.Settings

fun setBrightness(context: Context, brightness: Int): Result<Boolean> {
    // brightness: 0-255

    // Check if we can write settings
    if (!Settings.System.canWrite(context)) {
        // Request permission via Settings
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return Result.failure(SecurityException("WRITE_SETTINGS permission not granted"))
    }

    return try {
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            brightness.coerceIn(0, 255)
        )
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Required Permission:**
```xml
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
```

**Permission Type:** **Special** - Must be granted manually via Settings

#### E. Volume Control

**API: AudioManager**
```kotlin
import android.media.AudioManager

fun setVolume(context: Context, streamType: Int, volume: Int): Result<Boolean> {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    return try {
        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val targetVolume = volume.coerceIn(0, maxVolume)

        audioManager.setStreamVolume(
            streamType,
            targetVolume,
            0 // flags - 0 means no UI
        )
        Result.success(true)
    } catch (e: SecurityException) {
        Result.failure(e)
    }
}

// Stream types:
// AudioManager.STREAM_MUSIC - Media playback
// AudioManager.STREAM_RING - Phone ringer
// AudioManager.STREAM_NOTIFICATION - Notifications
// AudioManager.STREAM_ALARM - Alarms
// AudioManager.STREAM_VOICE_CALL - In-call volume
```

**Required Permission:**
```xml
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

**Permission Type:** **Normal** (auto-granted)

**LIMITATION:** `MODE_IN_CALL` restricted to system apps only. Use `MODE_IN_COMMUNICATION` for third-party apps.

#### F. Making Phone Calls

**API: Intent with ACTION_CALL**
```kotlin
fun makePhoneCall(context: Context, phoneNumber: String): Result<Boolean> {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = Uri.parse("tel:$phoneNumber")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(intent)
        Result.success(true)
    } catch (e: SecurityException) {
        Result.failure(e)
    } catch (e: ActivityNotFoundException) {
        Result.failure(e)
    }
}

// For dial screen (doesn't auto-call):
fun openDialer(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL).apply {
        data = Uri.parse("tel:$phoneNumber")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

**Required Permissions:**
```xml
<!-- For ACTION_CALL (auto-dial) -->
<uses-permission android:name="android.permission.CALL_PHONE" />

<!-- For reading phone state -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

**Permission Type:** **Dangerous** - Requires runtime request

**IMPORTANT:** Apps must request to be default Phone handler if using Call Log permissions extensively.

#### G. Sending SMS

**API: SmsManager**
```kotlin
import android.telephony.SmsManager

fun sendSMS(phoneNumber: String, message: String): Result<Boolean> {
    return try {
        val smsManager = SmsManager.getDefault()

        // For long messages, divide into parts
        if (message.length > 160) {
            val parts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                parts,
                null,
                null
            )
        } else {
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                null,
                null
            )
        }

        Result.success(true)
    } catch (e: SecurityException) {
        Result.failure(e)
    }
}
```

**Required Permissions:**
```xml
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
```

**Permission Type:** **Dangerous** - Requires runtime request

**CRITICAL Google Play Restriction:** Apps using SMS permissions must be default SMS handler OR explicitly approved. Non-default handlers must:
1. Display prominent disclosure within app
2. Clearly describe SMS usage
3. Get explicit user consent

#### H. Google Maps Navigation

**API: Intent with geo: URI**
```kotlin
// Navigation with turn-by-turn directions
fun navigateToLocation(context: Context, address: String, mode: String = "d"): Result<Boolean> {
    // mode: "d" = driving, "w" = walking, "b" = bicycling, "l" = two-wheeler
    val encodedAddress = Uri.encode(address)
    val uri = Uri.parse("google.navigation:q=$encodedAddress&mode=$mode")

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(intent)
        Result.success(true)
    } catch (e: ActivityNotFoundException) {
        // Google Maps not installed
        Result.failure(e)
    }
}

// Navigate to coordinates
fun navigateToCoordinates(context: Context, lat: Double, lng: Double): Result<Boolean> {
    val uri = Uri.parse("google.navigation:q=$lat,$lng")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(intent)
        Result.success(true)
    } catch (e: ActivityNotFoundException) {
        Result.failure(e)
    }
}

// Show location on map (not navigation)
fun showLocation(context: Context, lat: Double, lng: Double, label: String): Result<Boolean> {
    val query = "$lat,$lng($label)"
    val encodedQuery = Uri.encode(query)
    val uri = Uri.parse("geo:$lat,$lng?q=$encodedQuery&z=16")

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return try {
        context.startActivity(intent)
        Result.success(true)
    } catch (e: ActivityNotFoundException) {
        Result.failure(e)
    }
}
```

**Required Permissions:** None (uses implicit intent)

**Package Visibility:**
```xml
<queries>
    <package android:name="com.google.android.apps.maps"/>
</queries>
```

#### I. Spotify Control

**API: Spotify App Remote SDK**
```kotlin
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.android.appremote.api.ConnectionParams

// Connect to Spotify
fun connectSpotify(context: Context, clientId: String): Result<SpotifyAppRemote> {
    val connectionParams = ConnectionParams.Builder(clientId)
        .setRedirectUri("stoneos://callback")
        .showAuthView(true)
        .build()

    return try {
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                // Connected - can control playback
            }

            override fun onFailure(throwable: Throwable) {
                // Connection failed
            }
        })
        Result.success(appRemote)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Play track
fun playSpotifyTrack(appRemote: SpotifyAppRemote, trackUri: String) {
    appRemote.playerApi.play(trackUri)
}

// Control playback
fun pauseSpotify(appRemote: SpotifyAppRemote) {
    appRemote.playerApi.pause()
}

fun resumeSpotify(appRemote: SpotifyAppRemote) {
    appRemote.playerApi.resume()
}

fun skipToNext(appRemote: SpotifyAppRemote) {
    appRemote.playerApi.skipNext()
}
```

**Authentication:** OAuth 2.0 via Spotify Authorization Library
- Redirect URI must be registered in Spotify Developer Dashboard
- Requires `app-remote-control` scope
- Shows authorization view on first connection

**SDK Requirements:**
```gradle
implementation 'com.spotify.android:app-remote:0.8.0'
implementation 'com.spotify.android:auth:2.1.0'
```

**Package Visibility:**
```xml
<queries>
    <package android:name="com.spotify.music"/>
</queries>
```

**Capabilities:**
- Control playback (play, pause, skip, seek)
- Get currently playing track metadata
- Access to user's playlists
- Works for all users (not just Premium)
- Offline playback supported

#### J. Do Not Disturb (DND) Control

**API: NotificationManager**
```kotlin
import android.app.NotificationManager

fun setDoNotDisturb(context: Context, enabled: Boolean): Result<Boolean> {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
        as NotificationManager

    // Check if we have permission
    if (!notificationManager.isNotificationPolicyAccessGranted) {
        // Request permission via Settings
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return Result.failure(SecurityException("DND access not granted"))
    }

    return try {
        val filter = if (enabled) {
            NotificationManager.INTERRUPTION_FILTER_NONE
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }

        notificationManager.setInterruptionFilter(filter)
        Result.success(true)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

**Required Permission:** **Special** - Must be granted via Settings
- No manifest declaration
- User must grant via `Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS`

**Interruption Filter Options:**
- `INTERRUPTION_FILTER_NONE` - Silence everything
- `INTERRUPTION_FILTER_PRIORITY` - Allow priority notifications
- `INTERRUPTION_FILTER_ALL` - Allow all notifications
- `INTERRUPTION_FILTER_ALARMS` - Only alarms

**Android 14 Note:** Some users report issues with `setInterruptionFilter()` on Android 14.

---

### 2. Complete Permission Requirements

#### Normal Permissions (Auto-granted)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.BLUETOOTH"
                 android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

#### Dangerous Permissions (Runtime Request Required)
```xml
<!-- Location (for WiFi scanning, Maps) -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- Telephony -->
<uses-permission android:name="android.permission.CALL_PHONE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.SEND_SMS" />
<uses-permission android:name="android.permission.READ_SMS" />
<uses-permission android:name="android.permission.RECEIVE_SMS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />

<!-- Contacts -->
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.WRITE_CONTACTS" />

<!-- Calendar -->
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />

<!-- Audio/Video -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Bluetooth (Android 12+) -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- WiFi (Android 13+) -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
                 android:usesPermissionFlags="neverForLocation" />
```

#### Special Permissions (Manual Grant via Settings)
```xml
<uses-permission android:name="android.permission.WRITE_SETTINGS" />
<!-- DND access: No manifest declaration - request via Settings intent -->
```

#### Package Visibility (Android 11+)
```xml
<queries>
    <package android:name="com.spotify.music"/>
    <package android:name="com.google.android.apps.maps"/>
    <package android:name="com.google.android.gm"/> <!-- Gmail -->
    <package android:name="com.android.chrome"/>
    <!-- Add all controllable apps -->
</queries>
```

#### Runtime Permission Request Pattern
```kotlin
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { entry ->
            val permission = entry.key
            val granted = entry.value

            if (granted) {
                Log.d(TAG, "Permission granted: $permission")
            } else {
                Log.d(TAG, "Permission denied: $permission")
                // Show rationale or disable feature
            }
        }
    }

    fun requestDangerousPermissions() {
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.RECORD_AUDIO
        ))
    }
}
```

---

### 3. LiveKit Data Channel Message Format

#### Message Structure (JSON-RPC Style)

Based on LiveKit RPC documentation and existing agent code, messages follow this pattern:

```typescript
// Tool Call Message (Agent → Android)
interface ToolCallMessage {
  type: 'tool_call'
  id: string                    // Unique request ID
  method: string                // Tool name (e.g., "open_app", "set_wifi")
  params: Record<string, any>   // Tool parameters
  timestamp: number             // Unix timestamp
}

// Tool Result Message (Android → Agent)
interface ToolResultMessage {
  type: 'tool_result'
  id: string                    // Matches request ID
  success: boolean              // Operation success
  result?: any                  // Result data (if success)
  error?: {
    code: string                // Error code
    message: string             // Error message
  }
  timestamp: number
}

// Status Update Message (Android → Agent)
interface StatusUpdateMessage {
  type: 'status_update'
  operation_id: string          // Associated operation ID
  status: 'pending' | 'in_progress' | 'completed' | 'failed'
  message: string               // Human-readable status
  progress?: number             // 0-100 for long operations
  timestamp: number
}

// Chat Message (Bidirectional)
interface ChatMessage {
  type: 'chat_message'
  content: string               // Message text
  sender: 'user' | 'agent'
  timestamp: number
}
```

#### Example Message Exchange

**1. Agent requests to open Spotify:**
```json
{
  "type": "tool_call",
  "id": "req_1234567890",
  "method": "open_app",
  "params": {
    "package_name": "com.spotify.music"
  },
  "timestamp": 1700000000000
}
```

**2. Android sends status update:**
```json
{
  "type": "status_update",
  "operation_id": "req_1234567890",
  "status": "in_progress",
  "message": "Opening Spotify...",
  "timestamp": 1700000000100
}
```

**3. Android sends result:**
```json
{
  "type": "tool_result",
  "id": "req_1234567890",
  "success": true,
  "result": {
    "app_opened": true,
    "package_name": "com.spotify.music"
  },
  "timestamp": 1700000000500
}
```

**Error example:**
```json
{
  "type": "tool_result",
  "id": "req_1234567890",
  "success": false,
  "error": {
    "code": "APP_NOT_INSTALLED",
    "message": "Spotify is not installed on this device"
  },
  "timestamp": 1700000000500
}
```

#### LiveKit Topic Conventions

```kotlin
// Tool call requests
const val TOPIC_TOOL_CALL = "stone.tool_call"

// Tool results
const val TOPIC_TOOL_RESULT = "stone.tool_result"

// Status updates
const val TOPIC_STATUS = "stone.status"

// Chat messages
const val TOPIC_CHAT = "lk.chat"
```

#### Publishing Messages via LiveKit

```kotlin
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

// Publish tool result
suspend fun sendToolResult(result: ToolResultMessage) {
    val json = Json.encodeToString(result)
    val data = json.toByteArray(Charsets.UTF_8)

    LiveKitManager.publishData(
        data = data,
        topic = "stone.tool_result",
        reliability = DataPublishReliability.RELIABLE
    )
}

// Publish status update
suspend fun sendStatusUpdate(status: StatusUpdateMessage) {
    val json = Json.encodeToString(status)
    val data = json.toByteArray(Charsets.UTF_8)

    LiveKitManager.publishData(
        data = data,
        topic = "stone.status",
        reliability = DataPublishReliability.RELIABLE
    )
}
```

#### Receiving Messages

```kotlin
// In LiveKit event handler
room.events.collect { event ->
    when (event) {
        is RoomEvent.DataReceived -> {
            val message = String(event.data, Charsets.UTF_8)
            val parsed = Json.decodeFromString<ToolCallMessage>(message)

            // Route to appropriate handler
            handleToolCall(parsed)
        }
    }
}
```

---

### 4. Security Considerations

#### A. Rate Limiting

Implement token bucket algorithm for command rate limiting:

```kotlin
class CommandRateLimiter(
    private val maxTokens: Int = 10,
    private val refillRate: Long = 1000L // 1 token per second
) {
    private var tokens = maxTokens
    private var lastRefill = System.currentTimeMillis()

    fun allowCommand(commandType: String): Boolean {
        refillTokens()

        // High-risk commands cost more tokens
        val cost = when (commandType) {
            "make_call", "send_sms" -> 3
            "set_wifi", "set_bluetooth" -> 2
            else -> 1
        }

        return if (tokens >= cost) {
            tokens -= cost
            true
        } else {
            Log.w(TAG, "Rate limit exceeded for $commandType")
            false
        }
    }

    private fun refillTokens() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRefill
        val tokensToAdd = (elapsed / refillRate).toInt()

        if (tokensToAdd > 0) {
            tokens = (tokens + tokensToAdd).coerceAtMost(maxTokens)
            lastRefill = now
        }
    }
}
```

**Recommended Limits:**
- General commands: 10 per 10 seconds
- Phone calls: 3 per minute
- SMS: 5 per minute
- WiFi/Bluetooth toggles: 5 per minute

#### B. Command Validation

```kotlin
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

object CommandValidator {

    fun validateToolCall(message: ToolCallMessage): ValidationResult {
        // 1. Validate message structure
        if (message.id.isBlank()) {
            return ValidationResult.Invalid("Missing request ID")
        }

        if (message.method.isBlank()) {
            return ValidationResult.Invalid("Missing method name")
        }

        // 2. Validate method is allowed
        if (message.method !in ALLOWED_METHODS) {
            return ValidationResult.Invalid("Unknown method: ${message.method}")
        }

        // 3. Validate parameters
        return when (message.method) {
            "open_app" -> validateOpenApp(message.params)
            "make_call" -> validatePhoneNumber(message.params["phone_number"])
            "send_sms" -> validateSMS(message.params)
            "navigate_to" -> validateNavigation(message.params)
            else -> ValidationResult.Valid
        }
    }

    private fun validatePhoneNumber(phoneNumber: Any?): ValidationResult {
        if (phoneNumber !is String) {
            return ValidationResult.Invalid("Phone number must be a string")
        }

        // Basic phone number validation
        val cleaned = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleaned.length < 10 || cleaned.length > 15) {
            return ValidationResult.Invalid("Invalid phone number format")
        }

        return ValidationResult.Valid
    }

    private fun validateSMS(params: Map<String, Any>): ValidationResult {
        val phoneNumber = params["phone_number"]
        val message = params["message"]

        if (message !is String || message.length > 1600) {
            return ValidationResult.Invalid("Message too long (max 1600 chars)")
        }

        return validatePhoneNumber(phoneNumber)
    }

    companion object {
        val ALLOWED_METHODS = setOf(
            "open_app",
            "set_wifi",
            "set_bluetooth",
            "set_brightness",
            "set_volume",
            "make_call",
            "send_sms",
            "navigate_to",
            "play_spotify",
            "set_dnd"
        )
    }
}
```

#### C. Dangerous Operations Blocklist

```kotlin
object SecurityPolicy {

    // Operations that require explicit user confirmation
    val REQUIRES_CONFIRMATION = setOf(
        "make_call",
        "send_sms",
        "open_financial_app", // Bank, payment apps
        "uninstall_app",
        "factory_reset"
    )

    // Operations completely blocked from AI control
    val BLOCKED_OPERATIONS = setOf(
        "disable_security",
        "remove_account",
        "change_password",
        "grant_admin",
        "root_access"
    )

    // Package names that cannot be controlled
    val PROTECTED_PACKAGES = setOf(
        "com.android.settings",
        "com.google.android.gms",
        "com.stonelauncher" // Cannot close itself
    )

    fun isOperationAllowed(method: String, params: Map<String, Any>): Boolean {
        // Block completely prohibited operations
        if (method in BLOCKED_OPERATIONS) {
            return false
        }

        // Block protected packages
        if (method == "open_app" || method == "close_app") {
            val packageName = params["package_name"] as? String
            if (packageName in PROTECTED_PACKAGES) {
                return false
            }
        }

        return true
    }

    fun requiresUserConfirmation(method: String): Boolean {
        return method in REQUIRES_CONFIRMATION
    }
}
```

#### D. User Consent Mechanisms

```kotlin
class UserConsentManager(private val context: Context) {

    suspend fun requestConsent(
        operation: String,
        details: String
    ): Boolean = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            AlertDialog.Builder(context)
                .setTitle("AI Action Request")
                .setMessage("Stone wants to:\n\n$details\n\nAllow this action?")
                .setPositiveButton("Allow") { _, _ ->
                    continuation.resume(true)
                }
                .setNegativeButton("Deny") { _, _ ->
                    continuation.resume(false)
                }
                .setCancelable(false)
                .show()
        }
    }
}

// Usage in command handler
suspend fun handleMakeCall(params: Map<String, Any>): ToolResultMessage {
    val phoneNumber = params["phone_number"] as String

    // Request user consent
    val allowed = consentManager.requestConsent(
        operation = "make_call",
        details = "Call $phoneNumber"
    )

    if (!allowed) {
        return ToolResultMessage(
            id = requestId,
            success = false,
            error = ErrorInfo("USER_DENIED", "User denied call permission")
        )
    }

    // Proceed with call
    makePhoneCall(context, phoneNumber)
}
```

#### E. Audit Logging

```kotlin
data class AuditLog(
    val timestamp: Long,
    val operation: String,
    val params: Map<String, Any>,
    val success: Boolean,
    val errorMessage: String? = null
)

object AuditLogger {

    private val logs = mutableListOf<AuditLog>()
    private const val MAX_LOGS = 1000

    fun logOperation(
        operation: String,
        params: Map<String, Any>,
        success: Boolean,
        errorMessage: String? = null
    ) {
        val log = AuditLog(
            timestamp = System.currentTimeMillis(),
            operation = operation,
            params = params,
            success = success,
            errorMessage = errorMessage
        )

        logs.add(log)

        // Keep only recent logs
        if (logs.size > MAX_LOGS) {
            logs.removeAt(0)
        }

        // Persist to disk for forensics
        persistLog(log)
    }

    private fun persistLog(log: AuditLog) {
        // Write to encrypted local storage
        // Format: timestamp|operation|success|error
    }
}
```

---

### 5. Tool Definition Patterns (from agents.js)

Based on analysis of `/Users/samuellarson/Pebble/Github/stone-os/stone-agent/src/tools.ts`:

#### Tool Structure

```typescript
interface Tool {
  name: string                    // Unique tool name (prefixed by MCP server)
  description: string             // What the tool does
  parameters: object              // JSON Schema for parameters
  execute: (params: any) => Promise<any>  // Async execution
}
```

#### Example Tool Definition

```typescript
{
  name: 'spotify_play_track',
  description: 'Play a track on Spotify by name or URI',
  parameters: {
    type: 'object',
    properties: {
      query: {
        type: 'string',
        description: 'Track name, artist, or Spotify URI'
      }
    },
    required: ['query']
  },
  execute: async (params) => {
    // Send command to Android via LiveKit data channel
    const message: ToolCallMessage = {
      type: 'tool_call',
      id: generateId(),
      method: 'spotify_play',
      params: params,
      timestamp: Date.now()
    }

    const result = await sendToolCallAndWaitForResult(message)

    if (result.success) {
      return result.result
    } else {
      throw new Error(result.error.message)
    }
  }
}
```

#### Dynamic Tool Loading Pattern

From `DynamicToolLoader` class:

```typescript
// Tools are loaded based on context/app
const contextToMCP: Record<string, string[]> = {
  listen: ['spotify'],
  go: ['maps'],
  connect: ['telephony'],
  plan: ['calendar'],
  // ...
}

// MCP servers expose tools via standard protocol
const mcpTools = await client.listTools()

// Tools are wrapped to add context prefix
const wrappedTools = mcpTools.map(tool => ({
  name: `${source}_${tool.name}`,  // e.g., "spotify_play_track"
  description: tool.description,
  parameters: tool.inputSchema,
  execute: async (params) => {
    return await client.callTool(tool.name, params)
  }
}))
```

#### Error Handling in Tools

```typescript
execute: async (params: any) => {
  try {
    const result = await performOperation(params)
    return result
  } catch (error) {
    // Errors bubble up to LLM
    if (error instanceof SecurityException) {
      throw new Error(`Permission denied: ${error.message}`)
    } else if (error instanceof NotFoundException) {
      throw new Error(`Not found: ${error.message}`)
    } else {
      throw new Error(`Operation failed: ${error.message}`)
    }
  }
}
```

#### Status Broadcasting Pattern

From agent.ts:

```typescript
// During long operations, broadcast status updates
async function navigateToLocation(address: string) {
  // 1. Send initial status
  await broadcastStatus({
    operation_id: requestId,
    status: 'in_progress',
    message: 'Looking up address...'
  })

  // 2. Perform operation
  const coords = await geocodeAddress(address)

  await broadcastStatus({
    operation_id: requestId,
    status: 'in_progress',
    message: 'Opening Google Maps...',
    progress: 50
  })

  await openMaps(coords)

  // 3. Send completion
  await broadcastStatus({
    operation_id: requestId,
    status: 'completed',
    message: 'Navigation started',
    progress: 100
  })
}
```

---

### 6. Existing Infrastructure Analysis

#### StoneApiReceiver Pattern

From `/Users/samuellarson/Pebble/Github/stone-os/android/app/src/main/java/com/stonelauncher/api/StoneApiReceiver.kt`:

**Current Implementation:**
- BroadcastReceiver for handling Intent API calls
- Routes actions to handler methods
- Returns results via broadcast Intent
- Uses `IntentResult` data class for success/error

**Pattern We Can Extend:**

```kotlin
// Current: Intent-based API for ADB/external apps
// Pattern: Receive Intent → Route to handler → Send result broadcast

// New: LiveKit-based API for AI agents
// Pattern: Receive data message → Route to handler → Send result message

class DeviceCommandHandler(
    private val context: Context
) {

    suspend fun handleToolCall(message: ToolCallMessage): ToolResultMessage {
        Log.d(TAG, "Handling tool call: ${message.method}")

        // 1. Validate
        val validation = CommandValidator.validateToolCall(message)
        if (validation is ValidationResult.Invalid) {
            return ToolResultMessage(
                id = message.id,
                success = false,
                error = ErrorInfo("VALIDATION_ERROR", validation.reason),
                timestamp = System.currentTimeMillis()
            )
        }

        // 2. Check rate limit
        if (!rateLimiter.allowCommand(message.method)) {
            return ToolResultMessage(
                id = message.id,
                success = false,
                error = ErrorInfo("RATE_LIMIT", "Too many requests"),
                timestamp = System.currentTimeMillis()
            )
        }

        // 3. Check security policy
        if (!SecurityPolicy.isOperationAllowed(message.method, message.params)) {
            return ToolResultMessage(
                id = message.id,
                success = false,
                error = ErrorInfo("FORBIDDEN", "Operation not allowed"),
                timestamp = System.currentTimeMillis()
            )
        }

        // 4. Request user consent if needed
        if (SecurityPolicy.requiresUserConfirmation(message.method)) {
            val allowed = requestUserConsent(message)
            if (!allowed) {
                return ToolResultMessage(
                    id = message.id,
                    success = false,
                    error = ErrorInfo("USER_DENIED", "User denied permission"),
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        // 5. Route to appropriate handler
        return try {
            val result = when (message.method) {
                "open_app" -> handleOpenApp(message.params)
                "set_wifi" -> handleSetWifi(message.params)
                "set_bluetooth" -> handleSetBluetooth(message.params)
                "set_brightness" -> handleSetBrightness(message.params)
                "set_volume" -> handleSetVolume(message.params)
                "make_call" -> handleMakeCall(message.params)
                "send_sms" -> handleSendSMS(message.params)
                "navigate_to" -> handleNavigate(message.params)
                "spotify_play" -> handleSpotifyPlay(message.params)
                "set_dnd" -> handleSetDND(message.params)
                else -> throw IllegalArgumentException("Unknown method: ${message.method}")
            }

            // 6. Log operation
            AuditLogger.logOperation(
                operation = message.method,
                params = message.params,
                success = true
            )

            ToolResultMessage(
                id = message.id,
                success = true,
                result = result,
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error executing ${message.method}", e)

            // Log failure
            AuditLogger.logOperation(
                operation = message.method,
                params = message.params,
                success = false,
                errorMessage = e.message
            )

            ToolResultMessage(
                id = message.id,
                success = false,
                error = ErrorInfo(
                    code = e::class.simpleName ?: "UNKNOWN_ERROR",
                    message = e.message ?: "Unknown error occurred"
                ),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private suspend fun handleOpenApp(params: Map<String, Any>): Map<String, Any> {
        val packageName = params["package_name"] as String

        // Send status update
        sendStatusUpdate(
            operationId = currentOperationId,
            status = "in_progress",
            message = "Opening app..."
        )

        val result = AppController.openApp(context, packageName)

        return if (result.isSuccess) {
            mapOf("app_opened" to true, "package_name" to packageName)
        } else {
            throw result.exceptionOrNull()!!
        }
    }

    // Similar handlers for other operations...
}
```

**Key Differences from Intent API:**
- **Async**: All handlers are suspend functions (Intent handlers are synchronous)
- **Bidirectional**: Can send status updates during operation
- **Richer data**: JSON objects instead of Intent extras
- **Security**: Rate limiting, validation, user consent built-in

---

## Implementation Plan

### Phase 1: Android Infrastructure (3-4 days)

#### 1.1 Create Message Data Classes
**File:** `android/app/src/main/java/com/stonelauncher/tools/Messages.kt`

```kotlin
@Serializable
data class ToolCallMessage(
    val type: String = "tool_call",
    val id: String,
    val method: String,
    val params: Map<String, JsonElement>,
    val timestamp: Long
)

@Serializable
data class ToolResultMessage(
    val type: String = "tool_result",
    val id: String,
    val success: Boolean,
    val result: Map<String, JsonElement>? = null,
    val error: ErrorInfo? = null,
    val timestamp: Long
)

@Serializable
data class ErrorInfo(
    val code: String,
    val message: String
)

@Serializable
data class StatusUpdateMessage(
    val type: String = "status_update",
    val operation_id: String,
    val status: String,
    val message: String,
    val progress: Int? = null,
    val timestamp: Long
)
```

#### 1.2 Create Controller Classes
**Files to create:**
- `android/app/src/main/java/com/stonelauncher/tools/AppController.kt`
- `android/app/src/main/java/com/stonelauncher/tools/SettingsController.kt`
- `android/app/src/main/java/com/stonelauncher/tools/NavigationController.kt`
- `android/app/src/main/java/com/stonelauncher/tools/MediaController.kt`
- `android/app/src/main/java/com/stonelauncher/tools/TelephonyController.kt`

Each controller implements specific operations (WiFi, Bluetooth, calls, etc.)

#### 1.3 Create Security & Validation Layer
**Files to create:**
- `android/app/src/main/java/com/stonelauncher/tools/CommandValidator.kt`
- `android/app/src/main/java/com/stonelauncher/tools/SecurityPolicy.kt`
- `android/app/src/main/java/com/stonelauncher/tools/CommandRateLimiter.kt`
- `android/app/src/main/java/com/stonelauncher/tools/UserConsentManager.kt`
- `android/app/src/main/java/com/stonelauncher/tools/AuditLogger.kt`

#### 1.4 Create Main Command Handler
**File:** `android/app/src/main/java/com/stonelauncher/tools/DeviceCommandHandler.kt`

Integrates all controllers, validation, rate limiting, and user consent.

#### 1.5 Integrate with LiveKit
**Modify:** `android/app/src/main/java/com/stonelauncher/livekit/LiveKitManager.kt`

Add data channel message listeners and route to DeviceCommandHandler.

```kotlin
// In LiveKitManager
fun setupToolCallListener() {
    scope.launch {
        room?.events?.collect { event ->
            when (event) {
                is RoomEvent.DataReceived -> {
                    if (event.topic == "stone.tool_call") {
                        handleIncomingToolCall(event.data)
                    }
                }
            }
        }
    }
}

private suspend fun handleIncomingToolCall(data: ByteArray) {
    val message = Json.decodeFromString<ToolCallMessage>(
        String(data, Charsets.UTF_8)
    )

    val result = DeviceCommandHandler.handleToolCall(message)

    // Send result back
    sendToolResult(result)
}
```

#### 1.6 Update AndroidManifest.xml
Add package visibility declarations for all controllable apps:

```xml
<queries>
    <package android:name="com.spotify.music"/>
    <package android:name="com.google.android.apps.maps"/>
    <package android:name="com.google.android.gm"/>
    <package android:name="com.android.chrome"/>
    <!-- Add more as needed -->
</queries>
```

Add Bluetooth permissions for Android 12+:
```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
```

---

### Phase 2: Agent Server Integration (2-3 days)

#### 2.1 Create Device Control Tools
**File:** `stone-agent-server/src/tools/device-tools.ts`

```typescript
export const deviceTools = [
  {
    name: 'open_app',
    description: 'Open an Android app by package name',
    parameters: {
      type: 'object',
      properties: {
        package_name: {
          type: 'string',
          description: 'Android package name (e.g., com.spotify.music)'
        }
      },
      required: ['package_name']
    },
    execute: async (params: any) => {
      return await sendToolCallToDevice('open_app', params)
    }
  },
  {
    name: 'set_wifi',
    description: 'Enable or disable WiFi',
    parameters: {
      type: 'object',
      properties: {
        enabled: {
          type: 'boolean',
          description: 'true to enable, false to disable'
        }
      },
      required: ['enabled']
    },
    execute: async (params: any) => {
      return await sendToolCallToDevice('set_wifi', params)
    }
  },
  // Add remaining tools...
]
```

#### 2.2 Create Communication Bridge
**File:** `stone-agent-server/src/tools/android-bridge.ts`

```typescript
import { Room } from 'livekit-server-sdk'

class AndroidBridge {
  private pendingRequests = new Map<string, {
    resolve: (value: any) => void,
    reject: (reason: any) => void,
    timeout: NodeJS.Timeout
  }>()

  async sendToolCall(method: string, params: any): Promise<any> {
    const message: ToolCallMessage = {
      type: 'tool_call',
      id: generateUniqueId(),
      method,
      params,
      timestamp: Date.now()
    }

    // Send via LiveKit data channel
    await this.room.localParticipant.publishData(
      new TextEncoder().encode(JSON.stringify(message)),
      { topic: 'stone.tool_call', reliable: true }
    )

    // Wait for result with timeout
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        this.pendingRequests.delete(message.id)
        reject(new Error('Tool call timeout'))
      }, 30000) // 30 second timeout

      this.pendingRequests.set(message.id, { resolve, reject, timeout })
    })
  }

  handleToolResult(result: ToolResultMessage) {
    const pending = this.pendingRequests.get(result.id)
    if (!pending) return

    clearTimeout(pending.timeout)
    this.pendingRequests.delete(result.id)

    if (result.success) {
      pending.resolve(result.result)
    } else {
      pending.reject(new Error(result.error?.message))
    }
  }

  handleStatusUpdate(status: StatusUpdateMessage) {
    // Broadcast to agent for display in chat
    console.log(`[${status.operation_id}] ${status.message}`)

    // Could update UI here if needed
  }
}
```

#### 2.3 Update Agent Tool Loading
**Modify:** `stone-agent-server/src/tools.ts`

```typescript
import { deviceTools } from './tools/device-tools'

export class DynamicToolLoader {
  // ...existing code...

  private getBaseTools(): any[] {
    return [
      ...deviceTools,  // Add device control tools
      {
        name: 'get_current_time',
        description: 'Get the current time',
        parameters: {},
        execute: async () => new Date().toLocaleString()
      },
      // ...other base tools
    ]
  }
}
```

#### 2.4 Setup LiveKit Event Listeners
**Modify:** `stone-agent-server/src/agent.ts`

```typescript
export default defineAgent({
  entry: async (ctx: JobContext) => {
    await ctx.connect()

    const room = ctx.room
    const androidBridge = new AndroidBridge(room)

    // Listen for tool results from Android
    room.on('dataReceived', async (data: Uint8Array, participant: RemoteParticipant, topic: string) => {
      const message = JSON.parse(new TextDecoder().decode(data))

      if (message.type === 'tool_result') {
        androidBridge.handleToolResult(message)
      } else if (message.type === 'status_update') {
        androidBridge.handleStatusUpdate(message)
      }
    })

    // Initialize agent
    const agent = new StoneAgent(room, participant)

    // ... rest of agent setup
  }
})
```

---

### Phase 3: Testing & Refinement (2-3 days)

#### 3.1 Unit Tests
Test each controller independently:
- AppController opens apps correctly
- WiFi controller toggles WiFi
- Volume controller adjusts volume
- Validation catches invalid inputs
- Rate limiter blocks excessive requests

#### 3.2 Integration Tests
Test full flow:
1. Agent sends tool call
2. Android receives and validates
3. Android executes operation
4. Android sends status updates
5. Android sends result
6. Agent receives result

#### 3.3 Security Testing
- Verify dangerous operations require user consent
- Verify blocked operations are rejected
- Verify rate limiting works
- Verify audit logging captures all operations

#### 3.4 Permission Testing
- Test runtime permission requests
- Test special permission flows (WRITE_SETTINGS, DND)
- Verify graceful degradation when permissions denied

---

### Phase 4: Documentation & Handoff (1 day)

#### 4.1 Code Documentation
- Add KDoc/JSDoc comments to all public methods
- Document message formats
- Document error codes

#### 4.2 Create Testing Guide
Document how to test each tool:
```bash
# Example: Test opening Spotify
# Send message to agent: "Open Spotify"
# Expected: Spotify app opens on device
# Verify: Status updates appear in chat
```

#### 4.3 Create Troubleshooting Guide
Common issues and solutions:
- Permission denied errors
- Rate limit exceeded
- App not installed
- LiveKit connection issues

---

### Estimated Timeline

**Total: 8-11 days**
- Phase 1 (Android): 3-4 days
- Phase 2 (Agent): 2-3 days
- Phase 3 (Testing): 2-3 days
- Phase 4 (Docs): 1 day

---

### Dependencies

**Must be completed first:**
- TICKET_005: LiveKit Android SDK Integration
- TICKET_006: Agent Server Setup

**Gradle Dependencies to Add:**
```gradle
// For Spotify SDK (optional, can defer)
implementation 'com.spotify.android:app-remote:0.8.0'
implementation 'com.spotify.android:auth:2.1.0'

// For JSON serialization
implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0'
```

**NPM Dependencies to Add:**
```json
{
  "dependencies": {
    "uuid": "^9.0.0"  // For generating unique request IDs
  }
}
```

---

### Testing Checklist

After implementation, verify each capability:

**Device Control:**
- [ ] Open any of the 12 Stone apps
- [ ] Toggle WiFi on/off
- [ ] Toggle Bluetooth (request enable)
- [ ] Adjust screen brightness
- [ ] Adjust volume (media, ringer, alarm)
- [ ] Enable/disable Do Not Disturb

**Communication:**
- [ ] Make phone call (with user consent)
- [ ] Send SMS (with user consent)
- [ ] Open dialer with pre-filled number

**Navigation:**
- [ ] Navigate to address
- [ ] Navigate to coordinates
- [ ] Show location on map

**Media:**
- [ ] Open Spotify (defer SDK integration if needed)

**Security:**
- [ ] Dangerous operations require user consent
- [ ] Blocked operations are rejected
- [ ] Rate limiting prevents abuse
- [ ] Audit log captures all operations
- [ ] Validation rejects malformed requests

**LiveKit Integration:**
- [ ] Tool calls sent via data channel
- [ ] Results received via data channel
- [ ] Status updates appear in real-time
- [ ] Multiple concurrent tool calls handled
- [ ] Timeout handling works

---

### Success Criteria

This ticket is complete when:

1. All 10+ device control capabilities working
2. Bidirectional LiveKit communication established
3. Security layer (validation, rate limiting, consent) functional
4. All tests passing
5. Documentation complete
6. User can interact with Android device via AI voice/chat commands

---

## Tool Categories to Implement

### 1. App Control
- Open any of the 12 Stone apps
- Launch third-party apps
- Switch between apps
- Close apps

### 2. Device Settings
- WiFi on/off
- Bluetooth on/off
- Screen brightness
- Volume control
- Do Not Disturb

### 3. Communication
- Make phone calls
- Send SMS
- Open contacts

### 4. Navigation
- Open maps with destination
- Get directions
- Search nearby places

### 5. Media
- Play music on Spotify
- Control playback
- Search for content

### 6. Information
- Web search
- Set reminders
- Check calendar

---

## Files to Create/Modify

### Android Side
```
android/app/src/main/java/com/stonelauncher/tools/
├── DeviceCommandHandler.kt (NEW)
├── AppController.kt (NEW)
├── SettingsController.kt (NEW)
├── NavigationController.kt (NEW)
├── MediaController.kt (NEW)
└── PermissionManager.kt (NEW)
```

### Agent Side
```
stone-agent-server/src/tools/
├── app-tools.ts (MODIFY)
├── device-tools.ts (MODIFY)
├── navigation-tools.ts (NEW)
├── media-tools.ts (NEW)
└── communication-tools.ts (NEW)
```

---

## Testing Criteria

- [ ] Agent can open Stone apps via tool calls
- [ ] Device settings can be controlled
- [ ] Navigation commands work
- [ ] Media playback functions
- [ ] Status updates appear in chat
- [ ] Errors handled gracefully
- [ ] Permissions requested appropriately

---

## Acceptance Criteria

- [ ] All 12 Stone apps controllable via AI
- [ ] Core device settings accessible
- [ ] Navigation integration working
- [ ] Media control functional
- [ ] Bidirectional communication established
- [ ] Operation status visible in UI
- [ ] Security and permissions handled

---

## Notes

- Follow the "Head & Headless" pattern - all controls work via touch AND AI
- Use Android's accessibility APIs where appropriate
- Consider security implications of device control
- Implement rate limiting for safety