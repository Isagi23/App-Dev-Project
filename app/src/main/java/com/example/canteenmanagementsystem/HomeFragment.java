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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.adapters.RecentActivityAdapter;
import com.example.canteenmanagementsystem.models.Order;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView tvGreeting, tvCurrentDate, tvTotalEmployees, tvOrdersToday, tvMonthExpense;
    private View notificationBadge;
    private RecentActivityAdapter recentActivityAdapter;
    private final List<Order> recentOrders = new ArrayList<>();
    private FirebaseFirestore db;
    private ListenerRegistration employeesListener, ordersTodayListener, monthOrdersListener, notificationsListener, recentOrdersListener;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
        tvTotalEmployees = view.findViewById(R.id.tvTotalEmployeesCount);
        tvOrdersToday = view.findViewById(R.id.tvOrdersTodayCount);
        tvMonthExpense = view.findViewById(R.id.tvMonthExpense);
        notificationBadge = view.findViewById(R.id.notificationBadge);
        RecyclerView rvRecentActivity = view.findViewById(R.id.rvRecentActivity);

        rvRecentActivity.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentActivityAdapter = new RecentActivityAdapter(recentOrders);
        rvRecentActivity.setAdapter(recentActivityAdapter);

        setupClickListeners(view);
        updateHeaderInfo();
        startStatsListeners();
    }

    private void setupClickListeners(View view) {
        // Notification Button
        View btnNotifications = view.findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
            });
        }

        // Quick Actions
        View btnRecordOrder = view.findViewById(R.id.btnRecordOrder);
        if (btnRecordOrder != null) {
            btnRecordOrder.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToOrders();
                }
            });
        }
        
        View btnManageMenu = view.findViewById(R.id.btnManageMenu);
        if (btnManageMenu != null) {
            btnManageMenu.setOnClickListener(v -> startActivity(new Intent(requireContext(), MenuItemActivity.class)));
        }
        
        View btnExpenseHistory = view.findViewById(R.id.btnExpenseHistory);
        if (btnExpenseHistory != null) {
            btnExpenseHistory.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToReports();
                }
            });
        }

        View btnOrderHistory = view.findViewById(R.id.btnOrderHistory);
        if (btnOrderHistory != null) {
            btnOrderHistory.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), OrderHistoryActivity.class));
            });
        }
        
        View tvViewAllActivity = view.findViewById(R.id.tvViewAllActivity);
        if (tvViewAllActivity != null) {
            tvViewAllActivity.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), OrderHistoryActivity.class));
            });
        }
    }

    private void updateHeaderInfo() {
        if (tvGreeting == null || tvCurrentDate == null) return;

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
        tvGreeting.setText(String.format("%s,", greeting));

        SimpleDateFormat dateSdf = new SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault());
        tvCurrentDate.setText(dateSdf.format(new Date()));
    }

    private void startStatsListeners() {
        employeesListener = db.collection("employees").addSnapshotListener((value, error) -> {
            if (error != null || !isAdded()) return;
            if (value != null && tvTotalEmployees != null) {
                tvTotalEmployees.setText(String.valueOf(value.size()));
            }
        });

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());

        ordersTodayListener = db.collection("orders").whereEqualTo("date", today).addSnapshotListener((value, error) -> {
            if (error != null || !isAdded()) return;
            if (value != null && tvOrdersToday != null) {
                tvOrdersToday.setText(String.valueOf(value.size()));
            }
        });

        monthOrdersListener = db.collection("orders").whereEqualTo("month", currentMonth).addSnapshotListener((value, error) -> {
            if (error != null || !isAdded()) return;
            if (value != null && tvMonthExpense != null) {
                double totalExpense = 0;
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    totalExpense += order.getTotalAmount();
                }

                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "PH"));
                formatter.setMaximumFractionDigits(2);
                String formattedExpense = formatter.format(totalExpense);
                tvMonthExpense.setText(formattedExpense);
            }
        });

        notificationsListener = db.collection("notifications")
                .whereEqualTo("read", false)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value != null && notificationBadge != null) {
                        if (!value.isEmpty()) {
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadge.setVisibility(View.GONE);
                        }
                    }
                });

        recentOrdersListener = db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .addSnapshotListener((value, error) -> {
                    if (error != null || !isAdded()) return;
                    if (value != null) {
                        recentOrders.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Order order = doc.toObject(Order.class);
                            order.setId(doc.getId());
                            recentOrders.add(order);
                        }
                        recentActivityAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (employeesListener != null) employeesListener.remove();
        if (ordersTodayListener != null) ordersTodayListener.remove();
        if (monthOrdersListener != null) monthOrdersListener.remove();
        if (notificationsListener != null) notificationsListener.remove();
        if (recentOrdersListener != null) recentOrdersListener.remove();
    }
}