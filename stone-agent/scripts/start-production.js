#!/usr/bin/env node
/**
 * Production Startup Script
 *
 * Starts both the token server and agent worker in production mode.
 * Handles graceful shutdown and process monitoring.
 */

import { spawn } from 'child_process';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const rootDir = join(__dirname, '..');

console.log('='.repeat(80));
console.log('Stone Agent Server - Production Mode');
console.log('='.repeat(80));

// Validate required environment variables
const required = ['LIVEKIT_URL', 'LIVEKIT_API_KEY', 'LIVEKIT_API_SECRET'];
const missing = required.filter(key => !process.env[key]);

if (missing.length > 0) {
  console.error('\n❌ ERROR: Missing required environment variables:');
  missing.forEach(key => console.error(`   - ${key}`));
  console.error('\nPlease configure these in your deployment platform or .env file.\n');
  process.exit(1);
}

console.log('✓ Environment variables validated');
console.log(`✓ LiveKit URL: ${process.env.LIVEKIT_URL}`);
console.log(`✓ Token Server Port: ${process.env.TOKEN_SERVER_PORT || 8000}`);
console.log(`✓ Agent Port: ${process.env.AGENT_PORT || 8081}`);
console.log('='.repeat(80));

const processes = [];

/**
 * Start the token server (HTTP API)
 */
function startTokenServer() {
  console.log('\n[Token Server] Starting...');
  const tokenServer = spawn('node', [join(rootDir, 'dist/server/token-server.js')], {
    stdio: 'inherit',
    env: process.env,
  });

  tokenServer.on('error', (error) => {
    console.error('[Token Server] Failed to start:', error);
    process.exit(1);
  });

  tokenServer.on('exit', (code, signal) => {
    console.error(`[Token Server] Exited with code ${code} and signal ${signal}`);
    if (code !== 0) {
      console.error('[Token Server] Unexpected exit, shutting down...');
      cleanup();
    }
  });

  processes.push(tokenServer);
  return tokenServer;
}

/**
 * Start the agent worker (LiveKit agent)
 */
function startAgentWorker() {
  console.log('\n[Agent Worker] Starting...');
  const agentWorker = spawn('node', [join(rootDir, 'dist/index.js'), 'start'], {
    stdio: 'inherit',
    env: process.env,
  });

  agentWorker.on('error', (error) => {
    console.error('[Agent Worker] Failed to start:', error);
    process.exit(1);
  });

  agentWorker.on('exit', (code, signal) => {
    console.error(`[Agent Worker] Exited with code ${code} and signal ${signal}`);
    if (code !== 0) {
      console.error('[Agent Worker] Unexpected exit, shutting down...');
      cleanup();
    }
  });

  processes.push(agentWorker);
  return agentWorker;
}

/**
 * Clean up all processes
 */
function cleanup() {
  console.log('\n[Cleanup] Shutting down all processes...');
  processes.forEach((proc) => {
    if (proc && !proc.killed) {
      proc.kill('SIGTERM');
    }
  });

  // Force kill after 10 seconds
  setTimeout(() => {
    processes.forEach((proc) => {
      if (proc && !proc.killed) {
        console.log('[Cleanup] Force killing process...');
        proc.kill('SIGKILL');
      }
    });
    process.exit(0);
  }, 10000);
}

// Handle shutdown signals
process.on('SIGTERM', () => {
  console.log('\n[Signal] Received SIGTERM, shutting down gracefully...');
  cleanup();
});

process.on('SIGINT', () => {
  console.log('\n[Signal] Received SIGINT, shutting down gracefully...');
  cleanup();
});

// Handle uncaught errors
process.on('uncaughtException', (error) => {
  console.error('[Uncaught Exception]', error);
  cleanup();
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('[Unhandled Rejection]', reason);
  cleanup();
});

// Start both services
try {
  startTokenServer();
  // Wait a bit for token server to start before starting agent
  setTimeout(() => {
    startAgentWorker();
    console.log('\n' + '='.repeat(80));
    console.log('✓ All services started successfully');
    console.log('='.repeat(80));
  }, 2000);
} catch (error) {
  console.error('[Startup] Failed to start services:', error);
  cleanup();
  process.exit(1);
}
