package com.example.canteenmanagementsystem;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.PayrollAdapter;
import com.example.canteenmanagementsystem.models.Employee;
import com.example.canteenmanagementsystem.models.Order;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class ReportsFragment extends Fragment {

    private TextView tvSelectedMonth, tvTotalEmployees, tvGrandTotalDeductions;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvPayrollReport;
    private BarChart barChart;
    private PayrollAdapter adapter;
    private List<Employee> allEmployees = new ArrayList<>();
    private List<Employee> filteredEmployees = new ArrayList<>();
    private Map<String, Double> deductionsMap = new HashMap<>();
    private Map<String, Integer> itemSalesMap = new HashMap<>();
    private String selectedDepartment = "All";

    private FirebaseFirestore db;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat dbMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private static final int STORAGE_PERMISSION_CODE = 101;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reports, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        tvSelectedMonth = view.findViewById(R.id.tvSelectedMonth);
        tvTotalEmployees = view.findViewById(R.id.tvTotalEmployees);
        tvGrandTotalDeductions = view.findViewById(R.id.tvGrandTotalDeductions);
        rvPayrollReport = view.findViewById(R.id.rvPayrollReport);
        barChart = view.findViewById(R.id.barChart);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupDepartment);
        ImageButton btnPrev = view.findViewById(R.id.btnPrevMonth);
        ImageButton btnNext = view.findViewById(R.id.btnNextMonth);
        Button btnExport = view.findViewById(R.id.btnExportPdf);

        rvPayrollReport.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PayrollAdapter(filteredEmployees, deductionsMap);
        rvPayrollReport.setAdapter(adapter);

        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadData);

        updateMonthDisplay();
        setupChart();
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
                Chip chip = view.findViewById(checkedIds.get(0));
                selectedDepartment = chip.getText().toString();
            }
            filterList();
        });

        btnExport.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                } else {
                    exportToPdf();
                }
            } else {
                exportToPdf();
            }
        });
    }

    private void updateMonthDisplay() {
        tvSelectedMonth.setText(monthFormat.format(calendar.getTime()));
    }

    private void setupChart() {
        if (barChart == null) return;
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(60);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setLabelCount(5);

        barChart.getAxisLeft().setDrawGridLines(true);
        barChart.getAxisLeft().setGridColor(Color.LTGRAY);
        barChart.getAxisLeft().setTextColor(Color.GRAY);
        barChart.getAxisRight().setEnabled(false);
    }

    private void loadData() {
        if (getContext() == null || !NetworkUtils.isNetworkAvailable(getContext())) {
            Toast.makeText(getContext(), "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        db.collection("employees").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!isAdded()) return;
            allEmployees.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Employee employee = doc.toObject(Employee.class);
                employee.setId(doc.getId());
                allEmployees.add(employee);
            }
            loadDeductions();
        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            Toast.makeText(getContext(), "Error loading employees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void loadDeductions() {
        String monthStr = dbMonthFormat.format(calendar.getTime());
        db.collection("orders")
                .whereEqualTo("month", monthStr)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    deductionsMap.clear();
                    itemSalesMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        String empId = order.getEmployeeId();
                        double amount = order.getTotalAmount();
                        deductionsMap.put(empId, deductionsMap.getOrDefault(empId, 0.0) + amount);

                        // Collect item sales data
                        if (order.getItemNames() != null) {
                            for (String itemName : order.getItemNames()) {
                                itemSalesMap.put(itemName, itemSalesMap.getOrDefault(itemName, 0) + 1);
                            }
                        }
                    }
                    filterList();
                    updateChart();
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                }).addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    Toast.makeText(getContext(), "Error loading deductions: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChart() {
        if (barChart == null) return;

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(itemSalesMap.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        int count = 0;
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            if (count >= 7) break; // Top 7 items
            entries.add(new BarEntry(count, entry.getValue()));
            labels.add(entry.getKey());
            count++;
        }

        if (entries.isEmpty()) {
            barChart.clear();
            barChart.setNoDataText("No sales data for this period");
            barChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.title_top_selling_items));
        if (getContext() != null) {
            dataSet.setColor(ContextCompat.getColor(getContext(), R.color.primary_orange));
        } else {
            dataSet.setColor(Color.parseColor("#FF5722"));
        }
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        barChart.setData(data);
        barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        barChart.animateY(800);
        barChart.invalidate();
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
        tvTotalEmployees.setText(getString(R.string.report_total_employees, filteredEmployees.size()));
        tvGrandTotalDeductions.setText(String.format(Locale.getDefault(), "₱%.2f", grandTotal));
    }

    private void exportToPdf() {
        if (filteredEmployees.isEmpty()) {
            Toast.makeText(getContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Generating PDF...", Toast.LENGTH_SHORT).show();
        executorService.execute(() -> {
            PdfDocument pdfDocument = new PdfDocument();
            Paint paint = new Paint();
            Paint titlePaint = new Paint();
            Paint headerPaint = new Paint();

            int pageWidth = 595;
            int pageHeight = 842;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

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
            canvas.drawText("Payroll Report - " + tvSelectedMonth.getText(), x, y, paint);

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
            canvas.drawText("Salary", x + 300, y, headerPaint);
            canvas.drawText("Deduction", x + 400, y, headerPaint);
            canvas.drawText("Remaining", x + 500, y, headerPaint);

            y += 10;
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;

            // Table Content
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            paint.setColor(Color.BLACK);
            paint.setTextSize(10f);

            double totalDeductions = 0;
            for (Employee e : filteredEmployees) {
                if (y > 780) {
                    pdfDocument.finishPage(page);
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }
                double ded = deductionsMap.getOrDefault(e.getId(), 0.0);
                double rem = e.getSalary() - ded;
                totalDeductions += ded;

                canvas.drawText(e.getFullName(), x, y, paint);
                canvas.drawText(e.getDepartment() != null ? e.getDepartment() : "N/A", x + 200, y, paint);
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", e.getSalary()), x + 300, y, paint);
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", ded), x + 400, y, paint);
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", rem), x + 500, y, paint);
                y += 20;
            }

            // Summary
            y += 20;
            canvas.drawLine(x, y, 555, y, paint);
            y += 25;
            headerPaint.setTextSize(14);
            canvas.drawText("Total Employees: " + filteredEmployees.size(), x, y, headerPaint);
            y += 25;
            canvas.drawText("Grand Total Expenses: " + String.format(Locale.getDefault(), "₱%.2f", totalDeductions), x, y, headerPaint);

            // Footer
            paint.setTextSize(10);
            paint.setColor(Color.LTGRAY);
            canvas.drawText("End of Report", 260, 820, paint);

            pdfDocument.finishPage(page);

            String fileName = "Payroll_Report_" + dbMonthFormat.format(calendar.getTime()) + ".pdf";

            try {
                savePdfToDownloads(pdfDocument, fileName);
            } catch (IOException e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Failed to generate PDF: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            } finally {
                pdfDocument.close();
            }
        });
    }

    private void savePdfToDownloads(PdfDocument document, String fileName) throws IOException {
        if (getContext() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri uri = getContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                try (OutputStream outputStream = getContext().getContentResolver().openOutputStream(uri)) {
                    if (outputStream == null) throw new IOException("Failed to open output stream");
                    document.writeTo(outputStream);
                }

                contentValues.clear();
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                getContext().getContentResolver().update(uri, contentValues, null, null);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                        showOpenOption(uri);
                    });
                }
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
            MediaScannerConnection.scanFile(getContext(), new String[]{finalFile.getAbsolutePath()}, null, (path, uri) -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "PDF saved to Downloads", Toast.LENGTH_LONG).show();
                        if (uri != null) {
                            showOpenOption(uri);
                        } else {
                            Uri contentUri = FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", finalFile);
                            showOpenOption(contentUri);
                        }
                    });
                }
            });
        }
    }

    private void showOpenOption(Uri uri) {
        if (getContext() == null) return;
        new MaterialAlertDialogBuilder(getContext())
                .setTitle("PDF Generated")
                .setMessage("Your report has been saved to the Downloads folder.")
                .setPositiveButton("Open", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "No PDF viewer found", Toast.LENGTH_SHORT).show();
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
