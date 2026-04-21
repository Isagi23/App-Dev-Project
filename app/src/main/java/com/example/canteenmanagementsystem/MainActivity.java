package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.canteenmanagementsystem.models.Order;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView tvGreeting, tvCurrentDate, tvCycleBadge;
    private TextView tvTotalEmployees, tvOrdersToday, tvMonthExpense, tvActiveUsers;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ListenerRegistration employeesListener, ordersTodayListener, monthOrdersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvCycleBadge = findViewById(R.id.tvCycleBadge);
        
        tvTotalEmployees = findViewById(R.id.tvTotalEmployeesCount);
        tvOrdersToday = findViewById(R.id.tvOrdersTodayCount);
        tvMonthExpense = findViewById(R.id.tvMonthExpense);
        tvActiveUsers = findViewById(R.id.tvActiveUsersCount);

        updateHeaderInfo();
        setupNavigation();
        startStatsListeners();
    }

    private void updateHeaderInfo() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = getString(R.string.greeting_morning);
        } else if (hour >= 12 && hour < 17) {
            greeting = getString(R.string.greeting_afternoon);
        } else {
            greeting = getString(R.string.greeting_evening);
        }
        tvGreeting.setText(greeting);

        SimpleDateFormat dateSdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        tvCurrentDate.setText(dateSdf.format(new Date()));

        SimpleDateFormat monthYearSdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String cycleDate = monthYearSdf.format(new Date());
        tvCycleBadge.setText(getString(R.string.cycle_active, cycleDate));
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeaderInfo();
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (employeesListener != null) employeesListener.remove();
        if (ordersTodayListener != null) ordersTodayListener.remove();
        if (monthOrdersListener != null) monthOrdersListener.remove();
    }

    private void setupNavigation() {
        // Open Drawer
        findViewById(R.id.btnOpenDrawer).setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Notification Button
        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Quick Actions
        findViewById(R.id.btnRecordOrder).setOnClickListener(v -> startActivity(new Intent(this, RecordOrderActivity.class)));
        findViewById(R.id.btnManageEmployees).setOnClickListener(v -> startActivity(new Intent(this, EmployeeListActivity.class)));
        findViewById(R.id.btnManageMenu).setOnClickListener(v -> startActivity(new Intent(this, MenuItemActivity.class)));
        findViewById(R.id.btnExpenseHistory).setOnClickListener(v -> startActivity(new Intent(this, ExpenseHistoryActivity.class)));
        findViewById(R.id.btnPayrollReport).setOnClickListener(v -> startActivity(new Intent(this, PayrollReportActivity.class)));
        
        findViewById(R.id.tvSeeAllActions).setOnClickListener(v -> {
             // You can decide where "See all" goes, maybe a dedicated ActionsActivity 
             // or just a Toast for now.
             Toast.makeText(this, "See all actions clicked", Toast.LENGTH_SHORT).show();
        });

        // Bottom Navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            if (itemId == R.id.nav_staff) {
                startActivity(new Intent(this, EmployeeListActivity.class));
                return true;
            }
            if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, RecordOrderActivity.class));
                return true;
            }
            if (itemId == R.id.nav_report) {
                startActivity(new Intent(this, PayrollReportActivity.class));
                return true;
            }
            return false;
        });

        // Sidebar Navigation
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_employees) {
                startActivity(new Intent(this, EmployeeListActivity.class));
            } else if (id == R.id.nav_record_order) {
                startActivity(new Intent(this, RecordOrderActivity.class));
            } else if (id == R.id.nav_menu_items) {
                startActivity(new Intent(this, MenuItemActivity.class));
            } else if (id == R.id.nav_expense_history) {
                startActivity(new Intent(this, ExpenseHistoryActivity.class));
            } else if (id == R.id.nav_payroll_report) {
                startActivity(new Intent(this, PayrollReportActivity.class));
            } else if (id == R.id.nav_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void startStatsListeners() {
        employeesListener = db.collection("employees").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                tvTotalEmployees.setText(String.valueOf(value.size()));
            }
        });

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        ordersTodayListener = db.collection("orders").whereEqualTo("date", today).addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                tvOrdersToday.setText(String.valueOf(value.size()));
            }
        });

        monthOrdersListener = db.collection("orders").whereEqualTo("month", currentMonth).addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                double totalExpense = 0;
                Set<String> activeUsers = new HashSet<>();
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    totalExpense += order.getTotalAmount();
                    activeUsers.add(order.getEmployeeId());
                }

                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                formatter.setMaximumFractionDigits(0);
                String formattedExpense = formatter.format(totalExpense);
                tvMonthExpense.setText(formattedExpense);

                tvActiveUsers.setText(String.valueOf(activeUsers.size()));
            }
        });
    }
}
