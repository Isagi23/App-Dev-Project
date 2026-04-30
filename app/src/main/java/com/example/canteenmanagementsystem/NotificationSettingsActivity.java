package com.example.canteenmanagementsystem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private static final String PREFS_NAME = "NotificationSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupSwitches();
    }

    private void setupSwitches() {
        SwitchMaterial switchOrders = findViewById(R.id.switchOrders);
        SwitchMaterial switchStaff = findViewById(R.id.switchStaff);
        SwitchMaterial switchReports = findViewById(R.id.switchReports);
        SwitchMaterial switchSystem = findViewById(R.id.switchSystem);

        // Load saved states
        switchOrders.setChecked(prefs.getBoolean("orders", true));
        switchStaff.setChecked(prefs.getBoolean("staff", true));
        switchReports.setChecked(prefs.getBoolean("reports", true));
        switchSystem.setChecked(prefs.getBoolean("system", true));

        switchOrders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("orders", isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Order notifications " + status, Toast.LENGTH_SHORT).show();
        });

        switchStaff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("staff", isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Staff alerts " + status, Toast.LENGTH_SHORT).show();
        });

        switchReports.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("reports", isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Report notifications " + status, Toast.LENGTH_SHORT).show();
        });

        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveSetting("system", isChecked);
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "System updates " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void saveSetting(String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }
}