# StoneOS Deployment Guide

## Quick Start: Deploy to Production

### 1. Get LiveKit Cloud Account
1. Sign up at https://livekit.io/cloud
2. Create a new project
3. Get your API key and secret
4. Note your WebSocket URL (wss://your-project.livekit.cloud)

### 2. Deploy Agent Server to VPS

#### Option A: DigitalOcean (Recommended for simplicity)
```bash
# Create a $6/month droplet with Ubuntu 22.04
# SSH into the droplet

# Install Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# Clone and setup
git clone https://github.com/yourusername/stone-os.git
cd stone-os/stone-agent
npm install
npm run build

# Configure environment
cp .env.example .env
nano .env
# Add your LiveKit credentials:
# LIVEKIT_URL=wss://your-project.livekit.cloud
# LIVEKIT_API_KEY=your-api-key
# LIVEKIT_API_SECRET=your-api-secret
# OPENAI_API_KEY=your-openai-key (for GPT-4)

# Install PM2 for process management
sudo npm install -g pm2

# Start services
pm2 start npm --name "stone-agent" -- run start:agent
pm2 start npm --name "token-server" -- run token-server
pm2 save
pm2 startup
```

#### Option B: Railway.app (Even simpler)
1. Connect GitHub repo to Railway
2. Add environment variables in Railway dashboard
3. Deploy with one click

### 3. Update Android App

```kotlin
// app/build.gradle
android {
    defaultConfig {
        buildConfigField "String", "TOKEN_SERVER_URL", "\"https://your-server.com\""
    }
}

// ChatViewModel.kt
fun connect(context: Context, agentType: String = "router") {
    viewModelScope.launch {
        val serverUrl = BuildConfig.TOKEN_SERVER_URL

        // Fetch connection details from your token server
        val response = httpClient.get("$serverUrl/api/connection-details") {
            parameter("roomName", roomName)
            parameter("participantId", participantId)
        }

        val connectionDetails = response.body<ConnectionDetails>()

        // Connect to LiveKit Cloud
        LiveKitManager.connect(
            context,
            connectionDetails.url,
            connectionDetails.token
        )
    }
}
```

### 4. Configure Domain & SSL
```nginx
# Nginx configuration for your server
server {
    server_name stone-agent.yourdomain.com;

    location / {
        proxy_pass http://localhost:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
    }

    listen 443 ssl;
    ssl_certificate /etc/letsencrypt/live/stone-agent.yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/stone-agent.yourdomain.com/privkey.pem;
}
```

## Cost Breakdown

### Minimal Production Setup
- VPS: $6/month (DigitalOcean Basic Droplet)
- LiveKit Cloud: ~$10/month (400 minutes of usage)
- OpenAI API: ~$5/month (light usage)
- **Total: ~$21/month**

### Scale Considerations
- Each agent connection uses ~0.5GB RAM
- CPU usage is minimal (mostly I/O bound)
- Can handle 5-10 concurrent users on basic VPS
- Scale horizontally by adding more agent workers

## Security Checklist

- [ ] Use environment variables for all secrets
- [ ] Enable HTTPS on token server
- [ ] Implement rate limiting
- [ ] Add authentication to token endpoint
- [ ] Use webhook validation for LiveKit
- [ ] Implement user session management
- [ ] Add request signing between app and server

## Monitoring

```bash
# View logs
pm2 logs stone-agent
pm2 logs token-server

# Monitor resources
pm2 monit

# Set up alerts
pm2 install pm2-logrotate
pm2 set pm2-logrotate:max_size 10M
```

## Alternative: On-Device AI (Privacy Mode)

For users who want complete privacy:

### 1. Implement Local AI Processing
```kotlin
// LocalAIProcessor.kt
class LocalAIProcessor(context: Context) {
    private val whisperModel = WhisperCpp.loadModel(context, "whisper-tiny")
    private val llamaModel = LlamaCpp.loadModel(context, "llama-3.2-1b")

    suspend fun processVoiceCommand(audioData: ByteArray): String {
        val text = whisperModel.transcribe(audioData)
        val response = llamaModel.generate(text)
        return response
    }
}
```

### 2. Add Settings Toggle
```kotlin
// SettingsActivity.kt
class SettingsActivity : AppCompatActivity() {
    private fun setupPrivacyMode() {
        privacyModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Use local AI
                ChatViewModel.setMode(ChatMode.LOCAL_AI)
            } else {
                // Use cloud agents
                ChatViewModel.setMode(ChatMode.CLOUD_AGENTS)
            }
        }
    }
}
```

## Testing Production Setup

1. **Test Connection**
```bash
curl https://stone-agent.yourdomain.com/health
# Should return: {"status":"healthy"}
```

2. **Test Token Generation**
```bash
curl "https://stone-agent.yourdomain.com/api/connection-details?roomName=test&participantId=user123"
# Should return token and URL
```

3. **Test from Android**
- Install production APK
- Check logs: `adb logcat | grep LiveKit`
- Verify connection established

## Rollout Strategy

### Phase 1: Alpha Testing (You)
- Deploy to your personal VPS
- Test with your Pixel 8a
- Monitor costs and performance

### Phase 2: Beta Testing (Friends & Family)
- Add authentication to token server
- Implement user management
- Monitor usage patterns

### Phase 3: Public Release
- Consider self-hosting options for privacy-conscious users
- Provide Docker images for easy deployment
- Document local AI alternatives

## FAQ

**Q: Can I run this without internet?**
A: Not with the current architecture. We'd need to implement on-device AI models (Phase 2).

**Q: How much will this cost at scale?**
A: Approximately $0.05 per user per hour of voice interaction.

**Q: Can I self-host everything?**
A: Yes! You can run your own LiveKit server, but it's more complex to set up.

**Q: Is my voice data private?**
A: With cloud deployment, voice is processed by OpenAI/Anthropic. For privacy, wait for on-device mode.