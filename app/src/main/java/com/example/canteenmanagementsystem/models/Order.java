package com.example.canteenmanagementsystem.models;

import java.util.Date;
import java.util.List;

public class Order {
    private String id;
    private String employeeId;
    private String employeeName;
    private List<String> itemNames;
    private double totalAmount;
    private String date; // yyyy-MM-dd
    private String month; // yyyy-MM
    private Date timestamp;
    private List<OrderItem> items;

    public Order() {}

    public Order(String id, String employeeId, String employeeName, List<String> itemNames, double totalAmount, String date, String month) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.itemNames = itemNames;
        this.totalAmount = totalAmount;
        this.date = date;
        this.month = month;
        this.timestamp = new Date();
    }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public List<String> getItemNames() { return itemNames; }
    public void setItemNames(List<String> itemNames) { this.itemNames = itemNames; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getItemsSummary() {
        if (itemNames == null || itemNames.isEmpty()) {
            return "";
        }
        return String.join(", ", itemNames);
    }

    public String getDetailedItemsSummary() {
        if (items == null || items.isEmpty()) {
            return getItemsSummary();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            OrderItem item = items.get(i);
            sb.append(item.getName())
                    .append(" (₱")
                    .append(String.format(java.util.Locale.getDefault(), "%.2f", item.getPrice()))
                    .append(" x")
                    .append(item.getQuantity())
                    .append(")");
            if (i < items.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }
}