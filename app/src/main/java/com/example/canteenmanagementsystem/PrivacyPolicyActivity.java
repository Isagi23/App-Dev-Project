package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PrivacyPolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

//        findViewById(R.id.btnDownloadPdf).setOnClickListener(v ->
//            Toast.makeText(this, "Downloading Privacy Policy PDF...", Toast.LENGTH_SHORT).show()
//        );
//
//        findViewById(R.id.btnContactSupport).setOnClickListener(v ->
//            Toast.makeText(this, "Redirecting to Support...", Toast.LENGTH_SHORT).show()
//        );
    }
}