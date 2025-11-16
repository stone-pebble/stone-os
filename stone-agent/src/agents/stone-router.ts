/**
 * Stone Router Agent - Main routing agent for StoneOS
 *
 * This agent routes user requests to specialized agents and handles
 * basic device control operations.
 *
 * Based on the React prototype pattern with LiveKit agent dispatch.
 */

import { voice, llm, type JobContext } from '@livekit/agents';
import { z } from 'zod';
import { deviceTools } from '../tools/device-tools.js';
import type { StoneAgentType } from '../utils/types.js';

/**
 * Router Agent that handles general requests and delegates to specialists
 */
export class RouterAgent extends voice.Agent {
  constructor() {
    super({
      instructions: `You are Stone, a minimalist AI assistant for Android. You help users interact with their phone naturally.

Your capabilities:
- Open apps and control basic phone functions
- Play music on Spotify
- Navigate to locations
- Send messages and make calls
- Set alarms and timers
- Control system settings (WiFi, Bluetooth, brightness, volume)

When users ask for specific tasks, use the available tools to help them. Be concise and helpful.
If the user's request is outside your capabilities, let them know politely.`,
      tools: {
        ...deviceTools,
        delegateToSpecialist: llm.tool({
          description: 'Delegate to a specialist agent for complex tasks in specific domains',
          parameters: z.object({
            agent: z.enum([
              'tick',
              'pebbles',
              'set',
              'listen',
              'ask',
              'look',
              'plan',
              'think',
              'reflect',
              'connect',
              'go',
              'fund',
            ] as const).describe('The specialist agent to delegate to'),
            query: z.string().describe('The user\'s query or request'),
          }),
          execute: async ({ agent, query }, { ctx }) => {
            // In a full implementation, this would handoff to another agent instance
            // For now, we'll handle the delegation acknowledgment
            console.log(`[Router] Delegating to ${agent} agent: ${query}`);

            return llm.handoff({
              agent: await createSpecialistAgent(agent, query),
              returns: `Connecting you to the ${agent} specialist...`,
            });
          },
        }),
      },
    });
  }

  /**
   * Called when the user enters the room
   * DON'T auto-greet - wait for user to speak first (matches prototype pattern)
   */
  async onEnter() {
    // Wait for user to speak first (no auto-greeting)
    console.log('[RouterAgent] User entered, waiting for input...');
  }
}

/**
 * Create a specialist agent instance based on type
 */
async function createSpecialistAgent(
  agentType: StoneAgentType,
  query: string
): Promise<voice.Agent> {
  // Import and create the appropriate specialist agent
  // For now, return a simple agent that returns to router
  const specialistInstructions = getSpecialistInstructions(agentType);

  return new voice.Agent({
    instructions: `${specialistInstructions}\n\nUser query: ${query}`,
    tools: {
      ...deviceTools,
      returnToRouter: llm.tool({
        description: 'Return to the main router agent when task is complete',
        parameters: z.object({
          summary: z.string().describe('Brief summary of what was accomplished'),
        }),
        execute: async ({ summary }, { ctx }) => {
          console.log(`[${agentType}] Returning to router: ${summary}`);

          return llm.handoff({
            agent: new RouterAgent(),
            returns: summary,
          });
        },
      }),
    },
  });
}

/**
 * Get specialist instructions based on agent type
 */
function getSpecialistInstructions(agentType: StoneAgentType): string {
  const instructions: Record<StoneAgentType, string> = {
    router: 'You are Stone, the main assistant.',
    tick: 'You are the Time specialist. Help with alarms, timers, stopwatch, and time-related queries.',
    pebbles: 'You are the Task specialist. Help with app launching and task management.',
    set: 'You are the Settings specialist. Help with WiFi, Bluetooth, brightness, volume, and system settings.',
    listen: 'You are the Music specialist. Help with Spotify playback and music discovery.',
    ask: 'You are the Knowledge specialist. Help with search and information lookup via Perplexity.',
    look: 'You are the Library specialist. Help with finding and reading books from Project Gutenberg.',
    plan: 'You are the Calendar specialist. Help with scheduling, events, and goal tracking.',
    think: 'You are the Notes specialist. Help with note-taking and organizing thoughts.',
    reflect: 'You are the Reflection specialist. Help with daily summaries and activity insights.',
    connect: 'You are the Communications specialist. Help with calls, messages, and email.',
    go: 'You are the Navigation specialist. Help with directions and location services.',
    fund: 'You are the Payments specialist. Help with wallet and banking (view only, no transactions).',
  };

  return instructions[agentType] || instructions.router;
}
