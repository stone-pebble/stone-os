# TICKET_011: Embed Agent Workers in Android App

## Status: BLOCKED_BY_TICKET_010
Priority: HIGH
Type: Implementation
Depends On: TICKET_010 (Token Server)

## Description
Bundle and run the LiveKit agents.js workers within the embedded Node.js runtime, enabling on-device AI agent processing.

## Acceptance Criteria
- [ ] Agent workers start within embedded Node.js
- [ ] Agents connect to LiveKit rooms successfully
- [ ] Can process voice/text inputs
- [ ] Tool execution works (openApp, etc.)
- [ ] Multiple agents can run concurrently
- [ ] Memory usage stays under 200MB total

## Implementation Tasks
1. Bundle agent JavaScript files in APK
2. Modify agents to work in embedded context
3. Configure agent environment variables
4. Implement agent lifecycle management
5. Add agent health monitoring
6. Create agent restart logic
7. Optimize for mobile constraints

## Architecture
```
Android App Process
├── Main UI Thread (Kotlin)
├── Node.js Thread
│   ├── Token Server (:8000)
│   └── Agent Workers
│       ├── Router Agent
│       ├── Think Agent
│       └── [Other Stone Agents]
└── LiveKit SDK Thread
```

## Critical Integration Points
```javascript
// Agent must connect to same room as Android participant
const room = await connect(LIVEKIT_URL, token);

// Agent receives device commands via data channel
room.on('dataReceived', (data, participant) => {
  if (data.topic === 'device_command') {
    executeToolLocally(data);
  }
});
```

## Challenges to Solve
1. API key management (OpenAI, etc.)
2. Agent hot-reload during development
3. Debugging embedded JavaScript
4. Performance optimization for mobile
5. Handling agent crashes gracefully

## Testing Strategy
- Unit tests for each agent
- Integration test with Android UI
- Load test with multiple concurrent agents
- Memory leak detection
- Battery drain measurement
- Network failure handling

## Success Metrics
- Agent starts in < 3 seconds
- Voice response latency < 200ms (local)
- Can run 3+ agents simultaneously
- Survives 1 hour continuous operation
- Battery drain < 10% per hour