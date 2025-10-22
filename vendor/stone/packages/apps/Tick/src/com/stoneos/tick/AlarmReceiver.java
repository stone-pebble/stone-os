package com.stoneos.tick;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

/**
 * BroadcastReceiver for handling alarm triggers.
 * This is called by AlarmManager when an alarm time is reached.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        if (title == null) {
            title = "Alarm";
        }

        Log.d(TAG, "Alarm triggered: " + title);

        // Show notification
        Toast.makeText(context, "Alarm: " + title, Toast.LENGTH_LONG).show();

        // Vibrate
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 500, 200, 500, 200, 500};
            vibrator.vibrate(pattern, -1);
        }

        // Play default alarm sound
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Ringtone ringtone = RingtoneManager.getRingtone(context, alarmUri);
            ringtone.play();
        } catch (Exception e) {
            Log.e(TAG, "Error playing alarm sound", e);
        }
    }
}
