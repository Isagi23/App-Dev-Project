package com.example.canteenmanagementsystem;

import android.os.Bundle;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
 import android.content.Intent;

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

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // Title
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(18);
        canvas.drawText("Expense Report", 20, 40, titlePaint);

        // Header Info
        paint.setTextSize(12);
        canvas.drawText("Employee: " + employeeName, 20, 70, paint);
        canvas.drawText("Month: " + monthName, 20, 90, paint);
        canvas.drawText("Date Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Calendar.getInstance().getTime()), 20, 110, paint);

        // Table Header
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Date", 20, 150, paint);
        canvas.drawText("Items", 120, 150, paint);
        canvas.drawText("Amount", 480, 150, paint);
        canvas.drawLine(20, 155, 575, 155, paint);

        // Table Content
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        int y = 180;
        double total = 0;
        for (Order order : ordersList) {
            if (y > 780) { // Check for page end
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }
            canvas.drawText(order.getDate(), 20, y, paint);
            
            // Handle long item strings
            String items = order.getItemsSummary();
            if (items.length() > 50) items = items.substring(0, 47) + "...";
            canvas.drawText(items, 120, y, paint);
            
            canvas.drawText(String.format("₱%.2f", order.getTotalAmount()), 480, y, paint);
            total += order.getTotalAmount();
            y += 25;
        }

        // Footer Summary
        canvas.drawLine(20, y, 575, y, paint);
        y += 30;
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("Total Orders: " + ordersList.size(), 20, y, paint);
        canvas.drawText(String.format("Total Amount: ₱%.2f", total), 400, y, paint);

        pdfDocument.finishPage(page);

        // Save PDF to cache and share
        try {
            File cachePath = new File(getCacheDir(), "reports");
            cachePath.mkdirs();
            File file = new File(cachePath, "Expense_Report_" + employeeName.replace(" ", "_") + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

            // Share PDF
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Report"));

        } catch (Exception e) {
            Toast.makeText(this, "Error generating PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            pdfDocument.close();
        }
    }
}