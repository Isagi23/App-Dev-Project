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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.adapters.OrderHistoryAdapter;
import com.example.canteenmanagementsystem.models.Order;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmployeeOrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrderHistory;
    private OrderHistoryAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvEmptyState, tvEmployeeName, tvTotalSpent, tvTotalOrders;
    
    private String employeeId;
    private String employeeName;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_employee_order_history);

        employeeId = getIntent().getStringExtra("employeeId");
        employeeName = getIntent().getStringExtra("employeeName");

        if (employeeId == null) {
            Toast.makeText(this, "Error: Employee data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvEmployeeName = findViewById(R.id.tvEmployeeName);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvTotalOrders = findViewById(R.id.tvTotalOrders);

        tvEmployeeName.setText(employeeName);

        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(ordersList);
        rvOrderHistory.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnExportPDF).setOnClickListener(v -> exportToPDF());

        loadEmployeeOrders();
    }

    private void loadEmployeeOrders() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("orders")
                .whereEqualTo("employeeId", employeeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    ordersList.clear();
                    double totalAmount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setId(doc.getId());
                        ordersList.add(order);
                        totalAmount += order.getTotalAmount();
                    }

                    // Sort locally to avoid requiring composite index
                    ordersList.sort((o1, o2) -> {
                        if (o1.getTimestamp() == null || o2.getTimestamp() == null) {
                            if (o1.getDate() != null && o2.getDate() != null) {
                                return o2.getDate().compareTo(o1.getDate());
                            }
                            return 0;
                        }
                        return o2.getTimestamp().compareTo(o1.getTimestamp());
                    });
                    
                    adapter.notifyDataSetChanged();
                    tvEmptyState.setVisibility(ordersList.isEmpty() ? View.VISIBLE : View.GONE);
                    
                    tvTotalSpent.setText(String.format(Locale.getDefault(), "₱%.2f", totalAmount));
                    tvTotalOrders.setText(String.valueOf(ordersList.size()));
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void exportToPDF() {
        if (ordersList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            PdfDocument document = new PdfDocument();
            int pageWidth = 595;
            int pageHeight = 842;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();

            int x = 40;
            int y = 50;

            titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            titlePaint.setTextSize(20);
            titlePaint.setColor(Color.parseColor("#191C1D"));
            canvas.drawText("Canteen Management System", x, y, titlePaint);

            y += 25;
            paint.setTextSize(14);
            paint.setColor(Color.GRAY);
            canvas.drawText("Individual Order History: " + employeeName, x, y, paint);

            y += 20;
            String timestamp = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());
            canvas.drawText("Generated on: " + timestamp, x, y, paint);

            // Table Header
            y += 40;
            headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            headerPaint.setTextSize(11);
            headerPaint.setColor(Color.BLACK);

            canvas.drawText("DATE/TIME", x, y, headerPaint);
            canvas.drawText("ITEMS PURCHASED", x + 150, y, headerPaint);
            canvas.drawText("AMOUNT", x + 460, y, headerPaint);

            y += 10;
            paint.setColor(Color.parseColor("#E1E3E4"));
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;

            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setColor(Color.BLACK);
            paint.setTextSize(10f);

            SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            double totalExpense = 0;

            for (Order order : ordersList) {
                if (y > 780) {
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                    
                    headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("DATE/TIME", x, y, headerPaint);
                    canvas.drawText("ITEMS PURCHASED", x + 150, y, headerPaint);
                    canvas.drawText("AMOUNT", x + 460, y, headerPaint);
                    y += 10;
                    canvas.drawLine(x, y, 555, y, paint);
                    y += 25;
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                }

                String dateStr = order.getTimestamp() != null ? displaySdf.format(order.getTimestamp()) : order.getDate();
                canvas.drawText(dateStr, x, y, paint);
                
                String items = order.getDetailedItemsSummary();
                if (items != null && items.length() > 50) items = items.substring(0, 47) + "...";
                canvas.drawText(items != null ? items : "None", x + 150, y, paint);

                String amountStr = String.format(Locale.getDefault(), "₱%.2f", order.getTotalAmount());
                canvas.drawText(amountStr, x + 460, y, paint);

                totalExpense += order.getTotalAmount();
                y += 22;
            }

            y += 10;
            paint.setColor(Color.parseColor("#E1E3E4"));
            canvas.drawLine(x, y, 555, y, paint);
            y += 30;
            paint.setColor(Color.parseColor("#FF5722"));
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            paint.setTextSize(14f);
            String totalStr = "TOTAL EXPENSES: " + String.format(Locale.getDefault(), "₱%.2f", totalExpense);
            canvas.drawText(totalStr, 555 - paint.measureText(totalStr), y, paint);

            document.finishPage(page);

            String fileName = "History_" + employeeName.replace(" ", "_") + "_" + System.currentTimeMillis() + ".pdf";
            try {
                savePdfToDownloads(document, fileName);
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
            File file = new File(downloadsDir, fileName);
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                document.writeTo(outputStream);
            }

            MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, (path, uri) -> runOnUiThread(() -> {
                if (uri != null) showOpenOption(uri);
            }));
        }
    }

    private void showOpenOption(Uri uri) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("PDF Exported")
                .setMessage(getString(R.string.msg_history_exported, employeeName))
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
                .setNegativeButton("Done", null)
                .show();
    }
}
