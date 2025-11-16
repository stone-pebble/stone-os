#!/usr/bin/env node
/**
 * Token Server - Generates LiveKit tokens and dispatches agents
 *
 * This server provides the connection details endpoint that the Android app
 * calls to get room credentials and dispatch agents.
 *
 * Endpoint: GET /api/connection-details?roomName=xxx&participantId=yyy
 */

import express, { type Request, type Response } from 'express';
import cors from 'cors';
import dotenv from 'dotenv';
import { AccessToken, RoomServiceClient } from 'livekit-server-sdk';

// Load environment variables
dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

// Configuration from environment
const LIVEKIT_URL = process.env.LIVEKIT_URL!;
const LIVEKIT_API_KEY = process.env.LIVEKIT_API_KEY!;
const LIVEKIT_API_SECRET = process.env.LIVEKIT_API_SECRET!;
const TOKEN_SERVER_PORT = parseInt(process.env.TOKEN_SERVER_PORT || '8000', 10);

// Validate required configuration
if (!LIVEKIT_URL || !LIVEKIT_API_KEY || !LIVEKIT_API_SECRET) {
  console.error('ERROR: Missing required environment variables:');
  console.error('  LIVEKIT_URL:', LIVEKIT_URL ? 'set' : 'MISSING');
  console.error('  LIVEKIT_API_KEY:', LIVEKIT_API_KEY ? 'set' : 'MISSING');
  console.error('  LIVEKIT_API_SECRET:', LIVEKIT_API_SECRET ? 'set' : 'MISSING');
  console.error('\nPlease create a .env file with these values.');
  process.exit(1);
}

// Initialize LiveKit Room Service Client for agent dispatch
const roomService = new RoomServiceClient(LIVEKIT_URL, LIVEKIT_API_KEY, LIVEKIT_API_SECRET);

/**
 * Generate connection details for Android client
 *
 * This endpoint:
 * 1. Creates a room access token for the client
 * 2. Dispatches an agent to join the room
 * 3. Returns connection details to the client
 */
app.get('/api/connection-details', async (req: Request, res: Response) => {
  try {
    // Get room name and participant ID from query params
    const roomName = (req.query.roomName as string) || `stone-${Date.now()}`;
    const participantId = (req.query.participantId as string) || `user-${Math.random().toString(36).substring(7)}`;

    console.log(`[Token] Generating connection details for room: ${roomName}, participant: ${participantId}`);

    // Create access token for the client
    const token = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET, {
      identity: participantId,
      // Token expires in 24 hours
      ttl: 86400,
    });

    // Add room permissions
    token.addGrant({
      room: roomName,
      roomJoin: true,
      canPublish: true,
      canSubscribe: true,
      canPublishData: true,
    });

    // Generate JWT token
    const jwt = await token.toJwt();

    // Dispatch agent to the room
    // Note: Agent dispatch handled automatically by LiveKit when room is created
    // The agent worker will connect based on its configuration
    // In production, you may want to explicitly create the room first:
    try {
      await roomService.createRoom({
        name: roomName,
        emptyTimeout: 300, // Room deleted after 5 minutes of being empty
        metadata: JSON.stringify({
          agent_type: 'router',
          session_start: new Date().toISOString(),
        }),
      });
    } catch (error) {
      // Room might already exist, that's fine
      console.log(`[Token] Room ${roomName} may already exist:`, error instanceof Error ? error.message : 'Unknown error');
    }

    console.log(`[Token] Agent dispatched to room: ${roomName}`);

    // Return connection details to client
    res.json({
      serverUrl: LIVEKIT_URL,
      roomName,
      participantName: participantId,
      participantToken: jwt,
    });
  } catch (error) {
    console.error('[Token] Error generating connection details:', error);
    res.status(500).json({
      error: 'Failed to generate connection details',
      message: error instanceof Error ? error.message : 'Unknown error',
    });
  }
});

/**
 * Health check endpoint
 */
app.get('/health', (req: Request, res: Response) => {
  res.json({
    status: 'healthy',
    timestamp: new Date().toISOString(),
    livekit_url: LIVEKIT_URL,
  });
});

/**
 * Get available agents endpoint
 * Matches the RPC method from Android
 */
app.get('/api/agents', (req: Request, res: Response) => {
  const agents = [
    { name: 'router', description: 'Main routing agent', available: true },
    { name: 'tick', description: 'Time management - alarms, timers, stopwatch', available: true },
    { name: 'pebbles', description: 'Task management and app launcher', available: true },
    { name: 'set', description: 'System settings - WiFi, Bluetooth, brightness', available: true },
    { name: 'listen', description: 'Music control - Spotify integration', available: true },
    { name: 'ask', description: 'Knowledge and search - Perplexity integration', available: true },
    { name: 'look', description: 'Digital library - Project Gutenberg books', available: true },
    { name: 'plan', description: 'Calendar and goal tracking', available: true },
    { name: 'think', description: 'Note-taking and file management', available: true },
    { name: 'reflect', description: 'Activity logging and daily summaries', available: true },
    { name: 'connect', description: 'Communications - calls, SMS, email', available: true },
    { name: 'go', description: 'Navigation and location services', available: true },
    { name: 'fund', description: 'Payments and banking', available: true },
  ];

  res.json({
    success: true,
    agents,
  });
});

// Start server
app.listen(TOKEN_SERVER_PORT, () => {
  console.log('='.repeat(60));
  console.log('Stone Token Server');
  console.log('='.repeat(60));
  console.log(`Port: ${TOKEN_SERVER_PORT}`);
  console.log(`LiveKit URL: ${LIVEKIT_URL}`);
  console.log('='.repeat(60));
  console.log(`\nEndpoints:`);
  console.log(`  GET  /api/connection-details?roomName=xxx&participantId=yyy`);
  console.log(`  GET  /api/agents`);
  console.log(`  GET  /health`);
  console.log('='.repeat(60));
});
