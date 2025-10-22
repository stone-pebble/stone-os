package com.stoneos.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main Settings activity that displays a list of settings categories.
 * Clicking a category underlines it and navigates to the corresponding sub-activity.
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set up category click listeners
        setupCategoryNavigation(R.id.category_network, NetworkSettingsActivity.class);
        setupCategoryNavigation(R.id.category_display, DisplaySettingsActivity.class);

        // Stub categories that show "Coming soon"
        setupStubCategory(R.id.category_connected_devices, "Connected devices");
        setupStubCategory(R.id.category_sound, "Sound & vibration");
        setupStubCategory(R.id.category_storage, "Storage");
        setupStubCategory(R.id.category_security, "Security & privacy");
        setupStubCategory(R.id.category_system, "System");
    }

    /**
     * Sets up navigation to a functional sub-activity.
     * When clicked, underlines the text and launches the activity.
     */
    private void setupCategoryNavigation(int textViewId, final Class<?> activityClass) {
        TextView textView = findViewById(textViewId);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Underline the text
                TextView tv = (TextView) v;
                SpannableString content = new SpannableString(tv.getText());
                content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                tv.setText(content);

                // Navigate to sub-activity
                Intent intent = new Intent(SettingsActivity.this, activityClass);
                startActivity(intent);
            }
        });
    }

    /**
     * Sets up a stub category that shows a "Coming soon" message.
     */
    private void setupStubCategory(int textViewId, final String categoryName) {
        TextView textView = findViewById(textViewId);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingsActivity.this,
                    categoryName + " - Coming soon",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
