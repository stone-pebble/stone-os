# StoneOS React Native UI Shell

## Overview

The StoneOS UI is a React Native-based shell that completely replaces the traditional Android launcher and app paradigm. It provides a minimalist, AI-first interface that prioritizes voice interaction and reduces visual distractions.

## Architecture Decision: WebView Implementation

After careful analysis, we've chosen a **Privileged WebView Container** approach for the UI shell:

### Why WebView over React Native?

1. **Direct Code Reuse**: The existing `ui/` directory React code can be used with minimal modifications
2. **Rapid Development**: Web technologies allow faster iteration
3. **Maximum Flexibility**: Full control over rendering and interactions
4. **Simpler Bridge**: Direct JavaScript-to-native communication

### Implementation Details

```java
// MainActivity.java - The WebView container
public class MainActivity extends Activity {
    private WebView webView;
    private StoneOSBridge bridge;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Full screen, no status bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        // Initialize WebView
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        // Add native bridge
        bridge = new StoneOSBridge(this);
        webView.addJavascriptInterface(bridge, "StoneOS");
        
        // Load local UI bundle
        webView.loadUrl("file:///android_asset/index.html");
        
        setContentView(webView);
    }
}
```

## UI Design Principles

### 1. Minimalist Interface

- **Dark Theme**: Black background with white text
- **Typography First**: Clear, readable fonts (Inter/SF Pro)
- **No Chrome**: No status bars, navigation bars, or system UI
- **Gesture Navigation**: Simple swipes for navigation

### 2. Voice-First Interaction

```typescript
// Voice interaction component
const VoiceInterface: React.FC = () => {
    const [isListening, setIsListening] = useState(false);
    const [transcript, setTranscript] = useState("");
    
    const startListening = async () => {
        setIsListening(true);
        const result = await StoneOS.voice.startRecognition();
        setTranscript(result.transcript);
        
        // Send to agent
        const response = await StoneOS.agent.sendMessage(result.transcript);
        handleAgentResponse(response);
    };
    
    return (
        <div className="voice-interface">
            <button 
                className="voice-button"
                onTouchStart={startListening}
                onTouchEnd={stopListening}
            >
                {isListening ? <MicActiveIcon /> : <MicIcon />}
            </button>
            {transcript && (
                <div className="transcript">{transcript}</div>
            )}
        </div>
    );
};
```

### 3. Context-Aware Display

The UI adapts based on the current context and agent state:

```typescript
interface UIState {
    currentAgent: string;
    isProcessing: boolean;
    lastResponse: AgentResponse;
    activeTools: string[];
}

const ContextualDisplay: React.FC<{state: UIState}> = ({state}) => {
    switch(state.currentAgent) {
        case 'music':
            return <MusicInterface activeTools={state.activeTools} />;
        case 'navigation':
            return <MapInterface destination={state.lastResponse.data} />;
        case 'notes':
            return <NotesInterface content={state.lastResponse.content} />;
        default:
            return <DefaultInterface />;
    }
};
```

## Core UI Components

### 1. Home Screen

```typescript
const HomeScreen: React.FC = () => {
    return (
        <div className="home-screen">
            <TimeDisplay />
            <DateDisplay />
            <VoiceInterface />
            <QuickActions />
            <NotificationArea />
        </div>
    );
};
```

### 2. Agent Chat Interface

Based on the existing `AgentChatInterface` component:

```typescript
const AgentChat: React.FC = () => {
    const [messages, setMessages] = useState<Message[]>([]);
    const [room, setRoom] = useState<Room | null>(null);
    
    useEffect(() => {
        // Connect to LiveKit
        connectToAgent();
    }, []);
    
    return (
        <div className="agent-chat">
            <MessageList messages={messages} />
            <VoiceInput onMessage={handleUserMessage} />
            <AgentStatus status={agentState} />
        </div>
    );
};
```

### 3. Application Views

Each "application" is a specialized view within the shell:

```typescript
// Clock application
const ClockView: React.FC = () => {
    const [time, setTime] = useState(new Date());
    
    return (
        <div className="clock-view">
            <div className="time-display">
                {time.toLocaleTimeString()}
            </div>
            <WorldClock />
            <Alarms />
            <Timer />
        </div>
    );
};

// Music application
const MusicView: React.FC = () => {
    const [currentTrack, setCurrentTrack] = useState<Track | null>(null);
    
    return (
        <div className="music-view">
            <NowPlaying track={currentTrack} />
            <PlaybackControls />
            <Queue />
        </div>
    );
};
```

## Native Bridge API

The bridge provides JavaScript access to system functions:

```typescript
interface StoneOSBridge {
    // System functions
    system: {
        getDeviceInfo(): Promise<DeviceInfo>;
        setBrightness(level: number): Promise<void>;
        setVolume(level: number): Promise<void>;
        hapticFeedback(type: HapticType): Promise<void>;
    };
    
    // Voice functions
    voice: {
        startRecognition(): Promise<RecognitionResult>;
        stopRecognition(): Promise<void>;
        speak(text: string, options?: TTSOptions): Promise<void>;
    };
    
    // Agent communication
    agent: {
        sendMessage(message: string): Promise<AgentResponse>;
        onResponse(callback: (response: AgentResponse) => void): void;
        getCurrentAgent(): Promise<string>;
    };
    
    // MCP access
    mcp: {
        spotify: SpotifyAPI;
        maps: MapsAPI;
        calendar: CalendarAPI;
        contacts: ContactsAPI;
        camera: CameraAPI;
    };
    
    // UI functions
    ui: {
        showNotification(notification: Notification): Promise<void>;
        vibrate(pattern: number[]): Promise<void>;
        setTheme(theme: Theme): Promise<void>;
    };
}
```

### Bridge Implementation (Java)

```java
public class StoneOSBridge {
    private Context context;
    private MasterControlProgram mcp;
    
    @JavascriptInterface
    public String getDeviceInfo() {
        JSONObject info = new JSONObject();
        info.put("model", Build.MODEL);
        info.put("manufacturer", Build.MANUFACTURER);
        info.put("androidVersion", Build.VERSION.RELEASE);
        info.put("stoneOSVersion", BuildConfig.STONEOS_VERSION);
        return info.toString();
    }
    
    @JavascriptInterface
    public void sendAgentMessage(String message, String callbackId) {
        // Send to agent service
        Intent intent = new Intent(context, AgentService.class);
        intent.setAction("com.stoneos.AGENT_MESSAGE");
        intent.putExtra("message", message);
        intent.putExtra("callbackId", callbackId);
        context.startService(intent);
    }
    
    @JavascriptInterface
    public String callMCP(String module, String method, String args) {
        try {
            return mcp.call(module, method, args);
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }
}
```

## Navigation & Gestures

### Swipe Navigation

```typescript
const useSwipeNavigation = () => {
    const [currentPage, setCurrentPage] = useState(0);
    
    const handleSwipe = (direction: SwipeDirection) => {
        switch(direction) {
            case 'left':
                navigateToNext();
                break;
            case 'right':
                navigateToPrevious();
                break;
            case 'up':
                showQuickSettings();
                break;
            case 'down':
                dismissCurrent();
                break;
        }
    };
    
    return { currentPage, handleSwipe };
};
```

### Page Structure

```typescript
const pages = [
    { id: 'home', component: HomeScreen },
    { id: 'chat', component: AgentChat },
    { id: 'clock', component: ClockView },
    { id: 'music', component: MusicView },
    { id: 'notes', component: NotesView },
    { id: 'maps', component: MapsView },
    { id: 'settings', component: SettingsView }
];
```

## Styling & Theming

### Design System

```scss
// Base theme variables
:root {
    --color-background: #000000;
    --color-surface: #0A0A0A;
    --color-text-primary: #FFFFFF;
    --color-text-secondary: #888888;
    --color-accent: #007AFF;
    
    --font-display: 'SF Pro Display', 'Inter', sans-serif;
    --font-body: 'SF Pro Text', 'Inter', sans-serif;
    
    --spacing-xs: 4px;
    --spacing-sm: 8px;
    --spacing-md: 16px;
    --spacing-lg: 24px;
    --spacing-xl: 32px;
    
    --radius-sm: 8px;
    --radius-md: 12px;
    --radius-lg: 16px;
}

// Component styles
.voice-button {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: var(--color-surface);
    border: 2px solid var(--color-text-secondary);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
    
    &.active {
        background: var(--color-accent);
        border-color: var(--color-accent);
        transform: scale(1.1);
    }
}

.message {
    padding: var(--spacing-md);
    margin: var(--spacing-sm) 0;
    border-radius: var(--radius-md);
    
    &.user {
        background: var(--color-surface);
        margin-left: 20%;
    }
    
    &.agent {
        background: transparent;
        border: 1px solid var(--color-surface);
        margin-right: 20%;
    }
}
```

## Performance Optimization

### 1. Bundle Optimization

```javascript
// webpack.config.js
module.exports = {
    optimization: {
        splitChunks: {
            chunks: 'all',
            cacheGroups: {
                vendor: {
                    test: /[\\/]node_modules[\\/]/,
                    name: 'vendors',
                    priority: 10
                },
                common: {
                    minChunks: 2,
                    priority: 5,
                    reuseExistingChunk: true
                }
            }
        },
        minimize: true,
        usedExports: true
    }
};
```

### 2. Lazy Loading

```typescript
// Lazy load heavy components
const MapsView = lazy(() => import('./views/MapsView'));
const NotesEditor = lazy(() => import('./views/NotesEditor'));

// Preload critical components
const preloadCritical = () => {
    import('./components/VoiceInterface');
    import('./components/AgentChat');
};
```

### 3. State Management

```typescript
// Efficient state updates
const useOptimizedState = () => {
    const [state, setState] = useState(initialState);
    
    const updateState = useCallback((updates: Partial<State>) => {
        setState(prev => ({
            ...prev,
            ...updates,
            lastUpdated: Date.now()
        }));
    }, []);
    
    return [state, updateState];
};
```

## Testing

### 1. Component Testing

```typescript
// Voice interface test
describe('VoiceInterface', () => {
    it('should start recording on button press', async () => {
        const { getByRole } = render(<VoiceInterface />);
        const button = getByRole('button');
        
        fireEvent.touchStart(button);
        
        expect(StoneOS.voice.startRecognition).toHaveBeenCalled();
    });
});
```

### 2. Integration Testing

```typescript
// Test agent communication
it('should send message to agent and display response', async () => {
    const mockResponse = {
        text: 'Playing your music',
        agent: 'music',
        action: 'play'
    };
    
    StoneOS.agent.sendMessage.mockResolvedValue(mockResponse);
    
    const { getByText } = render(<AgentChat />);
    
    // Simulate user input
    await userEvent.type(getByRole('textbox'), 'Play some jazz');
    await userEvent.click(getByText('Send'));
    
    // Verify response displayed
    await waitFor(() => {
        expect(getByText('Playing your music')).toBeInTheDocument();
    });
});
```

## Deployment

### Building for StoneOS

```bash
# Build optimized bundle
npm run build:stoneos

# Output structure
build/
├── index.html
├── js/
│   ├── app.bundle.js
│   ├── vendor.bundle.js
│   └── runtime.bundle.js
├── css/
│   └── main.css
└── assets/
    ├── fonts/
    └── icons/
```

### Integration with AOSP

```makefile
# Android.mk for UI integration
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := StoneUI
LOCAL_MODULE_CLASS := APPS
LOCAL_SRC_FILES := StoneUI.apk
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE_SUFFIX := $(COMMON_ANDROID_PACKAGE_SUFFIX)
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true
include $(BUILD_PREBUILT)
```

## Future Enhancements

1. **Offline Support**: Service workers for offline functionality
2. **Adaptive UI**: Machine learning for personalized layouts
3. **Widget System**: Modular UI components
4. **Theme Engine**: User-customizable themes
5. **Accessibility**: Enhanced screen reader support 