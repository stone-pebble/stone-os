package com.stoneos.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/**
 * BroadcastReceiver that provides a programmatic API for controlling device settings.
 * This enables AI agents to control settings through broadcast intents.
 *
 * Supported actions:
 * - com.stoneos.settings.SET_WIFI_STATE (extra: boolean "enabled")
 * - com.stoneos.settings.SET_BLUETOOTH_STATE (extra: boolean "enabled")
 * - com.stoneos.settings.SET_BRIGHTNESS (extra: int "level" from 0-255)
 *
 * Example usage via adb:
 * adb shell am broadcast -a com.stoneos.settings.SET_WIFI_STATE --ez enabled true
 * adb shell am broadcast -a com.stoneos.settings.SET_BLUETOOTH_STATE --ez enabled false
 * adb shell am broadcast -a com.stoneos.settings.SET_BRIGHTNESS --ei level 128
 */
public class SettingsControlReceiver extends BroadcastReceiver {

    private static final String TAG = "StoneSettingsAPI";

    // Action constants
    private static final String ACTION_SET_WIFI_STATE = "com.stoneos.settings.SET_WIFI_STATE";
    private static final String ACTION_SET_BLUETOOTH_STATE = "com.stoneos.settings.SET_BLUETOOTH_STATE";
    private static final String ACTION_SET_BRIGHTNESS = "com.stoneos.settings.SET_BRIGHTNESS";

    // Extra keys
    private static final String EXTRA_ENABLED = "enabled";
    private static final String EXTRA_LEVEL = "level";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }

        String action = intent.getAction();
        if (action == null) {
            return;
        }

        Log.d(TAG, "Received action: " + action);

        try {
            switch (action) {
                case ACTION_SET_WIFI_STATE:
                    handleSetWifiState(context, intent);
                    break;

                case ACTION_SET_BLUETOOTH_STATE:
                    handleSetBluetoothState(context, intent);
                    break;

                case ACTION_SET_BRIGHTNESS:
                    handleSetBrightness(context, intent);
                    break;

                default:
                    Log.w(TAG, "Unknown action: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling action: " + action, e);
        }
    }

    /**
     * Handles SET_WIFI_STATE action.
     * Enables or disables Wi-Fi based on the "enabled" boolean extra.
     */
    private void handleSetWifiState(Context context, Intent intent) {
        if (!intent.hasExtra(EXTRA_ENABLED)) {
            Log.e(TAG, "SET_WIFI_STATE: missing 'enabled' extra");
            return;
        }

        boolean enabled = intent.getBooleanExtra(EXTRA_ENABLED, false);

        WifiManager wifiManager = (WifiManager) context.getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null) {
            wifiManager.setWifiEnabled(enabled);
            Log.i(TAG, "Wi-Fi " + (enabled ? "enabled" : "disabled"));

            // Optional: Show toast notification
            String message = "Wi-Fi " + (enabled ? "turned on" : "turned off");
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "WifiManager not available");
        }
    }

    /**
     * Handles SET_BLUETOOTH_STATE action.
     * Enables or disables Bluetooth based on the "enabled" boolean extra.
     */
    private void handleSetBluetoothState(Context context, Intent intent) {
        if (!intent.hasExtra(EXTRA_ENABLED)) {
            Log.e(TAG, "SET_BLUETOOTH_STATE: missing 'enabled' extra");
            return;
        }

        boolean enabled = intent.getBooleanExtra(EXTRA_ENABLED, false);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            if (enabled) {
                bluetoothAdapter.enable();
                Log.i(TAG, "Bluetooth enabled");
            } else {
                bluetoothAdapter.disable();
                Log.i(TAG, "Bluetooth disabled");
            }

            // Optional: Show toast notification
            String message = "Bluetooth " + (enabled ? "turned on" : "turned off");
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "BluetoothAdapter not available");
        }
    }

    /**
     * Handles SET_BRIGHTNESS action.
     * Sets screen brightness to the specified level (0-255) from the "level" int extra.
     */
    private void handleSetBrightness(Context context, Intent intent) {
        if (!intent.hasExtra(EXTRA_LEVEL)) {
            Log.e(TAG, "SET_BRIGHTNESS: missing 'level' extra");
            return;
        }

        int level = intent.getIntExtra(EXTRA_LEVEL, -1);

        if (level < 0 || level > 255) {
            Log.e(TAG, "SET_BRIGHTNESS: invalid level " + level + " (must be 0-255)");
            return;
        }

        try {
            // Set brightness mode to manual
            Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            // Set brightness value
            Settings.System.putInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, level);

            Log.i(TAG, "Brightness set to: " + level);

            // Optional: Show toast notification
            String message = "Brightness set to " + Math.round((level / 255.0f) * 100) + "%";
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to set brightness", e);
        }
    }
}
