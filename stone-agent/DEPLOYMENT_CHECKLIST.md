# Stone Agent Deployment Checklist

Use this checklist to ensure successful deployment.

---

## Pre-Deployment

### LiveKit Account Setup
- [ ] Created LiveKit Cloud account at https://cloud.livekit.io
- [ ] Created a new project
- [ ] Copied `LIVEKIT_URL` (wss://...)
- [ ] Copied `LIVEKIT_API_KEY` (API...)
- [ ] Copied `LIVEKIT_API_SECRET`
- [ ] Saved credentials securely (password manager recommended)

### Platform Selection
- [ ] Chose deployment platform:
  - [ ] Railway (recommended for simplicity)
  - [ ] Fly.io (recommended for global edge)
  - [ ] Google Cloud Run (recommended for pay-per-use)
  - [ ] Docker (recommended for self-hosted)
  - [ ] Other: __________________

### Local Testing (Optional but Recommended)
- [ ] Built project locally: `npm run build`
- [ ] Verified build succeeded (check `dist/` folder)
- [ ] Created `.env` file with credentials
- [ ] Tested token server: `npm run start:token`
- [ ] Verified health endpoint: `curl http://localhost:8000/health`
- [ ] Verified agents endpoint: `curl http://localhost:8000/api/agents`

---

## Deployment

### Railway Deployment
- [ ] Installed Railway CLI: `npm install -g @railway/cli`
- [ ] Authenticated: `railway login`
- [ ] Initialized project: `railway init`
- [ ] Set `LIVEKIT_URL` variable
- [ ] Set `LIVEKIT_API_KEY` variable
- [ ] Set `LIVEKIT_API_SECRET` variable
- [ ] Set `NODE_ENV=production` variable
- [ ] Deployed: `railway up`
- [ ] Copied deployment URL: https://__________________.railway.app
- [ ] Verified build succeeded in Railway dashboard
- [ ] Checked logs for errors: `railway logs`

### Fly.io Deployment
- [ ] Installed Fly CLI
- [ ] Authenticated: `fly auth login`
- [ ] Launched app: `fly launch --config fly.toml`
- [ ] Set secrets: `fly secrets set LIVEKIT_URL=...`
- [ ] Set secrets: `fly secrets set LIVEKIT_API_KEY=...`
- [ ] Set secrets: `fly secrets set LIVEKIT_API_SECRET=...`
- [ ] Deployed: `fly deploy`
- [ ] Copied deployment URL: https://__________________.fly.dev
- [ ] Checked status: `fly status`
- [ ] Viewed logs: `fly logs`

### Google Cloud Run Deployment
- [ ] Installed gcloud CLI
- [ ] Authenticated: `gcloud auth login`
- [ ] Set project: `gcloud config set project PROJECT_ID`
- [ ] Built container: `gcloud builds submit --tag gcr.io/PROJECT_ID/stone-agent`
- [ ] Deployed to Cloud Run with environment variables
- [ ] Copied deployment URL: https://__________________.run.app
- [ ] Verified deployment in Cloud Console
- [ ] Checked logs in Cloud Console

### Docker Deployment
- [ ] Built image: `docker build -t stone-agent:latest .`
- [ ] Tested locally with environment variables
- [ ] Pushed to registry: `docker push ...`
- [ ] Deployed to target platform
- [ ] Copied deployment URL: https://__________________
- [ ] Verified container is running
- [ ] Checked container logs

---

## Post-Deployment Verification

### Health Checks
- [ ] Health endpoint responding: `curl https://YOUR-URL/health`
  - [ ] Returns status: "healthy"
  - [ ] Returns correct livekit_url
  - [ ] Response time < 200ms

### API Endpoints
- [ ] Agents list working: `curl https://YOUR-URL/api/agents`
  - [ ] Returns success: true
  - [ ] Lists all 13 agents
  - [ ] All agents show available: true

- [ ] Connection details working: `curl "https://YOUR-URL/api/connection-details?roomName=test&participantId=test"`
  - [ ] Returns serverUrl
  - [ ] Returns roomName
  - [ ] Returns participantName
  - [ ] Returns participantToken (JWT)

### Log Verification
- [ ] Token server started successfully
- [ ] Agent worker started successfully
- [ ] No error messages in logs
- [ ] Silero VAD model loaded
- [ ] LiveKit connection established

### Performance
- [ ] Health check responds in < 200ms
- [ ] Connection details API responds in < 500ms
- [ ] No memory leaks after 1 hour
- [ ] CPU usage stays below 50%

---

## Integration Testing

### Android App Configuration
- [ ] Updated `AGENT_SERVER_URL` in Android app
- [ ] Updated to production URL (https://...)
- [ ] Rebuilt Android app with new URL
- [ ] Installed on test device

### End-to-End Testing
- [ ] Android app connects to agent server
- [ ] Can retrieve connection details
- [ ] Can join LiveKit room
- [ ] Can send voice input
- [ ] Agent responds to voice
- [ ] TTS audio plays correctly
- [ ] Agent can dispatch to sub-agents
- [ ] Tool calls execute successfully

### Voice Features
- [ ] Speech-to-text working (AssemblyAI)
- [ ] LLM responses working (OpenAI GPT-4 mini)
- [ ] Text-to-speech working (Cartesia)
- [ ] Voice activity detection working (Silero VAD)
- [ ] Latency < 500ms for full round trip

---

## Monitoring & Alerts

### Uptime Monitoring
- [ ] Created UptimeRobot account (or alternative)
- [ ] Added monitor for `/health` endpoint
- [ ] Set check interval to 5 minutes
- [ ] Configured email alerts
- [ ] Configured SMS alerts (optional)
- [ ] Tested alert by stopping service

### Error Tracking
- [ ] Set up Sentry (or alternative)
- [ ] Configured DSN in environment variables
- [ ] Tested error reporting
- [ ] Set up alert rules for critical errors

### Logging
- [ ] Can access production logs
- [ ] Logs include timestamps
- [ ] Logs show both token server and agent worker
- [ ] Error logs are clearly marked
- [ ] Log retention configured (30+ days recommended)

---

## Security

### Credentials
- [ ] Never committed secrets to Git
- [ ] Used `.env.production` template only (no real values)
- [ ] Stored credentials in platform secrets/variables
- [ ] Backed up credentials in secure password manager
- [ ] Shared credentials with team via secure method

### Network Security
- [ ] Deployment uses HTTPS only
- [ ] CORS configured correctly (not wildcard in production)
- [ ] No sensitive data in logs
- [ ] API endpoints don't expose internal details

### Access Control
- [ ] Production environment variables protected
- [ ] Deployment platform requires authentication
- [ ] Only authorized team members have access
- [ ] Two-factor authentication enabled on platform

---

## Documentation

### Internal Documentation
- [ ] Saved deployment URL
- [ ] Documented environment variables used
- [ ] Documented deployment date
- [ ] Documented platform used
- [ ] Created runbook for common issues
- [ ] Shared access with team

### Android App Updates
- [ ] Updated Android app configuration
- [ ] Updated API endpoint URLs
- [ ] Tested with production agent server
- [ ] Documented any API changes
- [ ] Updated Android app README

---

## Cost Management

### Platform Costs
- [ ] Reviewed pricing for chosen platform
- [ ] Set up billing alerts
- [ ] Configured budget limits
- [ ] Estimated monthly cost: $__________
- [ ] Approved by budget owner

### LiveKit Costs
- [ ] On free tier (10K minutes/month)
- [ ] Set up usage alerts at 75% of free tier
- [ ] Approved upgrade plan if needed: __________

---

## Rollback Plan

### Backup
- [ ] Previous deployment still accessible
- [ ] Can rollback via platform UI
- [ ] Tested rollback procedure
- [ ] Documented rollback steps

### Recovery
- [ ] Have local backup of working code
- [ ] Can redeploy from scratch in < 30 minutes
- [ ] Team knows who to contact for issues
- [ ] Emergency contact list created

---

## Final Checks

- [ ] All tests passing
- [ ] No critical errors in logs
- [ ] Performance metrics acceptable
- [ ] Monitoring alerts working
- [ ] Team notified of deployment
- [ ] Documentation updated
- [ ] Deployment marked as successful

---

## Deployment Information

**Record this information for future reference:**

```
Deployment Date: _______________
Platform: _______________
Production URL: https://_______________________________________________
Health Check: https://_______________________________________________/health
API Base URL: https://_______________________________________________/api

LiveKit Project: _______________
LiveKit URL: wss://_______________________________________________

Deployed By: _______________
Reviewed By: _______________
Approved By: _______________

Monthly Cost Estimate: $_______________
Expected Traffic: _______________ requests/month
Expected Agent Minutes: _______________ minutes/month

Monitoring:
- Uptime: _______________
- Errors: _______________
- Logs: _______________

Next Review Date: _______________
```

---

## Troubleshooting Reference

If any check fails, see:
- `DEPLOYMENT.md` - Full deployment guide
- `QUICKSTART_DEPLOYMENT.md` - Quick start for Railway
- `README.md` - Project overview and local development

Common issues:
- Health check failing → Check logs for startup errors
- Agent not connecting → Verify LIVEKIT_URL, API_KEY, API_SECRET
- Build failing → Ensure Node 20+, check build logs
- High latency → Check region, consider deploying closer to users

---

**Status**:
- [ ] Pre-Deployment Complete
- [ ] Deployment Complete
- [ ] Post-Deployment Verification Complete
- [ ] Integration Testing Complete
- [ ] Monitoring Setup Complete
- [ ] Security Verified
- [ ] Documentation Complete
- [ ] **READY FOR PRODUCTION** ✅
