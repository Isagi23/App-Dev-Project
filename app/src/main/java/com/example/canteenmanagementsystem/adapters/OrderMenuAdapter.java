package com.example.canteenmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.canteenmanagementsystem.R;
import com.example.canteenmanagementsystem.models.MenuItem;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderMenuAdapter extends RecyclerView.Adapter<OrderMenuAdapter.ViewHolder> {

    private List<MenuItem> menuItems;
    private final Map<MenuItem, Integer> quantities = new HashMap<>();
    private final OnQuantityChangedListener listener;

    public interface OnQuantityChangedListener {
        void onQuantityChanged(Map<MenuItem, Integer> selectedItems);
    }

    public OrderMenuAdapter(List<MenuItem> menuItems, OnQuantityChangedListener listener) {
        this.menuItems = menuItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MenuItem item = menuItems.get(position);
        holder.tvName.setText(item.getName());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "₱ %.2f", item.getPrice()));

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_food)
                    .error(R.drawable.ic_food)
                    .centerCrop()
                    .into(holder.ivMenuItem);
        } else {
            holder.ivMenuItem.setImageResource(R.drawable.ic_food);
        }

        int qty = quantities.getOrDefault(item, 0);
        holder.tvQuantity.setText(String.valueOf(qty));

        holder.btnPlus.setOnClickListener(v -> {
            int newQty = quantities.getOrDefault(item, 0) + 1;
            quantities.put(item, newQty);
            holder.tvQuantity.setText(String.valueOf(newQty));
            if (listener != null) {
                listener.onQuantityChanged(new HashMap<>(quantities));
            }
        });

        holder.btnMinus.setOnClickListener(v -> {
            int currentQty = quantities.getOrDefault(item, 0);
            if (currentQty > 0) {
                int newQty = currentQty - 1;
                if (newQty == 0) {
                    quantities.remove(item);
                } else {
                    quantities.put(item, newQty);
                }
                holder.tvQuantity.setText(String.valueOf(newQty));
                if (listener != null) {
                    listener.onQuantityChanged(new HashMap<>(quantities));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return menuItems.size();
    }

    public void updateList(List<MenuItem> newList) {
        this.menuItems = newList;
        notifyDataSetChanged();
    }

    public Map<MenuItem, Integer> getSelectedItemsWithQuantities() {
        return new HashMap<>(quantities);
    }

    public void clearSelection() {
        quantities.clear();
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvQuantity;
        ImageView ivMenuItem;
        MaterialButton btnPlus, btnMinus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvPrice = itemView.findViewById(R.id.tvItemPrice);
            ivMenuItem = itemView.findViewById(R.id.ivMenuItem);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}
