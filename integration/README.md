# StoneOS Third-Party App Integration Guide

## Overview

StoneOS provides deep integration with essential third-party applications through the Master Control Program (MCP) layer. This guide covers how to integrate apps, expose their functionality to AI agents, and maintain security.

## Core Integration Strategy

### Integration Levels

1. **Level 1: Intent-Based** - Basic app launching
2. **Level 2: API Integration** - Direct API access
3. **Level 3: SDK Integration** - Native SDK usage
4. **Level 4: Service Manipulation** - Deep system integration

## Essential App Integrations

### 1. Spotify Integration

**Integration Level**: Level 3 (SDK + Web API)

#### Implementation

```kotlin
// SpotifyMCPModule.kt
class SpotifyMCPModule : MCPModule {
    private val spotifyAppRemote: SpotifyAppRemote by lazy { 
        SpotifyAppRemote.connect(context, connectionParams)
    }
    
    override suspend fun play(trackId: String): Result<PlaybackState> {
        return withContext(Dispatchers.IO) {
            try {
                // Use Spotify SDK
                spotifyAppRemote.playerApi.play(trackId)
                
                // Get current state
                val state = spotifyAppRemote.playerApi.playerState.await()
                
                Result.success(PlaybackState(
                    isPlaying = !state.isPaused,
                    track = state.track?.name,
                    artist = state.track?.artist?.name,
                    position = state.playbackPosition
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun search(query: String, type: SearchType): Result<List<Track>> {
        // Use Web API for search
        val token = getSpotifyToken()
        val results = spotifyWebApi.search(query, type, token)
        
        return Result.success(results.map { it.toTrack() })
    }
}
```

#### MCP Server (Node.js)

```javascript
// spotify-mcp-server.js
import { Server } from '@modelcontextprotocol/sdk';
import SpotifyWebApi from 'spotify-web-api-node';

const server = new Server({
    name: 'spotify-control',
    version: '1.0.0'
});

const spotifyApi = new SpotifyWebApi({
    clientId: process.env.SPOTIFY_CLIENT_ID,
    clientSecret: process.env.SPOTIFY_CLIENT_SECRET,
    redirectUri: 'stoneos://spotify-callback'
});

// Define tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: 'play_track',
            description: 'Play a specific track on Spotify',
            inputSchema: {
                type: 'object',
                properties: {
                    track_uri: { 
                        type: 'string',
                        description: 'Spotify track URI (spotify:track:...)'
                    },
                    device_id: { 
                        type: 'string',
                        description: 'Target device ID (optional)'
                    }
                },
                required: ['track_uri']
            }
        },
        {
            name: 'search_music',
            description: 'Search for music on Spotify',
            inputSchema: {
                type: 'object',
                properties: {
                    query: { 
                        type: 'string',
                        description: 'Search query'
                    },
                    type: { 
                        type: 'string',
                        enum: ['track', 'album', 'artist', 'playlist'],
                        description: 'Type of search'
                    },
                    limit: {
                        type: 'number',
                        description: 'Number of results (max 50)'
                    }
                },
                required: ['query', 'type']
            }
        },
        {
            name: 'get_current_playback',
            description: 'Get current playback state',
            inputSchema: {
                type: 'object',
                properties: {}
            }
        },
        {
            name: 'control_playback',
            description: 'Control playback (play/pause/next/previous)',
            inputSchema: {
                type: 'object',
                properties: {
                    action: {
                        type: 'string',
                        enum: ['play', 'pause', 'next', 'previous']
                    }
                },
                required: ['action']
            }
        }
    ]
}));

// Implement tool handlers
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    
    switch (name) {
        case 'play_track':
            return await playTrack(args.track_uri, args.device_id);
        case 'search_music':
            return await searchMusic(args.query, args.type, args.limit || 20);
        case 'get_current_playback':
            return await getCurrentPlayback();
        case 'control_playback':
            return await controlPlayback(args.action);
        default:
            throw new Error(`Unknown tool: ${name}`);
    }
});
```

### 2. Google Maps Integration

**Integration Level**: Level 2/3 (Platform APIs + SDK)

#### Implementation

```kotlin
// MapsMCPModule.kt
class MapsMCPModule : MCPModule {
    private val placesClient: PlacesClient by lazy {
        Places.createClient(context)
    }
    
    private val directionsApi: DirectionsApi by lazy {
        DirectionsApi(context)
    }
    
    override suspend fun searchPlace(query: String): Result<List<Place>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .build()
                
                val response = placesClient.findAutocompletePredictions(request).await()
                
                val places = response.autocompletePredictions.map { prediction ->
                    Place(
                        id = prediction.placeId,
                        name = prediction.getPrimaryText(null).toString(),
                        address = prediction.getSecondaryText(null).toString()
                    )
                }
                
                Result.success(places)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getDirections(
        origin: Location,
        destination: Location
    ): Result<Route> {
        return withContext(Dispatchers.IO) {
            try {
                val result = directionsApi.getDirections(
                    origin = "${origin.lat},${origin.lng}",
                    destination = "${destination.lat},${destination.lng}",
                    mode = TravelMode.DRIVING
                ).await()
                
                Result.success(result.routes.first().toRoute())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun launchNavigation(destination: String): Result<Unit> {
        return try {
            val uri = Uri.parse("google.navigation:q=$destination")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3. Calendar Integration

**Integration Level**: Level 1 (Calendar Provider API)

#### Implementation

```kotlin
// CalendarMCPModule.kt
class CalendarMCPModule : MCPModule {
    private val contentResolver = context.contentResolver
    
    override suspend fun createEvent(event: CalendarEvent): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, getDefaultCalendarId())
                    put(CalendarContract.Events.TITLE, event.title)
                    put(CalendarContract.Events.DESCRIPTION, event.description)
                    put(CalendarContract.Events.DTSTART, event.startTime)
                    put(CalendarContract.Events.DTEND, event.endTime)
                    put(CalendarContract.Events.EVENT_TIMEZONE, event.timezone)
                    put(CalendarContract.Events.HAS_ALARM, event.hasReminder)
                }
                
                val uri = contentResolver.insert(
                    CalendarContract.Events.CONTENT_URI,
                    values
                )
                
                val eventId = uri?.lastPathSegment ?: throw Exception("Failed to create event")
                
                if (event.hasReminder) {
                    addReminder(eventId, event.reminderMinutes)
                }
                
                Result.success(eventId)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getEvents(
        startDate: Long,
        endDate: Long
    ): Result<List<Event>> {
        return withContext(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DESCRIPTION,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.DTEND,
                    CalendarContract.Events.EVENT_LOCATION
                )
                
                val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
                               "${CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf(startDate.toString(), endDate.toString())
                
                val cursor = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    CalendarContract.Events.DTSTART + " ASC"
                )
                
                val events = mutableListOf<Event>()
                cursor?.use {
                    while (it.moveToNext()) {
                        events.add(Event(
                            id = it.getString(0),
                            title = it.getString(1),
                            description = it.getString(2),
                            startTime = it.getLong(3),
                            endTime = it.getLong(4),
                            location = it.getString(5)
                        ))
                    }
                }
                
                Result.success(events)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### 4. Payment Integration (Google Pay)

**Integration Level**: Level 3 (Google Pay API)

#### Implementation

```kotlin
// PaymentMCPModule.kt
class PaymentMCPModule : MCPModule {
    private val paymentsClient: PaymentsClient by lazy {
        Wallet.getPaymentsClient(
            context,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
                .build()
        )
    }
    
    override suspend fun initiatePayment(
        amount: BigDecimal,
        currency: String,
        merchantName: String
    ): Result<PaymentResult> {
        return withContext(Dispatchers.Main) {
            try {
                val paymentDataRequest = createPaymentDataRequest(amount, currency, merchantName)
                
                val task = paymentsClient.loadPaymentData(paymentDataRequest)
                val paymentData = task.await()
                
                // Process payment data
                val paymentInfo = PaymentData.fromJson(paymentData.toJson())
                
                Result.success(PaymentResult(
                    transactionId = generateTransactionId(),
                    status = PaymentStatus.SUCCESS,
                    amount = amount,
                    currency = currency
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private fun createPaymentDataRequest(
        amount: BigDecimal,
        currency: String,
        merchantName: String
    ): PaymentDataRequest {
        val request = PaymentDataRequest.newBuilder()
            .setTransactionInfo(
                TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice(amount.toString())
                    .setCurrencyCode(currency)
                    .build()
            )
            .addAllowedPaymentMethod(
                PaymentMethod.newBuilder()
                    .setType(WalletConstants.PAYMENT_METHOD_CARD)
                    .setParameters(
                        PaymentMethodParameters.newBuilder()
                            .setAllowedAuthMethods(getAllowedCardAuthMethods())
                            .setAllowedCardNetworks(getAllowedCardNetworks())
                            .build()
                    )
                    .setTokenizationSpecification(getTokenizationSpec())
                    .build()
            )
            .setMerchantInfo(
                MerchantInfo.newBuilder()
                    .setMerchantName(merchantName)
                    .build()
            )
            .build()
            
        return request
    }
}
```

## Creating New Integrations

### Step 1: Analyze the App

1. **Identify Integration Points**
   - Official SDK availability
   - Public API access
   - Intent-based actions
   - Content Provider access

2. **Security Requirements**
   - Authentication method
   - Permission requirements
   - Data sensitivity

### Step 2: Design MCP Module

```kotlin
// Template for new MCP module
interface NewAppMCPModule : MCPModule {
    // Define the API surface
    suspend fun primaryAction(param: String): Result<Response>
    suspend fun secondaryAction(id: String): Result<Data>
    suspend fun queryData(filter: Filter): Result<List<Item>>
}

class NewAppMCPModuleImpl(
    private val context: Context,
    private val permissionManager: PermissionManager
) : NewAppMCPModule {
    
    init {
        // Initialize SDK or API client
    }
    
    override suspend fun primaryAction(param: String): Result<Response> {
        // Check permissions
        if (!permissionManager.hasPermission("com.stoneos.permission.NEW_APP")) {
            return Result.failure(PermissionDeniedException())
        }
        
        // Implement action
        return try {
            // SDK/API call
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Step 3: Create MCP Server

```python
# new_app_mcp_server.py
from mcp.server import Server
from mcp.server.stdio import stdio_server
import asyncio

app = Server("new-app-server")

@app.tool()
async def primary_action(param: str) -> dict:
    """Execute primary action in the app"""
    try:
        # Call MCP module via Android bridge
        result = await android_bridge.call_mcp(
            "new_app",
            "primary_action",
            {"param": param}
        )
        return {"success": True, "data": result}
    except Exception as e:
        return {"success": False, "error": str(e)}

@app.tool()
async def query_data(filter_type: str = "all") -> list:
    """Query data from the app"""
    result = await android_bridge.call_mcp(
        "new_app",
        "query_data",
        {"filter": filter_type}
    )
    return result

async def main():
    async with stdio_server() as streams:
        await app.run(
            streams[0],
            streams[1],
            app.create_initialization_options()
        )

if __name__ == "__main__":
    asyncio.run(main())
```

### Step 4: Register with Master Control Program

```kotlin
// In MasterControlProgram.kt
class MasterControlProgram {
    init {
        // Register all MCP modules
        registerModule("spotify", SpotifyMCPModule(context))
        registerModule("maps", MapsMCPModule(context))
        registerModule("calendar", CalendarMCPModule(context))
        registerModule("payment", PaymentMCPModule(context))
        registerModule("new_app", NewAppMCPModule(context)) // New integration
    }
}
```

### Step 5: Add Agent Support

```python
# In specialized agent
class NewAppAgent(Agent):
    def __init__(self):
        super().__init__(
            instructions="You can control NewApp...",
            mcp_servers=[
                mcp.MCPServerStdio(
                    command='python',
                    args=['new_app_mcp_server.py']
                )
            ]
        )
```

## Security Best Practices

### 1. Permission Model

```xml
<!-- AndroidManifest.xml -->
<permission
    android:name="com.stoneos.permission.APP_INTEGRATION"
    android:protectionLevel="dangerous"
    android:description="@string/perm_app_integration_desc" />

<uses-permission android:name="com.spotify.sdk.android.SPOTIFY_APP_REMOTE" />
<uses-permission android:name="android.permission.READ_CALENDAR" />
<uses-permission android:name="android.permission.WRITE_CALENDAR" />
```

### 2. Secure Token Storage

```kotlin
class SecureTokenManager {
    private val keyAlias = "StoneOSTokenKey"
    
    fun storeToken(service: String, token: String) {
        val encryptedToken = encrypt(token)
        securePrefs.edit()
            .putString("${service}_token", encryptedToken)
            .apply()
    }
    
    fun getToken(service: String): String? {
        val encryptedToken = securePrefs.getString("${service}_token", null)
        return encryptedToken?.let { decrypt(it) }
    }
}
```

### 3. API Rate Limiting

```kotlin
class RateLimiter {
    private val limits = mutableMapOf<String, RateLimit>()
    
    suspend fun <T> withRateLimit(
        api: String,
        maxCalls: Int,
        window: Duration,
        block: suspend () -> T
    ): T {
        val limit = limits.getOrPut(api) { 
            RateLimit(maxCalls, window) 
        }
        
        limit.acquire()
        return block()
    }
}
```

## Testing Integrations

### 1. Unit Tests

```kotlin
@Test
fun testSpotifyPlayback() = runTest {
    val module = SpotifyMCPModule(mockContext)
    
    val result = module.play("spotify:track:123")
    
    assertTrue(result.isSuccess)
    assertEquals("Playing", result.getOrNull()?.status)
}
```

### 2. Integration Tests

```python
# Test MCP server integration
async def test_spotify_search():
    async with TestServer() as server:
        result = await server.call_tool(
            "search_music",
            {"query": "jazz", "type": "playlist"}
        )
        
        assert result["success"]
        assert len(result["data"]) > 0
```

### 3. End-to-End Tests

```kotlin
@Test
fun testAgentSpotifyControl() {
    // Start agent
    val agent = startAgent("listen")
    
    // Send voice command
    agent.sendMessage("Play some relaxing jazz")
    
    // Verify Spotify started playing
    val state = getSpotifyState()
    assertTrue(state.isPlaying)
    assertTrue(state.track.contains("jazz"))
}
```

## Troubleshooting

### Common Issues

1. **Permission Denied**
   - Verify app permissions in manifest
   - Check runtime permission requests
   - Review SELinux policies

2. **SDK Not Found**
   - Ensure SDK is included in build.gradle
   - Check ProGuard rules
   - Verify initialization sequence

3. **API Authentication Failed**
   - Validate API keys
   - Check token expiration
   - Review OAuth flow

### Debug Tools

```bash
# Monitor MCP calls
adb logcat -s MCP:* | grep -E "(spotify|maps|calendar)"

# Check service status
adb shell dumpsys mcp

# Test API directly
adb shell am broadcast -a com.stoneos.TEST_MCP \
    --es module "spotify" \
    --es method "play" \
    --es args '{"track_id":"spotify:track:123"}'
``` 