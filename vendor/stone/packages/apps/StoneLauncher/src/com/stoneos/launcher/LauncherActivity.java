package com.stoneos.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * StoneLauncher - Minimalist launcher for StoneOS
 *
 * Displays a 3x4 grid of StoneOS app names and launches corresponding
 * third-party applications when tapped.
 */
public class LauncherActivity extends Activity {

    private static final String TAG = "StoneLauncher";

    // Package name mappings for StoneOS apps
    private static final String PKG_LISTEN = "com.spotify.music";           // Spotify
    private static final String PKG_GO = "com.google.android.apps.maps";    // Google Maps
    private static final String PKG_ASK = "com.google.android.googlequicksearchbox"; // Google Search
    private static final String PKG_TASK = "com.google.android.apps.tasks"; // Google Tasks
    private static final String PKG_SET = "com.android.settings";           // Settings
    private static final String PKG_TICK = "com.google.android.deskclock";  // Clock
    private static final String PKG_LOOK = "com.android.camera2";           // Camera
    private static final String PKG_PLAN = "com.google.android.calendar";   // Google Calendar
    private static final String PKG_THINK = "notion.id";                    // Notion
    private static final String PKG_CONNECT = "com.android.contacts";       // Contacts
    private static final String PKG_FUND = "com.google.android.apps.nbu.paisa.user"; // Google Pay
    private static final String PKG_REFLECT = "com.google.android.keep";    // Google Keep

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher_activity);

        Log.d(TAG, "StoneLauncher onCreate");

        // Set up click listeners for all 12 app tiles
        setupAppLauncher(R.id.app_listen, PKG_LISTEN, "LISTEN");
        setupAppLauncher(R.id.app_go, PKG_GO, "GO");
        setupAppLauncher(R.id.app_ask, PKG_ASK, "ASK");
        setupAppLauncher(R.id.app_task, PKG_TASK, "TASK");
        setupAppLauncher(R.id.app_set, PKG_SET, "SET");
        setupAppLauncher(R.id.app_tick, PKG_TICK, "TICK");
        setupAppLauncher(R.id.app_look, PKG_LOOK, "LOOK");
        setupAppLauncher(R.id.app_plan, PKG_PLAN, "PLAN");
        setupAppLauncher(R.id.app_think, PKG_THINK, "THINK");
        setupAppLauncher(R.id.app_connect, PKG_CONNECT, "CONNECT");
        setupAppLauncher(R.id.app_fund, PKG_FUND, "FUND");
        setupAppLauncher(R.id.app_reflect, PKG_REFLECT, "REFLECT");
    }

    /**
     * Set up click listener for an app tile
     *
     * @param viewId Resource ID of the TextView
     * @param packageName Package name of the app to launch
     * @param appName Display name for logging and toasts
     */
    private void setupAppLauncher(int viewId, final String packageName, final String appName) {
        TextView appView = findViewById(viewId);
        if (appView == null) {
            Log.e(TAG, "View not found for " + appName);
            return;
        }

        appView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchApp(packageName, appName);
            }
        });
    }

    /**
     * Launch an app by package name
     *
     * @param packageName Package name of the app
     * @param appName Display name for user feedback
     */
    private void launchApp(String packageName, String appName) {
        Log.d(TAG, "Attempting to launch " + appName + " (" + packageName + ")");

        try {
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);

            if (launchIntent != null) {
                // App is installed, launch it
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                Log.d(TAG, "Successfully launched " + appName);
            } else {
                // App not installed
                String message = appName + " not installed";
                Log.w(TAG, message);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            // Handle any errors
            String message = "Error launching " + appName + ": " + e.getMessage();
            Log.e(TAG, message, e);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "StoneLauncher onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "StoneLauncher onPause");
    }

    @Override
    public void onBackPressed() {
        // Don't allow back button to exit launcher
        // This is standard launcher behavior
        Log.d(TAG, "Back button pressed - ignored");
    }
}
