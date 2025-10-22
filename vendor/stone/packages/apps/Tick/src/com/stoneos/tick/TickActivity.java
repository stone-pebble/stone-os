package com.stoneos.tick;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class TickActivity extends Activity {
    private static final String TAG = "TickActivity";

    // World Clock
    private TextView timeSF, timeNY, timeTokyo;
    private Handler clockHandler = new Handler();
    private Runnable clockRunnable;

    // Alarms
    private LinearLayout alarmsContainer;
    private List<Alarm> alarms = new ArrayList<>();

    // Stopwatch
    private TextView stopwatchTime;
    private Button stopwatchStartStop, stopwatchLapReset;
    private LinearLayout lapsContainer;
    private Handler stopwatchHandler = new Handler();
    private long stopwatchStartTime = 0;
    private long stopwatchElapsedTime = 0;
    private boolean isStopwatchRunning = false;
    private List<Long> laps = new ArrayList<>();

    // Timer
    private TextView timerDisplay, timerHours, timerMinutes, timerSeconds;
    private Button timerStartStop;
    private Handler timerHandler = new Handler();
    private int timerH = 0, timerM = 0, timerS = 0;
    private int timerRemainingSeconds = 0;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tick);

        initializeViews();
        initializeWorldClock();
        initializeAlarms();
        initializeStopwatch();
        initializeTimer();

        // Handle intent if launched from agent API
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        String action = intent.getAction();
        Log.d(TAG, "Handling intent action: " + action);

        if ("SET_ALARM".equals(action)) {
            int hour = intent.getIntExtra("hour", -1);
            int minute = intent.getIntExtra("minute", -1);
            if (hour >= 0 && minute >= 0) {
                addAlarm(hour, minute);
            }
        } else if ("SET_TIMER".equals(action)) {
            int durationSeconds = intent.getIntExtra("duration_seconds", -1);
            if (durationSeconds > 0) {
                startTimerFromSeconds(durationSeconds);
            }
        }
    }

    private void initializeViews() {
        // World Clock
        timeSF = findViewById(R.id.time_sf);
        timeNY = findViewById(R.id.time_ny);
        timeTokyo = findViewById(R.id.time_tokyo);

        // Alarms
        alarmsContainer = findViewById(R.id.alarms_container);

        // Stopwatch
        stopwatchTime = findViewById(R.id.stopwatch_time);
        stopwatchStartStop = findViewById(R.id.stopwatch_start_stop);
        stopwatchLapReset = findViewById(R.id.stopwatch_lap_reset);
        lapsContainer = findViewById(R.id.laps_container);

        // Timer
        timerDisplay = findViewById(R.id.timer_display);
        timerHours = findViewById(R.id.timer_hours);
        timerMinutes = findViewById(R.id.timer_minutes);
        timerSeconds = findViewById(R.id.timer_seconds);
        timerStartStop = findViewById(R.id.timer_start_stop);
    }

    // ==================== WORLD CLOCK ====================

    private void initializeWorldClock() {
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateWorldClocks();
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void updateWorldClocks() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);

        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        timeSF.setText(sdf.format(Calendar.getInstance().getTime()));

        sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        timeNY.setText(sdf.format(Calendar.getInstance().getTime()));

        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        timeTokyo.setText(sdf.format(Calendar.getInstance().getTime()));
    }

    // ==================== ALARMS ====================

    private void initializeAlarms() {
        // Initialize with sample alarms matching the React prototype
        alarms.add(new Alarm("morning", "07:00", "weekdays", true));
        alarms.add(new Alarm("workout", "06:30", "tue, thu, sat", true));
        alarms.add(new Alarm("meditation", "08:00", "daily", true));
        alarms.add(new Alarm("lunch", "12:00", "weekdays", true));
        alarms.add(new Alarm("meeting", "09:30", "mon, wed, fri", true));

        renderAlarms();
    }

    private void renderAlarms() {
        alarmsContainer.removeAllViews();

        for (int i = 0; i < alarms.size(); i++) {
            final Alarm alarm = alarms.get(i);
            final int index = i;

            LinearLayout alarmView = new LinearLayout(this);
            alarmView.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(16));
            alarmView.setLayoutParams(params);

            // Title
            TextView titleView = new TextView(this);
            titleView.setText(alarm.title);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            titleView.setTextColor(Color.WHITE);
            titleView.setTypeface(android.graphics.Typeface.SERIF);
            alarmView.addView(titleView);

            // Time
            TextView timeView = new TextView(this);
            timeView.setText(alarm.time);
            timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            timeView.setTextColor(Color.WHITE);
            timeView.setTypeface(android.graphics.Typeface.SERIF);
            LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            timeParams.setMargins(0, dpToPx(4), 0, dpToPx(4));
            timeView.setLayoutParams(timeParams);
            alarmView.addView(timeView);

            // Repeat
            TextView repeatView = new TextView(this);
            repeatView.setText(alarm.repeat);
            repeatView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            repeatView.setTextColor(Color.parseColor("#80FFFFFF"));
            repeatView.setTypeface(android.graphics.Typeface.SERIF);
            alarmView.addView(repeatView);

            alarmsContainer.addView(alarmView);
        }
    }

    public void addAlarm(int hour, int minute) {
        String time = String.format(Locale.US, "%02d:%02d", hour, minute);
        Alarm newAlarm = new Alarm("alarm", time, "daily", true);
        alarms.add(newAlarm);
        renderAlarms();

        // Schedule the actual alarm
        scheduleAlarm(newAlarm, hour, minute);
        Log.d(TAG, "Alarm added: " + time);
    }

    private void scheduleAlarm(Alarm alarm, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("title", alarm.title);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            alarm.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.getTimeInMillis(),
            pendingIntent
        );
    }

    // ==================== STOPWATCH ====================

    private void initializeStopwatch() {
        stopwatchStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStopwatchRunning) {
                    stopStopwatch();
                } else {
                    startStopwatch();
                }
            }
        });

        stopwatchLapReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isStopwatchRunning) {
                    recordLap();
                } else {
                    resetStopwatch();
                }
            }
        });

        updateStopwatchDisplay();
    }

    private void startStopwatch() {
        if (!isStopwatchRunning) {
            stopwatchStartTime = SystemClock.uptimeMillis() - stopwatchElapsedTime;
            stopwatchHandler.post(stopwatchUpdateRunnable);
            isStopwatchRunning = true;
            stopwatchStartStop.setText(R.string.stop);
            stopwatchLapReset.setText(R.string.lap);
        }
    }

    private void stopStopwatch() {
        if (isStopwatchRunning) {
            stopwatchHandler.removeCallbacks(stopwatchUpdateRunnable);
            isStopwatchRunning = false;
            stopwatchStartStop.setText(R.string.start);
            stopwatchLapReset.setText(R.string.reset);
        }
    }

    private void resetStopwatch() {
        stopwatchElapsedTime = 0;
        laps.clear();
        updateStopwatchDisplay();
        renderLaps();
    }

    private void recordLap() {
        laps.add(stopwatchElapsedTime);
        renderLaps();
    }

    private Runnable stopwatchUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            stopwatchElapsedTime = SystemClock.uptimeMillis() - stopwatchStartTime;
            updateStopwatchDisplay();
            stopwatchHandler.postDelayed(this, 10);
        }
    };

    private void updateStopwatchDisplay() {
        int minutes = (int) (stopwatchElapsedTime / 60000);
        int seconds = (int) ((stopwatchElapsedTime % 60000) / 1000);
        int centiseconds = (int) ((stopwatchElapsedTime % 1000) / 10);

        String formatted = String.format(Locale.US, "%02d:%02d.%02d",
            minutes, seconds, centiseconds);
        stopwatchTime.setText(formatted);
    }

    private void renderLaps() {
        lapsContainer.removeAllViews();

        for (long lapTime : laps) {
            TextView lapView = new TextView(this);

            int minutes = (int) (lapTime / 60000);
            int seconds = (int) ((lapTime % 60000) / 1000);
            int centiseconds = (int) ((lapTime % 1000) / 10);

            String formatted = String.format(Locale.US, "%02d:%02d.%02d",
                minutes, seconds, centiseconds);

            lapView.setText(formatted);
            lapView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            lapView.setTextColor(Color.parseColor("#B3FFFFFF"));
            lapView.setTypeface(android.graphics.Typeface.SERIF);
            lapView.setGravity(Gravity.CENTER);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, dpToPx(8));
            lapView.setLayoutParams(params);

            lapsContainer.addView(lapView);
        }
    }

    // ==================== TIMER ====================

    private void initializeTimer() {
        // Hour controls
        findViewById(R.id.timer_hour_plus).setOnClickListener(v -> {
            timerH = (timerH + 1) % 24;
            updateTimerDisplay();
        });
        findViewById(R.id.timer_hour_minus).setOnClickListener(v -> {
            timerH = (timerH - 1 + 24) % 24;
            updateTimerDisplay();
        });

        // Minute controls
        findViewById(R.id.timer_minute_plus).setOnClickListener(v -> {
            timerM = (timerM + 1) % 60;
            updateTimerDisplay();
        });
        findViewById(R.id.timer_minute_minus).setOnClickListener(v -> {
            timerM = (timerM - 1 + 60) % 60;
            updateTimerDisplay();
        });

        // Second controls
        findViewById(R.id.timer_second_plus).setOnClickListener(v -> {
            timerS = (timerS + 1) % 60;
            updateTimerDisplay();
        });
        findViewById(R.id.timer_second_minus).setOnClickListener(v -> {
            timerS = (timerS - 1 + 60) % 60;
            updateTimerDisplay();
        });

        // Start/Stop button
        timerStartStop.setOnClickListener(v -> {
            if (isTimerRunning) {
                stopTimer();
            } else {
                startTimer();
            }
        });

        updateTimerDisplay();
    }

    private void startTimer() {
        int totalSeconds = (timerH * 3600) + (timerM * 60) + timerS;
        if (totalSeconds > 0) {
            timerRemainingSeconds = totalSeconds;
            isTimerRunning = true;
            timerStartStop.setText(R.string.stop);
            timerHandler.post(timerUpdateRunnable);
        }
    }

    public void startTimerFromSeconds(int durationSeconds) {
        if (durationSeconds > 0) {
            timerRemainingSeconds = durationSeconds;
            timerH = durationSeconds / 3600;
            timerM = (durationSeconds % 3600) / 60;
            timerS = durationSeconds % 60;
            isTimerRunning = true;
            timerStartStop.setText(R.string.stop);
            updateTimerDisplay();
            timerHandler.post(timerUpdateRunnable);
            Log.d(TAG, "Timer started: " + durationSeconds + " seconds");
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(timerUpdateRunnable);
        timerStartStop.setText(R.string.start);
        timerRemainingSeconds = 0;
    }

    private Runnable timerUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (timerRemainingSeconds > 0) {
                timerRemainingSeconds--;

                int h = timerRemainingSeconds / 3600;
                int m = (timerRemainingSeconds % 3600) / 60;
                int s = timerRemainingSeconds % 60;

                timerHours.setText(String.format(Locale.US, "%02d", h));
                timerMinutes.setText(String.format(Locale.US, "%02d", m));
                timerSeconds.setText(String.format(Locale.US, "%02d", s));
                timerDisplay.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));

                timerHandler.postDelayed(this, 1000);
            } else {
                // Timer finished
                stopTimer();
                Log.d(TAG, "Timer finished");
            }
        }
    };

    private void updateTimerDisplay() {
        timerHours.setText(String.format(Locale.US, "%02d", timerH));
        timerMinutes.setText(String.format(Locale.US, "%02d", timerM));
        timerSeconds.setText(String.format(Locale.US, "%02d", timerS));
        timerDisplay.setText(String.format(Locale.US, "%02d:%02d:%02d", timerH, timerM, timerS));
    }

    // ==================== UTILITY ====================

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockRunnable);
        stopwatchHandler.removeCallbacks(stopwatchUpdateRunnable);
        timerHandler.removeCallbacks(timerUpdateRunnable);
    }

    // ==================== ALARM DATA CLASS ====================

    private static class Alarm {
        String title;
        String time;
        String repeat;
        boolean enabled;

        Alarm(String title, String time, String repeat, boolean enabled) {
            this.title = title;
            this.time = time;
            this.repeat = repeat;
            this.enabled = enabled;
        }
    }
}
