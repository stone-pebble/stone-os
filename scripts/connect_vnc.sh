#!/bin/bash
#
# connect_vnc.sh
#
# Creates SSH tunnel to GCP instance for VNC connection.
# This script should be run on your LOCAL machine (not on GCP).
#
# Part of Ticket #12: Establish Secure, Interactive Emulator Environment on GCP
#
# Usage: ./connect_vnc.sh [--instance NAME] [--zone ZONE] [--port PORT]
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
PROJECT="dev-stone"
ZONE="us-central1-a"
INSTANCE_NAME=""
VNC_PORT="5901"
LOCAL_PORT="5901"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --instance)
            INSTANCE_NAME="$2"
            shift 2
            ;;
        --zone)
            ZONE="$2"
            shift 2
            ;;
        --port)
            VNC_PORT="$2"
            LOCAL_PORT="$2"
            shift 2
            ;;
        --help|-h)
            cat << EOF
VNC Connection Helper for StoneOS Emulator Testing

Usage: $0 [OPTIONS]

OPTIONS:
    --instance NAME    GCP instance name (auto-detected if not specified)
    --zone ZONE        GCP zone (default: us-central1-a)
    --port PORT        VNC port number (default: 5901)
    --help, -h         Show this help message

EXAMPLES:
    $0                                      # Auto-detect instance
    $0 --instance stoneos-builder-123456    # Specify instance
    $0 --port 5902                          # Use different port

WORKFLOW:
    1. Run this script to create SSH tunnel
    2. Keep this terminal open
    3. Open VNC Viewer and connect to: localhost:${LOCAL_PORT}
    4. Enter the VNC password you set on the GCP instance

NOTES:
    - Requires gcloud CLI configured with project: ${PROJECT}
    - VNC server must be running on GCP instance first
    - Run scripts/setup_vnc_server.sh on GCP before using this
    - Press Ctrl+C to close the tunnel when done

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
echo -e "${BLUE}║  StoneOS VNC Connection Helper (Local)    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}"
echo ""

# Check if gcloud is installed
if ! command -v gcloud &> /dev/null; then
    echo -e "${RED}✗ gcloud CLI not found!${NC}"
    echo ""
    echo "Please install gcloud CLI:"
    echo "  https://cloud.google.com/sdk/docs/install"
    exit 1
fi

# Set project
CURRENT_PROJECT=$(gcloud config get-value project 2>/dev/null)
if [ "$CURRENT_PROJECT" != "$PROJECT" ]; then
    echo -e "${YELLOW}⚠ Setting project to $PROJECT${NC}"
    gcloud config set project $PROJECT
fi

# Auto-detect instance if not specified
if [ -z "$INSTANCE_NAME" ]; then
    echo -e "${BLUE}Auto-detecting GCP instance...${NC}"

    # Look for running stoneos-builder instances
    INSTANCE_NAME=$(gcloud compute instances list \
        --filter="name:stoneos-builder* AND status:RUNNING" \
        --format="value(name)" \
        --limit=1 2>/dev/null || echo "")

    if [ -z "$INSTANCE_NAME" ]; then
        echo -e "${RED}✗ No running stoneos-builder instance found!${NC}"
        echo ""
        echo "Available instances:"
        gcloud compute instances list --format="table(name,zone,status)"
        echo ""
        echo "Please specify instance name with --instance flag"
        exit 1
    fi

    echo -e "${GREEN}✓ Found instance: ${INSTANCE_NAME}${NC}"
fi

# Get instance details
echo -e "${BLUE}Fetching instance details...${NC}"

INSTANCE_ZONE=$(gcloud compute instances list \
    --filter="name:${INSTANCE_NAME}" \
    --format="value(zone)" 2>/dev/null | head -1)

if [ -z "$INSTANCE_ZONE" ]; then
    echo -e "${RED}✗ Instance not found: ${INSTANCE_NAME}${NC}"
    exit 1
fi

# Use the detected zone if not specified
if [ "$ZONE" = "us-central1-a" ]; then
    ZONE="$INSTANCE_ZONE"
fi

INSTANCE_STATUS=$(gcloud compute instances describe ${INSTANCE_NAME} \
    --zone=${ZONE} \
    --format="value(status)" 2>/dev/null || echo "UNKNOWN")

if [ "$INSTANCE_STATUS" != "RUNNING" ]; then
    echo -e "${RED}✗ Instance is not running (status: ${INSTANCE_STATUS})${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Instance is running${NC}"
echo ""

# Display connection info
echo -e "${CYAN}Connection Details:${NC}"
echo -e "  Instance:     ${INSTANCE_NAME}"
echo -e "  Zone:         ${ZONE}"
echo -e "  Remote Port:  ${VNC_PORT}"
echo -e "  Local Port:   ${LOCAL_PORT}"
echo ""

# Create SSH tunnel
echo -e "${YELLOW}Creating SSH tunnel...${NC}"
echo -e "${BLUE}Press Ctrl+C to close the tunnel when done${NC}"
echo ""

# Check if port is already in use
if lsof -Pi :${LOCAL_PORT} -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠ Port ${LOCAL_PORT} is already in use!${NC}"
    echo ""
    echo "Close the existing connection or use a different port with --port"
    echo ""
    echo "To find what's using the port:"
    echo "  lsof -i :${LOCAL_PORT}"
    exit 1
fi

# Create the tunnel
echo -e "${GREEN}✓ Tunnel established!${NC}"
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║         Ready to Connect via VNC          ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${YELLOW}1. Open your VNC Viewer (e.g., RealVNC, TigerVNC)${NC}"
echo ""
echo -e "${YELLOW}2. Connect to:${NC}"
echo -e "   ${GREEN}localhost:${LOCAL_PORT}${NC}"
echo -e "   ${BLUE}(or localhost::${LOCAL_PORT} depending on your VNC client)${NC}"
echo ""
echo -e "${YELLOW}3. Enter the VNC password you set on the GCP instance${NC}"
echo ""
echo -e "${YELLOW}4. Once connected, open a terminal in VNC and run:${NC}"
echo -e "   ${BLUE}cd ~/aosp${NC}"
echo -e "   ${BLUE}source build/envsetup.sh${NC}"
echo -e "   ${BLUE}lunch aosp_x86_64-ap2a-eng${NC}"
echo -e "   ${BLUE}emulator -gpu swiftshader_indirect &${NC}"
echo ""
echo -e "${CYAN}Troubleshooting:${NC}"
echo -e "  - VNC Viewer shows 'Connection refused': VNC server not running on GCP"
echo -e "  - Password rejected: Reset with 'vncpasswd' on GCP instance"
echo -e "  - Black screen: Wait 30 seconds, or restart VNC on GCP"
echo ""
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}Tunnel is active - keep this window open...${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo ""

# Set up cleanup trap
cleanup() {
    echo ""
    echo -e "${YELLOW}Closing SSH tunnel...${NC}"
    echo -e "${GREEN}✓ Disconnected${NC}"
    exit 0
}

trap cleanup INT TERM

# Create the SSH tunnel with compression
gcloud compute ssh ${INSTANCE_NAME} \
    --zone=${ZONE} \
    --ssh-flag="-L ${LOCAL_PORT}:localhost:${VNC_PORT}" \
    --ssh-flag="-C" \
    --ssh-flag="-N" \
    -- 2>&1

# If we get here, the tunnel closed
cleanup
