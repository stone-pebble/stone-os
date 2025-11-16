package com.stonelauncher.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stonelauncher.R
import com.stonelauncher.livekit.ConnectionState
import com.stonelauncher.models.UnifiedMessage
import kotlinx.coroutines.launch

/**
 * Chat interface activity for Stone Launcher with LiveKit integration.
 *
 * TICKET_003: Chat Interface UI
 * TICKET_004: LiveKit Android SDK Integration
 *
 * Displays the AI chat interface with:
 * - Chat bubbles (user on right, agent on left)
 * - Input field with send/voice buttons
 * - Gesture navigation (swipe right/down to return to launcher)
 * - LiveKit connection for real-time agent communication
 * - Voice input with Android SpeechRecognizer
 */
class ChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var voiceButton: ImageButton
    private lateinit var gestureDetector: GestureDetector
    private lateinit var messageAdapter: UnifiedMessageAdapter

    // ViewModel for LiveKit integration
    private val viewModel: ChatViewModel by viewModels()

    // Permission launcher for microphone
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val micGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            Log.d(TAG, "Microphone permission granted")
            connectToAgent()
        } else {
            Log.w(TAG, "Microphone permission denied")
            Toast.makeText(this, "Microphone permission required for voice input", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        Log.d(TAG, "Stone Chat UI started (TICKET_003, TICKET_004)")

        setupFullScreen()
        setupViews()
        setupGestureDetection()
        setupObservers()

        // Request permissions and connect
        requestPermissionsAndConnect()
    }

    private fun setupFullScreen() {
        // Hide status bar and navigation bar for immersive full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun setupViews() {
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        inputField = findViewById(R.id.inputField)
        sendButton = findViewById(R.id.sendButton)
        voiceButton = findViewById(R.id.voiceButton)

        // Setup RecyclerView with UnifiedMessage adapter
        messageAdapter = UnifiedMessageAdapter()
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = messageAdapter
        }

        // Send button click
        sendButton.setOnClickListener {
            sendMessage()
        }

        // Voice button (placeholder for now)
        voiceButton.setOnClickListener {
            Log.d(TAG, "Voice input (placeholder)")
            Toast.makeText(this, "Voice input coming soon", Toast.LENGTH_SHORT).show()
            // TODO: Implement voice input with SpeechRecognizer
        }

        // Send on Enter key
        inputField.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    /**
     * Setup observers for ViewModel state.
     */
    private fun setupObservers() {
        // Observe messages
        lifecycleScope.launch {
            viewModel.messages.collect { messages ->
                messageAdapter.submitList(messages)
                // Auto-scroll to latest message
                if (messages.isNotEmpty()) {
                    messagesRecyclerView.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        // Observe connection state
        lifecycleScope.launch {
            viewModel.connectionState.collect { state ->
                Log.d(TAG, "Connection state: $state")
                // Update UI based on connection state if needed
                when (state) {
                    is ConnectionState.Connected -> {
                        // Enable voice button
                        voiceButton.isEnabled = true
                    }
                    is ConnectionState.Disconnected, is ConnectionState.Error -> {
                        // Disable voice button
                        voiceButton.isEnabled = false
                    }
                    else -> {}
                }
            }
        }

        // Observe loading state
        lifecycleScope.launch {
            viewModel.isConnecting.collect { isConnecting ->
                // Show/hide loading indicator if needed
                Log.d(TAG, "Is connecting: $isConnecting")
            }
        }
    }

    /**
     * Request necessary permissions and connect to agent.
     */
    private fun requestPermissionsAndConnect() {
        // Check if we have microphone permission
        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            connectToAgent()
        } else {
            // Request permission
            permissionLauncher.launch(arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
            ))
        }
    }

    /**
     * Connect to LiveKit agent.
     */
    private fun connectToAgent() {
        Log.d(TAG, "Connecting to agent...")
        viewModel.connect(applicationContext)
    }

    private fun setupGestureDetection() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false

                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y

                // Determine swipe direction
                when {
                    // Swipe right (back to launcher)
                    deltaX > SWIPE_THRESHOLD &&
                    Math.abs(deltaX) > Math.abs(deltaY) &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD -> {
                        Log.d(TAG, "Swipe right detected - returning to launcher")
                        finish()
                        return true
                    }
                    // Swipe down (also back to launcher)
                    deltaY > SWIPE_THRESHOLD &&
                    Math.abs(deltaY) > Math.abs(deltaX) &&
                    Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD -> {
                        Log.d(TAG, "Swipe down detected - returning to launcher")
                        finish()
                        return true
                    }
                }

                return false
            }
        })
    }

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return

        // Send via ViewModel (which handles LiveKit)
        viewModel.sendMessage(text)

        // Clear input field
        inputField.text.clear()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Let gesture detector handle gestures first
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupFullScreen()
        }
    }
}
