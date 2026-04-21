package com.example.canteenmanagementsystem.models;

import java.util.List;

public class Order {
    private String id;
    private String employeeId;
    private String employeeName;
    private List<String> itemNames;
    private double totalAmount;
    private String date; // yyyy-MM-dd
    private String month; // yyyy-MM

    public Order() {}

    public Order(String id, String employeeId, String employeeName, List<String> itemNames, double totalAmount, String date, String month) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.itemNames = itemNames;
        this.totalAmount = totalAmount;
        this.date = date;
        this.month = month;
    }

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

    public String getItemsSummary() {
        if (itemNames == null || itemNames.isEmpty()) {
            return "";
        }
        return String.join(", ", itemNames);
    }
}