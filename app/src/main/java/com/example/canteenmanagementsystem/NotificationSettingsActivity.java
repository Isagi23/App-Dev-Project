package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class NotificationSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        setupSwitches();
    }

    private void setupSwitches() {
        SwitchMaterial switchOrders = findViewById(R.id.switchOrders);
        SwitchMaterial switchStaff = findViewById(R.id.switchStaff);
        SwitchMaterial switchReports = findViewById(R.id.switchReports);
        SwitchMaterial switchSystem = findViewById(R.id.switchSystem);

        switchOrders.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Order notifications " + status, Toast.LENGTH_SHORT).show();
        });

        switchStaff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Staff alerts " + status, Toast.LENGTH_SHORT).show();
        });

        switchReports.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Report notifications " + status, Toast.LENGTH_SHORT).show();
        });

        switchSystem.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "System updates " + status, Toast.LENGTH_SHORT).show();
        });
    }
}