# Quick Start: Deploy Stone Agent in 10 Minutes

The fastest way to get your Stone Agent server running in production.

---

## Step 1: Get LiveKit Credentials (2 minutes)

1. Go to https://cloud.livekit.io
2. Sign up for free account
3. Create a new project
4. Copy these three values:
   - `LIVEKIT_URL` (looks like: wss://your-project.livekit.cloud)
   - `LIVEKIT_API_KEY` (starts with: API...)
   - `LIVEKIT_API_SECRET` (random string)

**Save these somewhere safe - you'll need them in Step 3.**

---

## Step 2: Deploy to Railway (3 minutes)

Railway is the simplest deployment platform.

### A. Install Railway CLI

```bash
npm install -g @railway/cli
```

### B. Deploy

```bash
cd stone-agent
railway login
railway init
```

Railway will:
- Create a new project
- Link it to your directory
- Open your browser for authentication

---

## Step 3: Configure Environment (2 minutes)

Set your LiveKit credentials:

```bash
railway variables set LIVEKIT_URL="wss://your-project.livekit.cloud"
railway variables set LIVEKIT_API_KEY="your_api_key_here"
railway variables set LIVEKIT_API_SECRET="your_secret_here"
railway variables set NODE_ENV="production"
```

---

## Step 4: Deploy! (2 minutes)

```bash
railway up
```

Railway will:
- Install dependencies
- Build TypeScript
- Start both servers (token server + agent worker)
- Give you a public HTTPS URL

---

## Step 5: Test Deployment (1 minute)

```bash
# Get your URL
railway status

# Test it
curl https://your-app.railway.app/health
```

You should see:
```json
{
  "status": "healthy",
  "timestamp": "...",
  "livekit_url": "wss://..."
}
```

**Success!** Your agent server is now running in the cloud.

---

## Step 6: Save Your URL

Your deployment URL is:
```
https://[your-app-name].railway.app
```

Update your Android app to use this URL:

```kotlin
// In Android app
const val AGENT_SERVER_URL = "https://your-app.railway.app"
```

---

## What Just Happened?

You deployed TWO services:

1. **Token Server** (port 8000)
   - HTTP API at `https://your-app.railway.app`
   - Generates LiveKit connection tokens
   - Handles agent dispatch

2. **Agent Worker** (port 8081)
   - Connects to LiveKit Cloud
   - Processes voice interactions
   - Runs AI agents (router, tick, listen, etc.)

---

## Next Steps

- ✅ Test voice functionality from Android app
- ✅ Set up monitoring: https://uptimerobot.com
- ✅ Configure custom domain (optional)
- ✅ Review full deployment guide: `DEPLOYMENT.md`

---

## Cost

- **LiveKit Cloud**: Free tier (10K minutes/month)
- **Railway**: $5/month (Hobby plan)
- **Total**: ~$5/month

---

## Troubleshooting

### "Environment variables not set"

```bash
# Check your variables
railway variables

# Set any missing ones
railway variables set VARIABLE_NAME="value"
```

### "Build failed"

```bash
# View logs
railway logs

# Common fix: ensure Node 20+ is available
# Railway auto-detects this from package.json
```

### "Can't connect from Android"

1. Verify health endpoint works: `curl https://your-app.railway.app/health`
2. Check Android logs for connection errors
3. Ensure URL in Android matches your Railway deployment URL

---

## Alternative Platforms

If Railway doesn't work for you:

- **Fly.io**: See `DEPLOYMENT.md` Section B
- **Google Cloud Run**: See `DEPLOYMENT.md` Section C
- **Docker**: See `DEPLOYMENT.md` Section D

---

## Support

Having issues? Check:
- Full deployment guide: `DEPLOYMENT.md`
- LiveKit docs: https://docs.livekit.io
- Railway docs: https://docs.railway.app

---

**Total Time**: 10 minutes ⏱️
**Monthly Cost**: $5 💰
**Difficulty**: Easy ⭐
