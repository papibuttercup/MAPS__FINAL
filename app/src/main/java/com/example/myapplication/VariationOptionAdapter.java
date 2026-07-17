package com.example.myapplication;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class VariationOptionAdapter extends RecyclerView.Adapter<VariationOptionAdapter.ViewHolder> {

    public static class VariationOption {
        String name;
        Uri imageUri;
        String uploadedUrl;

        public VariationOption(String name) {
            this.name = name;
        }
    }

    private List<VariationOption> options;
    private OnOptionInteractionListener listener;
    private boolean showImages = true;

    public interface OnOptionInteractionListener {
        void onRemoveOption(int position);
        void onPickImage(int position);
    }

    public VariationOptionAdapter(List<VariationOption> options, OnOptionInteractionListener listener) {
        this.options = options;
        this.listener = listener;
    }

    public void setShowImages(boolean show) {
        this.showImages = show;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_variation_option, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VariationOption option = options.get(position);
        holder.tvOptionName.setText(option.name);

        if (showImages) {
            holder.cardVariationImage.setVisibility(View.VISIBLE);
            if (option.imageUri != null) {
                holder.ivVariationPreview.setVisibility(View.VISIBLE);
                holder.ivAddIcon.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext()).load(option.imageUri).into(holder.ivVariationPreview);
            } else {
                holder.ivVariationPreview.setVisibility(View.GONE);
                holder.ivAddIcon.setVisibility(View.VISIBLE);
            }
        } else {
            holder.cardVariationImage.setVisibility(View.GONE);
        }

        holder.cardVariationImage.setOnClickListener(v -> listener.onPickImage(position));
        holder.btnRemoveOption.setOnClickListener(v -> listener.onRemoveOption(position));
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View cardVariationImage;
        ImageView ivVariationPreview, ivAddIcon;
        TextView tvOptionName;
        ImageButton btnRemoveOption;

        ViewHolder(View v) {
            super(v);
            cardVariationImage = v.findViewById(R.id.cardVariationImage);
            ivVariationPreview = v.findViewById(R.id.ivVariationPreview);
            ivAddIcon = v.findViewById(R.id.ivAddIcon);
            tvOptionName = v.findViewById(R.id.tvOptionName);
            btnRemoveOption = v.findViewById(R.id.btnRemoveOption);
        }
    }
}