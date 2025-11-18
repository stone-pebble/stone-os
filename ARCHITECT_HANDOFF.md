# Stone OS Architect Agent Handoff Document

## Current State Summary
The Stone Launcher is successfully running on a physical Pixel 8a device with LiveKit voice integration. The native Kotlin app connects to local servers (token server port 8000, agent port 8081) and LiveKit cloud. First test worked perfectly, but subsequent attempts had connectivity issues - likely cloud deployment related.

**Branch**: `stone-launcher-implementation` (pushed to GitHub)
**Device**: Pixel 8a (ID: 48151JEKB04299)
**Current Working Directory**: `/Users/samuellarson/Pebble/Github/stone-os`

## Critical Context & User Preferences

### Development Flow The User Likes
1. **Always use subagents** - User prefers dispatching specialized agents rather than doing work directly
2. **Research first, then implement** - Dispatch research agents before coding agents
3. **Test everything** - Use test-and-iterate agents after implementation
4. **Save work frequently** - Commit and push to GitHub branches regularly
5. **Protect sensitive data** - Always verify .env files are in .gitignore before commits

### User Communication Style
- Direct and action-oriented ("get to it", "you know the drill")
- Wants comprehensive solutions, not partial implementations
- Values proactive problem-solving
- Expects agents to handle technical details autonomously
- Prefers seeing the actual app working on device over documentation

### Key Architecture Decisions Made
- **Native Kotlin only** - NO React Native (user was very clear about this)
- **LiveKit for voice** - Using LiveKit Android SDK with cloud instance
- **Local servers during dev** - Token server and agent run locally, connect to LiveKit cloud
- **Intent API pattern** - For tool execution and app control
- **Minimalist aesthetic** - Black background, white text, no decorations

## Mistakes Made & Lessons Learned

### What Failed
1. **Embedded Node.js approach** - Cannot embed Node.js servers in Android due to @livekit/rtc-node incompatibility
2. **React Native assumption** - Initially assumed RN, user corrected firmly to native Kotlin
3. **Tapping "ask" button** - Initially only showed toast, had to fix to open ChatActivity
4. **Network security** - Needed special config to allow HTTP to local IP addresses

### What Worked
1. **Swipe gestures** - Critical UX feature that user specifically wanted
2. **Local server + cloud LiveKit** - Good hybrid approach for development
3. **Using subagents** - User appreciated when work was delegated properly
4. **Native Kotlin with LiveKit Android SDK** - Exactly what user wanted

## Immediate TODOs

### 1. Fix Connection Reliability Issue
**Problem**: App worked first time, failed on second attempt
**Likely cause**: Cloud deployment or token expiration
**Action**: Dispatch android-research-specialist to investigate, then android-feature-implementer to fix

### 2. UI Layout Fix - Grid Extension
**Problem**: 12-app grid bunched in top half, bottom half is black
**Requirement**: Grid should extend all the way down the page
**File**: `/Users/samuellarson/Pebble/Github/stone-os/android/app/src/main/res/layout/activity_main.xml`
**Action**: Dispatch android-feature-implementer for aesthetic edit

### 3. Default Launcher Setup
**Requirement**: App must become the default launcher when installed
**Current**: Just runs as regular app
**Action**: Research launcher manifest requirements, implement proper launcher categories

### 4. Build All 12 Stone Apps
**Source**: Web app prototypes (need location from user)
**Target**: Native Kotlin with LiveKit SDK
**Apps to build**:
- tick (time/reminders)
- pebbles (notes/thoughts)
- set (settings)
- listen (music/audio)
- ask (AI chat) ✓ (partially done)
- look (camera/vision)
- plan (calendar/tasks)
- think (analysis/reasoning)
- reflect (journal/meditation)
- connect (contacts/social)
- go (maps/navigation)
- fund (finance/payments)

### 5. Claude Sonnet 4.5 Integration
**Current**: Using OpenAI models in some places
**Required**: Claude Sonnet 4.5 everywhere
**Challenge**: Anthropic integration with LiveKit
**Action**:
1. Research LiveKit's Anthropic out-of-box options
2. If latency issues, implement custom optimization
3. Update all agent configurations

## File Structure & Key Locations

### Android App
```
/android/
├── app/
│   ├── src/main/java/com/stonelauncher/
│   │   ├── MainActivity.kt          # Home screen with 12-word grid
│   │   ├── ui/ChatActivity.kt       # Voice chat interface
│   │   ├── livekit/                 # LiveKit integration
│   │   ├── api/StoneApiReceiver.kt  # Intent API
│   │   └── controllers/             # Device control
│   └── src/main/res/
│       ├── layout/                  # UI layouts (THIS NEEDS GRID FIX)
│       └── xml/network_security_config.xml
```

### Agent Server
```
/stone-agent/
├── src/
│   ├── server/token-server.ts      # Port 8000
│   ├── agents/stone-router.ts      # Main agent logic
│   └── tools/device-tools.ts       # Tool implementations
├── .env                            # PROTECTED - Contains API keys
└── package.json
```

### Important Files
- `.env` - User's actual API keys (NEVER commit)
- `stone-agent/.env` - Agent server credentials (NEVER commit)
- `STONEOS_SPECS.md` - Complete feature specifications
- `android/app/build.gradle` - Build config with server URLs

## Testing Commands

### Build & Deploy to Device
```bash
cd android
./gradlew assembleDebug
adb -s 48151JEKB04299 install -r app/build/outputs/apk/debug/app-debug.apk
```

### Start Local Servers
```bash
# Terminal 1 - Token Server
cd stone-agent && npm run token-server

# Terminal 2 - Agent Server
cd stone-agent && npm run dev
```

### Monitor Device Logs
```bash
adb -s 48151JEKB04299 logcat | grep -E "StoneOS|MainActivity|ChatActivity|LiveKit"
```

## Agent Dispatch Patterns

### For Research
```
Use android-research-specialist for:
- LiveKit Anthropic integration options
- Default launcher requirements
- Performance optimization techniques
```

### For Implementation
```
Use android-feature-implementer for:
- UI layout fixes
- Building new Stone apps
- Implementing launcher capabilities
```

### For Testing
```
Use test-and-iterate for:
- Verifying fixes work on device
- Testing each new Stone app
- Confirming cloud deployment
```

### For Documentation
```
Use github-maintainer for:
- Updating README files
- Syncing ticket status
- Maintaining architecture docs
```

## Environment Details
- **LiveKit URL**: wss://stone-os48tc1d.livekit.cloud
- **Local IP**: 192.168.86.25 (Mac hosting servers)
- **Device IP**: 192.168.86.27 (Pixel on same network)
- **WiFi**: TreeHaus network

## Next Architect Actions

1. **Fix the grid layout** - Dispatch android-feature-implementer immediately
2. **Investigate connection issue** - Research then fix the "works once" problem
3. **Find web app prototypes** - Ask user for location, then clone all 12 apps
4. **Setup Claude Sonnet 4.5** - Research LiveKit integration, implement everywhere
5. **Make default launcher** - Add proper manifest categories and permissions
6. **Create comprehensive tests** - For each app and the launcher itself
7. **Update documentation** - Keep everything current as you develop

## Golden Rules
- User wants to see it WORKING on the device, not just hear about it
- Always dispatch subagents for actual work
- Protect .env files at all costs
- Test on physical device (48151JEKB04299) not just emulator
- Keep the minimalist aesthetic - no unnecessary UI elements
- Swipe gestures are CRITICAL - must always work

## Current Problems to Solve First
1. Grid layout only using top half of screen
2. Connection works once then fails
3. Need Claude Sonnet 4.5 not OpenAI
4. App doesn't register as launcher
5. Only "ask" app exists, need other 11

Good luck! The user expects quick action and working results on device.