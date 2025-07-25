# Master Control Program & Model Context Protocol Architecture

## Overview

StoneOS implements two distinct but complementary MCP systems:

1. **Master Control Program (MCP)** - A native Android service that provides unified access to device capabilities
2. **Model Context Protocol (MCP)** - Anthropic's standard for exposing tools and resources to AI agents

This document details how both systems work together to enable AI-driven device control.

## Master Control Program (Native Service)

### Purpose

The Master Control Program is a privileged Android system service that acts as the central API gateway for all device capabilities. It provides:

- Unified access to third-party app functionality
- Permission management and security enforcement
- State management across integrations
- Abstraction layer between AI agents and device APIs

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    JavaScript UI                         │
│                 (React WebView Shell)                    │
└────────────────────┬────────────────────────────────────┘
                     │ Native Bridge
┌────────────────────┴────────────────────────────────────┐
│              Master Control Program                      │
│                (System Service)                          │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Spotify   │  │    Maps     │  │  Calendar   │    │
│  │   Module    │  │   Module    │  │   Module    │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │  Contacts   │  │   Camera    │  │   Payment   │    │
│  │   Module    │  │   Module    │  │   Module    │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
└─────────────────────────────────────────────────────────┘
                     │ Android APIs
┌─────────────────────────────────────────────────────────┐
│           Android System & Third-Party Apps              │
└─────────────────────────────────────────────────────────┘
```

### Implementation

```kotlin
// MasterControlProgram.kt
@SystemService(Context.MCP_SERVICE)
class MasterControlProgram(
    private val context: Context,
    private val permissionManager: PermissionManager
) : IMasterControlProgram.Stub() {
    
    private val modules = mutableMapOf<String, MCPModule>()
    
    init {
        // Register all modules
        registerModule("spotify", SpotifyMCPModule(context))
        registerModule("maps", MapsMCPModule(context))
        registerModule("calendar", CalendarMCPModule(context))
        registerModule("contacts", ContactsMCPModule(context))
        registerModule("camera", CameraMCPModule(context))
        registerModule("payment", PaymentMCPModule(context))
        registerModule("phone", PhoneMCPModule(context))
        registerModule("messages", MessagesMCPModule(context))
        registerModule("email", EmailMCPModule(context))
        registerModule("notes", NotesMCPModule(context))
        registerModule("weather", WeatherMCPModule(context))
        registerModule("health", HealthMCPModule(context))
    }
    
    override fun call(
        module: String,
        method: String,
        args: Bundle,
        callback: IMCPCallback
    ) {
        // Permission check
        val permission = getRequiredPermission(module, method)
        if (!permissionManager.checkPermission(permission, Binder.getCallingUid())) {
            callback.onError(MCPError.PERMISSION_DENIED)
            return
        }
        
        // Audit log
        auditLog.log(AuditEvent(
            uid = Binder.getCallingUid(),
            module = module,
            method = method,
            timestamp = System.currentTimeMillis()
        ))
        
        // Execute in module
        val mcpModule = modules[module] ?: run {
            callback.onError(MCPError.MODULE_NOT_FOUND)
            return
        }
        
        coroutineScope.launch {
            try {
                val result = mcpModule.execute(method, args)
                callback.onSuccess(result)
            } catch (e: Exception) {
                callback.onError(MCPError.EXECUTION_FAILED, e.message)
            }
        }
    }
}
```

### Module Interface

Each MCP module implements a standard interface:

```kotlin
interface MCPModule {
    // Module metadata
    val name: String
    val version: String
    val capabilities: List<String>
    
    // Execute a method
    suspend fun execute(method: String, args: Bundle): Bundle
    
    // Lifecycle
    fun onInitialize()
    fun onShutdown()
}

// Example implementation
class SpotifyMCPModule(private val context: Context) : MCPModule {
    override val name = "spotify"
    override val version = "1.0.0"
    override val capabilities = listOf(
        "play", "pause", "search", "getPlaybackState",
        "setVolume", "skipNext", "skipPrevious"
    )
    
    private val spotifyApi: SpotifyAppRemote by lazy {
        SpotifyAppRemote.connect(context, connectionParams)
    }
    
    override suspend fun execute(method: String, args: Bundle): Bundle {
        return when (method) {
            "play" -> play(args.getString("uri"))
            "search" -> search(args.getString("query"))
            "getPlaybackState" -> getPlaybackState()
            else -> throw IllegalArgumentException("Unknown method: $method")
        }
    }
}
```

## Model Context Protocol (AI Tools)

### Purpose

Model Context Protocol (MCP) is Anthropic's standard for exposing tools to AI models. In StoneOS:

- Provides standardized tool interfaces for agents
- Enables language-agnostic tool implementation
- Supports composable, reusable capabilities
- Maintains security through sandboxed execution

### Architecture

```
┌─────────────────────────────────────────────────────────┐
│                   AI Agent (Python)                      │
│              (Router, Ask, Think, etc.)                  │
└────────────────────┬────────────────────────────────────┘
                     │ MCP Client
┌────────────────────┴────────────────────────────────────┐
│                  MCP Servers (Tools)                     │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │   Spotify   │  │  Perplexity │  │ File System │    │
│  │     MCP     │  │     MCP     │  │     MCP     │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │ Google Maps │  │   Weather   │  │  Calculator │    │
│  │     MCP     │  │     MCP     │  │     MCP     │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
└────────────────────┬────────────────────────────────────┘
                     │ Android Bridge
┌────────────────────┴────────────────────────────────────┐
│              Master Control Program                      │
│             (Executes on device)                         │
└─────────────────────────────────────────────────────────┘
```

### MCP Server Implementation

#### TypeScript/JavaScript Example

```typescript
// spotify-mcp-server.ts
import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  ListToolsRequestSchema,
  CallToolRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';

const server = new Server(
  {
    name: 'spotify-mcp',
    version: '1.0.0',
  },
  {
    capabilities: {
      tools: {},
    },
  }
);

// Define available tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [
    {
      name: 'play_music',
      description: 'Play music on Spotify',
      inputSchema: {
        type: 'object',
        properties: {
          query: {
            type: 'string',
            description: 'Song, artist, album, or playlist to play'
          },
          shuffle: {
            type: 'boolean',
            description: 'Enable shuffle mode',
            default: false
          }
        },
        required: ['query']
      }
    },
    {
      name: 'control_playback',
      description: 'Control Spotify playback',
      inputSchema: {
        type: 'object',
        properties: {
          action: {
            type: 'string',
            enum: ['play', 'pause', 'next', 'previous'],
            description: 'Playback control action'
          }
        },
        required: ['action']
      }
    },
    {
      name: 'get_current_track',
      description: 'Get information about the currently playing track',
      inputSchema: {
        type: 'object',
        properties: {}
      }
    }
  ]
}));

// Handle tool execution
server.setRequestHandler(CallToolRequestSchema, async (request) => {
  const { name, arguments: args } = request.params;
  
  try {
    // Call Android MCP through bridge
    const result = await androidBridge.callMCP('spotify', name, args);
    
    return {
      content: [
        {
          type: 'text',
          text: JSON.stringify(result)
        }
      ]
    };
  } catch (error) {
    return {
      content: [
        {
          type: 'text',
          text: `Error: ${error.message}`
        }
      ],
      isError: true
    };
  }
});

// Start server
const transport = new StdioServerTransport();
await server.connect(transport);
```

#### Python Example

```python
# calendar_mcp_server.py
from typing import Any
from mcp.server.models import InitializationOptions
from mcp.server import NotificationOptions, Server
from mcp.server.stdio import stdio_server
import asyncio

server = Server("calendar-mcp")

@server.list_tools()
async def handle_list_tools() -> list[dict[str, Any]]:
    return [
        {
            "name": "create_event",
            "description": "Create a calendar event",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "title": {
                        "type": "string",
                        "description": "Event title"
                    },
                    "start_time": {
                        "type": "string",
                        "description": "Start time (ISO 8601 format)"
                    },
                    "end_time": {
                        "type": "string",
                        "description": "End time (ISO 8601 format)"
                    },
                    "location": {
                        "type": "string",
                        "description": "Event location (optional)"
                    },
                    "description": {
                        "type": "string",
                        "description": "Event description (optional)"
                    }
                },
                "required": ["title", "start_time", "end_time"]
            }
        },
        {
            "name": "get_events",
            "description": "Get calendar events for a date range",
            "inputSchema": {
                "type": "object",
                "properties": {
                    "start_date": {
                        "type": "string",
                        "description": "Start date (YYYY-MM-DD)"
                    },
                    "end_date": {
                        "type": "string",
                        "description": "End date (YYYY-MM-DD)"
                    }
                },
                "required": ["start_date", "end_date"]
            }
        }
    ]

@server.call_tool()
async def handle_call_tool(
    name: str, 
    arguments: dict[str, Any]
) -> list[dict[str, Any]]:
    if name == "create_event":
        # Call Android MCP to create event
        result = await android_bridge.call_mcp(
            "calendar",
            "createEvent",
            arguments
        )
        return [{
            "type": "text",
            "text": f"Event created with ID: {result['eventId']}"
        }]
    
    elif name == "get_events":
        # Query calendar events
        events = await android_bridge.call_mcp(
            "calendar",
            "getEvents",
            arguments
        )
        return [{
            "type": "text",
            "text": format_events(events)
        }]
    
    else:
        raise ValueError(f"Unknown tool: {name}")

async def main():
    async with stdio_server() as (read_stream, write_stream):
        await server.run(
            read_stream,
            write_stream,
            InitializationOptions(
                server_name="calendar-mcp",
                server_version="1.0.0"
            )
        )

if __name__ == "__main__":
    asyncio.run(main())
```

### Agent Integration

Agents use MCP servers to access device capabilities:

```python
# In agent code
from livekit.agents import Agent, mcp

class CalendarAgent(Agent):
    def __init__(self):
        super().__init__(
            instructions="""
            You are a calendar assistant. You can:
            - Create events with natural language
            - Check schedules and availability
            - Set reminders and notifications
            - Find optimal meeting times
            """,
            mcp_servers=[
                mcp.MCPServerStdio(
                    command='python',
                    args=['calendar_mcp_server.py']
                )
            ]
        )
    
    async def on_enter(self):
        # Agent now has access to calendar tools
        # The LLM can call create_event, get_events, etc.
        self.session.generate_reply()
```

## Bridge Between MCP Systems

The two MCP systems are connected through an Android Bridge that translates between them:

```kotlin
// AndroidBridge.kt
class AndroidBridge {
    private val mcp = context.getSystemService(Context.MCP_SERVICE) as IMasterControlProgram
    
    suspend fun callMCP(
        module: String,
        method: String,
        args: Map<String, Any>
    ): Map<String, Any> {
        val bundle = Bundle().apply {
            args.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Boolean -> putBoolean(key, value)
                    // ... handle other types
                }
            }
        }
        
        return suspendCoroutine { continuation ->
            mcp.call(module, method, bundle, object : IMCPCallback.Stub() {
                override fun onSuccess(result: Bundle) {
                    continuation.resume(result.toMap())
                }
                
                override fun onError(error: Int, message: String?) {
                    continuation.resumeWithException(
                        MCPException(error, message)
                    )
                }
            })
        }
    }
}
```

## Security Model

### Permission System

```xml
<!-- AndroidManifest.xml -->
<!-- Master Control Program permissions -->
<permission
    android:name="com.stoneos.permission.MCP_ACCESS"
    android:protectionLevel="signature|privileged" />

<permission
    android:name="com.stoneos.permission.MCP_SPOTIFY"
    android:protectionLevel="dangerous" />

<permission
    android:name="com.stoneos.permission.MCP_CALENDAR"
    android:protectionLevel="dangerous" />

<permission
    android:name="com.stoneos.permission.MCP_PAYMENT"
    android:protectionLevel="dangerous" />
```

### Sandboxing

MCP servers run in isolated processes with limited permissions:

```python
# MCP server sandbox configuration
sandbox_config = {
    "filesystem": {
        "read": ["/data/mcp/calendar"],
        "write": ["/data/mcp/calendar/cache"]
    },
    "network": {
        "allowed_hosts": ["localhost", "api.calendar.com"]
    },
    "memory_limit": "256MB",
    "cpu_limit": "25%"
}
```

### Audit Logging

All MCP calls are logged for security analysis:

```kotlin
data class MCPAuditLog(
    val timestamp: Long,
    val uid: Int,
    val module: String,
    val method: String,
    val args: String,  // Sanitized
    val result: String, // Success/Failure
    val duration: Long
)

class MCPAuditLogger {
    fun log(event: MCPAuditLog) {
        // Write to secure log file
        secureLogFile.appendText(event.toJson())
        
        // Alert on suspicious activity
        if (isSuspicious(event)) {
            securityMonitor.alert(event)
        }
    }
}
```

## Performance Optimization

### Caching

```kotlin
class MCPCache {
    private val cache = LruCache<String, Bundle>(100)
    
    fun getCached(module: String, method: String, args: Bundle): Bundle? {
        val key = "$module:$method:${args.hashCode()}"
        return cache.get(key)
    }
    
    fun cache(module: String, method: String, args: Bundle, result: Bundle) {
        val key = "$module:$method:${args.hashCode()}"
        cache.put(key, result)
    }
}
```

### Connection Pooling

```python
# MCP server connection pool
class MCPServerPool:
    def __init__(self, max_servers=10):
        self.pool = {}
        self.max_servers = max_servers
    
    async def get_server(self, server_name: str) -> MCPServer:
        if server_name in self.pool:
            return self.pool[server_name]
        
        if len(self.pool) >= self.max_servers:
            # Evict least recently used
            await self._evict_lru()
        
        server = await self._start_server(server_name)
        self.pool[server_name] = server
        return server
```

## Testing

### Unit Tests

```kotlin
@Test
fun testSpotifyPlay() = runTest {
    val mcp = MockMasterControlProgram()
    val spotifyModule = SpotifyMCPModule(mockContext)
    mcp.registerModule("spotify", spotifyModule)
    
    val result = mcp.call("spotify", "play", bundleOf(
        "uri" to "spotify:track:123"
    ))
    
    assertEquals("success", result.getString("status"))
    verify(mockSpotifyApi).play("spotify:track:123")
}
```

### Integration Tests

```python
@pytest.mark.asyncio
async def test_calendar_mcp_integration():
    # Start MCP server
    server_process = await start_mcp_server("calendar_mcp_server.py")
    
    # Create agent with MCP
    agent = CalendarAgent()
    
    # Test tool execution
    result = await agent.execute_tool(
        "create_event",
        {
            "title": "Test Meeting",
            "start_time": "2024-01-01T10:00:00Z",
            "end_time": "2024-01-01T11:00:00Z"
        }
    )
    
    assert "eventId" in result
    
    # Cleanup
    server_process.terminate()
```

## Best Practices

1. **Module Design**
   - Keep modules focused on single responsibility
   - Use consistent naming conventions
   - Implement comprehensive error handling
   - Document all methods and parameters

2. **Security**
   - Always validate input parameters
   - Sanitize data before logging
   - Use least-privilege principle
   - Implement rate limiting

3. **Performance**
   - Cache frequently accessed data
   - Use connection pooling
   - Implement timeouts
   - Monitor resource usage

4. **Reliability**
   - Handle network failures gracefully
   - Implement retry logic
   - Provide fallback options
   - Log errors comprehensively

## Future Enhancements

1. **Dynamic Module Loading**
   - Hot-reload MCP modules
   - Plugin architecture
   - Version management

2. **Advanced Security**
   - Capability-based permissions
   - Encrypted communication
   - Secure enclaves for sensitive operations

3. **Performance Features**
   - Predictive caching
   - Parallel execution
   - GPU acceleration for AI operations

4. **Developer Tools**
   - MCP module generator
   - Testing framework
   - Performance profiler
   - Debug visualizer 