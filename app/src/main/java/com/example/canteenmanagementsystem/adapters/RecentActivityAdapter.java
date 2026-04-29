package com.example.canteenmanagementsystem.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.R;
import com.example.canteenmanagementsystem.models.Order;

import java.util.List;
import java.util.Locale;

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private List<Order> orders;

    public RecentActivityAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        
        String orderId = order.getId() != null ? order.getId() : "";
        if (orderId.length() > 6) {
            orderId = orderId.substring(orderId.length() - 6);
        }
        holder.tvTitle.setText(holder.itemView.getContext().getString(R.string.label_order_id, orderId));
        
        if (order.getTimestamp() != null) {
            holder.tvTime.setText(DateUtils.getRelativeTimeSpanString(order.getTimestamp().getTime(), 
                System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString().toUpperCase());
        } else {
            holder.tvTime.setText(order.getDate());
        }

        holder.tvDesc.setText(order.getItemsSummary());
        holder.tvAmount.setText(String.format(Locale.getDefault(), "₱%.2f", order.getTotalAmount()));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime, tvDesc, tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvActivityTitle);
            tvTime = itemView.findViewById(R.id.tvActivityTime);
            tvDesc = itemView.findViewById(R.id.tvActivityDesc);
            tvAmount = itemView.findViewById(R.id.tvActivityAmount);
        }
    }
}