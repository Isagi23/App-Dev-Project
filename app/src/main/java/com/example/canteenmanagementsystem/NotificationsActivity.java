package com.example.canteenmanagementsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.canteenmanagementsystem.adapters.NotificationAdapter;
import com.example.canteenmanagementsystem.models.Notification;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private FirebaseFirestore db;

    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        db = FirebaseFirestore.getInstance();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        emptyState = findViewById(R.id.emptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notificationList, this::markAsRead);
        rvNotifications.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadNotifications);

        loadNotifications();
    }

    private void loadNotifications() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if (notificationListener != null) {
            notificationListener.remove();
        }

        swipeRefresh.setRefreshing(true);
        // Using snapshot listener for real-time updates and better reliability
        notificationListener = db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    swipeRefresh.setRefreshing(false);
                    if (error != null) {
                        // If orderBy fails due to missing index, fallback to unordered and sort locally
                        if (error.getMessage() != null && error.getMessage().contains("FAILED_PRECONDITION")) {
                            loadNotificationsFallback();
                        } else {
                            Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (value != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setId(doc.getId());
                            notificationList.add(notification);
                        }
                        adapter.notifyDataSetChanged();
                        emptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void loadNotificationsFallback() {
        db.collection("notifications").get().addOnSuccessListener(queryDocumentSnapshots -> {
            notificationList.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Notification notification = doc.toObject(Notification.class);
                notification.setId(doc.getId());
                notificationList.add(notification);
            }
            // Sort locally by timestamp descending
            notificationList.sort((n1, n2) -> {
                if (n1.getTimestamp() == null || n2.getTimestamp() == null) return 0;
                return n2.getTimestamp().compareTo(n1.getTimestamp());
            });
            adapter.notifyDataSetChanged();
            emptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }

    private void markAsRead(Notification notification) {
        if (!notification.isRead()) {
            db.collection("notifications").document(notification.getId())
                    .update("read", true)
                    .addOnSuccessListener(aVoid -> {
                        notification.setRead(true);
                        adapter.notifyDataSetChanged();
                    });
        }
    }
}