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

        swipeRefresh.setRefreshing(true);
        db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    swipeRefresh.setRefreshing(false);
                    notificationList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Notification notification = doc.toObject(Notification.class);
                        notification.setId(doc.getId());
                        notificationList.add(notification);
                    }
                    adapter.notifyDataSetChanged();
                    emptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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