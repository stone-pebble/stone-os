# Stone Agent Server

LiveKit agents.js server for StoneOS AI agent processing. This server handles voice processing, AI model integration, tool calling, and agent dispatch for the Stone Launcher Android app.

## Architecture Overview

```
Android App (LiveKit SDK)
    ↓
Token Server (port 8000)
    ├─ Generates room tokens
    ├─ Dispatches agents
    └─ Returns connection details
    ↓
Agent Server (port 8081)
    ├─ Voice pipeline (STT → LLM → TTS)
    ├─ Tool calling (device control)
    └─ Agent routing
    ↓
LiveKit Cloud (or self-hosted)
```

## Features

- **Voice Pipeline**: STT → AI → TTS using LiveKit Inference Gateway
- **Router Agent**: Main assistant that handles general requests
- **Device Control Tools**: Control Android phone functions via data channels
- **Agent Dispatch**: Automatic agent deployment when Android client connects
- **Multi-Agent System**: Route to specialist agents for complex tasks

## Prerequisites

- Node.js 20.x or higher
- npm or yarn package manager
- LiveKit Cloud account (or self-hosted LiveKit server)

## Installation

```bash
# Navigate to stone-agent directory
cd stone-agent

# Install dependencies
npm install

# Create environment configuration
cp .env.example .env
```

## Configuration

Edit `.env` with your LiveKit credentials:

```bash
# LiveKit Connection (Required)
LIVEKIT_URL=wss://your-project.livekit.cloud
LIVEKIT_API_KEY=your_api_key_here
LIVEKIT_API_SECRET=your_api_secret_here

# Server Ports
AGENT_PORT=8081
TOKEN_SERVER_PORT=8000

# Optional: AI Provider API Keys
# If not provided, uses LiveKit Inference Gateway (recommended for development)
OPENAI_API_KEY=sk-...
```

### Getting LiveKit Credentials

1. Sign up at [LiveKit Cloud](https://cloud.livekit.io/)
2. Create a new project
3. Copy your project URL, API Key, and API Secret
4. Add them to your `.env` file

## Development

### Running in Development Mode

```bash
# Terminal 1: Start the agent server
npm run dev

# Terminal 2: Start the token server
npm run token-server
```

The agent server will:
- Listen on port 8081 (configurable via `AGENT_PORT`)
- Connect to LiveKit Cloud
- Wait for agent dispatch requests

The token server will:
- Listen on port 8000 (configurable via `TOKEN_SERVER_PORT`)
- Generate room tokens for Android clients
- Dispatch agents to rooms automatically

### Building for Production

```bash
# Compile TypeScript to JavaScript
npm run build

# Run compiled code
npm start
```

## Usage

### Android App Integration

The Android app should call the token server to get connection details:

```kotlin
// In Android app
val response = httpClient.get("http://localhost:8000/api/connection-details")
val connectionDetails = response.body<ConnectionDetails>()

// Connect to room with token
room.connect(
    url = connectionDetails.serverUrl,
    token = connectionDetails.participantToken
)
```

### Available Endpoints

**Token Server:**
- `GET /api/connection-details?roomName=xxx&participantId=yyy` - Get room token and dispatch agent
- `GET /api/agents` - List available agents
- `GET /health` - Health check

### Data Channel Topics

The agent uses these data channel topics to communicate with Android:

- `agent_operation_status` - Agent busy/idle status updates
- `device_command` - Commands from agent to Android (control device)
- `device_state` - State updates from Android to agent

### Operation Status Messages

Format sent on `agent_operation_status` topic:

```json
{
  "type": "operation_status",
  "agent": "router",
  "busy": true,
  "operation": "opening_app",
  "details": "Opening Spotify",
  "timestamp": 1699999999999
}
```

### Device Command Messages

Format sent on `device_command` topic:

```json
{
  "type": "device_control",
  "action": "open_app",
  "params": {
    "app": "Spotify"
  },
  "timestamp": 1699999999999
}
```

## Agent Tools

The router agent has access to these device control tools:

- `openApp(appName)` - Open an Android app
- `playMusic(query)` - Play music on Spotify
- `navigate(destination)` - Navigate to a location
- `sendMessage(recipient, message)` - Send a text message
- `makeCall(contact)` - Make a phone call
- `setAlarm(time, label?)` - Set an alarm
- `setTimer(duration, label?)` - Set a countdown timer
- `controlSettings(setting, action, value?)` - Control WiFi, Bluetooth, brightness, volume

## Project Structure

```
stone-agent/
├── src/
│   ├── index.ts                  # Main agent entry point
│   ├── agents/
│   │   └── stone-router.ts       # Router agent implementation
│   ├── tools/
│   │   └── device-tools.ts       # Device control tools
│   ├── utils/
│   │   ├── status.ts             # Operation status helpers
│   │   └── types.ts              # Type definitions
│   └── server/
│       └── token-server.ts       # Express token server
├── package.json
├── tsconfig.json
├── .env.example
└── README.md
```

## Testing

### Test Agent Connection

```bash
# Start agent server
npm run dev

# In another terminal, test connection
curl http://localhost:8081/debug
```

### Test Token Generation

```bash
# Start token server
npm run token-server

# Generate test token
curl "http://localhost:8000/api/connection-details?roomName=test-room&participantId=test-user"
```

### Test with Android App

1. Start both agent and token servers
2. Update Android app configuration to point to token server
3. Launch Android app and initiate voice chat
4. Watch logs for agent activity

## Troubleshooting

### Agent Not Connecting

- Verify `LIVEKIT_URL`, `LIVEKIT_API_KEY`, and `LIVEKIT_API_SECRET` are correct
- Check agent server logs for connection errors
- Ensure no firewall blocking WebSocket connections

### Token Generation Fails

- Verify token server is running on correct port
- Check LiveKit credentials in `.env`
- Ensure room name format is valid

### Agent Doesn't Respond

- Check agent server logs for errors
- Verify VAD model loaded successfully during prewarm
- Check LiveKit Cloud dashboard for room activity

## Deployment

### Docker Deployment

```dockerfile
FROM node:20-slim

WORKDIR /app

COPY package*.json ./
RUN npm install --production

COPY . .
RUN npm run build

CMD ["node", "dist/index.js", "start"]
```

Build and run:

```bash
docker build -t stone-agent .
docker run -p 8081:8081 --env-file .env stone-agent
```

### Environment Variables for Production

```bash
NODE_ENV=production
AGENT_PORT=8081
TOKEN_SERVER_PORT=8000
LIVEKIT_URL=wss://your-project.livekit.cloud
LIVEKIT_API_KEY=your_key
LIVEKIT_API_SECRET=your_secret
```

## Key Learnings from Prototype

### Critical Configuration Requirements

1. **DO NOT** include `agentName` in `WorkerOptions` - this breaks agent dispatch
2. **DO** use inference gateway pattern for easy setup (no API keys needed)
3. **DO** wait for user to speak first (no auto-greeting)
4. **DO** use Silero VAD for voice activity detection

### Agent Dispatch Flow

1. Android app calls token server: `/api/connection-details`
2. Token server generates room token
3. Token server calls `createAgentDispatch(roomName, 'stone-router-agent', metadata)`
4. LiveKit calls agent's `entry` function
5. Agent joins room and starts voice session
6. User and agent can now communicate

## Contributing

When adding new features:

1. Add new tools to `src/tools/`
2. Update agent instructions in `src/agents/stone-router.ts`
3. Test with Android app integration
4. Update this README with new capabilities

## License

Part of the StoneOS project. See main repository for license information.

## Support

For issues or questions:
- Check the troubleshooting section above
- Review LiveKit agents.js documentation
- See the React prototype implementation at `/stone-web-app-proto/`
