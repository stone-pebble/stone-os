package com.stonelauncher.livekit

import android.content.Context
import android.util.Log
import io.livekit.android.LiveKit
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RpcInvocationData
import io.livekit.android.room.track.DataPublishReliability
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.rpc.RpcError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Singleton manager for LiveKit room connections and event handling.
 *
 * TICKET_004: LiveKit Android SDK Integration
 *
 * Responsibilities:
 * - Manage connection lifecycle to LiveKit server
 * - Handle microphone enable/disable
 * - Publish and receive data messages
 * - Register and handle RPC methods
 * - Emit room events to observers
 */
object LiveKitManager {

    private const val TAG = "LiveKitManager"

    private var room: Room? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Event stream for room events
    private val _events = MutableSharedFlow<RoomEvent>()
    val events: SharedFlow<RoomEvent> = _events.asSharedFlow()

    // Connection state
    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    /**
     * Connect to a LiveKit room.
     *
     * @param context Application context
     * @param serverUrl WebSocket URL (e.g., "wss://livekit.example.com")
     * @param token Participant token from backend
     * @return Connected Room instance
     */
    suspend fun connect(
        context: Context,
        serverUrl: String,
        token: String
    ): Room = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Connecting to LiveKit room: $serverUrl")
            _connectionState.emit(ConnectionState.Connecting)

            // Create room instance
            val newRoom = LiveKit.create(context.applicationContext)

            // Start collecting room events in background
            scope.launch {
                newRoom.events.collect { event: RoomEvent ->
                    Log.d(TAG, "Room event: ${event::class.simpleName}")
                    _events.emit(event)

                    // Update connection state based on events
                    when (event) {
                        is RoomEvent.Connected -> {
                            _connectionState.emit(ConnectionState.Connected)
                        }
                        is RoomEvent.Disconnected -> {
                            _connectionState.emit(ConnectionState.Disconnected(event.error?.message))
                        }
                        is RoomEvent.Reconnecting -> {
                            _connectionState.emit(ConnectionState.Reconnecting)
                        }
                        is RoomEvent.Reconnected -> {
                            _connectionState.emit(ConnectionState.Connected)
                        }
                        else -> {}
                    }
                }
            }

            // Connect to room
            newRoom.connect(serverUrl, token)

            // Enable microphone by default
            newRoom.localParticipant.setMicrophoneEnabled(true)
            Log.d(TAG, "Microphone enabled")

            room = newRoom
            Log.d(TAG, "Successfully connected to LiveKit room")

            newRoom
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to LiveKit room", e)
            _connectionState.emit(ConnectionState.Error(e.message ?: "Unknown error"))
            throw e
        }
    }

    /**
     * Disconnect from the LiveKit room.
     */
    suspend fun disconnect() {
        try {
            Log.d(TAG, "Disconnecting from LiveKit room")
            room?.disconnect()
            room = null
            _connectionState.emit(ConnectionState.Disconnected(null))
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from room", e)
        }
    }

    /**
     * Register an RPC method that can be called by other participants.
     *
     * @param method Method name (max 64 bytes UTF-8)
     * @param handler Suspend function that processes the RPC call
     */
    fun registerRpcMethod(method: String, handler: suspend (RpcInvocationData) -> String) {
        room?.localParticipant?.registerRpcMethod(method) { data ->
            try {
                Log.d(TAG, "RPC method '$method' called by ${data.callerIdentity}")
                handler(data)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling RPC method '$method'", e)
                throw RpcError(1500, "Handler error: ${e.message}")
            }
        }
        Log.d(TAG, "Registered RPC method: $method")
    }

    /**
     * Call an RPC method on a remote participant.
     *
     * @param destinationIdentity Identity of the participant to call
     * @param method Method name
     * @param payload String payload (max 15 KiB UTF-8)
     * @return Response string from the remote participant
     */
    suspend fun performRpc(
        destinationIdentity: String,
        method: String,
        payload: String
    ): String? {
        return try {
            Log.d(TAG, "Calling RPC method '$method' on $destinationIdentity")
            val response = room?.localParticipant?.performRpc(
                destinationIdentity = Participant.Identity(destinationIdentity),
                method = method,
                payload = payload
            )
            Log.d(TAG, "RPC method '$method' returned: $response")
            response
        } catch (e: Exception) {
            Log.e(TAG, "RPC call failed: $method", e)
            null
        }
    }

    /**
     * Publish a data message to the room.
     *
     * @param data Message data as byte array
     * @param topic Optional topic string (e.g., "lk.chat", "agent_operation_status")
     * @param reliability Delivery reliability (RELIABLE or LOSSY)
     */
    suspend fun publishData(
        data: ByteArray,
        topic: String = "lk.chat",
        reliability: DataPublishReliability = DataPublishReliability.RELIABLE
    ) {
        try {
            room?.localParticipant?.publishData(
                data = data,
                topic = topic,
                reliability = reliability
            )
            Log.d(TAG, "Published data on topic '$topic' (${data.size} bytes)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish data", e)
        }
    }

    /**
     * Send a text message on the lk.chat topic.
     *
     * @param message Text message to send
     */
    suspend fun sendMessage(message: String) {
        publishData(
            data = message.toByteArray(Charsets.UTF_8),
            topic = "lk.chat"
        )
    }

    /**
     * Enable or disable the microphone.
     *
     * @param enabled true to enable, false to disable
     */
    suspend fun setMicrophoneEnabled(enabled: Boolean) {
        try {
            room?.localParticipant?.setMicrophoneEnabled(enabled)
            Log.d(TAG, "Microphone ${if (enabled) "enabled" else "disabled"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set microphone state", e)
        }
    }

    /**
     * Get current microphone enabled state.
     *
     * @return true if microphone is enabled
     */
    fun isMicrophoneEnabled(): Boolean {
        return room?.localParticipant?.isMicrophoneEnabled ?: false
    }

    /**
     * Get the current room instance.
     *
     * @return Room instance or null if not connected
     */
    fun getRoom(): Room? = room
}

/**
 * Connection state for LiveKit room.
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState() {
        operator fun invoke(error: String?): ConnectionState = if (error != null) {
            Error(error)
        } else {
            Disconnected
        }
    }
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
