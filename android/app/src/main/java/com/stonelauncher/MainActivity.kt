package com.stonelauncher

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stonelauncher.models.StoneApp
import com.stonelauncher.ui.ChatActivity
import com.stonelauncher.ui.StoneAppsAdapter

/**
 * Main launcher activity for Stone Launcher.
 *
 * TICKET_002: Native Kotlin Launcher UI
 *
 * Displays a minimalist 3x4 grid of Stone apps with gesture navigation:
 * - Swipe left → Stone chat (placeholder)
 * - Swipe right → Camera (placeholder)
 * - Swipe down → Unlock screen (placeholder)
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SWIPE_THRESHOLD = 150 // Increased for more intentional swipes
        private const val SWIPE_VELOCITY_THRESHOLD = 150 // Increased to avoid accidental swipes
    }

    private lateinit var gestureDetector: GestureDetector
    private lateinit var appsRecyclerView: RecyclerView

    private val stoneApps = listOf(
        StoneApp("tick", "tick"),
        StoneApp("pebbles", "pebbles"),
        StoneApp("set", "set"),
        StoneApp("listen", "listen"),
        StoneApp("ask", "ask"),
        StoneApp("look", "look"),
        StoneApp("plan", "plan"),
        StoneApp("think", "think"),
        StoneApp("reflect", "reflect"),
        StoneApp("connect", "connect"),
        StoneApp("go", "go"),
        StoneApp("fund", "fund")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "Stone Launcher UI started (TICKET_002)")

        setupFullScreen()
        setupRecyclerView()
        setupGestureDetection()
    }

    private fun setupFullScreen() {
        // Hide status bar and navigation bar for immersive full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Use WindowInsetsController for API 30+
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Fallback for older APIs
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

    private fun setupRecyclerView() {
        appsRecyclerView = findViewById(R.id.appsGrid)
        appsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        appsRecyclerView.adapter = StoneAppsAdapter(stoneApps) { app ->
            openApp(app)
        }

        // CRITICAL FIX: Add touch listener to RecyclerView to intercept swipes
        // This prevents RecyclerView from consuming horizontal swipes
        appsRecyclerView.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Let gesture detector process the event first
                // If it's a horizontal swipe gesture, intercept it
                gestureDetector.onTouchEvent(e)
                return false // Don't actually intercept, just observe
            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {}
            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })
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
                    // Swipe left (chat)
                    deltaX < -SWIPE_THRESHOLD &&
                    Math.abs(deltaX) > Math.abs(deltaY) &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD -> {
                        openStoneChat()
                        return true
                    }
                    // Swipe right (camera)
                    deltaX > SWIPE_THRESHOLD &&
                    Math.abs(deltaX) > Math.abs(deltaY) &&
                    Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD -> {
                        openCamera()
                        return true
                    }
                    // Swipe down (unlock)
                    deltaY > SWIPE_THRESHOLD &&
                    Math.abs(deltaY) > Math.abs(deltaX) &&
                    Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD -> {
                        openUnlock()
                        return true
                    }
                }

                return false
            }
        })
    }

    private fun openApp(app: StoneApp) {
        Log.d(TAG, "Opening app: ${app.name}")

        // Check if this is the "ask" app - open ChatActivity
        if (app.name == "ask") {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
            // Smooth transition animation
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
        } else {
            // For other apps, show a toast - actual app activities will be implemented in future tickets
            Toast.makeText(this, "Opening ${app.name}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openStoneChat() {
        Log.d(TAG, "Swipe left detected - opening Stone chat")
        val intent = Intent(this, ChatActivity::class.java)
        startActivity(intent)
        // Smooth transition animation
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }

    private fun openCamera() {
        Log.d(TAG, "Swipe right detected - opening camera")
        Toast.makeText(this, "Camera (placeholder)", Toast.LENGTH_SHORT).show()
    }

    private fun openUnlock() {
        Log.d(TAG, "Swipe down detected - opening unlock screen")
        Toast.makeText(this, "Unlock Screen (placeholder)", Toast.LENGTH_SHORT).show()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // CRITICAL FIX: Use dispatchTouchEvent instead of onTouchEvent
        // This intercepts gestures BEFORE RecyclerView consumes them
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
