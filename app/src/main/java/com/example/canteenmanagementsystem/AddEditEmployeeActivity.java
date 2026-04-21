package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.canteenmanagementsystem.models.Employee;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AddEditEmployeeActivity extends AppCompatActivity {

    private TextInputEditText etEmployeeId, etFullName, etPosition, etSalary;
    private Spinner spinnerDepartment;
    private Button btnSave;
    private android.widget.ProgressBar progressBar;
    private FirebaseFirestore db;
    private CollectionReference employeesRef;
    private String documentId;
    private final String[] departments = {"Field", "Packing", "Maintenance", "Harvest"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_employee);

        db = FirebaseFirestore.getInstance();
        employeesRef = db.collection("employees");

        etEmployeeId = findViewById(R.id.etEmployeeId);
        etFullName = findViewById(R.id.etFullName);
        etPosition = findViewById(R.id.etPosition);
        etSalary = findViewById(R.id.etSalary);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        btnSave = findViewById(R.id.btnSaveEmployee);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, departments);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(adapter);

        documentId = getIntent().getStringExtra("EMPLOYEE_ID");
        if (documentId != null) {
            loadEmployeeDetails();
        }

        btnSave.setOnClickListener(v -> saveEmployee());
    }

    private void loadEmployeeDetails() {
        employeesRef.document(documentId).get().addOnSuccessListener(documentSnapshot -> {
            Employee employee = documentSnapshot.toObject(Employee.class);
            if (employee != null) {
                etEmployeeId.setText(employee.getEmployeeId());
                etFullName.setText(employee.getFullName());
                etPosition.setText(employee.getPosition());
                etSalary.setText(String.valueOf(employee.getSalary()));
                for (int i = 0; i < departments.length; i++) {
                    if (departments[i].equals(employee.getDepartment())) {
                        spinnerDepartment.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    private void saveEmployee() {
        if (!com.example.canteenmanagementsystem.utils.NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        String empId = etEmployeeId.getText().toString().trim();
        String name = etFullName.getText().toString().trim();
        String pos = etPosition.getText().toString().trim();
        String salaryStr = etSalary.getText().toString().trim();
        String dept = spinnerDepartment.getSelectedItem().toString();

        boolean hasError = false;

        if (TextUtils.isEmpty(empId)) {
            etEmployeeId.setError("Employee ID is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Full name is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(pos)) {
            etPosition.setError("Position is required");
            hasError = true;
        }
        if (TextUtils.isEmpty(salaryStr)) {
            etSalary.setError("Salary is required");
            hasError = true;
        }

        if (hasError) return;

        double salary;
        try {
            salary = Double.parseDouble(salaryStr);
            if (salary <= 0) {
                etSalary.setError("Salary must be greater than zero");
                return;
            }
        } catch (NumberFormatException e) {
            etSalary.setError("Invalid salary format");
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");
        if (progressBar != null) progressBar.setVisibility(android.view.View.VISIBLE);

        // Check for unique Employee ID (excluding current one if editing)
        employeesRef.whereEqualTo("employeeId", empId).get().addOnSuccessListener(queryDocumentSnapshots -> {
            boolean isDuplicate = false;
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                if (documentId == null || !doc.getId().equals(documentId)) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                etEmployeeId.setError("Employee ID already exists");
                btnSave.setEnabled(true);
                btnSave.setText("Save Employee");
                if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
            } else {
                Employee employee = new Employee(documentId, empId, name, dept, pos, salary);
                if (documentId == null) {
                    employeesRef.add(employee).addOnSuccessListener(documentReference -> {
                        // Set the ID field to the document ID
                        String newId = documentReference.getId();
                        documentReference.update("id", newId);
                        if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "Employee added", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "Error adding employee: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Employee");
                    });
                } else {
                    employeesRef.document(documentId).set(employee).addOnSuccessListener(aVoid -> {
                        if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "Employee updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }).addOnFailureListener(e -> {
                        if (progressBar != null) progressBar.setVisibility(android.view.View.GONE);
                        Toast.makeText(this, "Error updating employee: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Employee");
                    });
                }
            }
        });
    }
}