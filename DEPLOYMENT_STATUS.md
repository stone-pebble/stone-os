# Stone Agent Cloud Deployment - Status

## Current Status: READY FOR DEPLOYMENT ✅

All deployment configuration files and documentation have been created. The stone-agent server is ready to be deployed to cloud infrastructure.

---

## What Was Completed

### Configuration Files Created

1. **Railway Deployment** (Recommended)
   - `/stone-agent/railway.json` - JSON configuration
   - `/stone-agent/railway.toml` - TOML configuration with health checks
   - Platform: Railway.app (~$5/month)

2. **Fly.io Deployment** (Alternative)
   - `/stone-agent/fly.toml` - Full Fly.io configuration
   - Multi-region support, autoscaling configured
   - Platform: Fly.io (generous free tier)

3. **Docker Deployment** (Universal)
   - `/stone-agent/Dockerfile` - Multi-stage production build
   - `/stone-agent/.dockerignore` - Optimized build context
   - Works with: Google Cloud Run, AWS, Azure, DigitalOcean, self-hosted

4. **Production Environment**
   - `/stone-agent/.env.production` - Production environment template
   - `/stone-agent/scripts/start-production.js` - Unified startup script
   - Updated `package.json` with production scripts

5. **Documentation**
   - `/stone-agent/DEPLOYMENT.md` - Complete deployment guide (all platforms)
   - `/stone-agent/QUICKSTART_DEPLOYMENT.md` - 10-minute Railway quick start
   - `/stone-agent/DEPLOYMENT_CHECKLIST.md` - Step-by-step verification checklist

---

## How the Agent Server Works

The stone-agent runs **TWO processes** in production:

### 1. Token Server (Port 8000)
- **Purpose**: HTTP API for Android app
- **Endpoints**:
  - `GET /health` - Health check
  - `GET /api/agents` - List available agents
  - `GET /api/connection-details?roomName=X&participantId=Y` - Get LiveKit credentials
- **Tech**: Express.js + LiveKit Server SDK

### 2. Agent Worker (Port 8081)
- **Purpose**: LiveKit agent processing
- **Handles**:
  - Voice processing (STT/TTS)
  - AI agent dispatch (router, tick, listen, etc.)
  - Tool calling and execution
- **Tech**: LiveKit Agents SDK + Silero VAD

Both processes start together via `npm run start:production`.

---

## Required Environment Variables

Before deploying, you need LiveKit credentials:

1. **Sign up**: https://cloud.livekit.io (free tier)
2. **Create project**: Get your credentials
3. **Required variables**:
   ```
   LIVEKIT_URL=wss://your-project.livekit.cloud
   LIVEKIT_API_KEY=APIxxxxxxxxxxxxx
   LIVEKIT_API_SECRET=xxxxxxxxxxxxxxxxxxxxx
   ```

Optional (recommended to omit for cost savings):
- `OPENAI_API_KEY` - Uses inference gateway if not set
- `ASSEMBLYAI_API_KEY` - Uses inference gateway if not set
- etc.

---

## Deployment Options

### Option A: Railway (RECOMMENDED)
**Why**: Simplest, excellent Node.js support, $5/month
**Time**: 10 minutes
**Guide**: See `QUICKSTART_DEPLOYMENT.md`

```bash
cd stone-agent
railway login
railway init
railway variables set LIVEKIT_URL="wss://..."
railway variables set LIVEKIT_API_KEY="..."
railway variables set LIVEKIT_API_SECRET="..."
railway up
```

### Option B: Fly.io
**Why**: Global edge deployment, generous free tier
**Time**: 15 minutes
**Guide**: See `DEPLOYMENT.md` Section B

```bash
cd stone-agent
fly launch --config fly.toml
fly secrets set LIVEKIT_URL="wss://..."
fly deploy
```

### Option C: Google Cloud Run
**Why**: Pay-per-use, serverless, auto-scaling
**Time**: 20 minutes
**Guide**: See `DEPLOYMENT.md` Section C

### Option D: Docker (Any Platform)
**Why**: Self-hosted or other cloud providers
**Time**: 30+ minutes
**Guide**: See `DEPLOYMENT.md` Section D

---

## Testing Your Deployment

After deploying, test these endpoints:

```bash
# 1. Health check
curl https://your-deployment.com/health

# 2. List agents
curl https://your-deployment.com/api/agents

# 3. Get connection details
curl "https://your-deployment.com/api/connection-details?roomName=test&participantId=user1"
```

All should return JSON responses without errors.

---

## Android App Integration

Once deployed, update your Android app:

```kotlin
// In StoneConfig.kt or similar
object StoneConfig {
    const val AGENT_SERVER_URL = "https://your-deployment.railway.app"
}

// When connecting to voice agent
val response = httpClient.get("$AGENT_SERVER_URL/api/connection-details") {
    parameter("roomName", "user-${userId}")
    parameter("participantId", userId)
}

val connectionDetails = response.body<ConnectionDetails>()
// Use connectionDetails.serverUrl, connectionDetails.participantToken
```

---

## Cost Breakdown

### Railway Deployment (Recommended)
- **LiveKit Cloud**: Free tier (10,000 minutes/month)
- **Railway Hobby**: $5/month
- **AI Inference**: Included in LiveKit (using inference gateway)
- **Total**: ~$5/month

### Fly.io Deployment
- **LiveKit Cloud**: Free tier (10,000 minutes/month)
- **Fly.io Free Tier**: 3 VMs with 256MB RAM
- **Beyond Free**: ~$5-10/month
- **Total**: $0-10/month

### Google Cloud Run
- **LiveKit Cloud**: Free tier (10,000 minutes/month)
- **Cloud Run Free Tier**: 2M requests/month
- **Beyond Free**: ~$0.10 per 100K requests
- **Total**: $0-5/month (variable)

---

## What Happens When Android Connects

1. **Android app** calls `/api/connection-details?roomName=X&participantId=Y`
2. **Token server** generates LiveKit access token
3. **Token server** dispatches agent to room
4. **Token server** returns connection details to Android
5. **Android app** connects to LiveKit using token
6. **Agent worker** joins the same room
7. **User speaks** → Android sends audio to LiveKit
8. **Agent worker** processes audio (STT → LLM → TTS)
9. **Agent responds** → Android plays TTS audio
10. **Agent executes tools** via Intent API if needed

---

## Monitoring & Maintenance

### Set Up Monitoring
- **Uptime**: UptimeRobot (free) - monitors `/health` endpoint
- **Errors**: Sentry (free tier) - tracks crashes and exceptions
- **Logs**: Platform built-in (Railway/Fly/Cloud Run dashboards)

### Performance Targets
- Health check response: < 200ms
- Connection details API: < 500ms
- Voice round trip latency: < 500ms
- Memory usage: < 400MB
- CPU usage: < 50%
- Uptime: > 99.9%

---

## Next Steps for User

1. **Choose platform**: Railway recommended for simplicity
2. **Get LiveKit credentials**: Sign up at cloud.livekit.io
3. **Follow quick start**: See `QUICKSTART_DEPLOYMENT.md` for Railway
4. **Test endpoints**: Verify health, agents, connection-details
5. **Update Android app**: Set AGENT_SERVER_URL to deployment URL
6. **Test voice**: Try end-to-end voice interaction
7. **Set up monitoring**: UptimeRobot for health checks
8. **Document**: Fill out deployment info in checklist

---

## Files Created

```
stone-agent/
├── railway.json              # Railway deployment config
├── railway.toml              # Railway config with health checks
├── fly.toml                  # Fly.io deployment config
├── Dockerfile                # Docker multi-stage build
├── .dockerignore             # Docker build optimization
├── .env.production           # Production environment template
├── scripts/
│   └── start-production.js   # Production startup script
├── DEPLOYMENT.md             # Complete deployment guide (all platforms)
├── QUICKSTART_DEPLOYMENT.md  # 10-minute Railway quick start
└── DEPLOYMENT_CHECKLIST.md   # Step-by-step verification

DEPLOYMENT_STATUS.md          # This file (project root)
```

---

## Support Resources

- **Full Guide**: `/stone-agent/DEPLOYMENT.md`
- **Quick Start**: `/stone-agent/QUICKSTART_DEPLOYMENT.md`
- **Checklist**: `/stone-agent/DEPLOYMENT_CHECKLIST.md`
- **LiveKit Docs**: https://docs.livekit.io
- **Railway Docs**: https://docs.railway.app
- **Fly.io Docs**: https://fly.io/docs

---

## Ready to Deploy? 🚀

**Recommended Path**: Start with Railway using the quick start guide.

```bash
cd stone-agent
cat QUICKSTART_DEPLOYMENT.md  # Read the 10-minute guide
# Then follow the steps - you'll be deployed in minutes!
```

---

**Status**: ✅ All configuration complete, ready for cloud deployment
**Estimated Time to Deploy**: 10-30 minutes (depending on platform)
**Estimated Monthly Cost**: $5-10
**Next Ticket**: TICKET_014 - Android LiveKit integration (connects to this deployment)
