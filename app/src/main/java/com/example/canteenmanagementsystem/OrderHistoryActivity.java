package com.example.canteenmanagementsystem;

import android.app.DatePickerDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.adapters.OrderHistoryAdapter;
import com.example.canteenmanagementsystem.models.Order;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryActivity extends AppCompatActivity {

    private RecyclerView rvOrderHistory;
    private OrderHistoryAdapter adapter;
    private List<Order> ordersList = new ArrayList<>();
    private List<Order> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextInputEditText etStartDate, etEndDate;

    private Calendar startCalendar = Calendar.getInstance();
    private Calendar endCalendar = Calendar.getInstance();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        db = FirebaseFirestore.getInstance();

        rvOrderHistory = findViewById(R.id.rvOrderHistory);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);

        rvOrderHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderHistoryAdapter(filteredList);
        rvOrderHistory.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFilter).setOnClickListener(v -> applyFilters());
        findViewById(R.id.btnExportPDF).setOnClickListener(v -> exportToPDF());

        setupDatePickers();
        loadAllOrders();
    }

    private void setupDatePickers() {
        DatePickerDialog.OnDateSetListener startListener = (view, year, month, dayOfMonth) -> {
            startCalendar.set(Calendar.YEAR, year);
            startCalendar.set(Calendar.MONTH, month);
            startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etStartDate.setText(sdf.format(startCalendar.getTime()));
        };

        DatePickerDialog.OnDateSetListener endListener = (view, year, month, dayOfMonth) -> {
            endCalendar.set(Calendar.YEAR, year);
            endCalendar.set(Calendar.MONTH, month);
            endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            etEndDate.setText(sdf.format(endCalendar.getTime()));
        };

        etStartDate.setOnClickListener(v -> new DatePickerDialog(this, startListener,
                startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)).show());

        etEndDate.setOnClickListener(v -> new DatePickerDialog(this, endListener,
                endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)).show());
        
        // Default values: last 30 days
        Calendar defaultStart = Calendar.getInstance();
        defaultStart.add(Calendar.DAY_OF_MONTH, -30);
        startCalendar = defaultStart;
        etStartDate.setText(sdf.format(startCalendar.getTime()));
        etEndDate.setText(sdf.format(endCalendar.getTime()));
    }

    private void loadAllOrders() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    ordersList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setId(doc.getId());
                        ordersList.add(order);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading orders: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        String startStr = etStartDate.getText().toString();
        String endStr = etEndDate.getText().toString();

        filteredList.clear();
        for (Order order : ordersList) {
            String orderDate = order.getDate(); // yyyy-MM-dd
            if (orderDate.compareTo(startStr) >= 0 && orderDate.compareTo(endStr) <= 0) {
                filteredList.add(order);
            }
        }

        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void exportToPDF() {
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        paint.setTextSize(18f);
        paint.setFakeBoldText(true);
        canvas.drawText("Order History Report", 20, 40, paint);
        
        paint.setTextSize(12f);
        paint.setFakeBoldText(false);
        canvas.drawText("Generated on: " + new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(new Date()), 20, 65, paint);
        canvas.drawText("Period: " + etStartDate.getText() + " to " + etEndDate.getText(), 20, 85, paint);

        // Table Header
        paint.setFakeBoldText(true);
        int y = 120;
        canvas.drawText("Date", 20, y, paint);
        canvas.drawText("Employee", 120, y, paint);
        canvas.drawText("Items", 280, y, paint);
        canvas.drawText("Amount", 500, y, paint);
        
        canvas.drawLine(20, y + 5, 575, y + 5, paint);
        
        paint.setFakeBoldText(false);
        y += 25;
        
        SimpleDateFormat displaySdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        double totalExpense = 0;

        for (Order order : filteredList) {
            if (y > 800) { // Very basic pagination check
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }
            
            canvas.drawText(order.getDate(), 20, y, paint);
            canvas.drawText(order.getEmployeeName(), 120, y, paint);
            
            String items = order.getItemsSummary();
            if (items.length() > 30) items = items.substring(0, 27) + "...";
            canvas.drawText(items, 280, y, paint);
            
            canvas.drawText(String.format(Locale.getDefault(), "₱%.2f", order.getTotalAmount()), 500, y, paint);
            
            totalExpense += order.getTotalAmount();
            y += 20;
        }
        
        y += 10;
        paint.setFakeBoldText(true);
        canvas.drawLine(20, y, 575, y, paint);
        y += 25;
        canvas.drawText("Total Expenses: " + String.format(Locale.getDefault(), "₱%.2f", totalExpense), 400, y, paint);

        document.finishPage(page);

        String fileName = "OrderHistory_" + System.currentTimeMillis() + ".pdf";
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, fileName);

        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to Downloads", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to export PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }
}