package com.example.myapplication;

import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ViewHolder> {
    private List<String> imageUrls;
    public ImagePagerAdapter(List<String> imageUrls) { this.imageUrls = imageUrls; }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ViewHolder(imageView);
    }
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Glide.with(holder.imageView.getContext())
            .load(imageUrls.get(position))
            .into(holder.imageView);
    }
    @Override
    public int getItemCount() { return imageUrls.size(); }
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(@NonNull ImageView itemView) { super(itemView); imageView = itemView; }
    }
} 