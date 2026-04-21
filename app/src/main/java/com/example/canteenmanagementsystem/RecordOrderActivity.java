package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.adapters.OrderMenuAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.MenuItem;
import com.example.canteenmanagementsystem.models.Order;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecordOrderActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteEmployee;
    private OrderMenuAdapter adapter;
    private TextView tvSummaryItems, tvSummaryTotal;
    private ChipGroup chipGroupCategories;
    private BottomNavigationView bottomNavigationView;
    private android.widget.ProgressBar progressBar;

    private FirebaseFirestore db;
    private final List<Employee> allEmployees = new ArrayList<>();
    private final List<Employee> filteredEmployees = new ArrayList<>();
    private final List<MenuItem> menuItemsList = new ArrayList<>();
    private final List<String> employeeNames = new ArrayList<>();
    private Employee selectedEmployee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_order);

        db = FirebaseFirestore.getInstance();

        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        autoCompleteEmployee = findViewById(R.id.autoCompleteEmployee);
        RecyclerView rvMenuItems = findViewById(R.id.rvMenuItems);
        tvSummaryItems = findViewById(R.id.tvSummaryItems);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        MaterialButton btnSaveOrder = findViewById(R.id.btnSaveOrder);
        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        // Removed toolbar navigation listener for back arrow

        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderMenuAdapter(menuItemsList, selectedItems -> updateSummary());
        rvMenuItems.setAdapter(adapter);

        setupCategoryFilter();
        setupBottomNavigation();

        autoCompleteEmployee.setOnItemClickListener((parent, view, position, id) -> {
            selectedEmployee = filteredEmployees.get(position);
            updateSummary();
        });

        loadEmployees();
        loadMenuItems();

        btnSaveOrder.setOnClickListener(v -> saveOrder());

        // Fix: Hide keyboard and clear focus when touching outside
        findViewById(R.id.rootLayout).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View focusedView = getCurrentFocus();
                if (focusedView != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    focusedView.clearFocus();
                }
            }
            return false;
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_orders);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (itemId == R.id.nav_staff) {
                startActivity(new Intent(this, EmployeeListActivity.class));
                return true;
            }
            if (itemId == R.id.nav_orders) return true;
            if (itemId == R.id.nav_report) {
                startActivity(new Intent(this, PayrollReportActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupCategoryFilter() {
        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            Chip chip = findViewById(checkedId);
            filterEmployeesByCategory(chip.getText().toString());
        });
    }

    private void filterEmployeesByCategory(String category) {
        filteredEmployees.clear();
        employeeNames.clear();

        for (Employee e : allEmployees) {
            if (e.getDepartment() != null && e.getDepartment().equalsIgnoreCase(category)) {
                filteredEmployees.add(e);
                employeeNames.add(e.getFullName());
            }
        }

        ArrayAdapter<String> employeeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, employeeNames);
        autoCompleteEmployee.setAdapter(employeeAdapter);
        autoCompleteEmployee.setText("", false);
        selectedEmployee = null;
        
        // Fix: Ensure the dropdown shows up if there are results
        autoCompleteEmployee.setOnClickListener(v -> {
            if (!employeeNames.isEmpty()) {
                autoCompleteEmployee.showDropDown();
            }
        });
        
        updateSummary();
    }

    private void loadEmployees() {
        db.collection("employees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allEmployees.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Employee employee = doc.toObject(Employee.class);
                employee.setId(doc.getId());
                allEmployees.add(employee);
            }
            // Default filter to first chip if available
            if (chipGroupCategories.getChildCount() > 0) {
                for (int i = 0; i < chipGroupCategories.getChildCount(); i++) {
                    View view = chipGroupCategories.getChildAt(i);
                    if (view instanceof Chip) {
                        Chip chip = (Chip) view;
                        if (chip.isChecked()) {
                            filterEmployeesByCategory(chip.getText().toString());
                            break;
                        }
                    }
                }
            }
        });
    }

    private void loadMenuItems() {
        db.collection("menuItems").get().addOnSuccessListener(queryDocumentSnapshots -> {
            menuItemsList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                MenuItem item = doc.toObject(MenuItem.class);
                item.setId(doc.getId());
                menuItemsList.add(item);
            }
            adapter.notifyDataSetChanged();
        });
    }

    private void updateSummary() {
        List<MenuItem> selected = adapter.getSelectedItems();
        double total = 0;
        StringBuilder itemsBuilder = new StringBuilder();
        
        if (selected.isEmpty()) {
            itemsBuilder.append("No items selected");
        } else {
            for (int i = 0; i < selected.size(); i++) {
                MenuItem item = selected.get(i);
                total += item.getPrice();
                itemsBuilder.append(item.getName());
                if (i < selected.size() - 1) itemsBuilder.append(", ");
            }
        }
        
        tvSummaryItems.setText(itemsBuilder.toString());
        tvSummaryTotal.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
    }

    private void saveOrder() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedEmployee == null) {
            Toast.makeText(this, "Please select an employee", Toast.LENGTH_SHORT).show();
            return;
        }

        List<MenuItem> selected = adapter.getSelectedItems();
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        double total = 0;
        List<String> itemNames = new ArrayList<>();
        for (MenuItem item : selected) {
            itemNames.add(item.getName());
            total += item.getPrice();
        }

        double finalTotal = total;
        new AlertDialog.Builder(this)
                .setTitle("Confirm Order")
                .setMessage(String.format(Locale.getDefault(), "Save order for %s — ₱%.2f?", selectedEmployee.getFullName(), total))
                .setPositiveButton("Save", (dialog, which) -> {
                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                    Date now = new Date();

                    Order order = new Order(null, selectedEmployee.getId(), selectedEmployee.getFullName(), itemNames, finalTotal, sdfDate.format(now), sdfMonth.format(now));
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    db.collection("orders").add(order).addOnSuccessListener(documentReference -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Order saved", Toast.LENGTH_SHORT).show();
                        showRunningMonthlyTotal(selectedEmployee.getId(), selectedEmployee.getFullName(), sdfMonth.format(now));
                    }).addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(RecordOrderActivity.this, "Error saving order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRunningMonthlyTotal(String employeeId, String fullName, String month) {
        db.collection("orders")
                .whereEqualTo("employeeId", employeeId)
                .whereEqualTo("month", month)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double monthlyTotal = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double amount = doc.getDouble("totalAmount");
                        if (amount != null) monthlyTotal += amount;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Order Recorded")
                            .setMessage(String.format(Locale.getDefault(), "%s's total for %s: ₱%.2f", fullName, month, monthlyTotal))
                            .setPositiveButton("OK", (dialog, which) -> resetForm())
                            .setCancelable(false)
                            .show();
                });
    }

    private void resetForm() {
        autoCompleteEmployee.setText("", false);
        selectedEmployee = null;
        
        // We need a way to clear selection in adapter. Let's add a clearSelection method to OrderMenuAdapter.
        adapter.clearSelection();
        updateSummary();
    }
}
