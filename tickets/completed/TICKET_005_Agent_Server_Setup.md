# Ticket #005: Agent Server Setup (LiveKit agents.js)

**Status**: Not Started
**Priority**: HIGH
**Dependencies**: TICKET_004 (LiveKit Android Integration)

---

## Objective

Set up the LiveKit agents.js server that will run the AI agents. This is the backend that processes voice, handles tool calls, and coordinates with the Android launcher.

---

## Background

agents.js is LiveKit's JavaScript/TypeScript framework for building AI agents. It runs as a separate server process (not in the Android app) and handles:
- Voice processing
- AI model integration (Claude, etc.)
- Tool calling and execution
- Multi-agent coordination

---

## Requirements

### Server Setup
- [ ] Install and configure agents.js
- [ ] Set up Stone router agent
- [ ] Configure voice pipeline
- [ ] Implement tool calling framework
- [ ] Connection to Android client

### Agent Implementation
- [ ] Router agent (main Stone assistant)
- [ ] Tool definitions for device control
- [ ] RPC method handlers
- [ ] Error handling and logging

### Deployment
- [ ] Local development setup
- [ ] Production deployment strategy
- [ ] Environment configuration
- [ ] Security and authentication

---

## Implementation Plan

### Step 1: Install agents.js
```bash
# Create agent server directory
mkdir stone-agent-server
cd stone-agent-server

# Initialize project
npm init -y
npm install @livekit/agents @livekit/rtc-node

# TypeScript setup
npm install -D typescript @types/node
npx tsc --init
```

### Step 2: Create Stone Router Agent
```typescript
// src/agents/stone-router.ts
import { Agent, AgentContext, RpcRequest } from '@livekit/agents';

export class StoneRouterAgent extends Agent {
  async onRoomJoined(ctx: AgentContext) {
    console.log('Stone router agent joined room:', ctx.room.name);

    // Set up voice pipeline
    await this.setupVoicePipeline(ctx);

    // Register RPC handlers
    this.registerRpcHandlers(ctx);
  }

  private async setupVoicePipeline(ctx: AgentContext) {
    // TODO: Configure STT, TTS, and AI model
  }

  private registerRpcHandlers(ctx: AgentContext) {
    // WiFi control
    ctx.registerRpcMethod('setWifi', async (request: RpcRequest) => {
      // Implementation
      return { success: true };
    });

    // App launching
    ctx.registerRpcMethod('launchApp', async (request: RpcRequest) => {
      // Implementation
      return { success: true };
    });

    // Add more tool handlers...
  }
}
```

### Step 3: Configure Server
```typescript
// src/index.ts
import { WorkerOptions, cli } from '@livekit/agents';
import { StoneRouterAgent } from './agents/stone-router';

const worker = new WorkerOptions({
  agent: StoneRouterAgent,
  wsUrl: process.env.LIVEKIT_WS_URL || 'ws://localhost:7880',
  apiKey: process.env.LIVEKIT_API_KEY,
  apiSecret: process.env.LIVEKIT_API_SECRET,
});

cli.runApp(worker);
```

### Step 4: Tool Definitions
```typescript
// src/tools/device-tools.ts
export const deviceTools = {
  setWifi: {
    description: "Enable or disable WiFi",
    parameters: {
      enabled: { type: "boolean", required: true }
    },
    execute: async (params) => {
      // Send to Android client via data channel
      return { success: true };
    }
  },

  launchApp: {
    description: "Launch a Stone app",
    parameters: {
      appId: { type: "string", required: true }
    },
    execute: async (params) => {
      // Send to Android client
      return { success: true };
    }
  }
};
```

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
│       └── livekit-client.ts (NEW)
└── scripts/
    ├── start-dev.sh (NEW)
    └── deploy.sh (NEW)
```

---

## Testing Criteria

- [ ] Agent server starts successfully
- [ ] Can connect from Android client
- [ ] Voice pipeline works (STT → AI → TTS)
- [ ] Tool calls execute properly
- [ ] Data channels communicate with client
- [ ] Error handling works

---

## Acceptance Criteria

- [ ] agents.js server configured and running
- [ ] Stone router agent implemented
- [ ] Tool calling framework in place
- [ ] Connection with Android client established
- [ ] Voice chat working end-to-end
- [ ] Documentation complete

---

## Research Findings

*This section will be filled by research agent if needed*

---

## Next Steps

After this ticket:
1. TICKET_006: Implement tool calling API
2. TICKET_007: Add WiFi control feature
3. Continue with device features

---

## Notes

- agents.js runs as separate server, not in Android app
- Consider deployment strategy early
- Security is important for production
- Keep tool definitions modular