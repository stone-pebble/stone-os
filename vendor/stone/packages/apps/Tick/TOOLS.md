# Tick Agent Tool Definitions

This document defines the programmatic API for controlling Tick (the StoneOS time management app) through broadcast intents. AI agents can use these tools to set alarms and timers without user interaction.

## Overview

Tick exposes its functionality through Android BroadcastIntents. Each tool corresponds to a broadcast action that triggers the `TickControlReceiver`.

**Base Package**: `com.stoneos.tick`

---

## set_alarm

**Description**: Sets an alarm for a specific time.

**Parameters**:
- `hour` (integer, required): Hour in 24-hour format (0-23)
- `minute` (integer, required): Minute (0-59)

**Broadcast Action**: `com.stoneos.tick.SET_ALARM`

**Example Usage**:
```bash
# Set alarm for 7:30 AM
adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 7 --ei minute 30

# Set alarm for 9:00 PM (21:00)
adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 21 --ei minute 0

# Set alarm for 6:45 AM
adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 6 --ei minute 45
```

**Permissions Required**:
- `android.permission.SCHEDULE_EXACT_ALARM`
- `android.permission.USE_EXACT_ALARM`

**Success Indicators**:
- Log message: "Alarm added: HH:MM"
- Toast notification: "Alarm set for HH:MM"
- Alarm appears in Tick app's alarm list

**Behavior**:
- Creates a new alarm entry in the app
- If the time has already passed today, schedules for tomorrow
- Alarm will trigger with vibration and sound
- Default repeat pattern: "daily"

---

## set_timer

**Description**: Starts a countdown timer for the specified duration.

**Parameters**:
- `duration_seconds` (integer, required): Timer duration in seconds (must be > 0)

**Broadcast Action**: `com.stoneos.tick.SET_TIMER`

**Example Usage**:
```bash
# Set timer for 5 minutes (300 seconds)
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 300

# Set timer for 1 minute (60 seconds)
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 60

# Set timer for 1 hour (3600 seconds)
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 3600

# Set timer for 10 seconds
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 10

# Set timer for 30 minutes (1800 seconds)
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 1800
```

**Permissions Required**:
- None (timer functionality doesn't require special permissions)

**Success Indicators**:
- Log message: "Timer started: {duration_seconds} seconds"
- Toast notification: "Timer set for M:SS" or "Timer set for N seconds"
- Timer display updates in Tick app
- Timer automatically starts countdown

**Behavior**:
- Immediately starts the countdown timer
- Opens Tick app if not already open
- Updates timer display showing HH:MM:SS format
- When timer reaches 0, it stops automatically
- No alarm/notification when timer completes (future enhancement)

---

## Error Handling

All tools log errors to Android logcat with the tag `TickControlAPI`:

```bash
# Monitor API logs
adb logcat -s TickControlAPI:*
```

**Common error messages**:

**set_alarm:**
- `"SET_ALARM: missing 'hour' or 'minute' extra"` - Required parameter not provided
- `"SET_ALARM: invalid hour X (must be 0-23)"` - Hour value out of range
- `"SET_ALARM: invalid minute X (must be 0-59)"` - Minute value out of range

**set_timer:**
- `"SET_TIMER: missing 'duration_seconds' extra"` - Duration parameter not provided
- `"SET_TIMER: invalid duration X (must be > 0)"` - Duration must be positive

---

## Integration with AI Agents

### LiveKit Agent Example

```typescript
// Example: Implementing set_alarm in a LiveKit agent
async function setAlarm(hour: number, minute: number): Promise<void> {
  const command = `am broadcast -a com.stoneos.tick.SET_ALARM --ei hour ${hour} --ei minute ${minute}`;

  // Execute via adb or Android shell
  await executeShellCommand(command);

  // Confirmation
  console.log(`Alarm set for ${hour}:${minute.toString().padStart(2, '0')}`);
}

// Example: Implementing set_timer in a LiveKit agent
async function setTimer(durationSeconds: number): Promise<void> {
  const command = `am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds ${durationSeconds}`;

  // Execute via adb or Android shell
  await executeShellCommand(command);

  // Confirmation
  const minutes = Math.floor(durationSeconds / 60);
  const seconds = durationSeconds % 60;
  console.log(`Timer set for ${minutes}:${seconds.toString().padStart(2, '0')}`);
}

// Usage examples
await setAlarm(7, 30);    // Set alarm for 7:30 AM
await setTimer(300);       // Set 5-minute timer
```

### MCP Server Example

```javascript
// Example: Exposing as MCP tools
const tools = [
  {
    name: "set_alarm",
    description: "Sets an alarm for a specific time",
    parameters: {
      type: "object",
      properties: {
        hour: {
          type: "integer",
          description: "Hour in 24-hour format (0-23)",
          minimum: 0,
          maximum: 23
        },
        minute: {
          type: "integer",
          description: "Minute (0-59)",
          minimum: 0,
          maximum: 59
        }
      },
      required: ["hour", "minute"]
    },
    execute: async ({ hour, minute }) => {
      const result = await execAsync(
        `adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour ${hour} --ei minute ${minute}`
      );
      return {
        success: true,
        message: `Alarm set for ${hour}:${minute.toString().padStart(2, '0')}`
      };
    }
  },
  {
    name: "set_timer",
    description: "Starts a countdown timer",
    parameters: {
      type: "object",
      properties: {
        duration_seconds: {
          type: "integer",
          description: "Timer duration in seconds",
          minimum: 1
        }
      },
      required: ["duration_seconds"]
    },
    execute: async ({ duration_seconds }) => {
      const result = await execAsync(
        `adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds ${duration_seconds}`
      );
      const minutes = Math.floor(duration_seconds / 60);
      const seconds = duration_seconds % 60;
      return {
        success: true,
        message: `Timer set for ${minutes}:${seconds.toString().padStart(2, '0')}`
      };
    }
  }
];
```

---

## Testing

### Manual Testing via ADB

1. **Test alarm setting**:
   ```bash
   adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 14 --ei minute 30
   adb logcat -s TickControlAPI:* TickActivity:*
   ```

2. **Test timer setting**:
   ```bash
   adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 120
   adb logcat -s TickControlAPI:* TickActivity:*
   ```

3. **Verify alarm was created**:
   - Open Tick app
   - Check alarms list for new entry

4. **Verify timer started**:
   - Open Tick app
   - Check timer is counting down

### Automated Testing Script

```bash
#!/bin/bash
# test_tick_api.sh

echo "Testing Tick API..."

# Test set_alarm
echo "1. Testing set_alarm for 14:30"
adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 14 --ei minute 30
sleep 2

echo "2. Testing set_alarm for 07:00"
adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 7 --ei minute 0
sleep 2

# Test set_timer
echo "3. Testing set_timer for 5 minutes"
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 300
sleep 2

echo "4. Testing set_timer for 1 minute"
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds 60
sleep 2

# Test error handling
echo "5. Testing invalid alarm hour (25)"
adb shell am broadcast -a com.stoneos.tick.SET_ALARM --ei hour 25 --ei minute 0
sleep 1

echo "6. Testing invalid timer duration (-10)"
adb shell am broadcast -a com.stoneos.tick.SET_TIMER --ei duration_seconds -10
sleep 1

echo "Done! Check logcat for results:"
echo "adb logcat -s TickControlAPI:*"
```

---

## Natural Language Examples

Here are examples of how users might ask an AI agent to use these tools:

**set_alarm:**
- "Set an alarm for 7:30 AM tomorrow"
- "Wake me up at 6:45"
- "Create an alarm for 9:00 PM"
- "Set my alarm for half past seven"

**set_timer:**
- "Set a timer for 5 minutes"
- "Start a 30-second timer"
- "Give me a 10-minute timer"
- "Set a timer for 1 hour"
- "Start a 2-minute countdown"

---

## Future Enhancements

Additional tools that could be implemented:

- `delete_alarm` - Remove a specific alarm by ID or time
- `enable_alarm` / `disable_alarm` - Toggle alarm without deleting
- `list_alarms` - Query current alarms (returns JSON)
- `cancel_timer` - Stop a running timer
- `pause_timer` / `resume_timer` - Pause/resume timer functionality
- `start_stopwatch` / `stop_stopwatch` - Control stopwatch
- `get_timer_status` - Query remaining time on active timer

---

## App Features

Tick provides four main features:

### 1. World Clock
- Displays current time in 3 timezones:
  - San Francisco (America/Los_Angeles)
  - New York (America/New_York)
  - Tokyo (Asia/Tokyo)
- Updates every second
- Large serif font display

### 2. Alarms
- Create alarms with title, time, and repeat pattern
- Agent API can add new alarms programmatically
- AlarmManager integration for reliable triggering
- Vibration and sound on alarm trigger

### 3. Stopwatch
- Start/stop functionality
- Lap recording
- Centisecond precision (00:00.00 format)
- Manual controls only (no agent API yet)

### 4. Timer
- Hour, minute, second input
- Countdown with 1-second precision
- Agent API can start timers programmatically
- Auto-stops at zero

---

## Architecture Notes

**Why BroadcastReceiver?**

Using BroadcastIntents as the API mechanism provides:
1. **Simplicity**: No need for AIDL or complex IPC
2. **Security**: Android's permission system controls access
3. **Debuggability**: Easy to test via `adb` command line
4. **Language-agnostic**: Any process can send broadcasts
5. **Asynchronous**: Non-blocking for both sender and receiver

**System App Requirement**:

Tick must be a privileged system app because:
- Setting exact alarms requires `SCHEDULE_EXACT_ALARM` permission
- AlarmManager requires system-level access for reliable triggering
- BroadcastReceiver must be exported but protected by permissions

---

## License

Part of StoneOS - a minimalist, AI-augmented Android ROM.
