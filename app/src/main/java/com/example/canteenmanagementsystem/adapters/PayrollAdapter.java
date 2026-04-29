package com.example.canteenmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.R;
import com.example.canteenmanagementsystem.models.Employee;

import java.util.List;
import java.util.Locale;
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee_deduction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee employee = employees.get(position);
        Double deduction = deductions.get(employee.getId());
        double amount = deduction != null ? deduction : 0.0;

        holder.tvName.setText(employee.getFullName());
        holder.tvRole.setText(employee.getPosition() != null ? employee.getPosition() : "Staff");
        holder.tvDeduction.setText(String.format(Locale.getDefault(), "-₱%.2f", amount));
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRole, tvDeduction;
        ImageView ivAvatar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvRole = itemView.findViewById(R.id.tvEmployeeRole);
            tvDeduction = itemView.findViewById(R.id.tvDeductionAmount);
            ivAvatar = itemView.findViewById(R.id.ivEmployeeAvatar);
        }
    }
}