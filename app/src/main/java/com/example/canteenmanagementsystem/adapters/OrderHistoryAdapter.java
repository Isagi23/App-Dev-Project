package com.example.canteenmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.R;
import com.example.canteenmanagementsystem.models.Order;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private List<Order> orders;
    private OnOrderDeleteListener deleteListener;
    private SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface OnOrderDeleteListener {
        void onDelete(Order order);
    }

    public OrderHistoryAdapter(List<Order> orders) {
        this(orders, null);
    }

    public OrderHistoryAdapter(List<Order> orders, OnOrderDeleteListener deleteListener) {
        this.orders = orders;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.tvDate.setText(order.getTimestamp() != null ? sdf.format(order.getTimestamp()) : order.getDate());
        holder.tvEmployee.setText(order.getEmployeeName());
        holder.tvItems.setText(order.getItemsSummary());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotalAmount()));

        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(order);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvEmployee, tvItems, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvOrderDate);
            tvEmployee = itemView.findViewById(R.id.tvOrderEmployee);
            tvItems = itemView.findViewById(R.id.tvOrderItems);
            tvAmount = itemView.findViewById(R.id.tvOrderAmount);
        }
    }
}