# LiveKit Agents.js Setup Guide for StoneOS

## Quick Start

### 1. Install Dependencies
```bash
npm install @livekit/agents @livekit/agents-plugin-silero @livekit/agents-plugin-livekit
npm install livekit-server-sdk express cors dotenv zod
npm install -D typescript @types/node @types/express @types/cors
```

### 2. Environment Setup (.env)
```bash
LIVEKIT_URL=wss://your-project.livekit.cloud
LIVEKIT_API_KEY=your_api_key
LIVEKIT_API_SECRET=your_api_secret
```

### 3. Minimal Working Agent
```typescript
import { defineAgent, voice, type JobContext, type JobProcess, WorkerOptions, cli } from '@livekit/agents';
import * as silero from '@livekit/agents-plugin-silero';
import { fileURLToPath } from 'node:url';

export default defineAgent({
  prewarm: async (proc: JobProcess) => {
    proc.userData.vad = await silero.VAD.load();
  },

  entry: async (ctx: JobContext) => {
    const agent = new voice.Agent({
      instructions: 'You are Stone, a minimalist AI assistant.',
    });

    const session = new voice.AgentSession({
      stt: 'assemblyai/universal-streaming:en',
      llm: 'openai/gpt-4.1-mini',
      tts: 'cartesia/sonic-2:9626c31c-bec5-4cca-baa8-f8ba9e84c8bc',
      vad: ctx.proc.userData.vad as silero.VAD,
    });

    await session.start({ agent, room: ctx.room });
  },
});

cli.runApp(new WorkerOptions({ agent: fileURLToPath(import.meta.url) }));
```

### 4. Run Agent
```bash
# Development
node agent.ts dev --log-level=debug

# Production
node agent.ts start
```

## Architecture

### Two Components Required

**1. Token Server (Express.js)**
- Generates JWT tokens for clients
- Creates agent dispatch requests
- Manages room lifecycle

**2. Agent Server (agents-js)**
- Runs voice pipeline (STT → LLM → TTS)
- Executes tool calls
- Joins LiveKit rooms

```
Android App → Token Server → LiveKit Cloud ← Agent Server
                ↓                              ↓
            JWT Token                    Voice Pipeline
            Room Name                    Tool Execution
```

## Token Server Example
```typescript
import express from 'express';
import { AccessToken, RoomServiceClient } from 'livekit-server-sdk';

const app = express();
const roomService = new RoomServiceClient(
  process.env.LIVEKIT_URL,
  process.env.LIVEKIT_API_KEY,
  process.env.LIVEKIT_API_SECRET
);

app.get('/api/connection-details', async (req, res) => {
  const roomName = `stone-${Date.now()}`;
  const participantName = `user-${Math.random().toString(36).substring(7)}`;

  const token = new AccessToken(apiKey, apiSecret, { identity: participantName });
  token.addGrant({ room: roomName, roomJoin: true, canPublish: true, canSubscribe: true });
  const jwt = await token.toJwt();

  // Dispatch agent to room
  await roomService.createAgentDispatch({
    room: roomName,
    agentName: 'stone-router-agent',
    metadata: JSON.stringify({ user_id: participantName }),
  });

  res.json({ participantName, roomName, participantToken: jwt, serverUrl: livekitHost });
});

app.listen(8000);
```

## Tool Calling Pattern
```typescript
import { llm, type JobContext } from '@livekit/agents';
import { z } from 'zod';

const openApp = llm.tool({
  description: 'Open an Android app',
  parameters: z.object({
    appName: z.string(),
  }),
  execute: async ({ appName }, { ctx }) => {
    // Send command to Android via data channel
    await ctx.room.localParticipant.publishData(
      Buffer.from(JSON.stringify({ type: 'open_app', app: appName })),
      { reliable: true, topic: 'device_command' }
    );
    return { success: true };
  },
});

// Use in agent
const agent = new voice.Agent({
  instructions: 'You are Stone assistant',
  tools: { openApp },
});
```

## Multi-Agent Handoff
```typescript
class RouterAgent extends voice.Agent {
  constructor() {
    super({
      instructions: 'Route user requests to specialists',
      tools: {
        delegateToMaps: llm.tool({
          description: 'Delegate to maps specialist',
          parameters: z.object({ query: z.string() }),
          execute: async ({ query }, { ctx }) => {
            return llm.handoff({
              agent: new MapsAgent(query),
              returns: 'Connecting to navigation...',
            });
          },
        }),
      },
    });
  }
}

class MapsAgent extends voice.Agent {
  constructor(query: string) {
    super({
      instructions: `Handle navigation for: ${query}`,
      tools: { /* maps tools */ },
    });
  }
}
```

## Broadcasting Status to Android
```typescript
async function sendOperationStatus(ctx: JobContext, operation: string, busy: boolean, details: string = '') {
  await ctx.room.localParticipant.publishData(
    Buffer.from(JSON.stringify({
      type: 'operation_status',
      agent: 'stone-router',
      busy,
      operation,
      details,
      timestamp: Date.now(),
    })),
    { reliable: true, topic: 'agent_status' }
  );
}

// Usage
await sendOperationStatus(ctx, 'maps', true, 'Opening Google Maps');
// ... perform action ...
await sendOperationStatus(ctx, 'maps', false, 'Maps opened');
```

## Receiving Data from Android
```typescript
ctx.room.on('dataReceived', (payload, participant, kind, topic) => {
  if (topic === 'device_state') {
    const data = JSON.parse(payload.toString());
    console.log('Device state:', data);
    // Update agent context
  }
});
```

## Critical Configuration Notes

### DO NOT Include agent_name in WorkerOptions
```typescript
// ❌ WRONG - breaks agent dispatch
cli.runApp(new WorkerOptions({
  agent: fileURLToPath(import.meta.url),
  agent_name: 'stone-router', // DON'T DO THIS
}));

// ✅ CORRECT
cli.runApp(new WorkerOptions({
  agent: fileURLToPath(import.meta.url),
}));
```

### Auto-Greeting Considerations
```typescript
// Wait for user to speak first (recommended for StoneOS)
async onEnter() {
  // Don't auto-greet
}

// Or greet explicitly
async onEnter() {
  this.session.generateReply({ instructions: 'greet the user warmly' });
}
```

## Development Commands
```bash
# Install dependencies
npm install

# Run in development mode
node agent.ts dev --log-level=debug

# Connect to specific room (testing)
node agent.ts connect --room=test-room

# Run token server
node token-server.ts

# Production mode
node agent.ts start
```

## Deployment Options

### Option 1: Local Server (Recommended for Development)
```bash
# Run agent server
node agent.ts start

# Run token server
node token-server.ts

# Use ngrok to expose to Android
ngrok http 8000
```

### Option 2: Docker (Production)
```dockerfile
FROM node:20-slim
WORKDIR /app
COPY package*.json ./
RUN npm install --production
COPY . .
RUN npm run build
CMD ["node", "dist/agent.js", "start"]
```

### Option 3: Cloud Run
- Build Docker image
- Push to Google Container Registry
- Deploy to Cloud Run with environment variables

## Monitoring and Metrics
```typescript
import { metrics } from '@livekit/agents';

const usageCollector = new metrics.UsageCollector();

session.on(voice.AgentSessionEventTypes.MetricsCollected, (ev) => {
  metrics.logMetrics(ev.metrics);
  usageCollector.collect(ev.metrics);
});

session.on(voice.AgentSessionEventTypes.Error, (ev) => {
  console.error('Agent error:', ev.error);
});

// On shutdown
const summary = usageCollector.getSummary();
console.log('Total usage:', summary);
```

## Android Integration
```typescript
import { Room, RoomEvent } from 'livekit-client';

// Connect to room
const room = new Room();
await room.connect(serverUrl, token);

// Listen for agent commands
room.on(RoomEvent.DataReceived, (payload, participant, topic) => {
  if (topic === 'device_command') {
    const command = JSON.parse(payload);
    handleDeviceCommand(command); // Execute on Android
  }
});

// Send device state to agent
room.localParticipant.publishData(
  JSON.stringify({ type: 'device_state', currentApp: 'spotify' }),
  { topic: 'device_state' }
);
```

## Reference Links
- [LiveKit Agents Documentation](https://docs.livekit.io/agents/)
- [agents-js GitHub](https://github.com/livekit/agents-js)
- [LiveKit Inference Gateway](https://docs.livekit.io/agents/models/)
- [Agent Dispatch Guide](https://docs.livekit.io/agents/worker/agent-dispatch/)
- [Stone Prototype](file:///Users/samuellarson/Pebble/Github/stone-web-app-proto)

## Next Steps
1. Set up LiveKit Cloud account
2. Create stone-agent-server project
3. Implement basic router agent
4. Set up token server
5. Test connection from Android
6. Add tool calling for device control
7. Implement multi-agent system
8. Deploy to production
