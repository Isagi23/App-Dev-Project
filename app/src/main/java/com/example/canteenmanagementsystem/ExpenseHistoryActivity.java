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
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.OrderHistoryAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.Order;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExpenseHistoryActivity extends AppCompatActivity {

    private Spinner spinnerEmployees;
    private TextView tvSelectedMonth, tvTotalOrders, tvTotalAmount, tvEmptyState;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvExpenseHistory;
    private OrderHistoryAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private List<Employee> employeeList = new ArrayList<>();
    private List<String> employeeNames = new ArrayList<>();
    
    private FirebaseFirestore db;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_history);

        db = FirebaseFirestore.getInstance();

        spinnerEmployees = findViewById(R.id.spinnerEmployees);
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);
        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvExpenseHistory = findViewById(R.id.rvExpenseHistory);
        ImageButton btnPrev = findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = findViewById(R.id.btnNextMonth);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvExpenseHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(ordersList, this::showDeleteConfirmation);
        rvExpenseHistory.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadExpenseHistory);

        updateMonthDisplay();
        loadEmployees();

        btnPrev.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, -1);
            updateMonthDisplay();
            loadExpenseHistory();
        });

        btnNext.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);
            updateMonthDisplay();
            loadExpenseHistory();
        });

        findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportToPdf());

        spinnerEmployees.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                loadExpenseHistory();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void showDeleteConfirmation(Order order) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Order")
                .setMessage("Are you sure you want to delete this order?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (order.getId() != null) {
                        db.collection("orders").document(order.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show();
                                    loadExpenseHistory();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMonthDisplay() {
        tvSelectedMonth.setText(monthFormat.format(calendar.getTime()));
    }

    private void loadEmployees() {
        db.collection("employees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            employeeList.clear();
            employeeNames.clear();
            employeeNames.add("Select Employee");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Employee employee = doc.toObject(Employee.class);
                employee.setId(doc.getId());
                employeeList.add(employee);
                employeeNames.add(employee.getFullName());
            }
            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, employeeNames);
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerEmployees.setAdapter(spinnerAdapter);
        });
    }

    private void loadExpenseHistory() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        int selection = spinnerEmployees.getSelectedItemPosition();
        if (selection <= 0) {
            ordersList.clear();
            adapter.notifyDataSetChanged();
            tvTotalOrders.setText("Total Orders: 0");
            tvTotalAmount.setText("Total Amount: ₱0.00");
            tvEmptyState.setVisibility(android.view.View.VISIBLE);
            tvEmptyState.setText("Select an employee to view history");
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }

        Employee employee = employeeList.get(selection - 1);
        String monthStr = dbMonthFormat.format(calendar.getTime());

        db.collection("orders")
                .whereEqualTo("employeeId", employee.getId())
                .whereEqualTo("month", monthStr)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    ordersList.clear();
                    double totalAmount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setId(doc.getId()); // Ensure ID is set
                        ordersList.add(order);
                        totalAmount += order.getTotalAmount();
                    }
                    adapter.notifyDataSetChanged();
                    tvTotalOrders.setText("Total Orders: " + ordersList.size());
                    tvTotalAmount.setText(String.format("Total Amount: ₱%.2f", totalAmount));

                    if (ordersList.isEmpty()) {
                        tvEmptyState.setVisibility(android.view.View.VISIBLE);
                        tvEmptyState.setText("No orders found for this month");
                    } else {
                        tvEmptyState.setVisibility(android.view.View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void exportToPdf() {
        if (ordersList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        int selection = spinnerEmployees.getSelectedItemPosition();
        String employeeName = (selection > 0) ? employeeNames.get(selection) : "All";
        String monthName = tvSelectedMonth.getText().toString();

        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            int x = 40;
            int y = 50;

            // Title
            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextSize(20);
            titlePaint.setColor(Color.parseColor("#0F172A")); // navy_dark
            canvas.drawText("Canteen Management System", x, y, titlePaint);

            y += 25;
            paint.setTextSize(14);
            paint.setColor(Color.GRAY);
            canvas.drawText("Expense Report - " + monthName, x, y, paint);

            y += 20;
            canvas.drawText("Employee: " + employeeName, x, y, paint);

            y += 20;
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Calendar.getInstance().getTime());
            canvas.drawText("Generated on: " + timestamp, x, y, paint);

            // Table Header
            y += 40;
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setTextSize(12);
            headerPaint.setColor(Color.BLACK);

            canvas.drawText("Date", x, y, headerPaint);
            canvas.drawText("Items", x + 100, y, headerPaint);
            canvas.drawText("Amount", x + 440, y, headerPaint);

            y += 10;
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;

            // Table Content
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setColor(Color.BLACK);
            paint.setTextSize(11);

            double total = 0;
            for (Order order : ordersList) {
                if (y > 780) { // Check for page end
                    pdfDocument.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }
                canvas.drawText(order.getDate(), x, y, paint);

                // Handle long item strings
                String items = order.getItemsSummary();
                if (items.length() > 55) items = items.substring(0, 52) + "...";
                canvas.drawText(items, x + 100, y, paint);

                canvas.drawText(String.format("₱%.2f", order.getTotalAmount()), x + 440, y, paint);
                total += order.getTotalAmount();
                y += 20;
            }

            // Footer Summary
            y += 20;
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;
            headerPaint.setTextSize(14);
            canvas.drawText("Total Orders: " + ordersList.size(), x, y, headerPaint);
            y += 25;
            canvas.drawText(String.format("Total Amount: ₱%.2f", total), x, y, headerPaint);

            // Footer
            paint.setTextSize(10);
            paint.setColor(Color.LTGRAY);
            canvas.drawText("End of Report", 260, 820, paint);

            pdfDocument.finishPage(page);

            String fileName = "Expense_Report_" + employeeName.replace(" ", "_") + "_" + dbMonthFormat.format(calendar.getTime()) + ".pdf";

            try {
                savePdfToDownloads(pdfDocument, fileName);
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                pdfDocument.close();
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
}