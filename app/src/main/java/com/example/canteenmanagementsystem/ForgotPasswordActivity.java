package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private MaterialButton btnSendResetLink;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        LinearLayout llBackToLogin = findViewById(R.id.llBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        btnSendResetLink.setOnClickListener(v -> resetPassword());
        llBackToLogin.setOnClickListener(v -> finish());
    }

    private void resetPassword() {
        if (etEmail.getText() == null) return;
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSendResetLink.setEnabled(false);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendResetLink.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email.", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(ForgotPasswordActivity.this, "Error! Reset link not sent: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}