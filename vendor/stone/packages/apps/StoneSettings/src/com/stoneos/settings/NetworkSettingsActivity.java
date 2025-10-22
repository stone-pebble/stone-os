package com.stoneos.settings;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Network Settings activity for controlling Wi-Fi.
 * Provides a switch to enable/disable Wi-Fi and shows current status.
 */
public class NetworkSettingsActivity extends Activity {

    private static final String TAG = "StoneSettings";

    private Switch wifiSwitch;
    private TextView wifiStatus;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_settings);

        wifiManager = (WifiManager) getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);

        wifiSwitch = findViewById(R.id.wifi_switch);
        wifiStatus = findViewById(R.id.wifi_status);

        // Load current Wi-Fi state
        loadWifiState();

        // Set up Wi-Fi switch listener
        wifiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setWifiState(isChecked);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh Wi-Fi state when activity resumes
        loadWifiState();
    }

    /**
     * Loads the current Wi-Fi state and updates the UI.
     */
    private void loadWifiState() {
        if (wifiManager != null) {
            boolean isEnabled = wifiManager.isWifiEnabled();
            wifiSwitch.setChecked(isEnabled);
            updateWifiStatus(isEnabled);
            Log.d(TAG, "Wi-Fi state: " + (isEnabled ? "enabled" : "disabled"));
        }
    }

    /**
     * Enables or disables Wi-Fi.
     */
    private void setWifiState(boolean enabled) {
        if (wifiManager != null) {
            try {
                wifiManager.setWifiEnabled(enabled);
                updateWifiStatus(enabled);
                Log.d(TAG, "Wi-Fi " + (enabled ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Failed to set Wi-Fi state", e);
            }
        }
    }

    /**
     * Updates the Wi-Fi status TextView.
     */
    private void updateWifiStatus(boolean enabled) {
        if (enabled) {
            wifiStatus.setText(R.string.wifi_enabled);
        } else {
            wifiStatus.setText(R.string.wifi_disabled);
        }
    }
}
