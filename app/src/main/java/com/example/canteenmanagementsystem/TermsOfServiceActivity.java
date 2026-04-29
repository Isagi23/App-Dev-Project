package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class TermsOfServiceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_of_service);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        
        // PDF download logic can be added here if needed
//        findViewById(R.id.btnDownloadPdf).setOnClickListener(v -> {
//            // Implementation for PDF export
//        });
    }
}