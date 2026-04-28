package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.canteenmanagementsystem.models.Order;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvCurrentDate, tvCycleBadge;
    private TextView tvTotalEmployees, tvOrdersToday, tvMonthExpense, tvActiveUsers;
    private TextView tvTopItem, tvPeakHour;
    private View notificationBadge;
    private FirebaseFirestore db;
    private ListenerRegistration employeesListener, ordersTodayListener, monthOrdersListener, notificationsListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvCurrentDate = view.findViewById(R.id.tvCurrentDate);
        tvCycleBadge = view.findViewById(R.id.tvCycleBadge);
        
        tvTotalEmployees = view.findViewById(R.id.tvTotalEmployeesCount);
        tvOrdersToday = view.findViewById(R.id.tvOrdersTodayCount);
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense);
        tvActiveUsers = view.findViewById(R.id.tvActiveUsersCount);
        tvTopItem = view.findViewById(R.id.tvTopItem);
        tvPeakHour = view.findViewById(R.id.tvPeakHour);
        notificationBadge = view.findViewById(R.id.notificationBadge);

        setupClickListeners(view);
        updateHeaderInfo();
        startStatsListeners();
    }

    private void setupClickListeners(View view) {
        // Notification Button
        view.findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), NotificationsActivity.class));
        });

        // Quick Actions
        view.findViewById(R.id.btnRecordOrder).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                activity.findViewById(R.id.bottomNavigation).performClick(); // Just to trigger UI state if needed, but we'll use loadFragment
                activity.navigateToOrders();
            }
        });
        
        view.findViewById(R.id.btnManageEmployees).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToStaff();
            }
        });
        
        view.findViewById(R.id.btnManageMenu).setOnClickListener(v -> startActivity(new Intent(requireContext(), MenuItemActivity.class)));
        
        view.findViewById(R.id.btnExpenseHistory).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToReports();
            }
        });

        view.findViewById(R.id.btnOrderHistory).setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), OrderHistoryActivity.class));
        });
        
        view.findViewById(R.id.tvSeeAllActions).setOnClickListener(v -> {
             Toast.makeText(requireContext(), "See all actions clicked", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateHeaderInfo() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour >= 5 && hour < 12) {
            greeting = getString(R.string.greeting_morning);
        } else if (hour >= 12 && hour < 17) {
            greeting = getString(R.string.greeting_afternoon);
        } else {
            greeting = getString(R.string.greeting_evening);
        }
        tvGreeting.setText(greeting);

        SimpleDateFormat dateSdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        tvCurrentDate.setText(dateSdf.format(new Date()));

        SimpleDateFormat monthYearSdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String cycleDate = monthYearSdf.format(new Date());
        tvCycleBadge.setText(getString(R.string.cycle_active, cycleDate));
    }

    private void startStatsListeners() {
        employeesListener = db.collection("employees").addSnapshotListener((value, error) -> {
            if (error != null || !isAdded()) return;
            if (value != null) {
                tvTotalEmployees.setText(String.valueOf(value.size()));
            }
        });

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        ordersTodayListener = db.collection("orders").whereEqualTo("date", today).addSnapshotListener((value, error) -> {
            if (error != null || !isAdded()) return;
            if (value != null) {
                tvOrdersToday.setText(String.valueOf(value.size()));
            }
        });

        monthOrdersListener = db.collection("orders").whereEqualTo("month", currentMonth).addSnapshotListener((value, error) -> {
            if (error != null || !isAdded()) return;
            if (value != null) {
                double totalExpense = 0;
                Set<String> activeUsers = new HashSet<>();
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    totalExpense += order.getTotalAmount();
                    activeUsers.add(order.getEmployeeId());
                }

                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                formatter.setMaximumFractionDigits(0);
                String formattedExpense = formatter.format(totalExpense);
                tvMonthExpense.setText(formattedExpense);

                tvActiveUsers.setText(String.valueOf(activeUsers.size()));
                calculateExtraStats(value);
            }
        });

        notificationsListener = db.collection("notifications")
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value != null && !value.isEmpty()) {
                        notificationBadge.setVisibility(View.VISIBLE);
                    } else {
                        notificationBadge.setVisibility(View.GONE);
                    }
                });
    }

    private void calculateExtraStats(com.google.firebase.firestore.QuerySnapshot value) {
        java.util.Map<String, Integer> itemCounts = new java.util.HashMap<>();
        java.util.Map<Integer, Integer> hourCounts = new java.util.HashMap<>();

        for (QueryDocumentSnapshot doc : value) {
            Order order = doc.toObject(Order.class);
            
            // Peak Hour Calculation
            if (order.getTimestamp() != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(order.getTimestamp());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourCounts.put(hour, hourCounts.getOrDefault(hour, 0) + 1);
            }

            // Top Item (Assuming Order has a list of items or we can infer it)
            // For now, let's just count orders as a placeholder if items aren't directly available
        }

        // Find peak hour
        int peakHour = -1;
        int maxOrders = 0;
        for (java.util.Map.Entry<Integer, Integer> entry : hourCounts.entrySet()) {
            if (entry.getValue() > maxOrders) {
                maxOrders = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        if (peakHour != -1) {
            String ampm = peakHour >= 12 ? "PM" : "AM";
            int displayHour = peakHour > 12 ? peakHour - 12 : (peakHour == 0 ? 12 : peakHour);
            tvPeakHour.setText(displayHour + ":00 " + ampm);
        } else {
            tvPeakHour.setText("N/A");
        }
        
        tvTopItem.setText("Regular Meal"); // Placeholder for now
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (employeesListener != null) employeesListener.remove();
        if (ordersTodayListener != null) ordersTodayListener.remove();
        if (monthOrdersListener != null) monthOrdersListener.remove();
        if (notificationsListener != null) notificationsListener.remove();
    }
}
