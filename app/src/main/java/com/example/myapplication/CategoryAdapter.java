package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private List<String> categories;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public CategoryAdapter(List<String> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        holder.categoryLabel.setText(category);

        // Highlight selected
        holder.cardView.setCardBackgroundColor(
            position == selectedPosition ?
                holder.itemView.getResources().getColor(R.color.primaryColor) :
                holder.itemView.getResources().getColor(android.R.color.white)
        );
        holder.categoryLabel.setTextColor(
            position == selectedPosition ?
                holder.itemView.getResources().getColor(android.R.color.white) :
                holder.itemView.getResources().getColor(android.R.color.black)
        );

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
            if (listener != null) listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryLabel;
        CardView cardView;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryLabel = itemView.findViewById(R.id.categoryLabel);
            cardView = (CardView) itemView;
        }
    }
} 