# Embedded Server Architecture for StoneOS

## Overview
Run both token server and agent workers directly inside the Android app, eliminating the need for external servers while maintaining scalability.

## Architecture Options

### Option 1: Node.js Mobile (Recommended)
Embed full Node.js runtime inside Android app using nodejs-mobile.

```
┌─────────────────────────────────────┐
│  Stone Launcher APK                 │
├─────────────────────────────────────┤
│  Native Android UI (Kotlin)         │
│    ↕ JNI Bridge                     │
│  Node.js Runtime (nodejs-mobile)    │
│    - Token Server (Express)         │
│    - Agent Workers (agents.js)      │
│    ↕ LiveKit SDK                    │
│  LiveKit Android SDK                │
└─────────────────────────────────────┘
```

#### Implementation:

**1. Add nodejs-mobile to Android project:**

```gradle
// app/build.gradle
dependencies {
    implementation 'com.janeasystems:nodejs-mobile-react-native:0.8.1'
    // OR
    implementation 'com.janeasystems:nodejs-mobile:0.1.0'
}
```

**2. Create Node.js service in Android:**

```kotlin
// app/src/main/java/com/stonelauncher/services/EmbeddedNodeService.kt
package com.stonelauncher.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.janeasystems.nodejs_mobile.NodeJS
import java.io.File

class EmbeddedNodeService : Service() {
    companion object {
        private const val TAG = "EmbeddedNodeService"
        var isRunning = false
            private set
    }

    private lateinit var nodejs: NodeJS

    override fun onCreate() {
        super.onCreate()

        // Initialize Node.js
        nodejs = NodeJS(this)

        // Copy our server files from assets to internal storage
        copyServerFiles()

        // Start Node.js with our server
        startNodeServer()
    }

    private fun copyServerFiles() {
        // Copy stone-agent files from assets to app's internal storage
        val serverDir = File(filesDir, "stone-server")
        if (!serverDir.exists()) {
            serverDir.mkdirs()

            // Copy from assets/stone-agent/* to serverDir
            assets.list("stone-agent")?.forEach { file ->
                assets.open("stone-agent/$file").use { input ->
                    File(serverDir, file).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun startNodeServer() {
        val serverPath = File(filesDir, "stone-server/index.js").absolutePath

        // Set environment variables
        nodejs.setEnvironmentVariable("LIVEKIT_URL", "wss://localhost:7880")
        nodejs.setEnvironmentVariable("PORT", "8000")
        nodejs.setEnvironmentVariable("AGENT_PORT", "8081")

        // Start the server
        nodejs.startNodeWithArguments(arrayOf(serverPath))

        isRunning = true
        Log.i(TAG, "Node.js server started")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        nodejs.stop()
        isRunning = false
        super.onDestroy()
    }
}
```

**3. Modified MainActivity to start service:**

```kotlin
// MainActivity.kt
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start embedded Node.js service if not running
        if (!EmbeddedNodeService.isRunning) {
            startService(Intent(this, EmbeddedNodeService::class.java))
        }

        // Wait for service to be ready
        lifecycleScope.launch {
            waitForNodeService()
            connectToLocalAgent()
        }
    }

    private suspend fun waitForNodeService() {
        while (!isNodeServerReady()) {
            delay(100)
        }
    }

    private suspend fun isNodeServerReady(): Boolean {
        return try {
            // Check if local server responds
            val response = httpClient.get("http://127.0.0.1:8000/health")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    private fun connectToLocalAgent() {
        // Connect to localhost Node.js server
        chatViewModel.connect(
            context = this,
            serverUrl = "http://127.0.0.1:8000",
            agentType = "router"
        )
    }
}
```

### Option 2: React Native Bridge (If needed for compatibility)

If agents.js has issues with nodejs-mobile, use React Native as a bridge:

```kotlin
// Use React Native's built-in Node.js support
class ReactNativeBridge {
    private val reactContext = ReactApplicationContext(this)

    fun startAgentServer() {
        // React Native modules can run Node.js code
        val agentModule = AgentServerModule(reactContext)
        agentModule.start()
    }
}
```

### Option 3: WebView + Service Worker (Lightweight)

Run JavaScript in a hidden WebView with service workers:

```kotlin
// Lightweight approach using WebView
class EmbeddedAgentService : Service() {
    private lateinit var webView: WebView

    override fun onCreate() {
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            // Load the agent JavaScript
            loadUrl("file:///android_asset/agent-bundle.html")

            // Bridge to communicate with Android
            addJavascriptInterface(AndroidBridge(this@EmbeddedAgentService), "Android")
        }
    }
}
```

## Critical Compatibility Check: LiveKit

**The Key Question: Can embedded agents.js connect to Android's LiveKit SDK?**

### Scenario A: Both in Same Process ✅ (WORKS)
```
Android App Process:
├── LiveKit Android SDK (Native)
├── Node.js Runtime (nodejs-mobile)
│   ├── Token Server → Generates tokens for Android SDK
│   └── Agent Worker → Connects to SAME LiveKit room
└── They share localhost networking
```

### Scenario B: LiveKit Cloud Relay ✅ (WORKS)
```
Android Device:
├── LiveKit Android SDK → LiveKit Cloud
├── Embedded Node.js
│   └── Agent Worker → LiveKit Cloud
└── Both connect to same room in cloud
```

## Recommended Architecture for Scale

Given your requirements for multiple devices and web UI control:

```yaml
Architecture: Hybrid Local + Cloud
─────────────────────────────────

Each Device Runs:
  - Stone Launcher (Kotlin)
  - Embedded Node.js Server
  - Local token generation
  - Local agent workers

Cloud Infrastructure (Minimal):
  - LiveKit Cloud (for WebRTC signaling only)
  - Web Dashboard (for device management)
  - Device Registry API

Benefits:
  ✓ Each device is self-contained
  ✓ Works offline (with local AI models)
  ✓ Scales to thousands of devices
  ✓ Web UI can manage devices remotely
  ✓ No per-device server costs
```

## Implementation Plan

### Phase 1: Embedded Token Server (Quick Win)
```kotlin
// Just embed the token server, use LiveKit Cloud for agents
class TokenServerService : Service() {
    // Run Express server locally on port 8000
    // Generate tokens for LiveKit Cloud
}
```

### Phase 2: Full Embedded Stack
```kotlin
// Embed both token server AND agent workers
class FullStackService : Service() {
    // Token server on :8000
    // Agent workers connecting to LiveKit
    // All running inside the app
}
```

### Phase 3: Web Management UI
```typescript
// Web dashboard for managing fleet of devices
interface DeviceManager {
  listDevices(): Device[]
  sendCommand(deviceId: string, command: Command): void
  viewStatus(deviceId: string): DeviceStatus
  updateConfig(deviceId: string, config: Config): void
}
```

## Memory & Performance Considerations

### Node.js Mobile Overhead:
- **RAM**: ~50-100MB for Node.js runtime
- **Storage**: ~30MB for Node.js + your server code
- **CPU**: Minimal when idle, spikes during agent processing

### Optimization Strategies:
1. **Lazy Loading**: Start Node.js only when chat is opened
2. **Memory Limits**: Set max heap size for Node.js
3. **Background Limits**: Reduce activity when app backgrounded
4. **Native Fallbacks**: Use Kotlin for performance-critical paths

## Build Configuration

```gradle
// app/build.gradle
android {
    packagingOptions {
        // Include Node.js files in APK
        jniLibs.useLegacyPackaging = true
    }

    sourceSets {
        main {
            // Include server files as assets
            assets.srcDirs += ['../stone-agent/dist']
        }
    }
}

dependencies {
    implementation 'com.janeasystems:nodejs-mobile:0.1.0'
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'
}
```

## Security Considerations

1. **Localhost Only**: Bind servers to 127.0.0.1, not 0.0.0.0
2. **Token Validation**: Validate all tokens even though local
3. **Process Isolation**: Run Node.js in separate process
4. **Update Mechanism**: How to update embedded server code

## Testing Strategy

```bash
# 1. Test embedded server
adb shell am startservice com.stonelauncher/.EmbeddedNodeService
adb shell curl http://127.0.0.1:8000/health

# 2. Check memory usage
adb shell dumpsys meminfo com.stonelauncher

# 3. Test agent connection
adb logcat | grep -E "Node|Agent|LiveKit"
```

## Fallback Strategy

If embedding proves problematic:

1. **Companion App**: Separate "Stone Server" app that runs the servers
2. **Termux Solution**: Use Termux to run Node.js servers
3. **Cloud Fallback**: Detect if local fails, fall back to cloud

## Cost Analysis

### Embedded Servers (Your Approach):
- **Per Device Cost**: $0 (after LiveKit Cloud subscription)
- **LiveKit Cloud**: ~$50/month for 100 devices (minimal usage)
- **Scales to**: Unlimited devices

### Traditional Cloud:
- **Per Device Cost**: ~$0.20/month (server resources)
- **LiveKit Cloud**: Same ~$50/month
- **Scales to**: Limited by server costs

## Conclusion

**YES, this is totally feasible!** Embedding the servers in the Android app is brilliant for your use case:

1. ✅ Each device is self-contained
2. ✅ Minimal cloud dependencies (just LiveKit signaling)
3. ✅ Scales to thousands of devices with no per-device server cost
4. ✅ Web UI can still manage devices via LiveKit rooms
5. ✅ Works offline with local AI models (future enhancement)

The nodejs-mobile approach gives you a real Node.js runtime inside Android, so agents.js should work without modifications.