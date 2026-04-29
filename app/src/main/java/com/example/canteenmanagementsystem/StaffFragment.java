package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.EmployeeAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StaffFragment extends Fragment implements EmployeeAdapter.OnItemClickListener {

    private RecyclerView rvEmployees;
    private EmployeeAdapter adapter;
    private List<Employee> employeeList;
    private List<Employee> fullEmployeeList;
    private FirebaseFirestore db;
    private CollectionReference employeesRef;
    private ChipGroup chipGroupDepartment;
    private SwipeRefreshLayout swipeRefresh;
    private EditText etSearch;
    private String currentDepartmentFilter = null;
    private ListenerRegistration employeeListener, notificationsListener;
    private View notificationBadge;

    public StaffFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_staff, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        employeesRef = db.collection("employees");

        rvEmployees = view.findViewById(R.id.rvEmployees);
        rvEmployees.setLayoutManager(new LinearLayoutManager(requireContext()));
        employeeList = new ArrayList<>();
        fullEmployeeList = new ArrayList<>();
        adapter = new EmployeeAdapter(employeeList, this);
        rvEmployees.setAdapter(adapter);

        notificationBadge = view.findViewById(R.id.notificationBadge);
        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
            });
        }

        etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadEmployees);

        View btnAddEmployee = view.findViewById(R.id.btnAddEmployee);
        if (btnAddEmployee != null) {
            btnAddEmployee.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddEditEmployeeActivity.class);
                startActivity(intent);
            });
        }

        chipGroupDepartment = view.findViewById(R.id.chipGroupDepartment);
        chipGroupDepartment.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentDepartmentFilter = null;
            } else {
                int checkedId = checkedIds.get(0);
                Chip chip = group.findViewById(checkedId);
                if (chip != null) {
                    String department = chip.getText().toString();
                    currentDepartmentFilter = department.equals("All") ? null : department;
                }
            }
            loadEmployees();
        });

        loadEmployees();
        startNotificationsListener();
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

    private void filterList(String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
        List<Employee> filtered = new ArrayList<>();
        for (Employee e : fullEmployeeList) {
            String fullName = e.getFullName() != null ? e.getFullName().toLowerCase(Locale.getDefault()) : "";
            String employeeId = e.getEmployeeId() != null ? e.getEmployeeId().toLowerCase(Locale.getDefault()) : "";
            
            if (fullName.contains(lowerCaseQuery) || employeeId.contains(lowerCaseQuery)) {
                filtered.add(e);
            }
        }
        employeeList.clear();
        employeeList.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void loadEmployees() {
        if (!isAdded()) return;
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        
        if (employeeListener != null) {
            employeeListener.remove();
        }

        Query query = employeesRef;
        if (currentDepartmentFilter != null) {
            query = employeesRef.whereEqualTo("department", currentDepartmentFilter);
        }

        employeeListener = query.addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            if (error != null) {
                Toast.makeText(requireContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                fullEmployeeList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Employee employee = doc.toObject(Employee.class);
                    employee.setId(doc.getId());
                    fullEmployeeList.add(employee);
                }
                filterList(etSearch != null ? etSearch.getText().toString() : "");
            }
        });
    }

    @Override
    public void onItemClick(Employee employee) {
        Intent intent = new Intent(requireContext(), AddEditEmployeeActivity.class);
        intent.putExtra("EMPLOYEE_ID", employee.getId());
        startActivity(intent);
    }

    @Override
    public void onItemLongClick(Employee employee) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Employee")
                .setMessage("Are you sure you want to delete " + employee.getFullName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(requireContext())) {
                        Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    employeesRef.document(employee.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(requireContext(), "Employee deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (employeeListener != null) {
            employeeListener.remove();
        }
        if (notificationsListener != null) {
            notificationsListener.remove();
        }
    }
}
