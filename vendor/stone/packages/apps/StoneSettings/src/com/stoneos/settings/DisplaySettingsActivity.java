package com.stoneos.settings;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

/**
 * Display Settings activity for controlling screen brightness.
 * Provides both manual brightness control and adaptive brightness toggle.
 */
public class DisplaySettingsActivity extends Activity {

    private static final String TAG = "StoneSettings";
    private static final int MIN_BRIGHTNESS = 0;
    private static final int MAX_BRIGHTNESS = 255;

    private SeekBar brightnessSeekBar;
    private Switch adaptiveBrightnessSwitch;
    private ContentResolver contentResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_settings);

        contentResolver = getContentResolver();

        brightnessSeekBar = findViewById(R.id.brightness_seekbar);
        adaptiveBrightnessSwitch = findViewById(R.id.adaptive_brightness_switch);

        // Load current brightness settings
        loadCurrentBrightness();
        loadAdaptiveBrightnessState();

        // Set up brightness SeekBar listener
        brightnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setBrightness(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Disable adaptive brightness when user manually adjusts
                if (adaptiveBrightnessSwitch.isChecked()) {
                    adaptiveBrightnessSwitch.setChecked(false);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Nothing to do
            }
        });

        // Set up adaptive brightness switch listener
        adaptiveBrightnessSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAdaptiveBrightness(isChecked);
            }
        });
    }

    /**
     * Loads the current system brightness value and updates the SeekBar.
     */
    private void loadCurrentBrightness() {
        try {
            int brightness = Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS);
            brightnessSeekBar.setProgress(brightness);
            Log.d(TAG, "Current brightness: " + brightness);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Failed to load brightness setting", e);
            brightnessSeekBar.setProgress(128); // Default to 50%
        }
    }

    /**
     * Loads the current adaptive brightness state and updates the Switch.
     */
    private void loadAdaptiveBrightnessState() {
        try {
            int mode = Settings.System.getInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE);
            boolean isAdaptive = (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
            adaptiveBrightnessSwitch.setChecked(isAdaptive);
            Log.d(TAG, "Adaptive brightness: " + isAdaptive);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Failed to load adaptive brightness setting", e);
            adaptiveBrightnessSwitch.setChecked(false);
        }
    }

    /**
     * Sets the system brightness to the specified level (0-255).
     */
    private void setBrightness(int brightness) {
        try {
            // Clamp to valid range
            brightness = Math.max(MIN_BRIGHTNESS, Math.min(MAX_BRIGHTNESS, brightness));

            // Set brightness mode to manual
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            // Set brightness value
            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, brightness);

            Log.d(TAG, "Brightness set to: " + brightness);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set brightness", e);
        }
    }

    /**
     * Enables or disables adaptive brightness.
     */
    private void setAdaptiveBrightness(boolean enabled) {
        try {
            int mode = enabled
                ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                : Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

            Settings.System.putInt(contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE, mode);

            Log.d(TAG, "Adaptive brightness: " + enabled);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set adaptive brightness", e);
        }
    }
}
