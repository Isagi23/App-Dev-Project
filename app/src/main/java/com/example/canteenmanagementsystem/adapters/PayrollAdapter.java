package com.example.canteenmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.R;
import com.example.canteenmanagementsystem.models.Employee;

import java.util.List;
import java.util.Map;

public class PayrollAdapter extends RecyclerView.Adapter<PayrollAdapter.ViewHolder> {

    private List<Employee> employees;
    private Map<String, Double> deductions;

    public PayrollAdapter(List<Employee> employees, Map<String, Double> deductions) {
        this.employees = employees;
        this.deductions = deductions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_payroll, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee employee = employees.get(position);
        double deduction = deductions.containsKey(employee.getId()) ? deductions.get(employee.getId()) : 0.0;
        double salary = employee.getSalary();
        double remaining = salary - deduction;

        holder.tvName.setText(employee.getFullName());
        holder.tvDept.setText(employee.getDepartment());
        holder.tvDeduction.setText(String.format("₱%.2f", deduction));
        holder.tvSalary.setText(String.format("₱%.2f", salary));
        holder.tvRemaining.setText(String.format("₱%.2f", remaining));
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDept, tvDeduction, tvSalary, tvRemaining;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPayrollName);
            tvDept = itemView.findViewById(R.id.tvPayrollDept);
            tvDeduction = itemView.findViewById(R.id.tvPayrollDeduction);
            tvSalary = itemView.findViewById(R.id.tvPayrollSalary);
            tvRemaining = itemView.findViewById(R.id.tvPayrollRemaining);
        }
    }
}