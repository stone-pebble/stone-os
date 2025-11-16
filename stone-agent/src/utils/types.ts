/**
 * Type definitions for Stone agent system
 */

export type StoneAgentType =
  | 'router'    // Main routing agent
  | 'tick'      // Time management
  | 'pebbles'   // Task management and app launcher
  | 'set'       // System settings
  | 'listen'    // Music control
  | 'ask'       // Knowledge and search
  | 'look'      // Digital library
  | 'plan'      // Calendar and goals
  | 'think'     // Note-taking
  | 'reflect'   // Activity logging
  | 'connect'   // Communications
  | 'go'        // Navigation
  | 'fund';     // Payments

export interface AgentInfo {
  name: StoneAgentType;
  description: string;
  available: boolean;
}

export interface DeviceState {
  type: 'device_state';
  currentApp?: string;
  batteryLevel?: number;
  networkStatus?: string;
  timestamp: number;
}

export interface ChatMessage {
  role: 'system' | 'user' | 'assistant';
  content: string;
  timestamp?: number;
}
