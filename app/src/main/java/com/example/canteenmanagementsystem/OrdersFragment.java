package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.adapters.OrderMenuAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.MenuItem;
import com.example.canteenmanagementsystem.models.Order;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class OrdersFragment extends Fragment {

    private AutoCompleteTextView autoCompleteEmployee;
    private OrderMenuAdapter adapter;
    private TextView tvSummaryItems, tvSummaryTotal;
    private ChipGroup chipGroupCategories;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private final List<Employee> allEmployees = new ArrayList<>();
    private final List<Employee> filteredEmployees = new ArrayList<>();
    private final List<MenuItem> menuItemsList = new ArrayList<>();
    private final List<String> employeeNames = new ArrayList<>();
    private Employee selectedEmployee;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        autoCompleteEmployee = view.findViewById(R.id.autoCompleteEmployee);
        RecyclerView rvMenuItems = view.findViewById(R.id.rvMenuItems);
        tvSummaryItems = view.findViewById(R.id.tvSummaryItems);
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal);
        MaterialButton btnSaveOrder = view.findViewById(R.id.btnSaveOrder);
        progressBar = view.findViewById(R.id.progressBar);

        rvMenuItems.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderMenuAdapter(menuItemsList, quantities -> updateSummary());
        rvMenuItems.setAdapter(adapter);

        setupCategoryFilter();

        autoCompleteEmployee.setOnItemClickListener((parent, view1, position, id) -> {
            selectedEmployee = filteredEmployees.get(position);
            updateSummary();
        });

        loadEmployees();
        loadMenuItems();

        btnSaveOrder.setOnClickListener(v -> saveOrder());

        view.findViewById(R.id.btnViewHistory).setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireContext(), OrderHistoryActivity.class));
        });

        // Fix: Hide keyboard and clear focus when touching outside
        view.findViewById(R.id.rootLayout).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View focusedView = getActivity().getCurrentFocus();
                if (focusedView != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
                    focusedView.clearFocus();
                }
            }
            return false;
        });
    }

    private void setupCategoryFilter() {
        chipGroupCategories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            Chip chip = group.findViewById(checkedId);
            if (chip != null) {
                filterEmployeesByCategory(chip.getText().toString());
            }
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

        if (getContext() != null) {
            ArrayAdapter<String> employeeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, employeeNames);
            autoCompleteEmployee.setAdapter(employeeAdapter);
        }
        autoCompleteEmployee.setText("", false);
        selectedEmployee = null;
        
        autoCompleteEmployee.setOnClickListener(v -> {
            if (!employeeNames.isEmpty()) {
                autoCompleteEmployee.showDropDown();
            }
        });
        
        updateSummary();
    }

    private void loadEmployees() {
        db.collection("employees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!isAdded()) return;
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
            if (!isAdded()) return;
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
        Map<MenuItem, Integer> quantities = adapter.getSelectedItemsWithQuantities();
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
            tvSummaryItems.setText(R.string.no_items_selected);
            tvSummaryTotal.setText(getString(R.string.label_total_amount_fmt, 0.0));
        } else {
            tvSummaryItems.setText(String.join(", ", summaryParts));
            tvSummaryTotal.setText(getString(R.string.label_total_amount_fmt, total));
        }
    }

    private void saveOrder() {
        if (getContext() == null || !NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }
        if (selectedEmployee == null) {
            Toast.makeText(getContext(), "Please select an employee", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<MenuItem, Integer> quantities = adapter.getSelectedItemsWithQuantities();
        if (quantities.isEmpty()) {
            Toast.makeText(getContext(), "Please select at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalAmount = 0;
        List<String> itemNames = new ArrayList<>();
        for (Map.Entry<MenuItem, Integer> entry : quantities.entrySet()) {
            MenuItem item = entry.getKey();
            int qty = entry.getValue();
            totalAmount += item.getPrice() * qty;
            for (int i = 0; i < qty; i++) {
                itemNames.add(item.getName());
            }
        }

        double finalTotal = totalAmount;
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Order")
                .setMessage(String.format(Locale.getDefault(), "Save order for %s — ₱%.2f?", selectedEmployee.getFullName(), finalTotal))
                .setPositiveButton("Save", (dialog, which) -> {
                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                    Date now = new Date();

                    Order order = new Order(null, selectedEmployee.getId(), selectedEmployee.getFullName(), itemNames, finalTotal, sdfDate.format(now), sdfMonth.format(now));
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    db.collection("orders").add(order).addOnSuccessListener(documentReference -> {
                        if (!isAdded()) return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Order saved", Toast.LENGTH_SHORT).show();
                        showRunningMonthlyTotal(selectedEmployee.getId(), selectedEmployee.getFullName(), sdfMonth.format(now));
                    }).addOnFailureListener(e -> {
                        if (!isAdded()) return;
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Error saving order: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    if (!isAdded()) return;
                    double monthlyTotal = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Double amount = doc.getDouble("totalAmount");
                        if (amount != null) monthlyTotal += amount;
                    }

                    new AlertDialog.Builder(requireContext())
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
        adapter.clearSelection();
        updateSummary();
    }
}
