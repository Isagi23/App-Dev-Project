package com.example.canteenmanagementsystem;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.PayrollAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.Order;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PayrollReportActivity extends AppCompatActivity {

    private RecyclerView rvPayroll;
    private PayrollAdapter adapter;
    private List<Employee> employeeList = new ArrayList<>();
    private Map<String, Double> deductionsMap = new HashMap<>();
    private FirebaseFirestore db;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvSelectedMonth, tvGrandTotalDeductions, tvTotalEmployees;
    private Calendar currentCalendar;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payroll_report);

        db = FirebaseFirestore.getInstance();
        currentCalendar = Calendar.getInstance();

        rvPayroll = findViewById(R.id.rvPayrollReport);
        rvPayroll.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PayrollAdapter(employeeList, deductionsMap);
        rvPayroll.setAdapter(adapter);

        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvGrandTotalDeductions = findViewById(R.id.tvGrandTotalDeductions);
        tvTotalEmployees = findViewById(R.id.tvTotalEmployees);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        swipeRefresh.setOnRefreshListener(this::loadData);

        findViewById(R.id.btnPrevMonth).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadData();
        });

        findViewById(R.id.btnNextMonth).setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadData();
        });

        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportToPdf());

        updateMonthDisplay();
        loadData();
    }

    private void exportToPdf() {
        if (employeeList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            PdfDocument document = new PdfDocument();
            // A4 size: 595 x 842 points
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();

            int x = 40;
            int y = 50;

            // Header
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextSize(20);
            titlePaint.setColor(Color.parseColor("#0F172A")); // navy_dark
            canvas.drawText("Canteen Management System", x, y, titlePaint);
            
            y += 25;
            paint.setTextSize(14);
            paint.setColor(Color.GRAY);
            String monthStr = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendar.getTime());
            canvas.drawText("Payroll Report - " + monthStr, x, y, paint);

            y += 20;
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime());
            canvas.drawText("Generated on: " + timestamp, x, y, paint);

            // Table Header
            y += 40;
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setTextSize(12);
            headerPaint.setColor(Color.BLACK);
            
            canvas.drawText("Employee Name", x, y, headerPaint);
            canvas.drawText("Department", x + 200, y, headerPaint);
            canvas.drawText("Deductions", x + 350, y, headerPaint);

            y += 10;
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;

            // Table Content
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setColor(Color.BLACK);
            paint.setTextSize(11);

            double grandTotal = 0;
            for (Employee emp : employeeList) {
                double deduction = deductionsMap.getOrDefault(emp.getId(), 0.0);
                grandTotal += deduction;

                // Check for page overflow
                if (y > 780) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }

                canvas.drawText(emp.getFullName(), x, y, paint);
                canvas.drawText(emp.getDepartment(), x + 200, y, paint);
                canvas.drawText(String.format(Locale.getDefault(), "P %.2f", deduction), x + 350, y, paint);
                
                y += 20;
            }

            // Summary
            y += 20;
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;
            headerPaint.setTextSize(14);
            canvas.drawText("Total Employees: " + employeeList.size(), x, y, headerPaint);
            y += 25;
            canvas.drawText("Grand Total Deductions: " + String.format(Locale.getDefault(), "P %.2f", grandTotal), x, y, headerPaint);

            // Footer
            paint.setTextSize(10);
            paint.setColor(Color.LTGRAY);
            canvas.drawText("End of Report", 260, 820, paint);

            document.finishPage(page);

            String fileName = "Canteen_Report_" + new SimpleDateFormat("MMMM_yyyy", Locale.getDefault()).format(currentCalendar.getTime()) + ".pdf";
            
            try {
                savePdfToDownloads(document, fileName);
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                document.close();
            }
        });
    }

    private void savePdfToDownloads(PdfDocument document, String fileName) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                    if (outputStream == null) throw new IOException("Failed to open output stream");
                    document.writeTo(outputStream);
                }

                contentValues.clear();
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContentResolver().update(uri, contentValues, null, null);

                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                    showOpenOption(uri);
                });
            } else {
                throw new IOException("Failed to create MediaStore entry");
            }
        } else {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                throw new IOException("Failed to create Downloads directory");
            }
            File file = new File(downloadsDir, fileName);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                document.writeTo(outputStream);
            }

            File finalFile = file;
            MediaScannerConnection.scanFile(this, new String[]{finalFile.getAbsolutePath()}, null, (path, uri) -> {
                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                    if (uri != null) {
                        showOpenOption(uri);
                    } else {
                        Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", finalFile);
                        showOpenOption(contentUri);
                    }
                });
            });
        }
    }

    private void showOpenOption(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("PDF Generated")
                .setMessage("Your report has been saved to the Downloads folder.")
                .setPositiveButton("Open", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("Share", (dialog, which) -> {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Report"));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void updateMonthDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvSelectedMonth.setText(sdf.format(currentCalendar.getTime()));
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);
        db.collection("employees").get().addOnSuccessListener(employeeDocs -> {
            employeeList.clear();
            for (QueryDocumentSnapshot doc : employeeDocs) {
                Employee employee = doc.toObject(Employee.class);
                employee.setId(doc.getId());
                employeeList.add(employee);
            }
            loadOrders();
        }).addOnFailureListener(e -> {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "Error loading employees", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadOrders() {
        String monthQuery = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.getTime());
        db.collection("orders")
                .whereEqualTo("month", monthQuery)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    swipeRefresh.setRefreshing(false);
                    deductionsMap.clear();
                    double grandTotal = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        String employeeId = order.getEmployeeId();
                        double amount = order.getTotalAmount();

                        deductionsMap.put(employeeId, deductionsMap.getOrDefault(employeeId, 0.0) + amount);
                        grandTotal += amount;
                    }

                    adapter.notifyDataSetChanged();
                    tvGrandTotalDeductions.setText(String.format(Locale.getDefault(), "₱ %.2f", grandTotal));
                    tvTotalEmployees.setText(employeeList.size() + (employeeList.size() == 1 ? " employee" : " employees"));
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error loading orders", Toast.LENGTH_SHORT).show();
                });
    }
}
