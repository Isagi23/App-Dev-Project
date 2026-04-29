package com.example.canteenmanagementsystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.canteenmanagementsystem.R;

import java.util.List;
import java.util.Map;

public class TopSellingAdapter extends RecyclerView.Adapter<TopSellingAdapter.ViewHolder> {

    private List<Map.Entry<String, Integer>> items;
    private int maxSales;

    public TopSellingAdapter(List<Map.Entry<String, Integer>> items, int maxSales) {
        this.items = items;
        this.maxSales = maxSales;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_selling, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Integer> entry = items.get(position);
        holder.tvName.setText(entry.getKey());
        holder.tvCount.setText(holder.itemView.getContext().getString(R.string.label_units, entry.getValue()));
        
        if (maxSales > 0) {
            int progress = (int) ((entry.getValue() * 100.0) / maxSales);
            holder.progressBar.setProgress(progress);
        } else {
            holder.progressBar.setProgress(0);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;
        ProgressBar progressBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvCount = itemView.findViewById(R.id.tvItemCount);
            progressBar = itemView.findViewById(R.id.pbItemSales);
        }
    }
}