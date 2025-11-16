# Ticket #020: Native Kotlin Launcher UI

**Status**: Not Started
**Priority**: CRITICAL (Foundation - must be done first)
**Dependencies**: None

---

## Objective

Create a native Android launcher app in Kotlin that clones the minimalist design from the web prototype. This is pure UI - no LiveKit, no AI, just get the launcher looking right and running in the emulator.

---

## Background

We're building Stone Launcher as a native Kotlin Android app (NOT React Native). This ticket focuses solely on the UI foundation - getting the home screen with 12 Stone apps in a grid layout working in the Android emulator.

**Reference Design**: `/Users/samuellarson/Pebble/Github/stone-web-app-proto/ui/src/pages/HomeScreen.tsx`

---

## Requirements

### Visual Design
- [ ] Black background (#000000)
- [ ] 3x4 grid of Stone apps
- [ ] Minimalist text-only app icons (lowercase, serif font)
- [ ] No icons or images - just text
- [ ] Full screen, no status bar

### The 12 Stone Apps
```
tick      pebbles    set
listen    ask        look
plan      think      reflect
connect   go         fund
```

### Functionality
- [ ] Tap on app opens placeholder activity (just shows app name for now)
- [ ] Swipe gestures:
  - Swipe left → Stone chat (placeholder for now)
  - Swipe right → Camera (placeholder)
  - Swipe down → Unlock screen (placeholder)
- [ ] Home launcher replacement (can be set as default launcher)

---

## Implementation Plan

### Step 1: Create Android Project
```bash
# Create new Android project in Android Studio
# Settings:
# - Name: StoneLauncher
# - Package: com.stone.launcher
# - Language: Kotlin
# - Minimum SDK: API 26 (Android 8.0)
# - No Activity (we'll create custom)
```

### Step 2: Create MainActivity (Launcher Activity)
```kotlin
// app/src/main/java/com/stone/launcher/MainActivity.kt
package com.stone.launcher

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.GestureDetector
import android.view.MotionEvent

class MainActivity : AppCompatActivity() {

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

        setupFullScreen()
        setupRecyclerView()
        setupGestureDetection()
    }

    private fun setupFullScreen() {
        // Hide status bar and navigation
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun setupRecyclerView() {
        appsRecyclerView = findViewById(R.id.appsGrid)
        appsRecyclerView.layoutManager = GridLayoutManager(this, 3)
        appsRecyclerView.adapter = StoneAppsAdapter(stoneApps) { app ->
            openApp(app)
        }
    }

    private fun setupGestureDetection() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null || e2 == null) return false

                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y

                when {
                    deltaX < -100 -> openStoneChat()  // Swipe left
                    deltaX > 100 -> openCamera()      // Swipe right
                    deltaY > 100 -> openUnlock()      // Swipe down
                }

                return true
            }
        })
    }

    private fun openApp(app: StoneApp) {
        // For now, just show a toast or placeholder
        Toast.makeText(this, "Opening ${app.name}", Toast.LENGTH_SHORT).show()
    }

    private fun openStoneChat() {
        Toast.makeText(this, "Stone Chat (placeholder)", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {
        Toast.makeText(this, "Camera (placeholder)", Toast.LENGTH_SHORT).show()
    }

    private fun openUnlock() {
        Toast.makeText(this, "Unlock Screen (placeholder)", Toast.LENGTH_SHORT).show()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event)
    }
}

data class StoneApp(val id: String, val name: String)
```

### Step 3: Create Layout
```xml
<!-- app/src/main/res/layout/activity_main.xml -->
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/appsGrid"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:padding="32dp" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

### Step 4: Create App Grid Adapter
```kotlin
// app/src/main/java/com/stone/launcher/StoneAppsAdapter.kt
class StoneAppsAdapter(
    private val apps: List<StoneApp>,
    private val onAppClick: (StoneApp) -> Unit
) : RecyclerView.Adapter<StoneAppsAdapter.AppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount() = apps.size

    inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val appName: TextView = view.findViewById(R.id.appName)

        fun bind(app: StoneApp) {
            appName.text = app.name
            itemView.setOnClickListener { onAppClick(app) }
        }
    }
}
```

### Step 5: App Item Layout
```xml
<!-- app/src/main/res/layout/item_app.xml -->
<?xml version="1.0" encoding="utf-8"?>
<TextView
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/appName"
    android:layout_width="match_parent"
    android:layout_height="80dp"
    android:gravity="center"
    android:textColor="#FFFFFF"
    android:textSize="20sp"
    android:fontFamily="serif"
    android:background="?attr/selectableItemBackground" />
```

### Step 6: Manifest Configuration
```xml
<!-- app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:theme="@style/Theme.StoneLauncher">

        <activity
            android:name=".MainActivity"
            android:launchMode="singleTask"
            android:screenOrientation="portrait">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.HOME" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>

</manifest>
```

### Step 7: Theme (Black/Minimalist)
```xml
<!-- app/src/main/res/values/themes.xml -->
<resources>
    <style name="Theme.StoneLauncher" parent="Theme.AppCompat.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowNoTitle">true</item>
        <item name="colorPrimary">@android:color/black</item>
        <item name="colorPrimaryDark">@android:color/black</item>
        <item name="colorAccent">@android:color/white</item>
    </style>
</resources>
```

---

## Files to Create/Modify

```
app/src/main/java/com/stone/launcher/
├── MainActivity.kt (NEW)
├── StoneAppsAdapter.kt (NEW)
└── models/
    └── StoneApp.kt (NEW)

app/src/main/res/
├── layout/
│   ├── activity_main.xml (NEW)
│   └── item_app.xml (NEW)
├── values/
│   ├── themes.xml (NEW)
│   └── strings.xml (MODIFY)
└── drawable/
    └── (no images needed - text only)

app/src/main/AndroidManifest.xml (MODIFY)
app/build.gradle.kts (MODIFY - add dependencies)
```

---

## Testing Criteria

### Emulator Setup
- [ ] Create AVD for Pixel 8a (or similar)
- [ ] API Level 34 (Android 14)
- [ ] Test launcher replacement

### Functionality Tests
- [ ] Grid shows all 12 apps correctly
- [ ] Tapping app shows toast/placeholder
- [ ] Swipe left shows "Stone Chat" toast
- [ ] Swipe right shows "Camera" toast
- [ ] Swipe down shows "Unlock" toast
- [ ] Can be set as default launcher

### Visual Tests
- [ ] Black background
- [ ] White serif text
- [ ] 3x4 grid layout
- [ ] Full screen (no status bar)

---

## Acceptance Criteria

- [ ] Native Kotlin Android app (no React Native)
- [ ] Runs in Android emulator
- [ ] Looks like web prototype (black, minimalist)
- [ ] All 12 Stone apps displayed in grid
- [ ] Basic swipe gestures work
- [ ] Can be set as home launcher
- [ ] Clean, well-structured Kotlin code
- [ ] No crashes or errors

---

## Research Findings

*This section will be filled by research agent if needed*

---

## Next Steps

After this ticket is complete:
1. TICKET_021: Add chat interface UI
2. TICKET_022: Integrate LiveKit
3. TICKET_023: Set up agent server

---

## Notes

- This is foundation work - keep it simple
- No libraries except Android SDK and AndroidX
- Focus on getting the look right
- Placeholder toasts are fine for navigation
- This establishes the codebase structure for future work