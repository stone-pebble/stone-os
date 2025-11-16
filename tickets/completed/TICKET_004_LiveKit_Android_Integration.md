# Ticket #004: LiveKit Android SDK Integration

**Status**: RESEARCH COMPLETE - Ready for Implementation
**Priority**: HIGH
**Dependencies**: TICKET_002 (Launcher UI), TICKET_003 (Chat UI)

**Research Summary**:
- LiveKit Android SDK 2.21.1 fully supports all React prototype features
- RPC methods enable bidirectional tool calling (matching React's `registerRpcMethod` pattern)
- Data channels support operation tracking via `publishData` with topics
- Kotlin Flow provides reactive event handling (equivalent to React hooks)
- Android SpeechRecognizer replaces Web Speech API
- Complete implementation plan provided with code examples

---

## Objective

Research and integrate LiveKit Android SDK into the Stone Launcher chat interface. This connects the chat UI to real-time voice/video communication with AI agents.

---

## Background

LiveKit provides real-time communication infrastructure. We need to:
1. Research the Android SDK implementation patterns
2. Integrate it with our Kotlin chat interface
3. Set up voice chat and data channels for tool calling

---

## Requirements

### Research Tasks
- [ ] LiveKit Android SDK setup and dependencies
- [ ] Room connection from Kotlin
- [ ] Audio track management
- [ ] Data channels for RPC/tool calling
- [ ] Connection lifecycle in Android

### Implementation
- [ ] Add LiveKit SDK to Gradle
- [ ] Connect to LiveKit room
- [ ] Handle audio input/output
- [ ] Implement data channels for tool calls
- [ ] Connection state management
- [ ] Error handling and reconnection

---

## Research Findings

### LiveKit Android SDK Documentation

**SDK Version**: 2.21.1 (Latest as of January 2025)

**Gradle Dependencies**:
```kotlin
dependencies {
    val livekitVersion = "2.21.1"
    implementation("io.livekit:livekit-android:$livekitVersion")
    // Optional: CameraX support with pinch to zoom, torch control, etc.
    implementation("io.livekit:livekit-android-camerax:$livekitVersion")
    // Optional: Track processors (virtual background, etc.)
    implementation("io.livekit:livekit-android-track-processors:$livekitVersion")
}
```

**Repository Configuration** (in settings.gradle.kts):
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // For SNAPSHOT access (optional):
        // maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    }
}
```

**Key Classes**:
- `Room`: Main entry point for LiveKit connections
- `LocalParticipant`: Represents the local user (microphone, camera control, RPC methods)
- `RemoteParticipant`: Represents remote participants
- `RoomEvent`: Sealed class hierarchy for all room events
- `VideoTrack`, `AudioTrack`: Media track abstractions
- `RpcInvocationData`: Data structure for RPC calls
- `LiveKit.create()`: Factory method to create Room instances

**Connection Patterns**:
```kotlin
// 1. Create Room
val room = LiveKit.create(applicationContext)

// 2. Handle events using Kotlin Flow
lifecycleScope.launch {
    launch {
        room.events.collect { event ->
            when (event) {
                is RoomEvent.TrackSubscribed -> handleTrackSubscribed(event)
                is RoomEvent.ParticipantConnected -> handleParticipantConnected(event)
                is RoomEvent.DataReceived -> handleDataReceived(event)
                is RoomEvent.Disconnected -> handleDisconnected(event)
                else -> {}
            }
        }
    }

    // 3. Connect to LiveKit server
    room.connect(url = "wss://your-server", token = "your-token")

    // 4. Enable microphone/camera
    room.localParticipant.setMicrophoneEnabled(true)
    room.localParticipant.setCameraEnabled(true)
}
```

### Audio Management

**Permissions Needed** (AndroidManifest.xml):
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<!-- For background operation (optional): -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

**Runtime Permissions** (must request at runtime for API 23+):
```kotlin
// In Activity
val audioPermissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        room.localParticipant.setMicrophoneEnabled(true)
    }
}

// Request permission
audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
```

**Audio Track Setup**:
```kotlin
// Default: microphone automatically managed
room.localParticipant.setMicrophoneEnabled(true)

// Custom audio configuration:
val room = LiveKit.create(
    appContext = application,
    overrides = LiveKitOverrides(
        audioOptions = AudioOptions(
            audioOutputType = AudioType.MediaAudioType() // Optimized for media playback
        )
    )
)
```

**Speaker/Microphone Control**:
```kotlin
// Enable/disable microphone
room.localParticipant.setMicrophoneEnabled(true)

// Observe microphone state using Flow
lifecycleScope.launch {
    room.localParticipant::isMicrophoneEnabled.flow.collectLatest { enabled ->
        // Update UI based on microphone state
    }
}
```

### Data Channels

**RPC Implementation**:

LiveKit Android SDK supports Remote Procedure Calls (RPC) for bidirectional communication between participants.

**1. Register RPC Method** (Receiving side):
```kotlin
// Register a method that can be called by other participants
room.localParticipant.registerRpcMethod("greet") { data: RpcInvocationData ->
    Log.i("TAG", "Received greeting from ${data.callerIdentity}: ${data.payload}")

    // Access RpcInvocationData fields:
    // - data.requestId: Unique identifier for this request
    // - data.callerIdentity: Identity of the caller
    // - data.payload: String payload from caller
    // - data.responseTimeout: Max time to respond

    // Return a string response
    "Hello, ${data.callerIdentity}!"
}
```

**2. Call RPC Method** (Calling side):
```kotlin
// Call an RPC method on a remote participant
try {
    val response = room.localParticipant.performRpc(
        destinationIdentity = "remote-participant-identity",
        method = "greet",
        payload = "Hello from Android!"
    )
    Log.i("TAG", "RPC response: $response")
} catch (e: Exception) {
    Log.e("TAG", "RPC call failed: ${e.message}")
}
```

**3. Error Handling**:
```kotlin
// Throw RpcError for structured error responses
room.localParticipant.registerRpcMethod("validateData") { data ->
    if (data.payload.isEmpty()) {
        throw RpcError(
            code = 1400,
            message = "Payload cannot be empty"
        )
    }
    "Valid data received"
}
```

**RPC Constraints**:
- Method names: Up to 64 bytes (UTF-8)
- Payload size: Maximum 15 KiB (UTF-8) for requests and responses
- Default timeout: 10 seconds (configurable per call)
- Error codes: 1400-1505 reserved for built-in errors

**Tool Calling Pattern** (Matching React prototype):

The React prototype uses RPC methods like `routeToAgent`, `sendBusySignal`, `getAvailableAgents`, and `healthCheck`. Here's the Android equivalent:

```kotlin
// Register tool calling methods (like React's createRpcMethods)
fun registerToolMethods(room: Room) {
    // Route to agent
    room.localParticipant.registerRpcMethod("routeToAgent") { data ->
        try {
            val params = JSONObject(data.payload)
            val agentType = params.getString("agentType")
            val message = params.getString("message")

            Log.i("Stone", "Routing to $agentType with message: $message")

            // Return JSON response
            JSONObject().apply {
                put("success", true)
                put("routed", true)
                put("targetAgent", agentType)
                put("message", "Routing request acknowledged for $agentType")
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("success", false)
                put("error", e.message)
            }.toString()
        }
    }

    // Get available agents
    room.localParticipant.registerRpcMethod("getAvailableAgents") { _ ->
        val agents = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "think")
                put("description", "Note-taking and file management")
            })
            put(JSONObject().apply {
                put("name", "go")
                put("description", "Navigation and location services")
            })
            put(JSONObject().apply {
                put("name", "router")
                put("description", "Agent routing and coordination")
            })
        }

        JSONObject().apply {
            put("success", true)
            put("agents", agents)
        }.toString()
    }

    // Health check
    room.localParticipant.registerRpcMethod("healthCheck") { _ ->
        JSONObject().apply {
            put("success", true)
            put("status", "healthy")
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }
}
```

**Message Format** (Data Packets for Operation Tracking):

The React prototype uses `room.on(RoomEvent.DataReceived, ...)` to receive operation status updates. Android equivalent:

```kotlin
lifecycleScope.launch {
    room.events.collect { event ->
        when (event) {
            is RoomEvent.DataReceived -> {
                // Check topic
                if (event.topic == "agent_operation_status" || event.topic == "agent_busy_signal") {
                    try {
                        val data = String(event.data, Charsets.UTF_8)
                        val signal = JSONObject(data)

                        // Extract operation signal fields
                        val type = signal.getString("type")
                        val agent = signal.getString("agent")
                        val busy = signal.getBoolean("busy")
                        val operation = signal.getString("operation")
                        val details = signal.optString("details", "")
                        val toolName = signal.optString("toolName")
                        val stepDescription = signal.optString("stepDescription")
                        val operationId = signal.optString("operationId")

                        // Add to conversation UI
                        addOperationMessage(agent, operation, busy, details)
                    } catch (e: Exception) {
                        Log.e("Stone", "Failed to parse operation signal", e)
                    }
                }
            }
            else -> {}
        }
    }
}
```

**Sending Data Packets**:
```kotlin
// Send text message on lk.chat topic (for chat with agent)
room.localParticipant.publishData(
    data = "Hello, agent!".toByteArray(Charsets.UTF_8),
    topic = "lk.chat",
    reliability = DataPublishReliability.RELIABLE
)

// Send operation status
val statusData = JSONObject().apply {
    put("type", "operation_status")
    put("agent", "stone")
    put("busy", true)
    put("operation", "processing")
    put("details", "Processing your request...")
}.toString()

room.localParticipant.publishData(
    data = statusData.toByteArray(Charsets.UTF_8),
    topic = "agent_operation_status",
    reliability = DataPublishReliability.RELIABLE
)
```

### Android Considerations

**Background Handling**:
```kotlin
// Use a foreground service for background audio
class LiveKitService : Service() {
    private lateinit var room: Room

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Create notification for foreground service
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stone Voice Chat")
            .setContentText("Connected to Stone Assistant")
            .setSmallIcon(R.drawable.ic_stone)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Initialize LiveKit
        room = LiveKit.create(applicationContext)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
```

**Battery Optimization**:
```kotlin
// Request battery optimization exemption (optional, for background operation)
val intent = Intent().apply {
    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    data = Uri.parse("package:$packageName")
}
startActivity(intent)
```

**ProGuard Rules** (proguard-rules.pro):
```
# LiveKit SDK
-keep class io.livekit.android.** { *; }
-keep interface io.livekit.android.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
```

**AndroidManifest.xml Configuration**:
```xml
<manifest>
    <!-- Permissions -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />

    <application>
        <!-- Foreground service for background audio -->
        <service
            android:name=".livekit.LiveKitService"
            android:foregroundServiceType="microphone"
            android:exported="false" />
    </application>
</manifest>
```

---

## Implementation Plan

### Phase 1: React Prototype Feature Analysis

**React Prototype Key Features** (from Stone.tsx and AgentChatInterface.tsx):

1. **Agent Lifecycle Management**:
   - Check if agent is running (`/api/router-agent/status`)
   - Start agent if not running (`/api/router-agent/start`)
   - Wait for initialization (3s for new, 1s for existing)
   - Auto-connect on component mount

2. **LiveKit Connection**:
   - Generate unique room name: `{agentType}-{timestamp}-{processId}-{random}`
   - Generate unique participant ID: `{agentType}_user_{timestamp}_{random}`
   - Fetch token from connection endpoint with participantId and roomName
   - Connect to room with token
   - Enable microphone automatically

3. **Voice Chat**:
   - Web Speech API for voice input (Android equivalent: SpeechRecognizer)
   - Voice interruption: Stop agent when user starts speaking
   - Auto-expand chat when agent speaks
   - Visual recording indicator (pulsing animation)

4. **Text Chat**:
   - Send text messages on `lk.chat` topic
   - Display user messages immediately
   - Show agent transcriptions in real-time
   - Interrupt agent if speaking when user types

5. **RPC Methods** (from Stone.tsx):
   - `routeToAgent`: Route requests to different agents
   - `sendBusySignal`: Signal operation status
   - `getAvailableAgents`: List available agents
   - `healthCheck`: Check agent health

6. **Operation Tracking** (from useUnifiedConversation.ts):
   - Listen for `agent_operation_status` and `agent_busy_signal` topics
   - Parse operation signals with fields: type, agent, busy, operation, details, toolName, stepDescription, progressPercentage, operationId
   - Display operation messages in chat UI
   - Show progress bars for operations with percentages
   - Deduplicate messages by operationId

7. **Conversation Management**:
   - Unified message interface: chat, transcription, operation, status
   - Message types: user, assistant, system
   - Auto-scroll to latest message
   - Deduplicate messages within 5 seconds

8. **Connection State**:
   - Show loading state while connecting
   - Display connection errors
   - Retry connection on failure
   - Handle disconnection gracefully

### Phase 2: Android Architecture Design

**Package Structure**:
```
com.stone.launcher/
├── livekit/
│   ├── LiveKitManager.kt         # Singleton managing LiveKit connection
│   ├── RoomConnection.kt         # Room connection lifecycle
│   ├── AudioManager.kt           # Microphone and speaker control
│   ├── RpcHandler.kt             # RPC method registration and handling
│   ├── MessageHandler.kt         # Data packet processing
│   └── VoiceInputManager.kt      # Android SpeechRecognizer integration
├── chat/
│   ├── ChatActivity.kt           # Main chat UI (integrates LiveKit)
│   ├── ChatViewModel.kt          # ViewModel for chat state
│   ├── MessageAdapter.kt         # RecyclerView adapter for messages
│   └── ChatRepository.kt         # Data layer for chat
├── models/
│   ├── UnifiedMessage.kt         # Message data class
│   ├── OperationSignal.kt        # Operation status data class
│   └── AgentConfig.kt            # Agent configuration
└── services/
    └── LiveKitService.kt         # Foreground service for background audio
```

**Key Components**:

1. **LiveKitManager.kt** (Singleton):
```kotlin
object LiveKitManager {
    private var room: Room? = null
    private var connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    suspend fun connect(context: Context, serverUrl: String, token: String): Room
    suspend fun disconnect()
    fun registerRpcMethods(methods: Map<String, RpcHandler>)
    fun sendMessage(message: String, topic: String = "lk.chat")
    fun observeEvents(): Flow<RoomEvent>
}
```

2. **ChatViewModel.kt**:
```kotlin
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<UnifiedMessage>>(emptyList())
    val messages: StateFlow<List<UnifiedMessage>> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun connect(agentType: String)
    fun sendMessage(content: String)
    fun startVoiceInput()
    fun stopVoiceInput()
    fun handleRoomEvent(event: RoomEvent)
}
```

3. **VoiceInputManager.kt**:
```kotlin
class VoiceInputManager(private val context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val _transcription = MutableStateFlow<String>("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    fun startListening()
    fun stopListening()
    fun isListening(): Boolean
}
```

### Phase 3: Step-by-Step Implementation

**Step 1: Add Dependencies** (build.gradle.kts):
```kotlin
dependencies {
    // LiveKit
    val livekitVersion = "2.21.1"
    implementation("io.livekit:livekit-android:$livekitVersion")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Jetpack
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // JSON parsing
    implementation("org.json:json:20231013")
}
```

**Step 2: Request Permissions**:
```kotlin
class ChatActivity : AppCompatActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            viewModel.connect(agentType = "router")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionLauncher.launch(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET
        ))
    }
}
```

**Step 3: Implement LiveKitManager**:
```kotlin
object LiveKitManager {
    private var room: Room? = null
    private val _events = MutableSharedFlow<RoomEvent>()
    val events: SharedFlow<RoomEvent> = _events.asSharedFlow()

    suspend fun connect(
        context: Context,
        serverUrl: String,
        token: String
    ): Room = withContext(Dispatchers.IO) {
        val newRoom = LiveKit.create(context.applicationContext)

        // Collect room events
        newRoom.events.collect { event ->
            _events.emit(event)
        }

        // Connect
        newRoom.connect(serverUrl, token)

        // Enable microphone
        newRoom.localParticipant.setMicrophoneEnabled(true)

        room = newRoom
        newRoom
    }

    fun registerRpcMethod(method: String, handler: suspend (RpcInvocationData) -> String) {
        room?.localParticipant?.registerRpcMethod(method) { data ->
            handler(data)
        }
    }

    suspend fun sendMessage(message: String, topic: String = "lk.chat") {
        room?.localParticipant?.publishData(
            data = message.toByteArray(Charsets.UTF_8),
            topic = topic,
            reliability = DataPublishReliability.RELIABLE
        )
    }
}
```

**Step 4: Implement ChatViewModel**:
```kotlin
class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<UnifiedMessage>>(emptyList())
    val messages: StateFlow<List<UnifiedMessage>> = _messages.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    fun connect(context: Context, agentType: String) {
        viewModelScope.launch {
            _isConnecting.value = true

            try {
                // 1. Generate unique room and participant names
                val timestamp = System.currentTimeMillis()
                val random = UUID.randomUUID().toString().take(8)
                val roomName = "${agentType}-${timestamp}-${random}"
                val participantId = "${agentType}_user_${timestamp}_${random}"

                // 2. Fetch token from backend
                val response = fetchConnectionDetails(participantId, roomName)

                // 3. Connect to LiveKit
                LiveKitManager.connect(
                    context = context,
                    serverUrl = response.serverUrl,
                    token = response.participantToken
                )

                // 4. Register RPC methods
                registerRpcMethods()

                // 5. Observe events
                LiveKitManager.events.collect { event ->
                    handleRoomEvent(event)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Connection failed", e)
            } finally {
                _isConnecting.value = false
            }
        }
    }

    private fun registerRpcMethods() {
        LiveKitManager.registerRpcMethod("greet") { data ->
            JSONObject().apply {
                put("success", true)
                put("message", "Hello, ${data.callerIdentity}!")
            }.toString()
        }
    }

    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.DataReceived -> {
                if (event.topic == "agent_operation_status") {
                    val data = String(event.data, Charsets.UTF_8)
                    val signal = JSONObject(data)
                    addOperationMessage(signal)
                }
            }
            else -> {}
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            // Add to UI immediately
            _messages.value = _messages.value + UnifiedMessage(
                id = UUID.randomUUID().toString(),
                content = content,
                role = "user",
                type = "chat",
                timestamp = System.currentTimeMillis(),
                source = "manual"
            )

            // Send to agent
            LiveKitManager.sendMessage(content, "lk.chat")
        }
    }
}
```

**Step 5: Implement Voice Input**:
```kotlin
class VoiceInputManager(private val context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcription = MutableStateFlow("")
    val transcription: StateFlow<String> = _transcription.asStateFlow()

    init {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    _transcription.value = text
                }
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
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

    fun stopListening() {
        speechRecognizer.stopListening()
    }
}
```

**Step 6: Build Chat UI** (ChatActivity.kt):
```kotlin
class ChatActivity : AppCompatActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var voiceInputManager: VoiceInputManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        voiceInputManager = VoiceInputManager(this)

        // Observe messages
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                updateChatUI(messages)
            }
        }

        // Observe connection state
        lifecycleScope.launch {
            viewModel.isConnecting.collect { isConnecting ->
                showLoadingState(isConnecting)
            }
        }

        // Voice input button
        findViewById<View>(R.id.voiceButton).setOnClickListener {
            if (voiceInputManager.isListening.value) {
                voiceInputManager.stopListening()
            } else {
                voiceInputManager.startListening()
            }
        }

        // Send button
        findViewById<View>(R.id.sendButton).setOnClickListener {
            val input = findViewById<EditText>(R.id.messageInput)
            viewModel.sendMessage(input.text.toString())
            input.text.clear()
        }
    }
}
```

### Phase 4: Testing & Validation

**Testing Checklist**:
- [ ] Permissions requested and granted
- [ ] LiveKit connection established
- [ ] Microphone enabled automatically
- [ ] RPC methods registered successfully
- [ ] Text messages sent and received
- [ ] Voice input transcription works
- [ ] Operation status messages displayed
- [ ] Connection state updates correctly
- [ ] Reconnection works after network loss
- [ ] Background service keeps connection alive
- [ ] No memory leaks (use LeakCanary)
- [ ] UI responsive during voice input

**Testing Strategy**:
1. Unit tests for ViewModel logic
2. Integration tests for LiveKit connection
3. UI tests for chat interactions
4. Manual testing with real agent backend

---

## Files to Create/Modify

```
app/build.gradle.kts (MODIFY - add LiveKit dependencies)
app/src/main/java/com/stone/launcher/livekit/
├── LiveKitManager.kt (NEW)
├── RoomConnection.kt (NEW)
├── AudioManager.kt (NEW)
└── RpcHandler.kt (NEW)

app/src/main/java/com/stone/launcher/chat/
└── ChatActivity.kt (MODIFY - integrate LiveKit)

app/src/main/AndroidManifest.xml (MODIFY - add permissions)
app/proguard-rules.pro (MODIFY - if needed)
```

---

## Testing Criteria

- [ ] Can connect to LiveKit room
- [ ] Audio input/output works
- [ ] Can send/receive data channel messages
- [ ] Connection survives app backgrounding
- [ ] Reconnects on network changes
- [ ] Proper error handling

---

## Acceptance Criteria

- [ ] LiveKit SDK integrated successfully
- [ ] Voice chat working with AI agent
- [ ] Data channels enable tool calling
- [ ] Clean architecture and separation of concerns
- [ ] Documentation of patterns used
- [ ] No memory leaks or crashes

---

## Next Steps

After this ticket:
1. TICKET_005: Set up agents.js server
2. Connect launcher to agent backend
3. Implement tool calling API

---

## Notes

- Research first, then implement
- Use Context7 MCP tool for LiveKit docs
- Focus on Kotlin patterns, not Java
- Consider Android lifecycle carefully