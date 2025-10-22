package com.stoneos.launcher;

import android.app.Application;
import android.util.Log;

/**
 * Application class for StoneLauncher
 *
 * Handles application-level initialization and lifecycle.
 */
public class LauncherApplication extends Application {

    private static final String TAG = "StoneLauncher";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "StoneLauncher Application onCreate");
    }
}
