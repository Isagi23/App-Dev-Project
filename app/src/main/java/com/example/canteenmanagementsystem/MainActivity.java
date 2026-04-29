package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        setupNavigation();

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), "HOME");
        }
    }

    private void setupNavigation() {
        // Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Fragment selectedFragment = null;
            String tag = "";

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                tag = "HOME";
            } else if (itemId == R.id.nav_staff) {
                selectedFragment = new StaffFragment();
                tag = "STAFF";
            } else if (itemId == R.id.nav_orders) {
                selectedFragment = new OrdersFragment();
                tag = "ORDERS";
            } else if (itemId == R.id.nav_report) {
                selectedFragment = new ReportsFragment();
                tag = "REPORTS";
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
                tag = "PROFILE";
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, tag);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment, tag);
        transaction.commit();
    }

    public void openDrawer() {
        // Method kept for compatibility with other fragments if needed, 
        // but drawer layout has been removed.
    }

    public void navigateToHome() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    public void navigateToStaff() {
        bottomNavigationView.setSelectedItemId(R.id.nav_staff);
    }

    public void navigateToOrders() {
        bottomNavigationView.setSelectedItemId(R.id.nav_orders);
    }

    public void navigateToReports() {
        bottomNavigationView.setSelectedItemId(R.id.nav_report);
    }

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}
