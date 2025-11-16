/**
 * Operation status helpers for broadcasting agent busy/idle states
 * Matches the React prototype pattern from sendBusySignal
 */

import type { Room } from '@livekit/rtc-node';

export interface OperationStatus {
  type: 'operation_status';
  agent: string;
  busy: boolean;
  operation: string;
  details?: string;
  toolName?: string;
  stepDescription?: string;
  operationId?: string;
  progressPercentage?: number;
  timestamp: number;
}

/**
 * Send operation status message to Android client via data channel.
 * This matches the React prototype's sendBusySignal pattern.
 *
 * @param room The LiveKit room
 * @param agent Agent name (e.g., "stone", "think", "go")
 * @param operation Operation description (e.g., "processing", "searching")
 * @param busy Whether the agent is currently busy
 * @param options Additional status details
 */
export async function sendOperationStatus(
  room: Room,
  agent: string,
  operation: string,
  busy: boolean,
  options: {
    details?: string;
    toolName?: string;
    stepDescription?: string;
    operationId?: string;
    progressPercentage?: number;
  } = {}
): Promise<void> {
  try {

    const statusData: OperationStatus = {
      type: 'operation_status',
      agent,
      busy,
      operation,
      timestamp: Date.now(),
      ...options,
    };

    const payload = Buffer.from(JSON.stringify(statusData), 'utf-8');

    await room.localParticipant?.publishData(payload, {
      reliable: true,
      topic: 'agent_operation_status',
    });

    console.log(`[Status] ${agent} - ${operation} (busy: ${busy})`);
  } catch (error) {
    console.error(`[Status] Failed to send operation status:`, error);
  }
}

/**
 * Send device command to Android client via data channel.
 * Commands are routed through the Android app's data channel handlers.
 *
 * @param room The LiveKit room
 * @param action Action type (e.g., "open_app", "play_music", "navigate")
 * @param params Action parameters
 */
export async function sendDeviceCommand(
  room: Room,
  action: string,
  params: Record<string, any>
): Promise<void> {
  try {

    const commandData = {
      type: 'device_control',
      action,
      params,
      timestamp: Date.now(),
    };

    const payload = Buffer.from(JSON.stringify(commandData), 'utf-8');

    await room.localParticipant?.publishData(payload, {
      reliable: true,
      topic: 'device_command',
    });

    console.log(`[Device] Command: ${action}`, params);
  } catch (error) {
    console.error(`[Device] Failed to send command:`, error);
  }
}
