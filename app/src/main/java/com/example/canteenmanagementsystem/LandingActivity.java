package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        // Buttons logic
        MaterialButton btnLogin = findViewById(R.id.btnLoginLanding);
        MaterialButton btnRegister = findViewById(R.id.btnRegisterLanding);

        btnLogin.setOnClickListener(v -> startActivity(new Intent(LandingActivity.this, LoginActivity.class)));
        btnRegister.setOnClickListener(v -> startActivity(new Intent(LandingActivity.this, RegisterActivity.class)));
    }
}