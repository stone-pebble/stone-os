# SSH Setup for VS Code/Cursor

## Cloud Instance Details
- **Instance Name**: stoneos-dev
- **Zone**: us-central1-a
- **Status**: Running (persistent instance)
- **Specs**: 16 vCPUs, 64GB RAM, 500GB disk

## Setting up SSH in VS Code/Cursor

### 1. Install the Remote-SSH Extension
In VS Code/Cursor, install the "Remote - SSH" extension by Microsoft.

### 2. Configure SSH
Add the following to your SSH config file (`~/.ssh/config`):

```
Host stoneos-dev
    HostName <external-ip>  # Get this from: gcloud compute instances describe stoneos-dev --zone=us-central1-a --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
    User samuellarson
    IdentityFile ~/.ssh/google_compute_engine
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
```

Or use the gcloud SSH proxy (recommended):

```
Host stoneos-dev
    HostName stoneos-dev.us-central1-a.stone-pebble
    User samuellarson
    ProxyCommand gcloud compute ssh --zone=us-central1-a stoneos-dev --command="nc %h %p"
    StrictHostKeyChecking no
    UserKnownHostsFile /dev/null
```

### 3. Connect from VS Code/Cursor
1. Open Command Palette (Cmd+Shift+P)
2. Type "Remote-SSH: Connect to Host..."
3. Select "stoneos-dev"
4. Open folder: `/home/samuellarson/stone-os`

## Repository Structure on Cloud Instance

```
~/stone-os/                  # Main StoneOS development repo
├── BUILD_PROCESS.md        # Build documentation
├── CLAUDE.md              # Claude Code instructions
├── README.md              # Project overview
├── build_stoneos.sh       # Main build script
├── stone/                 # Stone Java components
│   ├── StonePanel.java
│   └── StoneIcon.java
├── stone-launcher/        # React Native launcher
├── stone-agent/          # LiveKit agents
├── mcp-servers/          # MCP implementations
└── SystemUI/             # SystemUI modifications

~/aosp/                    # AOSP source tree (downloading)
├── frameworks/base/       # Using our fork with Stone components
└── ...                   # Full AOSP tree
```

## Build Scripts Available

```bash
# Check AOSP download status
~/check_status.sh

# Build SystemUI with Stone components
~/build_systemui.sh
```

## Managing the Instance

```bash
# Stop instance (saves money, keeps disk)
gcloud compute instances stop stoneos-dev --zone=us-central1-a

# Start instance
gcloud compute instances start stoneos-dev --zone=us-central1-a

# Check instance status
gcloud compute instances describe stoneos-dev --zone=us-central1-a
```

## Cost Management
- **Running**: ~$0.77/hour
- **Stopped**: ~$20/month (disk storage only)
- **Recommendation**: Stop when not actively developing

## AOSP Build Status
The AOSP source is currently downloading in the background. Check progress:
```bash
tail -f ~/aosp_sync.log
```

Expected download time: ~2-3 hours (150GB)