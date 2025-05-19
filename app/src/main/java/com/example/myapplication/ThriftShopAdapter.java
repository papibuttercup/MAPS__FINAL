package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class ThriftShopAdapter extends RecyclerView.Adapter<ThriftShopAdapter.ViewHolder> {
    private List<ThriftShop> shops;
    private Context context;
    private OnShopClickListener listener;

    public interface OnShopClickListener {
        void onVisitShop(ThriftShop shop);
        void onViewLocation(ThriftShop shop);
    }

    public ThriftShopAdapter(Context context, List<ThriftShop> shops, OnShopClickListener listener) {
        this.context = context;
        this.shops = shops;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_thrift_shop, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ThriftShop shop = shops.get(position);
        
        holder.tvShopName.setText(shop.getName());
        holder.tvShopTypeLocation.setText(shop.getType() + " · " + shop.getLocation());
        
        // Load shop image using Glide
        if (shop.getCoverPhotoUri() != null && !shop.getCoverPhotoUri().isEmpty()) {
            Glide.with(context)
                .load(shop.getCoverPhotoUri())
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_placeholder)
                .centerCrop()
                .into(holder.imgShopCover);
        } else {
            holder.imgShopCover.setImageResource(R.drawable.ic_image_placeholder);
        }

        // Set click listeners
        holder.btnVisitShop.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVisitShop(shop);
            }
        });

        holder.btnShopLocation.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewLocation(shop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shops.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShopName, tvShopTypeLocation;
        ImageView imgShopCover;
        com.google.android.material.button.MaterialButton btnVisitShop, btnShopLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShopName = itemView.findViewById(R.id.tvShopName);
            tvShopTypeLocation = itemView.findViewById(R.id.tvShopTypeLocation);
            imgShopCover = itemView.findViewById(R.id.imgShopCover);
            btnVisitShop = itemView.findViewById(R.id.btnVisitShop);
            btnShopLocation = itemView.findViewById(R.id.btnShopLocation);
        }
    }
} 