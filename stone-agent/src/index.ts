#!/usr/bin/env node
/**
 * Stone Agent Server - Main Entry Point
 *
 * LiveKit agents.js server that handles AI agent processing for StoneOS.
 * This server runs separately from the Android app and handles:
 * - Voice processing (STT/TTS)
 * - AI model integration
 * - Tool calling and execution
 * - Agent dispatch and routing
 *
 * Based on the React prototype pattern with agent dispatch.
 */

import { fileURLToPath } from 'node:url';
import { type JobContext, type JobProcess, WorkerOptions, cli } from '@livekit/agents';
import * as silero from '@livekit/agents-plugin-silero';
import { voice } from '@livekit/agents';
import { RouterAgent } from './agents/stone-router.js';
import dotenv from 'dotenv';

// Load environment variables
dotenv.config();

const logger = console;

/**
 * Prewarm function - loads heavy models before handling jobs
 * This improves first response time by loading VAD model upfront
 */
async function prewarm(proc: JobProcess): Promise<void> {
  logger.log('[Prewarm] Loading Silero VAD model...');

  // Store VAD in process userData for reuse
  proc.userData.vad = await silero.VAD.load();

  logger.log('[Prewarm] VAD model loaded successfully');
}

/**
 * Entry function - called when agent joins a room
 * This is the main entrypoint for agent dispatch
 */
async function entry(ctx: JobContext): Promise<void> {
  const roomName = ctx.room.name;
  logger.log(`[Entry] Agent joining room: ${roomName}`);

  // Connect to the room
  await ctx.connect();

  // Create voice agent session with inference gateway pattern
  // Using inference gateway (no API keys needed for basic setup)
  const session = new voice.AgentSession({
    // Speech-to-Text: Use AssemblyAI universal streaming model
    stt: 'assemblyai/universal-streaming:en',

    // Large Language Model: Use OpenAI GPT-4 mini via inference gateway
    llm: 'openai/gpt-4o-mini',

    // Text-to-Speech: Use Cartesia Sonic with specific voice
    tts: 'cartesia/sonic-2:9626c31c-bec5-4cca-baa8-f8ba9e84c8bc',

    // Voice Activity Detection: Use preloaded Silero VAD
    vad: ctx.proc.userData.vad as silero.VAD,
  });

  // Create router agent instance
  const agent = new RouterAgent();

  // Start the session
  await session.start({
    agent,
    room: ctx.room,
  });

  logger.log(`[Entry] Agent session started for room: ${roomName}`);
}

// Export the agent definition
export default {
  prewarm,
  entry,
};

// Run the agent worker if this is the main module
if (import.meta.url === `file://${process.argv[1]}`) {
  const port = parseInt(process.env.AGENT_PORT || '8081', 10);

  logger.log('='.repeat(60));
  logger.log('Stone Agent Server');
  logger.log('='.repeat(60));
  logger.log(`Port: ${port}`);
  logger.log(`LiveKit URL: ${process.env.LIVEKIT_URL || 'not set'}`);
  logger.log('='.repeat(60));

  cli.runApp(
    new WorkerOptions({
      agent: fileURLToPath(import.meta.url),
      port,
      // Do not include agent_name - this breaks agent dispatch (critical learning from prototype)
      // agentName: 'stone-router-agent', // WRONG - don't do this
    })
  );
}
