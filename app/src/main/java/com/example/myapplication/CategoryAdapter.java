package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    public interface OnCategoryClickListener {
        void onCategoryClick(String category);
    }

    private List<String> categories;
    private List<Integer> icons;
    private OnCategoryClickListener listener;
    private int selectedPosition = 0;

    public CategoryAdapter(List<String> categories, List<Integer> icons, OnCategoryClickListener listener) {
        this.categories = categories;
        this.icons = icons;
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
        if (icons != null && position < icons.size()) {
            holder.categoryIcon.setImageResource(icons.get(position));
        }
        // Highlight selected
        boolean isSelected = position == selectedPosition;
        holder.cardView.setCardBackgroundColor(
            isSelected ?
                ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryColor) :
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
        );
        holder.categoryLabel.setTextColor(
            isSelected ?
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white) :
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.black)
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
        ImageView categoryIcon;
        CardView cardView;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryLabel = itemView.findViewById(R.id.categoryLabel);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            cardView = (CardView) itemView;
        }
    }
} 