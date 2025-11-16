# Ticket #006: Agent Server Setup (LiveKit agents.js)

**Status**: Not Started
**Priority**: HIGH
**Dependencies**: TICKET_005 (LiveKit Android Integration)

---

## Objective

Set up the LiveKit agents.js server that will run the AI agents. This is the backend that processes voice, handles tool calls, and coordinates with the Android launcher.

---

## Background

agents.js is LiveKit's JavaScript/TypeScript framework for building AI agents. It runs as a separate server process (not in the Android app) and handles:
- Voice processing with STT/TTS
- AI model integration (Claude, GPT, etc.)
- Tool calling and execution
- Multi-agent coordination

Reference implementation: `/Users/samuellarson/Pebble/Github/stone-web-app-proto/` - This has a working agents implementation we need to adapt.

---

## Requirements

### Server Setup
- [ ] Install and configure LiveKit Agents SDK
- [ ] Set up Stone router agent (main assistant)
- [ ] Configure voice pipeline (STT → AI → TTS)
- [ ] Implement tool calling framework
- [ ] Connection endpoint for Android client

### Agent Implementation
- [ ] Router agent matching React prototype
- [ ] Tool definitions for device control
- [ ] RPC method handlers (routeToAgent, getAvailableAgents, healthCheck)
- [ ] Operation status broadcasting
- [ ] Error handling and logging

### Integration
- [ ] Token generation endpoint
- [ ] Room creation and management
- [ ] Agent dispatch system
- [ ] Connection with Android client

---

## Research Findings

### 1. LiveKit Agents Architecture Overview

**CRITICAL DISCOVERY**: The prototype uses **Python** for LiveKit agents, but LiveKit also provides **agents-js** (JavaScript/TypeScript) which is what we need for StoneOS.

**Two Distinct SDKs**:
- `livekit-server-sdk` (Node.js) - For token generation, room management, API calls
- `@livekit/agents` (agents-js) - For building AI agents with voice pipelines

**Agent Architecture**:
```
Token Server (Express.js)              Agent Server (agents-js)
↓                                      ↓
Generates room tokens                  Runs voice agents
Creates agent dispatch                 Handles STT → LLM → TTS
Returns connection details             Executes tool calls
                                       Joins LiveKit rooms
```

### 2. Installation and Setup

**Install Core Dependencies**:
```bash
npm install @livekit/agents
npm install @livekit/agents-plugin-silero     # VAD (Voice Activity Detection)
npm install @livekit/agents-plugin-livekit    # Turn detection
npm install @livekit/noise-cancellation-node  # Optional: Background noise cancellation

# Optional provider plugins (if not using inference gateway):
npm install @livekit/agents-plugin-openai
npm install @livekit/agents-plugin-deepgram
npm install @livekit/agents-plugin-elevenlabs
npm install @livekit/agents-plugin-cartesia
npm install @livekit/agents-plugin-google

# Token server dependencies:
npm install livekit-server-sdk
npm install express cors dotenv
```

**Environment Variables** (.env):
```bash
# Required for LiveKit connection
LIVEKIT_URL=wss://your-project.livekit.cloud
LIVEKIT_API_KEY=your_api_key_here
LIVEKIT_API_SECRET=your_api_secret_here

# Optional: Provider API keys if using plugins
OPENAI_API_KEY=sk-...
ASSEMBLYAI_API_KEY=...
CARTESIA_API_KEY=...
DEEPGRAM_API_KEY=...
ELEVENLABS_API_KEY=...
```

### 3. Agent Implementation Patterns

**Pattern 1: Simple Agent with Inference Gateway (Recommended)**
```typescript
import {
  type JobContext,
  type JobProcess,
  WorkerOptions,
  cli,
  defineAgent,
  llm,
  voice,
} from '@livekit/agents';
import * as silero from '@livekit/agents-plugin-silero';
import { fileURLToPath } from 'node:url';
import { z } from 'zod';

// Define tools for device control
const controlDevice = llm.tool({
  description: 'Control a device on the phone',
  parameters: z.object({
    action: z.enum(['open_app', 'play_music', 'navigate', 'send_message']),
    target: z.string().describe('App name or destination'),
  }),
  execute: async ({ action, target }, { ctx }) => {
    // Send control message to Android app via data channel
    const room = ctx.room;
    const controlData = JSON.stringify({
      type: 'device_control',
      action,
      target,
    });

    await room.localParticipant.publishData(
      Buffer.from(controlData),
      { reliable: true, topic: 'device_command' }
    );

    return { success: true, message: `Executed ${action} for ${target}` };
  },
});

export default defineAgent({
  // Prewarm: Load heavy models before handling jobs
  prewarm: async (proc: JobProcess) => {
    proc.userData.vad = await silero.VAD.load();
  },

  // Entry: Called when agent joins a room
  entry: async (ctx: JobContext) => {
    const agent = new voice.Agent({
      instructions: 'You are Stone, a minimalist AI assistant for Android.',
      tools: { controlDevice },
    });

    const session = new voice.AgentSession({
      // Use inference gateway (no API keys needed):
      stt: 'assemblyai/universal-streaming:en',
      llm: 'openai/gpt-4.1-mini',
      tts: 'cartesia/sonic-2:9626c31c-bec5-4cca-baa8-f8ba9e84c8bc',

      vad: ctx.proc.userData.vad as silero.VAD,
      turnDetection: new livekit.turnDetector.MultilingualModel(),
    });

    await session.start({
      agent,
      room: ctx.room,
    });

    // Generate initial greeting
    await session.generateReply({
      instructions: 'greet the user and ask what they need',
    });
  },
});

// Start worker
cli.runApp(new WorkerOptions({
  agent: fileURLToPath(import.meta.url)
}));
```

**Pattern 2: Multi-Agent with Tool Calling (From Prototype)**
```typescript
import { voice, llm, type JobContext } from '@livekit/agents';
import { z } from 'zod';

type UserData = {
  currentApp?: string;
  userIntent?: string;
};

class RouterAgent extends voice.Agent<UserData> {
  constructor() {
    super({
      instructions: 'You are Stone router. Route user requests to specialist agents.',
      tools: {
        delegateToMaps: llm.tool({
          description: 'Delegate navigation requests to maps agent',
          parameters: z.object({
            query: z.string().describe('User query about location/navigation'),
          }),
          execute: async ({ query }, { ctx }) => {
            // Store user context
            ctx.userData.userIntent = query;

            // Handoff to specialist agent
            return llm.handoff({
              agent: new MapsAgent(query),
              returns: 'Connecting you to navigation...',
            });
          },
        }),
      },
    });
  }

  async onEnter() {
    // Don't auto-greet - wait for user
    // this.session.generateReply(); // REMOVED
  }
}

class MapsAgent extends voice.Agent<UserData> {
  constructor(query: string) {
    super({
      instructions: `You are Stone Maps specialist. Handle navigation for: ${query}`,
      tools: {
        openMaps: llm.tool({
          description: 'Open Google Maps with location',
          parameters: z.object({
            location: z.string(),
          }),
          execute: async ({ location }, { ctx }) => {
            // Send command to Android app
            await ctx.room.localParticipant.publishData(
              Buffer.from(JSON.stringify({
                type: 'open_maps',
                location,
              })),
              { reliable: true, topic: 'device_command' }
            );
            return { success: true };
          },
        }),

        returnToRouter: llm.tool({
          description: 'Return to main router agent',
          parameters: z.object({}),
          execute: async ({}, { ctx }) => {
            return llm.handoff({
              agent: new RouterAgent(),
              returns: 'Returning to main assistant',
            });
          },
        }),
      },
    });
  }
}
```

### 4. Token Generation and Agent Dispatch

**Token Server (Express.js)**:
```typescript
import express from 'express';
import { AccessToken, RoomServiceClient } from 'livekit-server-sdk';
import dotenv from 'dotenv';
import cors from 'cors';

dotenv.config();

const app = express();
app.use(cors());

const livekitHost = process.env.LIVEKIT_URL!;
const apiKey = process.env.LIVEKIT_API_KEY!;
const apiSecret = process.env.LIVEKIT_API_SECRET!;

// Initialize room service for agent dispatch
const roomService = new RoomServiceClient(livekitHost, apiKey, apiSecret);

// Generate token for Android client
app.get('/api/connection-details', async (req, res) => {
  const roomName = `stone-${Date.now()}`;
  const participantName = `user-${Math.random().toString(36).substring(7)}`;

  try {
    // Create access token for client
    const token = new AccessToken(apiKey, apiSecret, {
      identity: participantName,
    });

    token.addGrant({
      room: roomName,
      roomJoin: true,
      canPublish: true,
      canSubscribe: true
    });

    const jwt = await token.toJwt();

    // Dispatch agent to room
    await roomService.createAgentDispatch({
      room: roomName,
      agentName: 'stone-router-agent',
      metadata: JSON.stringify({
        user_id: participantName,
        session_start: new Date().toISOString(),
      }),
    });

    res.json({
      participantName,
      roomName,
      participantToken: jwt,
      serverUrl: livekitHost,
    });
  } catch (err) {
    console.error('Error:', err);
    res.status(500).json({ error: err.message });
  }
});

app.listen(8000, () => {
  console.log('Token server running on http://localhost:8000');
});
```

### 5. RPC Methods and Operation Status Broadcasting

**Broadcasting Status to Android Client** (from prototype pattern):
```typescript
async function sendOperationStatus(
  ctx: JobContext,
  operation: string,
  busy: boolean,
  details: string = ''
) {
  const room = ctx.room;

  const statusData = {
    type: 'operation_status',
    agent: 'stone-router',
    busy,
    operation,
    details,
    timestamp: Date.now(),
  };

  await room.localParticipant.publishData(
    Buffer.from(JSON.stringify(statusData)),
    { reliable: true, topic: 'agent_status' }
  );
}

// Usage in tool execution:
async execute({ location }, { ctx }) {
  await sendOperationStatus(ctx, 'maps', true, 'Opening Google Maps');

  // Execute action
  await openMaps(location);

  await sendOperationStatus(ctx, 'maps', false, 'Maps opened');
  return { success: true };
}
```

**Receiving Data from Android** (listen for device state):
```typescript
session.on(voice.AgentSessionEventTypes.ConversationItemAdded, (ev) => {
  // Track conversation
});

// Listen for data from Android app
ctx.room.on('dataReceived', (payload, participant, kind, topic) => {
  if (topic === 'device_state') {
    const data = JSON.parse(payload.toString());
    console.log('Device state update:', data);
    // Update agent context based on device state
  }
});
```

### 6. Running the Agent

**Development Mode**:
```bash
# With debug logging
node agent.ts dev --log-level=debug

# Connect to specific room (testing)
node agent.ts connect --room=test-room --participant-identity=agent-test
```

**Production Mode**:
```bash
# Run worker that handles agent dispatch
node agent.ts start

# With custom port
PORT=8081 node agent.ts start
```

**Production Port Configuration** (from prototype):
```typescript
cli.runApp(new WorkerOptions({
  agent: fileURLToPath(import.meta.url),
  port: parseInt(process.env.AGENT_PORT || '8081'),
  numIdleProcesses: 3,
  loadThreshold: 0.7,
  production: true,
}));
```

### 7. Key Learnings from Prototype

**Critical Configuration Requirements** (from router_agent.py comments):

1. **Worker Options**: DO NOT include `agent_name` parameter in WorkerOptions - it breaks agent dispatch
2. **Agent Session**: Use `AgentSession(...)` without type parameters
3. **Entrypoint**: Must be `async def entrypoint(ctx: JobContext)` that calls `await ctx.connect()`
4. **Agent Dispatch Flow**:
   - UI generates unique room name
   - Token server calls `createDispatch(roomName, 'agent-name', metadata)`
   - LiveKit calls agent's entrypoint with room context
   - Agent joins room and starts session

**Auto-Greeting vs User-First**:
```typescript
// DON'T auto-greet (prototype pattern):
async onEnter() {
  // Wait for user to speak first
  // this.session.generateReply(); // REMOVED
}

// DO greet if needed:
async onEnter() {
  this.session.generateReply({
    instructions: 'greet the user warmly',
  });
}
```

### 8. Deployment Options

**Option 1: Cloud Run with Docker**:
```dockerfile
FROM node:20-slim

WORKDIR /app

COPY package*.json ./
RUN npm install --production

COPY . .
RUN npm run build

CMD ["node", "dist/agent.js", "start"]
```

**Option 2: Local Server**:
- Run on development machine for testing
- Use ngrok to expose to Android device
- Good for rapid iteration

**Option 3: VPS/Dedicated Server**:
- Run on DigitalOcean, AWS, GCP
- Use PM2 for process management
- Set up reverse proxy with nginx

**Recommended for StoneOS**: Start with local server, move to Docker on Cloud Run for production

### 9. Integration with Android

**Android Side** (React Native with LiveKit):
```typescript
// Connect to room
const room = new Room();
await room.connect(serverUrl, token);

// Listen for agent data
room.on(RoomEvent.DataReceived, (payload, participant, topic) => {
  if (topic === 'device_command') {
    const command = JSON.parse(payload);
    handleDeviceCommand(command);
  }
});

// Send device state to agent
room.localParticipant.publishData(
  JSON.stringify({ type: 'device_state', currentApp: 'spotify' }),
  { topic: 'device_state' }
);
```

---

## Implementation Plan

### Phase 1: Basic Agent Server Setup (Day 1)

**1.1 Project Structure**
```
stone-agent-server/
├── package.json
├── tsconfig.json
├── .env
├── src/
│   ├── index.ts              # Main agent entrypoint
│   ├── agents/
│   │   └── stone-router.ts   # Router agent
│   ├── tools/
│   │   ├── device-tools.ts   # Device control tools
│   │   └── app-tools.ts      # App-specific tools
│   ├── utils/
│   │   └── status.ts         # Operation status helpers
│   └── server/
│       └── token-server.ts   # Express token server
└── scripts/
    └── start.sh
```

**1.2 Install Dependencies**
```bash
mkdir stone-agent-server && cd stone-agent-server
npm init -y
npm install @livekit/agents @livekit/agents-plugin-silero @livekit/agents-plugin-livekit
npm install livekit-server-sdk express cors dotenv zod
npm install -D typescript @types/node @types/express @types/cors
npx tsc --init
```

**1.3 Configure Environment**
- Set up LiveKit Cloud account (or self-hosted)
- Generate API key and secret
- Create .env file with credentials

**1.4 Implement Basic Router Agent**
- Create stone-router.ts with simple greeting
- Implement prewarm for VAD loading
- Test connection with local room

**1.5 Implement Token Server**
- Create Express server for token generation
- Add agent dispatch integration
- Test token generation endpoint

**Success Criteria**: Agent can join a test room and speak a greeting

---

### Phase 2: Tool Calling Framework (Day 2)

**2.1 Define Device Control Tools**
```typescript
// device-tools.ts
export const openApp = llm.tool({
  description: 'Open an Android app',
  parameters: z.object({
    appName: z.string(),
  }),
  execute: async ({ appName }, { ctx }) => {
    await sendDeviceCommand(ctx, 'open_app', { app: appName });
    return { success: true };
  },
});

export const playMusic = llm.tool({
  description: 'Play music on Spotify',
  parameters: z.object({
    query: z.string(),
  }),
  execute: async ({ query }, { ctx }) => {
    await sendDeviceCommand(ctx, 'play_music', { query });
    return { success: true };
  },
});

export const navigate = llm.tool({
  description: 'Navigate to location',
  parameters: z.object({
    destination: z.string(),
  }),
  execute: async ({ destination }, { ctx }) => {
    await sendDeviceCommand(ctx, 'navigate', { destination });
    return { success: true };
  },
});
```

**2.2 Implement Operation Status Broadcasting**
- Create status helper functions
- Test status messages in Android app
- Implement busy/idle indicators

**2.3 Test Tool Execution**
- Verify tools are called correctly
- Check Android receives commands
- Validate error handling

**Success Criteria**: Agent can execute device control tools and Android receives commands

---

### Phase 3: Multi-Agent System (Day 3)

**3.1 Create Specialist Agents**
- MapsAgent for navigation
- SpotifyAgent for music control
- AppsAgent for app launching

**3.2 Implement Agent Handoff**
- Router delegates to specialists
- Specialists return to router
- Context preservation between agents

**3.3 Test Multi-Agent Flow**
- User asks for navigation → MapsAgent
- User asks for music → SpotifyAgent
- User asks general question → stays with router

**Success Criteria**: Seamless handoffs between agents with maintained context

---

### Phase 4: Production Readiness (Day 4)

**4.1 Metrics and Logging**
```typescript
const usageCollector = new metrics.UsageCollector();
session.on(voice.AgentSessionEventTypes.MetricsCollected, (ev) => {
  metrics.logMetrics(ev.metrics);
  usageCollector.collect(ev.metrics);
});
```

**4.2 Error Handling**
```typescript
session.on(voice.AgentSessionEventTypes.Error, (ev) => {
  console.error('Agent error:', ev.error);
  // Attempt recovery or graceful degradation
});
```

**4.3 Deployment Configuration**
- Create Dockerfile
- Set up environment variables
- Configure process management

**4.4 Documentation**
- API documentation
- Deployment guide
- Troubleshooting guide

**Success Criteria**: Agent runs reliably in production with proper monitoring

---

### Implementation Timeline

**Day 1**: Basic agent + token server working
**Day 2**: Tool calling functional with Android
**Day 3**: Multi-agent system operational
**Day 4**: Production deployment ready

**Total Estimated Time**: 4 days for complete implementation

---

## Files to Create/Modify

```
stone-agent-server/
├── package.json (NEW)
├── tsconfig.json (NEW)
├── .env (NEW)
├── src/
│   ├── index.ts (NEW)
│   ├── agents/
│   │   └── stone-router.ts (NEW)
│   ├── tools/
│   │   ├── device-tools.ts (NEW)
│   │   └── app-tools.ts (NEW)
│   └── utils/
│       └── connection.ts (NEW)
└── scripts/
    └── start.sh (NEW)
```

---

## Testing Criteria

- [ ] Agent server starts successfully
- [ ] Can generate tokens for room connection
- [ ] Android client can connect to agent
- [ ] Voice pipeline works (STT → AI → TTS)
- [ ] RPC methods respond correctly
- [ ] Tool calls execute properly
- [ ] Operation status messages broadcast

---

## Acceptance Criteria

- [ ] agents.js server configured and running
- [ ] Stone router agent implemented
- [ ] Voice chat working with Android client
- [ ] Tool calling framework operational
- [ ] All RPC methods from React prototype working
- [ ] Documentation complete

---

## Notes

- agents.js runs as a separate Node.js server
- Requires LiveKit server running (cloud or self-hosted)
- Reference the React prototype for exact behavior
- Consider Docker for deployment