package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.PayrollAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.Order;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PayrollReportActivity extends AppCompatActivity {

    private TextView tvSelectedMonth, tvTotalEmployees, tvGrandTotalDeductions;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPayrollReport;
    private PayrollAdapter adapter;
    private List<Employee> allEmployees = new ArrayList<>();
    private List<Employee> filteredEmployees = new ArrayList<>();
    private Map<String, Double> deductionsMap = new HashMap<>();
    private String selectedDepartment = "All";
    private BottomNavigationView bottomNavigationView;

    private FirebaseFirestore db;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payroll_report);

        db = FirebaseFirestore.getInstance();

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        tvGrandTotalDeductions = findViewById(R.id.tvGrandTotalDeductions);
        rvPayrollReport = findViewById(R.id.rvPayrollReport);
        ChipGroup chipGroup = findViewById(R.id.chipGroupDepartment);
        ImageButton btnPrev = findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = findViewById(R.id.btnNextMonth);
        Button btnExport = findViewById(R.id.btnExportPdf);

        // Removed back button listener since it was removed from layout
        
        rvPayrollReport.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PayrollAdapter(filteredEmployees, deductionsMap);
        rvPayrollReport.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadData);

        updateMonthDisplay();
        setupBottomNavigation();
        loadData();

        btnPrev.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadDeductions();
        });

        btnNext.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadDeductions();
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                selectedDepartment = "All";
            } else {
                Chip chip = findViewById(checkedIds.get(0));
                selectedDepartment = chip.getText().toString();
            }
            filterList();
        });

        btnExport.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                } else {
                    exportToPdf();
                }
            } else {
                exportToPdf();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportToPdf();
            } else {
                Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_report);
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
            if (itemId == R.id.nav_orders) {
                startActivity(new Intent(this, RecordOrderActivity.class));
                return true;
            }
            if (itemId == R.id.nav_report) return true;
            return false;
        });
    }

    private void updateMonthDisplay() {
        tvSelectedMonth.setText(monthFormat.format(calendar.getTime()));
    }

    private void loadData() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        db.collection("employees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allEmployees.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Employee employee = doc.toObject(Employee.class);
                employee.setId(doc.getId());
                allEmployees.add(employee);
            }
            loadDeductions();
        }).addOnFailureListener(e -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "Error loading employees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDeductions() {
        String monthStr = dbMonthFormat.format(calendar.getTime());
        db.collection("orders")
                .whereEqualTo("month", monthStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    deductionsMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        String empId = order.getEmployeeId();
                        double amount = order.getTotalAmount();
                        deductionsMap.put(empId, deductionsMap.getOrDefault(empId, 0.0) + amount);
                    }
                    filterList();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }).addOnFailureListener(e -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading deductions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterList() {
        filteredEmployees.clear();
        double grandTotal = 0;
        for (Employee e : allEmployees) {
            if (selectedDepartment.equals("All") || (e.getDepartment() != null && e.getDepartment().equals(selectedDepartment))) {
                filteredEmployees.add(e);
                grandTotal += deductionsMap.getOrDefault(e.getId(), 0.0);
            }
        }
        adapter.notifyDataSetChanged();
        tvTotalEmployees.setText("Total Employees: " + filteredEmployees.size());
        tvGrandTotalDeductions.setText(String.format(Locale.getDefault(), "₱%.2f", grandTotal));
    }

    private void exportToPdf() {
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        int pageWidth = 595; 
        int pageHeight = 842;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTextSize(18f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Canteen Expense Report", 50, 50, titlePaint);
        titlePaint.setTextSize(14f);
        canvas.drawText("Period: " + tvSelectedMonth.getText(), 50, 75, titlePaint);

        paint.setTextSize(10f);
        int y = 110;
        canvas.drawText("Name", 50, y, paint);
        canvas.drawText("Dept", 200, y, paint);
        canvas.drawText("Salary", 300, y, paint);
        canvas.drawText("Deduction", 400, y, paint);
        canvas.drawText("Remaining", 500, y, paint);
        y += 15;
        canvas.drawLine(50, y-10, 550, y-10, paint);

        double totalDeductions = 0;
        for (Employee e : filteredEmployees) {
            if (y > pageHeight - 100) {
                pdfDocument.finishPage(page);
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }
            double ded = deductionsMap.getOrDefault(e.getId(), 0.0);
            double rem = e.getSalary() - ded;
            totalDeductions += ded;

            canvas.drawText(e.getFullName(), 50, y, paint);
            canvas.drawText(e.getDepartment() != null ? e.getDepartment() : "N/A", 200, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", e.getSalary()), 300, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", ded), 400, y, paint);
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", rem), 500, y, paint);
            y += 20;
        }

        y += 10;
        canvas.drawLine(50, y-10, 550, y-10, paint);
        titlePaint.setTextSize(12f);
        canvas.drawText("Grand Total Expenses:", 300, y+10, titlePaint);
        canvas.drawText(String.format(Locale.getDefault(), "₱%.2f", totalDeductions), 500, y+10, titlePaint);

        pdfDocument.finishPage(page);

        String fileName = "Payroll_Report_" + dbMonthFormat.format(calendar.getTime()) + ".pdf";
        File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Generated Successfully", Toast.LENGTH_SHORT).show();
            sharePdf(file);
        } catch (IOException e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }

    private void sharePdf(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Share Report"));
    }
}
