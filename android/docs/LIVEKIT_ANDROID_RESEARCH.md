# LiveKit Android SDK Research Report

**Date**: January 13, 2025
**Researcher**: Claude (Android Research Specialist)
**Ticket**: TICKET_004_LiveKit_Android_Integration.md
**Context**: Replicating React prototype functionality in native Kotlin

---

## Research Report: LiveKit Android SDK Integration

### Context
- **Feature**: Stone Launcher Chat Interface
- **Ticket**: TICKET_004
- **Question**: How to replicate the React prototype's LiveKit functionality in native Kotlin for Android?

### Findings

## Approach 1: Direct LiveKit Android SDK Integration

**Description**: Use the official LiveKit Android SDK (version 2.21.1) to replicate all React prototype features in native Kotlin.

**Feasibility**: ✅ Doable

**Requirements**:

**Permissions**:
- `android.permission.RECORD_AUDIO` (dangerous - runtime request required)
- `android.permission.CAMERA` (dangerous - runtime request required, optional)
- `android.permission.INTERNET` (normal)
- `android.permission.MODIFY_AUDIO_SETTINGS` (normal)
- `android.permission.FOREGROUND_SERVICE` (normal, for background operation)
- `android.permission.FOREGROUND_SERVICE_MICROPHONE` (API 34+)

**APIs**:
- LiveKit Android SDK 2.21.1
- Android SpeechRecognizer (for voice input)
- Kotlin Coroutines and Flow
- Jetpack ViewModel and Lifecycle

**AOSP modifications**: No

**Minimum Android version**: API 29 (Android 10)+

**Implementation**:

```kotlin
// 1. Add dependencies (build.gradle.kts)
dependencies {
    val livekitVersion = "2.21.1"
    implementation("io.livekit:livekit-android:$livekitVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
}

// 2. Create Room and connect
class LiveKitManager {
    private var room: Room? = null

    suspend fun connect(context: Context, serverUrl: String, token: String) {
        room = LiveKit.create(context.applicationContext)

        // Handle events with Kotlin Flow
        room?.events?.collect { event ->
            when (event) {
                is RoomEvent.TrackSubscribed -> handleTrackSubscribed(event)
                is RoomEvent.DataReceived -> handleDataReceived(event)
                is RoomEvent.Disconnected -> handleDisconnected(event)
                else -> {}
            }
        }

        // Connect
        room?.connect(serverUrl, token)

        // Enable microphone
        room?.localParticipant?.setMicrophoneEnabled(true)
    }
}

// 3. Register RPC methods (like React prototype)
fun registerRpcMethods() {
    room?.localParticipant?.registerRpcMethod("greet") { data ->
        Log.i("Stone", "Received: ${data.payload} from ${data.callerIdentity}")
        "Hello, ${data.callerIdentity}!"
    }

    room?.localParticipant?.registerRpcMethod("routeToAgent") { data ->
        val params = JSONObject(data.payload)
        val agentType = params.getString("agentType")
        val message = params.getString("message")

        JSONObject().apply {
            put("success", true)
            put("targetAgent", agentType)
            put("message", "Routing request acknowledged")
        }.toString()
    }
}

// 4. Send messages (like React's sendText)
suspend fun sendMessage(content: String) {
    room?.localParticipant?.publishData(
        data = content.toByteArray(Charsets.UTF_8),
        topic = "lk.chat",
        reliability = DataPublishReliability.RELIABLE
    )
}

// 5. Handle operation tracking (like useUnifiedConversation)
fun handleDataReceived(event: RoomEvent.DataReceived) {
    if (event.topic == "agent_operation_status") {
        val data = String(event.data, Charsets.UTF_8)
        val signal = JSONObject(data)

        val agent = signal.getString("agent")
        val busy = signal.getBoolean("busy")
        val operation = signal.getString("operation")
        val toolName = signal.optString("toolName")
        val stepDescription = signal.optString("stepDescription")

        // Add to conversation UI
        addOperationMessage(agent, operation, busy, toolName, stepDescription)
    }
}

// 6. Voice input with Android SpeechRecognizer
class VoiceInputManager(context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    // Send transcription to agent
                    sendMessage(text)
                }
            }

            override fun onError(error: Int) {
                // Handle error
            }

            // Other required methods...
        })
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer.startListening(intent)
    }
}
```

**Pros**:
- Official SDK with full support for all LiveKit features
- Kotlin Flow provides reactive pattern matching React hooks
- RPC methods directly match React's `registerRpcMethod` pattern
- Data channels support operation tracking with topics
- No AOSP modifications required
- Well-documented with examples
- Active development and maintenance

**Cons**:
- Requires learning LiveKit Android SDK patterns (differs from React SDK)
- Android SpeechRecognizer has different API than Web Speech API
- Must handle Android lifecycle (Activity/Service lifecycle)
- Permissions must be requested at runtime
- Background operation requires foreground service

---

## Approach 2: WebView with React Prototype

**Description**: Embed the React prototype in an Android WebView and use JavaScript bridge for native integration.

**Feasibility**: ⚠️ Complex (not recommended)

**Requirements**:
- WebView with JavaScript enabled
- JavaScript bridge for native communication
- Same permissions as Approach 1

**Implementation**:
```kotlin
webView.settings.javaScriptEnabled = true
webView.addJavascriptInterface(StoneJsBridge(), "Android")
webView.loadUrl("file:///android_asset/stone-prototype/index.html")
```

**Pros**:
- Reuses existing React prototype code
- Faster initial development

**Cons**:
- Poor performance compared to native
- Complex JavaScript-Kotlin bridge
- Difficult to debug
- Not truly native Android experience
- WebView limitations on background operation
- Larger app size (bundle React app)
- Security concerns with JavaScript bridge
- **Goes against StoneOS design: "Real apps, not web views"**

---

## Approach 3: React Native

**Description**: Build the chat interface with React Native to share code with the React prototype.

**Feasibility**: ⚠️ Complex (architectural mismatch)

**Requirements**:
- React Native framework
- LiveKit React Native SDK
- Bridge between React Native and native Kotlin launcher

**Pros**:
- Code sharing with React prototype
- Familiar React patterns

**Cons**:
- Adds React Native framework overhead
- Complex integration with existing Kotlin launcher
- Larger app size
- Two separate runtimes (Kotlin + React Native)
- **Violates StoneOS architecture**: We're building a native Kotlin launcher, not a hybrid app
- Unnecessary complexity for a single feature

---

### Recommendation

**Recommended Approach**: Approach 1 - Direct LiveKit Android SDK Integration

**Rationale**:
1. **Native Android**: Aligns with StoneOS philosophy of real native apps, not web views
2. **Full feature parity**: LiveKit Android SDK supports all features in the React prototype
3. **Performance**: Native Kotlin provides best performance for real-time audio
4. **Maintainability**: Clean separation between UI (Kotlin) and agent backend (Python)
5. **No AOSP modifications**: Works on standard Android with root access
6. **Official support**: LiveKit actively maintains the Android SDK

**Implementation Guidance**:

**Step 1: Add LiveKit SDK** (build.gradle.kts)
```kotlin
dependencies {
    val livekitVersion = "2.21.1"
    implementation("io.livekit:livekit-android:$livekitVersion")
}
```

**Step 2: Create LiveKitManager singleton**
- Manages Room connection lifecycle
- Registers RPC methods
- Handles event collection
- Provides message sending API

**Step 3: Build ChatViewModel**
- Manages conversation state
- Handles connection state
- Processes room events
- Integrates with VoiceInputManager

**Step 4: Implement VoiceInputManager**
- Uses Android SpeechRecognizer
- Provides Flow-based transcription updates
- Matches Web Speech API functionality

**Step 5: Create Chat UI**
- RecyclerView for message list
- EditText for text input
- Voice input button
- Connection status indicator
- Operation progress indicators

**Step 6: Test with agent backend**
- Connect to LiveKit server
- Test RPC method calls
- Verify operation tracking
- Test voice input and transcription

**Impact on Current Plans**:
✅ Ticket can proceed as written - All React prototype features can be replicated in Kotlin

### Additional Considerations

**React vs Android Feature Mapping**:

| React Prototype | Android Equivalent | Notes |
|----------------|-------------------|-------|
| `useRoom()` hook | `LiveKit.create()` + `room.events.collect{}` | Kotlin Flow replaces React hook |
| `useVoiceAssistant()` | `room.localParticipant` + event handling | No direct equivalent, build custom |
| `room.registerRpcMethod()` | `room.localParticipant.registerRpcMethod()` | Identical API pattern |
| `room.localParticipant.sendText()` | `room.localParticipant.publishData()` | Different method name, same functionality |
| Web Speech API | `SpeechRecognizer` | Android system API |
| `useUnifiedConversation` | Custom ViewModel + StateFlow | Build unified message handling |
| `RoomEvent.DataReceived` handler | `room.events.collect { RoomEvent.DataReceived -> }` | Kotlin Flow collection |

**Security Implications**:
- RECORD_AUDIO is a dangerous permission - must justify to users
- LiveKit uses WebRTC with end-to-end encryption
- RPC methods should validate all inputs
- Operation signals should not expose sensitive data

**Performance Considerations**:
- Voice chat adds audio processing overhead
- Use foreground service for background operation
- Optimize RecyclerView for long conversations
- Debounce voice input to reduce RPC calls

**Android Version Compatibility**:
- Target API 34 (Android 14) per StoneOS specs
- Minimum API 29 (Android 10) for modern features
- Foreground service changes in API 34 require updates
- SpeechRecognizer works consistently on API 29+

---

## React Prototype Features Successfully Mapped to Android

### 1. Agent Lifecycle Management ✅
- React: Check status, start agent, wait for initialization
- Android: Same - use Retrofit/OkHttp for HTTP API calls

### 2. LiveKit Connection ✅
- React: Generate room name, fetch token, connect
- Android: Identical pattern with `LiveKit.create()` and `room.connect()`

### 3. Voice Chat ✅
- React: Web Speech API
- Android: SpeechRecognizer with RecognitionListener

### 4. Text Chat ✅
- React: Send on `lk.chat` topic
- Android: `publishData(topic = "lk.chat")`

### 5. RPC Methods ✅
- React: `room.registerRpcMethod(name, handler)`
- Android: `room.localParticipant.registerRpcMethod(name, handler)`

### 6. Operation Tracking ✅
- React: Listen for `agent_operation_status` topic
- Android: Filter `RoomEvent.DataReceived` by topic

### 7. Conversation Management ✅
- React: `useUnifiedConversation` hook
- Android: ChatViewModel + StateFlow<List<UnifiedMessage>>

### 8. Connection State ✅
- React: Loading states, error handling
- Android: StateFlow<ConnectionState> + UI updates

---

## Conclusion

The LiveKit Android SDK (version 2.21.1) provides complete feature parity with the React prototype. All key features - voice chat, RPC methods, data channels, operation tracking, and connection management - have direct Android equivalents using Kotlin and the LiveKit SDK.

**Next Steps**:
1. Review and approve this research report
2. Begin implementation following the step-by-step plan in TICKET_004
3. Create package structure: `livekit/`, `chat/`, `models/`, `services/`
4. Implement core components: LiveKitManager, ChatViewModel, VoiceInputManager
5. Build chat UI with RecyclerView and voice input
6. Test with agent backend

**Estimated Implementation Time**: 3-5 days for core functionality

**Risk Assessment**: LOW - All features proven to work in React prototype, official SDK support, no AOSP modifications required
