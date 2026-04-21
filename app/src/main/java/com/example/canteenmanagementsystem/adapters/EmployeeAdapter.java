package com.example.canteenmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.R;
import com.example.canteenmanagementsystem.models.Employee;
import com.google.android.material.chip.Chip;

import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.ViewHolder> {

    private List<Employee> employees;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Employee employee);
        void onItemLongClick(Employee employee);
    }

    public EmployeeAdapter(List<Employee> employees, OnItemClickListener listener) {
        this.employees = employees;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_employee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Employee employee = employees.get(position);
        holder.tvName.setText(employee.getFullName());
        holder.tvEmployeeId.setText("#" + employee.getEmployeeId());
        holder.tvPosition.setText(employee.getPosition());
        holder.chipDepartment.setText(employee.getDepartment());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(employee));
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(employee);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return employees.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmployeeId, tvPosition;
        Chip chipDepartment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmployeeId = itemView.findViewById(R.id.tvEmployeeId);
            tvPosition = itemView.findViewById(R.id.tvEmployeePosition);
            chipDepartment = itemView.findViewById(R.id.chipDepartmentBadge);
        }
    }
}