package com.example.canteenmanagementsystem.models;

public class Employee {
    private String id; // Firestore document ID
    private String employeeId;
    private String fullName;
    private String department;
    private String position;
    private double salary;

    public Employee() {
        // Required for Firestore
    }

    public Employee(String id, String employeeId, String fullName, String department, String position, double salary) {
        this.id = id;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.department = department;
        this.position = position;
        this.salary = salary;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public double getSalary() { return salary; }
    public void setSalary(double salary) { this.salary = salary; }
}