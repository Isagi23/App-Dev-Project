package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.EmployeeAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EmployeeListActivity extends AppCompatActivity implements EmployeeAdapter.OnItemClickListener {

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList;
    private List<Employee> fullEmployeeList;
    private FirebaseFirestore db;
    private CollectionReference employeesRef;
    private ChipGroup chipGroupDepartment;
    private BottomNavigationView bottomNavigationView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmployeeCountHeader, tvTotalEmployeesCount;
    private String currentDepartmentFilter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_list);

        db = FirebaseFirestore.getInstance();
        employeesRef = db.collection("employees");

        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        employeeList = new ArrayList<>();
        fullEmployeeList = new ArrayList<>();
        adapter = new EmployeeAdapter(employeeList, this);
        rvEmployees.setAdapter(adapter);

        tvEmployeeCountHeader = findViewById(R.id.tvEmployeeCountHeader);
        tvTotalEmployeesCount = findViewById(R.id.tvTotalEmployeesCount);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadEmployees);

        findViewById(R.id.btnAddEmployeeHeader).setOnClickListener(v -> {
            Intent intent = new Intent(this, AddEditEmployeeActivity.class);
            startActivity(intent);
        });

        chipGroupDepartment = findViewById(R.id.chipGroupDepartment);
        chipGroupDepartment.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentDepartmentFilter = null;
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                String department = chip.getText().toString();
                currentDepartmentFilter = department.equals("All") ? null : department;
            }
            loadEmployees();
        });

        setupBottomNavigation();
        loadEmployees();
    }

    private void filterList(String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
        List<Employee> filtered = new ArrayList<>();
        for (Employee e : fullEmployeeList) {
            if (e.getFullName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                (e.getEmployeeId() != null && e.getEmployeeId().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery))) {
                filtered.add(e);
            }
        }
        employeeList.clear();
        employeeList.addAll(filtered);
        adapter.notifyDataSetChanged();
        updateCounters();
    }

    private void updateCounters() {
        int count = employeeList.size();
        String countStr = count + (count == 1 ? " employee" : " employees");
        if (tvEmployeeCountHeader != null) tvEmployeeCountHeader.setText(countStr);
        if (tvTotalEmployeesCount != null) tvTotalEmployeesCount.setText(String.valueOf(count));
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_staff);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                return true;
            }
            if (itemId == R.id.nav_staff) return true;
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
    }

    private void loadEmployees() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        Query query = employeesRef;
        if (currentDepartmentFilter != null) {
            query = employeesRef.whereEqualTo("department", currentDepartmentFilter);
        }

        query.addSnapshotListener((value, error) -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            if (error != null) {
                Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                fullEmployeeList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Employee employee = doc.toObject(Employee.class);
                    employee.setId(doc.getId());
                    fullEmployeeList.add(employee);
                }
                filterList("");
            }
        });
    }


    @Override
    public void onItemClick(Employee employee) {
        Intent intent = new Intent(this, AddEditEmployeeActivity.class);
        intent.putExtra("EMPLOYEE_ID", employee.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(Employee employee) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Employee")
                .setMessage("Are you sure you want to delete " + employee.getFullName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    employeesRef.document(employee.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Employee deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
