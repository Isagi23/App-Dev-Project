package com.example.canteenmanagementsystem;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvName;
    private ImageView ivProfile;
    private View notificationBadge;
    private ListenerRegistration notificationsListener;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize the image picker launcher
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            updateProfileImage(imageUri);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        tvName = view.findViewById(R.id.tvProfileName);
        ivProfile = view.findViewById(R.id.ivProfile);
        notificationBadge = view.findViewById(R.id.notificationBadge);

        updateUI();
        startNotificationsListener();

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> logout());
//        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditNameDialog());
        view.findViewById(R.id.btnPersonalInfo).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PersonalInfoActivity.class));
        });
        view.findViewById(R.id.btnSecurityPassword).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), SecurityActivity.class));
        });
        view.findViewById(R.id.btnNotificationSettings).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), NotificationSettingsActivity.class));
        });
        view.findViewById(R.id.btnTermsOfService).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), TermsOfServiceActivity.class));
        });
        view.findViewById(R.id.btnPrivacyPolicy).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), PrivacyPolicyActivity.class));
        });

        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
            });
        }
        
        // Click profile image to change it
        ivProfile.setOnClickListener(v -> openGallery());
        
        View btnBack = view.findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToHome();
                }
            });
        }
    }

    private void updateUI() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                tvName.setText(user.getDisplayName());
            } else {
                tvName.setText("Administrator");
            }

            // Load profile picture if exists
            if (user.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivProfile);
            }
        }
    }

    private void startNotificationsListener() {
        notificationsListener = db.collection("notifications")
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value != null && notificationBadge != null) {
                        notificationBadge.setVisibility(value.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void updateProfileImage(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Optimistically show the image using Glide
            Glide.with(this).load(imageUri).into(ivProfile);

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setPhotoUri(imageUri)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile picture", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showEditNameDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Profile Name");

        final EditText input = new EditText(requireContext());
        input.setText(user.getDisplayName());
        input.setHint("Enter full name");
        
        int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(requireContext());
        android.widget.FrameLayout.LayoutParams params = new  android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = paddingPx;
        params.rightMargin = paddingPx;
        params.topMargin = paddingPx/2;
        input.setLayoutParams(params);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty()) {
                updateProfileName(newName);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updateProfileName(String newName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                            updateUI();
                        } else {
                            Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}
