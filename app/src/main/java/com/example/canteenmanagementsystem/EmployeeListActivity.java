package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.EmployeeAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class EmployeeListActivity extends AppCompatActivity implements EmployeeAdapter.OnItemClickListener {

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList;
    private List<Employee> filteredList;
    private FirebaseFirestore db;
    private CollectionReference employeesRef;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmployeeCountHeader, tvTotalEmployeesCount;
    private String currentDepartment = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_list);

        db = FirebaseFirestore.getInstance();
        employeesRef = db.collection("employees");

        rvEmployees = findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(this));
        employeeList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new EmployeeAdapter(filteredList, this);
        rvEmployees.setAdapter(adapter);

        tvEmployeeCountHeader = findViewById(R.id.tvEmployeeCountHeader);
        tvTotalEmployeesCount = findViewById(R.id.tvTotalEmployeesCount);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadEmployees);

        ChipGroup chipGroup = findViewById(R.id.chipGroupDepartment);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                Chip chip = findViewById(checkedIds.get(0));
                currentDepartment = chip.getText().toString().trim();
            } else {
                currentDepartment = "All";
            }
            filterList();
        });

        findViewById(R.id.btnAddEmployeeHeader).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditEmployeeActivity.class));
        });

        loadEmployees();
    }

    private void filterList() {
        filteredList.clear();
        for (Employee e : employeeList) {
            if (currentDepartment.equals("All") || (e.getDepartment() != null && e.getDepartment().equalsIgnoreCase(currentDepartment))) {
                filteredList.add(e);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmployeeCountHeader.setText(filteredList.size() + " employees");
    }

    private void loadEmployees() {
        swipeRefresh.setRefreshing(true);
        employeesRef.addSnapshotListener((value, error) -> {
            swipeRefresh.setRefreshing(false);
            if (error != null) {
                Toast.makeText(this, "Error loading employees: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                employeeList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Employee employee = doc.toObject(Employee.class);
                    employee.setId(doc.getId());
                    employeeList.add(employee);
                }
                filterList();
                tvTotalEmployeesCount.setText(String.valueOf(employeeList.size()));
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
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Employee")
                .setMessage("Are you sure you want to delete " + employee.getFullName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    employeesRef.document(employee.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Employee deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
