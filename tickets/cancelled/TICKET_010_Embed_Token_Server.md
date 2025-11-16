# TICKET_010: Embed Token Server in Android App

## Status: BLOCKED_BY_TICKET_009
Priority: HIGH
Type: Implementation
Depends On: TICKET_009 (Node.js Runtime)

## Description
Bundle and run the existing token server (`stone-agent/src/server/token-server.ts`) within the embedded Node.js runtime.

## Acceptance Criteria
- [ ] Token server runs on localhost:8000 within app
- [ ] Can generate valid LiveKit tokens
- [ ] Endpoints work: /api/connection-details, /api/agents, /health
- [ ] Android app can request tokens from localhost
- [ ] No port conflicts with other apps

## Implementation Tasks
1. Bundle token-server.js in APK assets
2. Configure environment variables in Android
3. Modify server to read Android-provided config
4. Update ChatViewModel to use localhost:8000
5. Handle server lifecycle with app lifecycle
6. Add retry logic for server startup

## Integration Points
```kotlin
// ChatViewModel.kt
private val TOKEN_SERVER_URL = "http://127.0.0.1:8000"

suspend fun fetchToken(roomName: String): String {
    // Fetch from embedded server
}
```

## Testing
- Generate tokens and verify with LiveKit
- Test all API endpoints
- Verify server restarts after app kill
- Test with airplane mode (local only)
- Concurrent request handling

## Success Metrics
- Token generation < 50ms
- Server starts with app launch
- Zero external dependencies
- Works offline (except LiveKit connection)