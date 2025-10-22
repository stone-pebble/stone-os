package com.stoneos.tick;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

/**
 * BroadcastReceiver for Agent API control of Tick app.
 *
 * Exposes two actions:
 * 1. SET_ALARM - Set an alarm with hour and minute
 * 2. SET_TIMER - Start a timer with duration in seconds
 */
public class TickControlReceiver extends BroadcastReceiver {
    private static final String TAG = "TickControlAPI";

    // Action constants
    private static final String ACTION_SET_ALARM = "com.stoneos.tick.SET_ALARM";
    private static final String ACTION_SET_TIMER = "com.stoneos.tick.SET_TIMER";

    // Extra constants
    private static final String EXTRA_HOUR = "hour";
    private static final String EXTRA_MINUTE = "minute";
    private static final String EXTRA_DURATION_SECONDS = "duration_seconds";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            Log.e(TAG, "Received null action");
            return;
        }

        Log.d(TAG, "Received action: " + action);

        switch (action) {
            case ACTION_SET_ALARM:
                handleSetAlarm(context, intent);
                break;
            case ACTION_SET_TIMER:
                handleSetTimer(context, intent);
                break;
            default:
                Log.e(TAG, "Unknown action: " + action);
                break;
        }
    }

    /**
     * Handle SET_ALARM action.
     * Expected extras: int "hour" (0-23), int "minute" (0-59)
     */
    private void handleSetAlarm(Context context, Intent intent) {
        if (!intent.hasExtra(EXTRA_HOUR) || !intent.hasExtra(EXTRA_MINUTE)) {
            Log.e(TAG, "SET_ALARM: missing 'hour' or 'minute' extra");
            Toast.makeText(context, "Error: missing hour or minute", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour = intent.getIntExtra(EXTRA_HOUR, -1);
        int minute = intent.getIntExtra(EXTRA_MINUTE, -1);

        if (hour < 0 || hour > 23) {
            Log.e(TAG, "SET_ALARM: invalid hour " + hour + " (must be 0-23)");
            Toast.makeText(context, "Error: invalid hour", Toast.LENGTH_SHORT).show();
            return;
        }

        if (minute < 0 || minute > 59) {
            Log.e(TAG, "SET_ALARM: invalid minute " + minute + " (must be 0-59)");
            Toast.makeText(context, "Error: invalid minute", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, String.format("Setting alarm for %02d:%02d", hour, minute));

        // Launch TickActivity and pass the alarm parameters
        Intent launchIntent = new Intent(context, TickActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launchIntent.setAction("SET_ALARM");
        launchIntent.putExtra(EXTRA_HOUR, hour);
        launchIntent.putExtra(EXTRA_MINUTE, minute);
        context.startActivity(launchIntent);

        Toast.makeText(context,
            String.format("Alarm set for %02d:%02d", hour, minute),
            Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle SET_TIMER action.
     * Expected extras: int "duration_seconds"
     */
    private void handleSetTimer(Context context, Intent intent) {
        if (!intent.hasExtra(EXTRA_DURATION_SECONDS)) {
            Log.e(TAG, "SET_TIMER: missing 'duration_seconds' extra");
            Toast.makeText(context, "Error: missing duration_seconds", Toast.LENGTH_SHORT).show();
            return;
        }

        int durationSeconds = intent.getIntExtra(EXTRA_DURATION_SECONDS, -1);

        if (durationSeconds <= 0) {
            Log.e(TAG, "SET_TIMER: invalid duration " + durationSeconds + " (must be > 0)");
            Toast.makeText(context, "Error: duration must be positive", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Setting timer for " + durationSeconds + " seconds");

        // Launch TickActivity and pass the timer parameters
        Intent launchIntent = new Intent(context, TickActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        launchIntent.setAction("SET_TIMER");
        launchIntent.putExtra(EXTRA_DURATION_SECONDS, durationSeconds);
        context.startActivity(launchIntent);

        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        String message = minutes > 0
            ? String.format("Timer set for %d:%02d", minutes, seconds)
            : String.format("Timer set for %d seconds", seconds);

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
