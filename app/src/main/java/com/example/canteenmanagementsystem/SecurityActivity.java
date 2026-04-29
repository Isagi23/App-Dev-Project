package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SecurityActivity extends AppCompatActivity {

    private TextInputEditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security);

        mAuth = FirebaseAuth.getInstance();

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnUpdatePassword).setOnClickListener(v -> updatePassword());
    }

    private void updatePassword() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPass)) {
            etCurrentPassword.setError("Current password is required");
            return;
        }

        if (newPass.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Re-authenticate user before updating password
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);

            findViewById(R.id.btnUpdatePassword).setEnabled(false);
            Toast.makeText(this, "Updating password...", Toast.LENGTH_SHORT).show();

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        findViewById(R.id.btnUpdatePassword).setEnabled(true);
                        if (updateTask.isSuccessful()) {
                            Toast.makeText(SecurityActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(SecurityActivity.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    findViewById(R.id.btnUpdatePassword).setEnabled(true);
                    etCurrentPassword.setError("Incorrect current password");
                    Toast.makeText(SecurityActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}