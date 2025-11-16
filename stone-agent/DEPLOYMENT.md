# Stone Agent Server - Deployment Guide

Complete guide to deploying the Stone Agent server to cloud infrastructure.

---

## Prerequisites

Before deploying, you need:

1. **LiveKit Cloud Account** (Free tier available)
   - Sign up at: https://cloud.livekit.io
   - Create a new project
   - Get your credentials:
     - `LIVEKIT_URL` (e.g., wss://your-project.livekit.cloud)
     - `LIVEKIT_API_KEY`
     - `LIVEKIT_API_SECRET`

2. **Cloud Platform Account** (choose one):
   - Railway.app (recommended, $5/month)
   - Fly.io (good free tier)
   - Google Cloud Run (pay-per-use)
   - AWS, Azure, DigitalOcean, etc.

---

## Option A: Railway Deployment (RECOMMENDED)

Railway is the simplest option with excellent Node.js support.

### Step 1: Install Railway CLI

```bash
npm install -g @railway/cli
```

### Step 2: Login to Railway

```bash
railway login
```

### Step 3: Initialize Project

```bash
cd stone-agent
railway init
```

This creates a new Railway project linked to your directory.

### Step 4: Set Environment Variables

```bash
railway variables set LIVEKIT_URL="wss://your-project.livekit.cloud"
railway variables set LIVEKIT_API_KEY="your_api_key"
railway variables set LIVEKIT_API_SECRET="your_api_secret"
railway variables set NODE_ENV="production"
railway variables set TOKEN_SERVER_PORT="8000"
railway variables set AGENT_PORT="8081"
```

### Step 5: Deploy

```bash
railway up
```

Railway will:
- Detect Node.js and install dependencies
- Build TypeScript (`npm run build`)
- Start the production server (`npm run start:production`)
- Provide you with a public HTTPS URL

### Step 6: Verify Deployment

```bash
# Get your deployment URL
railway status

# Test health endpoint
curl https://your-app.railway.app/health

# Test agents endpoint
curl https://your-app.railway.app/api/agents
```

### Railway Dashboard Configuration

1. Go to https://railway.app/dashboard
2. Select your project
3. Configure:
   - **Custom Domain** (optional): Add your own domain
   - **Autoscaling**: Enable if needed
   - **Health Checks**: Already configured in railway.toml
   - **Logs**: View real-time logs

---

## Option B: Fly.io Deployment

Fly.io offers global edge deployment with a generous free tier.

### Step 1: Install Fly CLI

```bash
# macOS
brew install flyctl

# Linux/WSL
curl -L https://fly.io/install.sh | sh
```

### Step 2: Login to Fly.io

```bash
fly auth login
```

### Step 3: Launch App

```bash
cd stone-agent
fly launch --config fly.toml
```

Fly will ask:
- **App name**: Choose a unique name (e.g., stone-agent-prod)
- **Region**: Select closest to your users
- **Create Postgres**: No (we don't need a database)

### Step 4: Set Secrets

```bash
fly secrets set LIVEKIT_URL="wss://your-project.livekit.cloud"
fly secrets set LIVEKIT_API_KEY="your_api_key"
fly secrets set LIVEKIT_API_SECRET="your_api_secret"
```

### Step 5: Deploy

```bash
fly deploy
```

### Step 6: Verify Deployment

```bash
# Check status
fly status

# View logs
fly logs

# Test endpoints
curl https://your-app.fly.dev/health
curl https://your-app.fly.dev/api/agents
```

### Fly.io Configuration

Edit `fly.toml` to customize:
- **Region**: Change `primary_region` to your preferred location
- **Resources**: Adjust `memory_mb` and `cpus` as needed
- **Scaling**: Modify `min_machines` and `max_machines`

---

## Option C: Google Cloud Run

Cloud Run is pay-per-use serverless platform from Google.

### Step 1: Install gcloud CLI

Follow: https://cloud.google.com/sdk/docs/install

### Step 2: Authenticate

```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

### Step 3: Build Container

```bash
cd stone-agent
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/stone-agent
```

### Step 4: Deploy to Cloud Run

```bash
gcloud run deploy stone-agent \
  --image gcr.io/YOUR_PROJECT_ID/stone-agent \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --port 8000 \
  --set-env-vars LIVEKIT_URL="wss://your-project.livekit.cloud" \
  --set-env-vars LIVEKIT_API_KEY="your_api_key" \
  --set-env-vars LIVEKIT_API_SECRET="your_api_secret" \
  --set-env-vars NODE_ENV="production" \
  --memory 512Mi \
  --cpu 1 \
  --min-instances 1 \
  --max-instances 10
```

### Step 5: Verify

```bash
# Get URL
gcloud run services describe stone-agent --region us-central1 --format 'value(status.url)'

# Test
curl https://stone-agent-xxx.run.app/health
```

---

## Option D: Docker Deployment (Any Platform)

Use this for DigitalOcean, AWS, Azure, or self-hosted servers.

### Step 1: Build Docker Image

```bash
cd stone-agent
docker build -t stone-agent:latest .
```

### Step 2: Test Locally

```bash
docker run -p 8000:8000 -p 8081:8081 \
  -e LIVEKIT_URL="wss://your-project.livekit.cloud" \
  -e LIVEKIT_API_KEY="your_api_key" \
  -e LIVEKIT_API_SECRET="your_api_secret" \
  -e NODE_ENV="production" \
  stone-agent:latest
```

### Step 3: Push to Registry

```bash
# Docker Hub
docker tag stone-agent:latest yourusername/stone-agent:latest
docker push yourusername/stone-agent:latest

# Or use cloud registry (GCR, ECR, ACR, etc.)
```

### Step 4: Deploy to Your Platform

Follow platform-specific instructions for running Docker containers.

---

## Environment Variables Reference

### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `LIVEKIT_URL` | LiveKit server WebSocket URL | `wss://project.livekit.cloud` |
| `LIVEKIT_API_KEY` | LiveKit API key | `APIxxxxxxxxxxxxx` |
| `LIVEKIT_API_SECRET` | LiveKit API secret | `xxxxxxxxxxxxx` |

### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `NODE_ENV` | Environment mode | `production` |
| `TOKEN_SERVER_PORT` | HTTP API port | `8000` |
| `AGENT_PORT` | Agent worker port | `8081` |
| `LOG_LEVEL` | Logging level | `info` |
| `CORS_ORIGIN` | CORS allowed origins | `*` |

### AI Provider Keys (Optional)

These are OPTIONAL. If not provided, the agent uses LiveKit's inference gateway:

- `OPENAI_API_KEY` - For GPT models
- `ASSEMBLYAI_API_KEY` - For speech-to-text
- `DEEPGRAM_API_KEY` - Alternative STT
- `ELEVENLABS_API_KEY` - For text-to-speech
- `CARTESIA_API_KEY` - Alternative TTS

**Recommendation**: Use inference gateway (don't set these) to reduce costs.

---

## Testing Your Deployment

### 1. Health Check

```bash
curl https://your-deployment-url.com/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2025-11-14T...",
  "livekit_url": "wss://..."
}
```

### 2. List Available Agents

```bash
curl https://your-deployment-url.com/api/agents
```

Expected response:
```json
{
  "success": true,
  "agents": [
    { "name": "router", "description": "Main routing agent", "available": true },
    { "name": "tick", "description": "Time management", "available": true },
    ...
  ]
}
```

### 3. Get Connection Details

```bash
curl "https://your-deployment-url.com/api/connection-details?roomName=test-room&participantId=test-user"
```

Expected response:
```json
{
  "serverUrl": "wss://...",
  "roomName": "test-room",
  "participantName": "test-user",
  "participantToken": "eyJhbGc..."
}
```

### 4. Monitor Logs

Check deployment logs for any errors:

```bash
# Railway
railway logs

# Fly.io
fly logs

# Cloud Run
gcloud run services logs read stone-agent --region us-central1

# Docker
docker logs <container-id>
```

---

## Connecting Android App

Update your Android app configuration with the deployment URL:

```kotlin
// In your Android app
object StoneConfig {
    const val AGENT_SERVER_URL = "https://your-deployment-url.com"
}

// When connecting to LiveKit
val connectionDetails = httpClient.get("$AGENT_SERVER_URL/api/connection-details") {
    parameter("roomName", "user-${userId}")
    parameter("participantId", userId)
}
```

---

## Monitoring & Maintenance

### Uptime Monitoring

Set up monitoring with:
- **UptimeRobot** (free): https://uptimerobot.com
- **Pingdom** (free tier): https://www.pingdom.com
- **Better Uptime**: https://betteruptime.com

Monitor the `/health` endpoint every 1-5 minutes.

### Error Tracking

Recommended error tracking services:
- **Sentry** (free tier): https://sentry.io
- **LogRocket**: https://logrocket.com
- **Rollbar**: https://rollbar.com

### Logging

Production logs are available through your platform:
- Railway: Dashboard → Logs
- Fly.io: `fly logs`
- Cloud Run: Cloud Console → Logs
- Docker: `docker logs <container>`

### Performance Monitoring

Track these metrics:
- Response time for `/api/connection-details` (should be < 200ms)
- Agent connection success rate (should be > 99%)
- Memory usage (should stay < 400MB)
- CPU usage (should stay < 50%)

---

## Cost Estimates

### Railway (Recommended)
- **Hobby Plan**: $5/month
- Includes: 512MB RAM, shared CPU, unlimited bandwidth
- Good for: MVP and early production

### Fly.io
- **Free Tier**: 3 shared CPU VMs, 256MB RAM each
- **Paid**: ~$5-10/month for 512MB VM
- Good for: Global edge deployment

### Google Cloud Run
- **Free Tier**: 2M requests/month, 360K GB-seconds/month
- **Beyond Free**: ~$0.10 per 100K requests
- Good for: Variable load, pay-per-use

### Total Monthly Cost
- **LiveKit Cloud**: Free tier (10K participant minutes/month)
- **Agent Deployment**: $0-5/month
- **AI Inference** (if using gateway): Included in LiveKit
- **Total**: ~$5/month for MVP

---

## Troubleshooting

### Issue: "Failed to connect to LiveKit"

**Solution**: Check environment variables:
```bash
# Railway
railway variables

# Fly.io
fly secrets list

# Verify they're set correctly
```

### Issue: "Health check failing"

**Solution**: Check logs for errors:
```bash
# Railway
railway logs --tail 100

# Fly.io
fly logs
```

### Issue: "Agent not joining room"

**Solution**: Verify agent worker is running:
- Check logs for "Agent joining room" messages
- Ensure both token server AND agent worker started
- Verify AGENT_PORT (8081) is accessible internally

### Issue: "Build failing"

**Solution**: Check Node.js version:
- Ensure Node 20+ is used
- Check `package.json` engines field
- Verify `npm run build` works locally

---

## Security Best Practices

1. **Never commit secrets**: Use `.env.production` template, not actual keys
2. **Rotate keys**: Change LiveKit API keys periodically
3. **Use HTTPS only**: All production deployments must use HTTPS
4. **Restrict CORS**: Set `CORS_ORIGIN` to your Android app domain
5. **Monitor logs**: Watch for unauthorized access attempts
6. **Update dependencies**: Run `npm audit` and update regularly

---

## Updating Deployment

### Railway
```bash
git push  # If connected to Git
# OR
railway up  # Manual deployment
```

### Fly.io
```bash
fly deploy
```

### Cloud Run
```bash
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/stone-agent
gcloud run deploy stone-agent --image gcr.io/YOUR_PROJECT_ID/stone-agent
```

---

## Rollback

### Railway
Dashboard → Deployments → Select previous deployment → Rollback

### Fly.io
```bash
fly releases list
fly releases rollback <release-id>
```

### Cloud Run
```bash
gcloud run services update-traffic stone-agent --to-revisions <revision>=100
```

---

## Next Steps

After deployment:
1. ✅ Save your deployment URL
2. ✅ Update Android app with URL
3. ✅ Test voice functionality end-to-end
4. ✅ Set up monitoring alerts
5. ✅ Configure custom domain (optional)
6. ✅ Enable auto-scaling if needed

---

## Support

- **LiveKit Docs**: https://docs.livekit.io
- **Railway Docs**: https://docs.railway.app
- **Fly.io Docs**: https://fly.io/docs
- **Cloud Run Docs**: https://cloud.google.com/run/docs

---

**Deployment URL Template**: Save this after deployment

```
Production URL: https://_____________________________.com
Health Check: https://_____________________________.com/health
Agents List: https://_____________________________.com/api/agents
Connection API: https://_____________________________.com/api/connection-details
Deployed: _______________ (date)
Platform: _______________ (Railway/Fly/etc.)
```
