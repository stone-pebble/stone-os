# Stone Launcher - Cloud Integration Setup

## Overview
The Stone Launcher Android app connects to a cloud-hosted agent server (TICKET_013) for AI voice functionality.

## Quick Start

### 1. Deploy Agent Server (TICKET_013)
Follow `/stone-agent/QUICKSTART_DEPLOYMENT.md` to deploy to Railway.

### 2. Configure Android App

Edit `/android/app/build.gradle`:

```gradle
buildTypes {
    release {
        // Replace with your Railway deployment URL
        buildConfigField "String", "TOKEN_SERVER_URL", "\"https://YOUR-APP.railway.app\""

        // Replace with your LiveKit project URL from cloud.livekit.io
        buildConfigField "String", "LIVEKIT_URL", "\"wss://YOUR-PROJECT.livekit.cloud\""
    }
    debug {
        // Local development (unchanged)
        buildConfigField "String", "TOKEN_SERVER_URL", "\"http://10.0.2.2:8000\""
        buildConfigField "String", "LIVEKIT_URL", "\"ws://10.0.2.2:7880\""
    }
}
```

### 3. Build and Test

#### Debug Build (Local Development)
```bash
# Start local agent server
cd stone-agent
npm run dev

# Build and install app
cd android
./gradlew installDebug

# Monitor logs
adb logcat -s ChatViewModel:* ConnectionManager:*
```

#### Release Build (Production)
```bash
# Build release APK
cd android
./gradlew assembleRelease

# Install
adb install app/build/outputs/apk/release/app-release.apk

# Test connection
# Open app → Chat → Should connect to cloud server
```

## Architecture

```
Android App (Kotlin)
  ↓
ConnectionManager (fetches token)
  ↓
Token Server (Railway: https://your-app.railway.app)
  ↓
LiveKit Cloud (wss://your-project.livekit.cloud)
  ↓
Stone Agent (runs on Railway)
```

## Key Components

### ConnectionManager
- Fetches connection tokens from cloud server
- Retry logic: 3 attempts with exponential backoff
- Error handling for network issues
- Location: `/android/app/src/main/java/com/stonelauncher/livekit/ConnectionManager.kt`

### NetworkStateManager
- Monitors network connectivity in real-time
- Detects WiFi ↔ Mobile data transitions
- Provides offline detection
- Location: `/android/app/src/main/java/com/stonelauncher/livekit/NetworkStateManager.kt`

### ChatViewModel
- Orchestrates connection flow
- Manages LiveKit connection lifecycle
- Handles user feedback and errors
- Location: `/android/app/src/main/java/com/stonelauncher/ui/ChatViewModel.kt`

## Configuration Files

### build.gradle
- Defines TOKEN_SERVER_URL and LIVEKIT_URL per build type
- Debug: localhost (emulator)
- Release: production URLs

### network_security_config.xml
- Enforces HTTPS in production
- Blocks cleartext traffic (except localhost in debug)
- Location: `/android/app/src/main/res/xml/network_security_config.xml`

### AndroidManifest.xml
- Declares INTERNET and ACCESS_NETWORK_STATE permissions
- References network security config

## Environment Variables Needed

On the **cloud server** (Railway/Fly/etc.), set:
- `LIVEKIT_URL` - Your LiveKit project WebSocket URL
- `LIVEKIT_API_KEY` - From cloud.livekit.io
- `LIVEKIT_API_SECRET` - From cloud.livekit.io
- `OPENAI_API_KEY` - For voice processing

On the **Android app**, set via BuildConfig (in build.gradle):
- `TOKEN_SERVER_URL` - Your deployed agent server URL
- `LIVEKIT_URL` - Your LiveKit project URL

## Testing

See comprehensive testing guide: `/android/CLOUD_INTEGRATION_TESTING.md`

**Quick Test:**
1. Ensure agent server is deployed and healthy
2. Build and install debug or release APK
3. Open app → Swipe left to chat
4. App should show "connected to stone agent"
5. Try voice input (grant microphone permission)
6. Verify agent responds

## Troubleshooting

### "No internet connection available"
- Check device network settings
- Verify WiFi/Mobile data enabled
- Test: `adb shell ping -c 3 google.com`

### "Connection failed after 3 attempts"
- Verify server is running: `curl https://your-app.railway.app/health`
- Check server logs: `railway logs`
- Verify LiveKit credentials are set
- Test token endpoint: `curl "https://your-app.railway.app/api/connection-details?participantId=test&roomName=test"`

### "Network security config" error
- Verify using HTTPS in release build
- For debug: localhost should use HTTP (10.0.2.2)
- Check `network_security_config.xml` domain list

### LiveKit connection fails
- Verify LIVEKIT_URL format: `wss://...` (not `ws://`)
- Check LiveKit project status at cloud.livekit.io
- Verify API key/secret are correct
- Check usage limits (free tier: 10K minutes/month)

## Performance Targets

- Connection establishment: < 3 seconds
- Token fetch: < 500ms
- Retry delay: 2s, 4s, 8s (exponential backoff)
- Voice latency: < 200ms

## Security

### Production
- HTTPS enforced for all cloud connections
- No cleartext traffic allowed
- Tokens fetched on-demand (not stored)
- Network security config blocks HTTP

### Debug
- HTTP allowed for localhost (10.0.2.2)
- Cleartext traffic permitted for local development
- User certificates trusted (for debugging proxies)

## Development Workflow

### Local Testing
1. Run `cd stone-agent && npm run dev`
2. Build debug APK
3. Connect via 10.0.2.2:8000

### Cloud Testing
1. Deploy to Railway
2. Update build.gradle with production URLs
3. Build release APK
4. Connect via HTTPS

## Next Steps

1. Complete TICKET_013 (deploy agent server)
2. Update build.gradle with your URLs
3. Run tests from CLOUD_INTEGRATION_TESTING.md
4. Verify voice chat works end-to-end
5. Deploy to production

## Support

- Agent deployment: See `/stone-agent/DEPLOYMENT.md`
- Testing guide: See `/android/CLOUD_INTEGRATION_TESTING.md`
- Architecture: See `/docs/LAUNCHER_ARCHITECTURE.md`
- Issues: Create ticket in `/tickets/outstanding/`
