# TICKET_013: Deploy Stone Agent to Cloud Infrastructure

## Status: CONFIGURATION COMPLETE - READY FOR USER DEPLOYMENT
Priority: CRITICAL
Type: Implementation
Estimated Time: 2-3 hours (10 minutes for user to deploy)

## Context
Based on TICKET_008 research findings, embedding Node.js in Android is NOT FEASIBLE due to @livekit/rtc-node incompatibility. This ticket implements the RECOMMENDED cloud deployment approach.

## Description
Deploy the existing stone-agent server to cloud infrastructure, enabling voice AI functionality without modifying the Android app architecture.

## Why This Approach
- ✅ Uses existing `/stone-agent/` code AS-IS (no modifications needed)
- ✅ Matches LiveKit's intended architecture
- ✅ 2 days to MVP vs. 6-7 weeks for alternatives
- ✅ Passes all performance criteria (memory, APK size, battery)
- ✅ Industry-standard approach

## Acceptance Criteria
- [x] Deployment configuration files created for all major platforms
- [x] Production startup script created (starts both token server and agent worker)
- [x] Environment variable templates created
- [x] Health check endpoints implemented (already in code)
- [x] Auto-restart configuration added (Railway/Fly/Docker)
- [x] Comprehensive deployment documentation created
- [x] Quick start guide created (10-minute Railway deployment)
- [x] Deployment checklist created for verification
- [ ] **USER ACTION REQUIRED**: User deploys to chosen platform
- [ ] **USER ACTION REQUIRED**: User tests endpoints
- [ ] **USER ACTION REQUIRED**: User configures monitoring

## Implementation Tasks

### 1. Choose Platform (30 min)
**Option A: Railway (RECOMMENDED)**
- Pros: Simple deployment, good Node.js support, $5/month
- Cons: Limited free tier

**Option B: Fly.io**
- Pros: Good free tier, global edge deployment
- Cons: More complex configuration

**Option C: Google Cloud Run**
- Pros: Pay-per-use, auto-scaling
- Cons: More setup required

### 2. Deploy Agent Server (1 hour)
```bash
# For Railway
cd stone-agent
railway login
railway init
railway up

# Add environment variables in Railway dashboard:
# - LIVEKIT_URL
# - LIVEKIT_API_KEY
# - LIVEKIT_API_SECRET
# - OPENAI_API_KEY
```

### 3. Configure Production Settings (30 min)
```javascript
// Update for production
const isProd = process.env.NODE_ENV === 'production';
const config = {
  port: process.env.PORT || 8000,
  livekitUrl: process.env.LIVEKIT_URL,
  corsOrigin: isProd ? 'https://stone-app.com' : '*'
};
```

### 4. Set Up Monitoring (30 min)
- Add health check endpoint
- Configure uptime monitoring (UptimeRobot/Pingdom)
- Set up error logging (Sentry/LogRocket)
- Configure auto-restart policy

### 5. Test Deployment (30 min)
```bash
# Test endpoints
curl https://stone-agent.railway.app/health
curl https://stone-agent.railway.app/api/agents
curl "https://stone-agent.railway.app/api/connection-details?roomName=test"
```

## Cost Analysis
- **Railway**: ~$5/month (Hobby plan)
- **LiveKit Cloud**: Free tier (10K minutes/month)
- **Total**: ~$5/month for MVP
- **Scales to**: Unlimited devices (one server serves all)

## Success Metrics
- [ ] Deployment completes in < 3 hours
- [ ] Agent responds to requests
- [ ] < 200ms response time
- [ ] 99.9% uptime achieved
- [ ] Zero configuration needed on Android side

## Documentation Required
- Deployment URL
- Environment variable list
- API endpoints documentation
- Troubleshooting guide

## Implementation Complete

All deployment configuration and documentation has been created. The following files are ready:

### Configuration Files
- `/stone-agent/railway.json` - Railway JSON config
- `/stone-agent/railway.toml` - Railway TOML config with health checks
- `/stone-agent/fly.toml` - Fly.io deployment config
- `/stone-agent/Dockerfile` - Multi-stage Docker build
- `/stone-agent/.dockerignore` - Docker build optimization
- `/stone-agent/.env.production` - Production environment template
- `/stone-agent/scripts/start-production.js` - Unified production startup

### Documentation
- `/stone-agent/DEPLOYMENT.md` - Complete guide for all platforms (Railway, Fly.io, Cloud Run, Docker)
- `/stone-agent/QUICKSTART_DEPLOYMENT.md` - 10-minute Railway quick start
- `/stone-agent/DEPLOYMENT_CHECKLIST.md` - Step-by-step verification checklist
- `/DEPLOYMENT_STATUS.md` - Overall deployment status and next steps

### Updated Files
- `/stone-agent/package.json` - Added production scripts

## User Next Steps

1. **Get LiveKit credentials** (2 minutes)
   - Sign up: https://cloud.livekit.io
   - Create project
   - Copy: LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET

2. **Deploy to Railway** (10 minutes) - RECOMMENDED
   ```bash
   cd stone-agent
   cat QUICKSTART_DEPLOYMENT.md  # Read the guide
   railway login
   railway init
   railway variables set LIVEKIT_URL="wss://..."
   railway variables set LIVEKIT_API_KEY="..."
   railway variables set LIVEKIT_API_SECRET="..."
   railway up
   ```

3. **Test deployment** (2 minutes)
   ```bash
   railway status  # Get URL
   curl https://your-app.railway.app/health
   curl https://your-app.railway.app/api/agents
   ```

4. **Update Android app** with deployment URL

## Alternative Platforms

If Railway doesn't work, see:
- **Fly.io**: `DEPLOYMENT.md` Section B
- **Google Cloud Run**: `DEPLOYMENT.md` Section C
- **Docker**: `DEPLOYMENT.md` Section D

## Notes
- This approach was validated by extensive research in TICKET_008
- The stone-agent code is already production-ready
- No Android app changes needed initially
- All configuration is complete - user just needs to deploy
- Estimated deployment time: 10 minutes (Railway quick start)
- Estimated monthly cost: ~$5 (Railway Hobby plan)