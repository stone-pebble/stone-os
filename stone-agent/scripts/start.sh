#!/bin/bash
# Start both agent and token servers

set -e

echo "==================================================================="
echo "Starting Stone Agent Server"
echo "==================================================================="

# Check if .env exists
if [ ! -f .env ]; then
    echo "ERROR: .env file not found!"
    echo "Please copy .env.example to .env and configure your LiveKit credentials."
    exit 1
fi

# Source environment variables
source .env

# Check required variables
if [ -z "$LIVEKIT_URL" ] || [ -z "$LIVEKIT_API_KEY" ] || [ -z "$LIVEKIT_API_SECRET" ]; then
    echo "ERROR: Missing required environment variables in .env:"
    echo "  LIVEKIT_URL: ${LIVEKIT_URL:-NOT SET}"
    echo "  LIVEKIT_API_KEY: ${LIVEKIT_API_KEY:-NOT SET}"
    echo "  LIVEKIT_API_SECRET: ${LIVEKIT_API_SECRET:-NOT SET}"
    exit 1
fi

# Build if needed
if [ ! -d "dist" ]; then
    echo "Building TypeScript..."
    npm run build
fi

echo ""
echo "Starting servers..."
echo ""

# Start token server in background
echo "Starting token server on port ${TOKEN_SERVER_PORT:-8000}..."
npm run token-server &
TOKEN_SERVER_PID=$!

# Wait a moment for token server to start
sleep 2

# Start agent server in foreground
echo "Starting agent server on port ${AGENT_PORT:-8081}..."
npm start

# Cleanup on exit
trap "kill $TOKEN_SERVER_PID 2>/dev/null" EXIT
