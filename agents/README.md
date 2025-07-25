# StoneOS AI Agents & MCP Integration

## Overview

StoneOS leverages LiveKit Agents and Model Context Protocol (MCP) to create a powerful, extensible AI system. This architecture enables natural language interaction, tool integration, and seamless orchestration of device capabilities.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    User Interface                        │
│                  (React Native Shell)                    │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│                   LiveKit Server                         │
│              (Real-time Communication)                   │
└─────────────────────────┬───────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────┐
│                   Router Agent                           │
│              (Central Orchestrator)                      │
└─────────────┬───────────────────────────┬───────────────┘
              │                           │
┌─────────────┴──────────┐   ┌───────────┴───────────────┐
│   Specialized Agents   │   │      MCP Servers          │
│  - Ask (Search)        │   │  - Spotify Control       │
│  - Think (Notes)       │   │  - Maps Integration      │
│  - Go (Navigation)     │   │  - Calendar Access       │
│  - Listen (Music)      │   │  - Payment Processing    │
│  - Calc (Math)         │   │  - File System           │
└────────────────────────┘   └───────────────────────────┘
```

## Core Components

### 1. LiveKit Infrastructure

LiveKit provides the real-time communication backbone:

```python
# Agent session configuration
session = AgentSession(
    vad=silero.VAD.load(),                    # Voice Activity Detection
    stt=deepgram.STT(model="nova-3"),         # Speech-to-Text
    llm=openai.LLM(model="gpt-4o"),           # Language Model
    tts=openai.TTS(voice="alloy"),            # Text-to-Speech
    turn_detection=MultilingualModel(),        # Turn detection
    mcp_servers=[...]                          # MCP tool servers
)
```

### 2. Router Agent

The central orchestrator that handles user requests and delegates to specialized agents:

```python
class RouterAgent(MemoryAgent):
    """Main entry point for all user interactions"""
    
    def __init__(self):
        super().__init__(
            instructions="""
            You are the main assistant for StoneOS. 
            Understand user intent and delegate to specialized agents:
            - Ask Agent: Web searches and information queries
            - Think Agent: Note-taking and document management
            - Go Agent: Navigation and location services
            - Listen Agent: Music and audio control
            - Calc Agent: Mathematical computations
            """,
            tools=[
                self.delegate_to_ask_agent,
                self.delegate_to_think_agent,
                self.delegate_to_go_agent,
                self.delegate_to_listen_agent,
                self.delegate_to_calc_agent
            ]
        )
```

### 3. Specialized Agents

Each agent handles specific domains with dedicated MCP servers:

#### Ask Agent (Web Search)
```python
# Uses Perplexity MCP server for web searches
mcp_servers=[
    mcp.MCPServerStdio(
        command='node',
        args=['server-perplexity-ask'],
        env={'PERPLEXITY_API_KEY': os.environ['PERPLEXITY_API_KEY']}
    )
]
```

#### Think Agent (Notes & Documents)
```python
# File system MCP server for note management
mcp_servers=[
    mcp.MCPServerStdio(
        command='node',
        args=['filesystem-server'],
        env={'ALLOWED_DIRECTORIES': '/data/notes'}
    )
]
```

#### Go Agent (Navigation)
```python
# Google Maps MCP integration
mcp_servers=[
    mcp.MCPServerStdio(
        command='node',
        args=['google-maps-server'],
        env={'GOOGLE_MAPS_API_KEY': os.environ['GOOGLE_MAPS_API_KEY']}
    )
]
```

#### Listen Agent (Music)
```python
# Spotify MCP server
mcp_servers=[
    mcp.MCPServerStdio(
        command='node',
        args=['spotify-server'],
        env={'SPOTIFY_CONFIG': '/etc/spotify/config.json'}
    )
]
```

## MCP Server Implementation

### What is MCP?

Model Context Protocol (MCP) is Anthropic's standard for exposing tools and resources to LLMs. It provides:

- **Standardized Interface**: Consistent tool exposure across languages
- **Security**: Sandboxed execution with permission controls
- **Composability**: Agents can use multiple MCP servers
- **Language Agnostic**: Servers can be written in any language

### MCP Server Structure

```javascript
// Example: Spotify MCP Server
export const server = new Server({
    name: 'spotify-control',
    version: '1.0.0'
});

// Define available tools
server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: 'play_track',
            description: 'Play a specific track on Spotify',
            inputSchema: {
                type: 'object',
                properties: {
                    track_id: { type: 'string' },
                    device_id: { type: 'string', optional: true }
                }
            }
        },
        {
            name: 'search_music',
            description: 'Search for music on Spotify',
            inputSchema: {
                type: 'object',
                properties: {
                    query: { type: 'string' },
                    type: { type: 'string', enum: ['track', 'album', 'artist'] }
                }
            }
        }
    ]
}));

// Implement tool handlers
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    switch (request.params.name) {
        case 'play_track':
            return await playTrack(request.params.arguments);
        case 'search_music':
            return await searchMusic(request.params.arguments);
    }
});
```

### Creating a New MCP Server

1. **Define the interface**:
```typescript
interface CalendarServer {
    createEvent(event: CalendarEvent): Promise<string>;
    getEvents(date: Date): Promise<Event[]>;
    updateEvent(id: string, updates: Partial<Event>): Promise<void>;
    deleteEvent(id: string): Promise<void>;
}
```

2. **Implement the server**:
```python
# Python MCP server example
from mcp.server import Server
from mcp.server.stdio import stdio_server

app = Server("calendar-server")

@app.tool()
async def create_event(
    title: str,
    start_time: str,
    end_time: str,
    description: str = ""
) -> str:
    """Create a new calendar event"""
    # Integration with Calendar Provider API
    event = await calendar_api.create_event(
        title=title,
        start=parse_time(start_time),
        end=parse_time(end_time),
        description=description
    )
    return f"Event created with ID: {event.id}"
```

3. **Register with StoneOS**:
```kotlin
// In MasterControlProgram.kt
class MasterControlProgram {
    private val mcpServers = mutableMapOf<String, MCPServer>()
    
    init {
        registerMCPServer("calendar", CalendarMCPServer())
        registerMCPServer("contacts", ContactsMCPServer())
        registerMCPServer("email", EmailMCPServer())
    }
}
```

## Agent Development Guide

### Creating a New Agent

1. **Define the agent's purpose**:
```python
class WeatherAgent(Agent):
    """Specialized agent for weather information and forecasts"""
    
    def __init__(self):
        super().__init__(
            instructions="""
            You are a weather specialist. Provide accurate weather 
            information, forecasts, and weather-related advice.
            Use the weather MCP tools to get current conditions
            and forecasts for any location.
            """,
            mcp_servers=[
                mcp.MCPServerStdio(
                    command='python',
                    args=['weather_server.py'],
                    env={'WEATHER_API_KEY': os.environ['WEATHER_API_KEY']}
                )
            ]
        )
```

2. **Add delegation in Router Agent**:
```python
@function_tool
async def delegate_to_weather_agent(self) -> str:
    """Delegate weather-related queries to the Weather Agent"""
    return "weather"  # Agent identifier for handoff
```

3. **Register the agent**:
```python
# In agent_registry.py
AGENT_REGISTRY = {
    "router": RouterAgent,
    "ask": AskAgent,
    "think": ThinkAgent,
    "go": GoAgent,
    "listen": ListenAgent,
    "calc": CalcAgent,
    "weather": WeatherAgent  # New agent
}
```

### Agent Communication Patterns

#### 1. Direct Delegation
```python
# Router hands off to specialized agent
async def on_user_message(self, message: str):
    if "weather" in message.lower():
        await self.delegate_to_weather_agent()
```

#### 2. Tool-Based Interaction
```python
# Agent uses MCP tools
result = await self.call_tool(
    "get_weather",
    {"location": "San Francisco", "units": "fahrenheit"}
)
```

#### 3. Multi-Agent Collaboration
```python
# Agents can work together
weather = await self.delegate_to_weather_agent()
calendar = await self.delegate_to_calendar_agent()
# Combine results for user
```

## Integration with StoneOS

### 1. Native Bridge Access

Agents access device capabilities through the MCP layer:

```javascript
// UI calls agent via bridge
window.StoneOS.agent.sendMessage("Play my workout playlist");

// Agent processes and calls MCP
await mcp.spotify.play(playlistId: "workout_mix");

// MCP calls native Android service
MasterControlProgram.spotify.play("workout_mix");
```

### 2. Permission Management

```kotlin
// MCP checks permissions before executing
class SpotifyMCPModule : MCPModule {
    override suspend fun play(trackId: String): Result<PlaybackState> {
        // Check if app has Spotify access permission
        if (!permissionManager.hasPermission("com.stoneos.permission.SPOTIFY")) {
            return Result.failure(PermissionDeniedException())
        }
        
        // Execute Spotify SDK call
        return spotifySDK.play(trackId)
    }
}
```

### 3. State Synchronization

```python
# Agents maintain conversation context
class AgentSession:
    def __init__(self):
        self.history = ChatContext()
        self.user_preferences = {}
        self.active_tasks = []
    
    async def on_user_input(self, input: str):
        # Add to history
        self.history.add_user_message(input)
        
        # Process with context
        response = await self.process_with_context(input)
        
        # Update UI
        await self.send_to_ui(response)
```

## Testing Agents

### 1. Local Testing

```bash
# Test agent without full StoneOS
cd agents
python -m pytest tests/test_weather_agent.py

# Test with mock MCP server
python test_with_mock_mcp.py
```

### 2. Integration Testing

```python
# Test agent with real MCP servers
async def test_spotify_integration():
    agent = ListenAgent()
    session = TestSession()
    
    # Simulate user request
    await session.send_message("Play some jazz music")
    
    # Verify MCP calls
    assert session.mcp_calls[0].tool == "search_music"
    assert session.mcp_calls[1].tool == "play_track"
```

### 3. End-to-End Testing

```bash
# Deploy to test device
./deploy_agent.sh weather_agent

# Run E2E test suite
adb shell am instrument -w com.stoneos.test/AgentTestRunner
```

## Performance Optimization

### 1. Agent Initialization

```python
# Lazy load MCP servers
class OptimizedAgent(Agent):
    def __init__(self):
        self._mcp_servers = None
    
    @property
    def mcp_servers(self):
        if self._mcp_servers is None:
            self._mcp_servers = self._load_mcp_servers()
        return self._mcp_servers
```

### 2. Response Caching

```python
# Cache common responses
@cache_response(ttl=300)  # 5 minutes
async def get_weather(location: str):
    return await mcp.weather.get_current(location)
```

### 3. Parallel Processing

```python
# Execute multiple MCP calls in parallel
async def get_dashboard_data():
    weather, calendar, news = await asyncio.gather(
        mcp.weather.get_current("current_location"),
        mcp.calendar.get_today_events(),
        mcp.news.get_headlines()
    )
    return combine_dashboard(weather, calendar, news)
```

## Security Considerations

### 1. MCP Sandboxing

```python
# MCP servers run in isolated processes
mcp_server = mcp.MCPServerStdio(
    command='python',
    args=['server.py'],
    cwd='/sandbox/weather',  # Restricted directory
    env={
        'HOME': '/sandbox/weather',
        'PATH': '/sandbox/bin:/usr/bin'
    }
)
```

### 2. Permission Checks

```kotlin
// Every MCP call requires permission verification
@RequiresPermission("com.stoneos.permission.CALENDAR_WRITE")
suspend fun createEvent(event: CalendarEvent): String {
    // Implementation
}
```

### 3. Data Privacy

```python
# Agents don't store sensitive data
class PrivacyAwareAgent(Agent):
    async def on_sensitive_data(self, data):
        # Process without storing
        result = await self.process_secure(data)
        
        # Clear from memory
        del data
        
        return result
```

## Future Enhancements

1. **On-Device Models**: Run smaller models locally for privacy
2. **Agent Marketplace**: Third-party agent development
3. **Multi-Modal Agents**: Vision and audio processing
4. **Proactive Agents**: Anticipate user needs
5. **Agent Learning**: Personalization over time 