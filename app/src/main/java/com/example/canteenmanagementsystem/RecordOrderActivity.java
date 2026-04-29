package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.adapters.OrderMenuAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.MenuItem;
import com.example.canteenmanagementsystem.models.Notification;
import com.example.canteenmanagementsystem.models.Order;
import com.example.canteenmanagementsystem.models.OrderItem;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RecordOrderActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteEmployee;
    private RecyclerView rvMenuItems;
    private OrderMenuAdapter adapter;
    private List<MenuItem> menuItemsList = new ArrayList<>();
    private List<Employee> employeeList = new ArrayList<>();
    private TextView tvSummaryItems, tvSummaryTotal;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String selectedEmployeeId;
    private String selectedEmployeeName;
    private String currentDepartmentFilter = "Packing House";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_order);

        db = FirebaseFirestore.getInstance();

        autoCompleteEmployee = findViewById(R.id.autoCompleteEmployee);
        rvMenuItems = findViewById(R.id.rvMenuItems);
        tvSummaryItems = findViewById(R.id.tvSummaryItems);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        progressBar = findViewById(R.id.progressBar);

        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderMenuAdapter(menuItemsList, this::onQuantityChanged);
        rvMenuItems.setAdapter(adapter);

        findViewById(R.id.btnSaveOrder).setOnClickListener(v -> saveOrder());
        findViewById(R.id.toolbar).setOnClickListener(v -> finish());

        setupCategoryChips();
        loadEmployees();
        loadMenuItems();
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategories);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentDepartmentFilter = "Field"; // Default fallback
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    currentDepartmentFilter = chip.getText().toString().trim();
                }
            }
            updateEmployeeDropdown();
        });
        
        // Ensure initial filter matches the checked chip
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            Chip chip = chipGroup.findViewById(checkedId);
            if (chip != null) {
                currentDepartmentFilter = chip.getText().toString().trim();
            }
        }
    }

    private void loadEmployees() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("employees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            progressBar.setVisibility(View.GONE);
            employeeList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Employee employee = doc.toObject(Employee.class);
                employee.setId(doc.getId());
                employeeList.add(employee);
            }
            updateEmployeeDropdown();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to load employees", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateEmployeeDropdown() {
        List<String> names = new ArrayList<>();
        for (Employee e : employeeList) {
            if (e.getDepartment() != null && e.getDepartment().equalsIgnoreCase(currentDepartmentFilter)) {
                names.add(e.getFullName());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, names);
        autoCompleteEmployee.setAdapter(adapter);
        autoCompleteEmployee.setText("", false);
        selectedEmployeeId = null;
        selectedEmployeeName = null;

        autoCompleteEmployee.setOnClickListener(v -> autoCompleteEmployee.showDropDown());
        autoCompleteEmployee.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) autoCompleteEmployee.showDropDown();
        });

        autoCompleteEmployee.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            for (Employee e : employeeList) {
                if (e.getFullName().equals(selectedName)) {
                    selectedEmployeeId = e.getId();
                    selectedEmployeeName = e.getFullName();
                    break;
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

    private void onQuantityChanged(Map<MenuItem, Integer> quantities) {
        double total = 0;
        List<String> summaryParts = new ArrayList<>();

        for (Map.Entry<MenuItem, Integer> entry : quantities.entrySet()) {
            MenuItem item = entry.getKey();
            int qty = entry.getValue();
            if (qty > 0) {
                total += item.getPrice() * qty;
                summaryParts.add(item.getName() + " (x" + qty + ")");
            }
        }

        if (summaryParts.isEmpty()) {
            tvSummaryItems.setText("No items selected");
            tvSummaryTotal.setText("₱ 0.00");
        } else {
            tvSummaryItems.setText(String.join(", ", summaryParts));
            tvSummaryTotal.setText(String.format(Locale.getDefault(), "₱ %.2f", total));
        }
    }

    private void saveOrder() {
        if (selectedEmployeeId == null) {
            Toast.makeText(this, "Please select an employee", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<MenuItem, Integer> selectedQuantities = adapter.getSelectedItemsWithQuantities();
        if (selectedQuantities.isEmpty()) {
            Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalAmount = 0;
        List<String> flatItemNames = new ArrayList<>();
        List<OrderItem> detailedItems = new ArrayList<>();

        for (Map.Entry<MenuItem, Integer> entry : selectedQuantities.entrySet()) {
            MenuItem item = entry.getKey();
            int qty = entry.getValue();
            totalAmount += item.getPrice() * qty;
            
            detailedItems.add(new OrderItem(item.getName(), item.getPrice(), qty));
            
            for (int i = 0; i < qty; i++) {
                flatItemNames.add(item.getName());
            }
        }

        final double finalTotal = totalAmount;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        Order order = new Order(null, selectedEmployeeId, selectedEmployeeName, flatItemNames, totalAmount, today, currentMonth);
        order.setItems(detailedItems);

        progressBar.setVisibility(View.VISIBLE);
        db.collection("orders").add(order).addOnSuccessListener(documentReference -> {
            // Create notification
            String notifTitle = "New Order: " + selectedEmployeeName;
            String notifMessage = "Purchased items for " + String.format(Locale.getDefault(), "₱%.2f", finalTotal);
            Notification notification = new Notification(null, notifTitle, notifMessage, "ORDER");
            notification.setTimestamp(new Date()); // Explicitly set current date
            db.collection("notifications").add(notification);

            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Order saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to save order", Toast.LENGTH_SHORT).show();
        });
    }
}
