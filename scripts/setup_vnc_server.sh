#!/bin/bash
#
# setup_vnc_server.sh
#
# Sets up VNC server on GCP instance for interactive AOSP emulator testing.
# This script should be run on the GCP instance (not locally).
#
# Part of Ticket #12: Establish Secure, Interactive Emulator Environment on GCP
#
# Usage: ./setup_vnc_server.sh [--resolution WIDTHxHEIGHT] [--display :N]
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Default configuration
RESOLUTION="1920x1080"
DISPLAY_NUMBER=":1"
VNC_PORT="5901"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --resolution)
            RESOLUTION="$2"
            shift 2
            ;;
        --display)
            DISPLAY_NUMBER="$2"
            # Calculate VNC port (5900 + display number)
            VNC_PORT=$((5900 + ${DISPLAY_NUMBER:1}))
            shift 2
            ;;
        --help|-h)
            cat << EOF
VNC Server Setup for StoneOS Emulator Testing

Usage: $0 [OPTIONS]

OPTIONS:
    --resolution WxH    Set VNC resolution (default: 1920x1080)
    --display :N        Set display number (default: :1, port 5901)
    --help, -h          Show this help message

EXAMPLES:
    $0                              # Use defaults (1920x1080 on :1)
    $0 --resolution 2560x1440       # Use higher resolution
    $0 --display :2                 # Use display :2 (port 5902)

NOTES:
    - This script installs XFCE desktop and TightVNC server
    - You will be prompted to set a VNC password on first run
    - After setup, connect from local machine using SSH tunnel
    - See scripts/connect_vnc.sh for local connection script

EOF
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  StoneOS VNC Server Setup (GCP Instance)  ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Check if running on GCP instance
if [ ! -f /tmp/ready ]; then
    echo -e "${YELLOW}⚠ Warning: /tmp/ready not found${NC}"
    echo -e "${YELLOW}  This script is designed for GCP AOSP build instances${NC}"
    echo -e "${YELLOW}  Continuing anyway...${NC}"
    echo ""
fi

# Phase 1: Install Desktop and VNC Server
echo -e "${CYAN}▶ Phase 1: Installing Desktop Environment${NC}"
echo ""

echo -e "${BLUE}Installing XFCE desktop and VNC server...${NC}"
echo -e "${YELLOW}This may take 5-10 minutes...${NC}"

if sudo apt-get update && sudo apt-get install -y xfce4 xfce4-goodies tightvncserver; then
    echo -e "${GREEN}✓ Desktop environment installed${NC}"
else
    echo -e "${RED}✗ Failed to install desktop environment${NC}"
    exit 1
fi
echo ""

# Phase 2: Initialize VNC Server
echo -e "${CYAN}▶ Phase 2: Configuring VNC Server${NC}"
echo ""

# Kill any existing VNC sessions on this display
if vncserver -kill ${DISPLAY_NUMBER} 2>/dev/null; then
    echo -e "${YELLOW}⚠ Killed existing VNC session on ${DISPLAY_NUMBER}${NC}"
fi

# Check if VNC password already exists
VNC_PASSWD_FILE="$HOME/.vnc/passwd"
if [ -f "$VNC_PASSWD_FILE" ]; then
    echo -e "${GREEN}✓ VNC password already configured${NC}"
    echo -e "${YELLOW}  To reset password, run: vncpasswd${NC}"
else
    echo -e "${YELLOW}⚠ First time VNC setup - you need to set a password${NC}"
    echo -e "${BLUE}  Please enter a VNC password when prompted:${NC}"
    echo ""
    vncpasswd
fi
echo ""

# Configure VNC startup script
VNC_XSTARTUP="$HOME/.vnc/xstartup"
echo -e "${BLUE}Configuring VNC startup script...${NC}"

cat > "$VNC_XSTARTUP" << 'EOF'
#!/bin/bash
xrdb $HOME/.Xresources
startxfce4 &
EOF

chmod +x "$VNC_XSTARTUP"
echo -e "${GREEN}✓ VNC startup script configured${NC}"
echo ""

# Phase 3: Start VNC Server
echo -e "${CYAN}▶ Phase 3: Starting VNC Server${NC}"
echo ""

echo -e "${BLUE}Starting VNC server on ${DISPLAY_NUMBER} (port ${VNC_PORT})...${NC}"
echo -e "${YELLOW}Resolution: ${RESOLUTION}${NC}"

if vncserver -geometry ${RESOLUTION} ${DISPLAY_NUMBER}; then
    echo -e "${GREEN}✓ VNC server started successfully${NC}"
else
    echo -e "${RED}✗ Failed to start VNC server${NC}"
    exit 1
fi
echo ""

# Get instance information for connection instructions
INSTANCE_NAME=$(hostname)
EXTERNAL_IP=$(curl -s -H "Metadata-Flavor: Google" http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip 2>/dev/null || echo "UNKNOWN")
ZONE=$(curl -s -H "Metadata-Flavor: Google" http://metadata.google.internal/computeMetadata/v1/instance/zone 2>/dev/null | cut -d/ -f4 || echo "UNKNOWN")

# Display connection instructions
echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║         VNC Server Setup Complete!        ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}Instance Information:${NC}"
echo -e "  Hostname:     ${INSTANCE_NAME}"
echo -e "  External IP:  ${EXTERNAL_IP}"
echo -e "  Zone:         ${ZONE}"
echo ""
echo -e "${CYAN}VNC Server Details:${NC}"
echo -e "  Display:      ${DISPLAY_NUMBER}"
echo -e "  Port:         ${VNC_PORT}"
echo -e "  Resolution:   ${RESOLUTION}"
echo ""
echo -e "${CYAN}Next Steps (on your LOCAL machine):${NC}"
echo ""
echo -e "${YELLOW}1. Create SSH tunnel:${NC}"
echo -e "   ${BLUE}ssh -L ${VNC_PORT}:localhost:${VNC_PORT} -C $(whoami)@${EXTERNAL_IP}${NC}"
echo ""
echo -e "   ${YELLOW}Or use gcloud SSH:${NC}"
echo -e "   ${BLUE}gcloud compute ssh ${INSTANCE_NAME} --zone=${ZONE} -- -L ${VNC_PORT}:localhost:${VNC_PORT} -C${NC}"
echo ""
echo -e "${YELLOW}2. Connect VNC Viewer to:${NC}"
echo -e "   ${BLUE}localhost:${VNC_PORT}${NC}"
echo ""
echo -e "${YELLOW}3. In VNC session, launch emulator:${NC}"
echo -e "   ${BLUE}cd ~/aosp${NC}"
echo -e "   ${BLUE}source build/envsetup.sh${NC}"
echo -e "   ${BLUE}lunch aosp_x86_64-ap2a-eng${NC}"
echo -e "   ${BLUE}emulator -gpu swiftshader_indirect &${NC}"
echo ""
echo -e "${CYAN}Helpful Commands:${NC}"
echo -e "  Stop VNC:     ${BLUE}vncserver -kill ${DISPLAY_NUMBER}${NC}"
echo -e "  Restart VNC:  ${BLUE}vncserver -geometry ${RESOLUTION} ${DISPLAY_NUMBER}${NC}"
echo -e "  View logs:    ${BLUE}cat ~/.vnc/${INSTANCE_NAME}${DISPLAY_NUMBER}.log${NC}"
echo -e "  List VNC:     ${BLUE}vncserver -list${NC}"
echo ""

# Create a helper script for restarting VNC
cat > ~/restart_vnc.sh << EOF
#!/bin/bash
vncserver -kill ${DISPLAY_NUMBER} 2>/dev/null
vncserver -geometry ${RESOLUTION} ${DISPLAY_NUMBER}
echo "VNC server restarted on ${DISPLAY_NUMBER} (port ${VNC_PORT})"
EOF

chmod +x ~/restart_vnc.sh

echo -e "${GREEN}✓ Helper script created: ~/restart_vnc.sh${NC}"
echo ""
