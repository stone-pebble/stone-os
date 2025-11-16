package com.stonelauncher.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stonelauncher.livekit.ConnectionManager
import com.stonelauncher.livekit.ConnectionState
import com.stonelauncher.livekit.LiveKitManager
import com.stonelauncher.livekit.NetworkStateManager
import com.stonelauncher.livekit.RpcHandler
import com.stonelauncher.models.MessageRole
import com.stonelauncher.models.MessageSource
import com.stonelauncher.models.MessageType
import com.stonelauncher.models.UnifiedMessage
import com.stonelauncher.tools.ToolCallMessage
import com.stonelauncher.tools.ToolExecutor
import com.stonelauncher.tools.ToolResultMessage
import io.livekit.android.events.RoomEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONObject
import java.util.UUID

/**
 * ViewModel for ChatActivity managing LiveKit connection and chat state.
 *
 * TICKET_004: LiveKit Android SDK Integration
 *
 * Responsibilities:
 * - Connect to LiveKit room
 * - Register RPC methods
 * - Handle incoming messages and events
 * - Send messages to agent
 * - Manage chat message list
 */
class ChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    // Chat messages
    private val _messages = MutableStateFlow<List<UnifiedMessage>>(emptyList())
    val messages: StateFlow<List<UnifiedMessage>> = _messages.asStateFlow()

    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Loading state
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // Tool executor (initialized lazily)
    private var toolExecutor: ToolExecutor? = null

    // TICKET_014: Connection management
    private var connectionManager: ConnectionManager? = null
    private var networkStateManager: NetworkStateManager? = null

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Connect to LiveKit room and start listening for events.
     *
     * TICKET_014: Updated to fetch tokens from cloud server
     *
     * @param context Application context
     * @param agentType Agent type to connect to (default: "router")
     */
    fun connect(
        context: Context,
        agentType: String = "router"
    ) {
        viewModelScope.launch {
            try {
                _isConnecting.value = true
                _connectionState.value = ConnectionState.Connecting
                Log.d(TAG, "Connecting to cloud agent: agentType=$agentType")

                // Initialize connection manager
                connectionManager = ConnectionManager(context.applicationContext)

                // Initialize network state manager
                networkStateManager = NetworkStateManager(context.applicationContext)

                // Check network availability
                val networkManager = networkStateManager!!
                if (!networkManager.isNetworkAvailable()) {
                    throw Exception("No internet connection available")
                }

                // Observe network state changes
                observeNetworkState()

                // Generate unique identifiers (matching React prototype pattern)
                val timestamp = System.currentTimeMillis()
                val random = UUID.randomUUID().toString().take(8)
                val roomName = "${agentType}-${timestamp}-${random}"
                val participantId = "${agentType}_user_${timestamp}_${random}"

                Log.d(TAG, "Room: $roomName, Participant: $participantId")

                // TICKET_014: Fetch connection details from cloud server
                addMessage(
                    UnifiedMessage(
                        content = "fetching connection details...",
                        role = MessageRole.SYSTEM,
                        type = MessageType.STATUS,
                        source = MessageSource.SYSTEM
                    )
                )

                val result = connectionManager!!.fetchConnectionDetails(
                    participantId = participantId,
                    roomName = roomName,
                    agentType = agentType
                )

                val connectionDetails = result.getOrElse { error ->
                    Log.e(TAG, "Failed to fetch connection details", error)
                    throw error
                }

                Log.d(TAG, "Connection details fetched: url=${connectionDetails.url}")

                // Connect to LiveKit Cloud
                addMessage(
                    UnifiedMessage(
                        content = "connecting to livekit...",
                        role = MessageRole.SYSTEM,
                        type = MessageType.STATUS,
                        source = MessageSource.SYSTEM
                    )
                )

                LiveKitManager.connect(
                    context = context.applicationContext,
                    serverUrl = connectionDetails.url,
                    token = connectionDetails.token
                )

                // Register RPC methods
                RpcHandler.registerAllMethods()
                Log.d(TAG, "RPC methods registered")

                // Initialize tool executor - TICKET_007
                toolExecutor = ToolExecutor(context.applicationContext)
                Log.d(TAG, "Tool executor initialized")

                // Add welcome message
                addMessage(
                    UnifiedMessage(
                        content = "connected to stone agent",
                        role = MessageRole.SYSTEM,
                        type = MessageType.STATUS,
                        source = MessageSource.SYSTEM
                    )
                )

                // Observe connection state
                observeConnectionState()

                // Observe room events
                observeRoomEvents()

                _connectionState.value = ConnectionState.Connected
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                val errorMessage = when (e) {
                    is java.net.UnknownHostException ->
                        "No internet connection. Please check your network."
                    is java.net.SocketTimeoutException ->
                        "Connection timeout. Please try again."
                    else ->
                        e.message ?: "Connection failed"
                }

                _connectionState.value = ConnectionState.Error(errorMessage)
                addMessage(
                    UnifiedMessage(
                        content = "connection error: $errorMessage",
                        role = MessageRole.SYSTEM,
                        type = MessageType.STATUS,
                        source = MessageSource.SYSTEM
                    )
                )
            } finally {
                _isConnecting.value = false
            }
        }
    }

    /**
     * Disconnect from LiveKit room.
     */
    fun disconnect() {
        viewModelScope.launch {
            try {
                LiveKitManager.disconnect()
                Log.d(TAG, "Disconnected from LiveKit")
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting", e)
            }
        }
    }

    /**
     * Send a text message.
     *
     * @param content Message text
     */
    fun sendMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                // Add to UI immediately
                val userMessage = UnifiedMessage(
                    content = content,
                    role = MessageRole.USER,
                    type = MessageType.CHAT,
                    source = MessageSource.MANUAL
                )
                addMessage(userMessage)

                // Send to agent via LiveKit
                LiveKitManager.sendMessage(content)
                Log.d(TAG, "Sent message: $content")

                // Simulate agent response (for testing without live server)
                simulateAgentResponse(content)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
                addMessage(
                    UnifiedMessage(
                        content = "failed to send message: ${e.message}",
                        role = MessageRole.SYSTEM,
                        type = MessageType.STATUS,
                        source = MessageSource.SYSTEM
                    )
                )
            }
        }
    }

    /**
     * Add a message to the chat.
     */
    private fun addMessage(message: UnifiedMessage) {
        _messages.value = _messages.value + message
    }

    /**
     * Observe network state changes.
     *
     * TICKET_014: Network monitoring
     */
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkStateManager?.isOnline?.collect { isOnline ->
                Log.d(TAG, "Network state: ${if (isOnline) "online" else "offline"}")

                if (!isOnline) {
                    _connectionState.value = ConnectionState.Error("Network connection lost")
                    addMessage(
                        UnifiedMessage(
                            content = "network connection lost - voice features unavailable",
                            role = MessageRole.SYSTEM,
                            type = MessageType.STATUS,
                            source = MessageSource.SYSTEM
                        )
                    )
                }
            }
        }
    }

    /**
     * Observe LiveKit connection state changes.
     */
    private fun observeConnectionState() {
        viewModelScope.launch {
            LiveKitManager.connectionState.collect { state ->
                _connectionState.value = state
                Log.d(TAG, "Connection state: $state")

                // Add status message for connection changes
                val statusMessage = when (state) {
                    is ConnectionState.Connecting -> "connecting to agent..."
                    is ConnectionState.Connected -> "connected"
                    is ConnectionState.Reconnecting -> "reconnecting..."
                    is ConnectionState.Disconnected -> "disconnected"
                    is ConnectionState.Error -> "error: ${state.message}"
                }

                if (state !is ConnectionState.Connected) {
                    addMessage(
                        UnifiedMessage(
                            content = statusMessage,
                            role = MessageRole.SYSTEM,
                            type = MessageType.STATUS,
                            source = MessageSource.SYSTEM
                        )
                    )
                }
            }
        }
    }

    /**
     * Observe LiveKit room events.
     */
    private fun observeRoomEvents() {
        viewModelScope.launch {
            LiveKitManager.events.collect { event ->
                handleRoomEvent(event)
            }
        }
    }

    /**
     * Handle room events from LiveKit.
     */
    private fun handleRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.DataReceived -> {
                handleDataReceived(event)
            }
            is RoomEvent.ParticipantConnected -> {
                Log.d(TAG, "Participant connected: ${event.participant.identity}")
            }
            is RoomEvent.ParticipantDisconnected -> {
                Log.d(TAG, "Participant disconnected: ${event.participant.identity}")
            }
            else -> {
                Log.d(TAG, "Room event: ${event::class.simpleName}")
            }
        }
    }

    /**
     * Handle data received from LiveKit data channel.
     */
    private fun handleDataReceived(event: RoomEvent.DataReceived) {
        try {
            val topic = event.topic
            val data = String(event.data, Charsets.UTF_8)

            Log.d(TAG, "Data received on topic '$topic': $data")

            when (topic) {
                "lk.chat" -> {
                    // Agent chat message
                    addMessage(
                        UnifiedMessage(
                            content = data,
                            role = MessageRole.ASSISTANT,
                            type = MessageType.CHAT,
                            source = MessageSource.AGENT
                        )
                    )
                }
                "agent_operation_status", "agent_busy_signal" -> {
                    // Operation status update
                    handleOperationStatus(data)
                }
                "device_command" -> {
                    // TICKET_007: Device control command from agent
                    handleDeviceCommand(data)
                }
                else -> {
                    Log.d(TAG, "Unknown topic: $topic")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling data received", e)
        }
    }

    /**
     * Handle operation status message.
     */
    private fun handleOperationStatus(jsonData: String) {
        try {
            val signal = JSONObject(jsonData)

            val agent = signal.optString("agent", "unknown")
            val operation = signal.optString("operation", "")
            val busy = signal.optBoolean("busy", false)
            val details = signal.optString("details", "")
            val toolName = signal.optString("toolName", null)
            val stepDescription = signal.optString("stepDescription", null)
            val operationId = signal.optString("operationId", null)
            val progressPercentage = if (signal.has("progressPercentage")) {
                signal.getInt("progressPercentage")
            } else null

            // Create operation message
            val content = buildString {
                append(agent)
                if (operation.isNotBlank()) {
                    append(": $operation")
                }
                if (details.isNotBlank()) {
                    append(" - $details")
                }
                if (progressPercentage != null) {
                    append(" ($progressPercentage%)")
                }
            }

            addMessage(
                UnifiedMessage(
                    content = content,
                    role = MessageRole.SYSTEM,
                    type = MessageType.OPERATION,
                    source = MessageSource.AGENT,
                    agent = agent,
                    busy = busy,
                    operation = operation,
                    toolName = toolName,
                    stepDescription = stepDescription,
                    operationId = operationId,
                    progressPercentage = progressPercentage
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse operation status", e)
        }
    }

    /**
     * Simulate agent response (for testing without live server).
     */
    private fun simulateAgentResponse(userMessage: String) {
        viewModelScope.launch {
            // Show typing indicator
            val typingMessage = UnifiedMessage(
                id = "typing",
                content = "",
                role = MessageRole.ASSISTANT,
                type = MessageType.TYPING,
                source = MessageSource.AGENT,
                isTyping = true
            )
            addMessage(typingMessage)

            // Simulate delay
            kotlinx.coroutines.delay(1500)

            // Remove typing indicator
            _messages.value = _messages.value.filter { it.id != "typing" }

            // Add response
            val response = when {
                userMessage.contains("hello", ignoreCase = true) ->
                    "hello! how can I help you today?"
                userMessage.contains("weather", ignoreCase = true) ->
                    "I can help with weather information. (note: this is simulated - connect to live agent for real responses)"
                else ->
                    "I understand: \"$userMessage\". (simulated response - connect to live agent for real interactions)"
            }

            addMessage(
                UnifiedMessage(
                    content = response,
                    role = MessageRole.ASSISTANT,
                    type = MessageType.CHAT,
                    source = MessageSource.AGENT
                )
            )
        }
    }

    /**
     * Handle device command from agent.
     *
     * TICKET_007: Tool Calling Integration
     *
     * Parses tool call message, executes the tool via ToolExecutor,
     * and sends the result back via LiveKit data channel.
     */
    private fun handleDeviceCommand(jsonData: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Handling device command: $jsonData")

                // Parse tool call message
                val toolCall = json.decodeFromString<ToolCallMessage>(jsonData)

                // Show operation status in chat
                addMessage(
                    UnifiedMessage(
                        content = "executing: ${toolCall.tool}",
                        role = MessageRole.SYSTEM,
                        type = MessageType.OPERATION,
                        source = MessageSource.AGENT
                    )
                )

                // Execute the tool
                val executor = toolExecutor
                if (executor == null) {
                    Log.e(TAG, "Tool executor not initialized")
                    sendToolResult(
                        ToolResultMessage(
                            success = false,
                            error = com.stonelauncher.tools.ToolError(
                                code = "NOT_INITIALIZED",
                                message = "Tool executor not initialized"
                            )
                        )
                    )
                    return@launch
                }

                val result = executor.executeTool(toolCall)

                // Send result back to agent
                sendToolResult(result)

                // Show result in chat
                val statusMessage = if (result.success) {
                    "✓ ${toolCall.tool} completed"
                } else {
                    "✗ ${toolCall.tool} failed: ${result.error?.message}"
                }

                addMessage(
                    UnifiedMessage(
                        content = statusMessage,
                        role = MessageRole.SYSTEM,
                        type = MessageType.OPERATION,
                        source = MessageSource.SYSTEM
                    )
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error handling device command", e)
                addMessage(
                    UnifiedMessage(
                        content = "error executing command: ${e.message}",
                        role = MessageRole.SYSTEM,
                        type = MessageType.STATUS,
                        source = MessageSource.SYSTEM
                    )
                )
            }
        }
    }

    /**
     * Send tool result back to agent via data channel.
     */
    private suspend fun sendToolResult(result: ToolResultMessage) {
        try {
            val resultJson = json.encodeToString(result)
            val data = resultJson.toByteArray(Charsets.UTF_8)

            LiveKitManager.publishData(
                data = data,
                topic = "device_command_result"
            )

            Log.d(TAG, "Sent tool result: success=${result.success}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send tool result", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            disconnect()
        }
        // Clean up network state manager
        networkStateManager?.cleanup()
    }
}
