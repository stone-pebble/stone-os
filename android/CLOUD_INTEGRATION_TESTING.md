# TICKET_014: Cloud Integration Testing Guide

## Overview
This guide covers testing the Android app's integration with the cloud-hosted Stone Agent server.

## Prerequisites

### 1. Cloud Agent Deployed
Ensure TICKET_013 is complete and the agent is deployed to Railway (or alternative platform).

```bash
# Verify deployment
curl https://your-app.railway.app/health
# Expected: {"status":"ok","timestamp":"..."}

curl https://your-app.railway.app/api/agents
# Expected: List of available agents
```

### 2. LiveKit Credentials Configured
Environment variables must be set on the cloud platform:
- `LIVEKIT_URL`
- `LIVEKIT_API_KEY`
- `LIVEKIT_API_SECRET`
- `OPENAI_API_KEY`

### 3. Update Production URL
Edit `/android/app/build.gradle`:
```gradle
release {
    buildConfigField "String", "TOKEN_SERVER_URL", "\"https://YOUR-APP.railway.app\""
    buildConfigField "String", "LIVEKIT_URL", "\"wss://YOUR-PROJECT.livekit.cloud\""
}
```

## Testing Checklist

### Local Development Testing (Debug Build)

#### Test 1: Local Server Connection
```bash
# Start local agent server
cd stone-agent
npm run dev

# In another terminal, build and run Android app
cd android
./gradlew installDebug

# Verify connection in logcat
adb logcat -s ChatViewModel:* ConnectionManager:* LiveKitManager:*
```

**Expected Behavior**:
- App connects to `http://10.0.2.2:8000`
- Fetches connection details successfully
- Connects to LiveKit
- Shows "connected to stone agent" message

#### Test 2: Network Availability Check
```bash
# Enable airplane mode on device/emulator
adb shell cmd connectivity airplane-mode enable

# Open chat, attempt to connect
# Expected: "No internet connection available"

# Disable airplane mode
adb shell cmd connectivity airplane-mode disable

# Retry connection
# Expected: Connection succeeds
```

#### Test 3: Connection Retry Logic
```bash
# Stop local server (simulate server down)
# pkill -f "node.*agents.js"

# Open chat, attempt to connect
# Expected: Retries 3 times with exponential backoff
# Expected: "Connection failed after 3 attempts"

# Restart server
npm run dev

# Retry connection
# Expected: Connection succeeds
```

### Production Testing (Release Build)

#### Test 4: Cloud Server Connection
```bash
# Build release APK
cd android
./gradlew assembleRelease

# Install on device
adb install app/build/outputs/apk/release/app-release.apk

# Open chat, attempt to connect
# Verify in logcat
adb logcat -s ChatViewModel:* ConnectionManager:*
```

**Expected Behavior**:
- App connects to production URL (Railway)
- Fetches connection details via HTTPS
- Connects to LiveKit Cloud
- Shows "connected to stone agent"

#### Test 5: HTTPS Enforcement
The network security config should block HTTP in production.

**Verify**:
- Edit `build.gradle` release config to use HTTP instead of HTTPS
- Rebuild and install
- Attempt connection
- Expected: Connection fails with security error

**Fix**: Change back to HTTPS URL

#### Test 6: Network State Monitoring
```bash
# Connect to cloud agent successfully
# Switch from WiFi to Mobile data
adb shell svc wifi disable

# Expected: App maintains connection (LiveKit handles network transition)

# Enable airplane mode (lose all connectivity)
adb shell cmd connectivity airplane-mode enable

# Expected: "network connection lost - voice features unavailable"

# Disable airplane mode
adb shell cmd connectivity airplane-mode disable

# Expected: Auto-reconnect (may need manual retry)
```

#### Test 7: Token Expiration
```bash
# Connect successfully
# Wait 1 hour (or set short token expiration in server)
# Attempt to use voice features

# Expected: Token refresh or re-connection flow
# (This depends on LiveKit token TTL configuration)
```

#### Test 8: Voice Chat End-to-End
```bash
# Connect successfully
# Grant microphone permission if needed
# Tap microphone button in chat
# Speak: "Hello, can you hear me?"

# Expected:
# - Audio is sent to agent
# - Agent responds
# - Response appears in chat
# - TTS audio plays (if implemented)
```

#### Test 9: Tool Execution
```bash
# Connect successfully
# Say or type: "Open Spotify"

# Expected:
# - Agent receives request
# - Calls openApp tool
# - Device command sent to Android
# - Spotify launches
# - Success message in chat
```

#### Test 10: Connection Performance
Measure connection establishment time:

```bash
# Clear app data
adb shell pm clear com.stonelauncher

# Start timing
# Open app → Open chat → Connect
# Stop timing when "connected to stone agent" appears

# Expected: < 3 seconds
```

### Error Scenarios

#### Test 11: Invalid Server URL
```bash
# Edit build.gradle with invalid URL
buildConfigField "String", "TOKEN_SERVER_URL", "\"https://invalid-url-xyz.railway.app\""

# Rebuild and install
# Attempt connection

# Expected: "Network unavailable. Check your internet connection."
# or "Connection failed after 3 attempts"
```

#### Test 12: Server 500 Error
Simulate by temporarily breaking the server endpoint.

**Expected**: Graceful error message, retry logic kicks in.

#### Test 13: Malformed Response
Simulate by modifying server to return invalid JSON.

**Expected**: Connection fails with clear error message.

## Monitoring Production

### Server Logs
```bash
# Railway
railway logs

# Check for:
# - Connection requests
# - Token generation
# - LiveKit room creation
# - Any errors
```

### Android Logs
```bash
# Filter for Stone components
adb logcat -s ChatViewModel:* ConnectionManager:* LiveKitManager:* NetworkStateManager:*

# Look for:
# - Connection attempts
# - Token fetch requests
# - Network state changes
# - Errors
```

### LiveKit Cloud Dashboard
- Check active rooms
- Monitor participant count
- View data transfer
- Check for errors

## Performance Benchmarks

### Success Criteria (from TICKET_014)
- [ ] Connection establishment: < 3 seconds
- [ ] Reconnection after network loss: < 5 seconds
- [ ] Token fetch: < 500ms
- [ ] Voice latency: < 200ms
- [ ] 99% connection success rate

### Measuring Latency
```kotlin
// Add timing logs in ChatViewModel.connect()
val startTime = System.currentTimeMillis()
// ... connection logic ...
val endTime = System.currentTimeMillis()
Log.d(TAG, "Connection took ${endTime - startTime}ms")
```

## Troubleshooting

### "No internet connection available"
- Check network state: `adb shell dumpsys connectivity`
- Verify WiFi/Mobile data enabled
- Test with: `adb shell ping -c 3 google.com`

### "Connection failed after 3 attempts"
- Verify server is running: `curl https://your-app.railway.app/health`
- Check server logs for errors
- Verify LIVEKIT credentials are set
- Test token endpoint directly: `curl "https://your-app.railway.app/api/connection-details?participantId=test&roomName=test"`

### "Request timeout"
- Check server response time
- Increase timeout in ConnectionManager if needed
- Verify network quality

### "Network security config" errors
- Verify using HTTPS in production
- Check `network_security_config.xml` domain list
- Use HTTP only for localhost (debug builds)

### LiveKit connection fails
- Verify `LIVEKIT_URL` is correct (should start with `wss://`)
- Check LiveKit project status on cloud.livekit.io
- Verify API credentials are correct
- Check LiveKit usage limits (free tier: 10K minutes/month)

## Verification Commands

### Check Build Config Values
```bash
# Decompile APK to verify BuildConfig
apktool d app-release.apk
grep "TOKEN_SERVER_URL" app-release/smali/com/stonelauncher/BuildConfig.smali
```

### Verify Network Security Config
```bash
# Extract from APK
unzip app-release.apk -d extracted/
cat extracted/res/xml/network_security_config.xml
```

### Test Token Endpoint
```bash
# Test from command line
curl -v "https://your-app.railway.app/api/connection-details?participantId=test_user&roomName=test_room"

# Expected response:
{
  "url": "wss://...",
  "token": "...",
  "roomName": "test_room"
}
```

## Success Metrics

Implementation is complete when:
- [ ] All 13 test scenarios pass
- [ ] Connection time < 3 seconds consistently
- [ ] No crashes during network transitions
- [ ] Error messages are user-friendly
- [ ] Retry logic works correctly
- [ ] Production HTTPS enforcement verified
- [ ] Voice chat works end-to-end
- [ ] Tool execution works via cloud agent

## Next Steps After Testing

1. Update TICKET_014 status to COMPLETED
2. Document any issues found
3. Create follow-up tickets for improvements
4. Update main README with deployment instructions
5. Create user documentation for setup
