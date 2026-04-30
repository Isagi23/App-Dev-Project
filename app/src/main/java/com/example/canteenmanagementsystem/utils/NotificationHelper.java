package com.example.canteenmanagementsystem.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.canteenmanagementsystem.models.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;

public class NotificationHelper {

    private static final String PREFS_NAME = "NotificationSettings";

    public static void createNotification(Context context, String title, String message, String type) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        boolean enabled = true;
        switch (type) {
            case "ORDER":
                enabled = prefs.getBoolean("orders", true);
                break;
            case "STAFF":
                enabled = prefs.getBoolean("staff", true);
                break;
            case "REPORT":
                enabled = prefs.getBoolean("reports", true);
                break;
            case "SYSTEM":
                enabled = prefs.getBoolean("system", true);
                break;
        }

        if (enabled) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Notification notification = new Notification(null, title, message, type);
            notification.setTimestamp(new Date());
            db.collection("notifications").add(notification);
        }
    }
}
