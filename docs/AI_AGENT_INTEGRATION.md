# Stone Launcher - AI Agent Integration

**Purpose**: Integrate voice-controlled AI agents with Stone Launcher
**Last Updated**: November 12, 2025

---

## Agent Architecture

### Main Agent + Sub-Agents with Dynamic Tool Loading

Stone uses a **main agent** that dynamically loads tool sets from **sub-agents** based on context.

```
Main Agent (Headless/Notification Layer)
    │
    ├─ Activate Sub-Agent Tool → Exposes tool index
    │   │
    │   ├─ TICK Agent (time tools)
    │   ├─ CONNECT Agent (communication tools)
    │   ├─ GO Agent (navigation tools)
    │   ├─ LISTEN Agent (music tools)
    │   ├─ ASK Agent (search tools)
    │   ├─ PLAN Agent (calendar tools)
    │   ├─ THINK Agent (notes tools)
    │   ├─ SET Agent (settings tools)
    │   ├─ REFLECT Agent (journal tools)
    │   ├─ TASK Agent (app management tools)
    │   └─ FUND Agent (minimal - just open wallet)
    │
    └─ Deactivate Sub-Agent → Removes tools from context
```

### How It Works

#### 1. User Makes Request

```
User: "Play some jazz music"
```

#### 2. Main Agent Recognizes Context

Main agent determines this needs LISTEN agent tools.

#### 3. Main Agent Activates Sub-Agent

```typescript
// Main agent calls this tool:
activate_listen_agent()

// This returns the full tool index for LISTEN:
{
  tools: [
    { name: "play_music", description: "...", parameters: {...} },
    { name: "control_playback", description: "...", parameters: {...} },
    { name: "create_playlist", description: "...", parameters: {...} },
    { name: "search_music", description: "...", parameters: {...} }
  ]
}

// These tools are now available to main agent in next response
```

#### 4. Main Agent Uses Sub-Agent Tools

```typescript
// Main agent can now call:
play_music({ query: "jazz music" })

// This tool sends Intent to launcher:
Intent: com.stone.launcher.action.PLAY_MUSIC
Extras: { query: "jazz music" }
```

#### 5. Main Agent Deactivates Sub-Agent

```typescript
// When done with task:
deactivate_listen_agent()

// This removes all LISTEN tools from context
// Frees up context window
```

#### 6. Main Agent Responds

```
Stone: "Playing jazz music"
```

---

## Sub-Agent Memory System

### Each Sub-Agent Has Own Memory

**Using mem0**:
- TICK Agent: Remembers common alarm times, timer durations
- CONNECT Agent: Remembers frequently called contacts, typical messages
- LISTEN Agent: Remembers music preferences, favorite playlists
- GO Agent: Remembers frequent destinations ("home", "work")
- PLAN Agent: Remembers common meeting times, recurring events
- THINK Agent: Remembers note categories, tagging patterns
- SET Agent: Remembers preferred settings configurations

### Main Agent Has Overarching Context

- Full conversation history
- Cross-app context (e.g., "call the person I'm meeting tomorrow")
- Long-term user preferences
- Delegation history

### Memory Inheritance

When main agent activates a sub-agent:
- Sub-agent receives conversation context from main agent
- Sub-agent accesses own mem0 instance for app-specific memory
- Both memories inform the agent's responses

---

## Tool Activation/Deactivation Pattern

### Implementation (TypeScript/Node.js Example)

```typescript
// stone-agent/src/toolActivation.ts

interface SubAgentTools {
  tick: TickTools;
  connect: ConnectTools;
  go: GoTools;
  listen: ListenTools;
  ask: AskTools;
  plan: PlanTools;
  think: ThinkTools;
  set: SetTools;
  reflect: ReflectTools;
  task: TaskTools;
  fund: FundTools;
}

class ToolActivationManager {
  private activeTools: Set<string> = new Set();
  private toolRegistry: SubAgentTools;
  private intentBridge: IntentApiBridge;

  constructor(intentBridge: IntentApiBridge) {
    this.intentBridge = intentBridge;
    this.toolRegistry = this.initializeToolRegistry();
  }

  /**
   * Activation tools - these are ALWAYS available to main agent
   */
  getActivationTools() {
    return [
      {
        name: 'activate_tick_agent',
        description: 'Load time management tools (alarms, timers, clock)',
        parameters: {},
        execute: async () => {
          return this.activateSubAgent('tick');
        }
      },
      {
        name: 'activate_connect_agent',
        description: 'Load communication tools (calls, SMS, email, contacts)',
        parameters: {},
        execute: async () => {
          return this.activateSubAgent('connect');
        }
      },
      {
        name: 'activate_go_agent',
        description: 'Load navigation tools (maps, directions, places)',
        parameters: {},
        execute: async () => {
          return this.activateSubAgent('go');
        }
      },
      {
        name: 'activate_listen_agent',
        description: 'Load music control tools (Spotify playback, playlists)',
        parameters: {},
        execute: async () => {
          return this.activateSubAgent('listen');
        }
      },
      // ... etc for all 11 sub-agents

      {
        name: 'deactivate_all_agents',
        description: 'Remove all sub-agent tools from context',
        parameters: {},
        execute: async () => {
          this.activeTools.clear();
          return { message: 'All sub-agent tools deactivated' };
        }
      }
    ];
  }

  /**
   * Activate a sub-agent and return its tool index
   */
  private activateSubAgent(agentName: keyof SubAgentTools) {
    this.activeTools.add(agentName);
    const tools = this.toolRegistry[agentName];

    return {
      message: `${agentName} agent activated`,
      tools_available: Object.keys(tools).length,
      tools: tools // This is what gets added to agent context
    };
  }

  /**
   * Get currently active tools
   */
  getActiveTools() {
    const allActiveTools = [];

    for (const agentName of this.activeTools) {
      const tools = this.toolRegistry[agentName as keyof SubAgentTools];
      allActiveTools.push(...Object.values(tools));
    }

    return allActiveTools;
  }

  private initializeToolRegistry(): SubAgentTools {
    return {
      tick: new TickTools(this.intentBridge),
      connect: new ConnectTools(this.intentBridge),
      go: new GoTools(this.intentBridge),
      listen: new ListenTools(this.intentBridge),
      ask: new AskTools(this.intentBridge),
      plan: new PlanTools(this.intentBridge),
      think: new ThinkTools(this.intentBridge),
      set: new SetTools(this.intentBridge),
      reflect: new ReflectTools(this.intentBridge),
      task: new TaskTools(this.intentBridge),
      fund: new FundTools(this.intentBridge)
    };
  }
}
```

### Example: ListenTools

```typescript
// stone-agent/src/tools/ListenTools.ts

class ListenTools {
  private intentBridge: IntentApiBridge;
  private memory: Mem0Client;

  constructor(intentBridge: IntentApiBridge) {
    this.intentBridge = intentBridge;
    this.memory = new Mem0Client('listen_agent');
  }

  /**
   * All tools that LISTEN agent provides
   */
  getTools() {
    return {
      play_music: {
        name: 'play_music',
        description: 'Play music on Spotify by song, artist, or playlist',
        parameters: {
          type: 'object',
          properties: {
            query: {
              type: 'string',
              description: 'Song name, artist, or playlist to play'
            }
          },
          required: ['query']
        },
        execute: async (params: { query: string }) => {
          // Send Intent to launcher
          await this.intentBridge.sendIntent(
            'com.stone.launcher.action.PLAY_MUSIC',
            { query: params.query }
          );

          // Store in memory
          await this.memory.add({
            type: 'music_played',
            query: params.query,
            timestamp: new Date()
          });

          return { success: true, playing: params.query };
        }
      },

      control_playback: {
        name: 'control_playback',
        description: 'Control music playback (play, pause, next, previous)',
        parameters: {
          type: 'object',
          properties: {
            command: {
              type: 'string',
              enum: ['play', 'pause', 'next', 'previous', 'stop']
            }
          },
          required: ['command']
        },
        execute: async (params: { command: string }) => {
          await this.intentBridge.sendIntent(
            'com.stone.launcher.action.CONTROL_PLAYBACK',
            { command: params.command }
          );

          return { success: true, command: params.command };
        }
      },

      create_playlist: {
        name: 'create_playlist',
        description: 'Create a new Spotify playlist',
        parameters: {
          type: 'object',
          properties: {
            name: { type: 'string' },
            tracks: {
              type: 'array',
              items: { type: 'string' },
              description: 'Array of track names or IDs'
            }
          },
          required: ['name']
        },
        execute: async (params: { name: string, tracks?: string[] }) => {
          await this.intentBridge.sendIntent(
            'com.stone.launcher.action.CREATE_PLAYLIST',
            { name: params.name, tracks: params.tracks || [] }
          );

          await this.memory.add({
            type: 'playlist_created',
            name: params.name,
            timestamp: new Date()
          });

          return { success: true, playlist: params.name };
        }
      },

      search_music: {
        name: 'search_music',
        description: 'Search Spotify catalog',
        parameters: {
          type: 'object',
          properties: {
            query: { type: 'string' },
            type: {
              type: 'string',
              enum: ['track', 'artist', 'album', 'playlist']
            }
          },
          required: ['query']
        },
        execute: async (params: { query: string, type?: string }) => {
          const result = await this.intentBridge.sendIntent(
            'com.stone.launcher.action.SEARCH_MUSIC',
            { query: params.query, type: params.type || 'track' }
          );

          return { success: true, results: result.results };
        }
      }
    };
  }
}
```

---

## Main Agent Implementation

### Using LiveKit Agents SDK

```typescript
// stone-agent/src/mainAgent.ts
import { JobContext, WorkerOptions, cli, defineAgent } from '@livekit/agents';
import * as openai from '@livekit/agents-plugin-openai';
import { IntentApiBridge } from './intentBridge';
import { ToolActivationManager } from './toolActivation';
import { Mem0Client } from './memory';

export class MainAgent {
  private intentBridge: IntentApiBridge;
  private toolManager: ToolActivationManager;
  private memory: Mem0Client;
  private room: any;

  constructor(room: any) {
    this.room = room;
    this.intentBridge = new IntentApiBridge(room);
    this.toolManager = new ToolActivationManager(this.intentBridge);
    this.memory = new Mem0Client('main_agent');
  }

  async processInput(userMessage: string): Promise<string> {
    // Get activation tools (always available)
    const activationTools = this.toolManager.getActivationTools();

    // Get currently active sub-agent tools
    const activeTools = this.toolManager.getActiveTools();

    // Combine all available tools
    const allTools = [...activationTools, ...activeTools];

    // Get relevant memory
    const relevantMemory = await this.memory.search(userMessage);

    // Build system prompt
    const systemPrompt = `You are Stone, an AI assistant integrated into a phone.

You control the phone by calling tools.

When a user asks for something, determine which sub-agent's tools you need:
- Time/alarms/timers → activate_tick_agent()
- Calls/SMS/contacts → activate_connect_agent()
- Navigation/maps → activate_go_agent()
- Music/Spotify → activate_listen_agent()
- Search/questions → activate_ask_agent()
- Calendar/events → activate_plan_agent()
- Notes/writing → activate_think_agent()
- Settings/WiFi/Bluetooth → activate_set_agent()
- Journaling → activate_reflect_agent()
- Apps/launch → activate_task_agent()
- Payments → activate_fund_agent() (limited tools)

After activating a sub-agent, its tools become available to you.

When you're done with a task, call deactivate_all_agents() to clean up context.

Be conversational and helpful. Don't mention sub-agents to the user.

${relevantMemory ? `\n\nRelevant memory:\n${JSON.stringify(relevantMemory)}` : ''}`;

    // Call OpenAI with all available tools
    const model = new openai.LLM({
      model: 'gpt-4',
      temperature: 0.7
    });

    const response = await model.generate({
      system: systemPrompt,
      messages: [{ role: 'user', content: userMessage }],
      tools: allTools
    });

    // Store interaction
    await this.memory.add({
      user_message: userMessage,
      agent_response: response.content,
      tools_used: response.tool_calls?.map(tc => tc.name) || [],
      timestamp: new Date()
    });

    return response.content;
  }

  async handleVoiceMode() {
    // Voice pipeline for pure voice interaction
    const vad = await openai.VAD.create();
    const stt = new openai.STT({ model: 'whisper-1' });
    const tts = new openai.TTS({ model: 'tts-1', voice: 'nova' });

    // Create voice assistant
    const assistant = await openai.VoiceAssistant.create({
      vad,
      stt,
      llm: this, // Main agent handles LLM calls
      tts,
      room: this.room
    });

    await assistant.start();
  }
}

// LiveKit agent entry point
export default defineAgent({
  entry: async (ctx: JobContext) => {
    await ctx.connect();

    const mainAgent = new MainAgent(ctx.room);

    // Listen for text messages via data channel
    ctx.room.on('dataReceived', async (data: Uint8Array) => {
      const message = new TextDecoder().decode(data);
      const parsed = JSON.parse(message);

      if (parsed.type === 'text_input') {
        const response = await mainAgent.processInput(parsed.content);

        // Send response back
        const responseData = JSON.stringify({
          type: 'text_response',
          content: response
        });

        await ctx.room.localParticipant.publishData(
          new TextEncoder().encode(responseData),
          { reliable: true }
        );
      } else if (parsed.type === 'start_voice') {
        await mainAgent.handleVoiceMode();
      }
    });

    console.log('Main agent ready');
  }
});

// Run the agent
if (require.main === module) {
  cli.runApp(new WorkerOptions({
    entrypoint: exports.default,
    port: 8080
  }));
}
```

---

## Intent API Bridge

### Connecting Agents to Launcher

```typescript
// stone-agent/src/intentBridge.ts
import { Room } from '@livekit/rtc-node';

export class IntentApiBridge {
  private room: Room;
  private pendingRequests: Map<string, {
    resolve: (value: any) => void;
    reject: (error: any) => void;
  }> = new Map();

  constructor(room: Room) {
    this.room = room;
    this.setupResponseListener();
  }

  private setupResponseListener() {
    this.room.on('dataReceived', async (data: Uint8Array) => {
      const message = new TextDecoder().decode(data);
      const parsed = JSON.parse(message);

      if (parsed.type === 'intent_result') {
        const pending = this.pendingRequests.get(parsed.requestId);
        if (pending) {
          if (parsed.success) {
            pending.resolve(parsed.data);
          } else {
            pending.reject(new Error(parsed.error_message));
          }
          this.pendingRequests.delete(parsed.requestId);
        }
      }
    });
  }

  async sendIntent(action: string, extras: Record<string, any> = {}): Promise<any> {
    const requestId = `req_${Date.now()}_${Math.random()}`;

    const intentMessage = JSON.stringify({
      type: 'execute_intent',
      requestId,
      action,
      extras
    });

    await this.room.localParticipant.publishData(
      new TextEncoder().encode(intentMessage),
      { reliable: true }
    );

    return new Promise((resolve, reject) => {
      this.pendingRequests.set(requestId, { resolve, reject });

      setTimeout(() => {
        if (this.pendingRequests.has(requestId)) {
          this.pendingRequests.delete(requestId);
          reject(new Error('Intent timeout'));
        }
      }, 10000);
    });
  }
}
```

---

## React Native Integration

### LiveKit Client in Launcher

```typescript
// src/services/LiveKitClient.ts
import { Room, RoomEvent, DataPacket_Kind } from '@livekit/react-native';
import { NativeModules } from 'react-native';

const { IntentExecutor } = NativeModules;

export class LiveKitClient {
  private room: Room | null = null;

  async connect(url: string, token: string) {
    this.room = new Room();

    // Listen for Intent requests from agent
    this.room.on(RoomEvent.DataReceived, async (payload: Uint8Array) => {
      const message = new TextDecoder().decode(payload);
      const parsed = JSON.parse(message);

      if (parsed.type === 'execute_intent') {
        try {
          // Execute Intent via native module
          const result = await IntentExecutor.executeIntent(
            parsed.action,
            parsed.extras
          );

          // Send result back to agent
          await this.sendIntentResult(parsed.requestId, true, result);
        } catch (error) {
          await this.sendIntentResult(
            parsed.requestId,
            false,
            undefined,
            error.message
          );
        }
      }
    });

    await this.room.connect(url, token);
  }

  async sendIntentResult(requestId: string, success: boolean, data?: any, error?: string) {
    if (!this.room) return;

    const message = JSON.stringify({
      type: 'intent_result',
      requestId,
      success,
      data: data || {},
      error_message: error
    });

    await this.room.localParticipant?.publishData(
      new TextEncoder().encode(message),
      DataPacket_Kind.RELIABLE
    );
  }

  async disconnect() {
    await this.room?.disconnect();
  }
}
```

---

## Deployment

### Cloud-Hosted (Recommended)

**Infrastructure**:
- LiveKit Cloud or self-hosted LiveKit server
- Agent workers on Railway/Render/Fly.io/VPS
- Node.js 18+

**Cost Estimate**:
- LiveKit Cloud: ~$50/month
- Agent hosting: ~$20/month
- OpenAI API: ~$50/month
- **Total**: ~$120/month

### Self-Hosted

**On a VPS**:
```bash
# Install LiveKit
docker run -d -p 7880:7880 -p 7881:7881 -p 7882:7882/udp \
  livekit/livekit-server

# Deploy agents
cd stone-agent
npm install
npm run build
pm2 start dist/mainAgent.js --name stone-agent
```

---

## Testing the Full Stack

### End-to-End Flow

```
User (on phone): "Turn on WiFi"
  ↓ [Voice captured by React Native]
  ↓ [Sent to LiveKit Cloud via WebRTC]
  ↓
Main Agent receives voice
  ↓ [STT → Text]
Agent: "Turn on WiFi"
  ↓
Main Agent: activate_set_agent()
  ↓ [SET tools now available]
Main Agent: set_wifi(enabled=true)
  ↓ [Intent sent via data channel]
  ↓
Launcher receives Intent
  ↓
IntentExecutor.executeIntent("com.stone.launcher.action.SET_WIFI", {enabled: true})
  ↓
BroadcastReceiver → WifiController.setWifiEnabled(true)
  ↓ [WiFi enabled]
  ↓ [Result broadcast sent]
  ↓
Launcher sends result via data channel
  ↓
Main Agent receives result: {success: true, wifi_enabled: true}
  ↓
Main Agent: deactivate_all_agents()
  ↓ [TTS]
  ↓ [Audio sent via WebRTC]
User hears: "WiFi is now enabled"
```

---

## Migration from Existing Code

Your existing `/stone-os/stone-agent/` code needs these changes:

1. **Remove MCP client connections** - Replace with Intent API Bridge
2. **Implement tool activation pattern** - Add activate/deactivate tools
3. **Update tool implementations** - Call intentBridge.sendIntent() instead of MCP
4. **Add mem0 per sub-agent** - Each tool class gets own mem0 instance

---

## Next Steps

1. **Review this architecture** - Confirm it matches your vision
2. **Implement IntentApiBridge** - Core component connecting agents to launcher
3. **Build one sub-agent** - Start with LISTEN or SET (simplest)
4. **Test activation pattern** - Ensure tools load/unload correctly
5. **Add remaining sub-agents** - Follow same pattern for all 11 apps

---

**This architecture enables dynamic context management while keeping all business logic in the launcher via the Intent API.**
