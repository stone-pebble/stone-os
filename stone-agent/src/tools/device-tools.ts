/**
 * Device control tools for Android integration
 * These tools send commands to the Android app via data channels
 */

import { llm } from '@livekit/agents';
import type { Room } from '@livekit/rtc-node';
import { z } from 'zod';
import { sendDeviceCommand, sendOperationStatus } from '../utils/status.js';

// Helper to get room from context
function getRoomFromContext(ctx: any): Room {
  // RunContext has session, which has roomIO internally
  // We need to access the room through the session
  const session = ctx.session;
  if (!session || !(session as any)._roomIO || !(session as any)._roomIO.room) {
    throw new Error('Room not accessible from context');
  }
  return (session as any)._roomIO.room as Room;
}

/**
 * Open an Android app by name
 */
export const openApp = llm.tool({
  description: 'Open an Android app by its name or package identifier',
  parameters: z.object({
    appName: z.string().describe('The name of the app to open (e.g., "Spotify", "Maps", "Calendar")'),
  }),
  execute: async ({ appName }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'router', 'opening_app', true, {
      details: `Opening ${appName}`,
    });

    await sendDeviceCommand(room, 'open_app', { app: appName });

    await sendOperationStatus(room, 'router', 'opening_app', false, {
      details: `Opened ${appName}`,
    });

    return { success: true, message: `Opened ${appName}` };
  },
});

/**
 * Play music on Spotify
 */
export const playMusic = llm.tool({
  description: 'Play music on Spotify by song name, artist, album, or playlist',
  parameters: z.object({
    query: z.string().describe('What to play - song name, artist, playlist, etc.'),
  }),
  execute: async ({ query }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'listen', 'playing_music', true, {
      details: `Playing: ${query}`,
    });

    await sendDeviceCommand(room, 'play_music', { query });

    await sendOperationStatus(room, 'listen', 'playing_music', false);

    return { success: true, message: `Playing ${query}` };
  },
});

/**
 * Navigate to a location using Google Maps
 */
export const navigate = llm.tool({
  description: 'Navigate to a location using Google Maps',
  parameters: z.object({
    destination: z.string().describe('The destination address or place name'),
  }),
  execute: async ({ destination }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'go', 'navigating', true, {
      details: `Navigating to ${destination}`,
    });

    await sendDeviceCommand(room, 'navigate', { destination });

    await sendOperationStatus(room, 'go', 'navigating', false);

    return { success: true, message: `Navigating to ${destination}` };
  },
});

/**
 * Send a text message
 */
export const sendMessage = llm.tool({
  description: 'Send a text message to a contact',
  parameters: z.object({
    recipient: z.string().describe('The contact name or phone number'),
    message: z.string().describe('The message text to send'),
  }),
  execute: async ({ recipient, message }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'connect', 'sending_message', true, {
      details: `Sending message to ${recipient}`,
    });

    await sendDeviceCommand(room, 'send_message', { recipient, message });

    await sendOperationStatus(room, 'connect', 'sending_message', false);

    return { success: true, message: `Message sent to ${recipient}` };
  },
});

/**
 * Make a phone call
 */
export const makeCall = llm.tool({
  description: 'Make a phone call to a contact',
  parameters: z.object({
    contact: z.string().describe('The contact name or phone number to call'),
  }),
  execute: async ({ contact }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'connect', 'calling', true, {
      details: `Calling ${contact}`,
    });

    await sendDeviceCommand(room, 'make_call', { contact });

    await sendOperationStatus(room, 'connect', 'calling', false);

    return { success: true, message: `Calling ${contact}` };
  },
});

/**
 * Set an alarm
 */
export const setAlarm = llm.tool({
  description: 'Set an alarm for a specific time',
  parameters: z.object({
    time: z.string().describe('The time for the alarm (e.g., "7:00 AM", "18:30")'),
    label: z.string().optional().describe('Optional label for the alarm'),
  }),
  execute: async ({ time, label }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'tick', 'setting_alarm', true, {
      details: `Setting alarm for ${time}`,
    });

    await sendDeviceCommand(room, 'set_alarm', { time, label });

    await sendOperationStatus(room, 'tick', 'setting_alarm', false);

    return { success: true, message: `Alarm set for ${time}${label ? ` (${label})` : ''}` };
  },
});

/**
 * Set a timer
 */
export const setTimer = llm.tool({
  description: 'Set a countdown timer',
  parameters: z.object({
    duration: z.string().describe('Duration for the timer (e.g., "5 minutes", "1 hour", "30 seconds")'),
    label: z.string().optional().describe('Optional label for the timer'),
  }),
  execute: async ({ duration, label }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'tick', 'setting_timer', true, {
      details: `Setting timer for ${duration}`,
    });

    await sendDeviceCommand(room, 'set_timer', { duration, label });

    await sendOperationStatus(room, 'tick', 'setting_timer', false);

    return { success: true, message: `Timer set for ${duration}${label ? ` (${label})` : ''}` };
  },
});

/**
 * Control system settings (WiFi, Bluetooth, brightness, volume)
 */
export const controlSettings = llm.tool({
  description: 'Control system settings like WiFi, Bluetooth, brightness, or volume',
  parameters: z.object({
    setting: z.enum(['wifi', 'bluetooth', 'brightness', 'volume']).describe('The setting to control'),
    action: z.enum(['enable', 'disable', 'toggle', 'set']).describe('The action to perform'),
    value: z.number().optional().describe('Value for "set" action (brightness: 0-100, volume: 0-100)'),
  }),
  execute: async ({ setting, action, value }, { ctx }) => {
    const room = getRoomFromContext(ctx);
    await sendOperationStatus(room, 'set', 'adjusting_settings', true, {
      details: `Adjusting ${setting}`,
    });

    await sendDeviceCommand(room, 'control_settings', { setting, action, value });

    await sendOperationStatus(room, 'set', 'adjusting_settings', false);

    return { success: true, message: `${setting} ${action} successful` };
  },
});

/**
 * Export all device tools as a collection
 */
export const deviceTools = {
  openApp,
  playMusic,
  navigate,
  sendMessage,
  makeCall,
  setAlarm,
  setTimer,
  controlSettings,
};
