package com.example.canteenmanagementsystem.models;

import java.util.Date;

public class Notification {
    private String id;
    private String title;
    private String message;
    private Date timestamp;
    private boolean read;
    private String type; // e.g., "ORDER", "SYSTEM", "STAFF"

    public Notification() {}

    public Notification(String id, String title, String message, String type) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timestamp = new Date();
        this.read = false;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}