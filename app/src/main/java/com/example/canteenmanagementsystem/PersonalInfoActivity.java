package com.example.canteenmanagementsystem;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PersonalInfoActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPhone, etAdminId;
    private ImageView ivProfile;
    private TextView tvLastUpdated;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAdminId = findViewById(R.id.etAdminId);
        ivProfile = findViewById(R.id.ivProfile);
        tvLastUpdated = findViewById(R.id.tvLastUpdated);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            Glide.with(this).load(selectedImageUri).into(ivProfile);
                        }
                    }
                }
        );

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDiscard).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveChanges());
        findViewById(R.id.btnEditPhoto).setOnClickListener(v -> openGallery());

        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etFullName.setText(user.getDisplayName());
            etEmail.setText(user.getEmail());
            
            // For this project, we'll use a placeholder for phone and ID if not in Auth
            // In a real app, these would come from a 'users' collection in Firestore
            etAdminId.setText("ADM-9420-24");
            etPhone.setText("+1 555-012-3456");

            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .into(ivProfile);
            }

            String date = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date());
            tvLastUpdated.setText("Last updated: " + date);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void saveChanges() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Name is required");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name);

            if (selectedImageUri != null) {
                builder.setPhotoUri(selectedImageUri);
            }

            UserProfileChangeRequest profileUpdates = builder.build();

            findViewById(R.id.btnSave).setEnabled(false);
            Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!email.equals(user.getEmail())) {
                                user.updateEmail(email).addOnCompleteListener(emailTask -> {
                                    finishUpdate();
                                });
                            } else {
                                finishUpdate();
                            }
                        } else {
                            findViewById(R.id.btnSave).setEnabled(true);
                            Toast.makeText(PersonalInfoActivity.this, "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void finishUpdate() {
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}